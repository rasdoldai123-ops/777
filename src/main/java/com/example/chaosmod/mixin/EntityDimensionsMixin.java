package com.example.chaosmod.mixin;

import com.example.chaosmod.client.ClientChaosState;
import com.example.chaosmod.server.ChaosServerManager;
import com.example.chaosmod.server.PlayerChaosData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class EntityDimensionsMixin {
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void chaosmod$modifyDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player self = (Player) (Object) this;
        float scale = 1.0f;
        if (self instanceof ServerPlayer sp) {
            PlayerChaosData data = ChaosServerManager.getData(sp);
            scale = data.getPlayerScale();
        } else if (self.level().isClientSide) {
            scale = ClientChaosState.playerScale;
        }

        if (scale != 1.0f) {
            EntityDimensions orig = cir.getReturnValue();
            cir.setReturnValue(orig.scale(scale));
        }
    }

    @Inject(method = "getStandingEyeHeight", at = @At("RETURN"), cancellable = true)
    private void chaosmod$modifyEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;
        float scale = 1.0f;
        if (self instanceof ServerPlayer sp) {
            PlayerChaosData data = ChaosServerManager.getData(sp);
            scale = data.getPlayerScale();
        } else if (self.level().isClientSide) {
            scale = ClientChaosState.playerScale;
        }

        if (scale != 1.0f) {
            cir.setReturnValue(cir.getReturnValue() * scale);
        }
    }
}
