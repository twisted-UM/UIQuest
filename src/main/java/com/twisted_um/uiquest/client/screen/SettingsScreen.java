package com.twisted_um.uiquest.client.screen;

import com.twisted_um.uiquest.UIQuestConfig;
import com.twisted_um.uiquest.client.UIQuestLang;
import com.twisted_um.uiquest.client.UISounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SettingsScreen extends Screen {

    private static final int ROW_H      = 24;
    private static final int TAB_H_TOP  = 28;
    private static final int LIST_W     = 220;
    private static final int HEADER_H   = TAB_H_TOP;
    private static final int SIDE_PAD   = 24;
    private static final int FIXED_UI_W = 620;
    private static final int FIXED_UI_H = 480;

    private static final int COL_GOLD        = 0xFFC8AA64;
    private static final int COL_GOLD_FAINT  = 0x33C8AA64;
    private static final int COL_BG_OVERLAY  = 0x77101010;
    private static final int COL_TAB_BG      = 0x55202020;
    private static final int COL_TEXT        = 0xFFFEFCF5;
    private static final int COL_TEXT_FAINT  = 0xFFA08060;
    private static final int COL_ROW_BG_CH   = 0xFF3A3A3A;
    private static final int COL_SEL_INNER   = 0xCCFEFCF5;
    private static final int COL_SEL_TEXT    = 0xFF3A3A3A;
    private static final int COL_SEP_LINE    = 0x55808080;
    private static final int COL_SLIDER_BG   = 0xFF2A2A2A;
    private static final int COL_SLIDER_FG   = 0xFFC8AA64;
    private static final int COL_SLIDER_THUMB= 0xFFFEFCF5;

    private boolean soundEnabled;
    private double soundVolume;

    private static final double MIN = 0.5, MAX = 2.0;
    private static final int SLIDER_W = 160;
    private static final int SLIDER_H = 8;
    private static final int ITEM_ROW_H = 28;

    private enum EntryType { SLIDER, TOGGLE, BUTTON }
    private enum Category { GENERAL, GUI, HUD }

    private record SettingEntry(
            String labelKey,
            EntryType type,
            double min, double max,
            java.util.function.DoubleSupplier getDouble,
            java.util.function.DoubleConsumer setDouble,
            java.util.function.BooleanSupplier getBool,
            java.util.function.Consumer<Boolean> setBool,
            Runnable action
    ) {
        static SettingEntry toggle(String key,
                                   java.util.function.BooleanSupplier get,
                                   java.util.function.Consumer<Boolean> set) {
            return new SettingEntry(key, EntryType.TOGGLE, 0, 1, null, null, get, set, null);
        }
        static SettingEntry button(String key, Runnable action) {
            return new SettingEntry(key, EntryType.BUTTON, 0, 1, null, null, null, null, action);
        }
        static SettingEntry slider(String key, double min, double max,
                                   java.util.function.DoubleSupplier get,
                                   java.util.function.DoubleConsumer set) {
            return new SettingEntry(key, EntryType.SLIDER, min, max, get, set, null, null, null);
        }
    }

    private boolean showCompletedQuests;
    private boolean hudShowCompletedTasks;

    private boolean hudAutoTrack;
    private boolean hudAutoTrackMulti;

    private boolean showCompletedChapters;
    private boolean hudEnabled;

    private final Screen parent;
    private Category activeCategory = Category.GUI;
    private int draggingSlider = -1;

    private int uiX, uiY, uiW, uiH;
    private int tabBarX, tabBarY, tabBarW;
    private int listX, listY, listH;
    private int detailX, detailY, detailW, detailH;

    private int saveBtnX, saveBtnY;
    private static final int SAVE_BTN_W = 60;
    private static final int SAVE_BTN_H = 17;

    private List<SettingEntry> currentEntries = List.of();

    public SettingsScreen(Screen parent) {
        super(Component.literal("UIQuest Settings"));
        this.parent = parent;
        loadValues();
    }

    private void loadValues() {
        showCompletedQuests   = UIQuestConfig.SHOW_COMPLETED_QUESTS.get();
        hudShowCompletedTasks = UIQuestConfig.HUD_SHOW_COMPLETED_TASKS.get();
        showCompletedChapters = UIQuestConfig.SHOW_COMPLETED_CHAPTERS.get();
        hudEnabled            = UIQuestConfig.HUD_ENABLED.get();
        hudAutoTrack          = UIQuestConfig.HUD_AUTO_TRACK.get();
        hudAutoTrackMulti     = UIQuestConfig.HUD_AUTO_TRACK_MULTI.get();
        soundEnabled = UIQuestConfig.SOUND_ENABLED.get();
        soundVolume  = UIQuestConfig.SOUND_VOLUME.get();
    }

    private List<SettingEntry> buildEntries(Category cat) {
        return switch (cat) {
            case GENERAL -> List.of(
                    SettingEntry.toggle("uiquest.settings.general.auto_track",
                            () -> hudAutoTrack,
                            v -> hudAutoTrack = v),
                    SettingEntry.toggle("uiquest.settings.general.auto_track_multi",
                            () -> hudAutoTrackMulti,
                            v -> hudAutoTrackMulti = v),
                    SettingEntry.toggle("uiquest.settings.general.sound_enabled",
                            () -> soundEnabled,
                            v -> soundEnabled = v),
                    SettingEntry.slider("uiquest.settings.general.sound_volume",
                            0.0, 1.0,
                            () -> soundVolume,
                            v -> soundVolume = v)
            );
            case GUI -> List.of(
                    SettingEntry.toggle("uiquest.settings.gui.show_completed",
                            () -> showCompletedQuests,
                            v -> showCompletedQuests = v),
                    SettingEntry.toggle("uiquest.settings.gui.show_completed_chapters",
                            () -> showCompletedChapters,
                            v -> showCompletedChapters = v)
            );
            case HUD -> List.of(
                    SettingEntry.toggle("uiquest.settings.hud.enabled",
                            () -> hudEnabled,
                            v -> hudEnabled = v),
                    SettingEntry.toggle("uiquest.settings.hud.show_completed_tasks",
                            () -> hudShowCompletedTasks,
                            v -> hudShowCompletedTasks = v),
                    SettingEntry.button("uiquest.settings.hud.position",
                            () -> minecraft.setScreen(new HudPositionScreen(this)))
            );
        };
    }

    @Override
    protected void init() {
        uiW = Math.min(width  - SIDE_PAD * 2, FIXED_UI_W);
        uiH = Math.min(height - 20, FIXED_UI_H);
        uiW = Math.max(uiW, 400);

        uiX = (width  - uiW) / 2;
        uiY = (height - uiH) / 2;
        uiY = Math.max(10, uiY);

        tabBarX = uiX;
        tabBarY = uiY;
        tabBarW = uiW;

        listX = uiX + SIDE_PAD;
        listY = uiY + HEADER_H;
        listH = uiH - HEADER_H;

        detailX = listX + LIST_W + 1;
        detailY = uiY + HEADER_H;
        detailW = uiX + uiW - SIDE_PAD - detailX;
        detailH = uiH - HEADER_H;

        saveBtnX = detailX + detailW - SAVE_BTN_W - 4;
        saveBtnY = detailY + detailH - SAVE_BTN_H - 6;

        currentEntries = buildEntries(activeCategory);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);

        gfx.fill(0, 0, width, height, COL_BG_OVERLAY);

        renderTabBar(gfx, mouseX, mouseY);
        renderCategoryList(gfx, mouseX, mouseY);
        renderDetailPanel(gfx, mouseX, mouseY);
    }

    private void renderTabBar(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.fill(0, 0, width, tabBarY + TAB_H_TOP, COL_TAB_BG);

        String title = UIQuestLang.get("uiquest.settings.title");
        int ty = tabBarY + (TAB_H_TOP - font.lineHeight) / 2;
        gfx.drawString(font, title, tabBarX + (tabBarW - font.width(title)) / 2, ty, COL_TEXT, false);
    }

    private void renderCategoryList(GuiGraphics gfx, int mouseX, int mouseY) {
        int treeTop = listY + 4;
        int right   = listX + LIST_W - 7;
        int GAP = 4;

        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            int ry     = treeTop + i * (ROW_H + GAP);
            int rowBot = ry + ROW_H;
            boolean selected = (cat == activeCategory);

            gfx.fill(listX, ry, right, rowBot, COL_ROW_BG_CH);
            if (selected) {
                gfx.fill(listX, ry, right, rowBot, COL_SEL_INNER);
                gfx.fill(listX, ry + 1, listX + 2, rowBot - 1, COL_GOLD);
            }

            int textCol = selected ? COL_SEL_TEXT : COL_GOLD;
            int textY   = ry + (ROW_H - font.lineHeight) / 2 + 1;
            String label = categoryLabel(cat).toUpperCase(java.util.Locale.ROOT);
            gfx.drawString(font, label, listX + 10, textY, textCol, false);
        }
    }

    private void renderDetailPanel(GuiGraphics gfx, int mouseX, int mouseY) {
        if (currentEntries.isEmpty()) {
            String hint = UIQuestLang.get("uiquest.settings.empty");
            gfx.drawString(font, hint,
                    detailX + (detailW - font.width(hint)) / 2,
                    detailY + detailH / 2,
                    COL_TEXT_FAINT, false);
        } else {
            int startY = detailY + 12;
            for (int i = 0; i < currentEntries.size(); i++) {
                renderEntry(gfx, currentEntries.get(i), i, startY, mouseX, mouseY);
            }
        }

        boolean hovered = mouseX >= saveBtnX && mouseX < saveBtnX + SAVE_BTN_W
                && mouseY >= saveBtnY && mouseY < saveBtnY + SAVE_BTN_H;
        gfx.fill(saveBtnX - 1, saveBtnY - 1, saveBtnX + SAVE_BTN_W + 1, saveBtnY + SAVE_BTN_H + 1, COL_GOLD);
        gfx.fill(saveBtnX, saveBtnY, saveBtnX + SAVE_BTN_W, saveBtnY + SAVE_BTN_H,
                hovered ? 0xFF555555 : 0xFF3A3A3A);
        String saveLabel = UIQuestLang.get("uiquest.settings.save");
        gfx.drawString(font, saveLabel,
                saveBtnX + (SAVE_BTN_W - font.width(saveLabel)) / 2,
                saveBtnY + (SAVE_BTN_H - font.lineHeight) / 2 + 1,
                COL_GOLD, false);

        int closeBtnX = saveBtnX - SAVE_BTN_W - 6;
        int closeBtnY = saveBtnY;
        boolean closeHovered = mouseX >= closeBtnX && mouseX < closeBtnX + SAVE_BTN_W
                && mouseY >= closeBtnY && mouseY < closeBtnY + SAVE_BTN_H;
        gfx.fill(closeBtnX - 1, closeBtnY - 1, closeBtnX + SAVE_BTN_W + 1, closeBtnY + SAVE_BTN_H + 1, 0xFF888880);
        gfx.fill(closeBtnX, closeBtnY, closeBtnX + SAVE_BTN_W, closeBtnY + SAVE_BTN_H,
                closeHovered ? 0xFF555555 : 0xFF3A3A3A);
        String closeLabel = UIQuestLang.get("uiquest.settings.close");
        gfx.drawString(font, closeLabel,
                closeBtnX + (SAVE_BTN_W - font.width(closeLabel)) / 2,
                closeBtnY + (SAVE_BTN_H - font.lineHeight) / 2 + 1,
                COL_TEXT, false);
    }

    private void renderEntry(GuiGraphics gfx, SettingEntry entry, int index,
                             int startY, int mouseX, int mouseY) {
        int ry = startY + index * ITEM_ROW_H;

        gfx.drawString(font, UIQuestLang.get(entry.labelKey()),
                detailX + 8, ry + (ITEM_ROW_H - font.lineHeight) / 2 + 1, COL_TEXT, false);

        if (entry.type() == EntryType.TOGGLE) {
            boolean val = entry.getBool().getAsBoolean();
            int tw = 36, th = 12;
            int tx = detailX + detailW - tw - 12;
            int ty = ry + (ITEM_ROW_H - th) / 2;

            gfx.fill(tx - 1, ty - 1, tx + tw + 1, ty + th + 1, val ? COL_GOLD : 0xFF888880);
            gfx.fill(tx, ty, tx + tw, ty + th, val ? 0xFF3A3A3A : 0xFF2A2A2A);

            int thumbX = val ? tx + tw - th : tx;
            gfx.fill(thumbX, ty, thumbX + th, ty + th, val ? COL_GOLD : 0xFF888880);

            String toggleLabel = val ? UIQuestLang.get("uiquest.settings.on")
                    : UIQuestLang.get("uiquest.settings.off");
            gfx.drawString(font, toggleLabel,
                    tx + (tw - font.width(toggleLabel)) / 2,
                    ty + (th - font.lineHeight) / 2 + 1,
                    val ? COL_TEXT : COL_TEXT_FAINT, false);
        } else if (entry.type() == EntryType.BUTTON) {
        int bw = 80, bh = 14;
        int bx = detailX + detailW - bw - 12;
        int by = ry + (ITEM_ROW_H - bh) / 2;
        boolean hovered = mouseX >= bx && mouseX < bx + bw
                && mouseY >= by && mouseY < by + bh;
        gfx.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, COL_GOLD);
        gfx.fill(bx, by, bx + bw, by + bh, hovered ? 0xFF555555 : 0xFF3A3A3A);
        String btnLabel = UIQuestLang.get("uiquest.settings.hud.position.btn");
        gfx.drawString(font, btnLabel,
                bx + (bw - font.width(btnLabel)) / 2,
                by + (bh - font.lineHeight) / 2 + 1,
                COL_GOLD, false);
    } else if (entry.type() == EntryType.SLIDER) {
        int sx = detailX + detailW - SLIDER_W - 40;
        int sy = ry + (ITEM_ROW_H - SLIDER_H) / 2;
        double ratio = (entry.getDouble().getAsDouble() - entry.min()) / (entry.max() - entry.min());

        gfx.fill(sx, sy, sx + SLIDER_W, sy + SLIDER_H, COL_SLIDER_BG);
        gfx.fill(sx, sy, sx + (int)(SLIDER_W * ratio), sy + SLIDER_H, COL_SLIDER_FG);
        int thumbX = sx + (int)(SLIDER_W * ratio) - 2;
        gfx.fill(thumbX, sy - 2, thumbX + 4, sy + SLIDER_H + 2, COL_SLIDER_THUMB);

        String valStr = String.format("%.0f%%", entry.getDouble().getAsDouble() * 100);
        gfx.drawString(font, valStr, sx + SLIDER_W + 6,
                ry + (ITEM_ROW_H - font.lineHeight) / 2 + 1, COL_TEXT_FAINT, false);
    }


        gfx.fill(detailX + 4, ry + ITEM_ROW_H - 1,
                detailX + detailW - 4, ry + ITEM_ROW_H, COL_SEP_LINE);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = (int)mx, y = (int)my;

        int treeTop = listY + 4;
        int right   = listX + LIST_W - 7;
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            int ry = treeTop + i * (ROW_H + 4);
            if (x >= listX && x < right && y >= ry && y < ry + ROW_H) {
                UISounds.play(UISounds.UI_CONFIG_CLICK);
                activeCategory = cats[i];
                currentEntries = buildEntries(activeCategory);
                draggingSlider = -1;
                return true;
            }
        }

        if (x >= saveBtnX && x < saveBtnX + SAVE_BTN_W
                && y >= saveBtnY && y < saveBtnY + SAVE_BTN_H) {
            UISounds.play(UISounds.UI_CONFIG_CLICK);
            saveAndClose();
            return true;
        }

        int closeBtnX = saveBtnX - SAVE_BTN_W - 6;
        if (x >= closeBtnX && x < closeBtnX + SAVE_BTN_W
                && y >= saveBtnY && y < saveBtnY + SAVE_BTN_H) {
            UISounds.play(UISounds.UI_CONFIG_CLICK);
            minecraft.setScreen(parent);
            return true;
        }

        int entryIdx = getEntryAt(x, y);
        if (entryIdx >= 0) {
            UISounds.play(UISounds.UI_CONFIG_CLICK);
            SettingEntry entry = currentEntries.get(entryIdx);
            if (entry.type() == EntryType.TOGGLE) {
                entry.setBool().accept(!entry.getBool().getAsBoolean());
            } else if (entry.type() == EntryType.BUTTON) {
                if (entry.action() != null) entry.action().run();
            } else {
                draggingSlider = entryIdx;
                updateSlider(entryIdx, x);
            }
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    private void updateSlider(int index, int mouseX) {
        if (index < 0 || index >= currentEntries.size()) return;
        SettingEntry entry = currentEntries.get(index);
        if (entry.type() != EntryType.SLIDER) return;
        int sx = detailX + detailW - SLIDER_W - 40;
        double ratio = Math.max(0, Math.min(1, (mouseX - sx) / (double) SLIDER_W));
        entry.setDouble().accept(entry.min() + ratio * (entry.max() - entry.min()));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingSlider >= 0) {
            updateSlider(draggingSlider, (int)mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingSlider = -1;
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

    private int getEntryAt(int x, int y) {
        if (currentEntries.isEmpty()) return -1;
        int startY = detailY + 12;
        for (int i = 0; i < currentEntries.size(); i++) {
            int ry = startY + i * ITEM_ROW_H;
            if (x >= detailX && x < detailX + detailW && y >= ry && y < ry + ITEM_ROW_H)
                return i;
        }
        return -1;
    }

    private void saveAndClose() {
        UIQuestConfig.SHOW_COMPLETED_QUESTS.set(showCompletedQuests);
        UIQuestConfig.HUD_SHOW_COMPLETED_TASKS.set(hudShowCompletedTasks);
        UIQuestConfig.SHOW_COMPLETED_CHAPTERS.set(showCompletedChapters);
        UIQuestConfig.HUD_ENABLED.set(hudEnabled);
        UIQuestConfig.HUD_AUTO_TRACK.set(hudAutoTrack);
        UIQuestConfig.HUD_AUTO_TRACK_MULTI.set(hudAutoTrackMulti);
        UIQuestConfig.SOUND_ENABLED.set(soundEnabled);
        UIQuestConfig.SOUND_VOLUME.set(soundVolume);
        UIQuestConfig.saveSettings();
        minecraft.setScreen(parent);
    }

    private String categoryLabel(Category cat) {
        return switch (cat) {
            case GENERAL -> UIQuestLang.get("uiquest.settings.category.general");
            case GUI     -> UIQuestLang.get("uiquest.settings.category.gui");
            case HUD     -> UIQuestLang.get("uiquest.settings.category.hud");
        };
    }

    @Override public boolean isPauseScreen() { return false; }
}