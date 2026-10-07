package com.spawnerbeacon.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.DoubleConsumer;

public final class CherrySlider extends AbstractSliderButton {
    private final String label;
    private final double min;
    private final double max;
    private final String format;
    private final DoubleConsumer setter;

    public CherrySlider(int x, int y, int width, int height, String label, double min, double max,
                        double current, String format, DoubleConsumer setter) {
        super(x, y, width, height, Component.empty(), (current - min) / (max - min));
        this.label = label;
        this.min = min;
        this.max = max;
        this.format = format;
        this.setter = setter;
        updateMessage();
    }

    private double actual() { return min + (max - min) * value; }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(label + "  " + String.format(Locale.ROOT, format, actual())));
    }

    @Override
    protected void applyValue() {
        setter.accept(actual());
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(getX(), getY() + 7, getRight(), getY() + 10, 0xFFDCC7CD);
        int knobX = getX() + (int) ((getWidth() - 10) * value);
        graphics.fill(knobX, getY() + 3, knobX + 10, getY() + 14, isHovered() ? 0xFFE76F9A : 0xFFB93B60);
        graphics.text(net.minecraft.client.Minecraft.getInstance().font, getMessage(), getX(), getY() - 1,
                isHovered() ? 0xFF7A1839 : 0xFF4A343B, false);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
