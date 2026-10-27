package fuzs.forgeconfigscreens.client.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class MutableIconButton extends Button {
    private final ResourceLocation resourceLocation;
    private final int textureWidth;
    private final int textureHeight;
    private final int yDiffTex;
    private int xTexStart;
    private int yTexStart;

    public MutableIconButton(int x, int y, int width, int height, int xTexStart, int yTexStart, ResourceLocation resourceLocation, Button.OnPress onPress) {
        this(x, y, width, height, xTexStart, yTexStart, 0, resourceLocation, 180, 20, onPress);
    }

    public MutableIconButton(int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex, ResourceLocation resourceLocation, Button.OnPress onPress) {
        this(x, y, width, height, xTexStart, yTexStart, yDiffTex, resourceLocation, 180, 60, onPress);
    }

    public MutableIconButton(int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex, ResourceLocation resourceLocation, int textureWidth, int textureHeight, Button.OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.xTexStart = xTexStart;
        this.yTexStart = yTexStart;
        this.yDiffTex = yDiffTex;
        this.resourceLocation = resourceLocation;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public void setTexture(int textureX, int textureY) {
        this.xTexStart = textureX;
        this.yTexStart = textureY;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        int textureY = this.yTexStart;
        if (this.yDiffTex > 0) {
            if (!this.active) {
                textureY += this.yDiffTex * 2;
            } else if (this.isHoveredOrFocused()) {
                textureY += this.yDiffTex;
            }
        }
        guiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), this.xTexStart, textureY,
                this.getWidth(), this.getHeight(), this.textureWidth, this.textureHeight);
    }
}
