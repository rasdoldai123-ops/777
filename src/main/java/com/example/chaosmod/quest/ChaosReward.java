package com.example.chaosmod.quest;

public enum ChaosReward {
    ROYAL_TREASURE("Королевская казна", "Вы получили 3 алмаза, 2 золотых яблока и 8 железных слитков!"),
    GUARDIAN_TOTEM("Дар бессмертия", "Вы получили Тотем бессмертия и 4 эндер-жемчуга!"),
    HERO_BLESSING("Благословение героя", "Регенерация II, Сила II, Скорость II и Огнестойкость на 90 секунд + 30 уровней опыта!"),
    NETHERITE_BONUS("Подгон кузнеца", "Вы получили Незеритовый слиток, 16 золотых морковок и 16 запеченного картофеля!"),
    FATE_GIFT("Подарок судьбы", "Запущено бонусное событие из категории «Подачки судьбы» + сундук припасов!");

    private final String title;
    private final String description;

    ChaosReward(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
