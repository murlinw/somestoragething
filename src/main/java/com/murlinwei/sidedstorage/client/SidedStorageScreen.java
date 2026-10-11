package com.murlinwei.sidedstorage.client;

import com.murlinwei.sidedstorage.StorageTier;
import com.murlinwei.sidedstorage.menu.SidedStorageMenu;
import com.murlinwei.sidedstorage.network.SelectFacePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tabbed container screen showing one face of a Sided Storage block at a time.
 *
 * <p>Slot positions are fixed when the menu is built, so this screen only draws them and swaps tabs by asking the
 * server to reopen the menu on another face.
 */
public class SidedStorageScreen extends AbstractContainerScreen<SidedStorageMenu> {

    private static final Direction[] FACES = Direction.values();

    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 16;
    private static final int TAB_START_X = 8;
    private static final int TAB_Y = 27;
    private static final int FACE_LABEL_Y = 16;

    public SidedStorageScreen(SidedStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        this.imageWidth = 176;
        this.imageHeight = this.menu.getImageHeight();
        // AbstractContainerScreen centres the panel using the dimensions set above.
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int right = x + this.imageWidth;
        int bottom = y + this.imageHeight;

        graphics.fill(x, y, right, bottom, 0xFFC6C6C6);
        graphics.fill(x, y, right, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y, x + 1, bottom, 0xFFFFFFFF);
        graphics.fill(x, bottom - 1, right, bottom, 0xFF555555);
        graphics.fill(right - 1, y, right, bottom, 0xFF555555);

        int active = this.menu.getActiveSide();
        for (int side = 0; side < StorageTier.SIDES; side++) {
            int tabX = x + TAB_START_X + side * TAB_WIDTH;
            int tabY = y + TAB_Y;
            int tabRight = tabX + TAB_WIDTH;
            int tabBottom = tabY + TAB_HEIGHT;

            graphics.fill(tabX, tabY, tabRight, tabBottom, side == active ? 0xFF8B8B8B : 0xFFB6B6B6);
            graphics.fill(tabX, tabBottom - 1, tabRight, tabBottom, 0xFF3A3A3A);
            graphics.fill(tabRight - 1, tabY, tabRight, tabBottom, 0xFF3A3A3A);

            // Tabs are only 26px wide, so they get a single letter; the selected face is spelled out above.
            Component label = Component.translatable("gui.sidedstorage.tab." + FACES[side].getSerializedName());
            graphics.drawString(this.font, label,
                    tabX + (TAB_WIDTH - this.font.width(label)) / 2,
                    tabY + (TAB_HEIGHT - 8) / 2,
                    0xFF1A1A1A, false);
        }

        // Slot frames follow the positions the menu already gave the slots.
        for (Slot slot : this.menu.slots) {
            this.slotFrame(graphics, x + slot.x, y + slot.y);
        }
    }

    private void slotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
        graphics.fill(x, y, x + 16, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + 16, 0xFF373737);
        graphics.fill(x + 1, y + 15, x + 16, y + 16, 0xFFFFFFFF);
        graphics.fill(x + 15, y + 1, x + 16, y + 16, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);

        // Own line under the title so long tier names can never collide with it.
        Component face = Component.translatable("gui.sidedstorage.side", Component.translatable(
                "gui.sidedstorage.face." + FACES[this.menu.getActiveSide()].getSerializedName()));
        graphics.drawString(this.font, face, 8, FACE_LABEL_Y, 0x404040, false);

        graphics.drawString(this.font, Component.translatable("container.inventory"),
                8, this.menu.getPlayerInvY() - 10, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int relX = (int) (mouseX - this.leftPos);
            int relY = (int) (mouseY - this.topPos);
            if (relY >= TAB_Y && relY < TAB_Y + TAB_HEIGHT) {
                for (int side = 0; side < StorageTier.SIDES; side++) {
                    int tabX = TAB_START_X + side * TAB_WIDTH;
                    if (relX >= tabX && relX < tabX + TAB_WIDTH) {
                        this.selectFace(side);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Asks the server to open the same block on a different face. The current
     * screen deliberately stays open until the replacement menu arrives, so
     * there is no flash of inventory and the cursor stays on the tab strip.
     * If the server declines, the old face simply stays showing.
     */
    private void selectFace(int side) {
        if (side == this.menu.getActiveSide() || this.minecraft.player == null) {
            return;
        }
        PacketDistributor.sendToServer(new SelectFacePayload(this.menu.getBlockPos(), side));
    }
}