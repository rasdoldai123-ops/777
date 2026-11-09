package com.example.chaosmod.event;

public enum EventCategory {
    CLIENT("Кат. 1: Насилие над клиентом и камерой", 0xFF5555),
    PHYSICS("Кат. 2: Инвалидность управления и физики", 0xFFAA00),
    MOBS("Кат. 3: Социальная инженерия и бунт мобов", 0xAA00AA),
    ECONOMY("Кат. 4: Экономический кризис и блоки", 0xFFFF55),
    BLESSINGS("Кат. 5: Подачки судьбы", 0x55FF55);

    private final String title;
    private final int color;

    EventCategory(String title, int color) {
        this.title = title;
        this.color = color;
    }

    public String getTitle() {
        return title;
    }

    public int getColor() {
        return color;
    }
}
