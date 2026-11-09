package com.example.chaosmod.network;

import com.example.chaosmod.client.ClientChaosState;
import com.example.chaosmod.config.ChaosConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class S2CSyncStatePacket {
    public final int modModeOrdinal;
    public final int eventIntervalSec;
    public final int questIntervalSec;
    public final int questDurationSec;
    public final long enabledEventsBitmask;

    public final int nextEventTicks;
    public final int nextQuestTicks;

    // Active events for this player: ordinal -> remainingTicks
    public final Map<Integer, Integer> activeEvents;

    // Active quest for this player (-1 if none)
    public final int activeQuestOrdinal;
    public final int questRemainingTicks;
    public final int questTotalTicks;
    public final int questProgress;
    public final int questMaxProgress;

    // Scale modifier for hitbox (1.0f normal, 2.0f claustrophobia, 0.25f lilliputian)
    public final float playerScale;

    public S2CSyncStatePacket(
            int modModeOrdinal,
            int eventIntervalSec,
            int questIntervalSec,
            int questDurationSec,
            long enabledEventsBitmask,
            int nextEventTicks,
            int nextQuestTicks,
            Map<Integer, Integer> activeEvents,
            int activeQuestOrdinal,
            int questRemainingTicks,
            int questTotalTicks,
            int questProgress,
            int questMaxProgress,
            float playerScale
    ) {
        this.modModeOrdinal = modModeOrdinal;
        this.eventIntervalSec = eventIntervalSec;
        this.questIntervalSec = questIntervalSec;
        this.questDurationSec = questDurationSec;
        this.enabledEventsBitmask = enabledEventsBitmask;
        this.nextEventTicks = nextEventTicks;
        this.nextQuestTicks = nextQuestTicks;
        this.activeEvents = activeEvents;
        this.activeQuestOrdinal = activeQuestOrdinal;
        this.questRemainingTicks = questRemainingTicks;
        this.questTotalTicks = questTotalTicks;
        this.questProgress = questProgress;
        this.questMaxProgress = questMaxProgress;
        this.playerScale = playerScale;
    }

    public static void encode(S2CSyncStatePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.modModeOrdinal);
        buf.writeInt(msg.eventIntervalSec);
        buf.writeInt(msg.questIntervalSec);
        buf.writeInt(msg.questDurationSec);
        buf.writeLong(msg.enabledEventsBitmask);
        buf.writeInt(msg.nextEventTicks);
        buf.writeInt(msg.nextQuestTicks);

        buf.writeInt(msg.activeEvents.size());
        for (Map.Entry<Integer, Integer> entry : msg.activeEvents.entrySet()) {
            buf.writeInt(entry.getKey());
            buf.writeInt(entry.getValue());
        }

        buf.writeInt(msg.activeQuestOrdinal);
        buf.writeInt(msg.questRemainingTicks);
        buf.writeInt(msg.questTotalTicks);
        buf.writeInt(msg.questProgress);
        buf.writeInt(msg.questMaxProgress);
        buf.writeFloat(msg.playerScale);
    }

    public static S2CSyncStatePacket decode(FriendlyByteBuf buf) {
        int modModeOrdinal = buf.readInt();
        int eventIntervalSec = buf.readInt();
        int questIntervalSec = buf.readInt();
        int questDurationSec = buf.readInt();
        long enabledEventsBitmask = buf.readLong();
        int nextEventTicks = buf.readInt();
        int nextQuestTicks = buf.readInt();

        int size = buf.readInt();
        Map<Integer, Integer> activeEvents = new HashMap<>();
        for (int i = 0; i < size; i++) {
            activeEvents.put(buf.readInt(), buf.readInt());
        }

        int activeQuestOrdinal = buf.readInt();
        int questRemainingTicks = buf.readInt();
        int questTotalTicks = buf.readInt();
        int questProgress = buf.readInt();
        int questMaxProgress = buf.readInt();
        float playerScale = buf.readFloat();

        return new S2CSyncStatePacket(
                modModeOrdinal, eventIntervalSec, questIntervalSec, questDurationSec,
                enabledEventsBitmask, nextEventTicks, nextQuestTicks, activeEvents,
                activeQuestOrdinal, questRemainingTicks, questTotalTicks, questProgress,
                questMaxProgress, playerScale
        );
    }

    public static void handle(S2CSyncStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ChaosConfig.ModMode[] modes = ChaosConfig.ModMode.values();
            if (msg.modModeOrdinal >= 0 && msg.modModeOrdinal < modes.length) {
                ChaosConfig.modMode = modes[msg.modModeOrdinal];
            }
            ChaosConfig.eventIntervalSeconds = msg.eventIntervalSec;
            ChaosConfig.questIntervalSeconds = msg.questIntervalSec;
            ChaosConfig.questDurationSeconds = msg.questDurationSec;
            ChaosConfig.applyEnabledBitmask(msg.enabledEventsBitmask);

            ClientChaosState.updateFromPacket(msg);
        });
        ctx.get().setPacketHandled(true);
    }
}
