package com.spawnerbeacon.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

public final class ColorSwatch extends AbstractWidget {
    private final int color;
    private final IntConsumer action;

    public ColorSwatch(int x, int y, int size, int color, IntConsumer action) {
        super(x, y, size, size, Component.empty());
        this.color = color & 0xFFFFFF;
        this.action = action;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int c = 0xFF000000 | color;
        graphics.fill(getX(), getY(), getRight(), getBottom(), c);
        graphics.outline(getX(), getY(), getWidth(), getHeight(), isHovered() ? 0xFFFFFFFF : 0xFFD8B7C1);
        if (isHovered()) graphics.outline(getX() - 1, getY() - 1, getWidth() + 2, getHeight() + 2, 0xFFE76F9A);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        action.accept(color);
        playButtonClickSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
