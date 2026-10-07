package com.spawnerbeacon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client configuration. Colors are stored as integers; the GUI never requires hex input. */
public final class BeaconConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final List<String> TYPES = List.of("zombie", "skeleton", "spider", "cave_spider", "blaze", "silverfish", "magma_cube", "husk", "stray", "slime", "creeper", "other");

    private static final Map<String, Integer> DEFAULT_COLORS = Map.ofEntries(
            Map.entry("zombie", 0x55FF55), Map.entry("skeleton", 0xF4F1E8), Map.entry("spider", 0xE94B5F),
            Map.entry("cave_spider", 0x48D9D0), Map.entry("blaze", 0xFF9D32), Map.entry("silverfish", 0xA6A6A6),
            Map.entry("magma_cube", 0xFF5A36), Map.entry("husk", 0xD2B48C), Map.entry("stray", 0xA9D9FF),
            Map.entry("slime", 0x91E65A), Map.entry("creeper", 0x58C94C), Map.entry("other", 0xE76F9A));

    public boolean enabled = true;
    public double thickness = 1.0;
    public double opacity = 0.78;
    public int maxY = 300;
    public int renderDistanceChunks = 12;
    public boolean animate = true;
    public boolean distanceFade = true;
    public boolean labels = false;
    public boolean tracers = false;
    public boolean outline = true;
    public boolean rainbow = false;
    public boolean overworld = true;
    public boolean nether = true;
    public boolean end = true;
    public int defaultColor = 0xF5A3B7;
    public Map<String, Integer> typeColors = new LinkedHashMap<>();
    public Map<String, Boolean> typeEnabled = new LinkedHashMap<>();
    public Map<String, Double> typeThickness = new LinkedHashMap<>();

    private static BeaconConfig instance;

    public static BeaconConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("spawnerbeacon.json");
    }

    public static BeaconConfig load() {
        BeaconConfig cfg = null;
        try {
            Path f = file();
            if (Files.exists(f)) cfg = GSON.fromJson(Files.readString(f), BeaconConfig.class);
        } catch (Exception e) {
            System.err.println("[SpawnerBeacon] Config konnte nicht gelesen werden: " + e);
        }
        if (cfg == null) cfg = new BeaconConfig();
        cfg.sanitize();
        instance = cfg;
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[SpawnerBeacon] Config konnte nicht gespeichert werden: " + e);
        }
    }

    public void resetToDefaults() {
        BeaconConfig d = new BeaconConfig();
        this.enabled = d.enabled; this.thickness = d.thickness; this.opacity = d.opacity; this.maxY = d.maxY;
        this.renderDistanceChunks = d.renderDistanceChunks; this.animate = d.animate; this.distanceFade = d.distanceFade;
        this.labels = d.labels; this.tracers = d.tracers; this.outline = d.outline; this.rainbow = d.rainbow;
        this.overworld = d.overworld; this.nether = d.nether; this.end = d.end; this.defaultColor = d.defaultColor;
        this.typeColors = new LinkedHashMap<>(d.typeColors); this.typeEnabled = new LinkedHashMap<>(d.typeEnabled);
        this.typeThickness = new LinkedHashMap<>(d.typeThickness);
        save();
    }

    private void sanitize() {
        if (typeColors == null) typeColors = new LinkedHashMap<>();
        if (typeEnabled == null) typeEnabled = new LinkedHashMap<>();
        if (typeThickness == null) typeThickness = new LinkedHashMap<>();
        for (String type : TYPES) {
            typeColors.putIfAbsent(type, DEFAULT_COLORS.get(type));
            typeEnabled.putIfAbsent(type, true);
            typeThickness.putIfAbsent(type, 1.0);
            typeColors.put(type, typeColors.get(type) & 0xFFFFFF);
            typeThickness.put(type, clamp(typeThickness.get(type), 0.1, 5.0));
        }
        defaultColor &= 0xFFFFFF;
        thickness = clamp(thickness, 0.1, 5.0);
        opacity = clamp(opacity, 0.05, 1.0);
        maxY = Math.max(64, Math.min(512, maxY));
        renderDistanceChunks = Math.max(2, Math.min(32, renderDistanceChunks));
    }

    private static double clamp(double v, double min, double max) {
        return Double.isFinite(v) ? Math.max(min, Math.min(max, v)) : min;
    }

    public boolean dimensionEnabled(net.minecraft.client.multiplayer.ClientLevel level) {
        if (level == null) return false;
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) return overworld;
        if (level.dimension() == net.minecraft.world.level.Level.NETHER) return nether;
        if (level.dimension() == net.minecraft.world.level.Level.END) return end;
        return true;
    }

    public boolean typeEnabled(String type) { return typeEnabled.getOrDefault(type, true); }
    public int colorFor(String type) { return typeColors.getOrDefault(type, defaultColor) & 0xFFFFFF; }
    public double thicknessFor(String type) { return typeThickness.getOrDefault(type, thickness); }
}
