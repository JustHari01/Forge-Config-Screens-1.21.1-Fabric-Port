package fuzs.forgeconfigscreens.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the vanilla world-list status icons used by the 1.21.1 GUI.
 * The old world_selection.png sheet was removed from Minecraft after 1.20.1.
 */
final class WorldSelectionIcons {
    enum Icon {
        JOIN("join"),
        MARKED_JOIN("marked_join"),
        WARNING("warning"),
        ERROR("error");

        private final String sprite;

        Icon(String sprite) {
            this.sprite = sprite;
        }
    }

    private WorldSelectionIcons() {
    }

    static void render(GuiGraphics guiGraphics, Icon icon, int x, int y, boolean highlighted) {
        String sprite = "world_list/" + icon.sprite + (highlighted ? "_highlighted" : "");
        guiGraphics.blitSprite(ResourceLocation.withDefaultNamespace(sprite), x, y, 32, 32);
    }
}
