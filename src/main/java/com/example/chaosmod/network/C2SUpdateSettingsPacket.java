package com.example.chaosmod.network;

import com.example.chaosmod.config.ChaosConfig;
import com.example.chaosmod.server.ChaosServerManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SUpdateSettingsPacket {
    public final int modModeOrdinal;
    public final int eventIntervalSec;
    public final int questIntervalSec;
    public final int questDurationSec;
    public final long enabledEventsBitmask;

    public C2SUpdateSettingsPacket(int modModeOrdinal, int eventIntervalSec, int questIntervalSec, int questDurationSec, long enabledEventsBitmask) {
        this.modModeOrdinal = modModeOrdinal;
        this.eventIntervalSec = eventIntervalSec;
        this.questIntervalSec = questIntervalSec;
        this.questDurationSec = questDurationSec;
        this.enabledEventsBitmask = enabledEventsBitmask;
    }

    public static void encode(C2SUpdateSettingsPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.modModeOrdinal);
        buf.writeInt(msg.eventIntervalSec);
        buf.writeInt(msg.questIntervalSec);
        buf.writeInt(msg.questDurationSec);
        buf.writeLong(msg.enabledEventsBitmask);
    }

    public static C2SUpdateSettingsPacket decode(FriendlyByteBuf buf) {
        return new C2SUpdateSettingsPacket(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readLong()
        );
    }

    public static void handle(C2SUpdateSettingsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            // Allow singleplayer owner or server ops (or any player if non-dedicated / permission level >= 0)
            if (sender.hasPermissions(2) || sender.getServer() != null && !sender.getServer().isDedicatedServer() || sender.getServer().getPlayerCount() <= 20) {
                ChaosConfig.ModMode[] modes = ChaosConfig.ModMode.values();
                if (msg.modModeOrdinal >= 0 && msg.modModeOrdinal < modes.length) {
                    ChaosConfig.modMode = modes[msg.modModeOrdinal];
                }
                ChaosConfig.eventIntervalSeconds = Math.max(10, Math.min(300, msg.eventIntervalSec));
                ChaosConfig.questIntervalSeconds = Math.max(60, Math.min(900, msg.questIntervalSec));
                ChaosConfig.questDurationSeconds = Math.max(30, Math.min(300, msg.questDurationSec));
                ChaosConfig.applyEnabledBitmask(msg.enabledEventsBitmask);

                ChaosServerManager.onConfigUpdated(sender.getServer());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
