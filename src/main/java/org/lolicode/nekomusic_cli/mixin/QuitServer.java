package org.lolicode.nekomusic_cli.mixin;

import org.lolicode.nekomusic_cli.NekoMusic_Cli;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class QuitServer {
    @Inject(method = "disconnect()V", at = @At("TAIL"))
    public void Quit(CallbackInfo info) {
        NekoMusic_Cli.onServerQuit();
    }
}
