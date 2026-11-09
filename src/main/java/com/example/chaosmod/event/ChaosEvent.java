package com.example.chaosmod.event;

public enum ChaosEvent {
    // Категория 1: Насилие над клиентом, камерой и интерфейсом (12)
    DVD_SCREENSAVER("dvd_screensaver", "DVD-скринсейвер",
            "Хотбар летает по монитору, отскакивая от краев и меняя цвет в углах.",
            EventCategory.CLIENT, 60),
    AUSTRALIAN_GAMING("australian_gaming", "Австралийский гейминг",
            "Камера переворачивается ровно на 180 градусов вверх ногами на 45 сек.",
            EventCategory.CLIENT, 45),
    SURVIVAL_CAPTCHA("survival_captcha", "Капча на выживание",
            "Всплывает окно: нажмите на 3 блока земли. Пока не прокликаешь — персонаж заморожен.",
            EventCategory.CLIENT, 45),
    FAKE_CRASH("fake_crash", "Фальшивый краш",
            "Игра на 4 секунды визуально «зависает» с экраном Saving World...",
            EventCategory.CLIENT, 4),
    SOUND_SCHIZOPHRENIA("sound_schizophrenia", "Шизофазия звука",
            "Шаги звучат как шипение крипера, а поедание еды — как вопли гаста.",
            EventCategory.CLIENT, 60),
    NARCISSISM("narcissism", "Нарциссизм",
            "Камера намертво блокируется в режиме от третьего лица спереди.",
            EventCategory.CLIENT, 60),
    ROSKOMNADZOR("roskomnadzor", "Роскомнадзор",
            "На всех мобов в радиусе видимости накладываются черные прямоугольники цензуры.",
            EventCategory.CLIENT, 60),
    PHANTOM_PAIN("phantom_pain", "Фантомная боль",
            "Каждые 6 секунд проигрывается звук урона, экран трясется и краснеет без потери ХП.",
            EventCategory.CLIENT, 60),
    GRANDPA_MYOPIA("grandpa_myopia", "Близорукость деда",
            "Дальность прорисовки режется до 1.5 чанков и заливается густым белым туманом.",
            EventCategory.CLIENT, 60),
    SNIPER_SCOPE("sniper_scope", "Снайперский прицел",
            "Поле зрения (FOV) падает до 15 градусов, а чувствительность мыши вырастает в 3 раза.",
            EventCategory.CLIENT, 60),
    BLIND_SPOT("blind_spot", "Слепая зона",
            "Весь интерфейс (ХП, голод, хотбар, прицел) полностью исчезает на минуту.",
            EventCategory.CLIENT, 60),
    DISCO_80S("disco_80s", "Дискотека 80-х",
            "Каждую секунду на экран накладывается случайный цветовой фильтр, прицел вращается.",
            EventCategory.CLIENT, 60),

    // Категория 2: Инвалидность управления и физики (12)
    DRUNKEN_MASTER("drunken_master", "Пьяный мастер",
            "Клавиши W, A, S, D меняются местами в случайном порядке на 45 секунд.",
            EventCategory.PHYSICS, 45),
    COW_ICE("cow_ice", "Коровий лед",
            "Трение абсолютно всех блоков падает до уровня синего льда.",
            EventCategory.PHYSICS, 60),
    BRAKES_FAILED("brakes_failed", "Отказали тормоза",
            "Персонаж непрерывно бежит вперед (принудительный спринт).",
            EventCategory.PHYSICS, 60),
    CLAUSTROPHOBIA("claustrophobia", "Клаустрофобия",
            "Физический хитбокс и рост увеличиваются в 2 раза (до размеров железного голема).",
            EventCategory.PHYSICS, 60),
    LILLIPUTIAN("lilliputian", "Лилипут",
            "Рост уменьшается до 0.4 блока. Запрыгнуть на блок без ступенек невозможно.",
            EventCategory.PHYSICS, 60),
    JUPITER_GRAVITY("jupiter_gravity", "Гравитация Юпитера",
            "Прыжки отключены, а падение даже с 2 блоков наносит урон.",
            EventCategory.PHYSICS, 60),
    ROSTELECOM_SIM("rostelecom_sim", "Симулятор Ростелекома (Пинг 999)",
            "Каждые 8 секунд тебя откатывает на координату, где ты стоял 2 секунды назад.",
            EventCategory.PHYSICS, 60),
    FLOOR_IS_CACTUS("floor_is_cactus", "Пол — это кактус",
            "Если стоять на одном блоке без движения дольше 1.5 сек, получаешь по 0.5 урона.",
            EventCategory.PHYSICS, 60),
    ACROPHOBIA("acrophobia", "Акрофобия",
            "Как только под ногами оказывается воздух, камера резко дергается строго вниз.",
            EventCategory.PHYSICS, 60),
    BUTTERFINGERS("butterfingers", "Дырявые руки",
            "При каждом третьем прыжке автоматически выбрасывается предмет из руки.",
            EventCategory.PHYSICS, 60),
    SPRING_JUMP("spring_jump", "Пружина",
            "Любой прыжок подбрасывает на 12 блоков вверх без отмены урона от падения.",
            EventCategory.PHYSICS, 60),
    KNOCKBACK_100("knockback_100", "Отдача 100-го уровня",
            "Любой полученный или нанесенный удар отбрасывает цель по горизонтали на 25 блоков.",
            EventCategory.PHYSICS, 60),

    // Категория 3: Социальная инженерия и бунт мобов (12)
    VEGAN_UPRISING("vegan_uprising", "Восстание веганов",
            "Мирные мобы в радиусе 30 блоков бегут забивать тебя толпой.",
            EventCategory.MOBS, 60),
    WEEPING_ANGEL("weeping_angel", "Плачущий ангел",
            "За спиной спавнится бессмертная стойка для брони с тыквой. Двигается, когда не смотришь.",
            EventCategory.MOBS, 60),
    PRANKSTER_CREEPER("prankster_creeper", "Крипер-пранкер",
            "Криперы вокруг взрываются конфетти с фейерверками без урона блокам, подкидывая в воздух.",
            EventCategory.MOBS, 60),
    CASTLING("castling", "Рокировка",
            "Мгновенный обмен координатами и взглядом с самым дальним мобом в радиусе 40 блоков.",
            EventCategory.MOBS, 5),
    CHICKEN_BOMBER("chicken_bomber", "Куриный бомбардировщик",
            "Над головой спавнится 10 куриц, каждые 3 секунды сбрасывающих зажженный ТНТ.",
            EventCategory.MOBS, 30),
    BAILIFF("bailiff", "Судебный пристав",
            "Спавнится быстрый зомби-житель, ворующий предметы при ударе. Убей его, чтобы вернуть вещи!",
            EventCategory.MOBS, 60),
    REVERSE_PSYCHOLOGY("reverse_psychology", "Обратная психология",
            "Оружие лечит мобов. Урон наносится только пустой рукой или брошенными через Q предметами.",
            EventCategory.MOBS, 60),
    BUILDER_ZOMBIES("builder_zombies", "Зомби-таджики",
            "Ближайшие зомби получают булыжник и строят столбы под собой или замуровывают тебя.",
            EventCategory.MOBS, 60),
    BLACK_HOLE("black_hole", "Черная дыра",
            "Все мобы и предметы в радиусе 25 блоков медленно притягиваются к тебе по воздуху.",
            EventCategory.MOBS, 60),
    COSMONAUT_SKELETONS("cosmonaut_skeletons", "Скелеты-космонавты",
            "Рядом спавнятся 2 скелета, стрелы которых накладывают Левитацию V на 4 секунды.",
            EventCategory.MOBS, 60),
    DISGUISE("disguise", "Маскировка",
            "Все мобы вокруг визуально отображаются как свиньи, сохраняя свои реальные звуки и атаки.",
            EventCategory.MOBS, 60),
    CHOIR_OF_CONDEMNATION("choir_of_condemnation", "Хор осуждения",
            "Мобы вокруг замирают, смотрят на тебя и синхронно кивают, ускоряя трату голода в 5 раз.",
            EventCategory.MOBS, 60),

    // Категория 4: Экономический кризис и блоки (12)
    SHELL_GAME("shell_game", "Наперсточник",
            "Все предметы в инвентаре, хотбаре и броне мгновенно перемешиваются.",
            EventCategory.ECONOMY, 5),
    HOT_POTATO("hot_potato", "Горячая картошка",
            "В хотбар падает пылающий клубень. Не выкинешь за 4 сек — взрыв на 3 сердечка (3 цикла).",
            EventCategory.ECONOMY, 30),
    GRAVEL_CURSE("gravel_curse", "Гравийное проклятие",
            "Любой твердый блок под ногами через 1 секунду превращается в песок или гравий.",
            EventCategory.ECONOMY, 60),
    SLUM_MIDAS("slum_midas", "Мидас из трущоб",
            "В течение минуты любой вскопанный блок дропает гнилую плоть вместо обычного ресурса.",
            EventCategory.ECONOMY, 60),
    SANCTIONS("sanctions", "Санкции",
            "6 из 9 слотов хотбара временно блокируются барьерами на минуту.",
            EventCategory.ECONOMY, 60),
    FURNITURE_REBELLION("furniture_rebellion", "Бунт мебели",
            "Сундуки, печки и верстаки при клике ПКМ подпрыгивают и убегают от тебя на 2 блока.",
            EventCategory.ECONOMY, 60),
    GLASS_WORLD("glass_world", "Стеклянный мир",
            "Камень, глубинный сланец и земля в радиусе 6 блоков становятся прозрачным стеклом.",
            EventCategory.ECONOMY, 45),
    EXPIRED_FOOD("expired_food", "Просрочка",
            "Вся еда в инвентаре на 60 секунд превращается в паучьи глаза и ядовитый картофель.",
            EventCategory.ECONOMY, 60),
    CRAFT_RANDOMIZER("craft_randomizer", "Рандомайзер крафта",
            "Любой скрафченный предмет превращается в случайный предмет из ванильного Майнкрафта.",
            EventCategory.ECONOMY, 60),
    OIL_SPILL("oil_spill", "Нефтяной разлив",
            "Блоки под ногами покрываются черной слизью, замедляющей любого наступившего на 60%.",
            EventCategory.ECONOMY, 60),
    MINEFIELD("minefield", "Минное поле",
            "При ломании блока с шансом 15% вместо дропа вылетает зажженный ТНТ с таймером 1.5 сек.",
            EventCategory.ECONOMY, 60),
    AIR_TAX("air_tax", "Налог на воздух",
            "За каждые 10 прыжков из инвентаря удаляется 1 булыжник, доска или факел.",
            EventCategory.ECONOMY, 60),

    // Категория 5: Подачки судьбы (12)
    IMMORTALITY_CREDIT("immortality_credit", "Бессмертие в кредит",
            "Полная неуязвимость на 40 секунд, но по истечении таймера остается ровно 1 сердечко.",
            EventCategory.BLESSINGS, 40),
    ALIEXPRESS_DIAMONDS("aliexpress_diamonds", "Алмазный дождь с Алиэкспресс",
            "С неба падает 25 алмазов: 22 фальшивки (исчезают со звуком поломки) и 3 настоящих.",
            EventCategory.BLESSINGS, 20),
    LASER_GAZE("laser_gaze", "Лазерный взгляд",
            "Взгляд на блок дольше 1.5 сек (до 10 блоков) автоматически ломает его с выпадением дропа.",
            EventCategory.BLESSINGS, 60),
    HUMANITARIAN_AID("humanitarian_aid", "Гуманитарная помощь",
            "С неба с курицей спускается сундук с полезным лутом (железо, еда, жемчуг, ведро воды).",
            EventCategory.BLESSINGS, 15),
    HUMAN_DRILL("human_drill", "Человек-бур",
            "В течение 30 секунд камень и земля перед лицом мгновенно ломаются сами при ходьбе.",
            EventCategory.BLESSINGS, 30),
    PMC_GOLEM("pmc_golem", "ЧВК «Голем»",
            "Рядом спавнятся 2 железных голема со Скоростью II, охраняющие тебя 60 секунд.",
            EventCategory.BLESSINGS, 60),
    POCKET_BLACKSMITH("pocket_blacksmith", "Кузнец в кармане",
            "Добытая руда сразу выпадает слитками в двойном размере, а с животных падает жареное мясо.",
            EventCategory.BLESSINGS, 60),
    CRYO_STEP("cryo_step", "Крио-шаг",
            "Вода под ногами превращается в лед, а лава — в гладкий базальт.",
            EventCategory.BLESSINGS, 60),
    LEGAL_XRAY("legal_xray", "Легальный X-Ray",
            "Все руды (железо, золото, алмазы) в радиусе 20 блоков подсвечиваются цветными контурами.",
            EventCategory.BLESSINGS, 60),
    VACUUM_CLEANER("vacuum_cleaner", "Пылесос",
            "Весь дроп и сферы опыта в радиусе 60 блоков телепортируются прямо к тебе.",
            EventCategory.BLESSINGS, 15),
    SECOND_WIND("second_wind", "Второе дыхание",
            "Полное исцеление, снятие негативных эффектов, максимум сытости и +6 золотых сердец.",
            EventCategory.BLESSINGS, 10),
    CREATIVE_FLIGHT_DEMO("creative_flight_demo", "Креативный полет (Демо-версия)",
            "Полноценный креативный полет ровно на 15 секунд. Успей приземлиться!",
            EventCategory.BLESSINGS, 15);

    private final String id;
    private final String displayName;
    private final String description;
    private final EventCategory category;
    private final int defaultDurationSeconds;

    ChaosEvent(String id, String displayName, String description, EventCategory category, int defaultDurationSeconds) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.defaultDurationSeconds = defaultDurationSeconds;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public EventCategory getCategory() {
        return category;
    }

    public int getDefaultDurationSeconds() {
        return defaultDurationSeconds;
    }

    public int getDefaultDurationTicks() {
        return defaultDurationSeconds * 20;
    }

    public static ChaosEvent byId(String id) {
        for (ChaosEvent event : values()) {
            if (event.id.equalsIgnoreCase(id)) {
                return event;
            }
        }
        return null;
    }
}
