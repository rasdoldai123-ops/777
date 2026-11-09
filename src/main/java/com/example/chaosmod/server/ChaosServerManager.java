package com.example.chaosmod.server;

import com.example.chaosmod.config.ChaosConfig;
import com.example.chaosmod.event.ChaosEvent;
import com.example.chaosmod.event.EventCategory;
import com.example.chaosmod.network.ChaosNetwork;
import com.example.chaosmod.network.S2CSyncStatePacket;
import com.example.chaosmod.quest.ChaosPunishment;
import com.example.chaosmod.quest.ChaosQuest;
import com.example.chaosmod.quest.ChaosReward;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber
public class ChaosServerManager {
    private static final Map<UUID, PlayerChaosData> PLAYER_DATA = new HashMap<>();
    private static final Random RANDOM = new Random();

    private static int nextEventTicks = 60 * 20;
    private static int nextQuestTicks = 300 * 20;

    private static final UUID CHRONIC_FATIGUE_UUID = UUID.fromString("8d7a9c12-45e1-4f88-9b11-223344556677");

    public static PlayerChaosData getData(ServerPlayer player) {
        return PLAYER_DATA.computeIfAbsent(player.getUUID(), k -> new PlayerChaosData());
    }

    public static PlayerChaosData getData(UUID uuid) {
        return PLAYER_DATA.get(uuid);
    }

    public static void onConfigUpdated(MinecraftServer server) {
        if (nextEventTicks > ChaosConfig.eventIntervalSeconds * 20) {
            nextEventTicks = ChaosConfig.eventIntervalSeconds * 20;
        }
        if (nextQuestTicks > ChaosConfig.questIntervalSeconds * 20) {
            nextQuestTicks = ChaosConfig.questIntervalSeconds * 20;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        if (ChaosConfig.modMode == ChaosConfig.ModMode.ENABLED) {
            nextEventTicks--;
            if (nextEventTicks <= 0) {
                nextEventTicks = ChaosConfig.eventIntervalSeconds * 20;
                ChaosEvent chosen = pickRandomEnabledEvent();
                if (chosen != null) {
                    for (ServerPlayer player : players) {
                        triggerEventForPlayer(player, chosen);
                    }
                }
            }

            nextQuestTicks--;
            if (nextQuestTicks <= 0) {
                nextQuestTicks = ChaosConfig.questIntervalSeconds * 20;
                ChaosQuest chosenQuest = ChaosQuest.values()[RANDOM.nextInt(ChaosQuest.values().length)];
                for (ServerPlayer player : players) {
                    startQuestForPlayer(player, chosenQuest);
                }
            }
        }

        for (ServerPlayer player : players) {
            tickPlayer(player);
        }
    }

    private static ChaosEvent pickRandomEnabledEvent() {
        List<ChaosEvent> enabled = new ArrayList<>();
        for (ChaosEvent ev : ChaosEvent.values()) {
            if (ChaosConfig.isEventEnabled(ev)) {
                enabled.add(ev);
            }
        }
        if (enabled.isEmpty()) return null;
        return enabled.get(RANDOM.nextInt(enabled.size()));
    }

    public static void triggerEventForPlayer(ServerPlayer player, ChaosEvent event) {
        PlayerChaosData data = getData(player);
        int durationTicks = event.getDefaultDurationTicks();
        data.activeEvents.put(event, durationTicks);

        // Broadcast chat notification
        Component msg = Component.literal("⚡ [Событие] ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(event.getDisplayName()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" — " + event.getDescription()).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(msg);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.MASTER, 0.5f, 1.4f);

        onEventStart(player, data, event);
        syncToPlayer(player);
    }

    public static void clearEventsForPlayer(ServerPlayer player) {
        PlayerChaosData data = getData(player);
        List<ChaosEvent> current = new ArrayList<>(data.activeEvents.keySet());
        for (ChaosEvent ev : current) {
            onEventEnd(player, data, ev);
        }
        data.activeEvents.clear();
        player.refreshDimensions();
        syncToPlayer(player);
    }

    private static void onEventStart(ServerPlayer player, PlayerChaosData data, ChaosEvent event) {
        ServerLevel level = player.serverLevel();

        switch (event) {
            case SURVIVAL_CAPTCHA -> {
                data.captchaFreezePos = player.position();
            }
            case CLAUSTROPHOBIA, LILLIPUTIAN -> {
                if (event == ChaosEvent.CLAUSTROPHOBIA) {
                    data.activeEvents.remove(ChaosEvent.LILLIPUTIAN);
                } else {
                    data.activeEvents.remove(ChaosEvent.CLAUSTROPHOBIA);
                }
                player.refreshDimensions();
            }
            case WEEPING_ANGEL -> {
                Vec3 look = player.getLookAngle().normalize();
                Vec3 spawnPos = player.position().subtract(look.scale(4.0));
                ArmorStand angel = new ArmorStand(level, spawnPos.x, player.getY(), spawnPos.z);
                angel.setCustomName(Component.literal("Плачущий ангел").withStyle(ChatFormatting.DARK_RED));
                angel.setCustomNameVisible(true);
                angel.setInvulnerable(true);
                angel.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Blocks.CARVED_PUMPKIN));
                angel.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                angel.addTag("chaos_weeping_angel_" + player.getUUID());
                level.addFreshEntity(angel);
                data.spawnedEventEntities.add(angel.getUUID());
            }
            case CASTLING -> {
                List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(40.0));
                Mob farthest = null;
                double maxDist = 0;
                for (Mob mob : mobs) {
                    double d = mob.distanceToSqr(player);
                    if (d > maxDist) {
                        maxDist = d;
                        farthest = mob;
                    }
                }
                if (farthest != null) {
                    Vec3 pPos = player.position();
                    float pYaw = player.getYRot();
                    float pPitch = player.getXRot();

                    Vec3 mPos = farthest.position();
                    float mYaw = farthest.getYRot();
                    float mPitch = farthest.getXRot();

                    player.teleportTo(level, mPos.x, mPos.y, mPos.z, mYaw, mPitch);
                    farthest.teleportTo(pPos.x, pPos.y, pPos.z);
                    farthest.setYRot(pYaw);
                    farthest.setXRot(pPitch);
                    level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }
            case CHICKEN_BOMBER -> {
                for (int i = 0; i < 10; i++) {
                    Chicken chicken = EntityType.CHICKEN.create(level);
                    if (chicken != null) {
                        double ox = (RANDOM.nextDouble() - 0.5) * 6.0;
                        double oz = (RANDOM.nextDouble() - 0.5) * 6.0;
                        chicken.moveTo(player.getX() + ox, player.getY() + 15.0, player.getZ() + oz, 0, 0);
                        chicken.setCustomName(Component.literal("Бомбардировщик").withStyle(ChatFormatting.RED));
                        chicken.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 35, 0, false, false));
                        chicken.addTag("chaos_bomber_chicken_" + player.getUUID());
                        level.addFreshEntity(chicken);
                        data.spawnedEventEntities.add(chicken.getUUID());
                    }
                }
            }
            case BAILIFF -> {
                ZombieVillager bailiff = EntityType.ZOMBIE_VILLAGER.create(level);
                if (bailiff != null) {
                    bailiff.moveTo(player.getX() + 3, player.getY(), player.getZ() + 3, 0, 0);
                    bailiff.setCustomName(Component.literal("Судебный пристав").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    bailiff.setCustomNameVisible(true);
                    bailiff.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                    bailiff.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                    bailiff.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 120, 1));
                    bailiff.addTag("chaos_bailiff_" + player.getUUID());
                    level.addFreshEntity(bailiff);
                }
            }
            case BUILDER_ZOMBIES -> {
                List<Zombie> zombies = level.getEntitiesOfClass(Zombie.class, player.getBoundingBox().inflate(25.0));
                if (zombies.isEmpty()) {
                    for (int i = 0; i < 2; i++) {
                        Zombie z = EntityType.ZOMBIE.create(level);
                        if (z != null) {
                            z.moveTo(player.getX() + (i == 0 ? 4 : -4), player.getY(), player.getZ() + 4, 0, 0);
                            z.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.COBBLESTONE));
                            z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                            z.setCustomName(Component.literal("Зомби-строитель"));
                            level.addFreshEntity(z);
                        }
                    }
                } else {
                    for (Zombie z : zombies) {
                        z.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.COBBLESTONE));
                    }
                }
            }
            case COSMONAUT_SKELETONS -> {
                for (int i = 0; i < 2; i++) {
                    Skeleton skel = EntityType.SKELETON.create(level);
                    if (skel != null) {
                        skel.moveTo(player.getX() + (i == 0 ? 5 : -5), player.getY(), player.getZ() + 5, 0, 0);
                        skel.setCustomName(Component.literal("Скелет-космонавт").withStyle(ChatFormatting.AQUA));
                        skel.setCustomNameVisible(true);
                        skel.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Blocks.GLASS));
                        skel.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
                        skel.addTag("chaos_cosmonaut_skeleton");
                        level.addFreshEntity(skel);
                    }
                }
            }
            case SHELL_GAME -> {
                List<ItemStack> allItems = new ArrayList<>();
                for (int i = 0; i < 36; i++) {
                    allItems.add(player.getInventory().getItem(i).copy());
                }
                Collections.shuffle(allItems, RANDOM);
                for (int i = 0; i < 36; i++) {
                    player.getInventory().setItem(i, allItems.get(i));
                }
                player.inventoryMenu.broadcastChanges();
            }
            case HOT_POTATO -> {
                data.hotPotatoCyclesLeft = 3;
                data.hotPotatoWaitingReturn = false;
                giveHotPotato(player, data);
            }
            case SANCTIONS -> {
                for (int slot = 3; slot < 9; slot++) {
                    ItemStack current = player.getInventory().getItem(slot);
                    if (!current.is(Blocks.BARRIER.asItem())) {
                        data.sanctionedItems.put(slot, current.copy());
                    }
                    ItemStack barrier = new ItemStack(Blocks.BARRIER);
                    barrier.setHoverName(Component.literal("САНКЦИИ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    barrier.getOrCreateTag().putBoolean("ChaosSanctionBarrier", true);
                    player.getInventory().setItem(slot, barrier);
                }
                player.inventoryMenu.broadcastChanges();
            }
            case EXPIRED_FOOD -> {
                data.expiredFoodBackup.clear();
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && stack.isEdible() && !stack.is(Items.SPIDER_EYE) && !stack.is(Items.POISONOUS_POTATO)) {
                        data.expiredFoodBackup.put(i, stack.copy());
                        Item replacement = RANDOM.nextBoolean() ? Items.SPIDER_EYE : Items.POISONOUS_POTATO;
                        ItemStack rotten = new ItemStack(replacement, stack.getCount());
                        rotten.setHoverName(Component.literal("Просрочка").withStyle(ChatFormatting.DARK_GREEN));
                        player.getInventory().setItem(i, rotten);
                    }
                }
                player.inventoryMenu.broadcastChanges();
            }
            case ALIEXPRESS_DIAMONDS -> {
                for (int i = 0; i < 25; i++) {
                    double ox = (RANDOM.nextDouble() - 0.5) * 10.0;
                    double oz = (RANDOM.nextDouble() - 0.5) * 10.0;
                    ItemStack diamond = new ItemStack(Items.DIAMOND);
                    if (i < 22) {
                        diamond.getOrCreateTag().putBoolean("ChaosFakeDiamond", true);
                        diamond.setHoverName(Component.literal("Алмаз с AliExpress").withStyle(ChatFormatting.AQUA));
                    }
                    ItemEntity itemEntity = new ItemEntity(level, player.getX() + ox, player.getY() + 12.0 + RANDOM.nextDouble() * 5, player.getZ() + oz, diamond);
                    level.addFreshEntity(itemEntity);
                }
            }
            case HUMANITARIAN_AID -> {
                BlockPos dropPos = player.blockPosition().above(10);
                Chicken parachuteChicken = EntityType.CHICKEN.create(level);
                if (parachuteChicken != null) {
                    parachuteChicken.moveTo(dropPos.getX() + 0.5, dropPos.getY() + 2, dropPos.getZ() + 0.5, 0, 0);
                    parachuteChicken.setCustomName(Component.literal("Гуманитарный десант").withStyle(ChatFormatting.GREEN));
                    level.addFreshEntity(parachuteChicken);
                }
                BlockPos chestPos = player.blockPosition().offset(1, 0, 1);
                level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
                BlockEntity be = level.getBlockEntity(chestPos);
                if (be instanceof ChestBlockEntity chest) {
                    chest.setItem(0, new ItemStack(Items.IRON_INGOT, 8));
                    chest.setItem(1, new ItemStack(Items.COOKED_BEEF, 12));
                    chest.setItem(2, new ItemStack(Items.ENDER_PEARL, 3));
                    chest.setItem(3, new ItemStack(Items.WATER_BUCKET, 1));
                    chest.setItem(4, new ItemStack(Items.GOLDEN_APPLE, 1));
                }
                level.playSound(null, chestPos, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            case PMC_GOLEM -> {
                for (int i = 0; i < 2; i++) {
                    IronGolem golem = EntityType.IRON_GOLEM.create(level);
                    if (golem != null) {
                        golem.moveTo(player.getX() + (i == 0 ? 2 : -2), player.getY(), player.getZ() + 2, 0, 0);
                        golem.setCustomName(Component.literal("ЧВК «Голем»").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                        golem.setCustomNameVisible(true);
                        golem.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 60, 1));
                        golem.addTag("chaos_pmc_golem_" + player.getUUID());
                        level.addFreshEntity(golem);
                        data.spawnedEventEntities.add(golem.getUUID());
                    }
                }
            }
            case VACUUM_CLEANER -> {
                List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(60.0));
                for (ItemEntity item : items) {
                    item.teleportTo(player.getX(), player.getY(), player.getZ());
                    item.setNoPickUpDelay();
                }
                List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, player.getBoundingBox().inflate(60.0));
                for (ExperienceOrb orb : orbs) {
                    orb.teleportTo(player.getX(), player.getY(), player.getZ());
                }
            }
            case SECOND_WIND -> {
                player.removeAllEffects();
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(20.0f);
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 120, 2)); // +6 hearts
                level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
            }
            case CREATIVE_FLIGHT_DEMO -> {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }
            default -> {}
        }
    }

    private static void onEventEnd(ServerPlayer player, PlayerChaosData data, ChaosEvent event) {
        ServerLevel level = player.serverLevel();

        switch (event) {
            case SURVIVAL_CAPTCHA -> data.captchaFreezePos = null;
            case CLAUSTROPHOBIA, LILLIPUTIAN -> player.refreshDimensions();
            case SANCTIONS -> {
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    ItemStack stack = player.getInventory().getItem(slot);
                    if (stack.hasTag() && stack.getTag().getBoolean("ChaosSanctionBarrier")) {
                        player.getInventory().setItem(slot, ItemStack.EMPTY);
                    }
                }
                for (Map.Entry<Integer, ItemStack> entry : data.sanctionedItems.entrySet()) {
                    int slot = entry.getKey();
                    ItemStack orig = entry.getValue();
                    if (!orig.isEmpty()) {
                        if (player.getInventory().getItem(slot).isEmpty()) {
                            player.getInventory().setItem(slot, orig);
                        } else if (!player.getInventory().add(orig)) {
                            player.drop(orig, false);
                        }
                    }
                }
                data.sanctionedItems.clear();
                player.inventoryMenu.broadcastChanges();
            }
            case EXPIRED_FOOD -> {
                for (Map.Entry<Integer, ItemStack> entry : data.expiredFoodBackup.entrySet()) {
                    int slot = entry.getKey();
                    ItemStack current = player.getInventory().getItem(slot);
                    if (current.is(Items.SPIDER_EYE) || current.is(Items.POISONOUS_POTATO)) {
                        int remainingCount = current.getCount();
                        ItemStack restored = entry.getValue().copy();
                        restored.setCount(Math.min(restored.getCount(), remainingCount));
                        player.getInventory().setItem(slot, restored);
                    }
                }
                data.expiredFoodBackup.clear();
                player.inventoryMenu.broadcastChanges();
            }
            case GLASS_WORLD -> {
                for (Map.Entry<BlockPos, BlockState> entry : data.glassWorldOriginalBlocks.entrySet()) {
                    if (level.getBlockState(entry.getKey()).is(Blocks.GLASS)) {
                        level.setBlockAndUpdate(entry.getKey(), entry.getValue());
                    }
                }
                data.glassWorldOriginalBlocks.clear();
            }
            case IMMORTALITY_CREDIT -> {
                player.setHealth(2.0f); // 1 heart left!
                player.sendSystemMessage(Component.literal("💔 Срок кредита бессмертия истек! Осталось 1 сердечко!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7f, 0.8f);
            }
            case CREATIVE_FLIGHT_DEMO -> {
                if (player.gameMode.getGameModeForPlayer() != GameType.CREATIVE && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }
            case WEEPING_ANGEL, PMC_GOLEM, CHICKEN_BOMBER -> {
                Iterator<UUID> it = data.spawnedEventEntities.iterator();
                while (it.hasNext()) {
                    UUID id = it.next();
                    Entity e = level.getEntity(id);
                    if (e != null) {
                        e.discard();
                    }
                    it.remove();
                }
            }
            default -> {}
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        PlayerChaosData data = getData(player);
        ServerLevel level = player.serverLevel();

        // Update position history for Rostelecom Simulator
        data.positionHistory.addLast(player.position());
        while (data.positionHistory.size() > 45) {
            data.positionHistory.removeFirst();
        }

        // Tick active events
        if (!data.activeEvents.isEmpty()) {
            List<ChaosEvent> finished = new ArrayList<>();
            for (Map.Entry<ChaosEvent, Integer> entry : new ArrayList<>(data.activeEvents.entrySet())) {
                ChaosEvent ev = entry.getKey();
                int rem = entry.getValue() - 1;
                if (rem <= 0) {
                    finished.add(ev);
                } else {
                    data.activeEvents.put(ev, rem);
                    tickActiveEvent(player, data, ev, rem);
                }
            }
            for (ChaosEvent ev : finished) {
                data.activeEvents.remove(ev);
                onEventEnd(player, data, ev);
            }
        }

        // Tick delayed Gravel Curse blocks
        if (!data.pendingGravelBlocks.isEmpty()) {
            Iterator<Map.Entry<BlockPos, Integer>> it = data.pendingGravelBlocks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, Integer> entry = it.next();
                int ticks = entry.getValue() - 1;
                if (ticks <= 0) {
                    BlockPos pos = entry.getKey();
                    BlockState st = level.getBlockState(pos);
                    if (!st.isAir() && !st.is(Blocks.BEDROCK) && level.getBlockEntity(pos) == null) {
                        Block fallBlock = RANDOM.nextBoolean() ? Blocks.GRAVEL : Blocks.SAND;
                        level.setBlockAndUpdate(pos, fallBlock.defaultBlockState());
                    }
                    it.remove();
                } else {
                    entry.setValue(ticks);
                }
            }
        }

        // Tick active quest
        if (data.activeQuest != null) {
            tickActiveQuest(player, data);
        }

        // Sync every 5 ticks to keep client HUD smooth and accurate
        if (player.tickCount % 5 == 0) {
            syncToPlayer(player);
        }
    }

    private static void tickActiveEvent(ServerPlayer player, PlayerChaosData data, ChaosEvent event, int remainingTicks) {
        ServerLevel level = player.serverLevel();

        switch (event) {
            case SURVIVAL_CAPTCHA -> {
                if (data.captchaFreezePos != null) {
                    player.teleportTo(data.captchaFreezePos.x, data.captchaFreezePos.y, data.captchaFreezePos.z);
                }
            }
            case ROSTELECOM_SIM -> {
                data.rostelecomTimer++;
                if (data.rostelecomTimer >= 160) { // Every 8 seconds
                    data.rostelecomTimer = 0;
                    Vec3 rollback = data.positionHistory.peekFirst();
                    if (rollback != null) {
                        player.teleportTo(level, rollback.x, rollback.y, rollback.z, player.getYRot(), player.getXRot());
                        level.playSound(null, player.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.6f);
                    }
                }
            }
            case FLOOR_IS_CACTUS -> {
                BlockPos currentPos = player.blockPosition();
                if (currentPos.equals(data.lastBlockPos)) {
                    data.stationaryTicks++;
                    if (data.stationaryTicks >= 30 && data.stationaryTicks % 10 == 0) {
                        player.hurt(level.damageSources().cactus(), 1.0f);
                    }
                } else {
                    data.lastBlockPos = currentPos;
                    data.stationaryTicks = 0;
                }
            }
            case VEGAN_UPRISING -> {
                if (remainingTicks % 10 == 0) {
                    List<Animal> animals = level.getEntitiesOfClass(Animal.class, player.getBoundingBox().inflate(30.0));
                    for (Animal animal : animals) {
                        animal.getNavigation().moveTo(player, 1.45D);
                        if (animal.distanceToSqr(player) < 3.0D) {
                            player.hurt(level.damageSources().mobAttack(animal), 2.0f);
                        }
                    }
                }
            }
            case WEEPING_ANGEL -> {
                List<ArmorStand> stands = level.getEntitiesOfClass(ArmorStand.class, player.getBoundingBox().inflate(35.0),
                        s -> s.getTags().contains("chaos_weeping_angel_" + player.getUUID()));
                for (ArmorStand angel : stands) {
                    Vec3 toAngel = angel.getEyePosition().subtract(player.getEyePosition()).normalize();
                    double dot = player.getLookAngle().normalize().dot(toAngel);
                    if (dot < 0.55) { // Player is NOT looking at the angel
                        Vec3 dir = player.position().subtract(angel.position()).normalize();
                        angel.moveTo(angel.getX() + dir.x * 0.28, player.getY(), angel.getZ() + dir.z * 0.28,
                                (float) (Math.atan2(dir.z, dir.x) * (180 / Math.PI)) - 90f, 0);
                        if (angel.distanceToSqr(player) < 2.2) {
                            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
                            player.hurt(level.damageSources().magic(), 2.0f);
                        }
                    }
                }
            }
            case PRANKSTER_CREEPER -> {
                List<Creeper> creepers = level.getEntitiesOfClass(Creeper.class, player.getBoundingBox().inflate(16.0));
                for (Creeper creeper : creepers) {
                    creeper.getNavigation().moveTo(player, 1.3D);
                    if (creeper.distanceToSqr(player) < 6.0D) {
                        spawnConfettiFirework(level, creeper.position());
                        level.playSound(null, creeper.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8f, 1.5f);
                        player.setDeltaMovement(player.getDeltaMovement().x, 1.15, player.getDeltaMovement().z);
                        player.hurtMarked = true;
                        creeper.discard();
                    }
                }
            }
            case CHICKEN_BOMBER -> {
                if (remainingTicks % 60 == 0) { // Every 3 seconds
                    List<Chicken> chickens = level.getEntitiesOfClass(Chicken.class, player.getBoundingBox().inflate(40.0),
                            c -> c.getTags().contains("chaos_bomber_chicken_" + player.getUUID()));
                    for (Chicken c : chickens) {
                        c.teleportTo(player.getX() + (RANDOM.nextDouble() - 0.5) * 6, player.getY() + 14, player.getZ() + (RANDOM.nextDouble() - 0.5) * 6);
                        PrimedTnt tnt = new PrimedTnt(level, c.getX(), c.getY() - 0.5, c.getZ(), c);
                        tnt.setFuse(50);
                        level.addFreshEntity(tnt);
                    }
                }
            }
            case BUILDER_ZOMBIES -> {
                if (remainingTicks % 20 == 0) {
                    List<Zombie> zombies = level.getEntitiesOfClass(Zombie.class, player.getBoundingBox().inflate(25.0));
                    for (Zombie z : zombies) {
                        if (player.getY() > z.getY() + 1.5) {
                            BlockPos under = z.blockPosition();
                            z.teleportTo(z.getX(), z.getY() + 1.1, z.getZ());
                            if (level.getBlockState(under).isAir()) {
                                level.setBlockAndUpdate(under, Blocks.COBBLESTONE.defaultBlockState());
                            }
                        } else if (z.distanceToSqr(player) < 9.0) {
                            BlockPos trapPos = player.blockPosition().offset(RANDOM.nextInt(3) - 1, RANDOM.nextInt(2), RANDOM.nextInt(3) - 1);
                            if (!trapPos.equals(player.blockPosition()) && level.getBlockState(trapPos).isAir()) {
                                level.setBlockAndUpdate(trapPos, Blocks.COBBLESTONE.defaultBlockState());
                            }
                        }
                    }
                }
            }
            case BLACK_HOLE -> {
                List<Entity> entities = level.getEntities(player, player.getBoundingBox().inflate(25.0),
                        e -> (e instanceof Mob || e instanceof ItemEntity) && e != player);
                for (Entity e : entities) {
                    Vec3 pull = player.position().subtract(e.position()).normalize().scale(0.15);
                    e.setDeltaMovement(e.getDeltaMovement().add(pull.x, pull.y + 0.04, pull.z));
                    e.hurtMarked = true;
                }
            }
            case CHOIR_OF_CONDEMNATION -> {
                List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20.0));
                if (!mobs.isEmpty()) {
                    float nodPitch = (float) (Math.sin(player.tickCount * 0.4) * 35.0);
                    for (Mob mob : mobs) {
                        mob.getNavigation().stop();
                        mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
                        mob.lookAt(player, 180f, 180f);
                        mob.setXRot(nodPitch);
                    }
                    player.causeFoodExhaustion(0.15f);
                }
            }
            case HOT_POTATO -> {
                if (data.hotPotatoCyclesLeft > 0) {
                    boolean hasPotato = hasHotPotatoInInventory(player);
                    if (hasPotato && !data.hotPotatoWaitingReturn) {
                        data.hotPotatoTimer++;
                        if (data.hotPotatoTimer >= 80) { // 4 seconds passed without throwing
                            removeHotPotatoFromInventory(player);
                            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
                            level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1, player.getZ(), 5, 0.3, 0.3, 0.3, 0.05);
                            player.hurt(level.damageSources().explosion(null, null), 6.0f); // 3 hearts
                            data.hotPotatoCyclesLeft--;
                            data.hotPotatoTimer = 0;
                            if (data.hotPotatoCyclesLeft > 0) {
                                data.hotPotatoWaitingReturn = true;
                            }
                        }
                    } else if (!hasPotato && !data.hotPotatoWaitingReturn) {
                        // Player threw it out! Wait 5 seconds before returning
                        data.hotPotatoWaitingReturn = true;
                        data.hotPotatoTimer = 0;
                        data.hotPotatoCyclesLeft--;
                    } else if (data.hotPotatoWaitingReturn) {
                        data.hotPotatoTimer++;
                        if (data.hotPotatoTimer >= 100 && data.hotPotatoCyclesLeft > 0) { // 5 seconds
                            data.hotPotatoWaitingReturn = false;
                            data.hotPotatoTimer = 0;
                            giveHotPotato(player, data);
                        }
                    }
                }
            }
            case GRAVEL_CURSE -> {
                if (player.onGround()) {
                    BlockPos posBelow = player.blockPosition().below();
                    BlockState st = level.getBlockState(posBelow);
                    if (!st.isAir() && !st.is(Blocks.BEDROCK) && !st.is(Blocks.GRAVEL) && !st.is(Blocks.SAND)) {
                        data.pendingGravelBlocks.putIfAbsent(posBelow.immutable(), 20); // 1 second
                    }
                }
            }
            case GLASS_WORLD -> {
                if (remainingTicks % 10 == 0) {
                    BlockPos center = player.blockPosition();
                    for (int dx = -6; dx <= 6; dx++) {
                        for (int dy = -6; dy <= 6; dy++) {
                            for (int dz = -6; dz <= 6; dz++) {
                                BlockPos p = center.offset(dx, dy, dz);
                                BlockState state = level.getBlockState(p);
                                if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)) {
                                    data.glassWorldOriginalBlocks.putIfAbsent(p.immutable(), state);
                                    level.setBlockAndUpdate(p, Blocks.GLASS.defaultBlockState());
                                }
                            }
                        }
                    }
                }
            }
            case OIL_SPILL -> {
                if (player.onGround()) {
                    player.setDeltaMovement(player.getDeltaMovement().multiply(0.4, 1.0, 0.4));
                    level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.2, 0.05, 0.2, 0.01);
                }
            }
            case LASER_GAZE -> {
                Vec3 eye = player.getEyePosition();
                Vec3 end = eye.add(player.getLookAngle().scale(10.0));
                BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos pos = hit.getBlockPos();
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0) {
                        if (pos.equals(data.laserTargetPos)) {
                            data.laserLookTicks++;
                            level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
                            if (data.laserLookTicks >= 30) { // 1.5 seconds
                                level.destroyBlock(pos, true, player);
                                data.laserLookTicks = 0;
                            }
                        } else {
                            data.laserTargetPos = pos.immutable();
                            data.laserLookTicks = 0;
                        }
                    }
                } else {
                    data.laserLookTicks = 0;
                }
            }
            case HUMAN_DRILL -> {
                Vec3 look = player.getLookAngle().normalize();
                BlockPos frontFoot = BlockPos.containing(player.getX() + look.x, player.getY(), player.getZ() + look.z);
                BlockPos frontHead = frontFoot.above();
                for (BlockPos p : new BlockPos[]{frontFoot, frontHead}) {
                    BlockState st = level.getBlockState(p);
                    if (st.is(BlockTags.BASE_STONE_OVERWORLD) || st.is(BlockTags.DIRT) || st.is(BlockTags.SAND)) {
                        level.destroyBlock(p, true, player);
                    }
                }
            }
            case PMC_GOLEM -> {
                if (remainingTicks % 15 == 0) {
                    List<IronGolem> golems = level.getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(40.0),
                            g -> g.getTags().contains("chaos_pmc_golem_" + player.getUUID()));
                    List<Monster> monsters = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(20.0));
                    for (IronGolem golem : golems) {
                        if (!monsters.isEmpty()) {
                            golem.setTarget(monsters.get(0));
                        } else if (golem.distanceToSqr(player) > 16.0) {
                            golem.getNavigation().moveTo(player, 1.3D);
                        }
                    }
                }
            }
            case CRYO_STEP -> {
                BlockPos center = player.blockPosition().below();
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos p = center.offset(dx, 0, dz);
                        BlockState st = level.getBlockState(p);
                        if (st.is(Blocks.WATER)) {
                            level.setBlockAndUpdate(p, Blocks.ICE.defaultBlockState());
                        } else if (st.is(Blocks.LAVA)) {
                            level.setBlockAndUpdate(p, Blocks.SMOOTH_BASALT.defaultBlockState());
                        }
                    }
                }
            }
            case VACUUM_CLEANER -> {
                List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(60.0));
                for (ItemEntity item : items) {
                    item.teleportTo(player.getX(), player.getY(), player.getZ());
                    item.setNoPickUpDelay();
                }
            }
            default -> {}
        }
    }

    private static void giveHotPotato(ServerPlayer player, PlayerChaosData data) {
        ItemStack potato = new ItemStack(Items.BAKED_POTATO);
        potato.setHoverName(Component.literal("🔥 ГОРЯЧАЯ КАРТОШКА (Выкинь за 4 сек!)").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        potato.getOrCreateTag().putBoolean("ChaosHotPotato", true);
        if (!player.getInventory().add(potato)) {
            player.getInventory().setItem(player.getInventory().selected, potato);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private static boolean hasHotPotatoInInventory(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (st.hasTag() && st.getTag().getBoolean("ChaosHotPotato")) {
                return true;
            }
        }
        return false;
    }

    private static void removeHotPotatoFromInventory(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (st.hasTag() && st.getTag().getBoolean("ChaosHotPotato")) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private static void spawnConfettiFirework(ServerLevel level, Vec3 pos) {
        ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag fireworksTag = fireworkStack.getOrCreateTagElement("Fireworks");
        ListTag explosions = new ListTag();
        CompoundTag exp = new CompoundTag();
        exp.putByte("Type", (byte) 1);
        exp.putIntArray("Colors", new int[]{0xFF0000, 0x00FF00, 0x00AAFF, 0xFFFF00, 0xFF00FF});
        exp.putBoolean("Flicker", true);
        explosions.add(exp);
        fireworksTag.put("Explosions", explosions);
        fireworksTag.putByte("Flight", (byte) 0);

        FireworkRocketEntity rocket = new FireworkRocketEntity(level, pos.x, pos.y + 0.5, pos.z, fireworkStack);
        level.addFreshEntity(rocket);
    }

    public static void solveCaptcha(ServerPlayer player) {
        PlayerChaosData data = getData(player);
        if (data.isEventActive(ChaosEvent.SURVIVAL_CAPTCHA)) {
            data.activeEvents.remove(ChaosEvent.SURVIVAL_CAPTCHA);
            data.captchaFreezePos = null;
            player.sendSystemMessage(Component.literal("✔ Капча успешно пройдена! Вы разморожены.").withStyle(ChatFormatting.GREEN));
            syncToPlayer(player);
        }
    }

    public static void onPlayerJumped(ServerPlayer player) {
        PlayerChaosData data = getData(player);
        data.jumpCounter++;

        // Butterfingers: drop held item every 3rd jump
        if (data.isEventActive(ChaosEvent.BUTTERFINGERS) && data.jumpCounter % 3 == 0) {
            player.drop(true);
            player.sendSystemMessage(Component.literal("👐 Дырявые руки! Предмет выпал из рук!").withStyle(ChatFormatting.YELLOW));
        }

        // Air Tax: every 10 jumps delete 1 cobblestone, planks, or torch
        if (data.isEventActive(ChaosEvent.AIR_TAX) && data.jumpCounter % 10 == 0) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack st = player.getInventory().getItem(i);
                if (st.is(Items.COBBLESTONE) || st.is(Items.TORCH) || st.is(net.minecraft.tags.ItemTags.PLANKS)) {
                    st.shrink(1);
                    player.sendSystemMessage(Component.literal("💸 Налог на воздух: списан 1 ресурс за 10 прыжков!").withStyle(ChatFormatting.GOLD));
                    break;
                }
            }
        }
    }

    // ==================== QUEST SYSTEM ====================

    public static void triggerRandomQuestForPlayer(ServerPlayer player) {
        ChaosQuest quest = ChaosQuest.values()[RANDOM.nextInt(ChaosQuest.values().length)];
        startQuestForPlayer(player, quest);
    }

    public static void startQuestForPlayer(ServerPlayer player, ChaosQuest quest) {
        PlayerChaosData data = getData(player);
        data.activeQuest = quest;
        data.questTotalTicks = ChaosConfig.questDurationSeconds * 20;
        data.questRemainingTicks = data.questTotalTicks;
        data.questProgress = 0;
        data.eatenFoodsInQuest.clear();
        data.ignitedEntitiesInQuest.clear();

        if (quest == ChaosQuest.EXTREME_ALPINISM) {
            // If below Y=65, require climbing above Y=110; otherwise require descending below Y=30
            data.questTargetHeightMode = (player.getY() < 65) ? 0 : 1;
        } else if (quest == ChaosQuest.PACIFIST_UNDER_PRESSURE) {
            data.questTotalTicks = Math.max(data.questTotalTicks, 60 * 20);
            data.questRemainingTicks = data.questTotalTicks;
            for (int i = 0; i < 3; i++) {
                Silverfish sf = EntityType.SILVERFISH.create(player.serverLevel());
                if (sf != null) {
                    sf.moveTo(player.getX() + (i - 1) * 1.5, player.getY(), player.getZ() + 1.5, 0, 0);
                    sf.setCustomName(Component.literal("Провокатор"));
                    player.serverLevel().addFreshEntity(sf);
                }
            }
        }

        Component msg = Component.literal("📜 [НОВЫЙ КВЕСТ] ")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                .append(Component.literal("«" + quest.getTitle() + "»: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(quest.getDescription()).withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(msg);
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.2f);
        syncToPlayer(player);
    }

    private static void tickActiveQuest(ServerPlayer player, PlayerChaosData data) {
        ChaosQuest quest = data.activeQuest;
        if (quest == null) return;

        ServerLevel level = player.serverLevel();
        data.questRemainingTicks--;

        switch (quest) {
            case TOUCH_GRASS -> {
                BlockPos below = player.blockPosition().below();
                if (level.getBlockState(below).is(Blocks.GRASS_BLOCK) && level.canSeeSky(player.blockPosition())) {
                    data.questProgress = 1;
                }
            }
            case SACRIFICE_TO_RNG -> {
                List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(10.0));
                for (ItemEntity itemEntity : nearbyItems) {
                    ItemStack st = itemEntity.getItem();
                    if (isValuableSacrificeItem(st.getItem())) {
                        BlockState bs = level.getBlockState(itemEntity.blockPosition());
                        if (itemEntity.isOnFire() || itemEntity.isInLava() || bs.is(Blocks.CACTUS) || bs.is(Blocks.FIRE) || bs.is(Blocks.LAVA)) {
                            itemEntity.discard();
                            data.questProgress = 1;
                            break;
                        }
                    }
                }
            }
            case AGGRESSIVE_VIEWER -> {
                List<Monster> monsters = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(5.5));
                for (Monster m : monsters) {
                    if (m.distanceTo(player) <= 5.0f) {
                        Vec3 toMob = m.getEyePosition().subtract(player.getEyePosition()).normalize();
                        if (player.getLookAngle().normalize().dot(toMob) > 0.92) {
                            data.questProgress++;
                            break;
                        }
                    }
                }
            }
            case DIOGENES -> {
                BlockPos pos = player.blockPosition();
                int light = level.getMaxLocalRawBrightness(pos);
                boolean enclosed = !level.getBlockState(pos.below()).isAir()
                        && !level.getBlockState(pos.above(2)).isAir()
                        && !level.getBlockState(pos.north()).isAir()
                        && !level.getBlockState(pos.south()).isAir()
                        && !level.getBlockState(pos.east()).isAir()
                        && !level.getBlockState(pos.west()).isAir();
                if (enclosed && light == 0 && player.getDeltaMovement().horizontalDistanceSqr() < 0.001) {
                    data.questProgress++;
                }
            }
            case EXTREME_ALPINISM -> {
                if (data.questTargetHeightMode == 0 && player.getY() >= 110) {
                    data.questProgress = 1;
                } else if (data.questTargetHeightMode == 1 && player.getY() <= 30) {
                    data.questProgress = 1;
                }
            }
            case PACIFIST_UNDER_PRESSURE -> {
                data.questProgress++;
            }
            case WATER_PROCEDURES -> {
                if (player.isUnderWater() && player.getAirSupply() < player.getMaxAirSupply()) {
                    data.questProgress++;
                }
            }
            default -> {}
        }

        if (data.questProgress >= quest.getMaxProgress()) {
            completeQuest(player, data);
        } else if (data.questRemainingTicks <= 0) {
            failQuest(player, data);
        }
    }

    private static boolean isValuableSacrificeItem(Item item) {
        String id = ForgeRegistries.ITEMS.getKey(item) != null ? ForgeRegistries.ITEMS.getKey(item).getPath() : "";
        return id.contains("iron") || id.contains("gold") || id.contains("diamond");
    }

    public static void completeQuest(ServerPlayer player, PlayerChaosData data) {
        ChaosQuest completed = data.activeQuest;
        data.activeQuest = null;
        data.questRemainingTicks = 0;
        data.questProgress = 0;

        // Clear Chronic Fatigue punishment if player had it
        if (data.hasChronicFatigue) {
            data.hasChronicFatigue = false;
            AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.removeModifier(CHRONIC_FATIGUE_UUID);
            }
            player.sendSystemMessage(Component.literal("💚 Эффект «Хроническая усталость» снят за выполнение квеста!").withStyle(ChatFormatting.GREEN));
        }

        // Grant random reward
        ChaosReward reward = ChaosReward.values()[RANDOM.nextInt(ChaosReward.values().length)];
        applyReward(player, reward);

        Component msg = Component.literal("🏆 [КВЕСТ ВЫПОЛНЕН] ")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                .append(Component.literal("«" + completed.getTitle() + "»! ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("Награда: " + reward.getTitle() + " — " + reward.getDescription()).withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(msg);
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        syncToPlayer(player);
    }

    private static void applyReward(ServerPlayer player, ChaosReward reward) {
        switch (reward) {
            case ROYAL_TREASURE -> {
                giveOrDrop(player, new ItemStack(Items.DIAMOND, 3));
                giveOrDrop(player, new ItemStack(Items.GOLDEN_APPLE, 2));
                giveOrDrop(player, new ItemStack(Items.IRON_INGOT, 8));
            }
            case GUARDIAN_TOTEM -> {
                giveOrDrop(player, new ItemStack(Items.TOTEM_OF_UNDYING, 1));
                giveOrDrop(player, new ItemStack(Items.ENDER_PEARL, 4));
            }
            case HERO_BLESSING -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 90, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 90, 1));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 90, 1));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 90, 0));
                player.giveExperienceLevels(5);
            }
            case NETHERITE_BONUS -> {
                giveOrDrop(player, new ItemStack(Items.NETHERITE_INGOT, 1));
                giveOrDrop(player, new ItemStack(Items.GOLDEN_CARROT, 16));
                giveOrDrop(player, new ItemStack(Items.BAKED_POTATO, 16));
            }
            case FATE_GIFT -> {
                ChaosEvent[] blessings = Arrays.stream(ChaosEvent.values())
                        .filter(e -> e.getCategory() == EventCategory.BLESSINGS)
                        .toArray(ChaosEvent[]::new);
                ChaosEvent chosen = blessings[RANDOM.nextInt(blessings.length)];
                triggerEventForPlayer(player, chosen);
                giveOrDrop(player, new ItemStack(Items.DIAMOND, 2));
            }
        }
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public static void failQuest(ServerPlayer player, PlayerChaosData data) {
        ChaosQuest failed = data.activeQuest;
        data.activeQuest = null;
        data.questRemainingTicks = 0;
        data.questProgress = 0;

        ChaosPunishment punishment = ChaosPunishment.values()[RANDOM.nextInt(ChaosPunishment.values().length)];
        applyPunishment(player, data, punishment);

        Component msg = Component.literal("💀 [КВЕСТ ПРОВАЛЕН] ")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                .append(Component.literal("«" + failed.getTitle() + "»! ").withStyle(ChatFormatting.RED))
                .append(Component.literal("Наказание: " + punishment.getTitle() + " — " + punishment.getDescription()).withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(msg);
        player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.7f, 0.9f);
        syncToPlayer(player);
    }

    private static void applyPunishment(ServerPlayer player, PlayerChaosData data, ChaosPunishment punishment) {
        ServerLevel level = player.serverLevel();
        switch (punishment) {
            case CONFISCATION -> {
                boolean hadArmor = false;
                for (int i = 0; i < player.getInventory().armor.size(); i++) {
                    if (!player.getInventory().armor.get(i).isEmpty()) {
                        player.getInventory().armor.set(i, ItemStack.EMPTY);
                        hadArmor = true;
                    }
                }
                if (!hadArmor) {
                    for (int i = 0; i < 9; i++) {
                        if (!player.getInventory().getItem(i).isEmpty()) {
                            player.getInventory().setItem(i, ItemStack.EMPTY);
                            break;
                        }
                    }
                }
                player.inventoryMenu.broadcastChanges();
            }
            case SOLITARY_CELL -> {
                BlockPos center = player.blockPosition();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            BlockPos p = center.offset(dx, dy, dz);
                            boolean isWall = Math.abs(dx) == 1 || Math.abs(dz) == 1 || dy == -1 || dy == 2;
                            if (isWall) {
                                level.setBlockAndUpdate(p, Blocks.OBSIDIAN.defaultBlockState());
                            } else {
                                level.setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
                            }
                        }
                    }
                }
                Pufferfish puffer = EntityType.PUFFERFISH.create(level);
                if (puffer != null) {
                    puffer.moveTo(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 0, 0);
                    puffer.setCustomName(Component.literal("Сокамерник"));
                    level.addFreshEntity(puffer);
                }
            }
            case SPACE_PROGRAM -> {
                player.teleportTo(level, player.getX(), player.getY() + 150.0, player.getZ(), player.getYRot(), player.getXRot());
                level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.8f);
            }
            case CHRONIC_FATIGUE -> {
                data.hasChronicFatigue = true;
                AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.removeModifier(CHRONIC_FATIGUE_UUID);
                    maxHealth.addPermanentModifier(new AttributeModifier(
                            CHRONIC_FATIGUE_UUID, "Chaos Chronic Fatigue", -14.0D, AttributeModifier.Operation.ADDITION));
                    if (player.getHealth() > 6.0f) {
                        player.setHealth(6.0f);
                    }
                }
            }
            case PUNITIVE_SQUAD -> {
                for (int i = 0; i < 4; i++) {
                    Vindicator v = EntityType.VINDICATOR.create(level);
                    if (v != null) {
                        double angle = (Math.PI / 2) * i;
                        v.moveTo(player.getX() + Math.cos(angle) * 4, player.getY(), player.getZ() + Math.sin(angle) * 4, 0, 0);
                        v.setCustomName(Component.literal("Коллектор").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                        v.setCustomNameVisible(true);
                        v.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
                        level.addFreshEntity(v);
                    }
                }
            }
        }
    }

    // ==================== FORGE GAMEPLAY EVENT HOOKS ====================

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        Entity sourceEntity = event.getSource().getEntity();
        Entity directEntity = event.getSource().getDirectEntity();

        // Check if target is a ServerPlayer
        if (target instanceof ServerPlayer player) {
            PlayerChaosData data = getData(player);

            // Immortality on Credit
            if (data.isEventActive(ChaosEvent.IMMORTALITY_CREDIT)) {
                event.setCanceled(true);
                return;
            }

            // Bailiff steals an item instead of dealing damage
            if (sourceEntity != null && sourceEntity.getTags().contains("chaos_bailiff_" + player.getUUID())) {
                event.setAmount(0.0f);
                List<Integer> nonEmptySlots = new ArrayList<>();
                for (int i = 0; i < 36; i++) {
                    if (!player.getInventory().getItem(i).isEmpty()) {
                        nonEmptySlots.add(i);
                    }
                }
                if (!nonEmptySlots.isEmpty()) {
                    int slot = nonEmptySlots.get(RANDOM.nextInt(nonEmptySlots.size()));
                    ItemStack stolen = player.getInventory().removeItemNoUpdate(slot);
                    data.bailiffStolenItems.computeIfAbsent(sourceEntity.getUUID(), k -> new ArrayList<>()).add(stolen);
                    player.sendSystemMessage(Component.literal("👮 Судебный пристав изъял: " + stolen.getHoverName().getString() + "! Убейте его, чтобы вернуть!").withStyle(ChatFormatting.RED));
                    player.inventoryMenu.broadcastChanges();
                }
            }

            // Cosmonaut Skeletons apply Levitation V for 4 seconds
            if (sourceEntity != null && sourceEntity.getTags().contains("chaos_cosmonaut_skeleton")) {
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80, 4));
            }

            // Knockback 100 when player is hit
            if (data.isEventActive(ChaosEvent.KNOCKBACK_100) && sourceEntity != null) {
                Vec3 dir = player.position().subtract(sourceEntity.position()).normalize().scale(3.2);
                player.setDeltaMovement(dir.x, 0.5, dir.z);
                player.hurtMarked = true;
            }
        }

        // Check if attacker is a ServerPlayer
        if (sourceEntity instanceof ServerPlayer attacker) {
            PlayerChaosData data = getData(attacker);

            // Quest: Pacifist under pressure fails if player deals any damage
            if (data.activeQuest == ChaosQuest.PACIFIST_UNDER_PRESSURE) {
                failQuest(attacker, data);
                return;
            }

            // Quest: Sniper (hit mob from >= 15 blocks with snowball, egg, or arrow)
            if (data.activeQuest == ChaosQuest.SNIPER && target instanceof Mob) {
                if (directEntity instanceof Snowball || directEntity instanceof ThrownEgg || directEntity instanceof AbstractArrow) {
                    if (attacker.distanceTo(target) >= 15.0f) {
                        data.questProgress = 1;
                        completeQuest(attacker, data);
                    }
                }
            }

            // Quest: Pyromaniac (ignite 3 entities with flaming arrow)
            if (data.activeQuest == ChaosQuest.PYROMANIAC && directEntity instanceof Projectile proj && proj.isOnFire()) {
                data.ignitedEntitiesInQuest.add(target.getUUID());
                data.questProgress = data.ignitedEntitiesInQuest.size();
            }

            // Knockback 100 when player hits target
            if (data.isEventActive(ChaosEvent.KNOCKBACK_100)) {
                Vec3 dir = target.position().subtract(attacker.position()).normalize().scale(3.2);
                target.setDeltaMovement(dir.x, 0.5, dir.z);
                target.hurtMarked = true;
            }

            // Reverse Psychology: weapons heal mobs 100%, empty hand hurts
            if (data.isEventActive(ChaosEvent.REVERSE_PSYCHOLOGY) && target instanceof Mob) {
                ItemStack held = attacker.getMainHandItem();
                if (!held.isEmpty()) {
                    target.heal(event.getAmount());
                    event.setCanceled(true);
                    attacker.sendSystemMessage(Component.literal("🔄 Обратная психология: оружие вылечило моба! Бейте пустой рукой!").withStyle(ChatFormatting.LIGHT_PURPLE));
                } else {
                    event.setAmount(6.0f);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerChaosData data = getData(player);

            // Jupiter Gravity: falling even 2 blocks deals damage
            if (data.isEventActive(ChaosEvent.JUPITER_GRAVITY) && event.getDistance() >= 2.0f) {
                event.setDistance(event.getDistance() + 3.0f);
                event.setDamageMultiplier(1.5f);
            }

            // Quest: Leap of Faith (take 3 to 6 hearts = 6 to 12 HP fall damage and survive)
            if (data.activeQuest == ChaosQuest.LEAP_OF_FAITH) {
                float approxDamage = (event.getDistance() - 3.0f) * event.getDamageMultiplier();
                if (approxDamage >= 6.0f && approxDamage <= 13.0f && player.getHealth() > approxDamage) {
                    data.questProgress = 1;
                    completeQuest(player, data);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerChaosData data = getData(player);
            if (data.isEventActive(ChaosEvent.SPRING_JUMP)) {
                player.setDeltaMovement(player.getDeltaMovement().x, 1.55D, player.getDeltaMovement().z);
                player.hurtMarked = true;
            } else if (data.isEventActive(ChaosEvent.JUPITER_GRAVITY)) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
                player.hurtMarked = true;
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Return Bailiff stolen items on death
        for (ServerPlayer player : entity.level().getServer().getPlayerList().getPlayers()) {
            PlayerChaosData data = getData(player);
            List<ItemStack> stolen = data.bailiffStolenItems.remove(entity.getUUID());
            if (stolen != null) {
                for (ItemStack st : stolen) {
                    entity.spawnAtLocation(st);
                }
                player.sendSystemMessage(Component.literal("💰 Вы убили пристава и вернули украденные вещи!").withStyle(ChatFormatting.GREEN));
            }
        }

        // Pocket Blacksmith: animals drop cooked meat
        if (event.getSource().getEntity() instanceof ServerPlayer killer && entity instanceof Animal) {
            PlayerChaosData data = getData(killer);
            if (data.isEventActive(ChaosEvent.POCKET_BLACKSMITH)) {
                entity.spawnAtLocation(new ItemStack(Items.COOKED_BEEF, 2));
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        PlayerChaosData data = getData(player);
        ServerLevel level = player.serverLevel();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        // Slum Midas: broken blocks drop rotten flesh
        if (data.isEventActive(ChaosEvent.SLUM_MIDAS)) {
            event.setCanceled(true);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            Block.popResource(level, pos, new ItemStack(Items.ROTTEN_FLESH, 1));
            return;
        }

        // Minefield: 15% chance to spawn primed TNT with 1.5s fuse instead of drop
        if (data.isEventActive(ChaosEvent.MINEFIELD) && RANDOM.nextFloat() < 0.15f) {
            event.setCanceled(true);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            PrimedTnt tnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, player);
            tnt.setFuse(30); // 1.5 seconds
            level.addFreshEntity(tnt);
            level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0f, 1.2f);
            return;
        }

        // Pocket Blacksmith: ores drop double smelted ingots
        if (data.isEventActive(ChaosEvent.POCKET_BLACKSMITH)) {
            ItemStack smelted = getSmeltedOreResult(state);
            if (!smelted.isEmpty()) {
                event.setCanceled(true);
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                Block.popResource(level, pos, smelted);
            }
        }
    }

    private static ItemStack getSmeltedOreResult(BlockState state) {
        if (state.is(BlockTags.IRON_ORES)) return new ItemStack(Items.IRON_INGOT, 2);
        if (state.is(BlockTags.GOLD_ORES)) return new ItemStack(Items.GOLD_INGOT, 2);
        if (state.is(BlockTags.COPPER_ORES)) return new ItemStack(Items.COPPER_INGOT, 4);
        if (state.is(BlockTags.DIAMOND_ORES)) return new ItemStack(Items.DIAMOND, 2);
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerChaosData data = getData(player);
        ServerLevel level = player.serverLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // Furniture Rebellion: chests, furnaces, crafting tables jump and run away 2 blocks
        if (data.isEventActive(ChaosEvent.FURNITURE_REBELLION)) {
            if (state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE)
                    || state.is(Blocks.SMOKER) || state.is(Blocks.CHEST) || state.is(Blocks.BARREL)) {
                event.setCanceled(true);
                Vec3 away = Vec3.atCenterOf(pos).subtract(player.position()).normalize().scale(2.0);
                BlockPos targetPos = pos.offset((int) Math.round(away.x), 1, (int) Math.round(away.z));
                while (targetPos.getY() > level.getMinBuildHeight() + 1 && level.getBlockState(targetPos.below()).isAir()) {
                    targetPos = targetPos.below();
                }
                if (level.getBlockState(targetPos).isAir() && level.getBlockEntity(pos) == null) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(targetPos, state);
                }
                level.playSound(null, pos, SoundEvents.SLIME_JUMP, SoundSource.BLOCKS, 1.0f, 1.2f);
                player.sendSystemMessage(Component.literal("🪑 Бунт мебели! Блок сбежал от вас!").withStyle(ChatFormatting.GOLD));
            }
        }

        // Quest: Pyromaniac (using flint & steel or lava bucket)
        if (data.activeQuest == ChaosQuest.PYROMANIAC) {
            ItemStack held = event.getItemStack();
            if (held.is(Items.FLINT_AND_STEEL) || held.is(Items.LAVA_BUCKET) || held.is(Items.FIRE_CHARGE)) {
                List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3.0), e -> e != player);
                for (LivingEntity e : nearby) {
                    e.setSecondsOnFire(5);
                    data.ignitedEntitiesInQuest.add(e.getUUID());
                }
                data.questProgress = data.ignitedEntitiesInQuest.size();
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerChaosData data = getData(player);
        if (data.activeQuest == ChaosQuest.PYROMANIAC && event.getTarget() instanceof LivingEntity target) {
            ItemStack held = event.getItemStack();
            if (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE)) {
                target.setSecondsOnFire(5);
                data.ignitedEntitiesInQuest.add(target.getUUID());
                data.questProgress = data.ignitedEntitiesInQuest.size();
                if (data.questProgress >= ChaosQuest.PYROMANIAC.getMaxProgress()) {
                    completeQuest(player, data);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerChaosData data = getData(player);
        if (data.activeQuest == ChaosQuest.INTERIOR_DESIGNER) {
            BlockState placed = event.getPlacedBlock();
            if (placed.is(BlockTags.BEDS) && !placed.is(Blocks.WHITE_BED)) {
                data.questProgress = 1;
                completeQuest(player, data);
            }
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerChaosData data = getData(player);
        if (data.isEventActive(ChaosEvent.CRAFT_RANDOMIZER)) {
            List<Item> allItems = new ArrayList<>(ForgeRegistries.ITEMS.getValues());
            if (!allItems.isEmpty()) {
                Item randomItem = allItems.get(RANDOM.nextInt(allItems.size()));
                ItemStack crafted = event.getCrafting();
                ItemStack replacement = new ItemStack(randomItem, Math.max(1, crafted.getCount()));
                crafted.setCount(0);
                giveOrDrop(player, replacement);
            }
        }
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem().getItem();
        if (stack.hasTag() && stack.getTag().getBoolean("ChaosFakeDiamond")) {
            event.setCanceled(true);
            event.getItem().discard();
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0f, 0.9f);
            player.sendSystemMessage(Component.literal("💎 Фальшивка с AliExpress рассыпалась в руках!").withStyle(ChatFormatting.GRAY));
        }
    }

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack item = event.getItem();
        if (item.isEdible()) {
            PlayerChaosData data = getData(player);
            if (data.isEventActive(ChaosEvent.SOUND_SCHIZOPHRENIA)) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.GHAST_SCREAM, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            if (data.activeQuest == ChaosQuest.TASTER) {
                data.eatenFoodsInQuest.add(item.getItem());
                data.questProgress = data.eatenFoodsInQuest.size();
                if (data.questProgress >= ChaosQuest.TASTER.getMaxProgress()) {
                    completeQuest(player, data);
                }
            }
        }
    }

    public static void syncToPlayer(ServerPlayer player) {
        PlayerChaosData data = getData(player);
        Map<Integer, Integer> activeMap = new HashMap<>();
        for (Map.Entry<ChaosEvent, Integer> e : data.activeEvents.entrySet()) {
            activeMap.put(e.getKey().ordinal(), e.getValue());
        }

        int questOrd = data.activeQuest != null ? data.activeQuest.ordinal() : -1;
        int maxProg = data.activeQuest != null ? data.activeQuest.getMaxProgress() : 1;

        S2CSyncStatePacket packet = new S2CSyncStatePacket(
                ChaosConfig.modMode.ordinal(),
                ChaosConfig.eventIntervalSeconds,
                ChaosConfig.questIntervalSeconds,
                ChaosConfig.questDurationSeconds,
                ChaosConfig.getEnabledBitmask(),
                nextEventTicks,
                nextQuestTicks,
                activeMap,
                questOrd,
                data.questRemainingTicks,
                data.questTotalTicks,
                data.questProgress,
                maxProg,
                data.getPlayerScale()
        );
        ChaosNetwork.sendToPlayer(player, packet);
    }
}
