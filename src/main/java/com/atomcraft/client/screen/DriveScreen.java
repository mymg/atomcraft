package com.atomcraft.client.screen;

import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.client.ClientState;
import com.atomcraft.menu.DriveMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Drive: slots de disco e lista dos elementos guardados na rede. */
public class DriveScreen extends MachineScreen<DriveMenu> {

    private static final int LIST_X = 86, LIST_Y = 18, LIST_W = 82, LIST_H = 66, ROW_H = 9;
    private static final int VISIBLE_ROWS = (LIST_H - 4) / ROW_H;

    private int scroll;

    public DriveScreen(DriveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, DriveMenu.WIDTH, DriveMenu.HEIGHT);
    }

    private List<Map.Entry<ChemicalElement, Long>> entries() {
        return new ArrayList<>(ClientState.networkTotals().elements().entrySet());
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        networkBar(graphics, leftPos + 8, topPos + 62, 72);
        GuiUtil.inset(graphics, leftPos + LIST_X, topPos + LIST_Y, LIST_W, LIST_H);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, networkLabel(), 8, 72, GuiUtil.TEXT_LIGHT, false);

        List<Map.Entry<ChemicalElement, Long>> entries = entries();
        scroll = Math.max(0, Math.min(scroll, entries.size() - VISIBLE_ROWS));
        if (entries.isEmpty()) {
            graphics.text(font, Component.translatable("gui.atomcraft.empty"), LIST_X + 4, LIST_Y + 4, 0xFFE0E0E0, false);
        }
        for (int i = 0; i < VISIBLE_ROWS && i + scroll < entries.size(); i++) {
            var entry = entries.get(i + scroll);
            int y = LIST_Y + 3 + i * ROW_H;
            int color = entry.getKey().exotic() ? 0xFFE8B8FF : 0xFFFFFFFF;
            graphics.text(font, entry.getKey().symbol(), LIST_X + 4, y, color, true);
            String amount = GuiUtil.formatAmount(entry.getValue());
            graphics.text(font, amount, LIST_X + LIST_W - 4 - font.width(amount), y, 0xFFFFFFFF, true);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (GuiUtil.inside(mouseX, mouseY, leftPos + LIST_X, topPos + LIST_Y, LIST_W, LIST_H)) {
            scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
