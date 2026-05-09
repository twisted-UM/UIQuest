package com.twisted_um.uiquest.client.screen;

import com.twisted_um.uiquest.UIQuestConfig;
import com.twisted_um.uiquest.client.UIQuestLang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HudPositionScreen extends Screen {

    private static final int COL_GOLD       = 0xFFC8AA64;
    private static final int COL_TEXT       = 0xFFFEFCF5;
    private static final int COL_TEXT_FAINT = 0xFFA08060;
    private static final int COL_BG_OVERLAY = 0x44101010;

    private static final int HUD_W    = 120;
    private static final int PREVIEW_H = 80;

    private final Screen parent;
    private int hudX, hudY;
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    private static final int BTN_W = 60;
    private static final int BTN_H = 17;
    private int saveBtnX, saveBtnY;
    private int cancelBtnX, cancelBtnY;

    private static final int DEFAULT_X = 4;
    private static final int DEFAULT_Y = 104;

    private int resetBtnX, resetBtnY;

    public HudPositionScreen(Screen parent) {
        super(Component.literal("HUD Position"));
        this.parent = parent;
        hudX = UIQuestConfig.HUD_POS_X.get();
        hudY = UIQuestConfig.HUD_POS_Y.get();
    }

    @Override
    protected void init() {
        saveBtnX   = width / 2 + BTN_W + 8;
        saveBtnY   = height - BTN_H - 8;
        resetBtnX  = width / 2 - BTN_W / 2;
        resetBtnY  = saveBtnY;
        cancelBtnX = width / 2 - BTN_W * 2 - 8;
        cancelBtnY = saveBtnY;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);

        gfx.fill(0, 0, width, height, COL_BG_OVERLAY);

        String hint = UIQuestLang.get("uiquest.hud_position.hint");
        gfx.drawString(font, hint, (width - font.width(hint)) / 2, 8, 0xFFFEFCF5, false);

        renderHudPreview(gfx, mouseX, mouseY);

        boolean saveHovered = mouseX >= saveBtnX && mouseX < saveBtnX + BTN_W
                && mouseY >= saveBtnY && mouseY < saveBtnY + BTN_H;
        gfx.fill(saveBtnX - 1, saveBtnY - 1, saveBtnX + BTN_W + 1, saveBtnY + BTN_H + 1, COL_GOLD);
        gfx.fill(saveBtnX, saveBtnY, saveBtnX + BTN_W, saveBtnY + BTN_H,
                saveHovered ? 0xFF555555 : 0xFF3A3A3A);
        String saveLabel = UIQuestLang.get("uiquest.settings.save");
        gfx.drawString(font, saveLabel,
                saveBtnX + (BTN_W - font.width(saveLabel)) / 2,
                saveBtnY + (BTN_H - font.lineHeight) / 2,
                COL_GOLD, false);

        boolean cancelHovered = mouseX >= cancelBtnX && mouseX < cancelBtnX + BTN_W
                && mouseY >= cancelBtnY && mouseY < cancelBtnY + BTN_H;
        gfx.fill(cancelBtnX - 1, cancelBtnY - 1, cancelBtnX + BTN_W + 1, cancelBtnY + BTN_H + 1, 0xFF888880);
        gfx.fill(cancelBtnX, cancelBtnY, cancelBtnX + BTN_W, cancelBtnY + BTN_H,
                cancelHovered ? 0xFF555555 : 0xFF3A3A3A);
        String cancelLabel = UIQuestLang.get("uiquest.settings.close");
        gfx.drawString(font, cancelLabel,
                cancelBtnX + (BTN_W - font.width(cancelLabel)) / 2,
                cancelBtnY + (BTN_H - font.lineHeight) / 2,
                COL_TEXT, false);

        boolean resetHovered = mouseX >= resetBtnX && mouseX < resetBtnX + BTN_W
                && mouseY >= resetBtnY && mouseY < resetBtnY + BTN_H;
        gfx.fill(resetBtnX - 1, resetBtnY - 1, resetBtnX + BTN_W + 1, resetBtnY + BTN_H + 1, 0xFF888880);
        gfx.fill(resetBtnX, resetBtnY, resetBtnX + BTN_W, resetBtnY + BTN_H,
                resetHovered ? 0xFF555555 : 0xFF3A3A3A);
        String resetLabel = UIQuestLang.get("uiquest.hud_position.reset");
        gfx.drawString(font, resetLabel,
                resetBtnX + (BTN_W - font.width(resetLabel)) / 2,
                resetBtnY + (BTN_H - font.lineHeight) / 2,
                COL_TEXT, false);

        String posStr = "X: " + hudX + "  Y: " + hudY;
        gfx.drawString(font, posStr,
                (width - font.width(posStr)) / 2,
                saveBtnY - font.lineHeight - 4,
                COL_TEXT_FAINT, false);
    }

    private void renderHudPreview(GuiGraphics gfx, int mouseX, int mouseY) {
        boolean hovered = mouseX >= hudX && mouseX < hudX + HUD_W
                && mouseY >= hudY && mouseY < hudY + PREVIEW_H;

        int steps = 60;
        for (int i = 0; i < steps; i++) {
            int sliceX1 = hudX + (HUD_W * i / steps);
            int sliceX2 = hudX + (HUD_W * (i + 1) / steps);
            int alpha = (int)(0xBB * (1.0f - (float)i / steps));
            int color = (alpha << 24) | 0x101010;
            gfx.fill(sliceX1, hudY, sliceX2, hudY + PREVIEW_H, color);
        }
        gfx.fill(hudX, hudY, hudX + 2, hudY + PREVIEW_H, COL_GOLD);

        int textX = hudX + 6;
        int drawY = hudY + 4;
        gfx.drawString(font, UIQuestLang.get("uiquest.hud_position.preview.chapter"),
                textX, drawY, COL_TEXT_FAINT, false);
        drawY += font.lineHeight + 2;
        gfx.drawString(font, UIQuestLang.get("uiquest.hud_position.preview.quest"),
                textX, drawY, COL_TEXT, false);
        drawY += font.lineHeight + 4;
        gfx.fill(textX, drawY - 2, hudX + HUD_W - 6, drawY - 1, COL_GOLD);
        gfx.drawString(font, UIQuestLang.get("uiquest.hud_position.preview.task"),
                textX + 8, drawY + 2, COL_TEXT, false);
        drawY += font.lineHeight + 2;
        gfx.drawString(font, UIQuestLang.get("uiquest.hud_position.preview.hint"),
                textX, drawY + 2, COL_TEXT_FAINT, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = (int)mx, y = (int)my;

        if (x >= saveBtnX && x < saveBtnX + BTN_W
                && y >= saveBtnY && y < saveBtnY + BTN_H) {
            saveAndClose();
            return true;
        }

        if (x >= cancelBtnX && x < cancelBtnX + BTN_W
                && y >= cancelBtnY && y < cancelBtnY + BTN_H) {
            minecraft.setScreen(parent);
            return true;
        }

        if (x >= resetBtnX && x < resetBtnX + BTN_W
                && y >= resetBtnY && y < resetBtnY + BTN_H) {
            hudX = DEFAULT_X;
            hudY = DEFAULT_Y;
            return true;
        }

        if (x >= hudX && x < hudX + HUD_W && y >= hudY && y < hudY + PREVIEW_H) {
            dragging = true;
            dragOffsetX = x - hudX;
            dragOffsetY = y - hudY;
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            hudX = Math.max(0, Math.min(width  - HUD_W,    (int)mx - dragOffsetX));
            hudY = Math.max(0, Math.min(height - PREVIEW_H, (int)my - dragOffsetY));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void saveAndClose() {
        UIQuestConfig.HUD_POS_X.set(hudX);
        UIQuestConfig.HUD_POS_Y.set(hudY);
        UIQuestConfig.saveSettings();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}