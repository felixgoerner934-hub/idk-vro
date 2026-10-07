package com.spawnerbeacon.spawner;

import com.spawnerbeacon.BeaconConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SpawnerTracker {
    private static final Map<BlockPos, SpawnerInfo> SPAWNERS = new ConcurrentHashMap<>();
    private static volatile ClientLevel currentLevel;

    private SpawnerTracker() {}

    public static void register() {
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, level) -> {
            if (be instanceof SpawnerBlockEntity spawner) add(spawner, level);
        });
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, level) -> {
            if (be instanceof SpawnerBlockEntity) SPAWNERS.remove(be.getBlockPos());
        });
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((minecraft, newLevel) -> {
            SPAWNERS.clear();
            currentLevel = newLevel;
        });
    }

    private static void add(SpawnerBlockEntity spawner, ClientLevel level) {
        currentLevel = level;
        String type = typeOf(spawner, level);
        SPAWNERS.put(spawner.getBlockPos().immutable(), new SpawnerInfo(spawner.getBlockPos().immutable(), type));
    }

    private static String typeOf(SpawnerBlockEntity spawner, ClientLevel level) {
        try {
            Entity entity = spawner.getSpawner().getOrCreateDisplayEntity(level, spawner.getBlockPos());
            if (entity != null) return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        } catch (Exception ignored) {}
        return "other";
    }

    public static List<SpawnerInfo> snapshot() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level != currentLevel) return List.of();
        SPAWNERS.entrySet().removeIf(e -> {
            BlockEntity be = level.getBlockEntity(e.getKey());
            return !(be instanceof SpawnerBlockEntity) || be.isRemoved();
        });
        return List.copyOf(SPAWNERS.values());
    }

    public static List<SpawnerInfo> sortedByDistance() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        List<SpawnerInfo> list = new ArrayList<>(snapshot());
        list.sort(Comparator.comparingDouble(s -> s.pos().distToCenterSqr(camera.x, camera.y, camera.z)));
        return list;
    }
}
