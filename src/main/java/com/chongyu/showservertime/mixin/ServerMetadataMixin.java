package com.chongyu.showservertime.mixin;

import com.chongyu.showservertime.core.IServerMetadataAssor;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.*;

@Mixin(ServerMetadata.class)
public class ServerMetadataMixin implements IServerMetadataAssor {
    @Unique
    private int timeClient;
    @Mutable
    @Final
    @Shadow
    private Text description;

    @Overwrite
    public Text description(){
        if (this.description != null){
            this.description = this.description.copy().append("\n")
                    .append((Text.translatable("multiplayer.server.aliveandwell_time_tip",
                                    aliveandwell$getTime())
                            .formatted(Formatting.YELLOW)));
        }
        return this.description;
    }

    @Override
    public void aliveandwell$setTime(int time) {
        this.timeClient = time;
    }

    @Override
    public int aliveandwell$getTime() {
        return this.timeClient;
    }
}

