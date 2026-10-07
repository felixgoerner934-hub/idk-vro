package com.spawnerbeacon.gui;

import com.spawnerbeacon.BeaconConfig;
import com.spawnerbeacon.SpawnerBeaconClient;
import com.spawnerbeacon.spawner.SpawnerInfo;
import com.spawnerbeacon.spawner.SpawnerTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * Cherry Blossom themed client configuration. The background is deliberately rendered
 * before the widgets so the interactive controls are never painted over.
 */
public final class ConfigScreen extends Screen {
    private static final int CHERRY = 0xFFE76F9A;
    private static final int CHERRY_DARK = 0xFF9F3E61;
    private static final int INK = 0xFF3A2930;
    private static final int MUTED = 0xFF816771;
    private static final int PANEL = 0xFFFDF8FA;
    private static final int PANEL_ALT = 0xFFF7EEF1;
    private static final int LINE = 0xFFE2C8D1;
    private static final int GREEN = 0xFF5E9A73;
    private static final int RED = 0xFFB84A67;
    private static final int[] PALETTE = {
            0xE76F9A, 0xF5A3B7, 0xB93B60, 0xFF7D9D, 0xFF9F68, 0xFF9D32,
            0xF3C969, 0x91E65A, 0x48D9D0, 0x5AA9E6, 0x8D6BFF, 0xB06BE8,
            0xF4F1E8, 0xA6A6A6, 0x4A343B
    };
    private static final List<String> TABS = List.of("general", "colors", "spawners", "list");
    private static final Identifier BRANCH = Identifier.fromNamespaceAndPath(SpawnerBeaconClient.MOD_ID, "textures/gui/cherry_branch.png");
    private static final Identifier BRANCH_MIRROR = Identifier.fromNamespaceAndPath(SpawnerBeaconClient.MOD_ID, "textures/gui/cherry_branch_mirror.png");
    private static final Identifier CAR = Identifier.fromNamespaceAndPath(SpawnerBeaconClient.MOD_ID, "textures/gui/cherry_car.png");

    private final BeaconConfig cfg = BeaconConfig.get();
    private String tab = "general";
    private String selectedType = "zombie";
    private int listScroll;
    private boolean dirty;
    private long lastChange;
    private final long openedAt = System.nanoTime();

    public ConfigScreen() {
        super(Component.translatable("screen.spawnerbeacon.title"));
    }

    private int panelLeft() { return Math.max(28, (width - Math.min(1080, width - 56)) / 2); }
    private int panelWidth() { return Math.min(1080, width - 56); }
    private int panelRight() { return panelLeft() + panelWidth(); }
    private int panelTop() { return Math.max(28, (height - Math.min(700, height - 56)) / 2); }
    private int panelBottom() { return panelTop() + Math.min(700, height - 56); }
    private int sidebarW() { return 182; }
    private int contentLeft() { return panelLeft() + sidebarW() + 24; }
    private int contentRight() { return panelRight() - 26; }
    private int contentTop() { return panelTop() + 104; }

    @Override
    protected void init() {
        clearWidgets();
        int left = panelLeft();
        int top = panelTop();
        int sideX = left + 18;
        int sideY = top + 126;

        for (String t : TABS) {
            int y = sideY + TABS.indexOf(t) * 42;
            addRenderableWidget(new CherryButton(sideX, y, sidebarW() - 36, 34,
                    Component.translatable("screen.spawnerbeacon.tab." + t), () -> {
                        tab = t;
                        listScroll = 0;
                        rebuildWidgets();
                    }));
        }

        switch (tab) {
            case "general" -> initGeneral();
            case "colors" -> initColors();
            case "spawners" -> initSpawners();
            case "list" -> initList();
        }

        addRenderableWidget(new CherryButton(contentLeft(), panelBottom() - 48, 170, 28,
                Component.translatable("screen.spawnerbeacon.reset"), () -> {
                    cfg.resetToDefaults();
                    dirty = false;
                    rebuildWidgets();
                }));
        addRenderableWidget(new CherryButton(contentRight() - 170, panelBottom() - 48, 170, 28,
                Component.translatable("gui.done"), this::onClose));
    }

    private void initGeneral() {
        int x = contentLeft();
        int w = contentRight() - x;
        int half = (w - 12) / 2;
        int y = contentTop();

        addRenderableWidget(new CherryButton(x, y, half, 38,
                Component.translatable(cfg.enabled ? "screen.spawnerbeacon.enabled_on" : "screen.spawnerbeacon.enabled_off"), () -> {
                    cfg.enabled = !cfg.enabled; markChanged(); rebuildWidgets();
                }));
        addRenderableWidget(new CherryButton(x + half + 12, y, half, 38,
                Component.translatable("screen.spawnerbeacon.animation", cfg.animate ? "AN" : "AUS"), () -> {
                    cfg.animate = !cfg.animate; markChanged(); rebuildWidgets();
                }));
        y += 62;

        addRenderableWidget(new CherrySlider(x, y, w, 30, tr("screen.spawnerbeacon.thickness"), 0.1, 5.0,
                cfg.thickness, "%.2f", v -> { cfg.thickness = v; markChanged(); }));
        y += 52;
        addRenderableWidget(new CherrySlider(x, y, w, 30, tr("screen.spawnerbeacon.opacity"), 0.05, 1.0,
                cfg.opacity, "%.2f", v -> { cfg.opacity = v; markChanged(); }));
        y += 52;
        addRenderableWidget(new CherrySlider(x, y, w, 30, tr("screen.spawnerbeacon.height"), 64, 512,
                cfg.maxY, "%.0f", v -> { cfg.maxY = (int) Math.round(v); markChanged(); }));
        y += 52;
        addRenderableWidget(new CherrySlider(x, y, w, 30, tr("screen.spawnerbeacon.distance"), 2, 32,
                cfg.renderDistanceChunks, "%.0f chunks", v -> { cfg.renderDistanceChunks = (int) Math.round(v); markChanged(); }));
        y += 52;

        addRenderableWidget(new CherryButton(x, y, half, 32,
                Component.translatable("screen.spawnerbeacon.fade", cfg.distanceFade ? "AN" : "AUS"), () -> {
                    cfg.distanceFade = !cfg.distanceFade; markChanged(); rebuildWidgets();
                }));
        addRenderableWidget(new CherryButton(x + half + 12, y, half, 32,
                Component.translatable("screen.spawnerbeacon.rainbow", cfg.rainbow ? "AN" : "AUS"), () -> {
                    cfg.rainbow = !cfg.rainbow; markChanged(); rebuildWidgets();
                }));
        y += 46;

        addRenderableWidget(new CherryButton(x, y, half, 32,
                Component.translatable("screen.spawnerbeacon.dimension_overworld", cfg.overworld ? "AN" : "AUS"), () -> {
                    cfg.overworld = !cfg.overworld; markChanged(); rebuildWidgets();
                }));
        addRenderableWidget(new CherryButton(x + half + 12, y, half, 32,
                Component.translatable("screen.spawnerbeacon.dimension_nether", cfg.nether ? "AN" : "AUS"), () -> {
                    cfg.nether = !cfg.nether; markChanged(); rebuildWidgets();
                }));
        y += 42;
        addRenderableWidget(new CherryButton(x, y, half, 32,
                Component.translatable("screen.spawnerbeacon.dimension_end", cfg.end ? "AN" : "AUS"), () -> {
                    cfg.end = !cfg.end; markChanged(); rebuildWidgets();
                }));
    }

    private void initColors() {
        int x = contentLeft();
        int w = contentRight() - x;
        int y = contentTop();
        int sw = 30;

        addRenderableWidget(new CherryButton(x, y, 210, 32,
                Component.translatable("screen.spawnerbeacon.global_color"), () -> {
                    selectedType = "other"; rebuildWidgets();
                }));
        y += 48;

        for (int i = 0; i < PALETTE.length; i++) {
            int row = i / 7, col = i % 7;
            addRenderableWidget(new ColorSwatch(x + col * (sw + 8), y + row * (sw + 8), sw, PALETTE[i], c -> setSelectedColor(c)));
        }
        y += 3 * (sw + 8) + 24;

        addRenderableWidget(new CherrySlider(x, y, w, 30, "R", 0, 255, red(selectedColor()), "%.0f", v -> setChannel(0, (int) Math.round(v))));
        y += 52;
        addRenderableWidget(new CherrySlider(x, y, w, 30, "G", 0, 255, green(selectedColor()), "%.0f", v -> setChannel(1, (int) Math.round(v))));
        y += 52;
        addRenderableWidget(new CherrySlider(x, y, w, 30, "B", 0, 255, blue(selectedColor()), "%.0f", v -> setChannel(2, (int) Math.round(v))));
    }


    private void initSpawners() {
        int x = contentLeft();
        int right = contentRight();
        int y = contentTop();
        int rowH = 44;
        for (int i = 0; i < BeaconConfig.TYPES.size(); i++) {
            String type = BeaconConfig.TYPES.get(i);
            int yy = y + i * rowH;
            if (yy > panelBottom() - 88) break;
            addRenderableWidget(new CherryButton(x, yy, 180, 32, typeName(type), () -> {
                selectedType = type;
                tab = "colors";
                rebuildWidgets();
            }));
            addRenderableWidget(new ColorSwatch(x + 192, yy + 3, 26, cfg.colorFor(type), c -> {
                cfg.typeColors.put(type, c & 0xFFFFFF); markChanged();
            }));
            addRenderableWidget(new CherryButton(x + 230, yy, 110, 32,
                    Component.translatable(cfg.typeEnabled(type) ? "screen.spawnerbeacon.visible" : "screen.spawnerbeacon.hidden"), () -> {
                        cfg.typeEnabled.put(type, !cfg.typeEnabled(type)); markChanged(); rebuildWidgets();
                    }));
            addRenderableWidget(new CherrySlider(x + 352, yy, Math.max(120, right - x - 352), 32,
                    tr("screen.spawnerbeacon.type_thickness"), 0.1, 5.0, cfg.thicknessFor(type), "%.1f", v -> {
                        cfg.typeThickness.put(type, v); markChanged();
                    }));
        }
    }

    private void initList() {
        int x = contentLeft();
        addRenderableWidget(new CherryButton(contentRight() - 200, contentTop() - 44, 200, 30,
                Component.translatable("screen.spawnerbeacon.copy_closest"), () -> {
                    List<SpawnerInfo> list = SpawnerTracker.sortedByDistance();
                    if (!list.isEmpty()) {
                        var p = list.get(0).pos();
                        GLFW.glfwSetClipboardString(Minecraft.getInstance().getWindow().handle(), formatPos(p.getX(), p.getY(), p.getZ()));
                    }
                }));
    }

    private int selectedColor() { return selectedType.equals("other") ? cfg.defaultColor : cfg.colorFor(selectedType); }
    private Component selectedColorName() { return selectedType.equals("other") ? Component.translatable("screen.spawnerbeacon.default_color") : typeName(selectedType); }

    private void setSelectedColor(int color) {
        if (selectedType.equals("other")) cfg.defaultColor = color & 0xFFFFFF;
        else cfg.typeColors.put(selectedType, color & 0xFFFFFF);
        markChanged(); rebuildWidgets();
    }

    private void setChannel(int channel, int value) {
        int c = selectedColor();
        int r = red(c), g = green(c), b = blue(c);
        if (channel == 0) r = value; else if (channel == 1) g = value; else b = value;
        int next = (r << 16) | (g << 8) | b;
        if (selectedType.equals("other")) cfg.defaultColor = next; else cfg.typeColors.put(selectedType, next);
        markChanged();
    }

    private int red(int c) { return (c >> 16) & 255; }
    private int green(int c) { return (c >> 8) & 255; }
    private int blue(int c) { return c & 255; }
    private Component typeName(String type) { return type.equals("other") ? Component.translatable("screen.spawnerbeacon.type.other") : Component.translatable("entity.minecraft." + type); }
    private String tr(String key) { return Component.translatable(key).getString(); }
    private String formatPos(int x, int y, int z) { return x + " " + y + " " + z; }
    private void markChanged() { dirty = true; lastChange = System.currentTimeMillis(); }

    @Override
    public void tick() {
        super.tick();
        if (dirty && System.currentTimeMillis() - lastChange > 350) {
            cfg.save();
            dirty = false;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        drawBackground(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        drawForeground(graphics, mouseX, mouseY);
    }

    private void drawBackground(GuiGraphicsExtractor g) {
        float open = Math.min(1f, (System.nanoTime() - openedAt) / 280_000_000f);
        int a = (int) (220 * open);
        g.fill(0, 0, width, height, (a << 24) | 0x1A1216);

        int left = panelLeft(), top = panelTop(), right = panelRight(), bottom = panelBottom();
        g.fillGradient(left, top, right, bottom, 0xFFFDF9FA, 0xFFF6E9EE);
        g.outline(left, top, right - left, bottom - top, LINE);
        g.fill(left, top, left + sidebarW(), bottom, 0xFFF9EFF2);
        g.verticalLine(left + sidebarW(), top + 18, bottom - 18, LINE);

        // Realistic raster branch assets keep the UI from feeling like an empty vanilla screen.
        int branchH = Math.min(460, Math.max(300, height / 2));
        int branchW = branchH * 9 / 7;
        g.blit(RenderPipelines.GUI_TEXTURED, BRANCH, left - branchW / 2 + 8, bottom - branchH + 30,
                0, 0, branchW, branchH, 900, 700);
        g.blit(RenderPipelines.GUI_TEXTURED, BRANCH_MIRROR, right - branchW / 2 - 8, bottom - branchH + 30,
                0, 0, branchW, branchH, 900, 700);

        g.text(font, Component.literal("SPAWNERBEACON"), left + 24, top + 24, INK, false);
        g.text(font, Component.translatable("screen.spawnerbeacon.subtitle"), left + 24, top + 42, MUTED, false);
        g.horizontalLine(left + 20, right - 20, top + 72, LINE);

        // Sidebar title and a tiny live status block.
        g.text(font, Component.translatable("screen.spawnerbeacon.navigation"), left + 18, top + 94, MUTED, false);
        int count = SpawnerTracker.snapshot().size();
        int statusColor = cfg.enabled ? GREEN : RED;
        g.fill(left + 18, bottom - 88, left + sidebarW() - 18, bottom - 20, PANEL);
        g.outline(left + 18, bottom - 88, sidebarW() - 36, 68, LINE);
        g.fill(left + 30, bottom - 72, left + 38, bottom - 64, statusColor);
        g.text(font, Component.translatable(cfg.enabled ? "screen.spawnerbeacon.status_active" : "screen.spawnerbeacon.status_disabled"), left + 46, bottom - 75, INK, false);
        g.text(font, Component.translatable("screen.spawnerbeacon.status_spawners", count), left + 30, bottom - 52, MUTED, false);

        // Page heading.
        g.text(font, Component.translatable("screen.spawnerbeacon.tab." + tab), contentLeft(), top + 88, CHERRY_DARK, false);
        g.horizontalLine(contentLeft(), right - 24, top + 101, LINE);

        if (tab.equals("general")) {
            int carW = Math.min(190, Math.max(130, right - contentLeft() - 360));
            int carH = carW / 2;
            g.blit(RenderPipelines.GUI_TEXTURED, CAR, right - carW, top + 12, 0, 0, carW, carH, 720, 360);
        }
    }

    private void drawForeground(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = contentLeft();
        int right = contentRight();
        int y = contentTop();

        if (tab.equals("general")) {
            drawInfoCards(g, x, y + 332, right - x);
        } else if (tab.equals("colors")) {
            int c = selectedColor();
            g.fill(x, y - 30, x + 18, y - 12, 0xFF000000 | c);
            g.outline(x, y - 30, 18, 18, LINE);
            g.text(font, selectedColorName(), x + 28, y - 27, INK, false);
        } else if (tab.equals("list")) {
            drawSpawnerList(g, x, right, y, mouseX, mouseY);
        }

    }

    private void drawInfoCards(GuiGraphicsExtractor g, int x, int y, int w) {
        int gap = 10;
        int cardW = (w - gap * 2) / 3;
        int count = SpawnerTracker.snapshot().size();
        g.fill(x, y, x + cardW, y + 64, PANEL);
        g.fill(x + cardW + gap, y, x + cardW * 2 + gap, y + 64, PANEL);
        g.fill(x + (cardW + gap) * 2, y, x + w, y + 64, PANEL);
        g.outline(x, y, cardW, 64, LINE);
        g.outline(x + cardW + gap, y, cardW, 64, LINE);
        g.outline(x + (cardW + gap) * 2, y, cardW, 64, LINE);
        g.text(font, Component.translatable("screen.spawnerbeacon.card.tracked"), x + 12, y + 10, MUTED, false);
        g.text(font, Component.literal(Integer.toString(count)), x + 12, y + 30, INK, false);
        g.text(font, Component.translatable("screen.spawnerbeacon.card.beam"), x + cardW + gap + 12, y + 10, MUTED, false);
        g.text(font, Component.translatable(cfg.enabled ? "screen.spawnerbeacon.card.on" : "screen.spawnerbeacon.card.off"), x + cardW + gap + 12, y + 30, cfg.enabled ? GREEN : RED, false);
        g.text(font, Component.translatable("screen.spawnerbeacon.card.distance"), x + (cardW + gap) * 2 + 12, y + 10, MUTED, false);
        g.text(font, Component.literal(cfg.renderDistanceChunks + " chunks"), x + (cardW + gap) * 2 + 12, y + 30, INK, false);
    }

    private void drawSpawnerList(GuiGraphicsExtractor g, int left, int right, int top, int mouseX, int mouseY) {
        List<SpawnerInfo> list = SpawnerTracker.sortedByDistance();
        int rowH = 48;
        int bottom = panelBottom() - 64;
        g.enableScissor(left, top, right, bottom);
        int y = top - listScroll;
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        for (SpawnerInfo info : list) {
            if (y + rowH >= top && y <= bottom) {
                int color = cfg.colorFor(info.type());
                g.fill(left, y, right, y + rowH - 4, 0xFFF9F0F3);
                g.fill(left, y, left + 5, y + rowH - 4, 0xFF000000 | color);
                g.text(font, typeName(info.type()), left + 16, y + 7, INK, false);
                double distance = Math.sqrt(info.pos().distToCenterSqr(camera.x, camera.y, camera.z));
                String details = info.pos().getX() + " / " + info.pos().getY() + " / " + info.pos().getZ();
                g.text(font, details, left + 16, y + 25, MUTED, false);
                g.text(font, String.format(Locale.ROOT, "%.0fm", distance), right - 52, y + 16, CHERRY_DARK, false);
                if (mouseX >= left && mouseX <= right && mouseY >= y && mouseY < y + rowH - 4) {
                    g.outline(left, y, right - left, rowH - 4, CHERRY);
                }
            }
            y += rowH;
        }
        g.disableScissor();
        if (list.isEmpty()) {
            g.text(font, Component.translatable("screen.spawnerbeacon.list_empty"), left, top + 50, MUTED, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab.equals("list")) {
            int max = Math.max(0, SpawnerTracker.sortedByDistance().size() * 48 - (panelBottom() - contentTop() - 64));
            listScroll = Math.max(0, Math.min(max, listScroll - (int) Math.round(scrollY * 34)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        cfg.save();
        super.onClose();
    }
}
