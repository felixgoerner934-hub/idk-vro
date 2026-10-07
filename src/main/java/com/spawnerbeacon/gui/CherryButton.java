package com.spawnerbeacon.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Minimal Cherry button with hover/click animation. */
public final class CherryButton extends AbstractWidget {
    private final Runnable action;
    private float press = 0f;

    public CherryButton(int x, int y, int width, int height, Component message, Runnable action) {
        super(x, y, width, height, message);
        this.action = action;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        press *= 0.78f;
        int bg = isHovered() ? 0xFFF4DDE3 : 0xFFF9F2F4;
        int border = isHovered() ? 0xFFB93B60 : 0xFFD8B7C1;
        graphics.fill(getX(), getY(), getRight(), getBottom(), bg);
        graphics.outline(getX(), getY(), getWidth(), getHeight(), border);
        if (isHovered()) graphics.fill(getX(), getBottom() - 2, getRight(), getBottom(), 0xFFE76F9A);
        int textColor = isHovered() ? 0xFF7A1839 : 0xFF35282D;
        int tx = getX() + Math.max(6, (getWidth() -  font().width(getMessage())) / 2);
        graphics.text(font(), getMessage(), tx, getY() + (getHeight() - font().lineHeight) / 2 + 1, textColor, false);
    }

    private net.minecraft.client.gui.Font font() {
        return net.minecraft.client.Minecraft.getInstance().font;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        press = 1f;
        action.run();
        playButtonClickSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
