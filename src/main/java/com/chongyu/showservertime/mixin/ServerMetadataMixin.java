package com.chongyu.showservertime.mixin;

import com.chongyu.showservertime.core.IServerMetadataAssor;
import net.minecraft.server.ServerMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(ServerMetadata.class)
public class ServerMetadataMixin implements IServerMetadataAssor {
    @Unique
    private int timeClient;

    @Override
    public void aliveandwell$setTime(int time) {
        this.timeClient = time;
    }

    @Override
    public int aliveandwell$getTime() {
        return this.timeClient;
    }
}

