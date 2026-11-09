package com.example.chaosmod.mixin;

import com.example.chaosmod.client.ClientChaosState;
import com.example.chaosmod.event.ChaosEvent;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void chaosmod$multiplySensitivityForSniperScope(CallbackInfo ci) {
        if (ClientChaosState.isEventActive(ChaosEvent.SNIPER_SCOPE)) {
            this.accumulatedDX *= 3.0D;
            this.accumulatedDY *= 3.0D;
        }
    }
}
