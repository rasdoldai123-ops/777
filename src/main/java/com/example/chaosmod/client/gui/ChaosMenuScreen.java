package com.example.chaosmod.client.gui;

import com.example.chaosmod.client.ClientChaosState;
import com.example.chaosmod.config.ChaosConfig;
import com.example.chaosmod.event.ChaosEvent;
import com.example.chaosmod.event.EventCategory;
import com.example.chaosmod.network.C2STriggerActionPacket;
import com.example.chaosmod.network.C2SUpdateSettingsPacket;
import com.example.chaosmod.network.ChaosNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChaosMenuScreen extends Screen {
    private static final int VISIBLE_ROWS = 6;
    private static final int ROW_HEIGHT = 24;

    private int scrollOffset = 0;
    private EventCategory selectedCategoryFilter = null; // null = All 60 events
    private boolean draggingHudPreview = false;

    private final List<ChaosEvent> filteredEvents = new ArrayList<>();
    private final List<Button> rowToggleButtons = new ArrayList<>();
    private final List<Button> rowRunButtons = new ArrayList<>();

    public ChaosMenuScreen() {
        super(Component.literal("Настройки Хаос-Мода (60 Событий и Квесты)"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildFilteredEvents();
        buildWidgets();
    }

    private void rebuildFilteredEvents() {
        filteredEvents.clear();
        for (ChaosEvent ev : ChaosEvent.values()) {
            if (selectedCategoryFilter == null || ev.getCategory() == selectedCategoryFilter) {
                filteredEvents.add(ev);
            }
        }
        int maxScroll = Math.max(0, filteredEvents.size() - VISIBLE_ROWS);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
    }

    private void buildWidgets() {
        this.clearWidgets();
        rowToggleButtons.clear();
        rowRunButtons.clear();

        int centerX = this.width / 2;
        int topY = 24;

        // Row 1: Mod Mode toggle, HUD corner toggle, Sound Ticking toggle
        this.addRenderableWidget(Button.builder(getModModeText(), btn -> {
            ChaosConfig.modMode = ChaosConfig.modMode.next();
            btn.setMessage(getModModeText());
            sendConfigToServer();
        }).bounds(centerX - 210, topY, 135, 20).build());

        this.addRenderableWidget(Button.builder(getHudCornerText(), btn -> {
            ChaosConfig.hudCorner = ChaosConfig.hudCorner.next();
            ChaosConfig.hudCustomX = -1;
            ChaosConfig.hudCustomY = -1;
            btn.setMessage(getHudCornerText());
        }).bounds(centerX - 70, topY, 145, 20).build());

        this.addRenderableWidget(Button.builder(getSoundTickText(), btn -> {
            ChaosConfig.soundTicking = !ChaosConfig.soundTicking;
            btn.setMessage(getSoundTickText());
        }).bounds(centerX + 80, topY, 130, 20).build());

        // Row 2: Three Time Sliders
        int sliderY = topY + 24;
        // Event Interval Slider (10 sec to 300 sec)
        this.addRenderableWidget(new IntSlider(
                centerX - 210, sliderY, 135, 20,
                "Интервал событий: ", " сек",
                10, 300, ChaosConfig.eventIntervalSeconds,
                val -> {
                    ChaosConfig.eventIntervalSeconds = val;
                    sendConfigToServer();
                }
        ));

        // Quest Interval Slider (60 sec to 900 sec)
        this.addRenderableWidget(new IntSlider(
                centerX - 70, sliderY, 145, 20,
                "Интервал квестов: ", " сек",
                60, 900, ChaosConfig.questIntervalSeconds,
                val -> {
                    ChaosConfig.questIntervalSeconds = val;
                    sendConfigToServer();
                }
        ));

        // Quest Duration Slider (30 sec to 300 sec)
        this.addRenderableWidget(new IntSlider(
                centerX + 80, sliderY, 130, 20,
                "Время на квест: ", " сек",
                30, 300, ChaosConfig.questDurationSeconds,
                val -> {
                    ChaosConfig.questDurationSeconds = val;
                    sendConfigToServer();
                }
        ));

        // Row 3: Category Filter Tabs + Quick Actions
        int tabY = sliderY + 24;
        this.addRenderableWidget(Button.builder(Component.literal(selectedCategoryFilter == null ? "[Все 60]" : "Все 60"), btn -> {
            selectedCategoryFilter = null;
            scrollOffset = 0;
            rebuildFilteredEvents();
            buildWidgets();
        }).bounds(centerX - 210, tabY, 55, 18).build());

        EventCategory[] cats = EventCategory.values();
        for (int i = 0; i < cats.length; i++) {
            final EventCategory cat = cats[i];
            boolean sel = selectedCategoryFilter == cat;
            String label = (sel ? "[Кат." + (i + 1) + "]" : "Кат." + (i + 1));
            this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                selectedCategoryFilter = cat;
                scrollOffset = 0;
                rebuildFilteredEvents();
                buildWidgets();
            }).bounds(centerX - 152 + i * 46, tabY, 44, 18).build());
        }

        // Quick action: Trigger Random Quest & Clear Active Events
        this.addRenderableWidget(Button.builder(Component.literal("🎲 Квест сейчас").withStyle(ChatFormatting.AQUA), btn -> {
            ChaosNetwork.sendToServer(new C2STriggerActionPacket(C2STriggerActionPacket.ACTION_TRIGGER_RANDOM_QUEST, 0));
        }).bounds(centerX + 80, tabY, 82, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("✖ Сброс").withStyle(ChatFormatting.RED), btn -> {
            ChaosNetwork.sendToServer(new C2STriggerActionPacket(C2STriggerActionPacket.ACTION_CLEAR_ACTIVE_EVENTS, 0));
        }).bounds(centerX + 164, tabY, 46, 18).build());

        // Event Manager List Rows (6 visible rows)
        int listTopY = tabY + 22;
        for (int r = 0; r < VISIBLE_ROWS; r++) {
            final int rowIndex = r;
            int ry = listTopY + r * ROW_HEIGHT;

            // Checkbox button [✔] / [✖]
            Button toggleBtn = Button.builder(Component.literal("✔"), btn -> {
                int idx = scrollOffset + rowIndex;
                if (idx >= 0 && idx < filteredEvents.size()) {
                    ChaosEvent ev = filteredEvents.get(idx);
                    boolean nextState = !ChaosConfig.isEventEnabled(ev);
                    ChaosConfig.setEventEnabled(ev, nextState);
                    updateRowButtons();
                    sendConfigToServer();
                }
            }).bounds(centerX - 206, ry + 2, 24, 20).build();

            // "Запустить сейчас" button
            Button runBtn = Button.builder(Component.literal("▶ Запустить сейчас").withStyle(ChatFormatting.GREEN), btn -> {
                int idx = scrollOffset + rowIndex;
                if (idx >= 0 && idx < filteredEvents.size()) {
                    ChaosEvent ev = filteredEvents.get(idx);
                    ChaosNetwork.sendToServer(new C2STriggerActionPacket(C2STriggerActionPacket.ACTION_TRIGGER_EVENT, ev.ordinal()));
                }
            }).bounds(centerX + 92, ry + 2, 114, 20).build();

            rowToggleButtons.add(toggleBtn);
            rowRunButtons.add(runBtn);
            this.addRenderableWidget(toggleBtn);
            this.addRenderableWidget(runBtn);
        }

        // Scroll Up / Down & Enable All / Disable All buttons
        int bottomY = this.height - 24;
        this.addRenderableWidget(Button.builder(Component.literal("▲ Вверх"), btn -> {
            scroll(-1);
        }).bounds(centerX - 210, bottomY, 65, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("▼ Вниз"), btn -> {
            scroll(1);
        }).bounds(centerX - 142, bottomY, 65, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Включить все"), btn -> {
            for (ChaosEvent ev : ChaosEvent.values()) ChaosConfig.setEventEnabled(ev, true);
            updateRowButtons();
            sendConfigToServer();
        }).bounds(centerX - 72, bottomY, 90, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Выключить все"), btn -> {
            for (ChaosEvent ev : ChaosEvent.values()) ChaosConfig.setEventEnabled(ev, false);
            updateRowButtons();
            sendConfigToServer();
        }).bounds(centerX + 22, bottomY, 90, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Готово"), btn -> this.onClose())
                .bounds(centerX + 120, bottomY, 90, 20).build());

        updateRowButtons();
    }

    private void scroll(int delta) {
        int maxScroll = Math.max(0, filteredEvents.size() - VISIBLE_ROWS);
        scrollOffset = Mth.clamp(scrollOffset + delta, 0, maxScroll);
        updateRowButtons();
    }

    private void updateRowButtons() {
        for (int r = 0; r < VISIBLE_ROWS; r++) {
            int idx = scrollOffset + r;
            Button toggleBtn = rowToggleButtons.get(r);
            Button runBtn = rowRunButtons.get(r);
            if (idx < filteredEvents.size()) {
                ChaosEvent ev = filteredEvents.get(idx);
                boolean enabled = ChaosConfig.isEventEnabled(ev);
                toggleBtn.visible = true;
                runBtn.visible = true;
                toggleBtn.setMessage(Component.literal(enabled ? "✔" : "✖")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
            } else {
                toggleBtn.visible = false;
                runBtn.visible = false;
            }
        }
    }

    private Component getModModeText() {
        ChatFormatting color = switch (ChaosConfig.modMode) {
            case ENABLED -> ChatFormatting.GREEN;
            case PAUSED -> ChatFormatting.YELLOW;
            case DISABLED -> ChatFormatting.RED;
        };
        return Component.literal("Режим: ").append(Component.literal(ChaosConfig.modMode.getLabel()).withStyle(color, ChatFormatting.BOLD));
    }

    private Component getHudCornerText() {
        if (ChaosConfig.hudCustomX >= 0) {
            return Component.literal("HUD: Своя поз. (" + ChaosConfig.hudCustomX + "," + ChaosConfig.hudCustomY + ")");
        }
        return Component.literal("HUD: " + ChaosConfig.hudCorner.getLabel());
    }

    private Component getSoundTickText() {
        return Component.literal("Тиканье (5с): ")
                .append(Component.literal(ChaosConfig.soundTicking ? "Вкл" : "Выкл")
                        .withStyle(ChaosConfig.soundTicking ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private void sendConfigToServer() {
        ChaosNetwork.sendToServer(new C2SUpdateSettingsPacket(
                ChaosConfig.modMode.ordinal(),
                ChaosConfig.eventIntervalSeconds,
                ChaosConfig.questIntervalSeconds,
                ChaosConfig.questDurationSeconds,
                ChaosConfig.getEnabledBitmask()
        ));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            scroll(-1);
            return true;
        } else if (delta < 0) {
            scroll(1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Allow dragging the HUD preview box directly with the mouse!
        int hudX = getHudPreviewX();
        int hudY = getHudPreviewY();
        if (button == 0 && mouseX >= hudX && mouseX <= hudX + 150 && mouseY >= hudY && mouseY <= hudY + 34) {
            draggingHudPreview = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingHudPreview) {
            ChaosConfig.hudCustomX = Mth.clamp((int) mouseX - 75, 4, Math.max(4, this.width - 154));
            ChaosConfig.hudCustomY = Mth.clamp((int) mouseY - 15, 4, Math.max(4, this.height - 40));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingHudPreview) {
            draggingHudPreview = false;
            buildWidgets();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int getHudPreviewX() {
        if (ChaosConfig.hudCustomX >= 0) return ChaosConfig.hudCustomX;
        return switch (ChaosConfig.hudCorner) {
            case TOP_LEFT, BOTTOM_LEFT -> 8;
            case TOP_RIGHT, BOTTOM_RIGHT -> this.width - 160;
        };
    }

    private int getHudPreviewY() {
        if (ChaosConfig.hudCustomY >= 0) return ChaosConfig.hudCustomY;
        return switch (ChaosConfig.hudCorner) {
            case TOP_LEFT, TOP_RIGHT -> 8;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> this.height - 54;
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;

        // Title bar
        graphics.drawCenteredString(this.font,
                Component.literal("⚙ МЕНЮ ХАОС-МОДА (60 Событий & Квесты)").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                centerX, 8, 0xFFFFFF);

        // Render Event List Panel background
        int listTopY = 94;
        int listHeight = VISIBLE_ROWS * ROW_HEIGHT;
        graphics.fill(centerX - 210, listTopY, centerX + 210, listTopY + listHeight + 2, 0xCC101010);
        graphics.fill(centerX - 210, listTopY, centerX + 210, listTopY + 1, 0xFF666666);

        // Render each visible event row
        ChaosEvent hoveredEvent = null;
        for (int r = 0; r < VISIBLE_ROWS; r++) {
            int idx = scrollOffset + r;
            if (idx >= filteredEvents.size()) break;

            ChaosEvent ev = filteredEvents.get(idx);
            int ry = listTopY + r * ROW_HEIGHT;
            boolean isActiveNow = ClientChaosState.isEventActive(ev);

            int rowBg = (r % 2 == 0) ? 0x66222222 : 0x66181818;
            if (isActiveNow) {
                rowBg = 0x881E5E1E; // Highlight currently active event in green!
            }
            graphics.fill(centerX - 208, ry + 1, centerX + 208, ry + ROW_HEIGHT, rowBg);

            // Event Number & Name
            String numStr = String.format("#%02d ", ev.ordinal() + 1);
            graphics.drawString(this.font, numStr, centerX - 178, ry + 4, 0xAAAAAA);
            graphics.drawString(this.font, ev.getDisplayName(), centerX - 154, ry + 4, ev.getCategory().getColor());

            // Short description or active timer
            String subText = ev.getDescription();
            if (subText.length() > 48) {
                subText = subText.substring(0, 48) + "...";
            }
            if (isActiveNow) {
                int remSec = ClientChaosState.activeEvents.getOrDefault(ev, 0) / 20;
                subText = "🔥 АКТИВНО СЕЙЧАС (" + remSec + " сек) — " + subText;
            }
            graphics.drawString(this.font, subText, centerX - 178, ry + 14, isActiveNow ? 0x55FF55 : 0x888888);

            if (mouseX >= centerX - 180 && mouseX <= centerX + 90 && mouseY >= ry && mouseY <= ry + ROW_HEIGHT) {
                hoveredEvent = ev;
            }
        }

        // Render Currently Active Events inspector panel below list
        int activeBoxY = listTopY + listHeight + 5;
        graphics.fill(centerX - 210, activeBoxY, centerX + 210, activeBoxY + 26, 0xCC1B1B2F);
        StringBuilder activeSb = new StringBuilder("Сейчас действует: ");
        if (ClientChaosState.activeEvents.isEmpty()) {
            activeSb.append("Нет активных событий (следующее через ").append(ClientChaosState.nextEventTicks / 20).append(" сек)");
        } else {
            boolean first = true;
            for (Map.Entry<ChaosEvent, Integer> entry : ClientChaosState.activeEvents.entrySet()) {
                if (!first) activeSb.append(", ");
                activeSb.append(entry.getKey().getDisplayName()).append(" (").append(entry.getValue() / 20).append("с)");
                first = false;
            }
        }
        graphics.drawString(this.font, activeSb.toString(), centerX - 204, activeBoxY + 4, 0x55FFFF);

        String questStatus = ClientChaosState.activeQuest != null
                ? "Активный квест: «" + ClientChaosState.activeQuest.getTitle() + "» — " + ClientChaosState.activeQuest.getDescription()
                : "Квест: ожидание (новый через " + (ClientChaosState.nextQuestTicks / 20) + " сек)";
        if (questStatus.length() > 74) questStatus = questStatus.substring(0, 74) + "...";
        graphics.drawString(this.font, questStatus, centerX - 204, activeBoxY + 15, 0xFFDD55);

        // Render draggable HUD position preview box
        int hx = getHudPreviewX();
        int hy = getHudPreviewY();
        graphics.fill(hx, hy, hx + 150, hy + 28, 0xAA004466);
        graphics.drawCenteredString(this.font, "✥ Перетащи HUD мышкой", hx + 75, hy + 10, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);

        // Tooltip with full event description on hover
        if (hoveredEvent != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(hoveredEvent.getDisplayName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltip.add(Component.literal(hoveredEvent.getCategory().getTitle()).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal(hoveredEvent.getDescription()).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.literal("Длительность: " + hoveredEvent.getDefaultDurationSeconds() + " сек").withStyle(ChatFormatting.GRAY));
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class IntSlider extends AbstractSliderButton {
        private final String prefix;
        private final String suffix;
        private final int min;
        private final int max;
        private final java.util.function.IntConsumer onValueChange;

        public IntSlider(int x, int y, int width, int height, String prefix, String suffix, int min, int max, int current, java.util.function.IntConsumer onValueChange) {
            super(x, y, width, height, Component.empty(), (double) (current - min) / (double) (max - min));
            this.prefix = prefix;
            this.suffix = suffix;
            this.min = min;
            this.max = max;
            this.onValueChange = onValueChange;
            this.updateMessage();
        }

        private int getIntValue() {
            return (int) Math.round(min + value * (max - min));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(prefix + getIntValue() + suffix));
        }

        @Override
        protected void applyValue() {
            onValueChange.accept(getIntValue());
        }
    }
}
