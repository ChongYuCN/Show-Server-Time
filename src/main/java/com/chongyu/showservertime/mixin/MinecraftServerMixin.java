package com.chongyu.showservertime.mixin;

import com.chongyu.showservertime.core.IServerMetadataAssor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin extends ReentrantThreadExecutor<ServerTask> implements CommandOutput, AutoCloseable {


    public MinecraftServerMixin(String string) {
        super(string);
    }

    @Shadow public abstract ServerWorld getOverworld();

    @Shadow private ServerMetadata metadata;


    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        ((IServerMetadataAssor)(Object)metadata).aliveandwell$setTime((int) this.aliveandwell$getTimeHour());
//        System.out.println("666666666666666666666666666666TIME:"+((IServerMetadataAssor)(Object)metadata).aliveandwell$getTime());
    }

    @Unique
    public long aliveandwell$getTimeHour() {
        long timeOfDay = this.getOverworld().getTimeOfDay();
        long day = this.getOverworld().getTimeOfDay() / 24000L;
        return (timeOfDay-day*24000L)/1000L >= 18 ? (timeOfDay-day*24000L)/1000L - 18L : (timeOfDay-day*24000L)/1000L + 6L;
    }

}
