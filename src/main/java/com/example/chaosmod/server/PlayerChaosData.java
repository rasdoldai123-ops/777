package com.example.chaosmod.server;

import com.example.chaosmod.event.ChaosEvent;
import com.example.chaosmod.quest.ChaosQuest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class PlayerChaosData {
    // Active events -> remaining ticks
    public final Map<ChaosEvent, Integer> activeEvents = new EnumMap<>(ChaosEvent.class);

    // Active quest state
    public ChaosQuest activeQuest = null;
    public int questRemainingTicks = 0;
    public int questTotalTicks = 0;
    public int questProgress = 0;
    public int questTargetHeightMode = 0; // 0 = climb >110, 1 = descend <30
    public final Set<Item> eatenFoodsInQuest = new HashSet<>();
    public final Set<UUID> ignitedEntitiesInQuest = new HashSet<>();

    // Rostelecom Simulator (Ping 999) history buffer (last 40 ticks of positions)
    public final Deque<Vec3> positionHistory = new ArrayDeque<>();
    public int rostelecomTimer = 0;

    // Floor is Cactus stationary tracker
    public BlockPos lastBlockPos = null;
    public int stationaryTicks = 0;

    // Butterfingers & Air Tax jump counter
    public int jumpCounter = 0;
    public double lastY = 0;
    public boolean wasOnGround = true;

    // Hot Potato state
    public int hotPotatoCyclesLeft = 0;
    public int hotPotatoTimer = 0;
    public boolean hotPotatoWaitingReturn = false;

    // Gravel Curse delayed blocks: BlockPos -> ticks until turn to gravel/sand
    public final Map<BlockPos, Integer> pendingGravelBlocks = new HashMap<>();

    // Sanctions stored items (6 slots of hotbar)
    public final Map<Integer, ItemStack> sanctionedItems = new HashMap<>();

    // Expired food stored items (slot -> original food stack)
    public final Map<Integer, ItemStack> expiredFoodBackup = new HashMap<>();

    // Glass world original blocks (BlockPos -> original BlockState)
    public final Map<BlockPos, BlockState> glassWorldOriginalBlocks = new HashMap<>();

    // Bailiff stolen items (Zombie Villager UUID -> stolen stacks)
    public final Map<UUID, List<ItemStack>> bailiffStolenItems = new HashMap<>();

    // Laser Gaze tracking
    public BlockPos laserTargetPos = null;
    public int laserLookTicks = 0;

    // Survival Captcha freeze coordinates
    public Vec3 captchaFreezePos = null;

    // Chronic Fatigue punishment active flag
    public boolean hasChronicFatigue = false;

    // Spawned temporary entities to clean up if needed (Weeping Angel, Golems, Chicken Bombers)
    public final List<UUID> spawnedEventEntities = new ArrayList<>();

    public boolean isEventActive(ChaosEvent event) {
        return activeEvents.getOrDefault(event, 0) > 0;
    }

    public float getPlayerScale() {
        if (isEventActive(ChaosEvent.CLAUSTROPHOBIA)) {
            return 2.0f;
        }
        if (isEventActive(ChaosEvent.LILLIPUTIAN)) {
            return 0.25f;
        }
        return 1.0f;
    }
}
