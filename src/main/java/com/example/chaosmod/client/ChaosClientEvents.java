package com.example.chaosmod.client;

import com.example.chaosmod.ChaosMod;
import com.example.chaosmod.client.gui.CaptchaScreen;
import com.example.chaosmod.client.gui.ChaosMenuScreen;
import com.example.chaosmod.config.ChaosConfig;
import com.example.chaosmod.event.ChaosEvent;
import com.example.chaosmod.network.C2STriggerActionPacket;
import com.example.chaosmod.network.ChaosNetwork;
import com.example.chaosmod.quest.ChaosQuest;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = ChaosMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ChaosClientEvents {
    private static final Random RANDOM = new Random();
    private static Pig dummyPig = null;
    private static boolean renderingDisguisePig = false;
    private static int lastTickSoundSecond = -1;
    private static int lastQuestTickSoundSecond = -1;
    private static boolean wasOnGround = true;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        // Open Chaos Menu on key I
        while (ChaosKeybindings.OPEN_MENU_KEY.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new ChaosMenuScreen());
            }
        }

        // Open Survival Captcha screen automatically if active
        if (ClientChaosState.isEventActive(ChaosEvent.SURVIVAL_CAPTCHA)) {
            if (!(mc.screen instanceof CaptchaScreen)) {
                ClientChaosState.captchaScreenOpened = true;
                mc.setScreen(new CaptchaScreen());
            }
        } else if (ClientChaosState.captchaScreenOpened && mc.screen instanceof CaptchaScreen) {
            ClientChaosState.captchaScreenOpened = false;
            mc.setScreen(null);
        }

        // Detect player jump for Butterfingers and Air Tax
        if (wasOnGround && !player.onGround() && player.getDeltaMovement().y > 0.1) {
            ChaosNetwork.sendToServer(new C2STriggerActionPacket(C2STriggerActionPacket.ACTION_PLAYER_JUMPED, 0));
        }
        wasOnGround = player.onGround();

        // Sound ticking on the last 5 seconds before next event or quest failure
        if (ChaosConfig.soundTicking && ChaosConfig.modMode == ChaosConfig.ModMode.ENABLED) {
            int eventSec = ClientChaosState.nextEventTicks / 20;
            if (eventSec >= 1 && eventSec <= 5 && ClientChaosState.nextEventTicks % 20 == 0 && eventSec != lastTickSoundSecond) {
                lastTickSoundSecond = eventSec;
                player.playSound(SoundEvents.NOTE_BLOCK_HAT.get(), 0.8f, 1.5f);
            } else if (eventSec > 5) {
                lastTickSoundSecond = -1;
            }

            if (ClientChaosState.activeQuest != null) {
                int questSec = ClientChaosState.questRemainingTicks / 20;
                if (questSec >= 1 && questSec <= 5 && envTickCheck(questSec)) {
                    lastQuestTickSoundSecond = questSec;
                    player.playSound(SoundEvents.NOTE_BLOCK_PLING.get(), 0.9f, 1.8f);
                }
            }
        }

        // Narcissism: lock camera in third person front
        if (ClientChaosState.isEventActive(ChaosEvent.NARCISSISM)) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }

        // Phantom Pain: every 6 seconds (120 ticks) play hurt sound & flash screen red
        if (ClientChaosState.isEventActive(ChaosEvent.PHANTOM_PAIN)) {
            ClientChaosState.phantomPainCounter++;
            if (ClientChaosState.phantomPainCounter >= 120) {
                ClientChaosState.phantomPainCounter = 0;
                ClientChaosState.phantomPainFlashTicks = 12;
                player.playSound(SoundEvents.PLAYER_HURT, 1.0f, 1.0f);
            }
        }
        if (ClientChaosState.phantomPainFlashTicks > 0) {
            ClientChaosState.phantomPainFlashTicks--;
        }

        // Disco 80s: change color filter every second & rotate crosshair
        if (ClientChaosState.isEventActive(ChaosEvent.DISCO_80S)) {
            ClientChaosState.crosshairAngle = (ClientChaosState.crosshairAngle + 12.0f) % 360.0f;
            if (player.tickCount % 20 == 0) {
                int[] palette = new int[]{0x44FF0055, 0x4400FFAA, 0x44FFFF00, 0x44AA00FF, 0x4400AAFF};
                ClientChaosState.discoColor = palette[RANDOM.nextInt(palette.length)];
            }
        }

        // Acrophobia: when air is below feet, snap camera pitch strictly down (90 degrees)
        if (ClientChaosState.isEventActive(ChaosEvent.ACROPHOBIA)) {
            if (!player.onGround() && mc.level.getBlockState(player.blockPosition().below()).isAir()) {
                player.setXRot(90.0f);
            }
        }

        // Sound Schizophrenia: step sounds become creeper primed hiss
        if (ClientChaosState.isEventActive(ChaosEvent.SOUND_SCHIZOPHRENIA)) {
            if (player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 0.005 && player.tickCount % 12 == 0) {
                mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                        SoundEvents.CREEPER_PRIMED, SoundSource.PLAYERS, 0.7f, 1.2f, false);
            }
        }

        // Drunken Master permutation setup
        boolean drunkenNow = ClientChaosState.isEventActive(ChaosEvent.DRUNKEN_MASTER);
        if (drunkenNow && !ClientChaosState.wasDrunkenActive) {
            List<Integer> perm = Arrays.asList(0, 1, 2, 3);
            Collections.shuffle(perm, RANDOM);
            // Ensure not identity
            if (perm.get(0) == 0 && perm.get(1) == 1 && perm.get(2) == 2 && perm.get(3) == 3) {
                Collections.swap(perm, 0, 1);
            }
            for (int i = 0; i < 4; i++) {
                ClientChaosState.drunkenPermutation[i] = perm.get(i);
            }
        }
        ClientChaosState.wasDrunkenActive = drunkenNow;

        // Refresh hitbox dimensions on client if Claustrophobia or Lilliputian active
        if (player.tickCount % 10 == 0) {
            player.refreshDimensions();
        }
    }

    private static boolean envTickCheck(int questSec) {
        return questSec != lastQuestTickSoundSecond;
    }

    // ==================== MOVEMENT INPUT HOOKS ====================

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        Input input = event.getInput();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // Survival Captcha freezes movement completely
        if (ClientChaosState.isEventActive(ChaosEvent.SURVIVAL_CAPTCHA)) {
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.forwardImpulse = 0;
            input.leftImpulse = 0;
            input.jumping = false;
            return;
        }

        // Drunken Master: shuffle W, S, A, D
        if (ClientChaosState.isEventActive(ChaosEvent.DRUNKEN_MASTER)) {
            boolean[] orig = new boolean[]{input.up, input.down, input.left, input.right};
            input.up = orig[ClientChaosState.drunkenPermutation[0]];
            input.down = orig[ClientChaosState.drunkenPermutation[1]];
            input.left = orig[ClientChaosState.drunkenPermutation[2]];
            input.right = orig[ClientChaosState.drunkenPermutation[3]];

            input.forwardImpulse = (input.up == input.down) ? 0.0f : (input.up ? 1.0f : -1.0f);
            input.leftImpulse = (input.left == input.right) ? 0.0f : (input.left ? 1.0f : -1.0f);
        }

        // Brakes Failed: continuous forward sprint
        if (ClientChaosState.isEventActive(ChaosEvent.BRAKES_FAILED)) {
            input.up = true;
            input.down = false;
            input.forwardImpulse = 1.0f;
            player.setSprinting(true);
        }

        // Jupiter Gravity: jumping disabled (height = 0)
        if (ClientChaosState.isEventActive(ChaosEvent.JUPITER_GRAVITY)) {
            input.jumping = false;
        }
    }

    // ==================== CAMERA, FOV & FOG HOOKS ====================

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        // Australian Gaming: flip camera 180 degrees upside down
        if (ClientChaosState.isEventActive(ChaosEvent.AUSTRALIAN_GAMING)) {
            event.setRoll(event.getRoll() + 180.0f);
        }

        // Phantom Pain camera shake
        if (ClientChaosState.phantomPainFlashTicks > 0) {
            event.setRoll(event.getRoll() + (RANDOM.nextFloat() - 0.5f) * 14.0f);
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        // Sniper Scope: FOV drops to 15 degrees
        if (ClientChaosState.isEventActive(ChaosEvent.SNIPER_SCOPE)) {
            event.setFOV(15.0);
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        // Grandpa's Myopia: 1.5 chunks render distance & thick white fog
        if (ClientChaosState.isEventActive(ChaosEvent.GRANDPA_MYOPIA)) {
            event.setNearPlaneDistance(2.0f);
            event.setFarPlaneDistance(24.0f); // 1.5 chunks = 24 blocks
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (ClientChaosState.isEventActive(ChaosEvent.GRANDPA_MYOPIA)) {
            event.setRed(0.96f);
            event.setGreen(0.96f);
            event.setBlue(0.96f);
        }
    }

    // ==================== ENTITY RENDERING & X-RAY HOOKS ====================

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player || renderingDisguisePig) return;

        // Disguise: all mobs around get the model and texture of a normal Pig!
        if (ClientChaosState.isEventActive(ChaosEvent.DISGUISE) && entity instanceof Mob) {
            event.setCanceled(true);
            if (dummyPig == null || dummyPig.level() != mc.level) {
                dummyPig = EntityType.PIG.create(mc.level);
            }
            if (dummyPig != null) {
                dummyPig.tickCount = entity.tickCount;
                dummyPig.setYRot(entity.getYRot());
                dummyPig.yRotO = entity.yRotO;
                dummyPig.setXRot(entity.getXRot());
                dummyPig.xRotO = entity.xRotO;
                dummyPig.yBodyRot = entity.yBodyRot;
                dummyPig.yBodyRotO = entity.yBodyRotO;
                dummyPig.yHeadRot = entity.yHeadRot;
                dummyPig.yHeadRotO = entity.yHeadRotO;
                dummyPig.walkAnimation.setSpeed(entity.walkAnimation.speed());

                renderingDisguisePig = true;
                try {
                    mc.getEntityRenderDispatcher().render(
                            dummyPig, 0, 0, 0,
                            entity.getYRot(), event.getPartialTick(),
                            event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight()
                    );
                } finally {
                    renderingDisguisePig = false;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // Roskomnadzor: draw black censorship rectangles over all mobs in view
        if (ClientChaosState.isEventActive(ChaosEvent.ROSKOMNADZOR)) {
            VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
            List<Mob> mobs = mc.level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32.0));
            for (Mob mob : mobs) {
                AABB box = mob.getBoundingBox().inflate(0.15).move(-camPos.x, -camPos.y, -camPos.z);
                // Draw dense black censorship cage/box over the mob model
                for (double shrink = 0.0; shrink <= 0.4; shrink += 0.08) {
                    LevelRenderer.renderLineBox(poseStack, lineConsumer, box.deflate(shrink), 0.0f, 0.0f, 0.0f, 1.0f);
                }
            }
            bufferSource.endBatch(RenderType.lines());
        }

        // Legal X-Ray: highlight iron, gold, and diamond ores within 20 blocks with colored outlines through walls
        if (ClientChaosState.isEventActive(ChaosEvent.LEGAL_XRAY)) {
            RenderSystem.disableDepthTest();
            VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
            BlockPos center = player.blockPosition();
            for (int dx = -20; dx <= 20; dx++) {
                for (int dy = -20; dy <= 20; dy++) {
                    for (int dz = -20; dz <= 20; dz++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        BlockState st = mc.level.getBlockState(pos);
                        float r = 0, g = 0, b = 0;
                        boolean isOre = false;
                        if (st.is(BlockTags.DIAMOND_ORES)) {
                            r = 0.2f; g = 0.9f; b = 1.0f; isOre = true;
                        } else if (st.is(BlockTags.GOLD_ORES)) {
                            r = 1.0f; g = 0.85f; b = 0.1f; isOre = true;
                        } else if (st.is(BlockTags.IRON_ORES)) {
                            r = 0.9f; g = 0.7f; b = 0.55f; isOre = true;
                        }
                        if (isOre) {
                            AABB box = new AABB(pos).move(-camPos.x, -camPos.y, -camPos.z);
                            LevelRenderer.renderLineBox(poseStack, lineConsumer, box, r, g, b, 1.0f);
                        }
                    }
                }
            }
            bufferSource.endBatch(RenderType.lines());
            RenderSystem.enableDepthTest();
        }
    }

    // ==================== HUD & GUI OVERLAYS ====================

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        // Blind Spot: hide all HUD elements (HP, hunger, hotbar, crosshair)
        if (ClientChaosState.isEventActive(ChaosEvent.BLIND_SPOT)) {
            if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()
                    || event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()
                    || event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()
                    || event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()
                    || event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type()
                    || event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type()) {
                event.setCanceled(true);
                return;
            }
        }

        // DVD Screensaver: hide vanilla bottom hotbar and render flying bouncing hotbar instead
        if (ClientChaosState.isEventActive(ChaosEvent.DVD_SCREENSAVER)
                && event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
            event.setCanceled(true);
        }

        // Disco 80s: hide default crosshair so we can draw the rotating one
        if (ClientChaosState.isEventActive(ChaosEvent.DISCO_80S)
                && event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // 1. Disco 80s color filter + rotating crosshair
        if (ClientChaosState.isEventActive(ChaosEvent.DISCO_80S)) {
            graphics.fill(0, 0, screenW, screenH, ClientChaosState.discoColor);
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(screenW / 2.0f, screenH / 2.0f, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(ClientChaosState.crosshairAngle));
            graphics.fill(-6, -1, 6, 1, 0xFFFFFFFF);
            graphics.fill(-1, -6, 1, 6, 0xFFFFFFFF);
            pose.popPose();
        }

        // 2. Phantom Pain red hurt flash overlay
        if (ClientChaosState.phantomPainFlashTicks > 0) {
            graphics.fill(0, 0, screenW, screenH, 0x55FF0000);
        }

        // 3. DVD Screensaver flying hotbar
        if (ClientChaosState.isEventActive(ChaosEvent.DVD_SCREENSAVER)) {
            renderDvdHotbar(graphics, mc, screenW, screenH);
        }

        // 4. Roskomnadzor 2D screen-space black censor bars over visible mobs
        if (ClientChaosState.isEventActive(ChaosEvent.ROSKOMNADZOR)) {
            graphics.drawCenteredString(font, "██ РОСКОМНАДЗОР: МОБЫ ЗАЦЕНЗУРЕНЫ ██", screenW / 2, screenH - 48, 0xFF5555);
        }

        // 5. Fake Crash ("Saving World...") full-screen freeze overlay for 4 seconds
        if (ClientChaosState.isEventActive(ChaosEvent.FAKE_CRASH)) {
            graphics.fill(0, 0, screenW, screenH, 0xFF2E2218);
            graphics.drawCenteredString(font, "Saving World...", screenW / 2, screenH / 2 - 10, 0xFFFFFF);
            return;
        }

        // 6. Render Quest Top Progress Bar (if a quest is active)
        if (ClientChaosState.activeQuest != null) {
            renderQuestOverlay(graphics, font, screenW);
        }

        // 7. Render Chaos Mod Timers & Active Events HUD in configured corner
        renderChaosHud(graphics, font, screenW, screenH);
    }

    private static void renderDvdHotbar(GuiGraphics graphics, Minecraft mc, int screenW, int screenH) {
        int barW = 182;
        int barH = 22;

        ClientChaosState.dvdX += ClientChaosState.dvdVx;
        ClientChaosState.dvdY += ClientChaosState.dvdVy;

        boolean hitX = false;
        boolean hitY = false;

        if (ClientChaosState.dvdX <= 0) {
            ClientChaosState.dvdX = 0;
            ClientChaosState.dvdVx = Math.abs(ClientChaosState.dvdVx);
            hitX = true;
        } else if (ClientChaosState.dvdX + barW >= screenW) {
            ClientChaosState.dvdX = screenW - barW;
            ClientChaosState.dvdVx = -Math.abs(ClientChaosState.dvdVx);
            hitX = true;
        }

        if (ClientChaosState.dvdY <= 0) {
            ClientChaosState.dvdY = 0;
            ClientChaosState.dvdVy = Math.abs(ClientChaosState.dvdVy);
            hitY = true;
        } else if (ClientChaosState.dvdY + barH >= screenH) {
            ClientChaosState.dvdY = screenH - barH;
            ClientChaosState.dvdVy = -Math.abs(ClientChaosState.dvdVy);
            hitY = true;
        }

        // Change color if it hits a corner (or edge)
        if (hitX && hitY) {
            int[] colors = new int[]{0xFFFF5555, 0xFF55FF55, 0xFF5555FF, 0xFFFFFF55, 0xFFFF55FF, 0xFF55FFFF};
            ClientChaosState.dvdColor = colors[RANDOM.nextInt(colors.length)];
        } else if (hitX || hitY) {
            int[] colors = new int[]{0xAA222222, 0xAA331144, 0xAA113344, 0xAA443311};
            ClientChaosState.dvdColor = colors[RANDOM.nextInt(colors.length)];
        }

        int x = Math.round(ClientChaosState.dvdX);
        int y = Math.round(ClientChaosState.dvdY);

        graphics.fill(x - 2, y - 2, x + barW + 2, y + barH + 2, ClientChaosState.dvdColor);
        graphics.fill(x, y, x + barW, y + barH, 0xDD111111);

        for (int slot = 0; slot < 9; slot++) {
            int sx = x + slot * 20 + 2;
            int sy = y + 3;
            if (mc.player.getInventory().selected == slot) {
                graphics.fill(sx - 2, sy - 2, sx + 18, sy + 18, 0xAAFFFFFF);
            }
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, sx, sy);
                graphics.renderItemDecorations(mc.font, stack, sx, sy);
            }
        }
    }

    private static void renderQuestOverlay(GuiGraphics graphics, Font font, int screenW) {
        ChaosQuest quest = ClientChaosState.activeQuest;
        int boxW = 280;
        int boxH = 38;
        int boxX = (screenW - boxW) / 2;
        int boxY = 6;

        graphics.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + boxH + 2, 0xFFAA8800);
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xDD101420);

        int secLeft = Math.max(0, ClientChaosState.questRemainingTicks / 20);
        String header = "📜 КВЕСТ: «" + quest.getTitle() + "» [" + secLeft + " сек]";
        graphics.drawCenteredString(font, Component.literal(header).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                screenW / 2, boxY + 4, 0xFFFFFF);

        graphics.drawCenteredString(font, quest.getDescription(), screenW / 2, boxY + 15, 0xDDDDDD);

        // Progress Bar
        int barW = boxW - 20;
        int barX = boxX + 10;
        int barY = boxY + 27;
        graphics.fill(barX, barY, barX + barW, barY + 7, 0xFF333333);

        float ratio = Math.min(1.0f, (float) ClientChaosState.questProgress / (float) Math.max(1, ClientChaosState.questMaxProgress));
        int fillW = (int) (barW * ratio);
        graphics.fill(barX, barY, barX + fillW, barY + 7, 0xFF22DD44);
    }

    private static void renderChaosHud(GuiGraphics graphics, Font font, int screenW, int screenH) {
        int hudW = 165;
        int activeCount = ClientChaosState.activeEvents.size();
        int hudH = 32 + activeCount * 11;

        int x;
        int y;
        if (ChaosConfig.hudCustomX >= 0 && ChaosConfig.hudCustomY >= 0) {
            x = Math.min(ChaosConfig.hudCustomX, screenW - hudW - 4);
            y = Math.min(ChaosConfig.hudCustomY, screenH - hudH - 4);
        } else {
            x = switch (ChaosConfig.hudCorner) {
                case TOP_LEFT, BOTTOM_LEFT -> 6;
                case TOP_RIGHT, BOTTOM_RIGHT -> screenW - hudW - 6;
            };
            y = switch (ChaosConfig.hudCorner) {
                case TOP_LEFT, TOP_RIGHT -> 6;
                case BOTTOM_LEFT, BOTTOM_RIGHT -> screenH - hudH - 6;
            };
        }

        graphics.fill(x - 1, y - 1, x + hudW + 1, y + hudH + 1, 0xAA444444);
        graphics.fill(x, y, x + hudW, y + hudH, 0xBB121218);

        int evSec = ClientChaosState.nextEventTicks / 20;
        int qSec = ClientChaosState.nextQuestTicks / 20;

        String modeBadge = ChaosConfig.modMode == ChaosConfig.ModMode.ENABLED ? "" : " [" + ChaosConfig.modMode.getLabel() + "]";
        graphics.drawString(font, "⚡ След. событие: " + formatTime(evSec) + modeBadge, x + 5, y + 5,
                evSec <= 5 ? 0xFF5555 : 0x55FFFF);
        graphics.drawString(font, "📜 След. квест: " + formatTime(qSec), x + 5, y + 16, 0xFFDD55);

        int ry = y + 28;
        for (Map.Entry<ChaosEvent, Integer> entry : ClientChaosState.activeEvents.entrySet()) {
            ChaosEvent ev = entry.getKey();
            int remSec = entry.getValue() / 20;
            String line = "• " + ev.getDisplayName() + " (" + remSec + "с)";
            graphics.drawString(font, line, x + 5, ry, ev.getCategory().getColor());
            ry += 11;
        }
    }

    private static String formatTime(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}
