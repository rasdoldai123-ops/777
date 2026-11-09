package com.example.chaosmod.client.gui;

import com.example.chaosmod.event.ChaosEvent;
import com.example.chaosmod.network.C2STriggerActionPacket;
import com.example.chaosmod.network.ChaosNetwork;
import com.example.chaosmod.client.ClientChaosState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CaptchaScreen extends Screen {
    private static class GridCell {
        final ItemStack stack;
        final boolean isDirt;
        boolean clicked = false;

        GridCell(ItemStack stack, boolean isDirt) {
            this.stack = stack;
            this.isDirt = isDirt;
        }
    }

    private final List<GridCell> cells = new ArrayList<>();
    private int dirtClickedCount = 0;

    public CaptchaScreen() {
        super(Component.literal("Капча на выживание"));
        initGrid();
    }

    private void initGrid() {
        cells.clear();
        dirtClickedCount = 0;
        // 3 dirt blocks and 6 decoys
        cells.add(new GridCell(new ItemStack(Items.DIRT), true));
        cells.add(new GridCell(new ItemStack(Items.DIRT), true));
        cells.add(new GridCell(new ItemStack(Items.DIRT), true));

        ItemStack[] decoys = new ItemStack[]{
                new ItemStack(Items.COBBLESTONE),
                new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.SAND),
                new ItemStack(Items.GRAVEL),
                new ItemStack(Items.STONE),
                new ItemStack(Items.RED_MUSHROOM)
        };
        for (ItemStack d : decoys) {
            cells.add(new GridCell(d, false));
        }
        Collections.shuffle(cells, new Random());
    }

    @Override
    protected void init() {
        super.init();
        int panelX = (this.width - 220) / 2;
        int panelY = (this.height - 180) / 2;

        for (int i = 0; i < 9; i++) {
            final int index = i;
            int row = i / 3;
            int col = i % 3;
            int bx = panelX + 35 + col * 52;
            int by = panelY + 45 + row * 40;

            this.addRenderableWidget(Button.builder(Component.empty(), btn -> onCellClick(index, btn))
                    .bounds(bx, by, 44, 34)
                    .build());
        }
    }

    private void onCellClick(int index, Button btn) {
        if (index < 0 || index >= cells.size()) return;
        GridCell cell = cells.get(index);
        if (cell.clicked) return;

        if (cell.isDirt) {
            cell.clicked = true;
            dirtClickedCount++;
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            }
            if (dirtClickedCount >= 3) {
                ClientChaosState.activeEvents.remove(ChaosEvent.SURVIVAL_CAPTCHA);
                ClientChaosState.captchaScreenOpened = false;
                ChaosNetwork.sendToServer(new C2STriggerActionPacket(C2STriggerActionPacket.ACTION_CAPTCHA_SOLVED, 0));
                this.onClose();
            }
        } else {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
            }
            // Reshuffle on wrong click!
            initGrid();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int panelW = 220;
        int panelH = 180;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        // Draw Minecraft-style window frame
        graphics.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, 0xFFAAAAAA);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE181818);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 28, 0xFF25488E);

        graphics.drawCenteredString(this.font,
                Component.literal("🤖 ДОКАЖИТЕ, ЧТО ВЫ НЕ БОТ").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                this.width / 2, panelY + 6, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.literal("Нажмите на 3 блока земли (" + dirtClickedCount + "/3)"),
                this.width / 2, panelY + 17, 0xFFDD55);

        super.render(graphics, mouseX, mouseY, partialTick);

        // Render items over the buttons
        for (int i = 0; i < 9 && i < cells.size(); i++) {
            GridCell cell = cells.get(i);
            int row = i / 3;
            int col = i % 3;
            int bx = panelX + 35 + col * 52;
            int by = panelY + 45 + row * 40;

            if (cell.clicked) {
                graphics.fill(bx + 2, by + 2, bx + 42, by + 32, 0x8800AA00);
                graphics.drawCenteredString(this.font, "✔", bx + 22, by + 12, 0x55FF55);
            } else {
                graphics.renderItem(cell.stack, bx + 14, by + 9);
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !ClientChaosState.isEventActive(ChaosEvent.SURVIVAL_CAPTCHA);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // World continues living around you!
    }
}
