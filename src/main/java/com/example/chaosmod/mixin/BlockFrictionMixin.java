package com.example.chaosmod.mixin;

import com.example.chaosmod.client.ClientChaosState;
import com.example.chaosmod.event.ChaosEvent;
import com.example.chaosmod.server.ChaosServerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockFrictionMixin {
    @Inject(method = "getFriction", at = @At("HEAD"), cancellable = true, remap = false)
    private void chaosmod$modifyBlockFriction(LevelReader level, BlockPos pos, Entity entity, CallbackInfoReturnable<Float> cir) {
        if (entity == null) return;
        if (entity.level().isClientSide) {
            if (ClientChaosState.isEventActive(ChaosEvent.COW_ICE)) {
                cir.setReturnValue(0.989f); // Blue Ice friction!
            }
        } else if (entity instanceof ServerPlayer sp) {
            if (ChaosServerManager.getData(sp).isEventActive(ChaosEvent.COW_ICE)) {
                cir.setReturnValue(0.989f);
            }
        }
    }
}
