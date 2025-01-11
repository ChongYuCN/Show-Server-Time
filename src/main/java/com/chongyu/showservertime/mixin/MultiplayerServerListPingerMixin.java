package com.chongyu.showservertime.mixin;

import com.chongyu.showservertime.core.IServerMetadataAssor;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.network.*;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.listener.ClientQueryPacketListener;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.query.QueryPongS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;

@Mixin(MultiplayerServerListPinger.class)
public abstract class MultiplayerServerListPingerMixin {
    @Shadow
    static final Logger LOGGER = LogUtils.getLogger();

    @Shadow
    private final List<ClientConnection> clientConnections = Collections.synchronizedList(Lists.newArrayList());

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void add(final ServerInfo entry, final Runnable saver) throws UnknownHostException {
        ServerAddress serverAddress = ServerAddress.parse(entry.address);
        Optional<InetSocketAddress> optional = AllowedAddressResolver.DEFAULT.resolve(serverAddress).map(Address::getInetSocketAddress);
        if (!optional.isPresent()) {
            this.showError(ConnectScreen.BLOCKED_HOST_TEXT, entry);
            return;
        }
        final InetSocketAddress inetSocketAddress = optional.get();
        final ClientConnection clientConnection = ClientConnection.connect(inetSocketAddress, false);
        this.clientConnections.add(clientConnection);
        entry.label = Text.translatable("multiplayer.status.pinging");
        entry.ping = -1L;
        entry.playerListSummary = null;
        clientConnection.setPacketListener(new ClientQueryPacketListener(){
            private boolean sentQuery;
            private boolean received;
            private long startTime;

            @Override
            public void onResponse(QueryResponseS2CPacket packet) {
                if (this.received) {
                    clientConnection.disconnect(Text.translatable("multiplayer.status.unrequested"));
                    return;
                }
                this.received = true;
                ServerMetadata serverMetadata = packet.getServerMetadata();
//                entry.label = serverMetadata.getDescription() != null ? serverMetadata.getDescription() : ScreenTexts.EMPTY;

                //客户端多人游戏列表显示服务器时间+++++++++++++++++++++++++++++++++++++++++
                if (serverMetadata.getDescription() != null) {
                    entry.label = serverMetadata.getDescription().copy()
                            .append("\n")
                            .append((Text.translatable("multiplayer.server.aliveandwell_time_tip",
                                    ((IServerMetadataAssor)(Object)serverMetadata).aliveandwell$getTime())
                                    .formatted(Formatting.YELLOW)));
                } else {
                    entry.label = ScreenTexts.EMPTY;
                }

                if (serverMetadata.getVersion() != null) {
                    entry.version = Text.literal(serverMetadata.getVersion().getGameVersion());
                    entry.protocolVersion = serverMetadata.getVersion().getProtocolVersion();
                } else {
                    entry.version = Text.translatable("multiplayer.status.old");
                    entry.protocolVersion = 0;
                }
                if (serverMetadata.getPlayers() != null) {
                    entry.playerCountLabel = createPlayerCountText(serverMetadata.getPlayers().getOnlinePlayerCount(), serverMetadata.getPlayers().getPlayerLimit());
                    ArrayList<Text> list = Lists.newArrayList();
                    GameProfile[] gameProfiles = serverMetadata.getPlayers().getSample();
                    if (gameProfiles != null && gameProfiles.length > 0) {
                        for (GameProfile gameProfile : gameProfiles) {
                            list.add(Text.literal(gameProfile.getName()));
                        }
                        if (gameProfiles.length < serverMetadata.getPlayers().getOnlinePlayerCount()) {
                            list.add(Text.translatable("multiplayer.status.and_more", serverMetadata.getPlayers().getOnlinePlayerCount() - gameProfiles.length));
                        }
                        entry.playerListSummary = list;
                    }
                } else {
                    entry.playerCountLabel = Text.translatable("multiplayer.status.unknown").formatted(Formatting.DARK_GRAY);
                }
                String string = serverMetadata.getFavicon();
                if (string != null) {
                    try {
                        string = ServerInfo.parseFavicon(string);
                    } catch (ParseException parseException) {
                        LOGGER.error("Invalid server icon", parseException);
                    }
                }
                if (!Objects.equals(string, entry.getIcon())) {
                    entry.setIcon(string);
                    saver.run();
                }
                this.startTime = Util.getMeasuringTimeMs();
                clientConnection.send(new QueryPingC2SPacket(this.startTime));
                this.sentQuery = true;
            }

            @Override
            public void onPong(QueryPongS2CPacket packet) {
                long l = this.startTime;
                long m = Util.getMeasuringTimeMs();
                entry.ping = m - l;
                clientConnection.disconnect(Text.translatable("multiplayer.status.finished"));
            }

            @Override
            public void onDisconnected(Text reason) {
                if (!this.sentQuery) {
                    showError(reason, entry);
                    ping(inetSocketAddress, entry);
                }
            }

            @Override
            public ClientConnection getConnection() {
                return clientConnection;
            }
        });
        try {
            clientConnection.send(new HandshakeC2SPacket(serverAddress.getAddress(), serverAddress.getPort(), NetworkState.STATUS));
            clientConnection.send(new QueryRequestC2SPacket());
        } catch (Throwable throwable) {
            LOGGER.error("Failed to ping server {}", (Object)serverAddress, (Object)throwable);
        }
    }

    @Shadow
    abstract void showError(Text error, ServerInfo info);

    @Shadow
    abstract void ping(final InetSocketAddress address, final ServerInfo info);

    @Shadow
    static Text createPlayerCountText(int current, int max) {
        return Text.literal(Integer.toString(current)).append(Text.literal("/").formatted(Formatting.DARK_GRAY)).append(Integer.toString(max)).formatted(Formatting.GRAY);
    }
}
