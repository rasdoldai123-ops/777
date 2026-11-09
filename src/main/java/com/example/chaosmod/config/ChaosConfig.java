package com.example.chaosmod.config;

import com.example.chaosmod.event.ChaosEvent;

import java.util.EnumMap;
import java.util.Map;

public class ChaosConfig {
    public enum ModMode {
        ENABLED("Вкл"),
        PAUSED("Пауза таймеров"),
        DISABLED("Выкл");

        private final String label;

        ModMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public ModMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }

    public enum HudCorner {
        TOP_LEFT("Верхний левый угол"),
        TOP_RIGHT("Верхний правый угол"),
        BOTTOM_LEFT("Нижний левый угол"),
        BOTTOM_RIGHT("Нижний правый угол");

        private final String label;

        HudCorner(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public HudCorner next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }

    // Server-synced settings
    public static ModMode modMode = ModMode.ENABLED;
    public static int eventIntervalSeconds = 60;       // 10 to 300 sec
    public static int questIntervalSeconds = 300;      // 60 to 900 sec
    public static int questDurationSeconds = 90;       // 30 to 300 sec

    private static final Map<ChaosEvent, Boolean> ENABLED_EVENTS = new EnumMap<>(ChaosEvent.class);

    // Client-local HUD settings
    public static HudCorner hudCorner = HudCorner.TOP_LEFT;
    public static int hudCustomX = -1;
    public static int hudCustomY = -1;
    public static boolean soundTicking = true;

    static {
        for (ChaosEvent event : ChaosEvent.values()) {
            ENABLED_EVENTS.put(event, true);
        }
    }

    public static boolean isEventEnabled(ChaosEvent event) {
        return ENABLED_EVENTS.getOrDefault(event, true);
    }

    public static void setEventEnabled(ChaosEvent event, boolean enabled) {
        ENABLED_EVENTS.put(event, enabled);
    }

    public static Map<ChaosEvent, Boolean> getEnabledEventsMap() {
        return ENABLED_EVENTS;
    }

    public static long getEnabledBitmask() {
        long mask = 0L;
        for (ChaosEvent event : ChaosEvent.values()) {
            if (isEventEnabled(event)) {
                mask |= (1L << event.ordinal());
            }
        }
        return mask;
    }

    public static void applyEnabledBitmask(long mask) {
        for (ChaosEvent event : ChaosEvent.values()) {
            boolean enabled = (mask & (1L << event.ordinal())) != 0;
            ENABLED_EVENTS.put(event, enabled);
        }
    }
}
