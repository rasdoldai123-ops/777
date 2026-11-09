package com.example.chaosmod.quest;

public enum ChaosQuest {
    TOUCH_GRASS("touch_grass", "Потрогай траву",
            "Встаньте на блок обычного дерна под открытым небом.", 1),
    SACRIFICE_TO_RNG("sacrifice_to_rng", "Жертва богам рандома",
            "Сожгите в огне, лаве или об кактус любой железный, золотой или алмазный предмет.", 1),
    AGGRESSIVE_VIEWER("aggressive_viewer", "Агрессивный зритель",
            "Удерживайте прицел прямо на враждебном мобе суммарно 10 сек (дистанция до 5 блоков).", 200),
    LEAP_OF_FAITH("leap_of_faith", "Прыжок веры",
            "Получите за один раз от 3 до 6 сердечек (6-12 ХП) урона от падения и выживите.", 1),
    DIOGENES("diogenes", "Диоген",
            "Замуруйте себя в глухую коробку 1х2 и простойте в полной темноте без движения 15 сек.", 300),
    TASTER("taster", "Дегустатор",
            "Съешьте 3 разных вида еды за отведенное время.", 3),
    EXTREME_ALPINISM("extreme_alpinism", "Экстремальный альпинизм / Спелеология",
            "Поднимитесь выше Y=110 (если вы ниже Y=65) или спуститесь ниже Y=30 (если вы выше).", 1),
    PACIFIST_UNDER_PRESSURE("pacifist_under_pressure", "Пацифист под прессингом",
            "Не нанесите ни единицы урона ни одному существу в течение 60 сек (вокруг 3 чешуйницы).", 1200),
    WATER_PROCEDURES("water_procedures", "Водные процедуры",
            "Проведите полностью под водой суммарно 20 секунд (пузырьки воздуха должны тратиться).", 400),
    INTERIOR_DESIGNER("interior_designer", "Дизайнер интерьера",
            "Скрафтите и поставьте рядом с собой кровать любого цвета, кроме белого.", 1),
    SNIPER("sniper", "Снайпер",
            "Попадите снежком, яйцом или стрелой в любого моба с дистанции минимум 15 блоков.", 1),
    PYROMANIAC("pyromaniac", "Пироман",
            "Подожгите 3 любых живых существа (огнивом, лавой или горящей стрелой).", 3);

    private final String id;
    private final String title;
    private final String description;
    private final int maxProgress;

    ChaosQuest(String id, String title, String description, int maxProgress) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.maxProgress = maxProgress;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getMaxProgress() {
        return maxProgress;
    }
}
