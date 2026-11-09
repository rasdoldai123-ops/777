package com.example.chaosmod.client;

import com.example.chaosmod.event.ChaosEvent;
import com.example.chaosmod.network.S2CSyncStatePacket;
import com.example.chaosmod.quest.ChaosQuest;

import java.util.EnumMap;
import java.util.Map;

public class ClientChaosState {
    public static int nextEventTicks = 60 * 20;
    public static int nextQuestTicks = 300 * 20;

    public static final Map<ChaosEvent, Integer> activeEvents = new EnumMap<>(ChaosEvent.class);

    public static ChaosQuest activeQuest = null;
    public static int questRemainingTicks = 0;
    public static int questTotalTicks = 1;
    public static int questProgress = 0;
    public static int questMaxProgress = 1;

    public static float playerScale = 1.0f;

    // DVD Screensaver state
    public static float dvdX = 40.0f;
    public static float dvdY = 40.0f;
    public static float dvdVx = 2.6f;
    public static float dvdVy = 2.1f;
    public static int dvdColor = 0xFF55FF55;

    // Phantom pain flash ticks
    public static int phantomPainFlashTicks = 0;
    public static int phantomPainCounter = 0;

    // Disco 80s filter state
    public static int discoColor = 0x44FF00FF;
    public static float crosshairAngle = 0.0f;

    // Drunken master WASD permutation [0=W, 1=S, 2=A, 3=D]
    public static int[] drunkenPermutation = new int[]{0, 1, 2, 3};
    public static boolean wasDrunkenActive = false;

    // Captcha opened flag
    public static boolean captchaScreenOpened = false;

    public static void updateFromPacket(S2CSyncStatePacket msg) {
        nextEventTicks = msg.nextEventTicks;
        nextQuestTicks = msg.nextQuestTicks;

        activeEvents.clear();
        ChaosEvent[] allEvents = ChaosEvent.values();
        for (Map.Entry<Integer, Integer> e : msg.activeEvents.entrySet()) {
            int ord = e.getKey();
            if (ord >= 0 && ord < allEvents.length) {
                activeEvents.put(allEvents[ord], e.getValue());
            }
        }

        ChaosQuest[] allQuests = ChaosQuest.values();
        if (msg.activeQuestOrdinal >= 0 && msg.activeQuestOrdinal < allQuests.length) {
            activeQuest = allQuests[msg.activeQuestOrdinal];
        } else {
            activeQuest = null;
        }
        questRemainingTicks = msg.questRemainingTicks;
        questTotalTicks = Math.max(1, msg.questTotalTicks);
        questProgress = msg.questProgress;
        questMaxProgress = Math.max(1, msg.questMaxProgress);
        playerScale = msg.playerScale;
    }

    public static boolean isEventActive(ChaosEvent event) {
        return activeEvents.getOrDefault(event, 0) > 0;
    }
}
