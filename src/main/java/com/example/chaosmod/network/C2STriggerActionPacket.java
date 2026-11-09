package com.example.chaosmod.network;

import com.example.chaosmod.event.ChaosEvent;
import com.example.chaosmod.server.ChaosServerManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2STriggerActionPacket {
    public static final int ACTION_TRIGGER_EVENT = 0;
    public static final int ACTION_CAPTCHA_SOLVED = 1;
    public static final int ACTION_TRIGGER_RANDOM_QUEST = 2;
    public static final int ACTION_CLEAR_ACTIVE_EVENTS = 3;
    public static final int ACTION_PLAYER_JUMPED = 4;

    public final int actionType;
    public final int eventOrdinal;

    public C2STriggerActionPacket(int actionType, int eventOrdinal) {
        this.actionType = actionType;
        this.eventOrdinal = eventOrdinal;
    }

    public static void encode(C2STriggerActionPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.actionType);
        buf.writeInt(msg.eventOrdinal);
    }

    public static C2STriggerActionPacket decode(FriendlyByteBuf buf) {
        return new C2STriggerActionPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(C2STriggerActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            switch (msg.actionType) {
                case ACTION_TRIGGER_EVENT -> {
                    ChaosEvent[] events = ChaosEvent.values();
                    if (msg.eventOrdinal >= 0 && msg.eventOrdinal < events.length) {
                        ChaosServerManager.triggerEventForPlayer(player, events[msg.eventOrdinal]);
                    }
                }
                case ACTION_CAPTCHA_SOLVED -> ChaosServerManager.solveCaptcha(player);
                case ACTION_TRIGGER_RANDOM_QUEST -> ChaosServerManager.triggerRandomQuestForPlayer(player);
                case ACTION_CLEAR_ACTIVE_EVENTS -> ChaosServerManager.clearEventsForPlayer(player);
                case ACTION_PLAYER_JUMPED -> ChaosServerManager.onPlayerJumped(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
