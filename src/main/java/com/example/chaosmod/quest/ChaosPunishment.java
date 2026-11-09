package com.example.chaosmod.quest;

public enum ChaosPunishment {
    CONFISCATION("Конфискация имущества", "Удалена вся надетая броня (или самый ценный предмет из хотбара)!"),
    SOLITARY_CELL("Карцер", "Вокруг вас сгенерирована обсидиановая клетка 3х3 с водой и рыбой-фугу!"),
    SPACE_PROGRAM("Космическая программа", "Вы запущены на 150 блоков вертикально вверх! Ведро воды в комплект не входит."),
    CHRONIC_FATIGUE("Хроническая усталость", "Максимальное здоровье урезано до 3 сердечек (6 ХП) до выполнения следующего квеста!"),
    PUNITIVE_SQUAD("Карательный отряд", "Вокруг появились 4 поборника «Коллектор» с топорами!");

    private final String title;
    private final String description;

    ChaosPunishment(String title, String description) {
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
