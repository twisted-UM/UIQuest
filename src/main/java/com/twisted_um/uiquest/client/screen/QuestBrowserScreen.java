package com.twisted_um.uiquest.client.screen;

import com.twisted_um.uiquest.UIQuest;
import com.twisted_um.uiquest.UIQuestConfig;
import com.twisted_um.uiquest.client.UIQuestLang;
import com.twisted_um.uiquest.client.UISounds;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.reward.*;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.*;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.task.Task;

import dev.ftb.mods.ftbquests.net.SubmitTaskMessage;

import dev.ftb.mods.ftbquests.net.ClaimRewardMessage;
import dev.architectury.networking.NetworkManager;

import dev.ftb.mods.ftbquests.quest.task.CheckmarkTask;

import dev.ftb.mods.ftbquests.client.gui.SelectChoiceRewardScreen;

import com.twisted_um.uiquest.compat.ImageComponentAccess;
import java.util.HashSet;
import java.util.Set;

import com.twisted_um.uiquest.client.QuestChainFinder;

public class QuestBrowserScreen extends Screen {

    private static final int ROW_H      = 24;
    private static final int TAB_H_TOP  = 28;
    private static final int LIST_W     = 220;
    private static final int HEADER_H   = TAB_H_TOP;
    private static final int SIDE_PAD   = 24;

    private static final int FIXED_UI_W = 620;
    private static final int FIXED_UI_H = 480;

    private static final int COL_GOLD           = 0xFFC8AA64;
    private static final int COL_GOLD_FAINT     = 0x33C8AA64;
    private static final int COL_SEP_LINE       = 0x55808080;
    private static final int COL_BG_OVERLAY     = 0x77101010;
    private static final int COL_TAB_BG         = 0x55202020;
    private static final int COL_TEXT           = 0xFFFEFCF5;
    private static final int COL_TEXT_DIM       = 0xFFECEAE0;
    private static final int COL_TEXT_FAINT     = 0xFFA08060;
    private static final int COL_TITLE          = 0xFFFEFCF5;

    private static final int COL_CAN_START      = 0xFFFFDD44;
    private static final int COL_IN_PROG        = 0xFFFF8C00;
    private static final int COL_CAN_CLAIM      = 0xFF7EC87E;
    private static final int COL_COMPLETED      = 0xFF787880;
    private static final int COL_LOCKED         = 0xFF503838;

    private static final int COL_ROW_BG_CH      = 0xFF3A3A3A;
    private static final int COL_ROW_BG_OUTER   = 0xAA909090;
    private static final int COL_ROW_BG_INNER   = 0xFF4A4A4A;

    private static final int COL_SEL_INNER      = 0xCCFEFCF5;
    private static final int COL_SEL_TEXT       = 0xFF3A3A3A;

    private static final int COL_TRACK_BG       = 0xFFFEFCF5;
    private static final int COL_TRACK_TEXT     = 0xFF3A3A3A;

    private boolean rewardPopupOpen = false;
    private static final int MAX_INLINE_REWARDS = 5;
    private static final int POPUP_W = 200;
    private static final int POPUP_H = 160;

    private long lastScrollSoundTime = 0;
    private static final long SCROLL_SOUND_COOLDOWN = 60;

    private enum NodeType   { CHAPTER, QUEST }
    private enum QuestStatus { CAN_START, IN_PROGRESS, CAN_CLAIM, COMPLETED, LOCKED }

    private boolean suppressOpenSound = false;
    private boolean firstInit = true;

    private static class TreeNode {
        NodeType    type;
        String      label;
        boolean     expanded = true;
        Chapter     chapter;
        Quest       quest;
        QuestStatus status = QuestStatus.LOCKED;

        TreeNode(Chapter c) {
            type = NodeType.CHAPTER;
            chapter = c;
            label = c.getTitle().getString();
            if (label == null || label.isBlank()) label = UIQuestLang.get("uiquest.screen.chapter_default");
            expanded = false;
        }
        TreeNode(Quest q, QuestStatus status) {
            type = NodeType.QUEST;
            quest = q;
            label = q.getTitle().getString();
            if (label == null || label.isBlank()) label = UIQuestLang.get("uiquest.screen.quest_default");
            this.status = status;
            expanded = false;
        }
    }

    private static class GroupEntry {
        String         label;
        ChapterGroup   group;
        List<TreeNode> nodes = new ArrayList<>();
        List<TreeNode> flat  = new ArrayList<>();
        GroupEntry(String l, ChapterGroup g) { label = l; group = g; }
    }

    private final List<GroupEntry> groups = new ArrayList<>();
    private int activeGroupIdx = 0;
    private TreeNode selectedNode = null;

    private int  listScrollOffset = 0, listVisibleRows = 0;
    private boolean listScrollDragging = false;
    private int  listScrollDragStartY, listScrollDragStartOffset;

    private int  descScrollOffset = 0;
    private boolean descScrollDragging = false;
    private int  descScrollDragStartY, descScrollDragStartOffset;

    private int  tasksScrollOffset = 0;
    private boolean tasksScrollDragging = false;
    private int  tasksScrollDragStartY, tasksScrollDragStartOffset;

    private int uiX, uiY, uiW, uiH;
    private int tabBarX, tabBarY, tabBarW;
    private int listX, listY, listH;
    private int detailX, detailY, detailW, detailH;

    private int dHeaderY, dHeaderH;
    private int dTasksY,  dTasksH;
    private int dDescY,   dDescH;
    private int dRewardY, dRewardH;
    private int dFooterY, dFooterH;

    private static final int TASKS_MAX_H   = 66;
    private static final int REWARD_H      = 34;
    private static final int FOOTER_H      = 26;
    private static final int ICON_SIZE     = 16;
    private static final int MAX_VIS_TASKS = 3;

    private int trackBtnX, trackBtnY;
    private int openBtnX, openBtnY;
    private static final int TRACK_BTN_W = 94;
    private static final int TRACK_BTN_H = 17;

    private static final int OPEN_BTN_W  = 60;
    private static final int OPEN_BTN_H  = TRACK_BTN_H;

    private int settingsBtnX, settingsBtnY;
    private static final int SETTINGS_BTN_SIZE = 20;

    private ItemStack hoveredRewardStack = null;
    private Reward hoveredReward = null;
    private Reward hoveredRewardClickable = null;

    private Component hoveredRewardTooltip = null;

    private static int savedGroupIdx = 0;
    private static long savedQuestId = -1L;
    private static int savedListScroll = 0;
    private static int savedDescScroll = 0;
    private static int savedTasksScroll = 0;
    private static final Set<Long> savedExpandedChapters = new HashSet<>();

    public QuestBrowserScreen() {
        super(Component.literal("Quest Browser"));
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
        listH = uiH - HEADER_H - SEARCH_H - 2;

        detailX = listX + LIST_W + 1;
        detailY = uiY + HEADER_H;
        detailW = uiX + uiW - SIDE_PAD - detailX;
        detailH = uiH - HEADER_H;

        dHeaderY = detailY;
        dHeaderH = 48;
        dTasksY  = dHeaderY + dHeaderH + 1;
        dTasksH  = TASKS_MAX_H;
        dFooterY = detailY + detailH - FOOTER_H;
        dFooterH = FOOTER_H;
        dRewardY = dFooterY - REWARD_H - 1;
        dRewardH = REWARD_H;
        dDescY   = dTasksY + dTasksH + 12;
        subtitleH = font.lineHeight + 8;
        dDescY  += subtitleH;
        dDescH   = dRewardY - dDescY - font.lineHeight;

        listVisibleRows = Math.max(1, (listH - (SEARCH_H + 6)) / ROW_H);

        settingsBtnX = uiX - 12;
        settingsBtnY = tabBarY + (TAB_H_TOP - SETTINGS_BTN_SIZE) / 2 - 5;

        for (GroupEntry g : groups)
            for (TreeNode n : g.nodes)
                if (n.type == NodeType.CHAPTER && savedExpandedChapters.contains(n.chapter.id))
                    n.expanded = true;


        buildGroups();
        for (GroupEntry g : groups)
            for (TreeNode n : g.nodes)
                if (n.type == NodeType.CHAPTER && savedExpandedChapters.contains(n.chapter.id))
                    n.expanded = true;

        if (savedGroupIdx < groups.size()) {
            activeGroupIdx = savedGroupIdx;
        }
        if (!groups.isEmpty()) {
            rebuildFlat(groups.get(activeGroupIdx));
        }

        long trackedId = UIQuestConfig.TRACKED_QUEST_ID.get();
        if (trackedId >= 0) {
            autoSelectTrackedQuest();
        } else if (savedQuestId >= 0 && !groups.isEmpty()) {
            for (TreeNode n : groups.get(activeGroupIdx).flat)
                if (n.type == NodeType.QUEST && n.quest.id == savedQuestId)
                    selectedNode = n;
            listScrollOffset = savedListScroll;
            descScrollOffset = savedDescScroll;
            tasksScrollOffset = savedTasksScroll;
        }

        if (firstInit) {
            firstInit = false;
            if (!suppressOpenSound) {
                UISounds.play(UISounds.UI_BASE);
            } else {
                suppressOpenSound = false;
            }
        }
    }

    private void buildGroups() {
        groups.clear();
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        if (file == null) return;
        TeamData teamData = file.selfTeamData;
        if (teamData == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        UUID playerUUID = mc.player.getUUID();

        Map<String, ChapterGroup>  groupObjMap  = new LinkedHashMap<>();
        Map<String, List<Chapter>> groupChapMap = new LinkedHashMap<>();
        List<Chapter> ungrouped = new ArrayList<>();

        for (Chapter chapter : file.getAllChapters()) {
            if (chapter.isAlwaysInvisible()) continue;
            if (!UIQuestConfig.SHOW_COMPLETED_CHAPTERS.get()) {
                boolean allQuestsDone = !chapter.getQuests().isEmpty()
                        && chapter.getQuests().stream()
                        .allMatch(q -> getStatus(teamData, q, playerUUID) == QuestStatus.COMPLETED);
                if (allQuestsDone) continue;
            }
            ChapterGroup g = chapter.getGroup();
            if (g != null) {
                groupObjMap.putIfAbsent(g.getRawTitle(), g);
                groupChapMap.computeIfAbsent(g.getRawTitle(), k -> new ArrayList<>()).add(chapter);
            } else ungrouped.add(chapter);
        }

        for (Map.Entry<String, ChapterGroup> e : groupObjMap.entrySet()) {
            ChapterGroup g = e.getValue();
            String label = g.getTitle().getString();
            if (label == null || label.isBlank()) label = e.getKey();
            GroupEntry entry = new GroupEntry(label, g);
            for (Chapter ch : groupChapMap.getOrDefault(e.getKey(), List.of())) {
                entry.nodes.add(new TreeNode(ch));
                for (Quest quest : ch.getQuests()) {
                    if (!quest.isVisible(teamData)) continue;
                    QuestStatus st = getStatus(teamData, quest, playerUUID);
                    if (st == QuestStatus.LOCKED) continue;
                    if (st == QuestStatus.COMPLETED && !UIQuestConfig.SHOW_COMPLETED_QUESTS.get()) continue;
                    entry.nodes.add(new TreeNode(quest, st));
                }
            }
            groups.add(entry);
        }

        if (!ungrouped.isEmpty()) {
            GroupEntry entry = new GroupEntry(UIQuestLang.get("uiquest.screen.group_other"), null);
            for (Chapter ch : ungrouped) {
                entry.nodes.add(new TreeNode(ch));
                for (Quest quest : ch.getQuests()) {
                    if (!quest.isVisible(teamData)) continue;
                    QuestStatus st = getStatus(teamData, quest, playerUUID);
                    if (st == QuestStatus.LOCKED) continue;
                    entry.nodes.add(new TreeNode(quest, st));
                }
            }
            groups.add(entry);
        }

        if (!groups.isEmpty()) rebuildFlat(groups.get(activeGroupIdx));
    }

    private void rebuildFlat(GroupEntry entry) {
        entry.flat.clear();
        String q = searchQuery.toLowerCase(Locale.ROOT).trim();

        if (q.isEmpty()) {
            TreeNode curChapter = null;
            for (TreeNode n : entry.nodes) {
                if (n.type == NodeType.CHAPTER) { curChapter = n; entry.flat.add(n); }
                else if (curChapter == null || curChapter.expanded) entry.flat.add(n);
            }
        } else {
            TreeNode curChapter = null;
            for (TreeNode n : entry.nodes) {
                if (n.type == NodeType.CHAPTER) {
                    curChapter = n;
                } else {
                    boolean chapterMatch = curChapter != null
                            && curChapter.label.toLowerCase(Locale.ROOT).contains(q);
                    boolean questMatch   = n.label.toLowerCase(Locale.ROOT).contains(q);

                    if (chapterMatch || questMatch) {
                        if (curChapter != null && !entry.flat.contains(curChapter))
                            entry.flat.add(curChapter);
                        if (chapterMatch || questMatch) entry.flat.add(n);
                    }
                }
            }
        }

        listVisibleRows = Math.max(1, (listH - (SEARCH_H + 6)) / ROW_H);
        listScrollOffset = Math.min(listScrollOffset, Math.max(0, entry.flat.size() - listVisibleRows));
    }

    private QuestStatus getStatus(TeamData td, Quest quest, UUID uuid) {
        if (td.hasUnclaimedRewards(uuid, quest)) return QuestStatus.CAN_CLAIM;
        if (td.isCompleted(quest))               return QuestStatus.COMPLETED;
        if (td.isStarted(quest))                 return QuestStatus.IN_PROGRESS;
        if (!quest.isVisible(td))                return QuestStatus.LOCKED;
        if (td.canStartTasks(quest))             return QuestStatus.CAN_START;
        return QuestStatus.LOCKED;
    }

    private void autoSelectTrackedQuest() {
        long trackedId = UIQuestConfig.TRACKED_QUEST_ID.get();
        if (trackedId < 0) return;

        for (int gi = 0; gi < groups.size(); gi++) {
            GroupEntry entry = groups.get(gi);
            for (TreeNode n : entry.nodes) {
                if (n.type == NodeType.QUEST && n.quest.id == trackedId) {
                    activeGroupIdx = gi;
                    Chapter questChapter = n.quest.getChapter();
                    for (TreeNode cn : entry.nodes)
                        if (cn.type == NodeType.CHAPTER && cn.chapter == questChapter)
                            cn.expanded = true;
                    rebuildFlat(entry);
                    selectedNode = n;
                    return;
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        hoveredRewardStack = null;
        hoveredReward = null;
        hoveredRewardTooltip = null;

        gfx.fill(0, 0, width, height, COL_BG_OVERLAY);

        renderTopTabBar(gfx, mouseX, mouseY);
        renderListCol(gfx, mouseX, mouseY, partialTick);
        renderDetailCol(gfx, mouseX, mouseY);

        if (rewardPopupOpen && selectedNode != null && selectedNode.type == NodeType.QUEST) {
            ClientQuestFile file = ClientQuestFile.INSTANCE;
            if (file != null && file.selfTeamData != null && Minecraft.getInstance().player != null) {
                renderRewardPopup(gfx, selectedNode.quest, file.selfTeamData,
                        Minecraft.getInstance().player.getUUID(), mouseX, mouseY);
            }
        }

        renderSettingsButton(gfx, mouseX, mouseY);

        if (hoveredRewardStack != null && !hoveredRewardStack.isEmpty())
            gfx.renderTooltip(font, hoveredRewardStack, mouseX, mouseY);
        if (hoveredRewardTooltip != null)
            gfx.renderTooltip(font, hoveredRewardTooltip, mouseX, mouseY);
        if (mouseX >= settingsBtnX && mouseX < settingsBtnX + SETTINGS_BTN_SIZE
                && mouseY >= settingsBtnY && mouseY < settingsBtnY + SETTINGS_BTN_SIZE) {
            gfx.renderTooltip(font, Component.literal(UIQuestLang.get("uiquest.tooltip.open_settings")), mouseX, mouseY);
        }
        if (selectedNode != null && selectedNode.type == NodeType.QUEST
                && mouseX >= openBtnX && mouseX < openBtnX + OPEN_BTN_W
                && mouseY >= openBtnY && mouseY < openBtnY + OPEN_BTN_H) {
            gfx.renderTooltip(font, Component.literal(UIQuestLang.get("uiquest.tooltip.open_ftb")), mouseX, mouseY);
        }
    }

    private void renderSettingsButton(GuiGraphics gfx, int mouseX, int mouseY) {
        boolean settingsHovered = mouseX >= settingsBtnX && mouseX < settingsBtnX + SETTINGS_BTN_SIZE
                && mouseY >= settingsBtnY && mouseY < settingsBtnY + SETTINGS_BTN_SIZE;

        gfx.fill(settingsBtnX - 2, settingsBtnY - 2,
                settingsBtnX + SETTINGS_BTN_SIZE + 2, settingsBtnY + SETTINGS_BTN_SIZE + 2,
                settingsHovered ? COL_GOLD : 0xFF888880);

        gfx.fill(settingsBtnX, settingsBtnY,
                settingsBtnX + SETTINGS_BTN_SIZE, settingsBtnY + SETTINGS_BTN_SIZE,
                settingsHovered ? 0xFF555555 : 0xFF3A3A3A);

        var rl = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("uiquest", "textures/setting.png");
        gfx.blit(rl,
                settingsBtnX, settingsBtnY,
                0, 0,
                SETTINGS_BTN_SIZE, SETTINGS_BTN_SIZE,
                SETTINGS_BTN_SIZE, SETTINGS_BTN_SIZE);
    }

    private void renderTopTabBar(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.fill(0, 0, width, tabBarY + TAB_H_TOP, COL_TAB_BG);
        if (groups.isEmpty()) return;

        int tabPadX = 14;
        int arrowY  = tabBarY + (TAB_H_TOP - font.lineHeight) / 2;

        int areaLeft  = tabBarX + SETTINGS_BTN_SIZE + 10 + TAB_ARROW_W;
        int areaRight = tabBarX + tabBarW - TAB_ARROW_W - 4;

        int visibleW = 0;
        int visibleCount = 0;
        for (int i = tabScrollOffset; i < groups.size(); i++) {
            int w = font.width(groups.get(i).label) + tabPadX * 2 + 2;
            if (visibleW + w > areaRight - areaLeft) break;
            visibleW += w;
            visibleCount++;
        }
        if (visibleCount > 0) visibleW -= 2;

        boolean leftActive = tabScrollOffset > 0;
        gfx.drawString(font, "◀", tabBarX + SETTINGS_BTN_SIZE + 10,
                arrowY, leftActive ? COL_GOLD : COL_TEXT_FAINT, false);

        boolean rightActive = (tabScrollOffset + visibleCount) < groups.size();
        gfx.drawString(font, "▶", tabBarX + tabBarW - TAB_ARROW_W + 2,
                arrowY, rightActive ? COL_GOLD : COL_TEXT_FAINT, false);

        int startX = areaLeft + (areaRight - areaLeft - visibleW) / 2;

        gfx.enableScissor(areaLeft, tabBarY, areaRight, tabBarY + TAB_H_TOP);
        for (int i = tabScrollOffset; i < tabScrollOffset + visibleCount; i++) {
            GroupEntry g  = groups.get(i);
            int itemW     = font.width(g.label) + tabPadX * 2;
            boolean active = (i == activeGroupIdx);
            if (active) {
                gfx.fill(startX, tabBarY, startX + itemW, tabBarY + TAB_H_TOP, 0x44FEFCF5);
                gfx.fill(startX + 2, tabBarY + TAB_H_TOP - 2, startX + itemW - 2,
                        tabBarY + TAB_H_TOP, COL_GOLD);
            }
            int textCol = active ? COL_TEXT : COL_TEXT_FAINT;
            int ty = tabBarY + (TAB_H_TOP - font.lineHeight) / 2;
            gfx.drawString(font, g.label, startX + tabPadX, ty, textCol, false);
            startX += itemW + 2;
        }
        gfx.disableScissor();
    }

    private void renderListCol(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (groups.isEmpty()) return;
        renderSearchBar(gfx, mouseX, mouseY);
        GroupEntry entry = groups.get(activeGroupIdx);

        int treeTop = listY + SEARCH_H + 6;
        int sbX     = listX + LIST_W - 5;
        int right   = sbX - 2;

        gfx.enableScissor(listX, treeTop, listX + LIST_W, listY + listH);

        String prevChapterId = null;

        for (int i = 0; i < listVisibleRows; i++) {
            int idx = i + listScrollOffset;
            if (idx >= entry.flat.size()) break;
            TreeNode node = entry.flat.get(idx);

            TreeNode prevFlat = (idx > 0) ? entry.flat.get(idx - 1) : null;
            TreeNode nextFlat = (idx + 1 < entry.flat.size()) ? entry.flat.get(idx + 1) : null;

            boolean isFirstInGroup = (prevFlat == null)
                    || (node.type == NodeType.CHAPTER && prevFlat.type != NodeType.CHAPTER)
                    || (node.type == NodeType.CHAPTER && prevFlat.type == NodeType.CHAPTER);

            int ry = treeTop + i * ROW_H;
            int rowTop = ry;
            int rowBot = ry + ROW_H;

            boolean selected = (node == selectedNode);

            if (node.type == NodeType.CHAPTER) {
                int topOff = (isFirstInGroup && prevFlat != null) ? 2 : 0;
                gfx.fill(listX, rowTop + topOff, right, rowBot, COL_ROW_BG_CH);
                if (selected) {
                    gfx.fill(listX, rowTop + topOff, right, rowBot, COL_SEL_INNER);
                    gfx.fill(listX, rowTop + topOff + 1, listX + 2, rowBot - 1, COL_GOLD);
                }
            } else {
                int margin = 2;
                boolean isFirstQuest = (prevFlat != null && prevFlat.type == NodeType.CHAPTER);
                int topOff = 0;
                int botOff = 0;

                gfx.fill(listX + margin, rowTop + topOff, right - margin, rowBot + botOff, COL_ROW_BG_OUTER);
                gfx.fill(listX + margin + 3, rowTop + topOff + 1, right - margin - 3, rowBot + botOff - 1, COL_ROW_BG_INNER);
                if (selected) {
                    gfx.fill(listX + margin + 3, rowTop + topOff + 1, right - margin - 3, rowBot + botOff - 1, COL_SEL_INNER);
                    gfx.fill(listX + margin, rowTop + topOff + 2, listX + margin + 2, rowBot + botOff - 2, COL_GOLD);
                }
            }

            int ty = ry + (ROW_H - font.lineHeight) / 2 + 1;

            if (node.type == NodeType.CHAPTER) {
                String arrow = node.expanded ? "▾" : "▸";
                gfx.drawString(font, arrow, listX + 6, ty, COL_GOLD, false);

                int iconX = listX + 17;
                int iconY = ry + (ROW_H - ICON_SIZE) / 2;
                Icon chapIcon = node.chapter.getIcon();
                if (!chapIcon.isEmpty()) {
                    chapIcon.draw(gfx, iconX, iconY, ICON_SIZE, ICON_SIZE);
                }

                int textCol = selected ? COL_SEL_TEXT : COL_GOLD;
                int nameX = chapIcon.isEmpty() ? listX + 17 : iconX + ICON_SIZE + 4;
                gfx.drawString(font, trimLabel(node.label.toUpperCase(Locale.ROOT), LIST_W - nameX + listX - 8),
                        nameX, ty, textCol, false);
            } else {
                int iconX = listX + 6;
                int iconY = ry + (ROW_H - ICON_SIZE) / 2;
                renderQuestIcon(gfx, node.quest, iconX, iconY);

                int nameX = iconX + ICON_SIZE + 5;
                int nameCol = selected ? COL_SEL_TEXT
                        : (node.status == QuestStatus.COMPLETED ? COL_TEXT_FAINT : COL_TEXT);

                QuestChainFinder.ChainInfo chain = QuestChainFinder.find(node.quest);
                int nameMaxW;
                if (node.status == QuestStatus.COMPLETED) {
                    String doneStr = "✔";
                    int boxSize = font.lineHeight + 2;
                    int doneX = right - boxSize - 8;
                    int doneY = ty - 1;
                    int bgCol = selected ? COL_SEL_TEXT : COL_GOLD;
                    int checkCol = selected ? COL_GOLD : COL_TEXT;
                    gfx.fill(doneX, doneY, doneX + boxSize, doneY + boxSize, bgCol);
                    gfx.drawString(font, doneStr,
                            doneX + (boxSize - font.width(doneStr)) / 2,
                            doneY + (boxSize - font.lineHeight) / 2 + 1,
                            checkCol, false);
                    nameMaxW = doneX - nameX - 2;
                } else if (chain.total() > 1 && UIQuestConfig.SHOW_CHAIN_IN_LIST.get()) {
                    String chainStr = "[" + chain.position() + "/" + chain.totalStr() + "]";
                    int chainW = font.width(chainStr);
                    int chainX = right - chainW - 8;
                    int chainCol = selected ? COL_SEL_TEXT : COL_TEXT;
                    gfx.drawString(font, chainStr, chainX, ty, chainCol, false);
                    nameMaxW = chainX - nameX - 2;
                } else {
                    nameMaxW = right - nameX - 8;
                }

                gfx.drawString(font, trimLabel(node.label, nameMaxW), nameX, ty, nameCol, false);
            }
        }

        gfx.disableScissor();

        renderScrollBar(gfx, sbX + 1, treeTop, listY + listH,
                entry.flat.size(), listVisibleRows, listScrollOffset);
    }

    private void renderQuestIcon(GuiGraphics gfx, Quest quest, int x, int y) {
        try {
            Icon icon = quest.getIcon();
            if (!icon.isEmpty()) {
                icon.draw(gfx, x, y, ICON_SIZE, ICON_SIZE);
                return;
            }
        } catch (Exception ignored) {}
        gfx.fill(x + 3, y + 3, x + ICON_SIZE - 3, y + ICON_SIZE - 3, COL_GOLD_FAINT);
    }

    private void renderDetailCol(GuiGraphics gfx, int mouseX, int mouseY) {
        if (selectedNode == null || selectedNode.type != NodeType.QUEST) {
            String hint = UIQuestLang.get("uiquest.screen.select_quest");
            int hw = font.width(hint);
            gfx.drawString(font, hint,
                    detailX + (detailW - hw) / 2,
                    detailY + detailH / 2 - font.lineHeight / 2,
                    COL_TEXT_FAINT, false);
            return;
        }

        Quest quest = selectedNode.quest;
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        if (file == null) return;
        TeamData teamData = file.selfTeamData;
        if (teamData == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        UUID uuid = mc.player.getUUID();

        renderDetailHeader(gfx, quest, teamData, uuid);
        renderDetailTasks(gfx, quest, teamData, mouseX, mouseY);
        renderDetailDesc(gfx, quest, mouseX, mouseY);
        renderDetailRewards(gfx, quest, teamData, uuid, mouseX, mouseY);
        renderDetailFooter(gfx, quest, mouseX, mouseY);
    }

    private void renderDetailHeader(GuiGraphics gfx, Quest quest, TeamData td, UUID uuid) {
        int iconX = detailX + 8;
        int iconY = dHeaderY + 8;
        renderQuestIcon(gfx, quest, iconX, iconY);

        int textX = iconX + ICON_SIZE + 8;
        String typeStr = getQuestType(quest);
        gfx.drawString(font, trimLabel(typeStr, detailW - (textX - detailX) - OPEN_BTN_W - 16),
                textX, dHeaderY + 8, COL_TEXT_FAINT, false);
        gfx.drawString(font, trimLabel(quest.getTitle().getString(), detailW - (textX - detailX) - OPEN_BTN_W - 16),
                textX, dHeaderY + 20, COL_TITLE, false);

        QuestStatus status = getStatus(td, quest, uuid);
        int statusCol = statusColor(status);
        gfx.fill(detailX + 8, dHeaderY + 36, detailX + 11, dHeaderY + 39, statusCol);
        gfx.drawString(font, statusLabel(status), detailX + 14, dHeaderY + 34, statusCol, false);
    }

    private void renderDetailTasks(GuiGraphics gfx, Quest quest, TeamData td, int mouseX, int mouseY) {
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        if (file == null) return;

        gfx.fill(detailX + 4, dTasksY - 4, detailX + detailW - 4, dTasksY - 3, COL_SEP_LINE);

        List<Task> taskList = quest.getTasksAsList();
        int total = taskList.size();
        int maxScroll = Math.max(0, total - MAX_VIS_TASKS);
        tasksScrollOffset = Math.min(tasksScrollOffset, maxScroll);

        int sbX      = detailX + detailW - 6;
        int TASK_ROW = dTasksH / MAX_VIS_TASKS;

        gfx.enableScissor(detailX, dTasksY, sbX, dTasksY + dTasksH);

        for (int ti = 0; ti < MAX_VIS_TASKS; ti++) {
            int taskIdx = ti + tasksScrollOffset;
            if (taskIdx >= total) break;

            var task  = taskList.get(taskIdx);
            int rowY    = dTasksY + ti * TASK_ROW;
            int rowEnd  = rowY + TASK_ROW - 1;

            gfx.fill(detailX + 4, rowY + 1, sbX - 2, rowEnd, 0x33808080);

            boolean done = td.isCompleted(task);
            long max = task.getMaxProgress();
            long cur = td.getProgress(task);

            String tName = task.getRawTitle();
            if (tName == null || tName.isBlank()) {
                if (task instanceof ItemTask it) tName = it.getItemStack().getHoverName().getString();
                else tName = task.getTitle().getString();
            }

            int x     = detailX + 10;
            int textY = rowY + (TASK_ROW - font.lineHeight) / 2 + 1;

            int bx = x, by = textY + (font.lineHeight - 6) / 2;
            int bs = 6;

            if (done) {
                gfx.fill(bx, by, bx + bs, by + bs, COL_GOLD);
            } else {
                gfx.fill(bx,          by,          bx + bs, by + 1,      0xAAC8AA64);
                gfx.fill(bx,          by + bs - 1, bx + bs, by + bs,     0xAAC8AA64);
                gfx.fill(bx,          by,          bx + 1,  by + bs,     0xAAC8AA64);
                gfx.fill(bx + bs - 1, by,          bx + bs, by + bs,     0xAAC8AA64);
            }

            String progStr = (max > 1) ? " [" + cur + "/" + max + "]" : "";
            gfx.drawString(font, trimLabel(tName, detailW - 44) + progStr,
                    x + 8, textY, done ? COL_TEXT_FAINT : COL_TEXT, false);

            if (!done) {
                String submitLabel = null;
                if (task instanceof CheckmarkTask) {
                    submitLabel = UIQuestLang.get("uiquest.screen.task.click_complete");
                } else if (task instanceof ItemTask it && it.consumesResources() && !it.isTaskScreenOnly()) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        boolean hasItem = mc.player.getInventory().items.stream()
                                .anyMatch(stack -> it.test(stack) && !stack.isEmpty());
                        if (hasItem) {
                            submitLabel = UIQuestLang.get("uiquest.screen.task.click_submit");
                        }
                    }
                }
                if (submitLabel != null) {
                    gfx.drawString(font, submitLabel,
                            sbX - font.width(submitLabel) - 6,
                            textY,
                            COL_GOLD, false);
                }
            }

            if (!done) {
                long displayCur = done ? max : cur;
                long displayMax = Math.max(max, 1);

                int barY = textY + font.lineHeight + 1;
                if (barY + 2 < rowEnd) {
                    int barW = detailW - 28;
                    gfx.fill(x + 8, barY, x + 8 + barW, barY + 2, 0x1AC8AA64);
                    gfx.fill(x + 8, barY, x + 8 + (int)(barW * displayCur / (float)displayMax), barY + 2, 0x66C8AA64);
                }
            }
        }

        gfx.disableScissor();

        if (total > MAX_VIS_TASKS)
            renderScrollBar(gfx, sbX, dTasksY + 2, dTasksY + dTasksH - 2,
                    total, MAX_VIS_TASKS, tasksScrollOffset);
    }

    private void renderDetailDesc(GuiGraphics gfx, Quest quest, int mouseX, int mouseY) {
        Component subtitle = quest.getSubtitle();
        if (subtitle != null && !subtitle.getString().isBlank()) {
            int subY = dDescY - subtitleH;
            gfx.drawString(font, trimLabel(subtitle.getString(), detailW - 20),
                    detailX + 8, subY + (subtitleH - font.lineHeight) / 2,
                    COL_GOLD, false);
        }

        int contentY = dDescY + 4;
        int x = detailX + 8;
        int sbX = detailX + detailW - 6;

        List<Component> lines = quest.getDescription();

        int totalPixels = lines.size() * font.lineHeight;

        int pixelOffset = descScrollOffset;

        int scissorBottom = rewardPopupOpen
                ? Math.min(dDescY + dDescH, dRewardY - POPUP_H - 4)
                : dDescY + dDescH;
        gfx.enableScissor(detailX, dDescY, detailX + detailW - 6, Math.max(dDescY, scissorBottom));

        int drawY = contentY - pixelOffset;
        for (Component line : lines) {
            Object img = ImageComponentAccess.findIn(line);
            if (img != null) {
                int imgW = ImageComponentAccess.getWidth(img);
                int imgH = ImageComponentAccess.getHeight(img);
                if (imgW <= 0) imgW = 64;
                if (imgH <= 0) imgH = 64;

                if (drawY + imgH > dDescY && drawY < dDescY + dDescH) {
                    int drawX = switch (ImageComponentAccess.getAlignName(img)) {
                        case "CENTER" -> detailX + (detailW - imgW) / 2;
                        case "RIGHT"  -> detailX + detailW - imgW - 8;
                        default       -> x;
                    };
                    ImageComponentAccess.getImage(img).draw(gfx, drawX, drawY, imgW, imgH);
                }
                drawY += imgH + 4;
            } else {
                if (drawY + font.lineHeight > dDescY && drawY < dDescY + dDescH) {
                    List<net.minecraft.util.FormattedCharSequence> wrapped =
                            font.split(line, detailW - 20);
                    for (var seq : wrapped) {
                        if (drawY + font.lineHeight > dDescY && drawY < dDescY + dDescH) {
                            gfx.drawString(font, seq, x, drawY, COL_TEXT_DIM, false);
                        }
                        drawY += font.lineHeight;
                    }
                } else {
                    drawY += font.split(line, detailW - 20).size() * font.lineHeight;
                }
            }
        }

        gfx.disableScissor();

        int realTotalPixels = 0;
        for (Component line : lines) {
            Object img = ImageComponentAccess.findIn(line);
            if (img != null) {
                int h = ImageComponentAccess.getHeight(img);
                realTotalPixels += (h > 0 ? h : 64) + 4;
            } else {
                realTotalPixels += font.split(line, detailW - 20).size() * font.lineHeight;
            }
        }

        renderScrollBar(gfx, sbX, dDescY + 2, dDescY + dDescH - 2,
                realTotalPixels, dDescH, descScrollOffset);
    }

    private void renderDetailRewards(GuiGraphics gfx, Quest quest, TeamData td, UUID uuid, int mouseX, int mouseY) {
        gfx.fill(detailX + 4, dRewardY - 1, detailX + detailW - 4, dRewardY, COL_SEP_LINE);

        int x = detailX + 8, y = dRewardY + 4;
        gfx.drawString(font, UIQuestLang.get("uiquest.screen.reward.title"), x, y, COL_TEXT_FAINT, false);
        y += font.lineHeight + 3;

        List<Reward> rewards = new ArrayList<>(quest.getRewards());
        int rx = x;
        int maxShow = MAX_INLINE_REWARDS;

        for (int i = 0; i < Math.min(rewards.size(), maxShow); i++) {
            Reward reward = rewards.get(i);
            renderSingleReward(gfx, td, uuid, quest, reward, rx, y, mouseX, mouseY);
            rx += ICON_SIZE + 5;
        }

        if (rewards.size() > maxShow) {
            int moreBtnX = rx;
            int moreBtnY = y;
            boolean hovered = mouseX >= moreBtnX && mouseX < moreBtnX + ICON_SIZE + 4
                    && mouseY >= moreBtnY && mouseY < moreBtnY + ICON_SIZE;
            gfx.fill(moreBtnX, moreBtnY, moreBtnX + ICON_SIZE + 4, moreBtnY + ICON_SIZE,
                    hovered ? 0xFF555555 : 0xFF3A3A3A);
            gfx.fill(moreBtnX - 1, moreBtnY - 1, moreBtnX + ICON_SIZE + 5, moreBtnY + ICON_SIZE + 1,
                    0xFF888880);
            String moreLabel = "+" + (rewards.size() - maxShow);
            gfx.drawString(font, moreLabel,
                    moreBtnX + (ICON_SIZE + 4 - font.width(moreLabel)) / 2,
                    moreBtnY + (ICON_SIZE - font.lineHeight) / 2,
                    COL_GOLD, false);
        }
    }

    private void renderSingleReward(GuiGraphics gfx, TeamData td, UUID uuid, Quest quest,
                                    Reward reward, int rx, int ry, int mouseX, int mouseY) {
        boolean claimed  = td.isRewardClaimed(uuid, reward);
        boolean canClaim = td.hasUnclaimedRewards(uuid, quest);

        if (reward instanceof XPReward xr) {
            Icon icon = xr.getIcon();
            int borderCol = claimed ? 0x55404040 : canClaim ? 0xFFC8AA64 : 0x33C8AA64;
            gfx.fill(rx - 1, ry - 1, rx + ICON_SIZE + 1, ry + ICON_SIZE + 1, borderCol);
            gfx.fill(rx, ry, rx + ICON_SIZE, ry + ICON_SIZE, 0xFF2A2A2A);

            if (!icon.isEmpty()) {
                icon.draw(gfx, rx, ry, ICON_SIZE, ICON_SIZE);
            }

            String btnText = xr.getButtonText();
            String amtStr;
            try {
                int amt = Integer.parseInt(btnText.replace("+", "").trim());
                amtStr = amt >= 1000 ? (amt / 1000) + "k" : String.valueOf(amt);
            } catch (NumberFormatException e) {
                amtStr = btnText;
            }
            gfx.pose().pushPose();
            gfx.pose().translate(0, 0, 200);
            gfx.drawString(font, amtStr,
                    rx + ICON_SIZE - font.width(amtStr),
                    ry + ICON_SIZE - font.lineHeight + 1,
                    0xFFECEAE0, true);
            gfx.pose().popPose();

            if (!claimed && canClaim) {
                gfx.pose().pushPose();
                gfx.pose().translate(0, 0, 300);
                String exclaim = "!";
                int exX = rx + ICON_SIZE - font.width(exclaim);
                int exY = ry - 1;
                gfx.fill(exX - 1, exY - 1, exX + font.width(exclaim) + 1, exY + font.lineHeight, 0xFF000000);
                gfx.drawString(font, exclaim, exX + 1, exY, 0xFFFEFCF5, false);
                gfx.pose().popPose();
            }
            if (mouseX >= rx && mouseX < rx + ICON_SIZE && mouseY >= ry && mouseY < ry + ICON_SIZE && !claimed) {
                hoveredReward = reward;
                hoveredRewardTooltip = Component.literal(xr.getButtonText().replace("+", "") + " XP");
            }
            return;
        }

        if (reward instanceof XPLevelsReward xlr) {
            Icon icon = xlr.getIcon();
            int borderCol = claimed ? 0x55404040 : canClaim ? 0xFFC8AA64 : 0x33C8AA64;
            gfx.fill(rx - 1, ry - 1, rx + ICON_SIZE + 1, ry + ICON_SIZE + 1, borderCol);
            gfx.fill(rx, ry, rx + ICON_SIZE, ry + ICON_SIZE, 0xFF2A2A2A);

            if (!icon.isEmpty()) {
                icon.draw(gfx, rx, ry, ICON_SIZE, ICON_SIZE);
            }

            String amtStr = xlr.getButtonText();
            gfx.pose().pushPose();
            gfx.pose().translate(0, 0, 200);
            gfx.drawString(font, amtStr,
                    rx + ICON_SIZE - font.width(amtStr),
                    ry + ICON_SIZE - font.lineHeight + 1,
                    0xFF7CFC00, true);
            gfx.pose().popPose();

            if (!claimed && canClaim) {
                gfx.pose().pushPose();
                gfx.pose().translate(0, 0, 300);
                String exclaim = "!";
                int exX = rx + ICON_SIZE - font.width(exclaim);
                int exY = ry - 1;
                gfx.fill(exX - 1, exY - 1, exX + font.width(exclaim) + 1, exY + font.lineHeight, 0xFF000000);
                gfx.drawString(font, exclaim, exX + 1, exY, 0xFFFEFCF5, false);
                gfx.pose().popPose();
            }
            if (mouseX >= rx && mouseX < rx + ICON_SIZE && mouseY >= ry && mouseY < ry + ICON_SIZE && !claimed) {
                hoveredReward = reward;
                hoveredRewardTooltip = Component.literal(xlr.getButtonText().replace("+", "") + " Lv");
            }
            return;
        }



        if (reward instanceof ItemReward ir) {
            ItemStack stack = ir.getItem();
            int cnt = ir.getCount();

            if (!claimed && canClaim) {
                gfx.fill(rx - 1, ry - 1, rx + ICON_SIZE + 1, ry + ICON_SIZE + 1, 0xFFC8AA64);
            }
            gfx.renderItem(stack, rx, ry);

            if (!claimed && canClaim) {
                gfx.pose().pushPose();
                gfx.pose().translate(0, 0, 300);
                String exclaim = "!";
                int exX = rx + ICON_SIZE - font.width(exclaim);
                int exY = ry - 1;
                gfx.fill(exX - 1, exY - 1, exX + font.width(exclaim) + 1, exY + font.lineHeight, 0xFF000000);
                gfx.drawString(font, exclaim, exX + 1, exY, 0xFFFEFCF5, false);
                gfx.pose().popPose();
            }

            if (cnt > 1) {
                gfx.pose().pushPose();
                gfx.pose().translate(0, 0, 200);
                String cntStr = String.valueOf(cnt);
                gfx.drawString(font, cntStr,
                        rx + ICON_SIZE - font.width(cntStr),
                        ry + ICON_SIZE - font.lineHeight + 1,
                        COL_TEXT, true);
                gfx.pose().popPose();
            }

            if (mouseX >= rx && mouseX < rx + ICON_SIZE && mouseY >= ry && mouseY < ry + ICON_SIZE) {
                hoveredRewardStack = stack;
                hoveredReward = reward;
            }
        } else {
            boolean hovered = mouseX >= rx && mouseX < rx + ICON_SIZE && mouseY >= ry && mouseY < ry + ICON_SIZE;
            int borderCol = claimed ? 0x55404040 : canClaim ? 0xFFC8AA64 : 0x33C8AA64;
            gfx.fill(rx - 1, ry - 1, rx + ICON_SIZE + 1, ry + ICON_SIZE + 1, borderCol);
            gfx.fill(rx, ry, rx + ICON_SIZE, ry + ICON_SIZE, 0xFF2A2A2A);

            Icon rIcon = reward.getIcon();
            if (!rIcon.isEmpty()) {
                rIcon.draw(gfx, rx, ry, ICON_SIZE, ICON_SIZE);
            } else {
                gfx.drawString(font, "?",
                        rx + (ICON_SIZE - font.width("?")) / 2,
                        ry + (ICON_SIZE - font.lineHeight) / 2,
                        COL_GOLD, false);
            }

            if (!claimed && canClaim) {
                gfx.pose().pushPose();
                gfx.pose().translate(0, 0, 300);
                String exclaim = "!";
                int exX = rx + ICON_SIZE - font.width(exclaim);
                int exY = ry - 1;
                gfx.fill(exX - 1, exY - 1, exX + font.width(exclaim) + 1, exY + font.lineHeight, 0xFF000000);
                gfx.drawString(font, exclaim, exX, exY, 0xFFFEFCF5, false);
                gfx.pose().popPose();
            }

            if (hovered && !claimed) {
                hoveredReward = reward;
                hoveredRewardClickable = reward;
            }
        }
    }

    private void renderRewardPopup(GuiGraphics gfx, Quest quest, TeamData td, UUID uuid, int mouseX, int mouseY) {
        int popX = detailX + detailW - POPUP_W - 4;
        int popY = dRewardY - POPUP_H - 4;

        gfx.fill(popX - 1, popY - 1, popX + POPUP_W + 1, popY + POPUP_H + 1, 0xFFC8AA64);
        gfx.fill(popX, popY, popX + POPUP_W, popY + POPUP_H, 0xFF1E1E1E);

        gfx.drawString(font, UIQuestLang.get("uiquest.screen.popup.all_rewards"), popX + 6, popY + 5, COL_TEXT_FAINT, false);

        int closeX = popX + POPUP_W - 14;
        int closeY = popY + 3;
        gfx.drawString(font, "✕", closeX, closeY, COL_TEXT_FAINT, false);

        int rx = popX + 6;
        int ry = popY + 5 + font.lineHeight + 4;
        int rowEnd = popX + POPUP_W - 6;

        for (Reward reward : new ArrayList<>(quest.getRewards())) {
            if (rx + ICON_SIZE > rowEnd) {
                rx = popX + 6;
                ry += ICON_SIZE + 4;
            }
            if (ry + ICON_SIZE > popY + POPUP_H - 4) break;
            renderSingleReward(gfx, td, uuid, quest, reward, rx, ry, mouseX, mouseY);
            rx += ICON_SIZE + 5;
        }
    }

    private void renderDetailFooter(GuiGraphics gfx, Quest quest, int mouseX, int mouseY) {
        trackBtnX = detailX + detailW - TRACK_BTN_W - 24;
        trackBtnY = dFooterY + (dFooterH - TRACK_BTN_H) / 2;

        openBtnX = trackBtnX - OPEN_BTN_W - 6;
        openBtnY = trackBtnY;

        openBtnX = detailX + detailW - OPEN_BTN_W - 10;
        openBtnY = dHeaderY + 12;
        QuestChainFinder.ChainInfo chain = QuestChainFinder.find(quest);
        if (chain.total() > 1 && UIQuestConfig.SHOW_CHAIN_IN_LIST.get()) {
            String chainStr = UIQuestLang.get("uiquest.screen.chain.step") + " [" + chain.position() + "/" + chain.totalStr() + "]";
            int chainX = detailX + detailW - font.width(chainStr) - 8;
            int chainY = dHeaderY + 34;
            gfx.drawString(font, chainStr, chainX, chainY, COL_TEXT, false);
        }
        gfx.fill(openBtnX - 1, openBtnY - 1, openBtnX + OPEN_BTN_W + 1, openBtnY + OPEN_BTN_H + 1, 0xFFC8AA64);
        gfx.fill(openBtnX, openBtnY, openBtnX + OPEN_BTN_W, openBtnY + OPEN_BTN_H, 0xFF3A3A3A);
        String openLabel = UIQuestLang.get("uiquest.screen.btn.open_quest");
        gfx.drawString(font, openLabel,
                openBtnX + (OPEN_BTN_W - font.width(openLabel)) / 2,
                openBtnY + (OPEN_BTN_H - font.lineHeight) / 2,
                COL_GOLD, false);

        long trackedId = UIQuestConfig.TRACKED_QUEST_ID.get();
        boolean tracked = (trackedId == quest.id);

        int fillCol   = tracked ? 0xFF3A3A3A : COL_TRACK_BG;
        int borderCol = tracked ? 0xFFC8AA64 : 0xFF888880;
        String label = tracked ? UIQuestLang.get("uiquest.screen.btn.tracking")
                : UIQuestLang.get("uiquest.screen.btn.track");

        gfx.fill(trackBtnX - 1, trackBtnY - 1, trackBtnX + TRACK_BTN_W + 1, trackBtnY + TRACK_BTN_H + 1, borderCol);
        gfx.fill(trackBtnX, trackBtnY, trackBtnX + TRACK_BTN_W, trackBtnY + TRACK_BTN_H, fillCol);
        int labelCol = tracked ? COL_GOLD : COL_TRACK_TEXT;
        gfx.drawString(font, label,
                trackBtnX + (TRACK_BTN_W - font.width(label)) / 2,
                trackBtnY + (TRACK_BTN_H - font.lineHeight) / 2 + 1,
                labelCol, false);
    }

    private void renderScrollBar(GuiGraphics gfx, int sbX, int top, int bottom,
                                 int total, int visible, int offset) {
        if (total <= 0 || total <= visible) return;
        int sbH    = bottom - top;
        if (sbH <= 0) return;
        int thumbH = Math.max(8, sbH * visible / total);
        int maxS   = Math.max(1, total - visible);
        int thumbY = top + (sbH - thumbH) * offset / maxS;

        gfx.fill(sbX + 1, top, sbX + 2, bottom, 0x44C8AA64);
        gfx.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, 0xFFC8AA64);

        if (offset > 0) {
            int ax = sbX + 1, ay = top - 8;
            gfx.fill(ax, ay + 4, ax + 1, ay + 7, 0xAAC8AA64);
            gfx.fill(ax - 1, ay + 5, ax + 2, ay + 7, 0x88C8AA64);
            gfx.fill(ax - 2, ay + 6, ax + 3, ay + 7, 0x55C8AA64);
        }
        if (offset < maxS) {
            int ax = sbX + 1, ay = bottom + 1;
            gfx.fill(ax, ay, ax + 1, ay + 3, 0xAAC8AA64);
            gfx.fill(ax - 1, ay, ax + 2, ay + 2, 0x88C8AA64);
            gfx.fill(ax - 2, ay, ax + 3, ay + 1, 0x55C8AA64);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int rawX = (int)mx, rawY = (int)my;
        int x = (int)mx, y = (int)my;

        int sw = LIST_W - 8;
        if (rawX >= listX && rawX < listX + sw && rawY >= listY + 1 && rawY < listY + SEARCH_H + 1) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;

        if (rawX >= settingsBtnX && rawX < settingsBtnX + SETTINGS_BTN_SIZE
                && rawY >= settingsBtnY && rawY < settingsBtnY + SETTINGS_BTN_SIZE) {
            UISounds.play(UISounds.UI_CLICK);
            suppressOpenSound = true;
            minecraft.setScreen(new SettingsScreen(this));
            return true;
        }

        if (rewardPopupOpen && selectedNode != null && selectedNode.type == NodeType.QUEST) {
            int popX = detailX + detailW - POPUP_W - 4;
            int popY = dRewardY - POPUP_H - 4;

            int closeX = popX + POPUP_W - 14;
            int closeY = popY + 3;
            if (x >= closeX - 2 && x < closeX + font.width("✕") + 4
                    && y >= closeY - 2 && y < closeY + font.lineHeight + 2) {
                rewardPopupOpen = false;
                UISounds.play(UISounds.UI_CLICK);
                return true;
            }

            if (x >= popX && x < popX + POPUP_W && y >= popY && y < popY + POPUP_H) {
                if (hoveredReward != null) {
                    if (hoveredReward instanceof ChoiceReward choiceReward) {
                        new SelectChoiceRewardScreen(choiceReward).openGui();
                    } else {
                        NetworkManager.sendToServer(new ClaimRewardMessage(hoveredReward.id, true));
                    }
                }
                return true;
            }

            rewardPopupOpen = false;
            return true;
        }

        if (!groups.isEmpty() && y >= tabBarY && y < tabBarY + TAB_H_TOP) {
            if (x >= tabBarX + SETTINGS_BTN_SIZE + 10
                    && x < tabBarX + SETTINGS_BTN_SIZE + 10 + TAB_ARROW_W
                    && tabScrollOffset > 0) {
                UISounds.play(UISounds.UI_CHAPTER);
                tabScrollOffset--;
                return true;
            }
            if (x >= tabBarX + tabBarW - TAB_ARROW_W && x < tabBarX + tabBarW) {
                int tabPadX = 14;
                int availW  = tabBarW - TAB_ARROW_W * 2 - 8;
                int usedW   = 0;
                boolean canScroll = false;
                for (int i = tabScrollOffset; i < groups.size(); i++) {
                    int w = font.width(groups.get(i).label) + tabPadX * 2 + 2;
                    if (usedW + w > availW) { canScroll = true; break; }
                    usedW += w;
                }
                if (canScroll) {
                    UISounds.play(UISounds.UI_CHAPTER);
                    tabScrollOffset++;
                    return true;
                }
            }
            int tabPadX = 14;
            int areaLeft  = tabBarX + SETTINGS_BTN_SIZE + 10 + TAB_ARROW_W;
            int areaRight = tabBarX + tabBarW - TAB_ARROW_W - 4;

            int visibleW = 0;
            int visibleCount = 0;
            for (int i = tabScrollOffset; i < groups.size(); i++) {
                int w = font.width(groups.get(i).label) + tabPadX * 2 + 2;
                if (visibleW + w > areaRight - areaLeft) break;
                visibleW += w;
                visibleCount++;
            }
            if (visibleCount > 0) visibleW -= 2;

            int startX = areaLeft + (areaRight - areaLeft - visibleW) / 2;

            for (int i = tabScrollOffset; i < tabScrollOffset + visibleCount; i++) {
                int itemW = font.width(groups.get(i).label) + tabPadX * 2;
                if (x >= startX && x < startX + itemW) {
                    UISounds.play(UISounds.UI_CHAPTER);
                    activeGroupIdx = i;
                    listScrollOffset = 0; descScrollOffset = 0; tasksScrollOffset = 0;
                    searchQuery = "";
                    selectedNode = null;
                    rebuildFlat(groups.get(i));
                    return true;
                }
                startX += itemW + 2;
            }
        }

        GroupEntry entry = groups.isEmpty() ? null : groups.get(activeGroupIdx);
        if (entry != null) {
            int treeTop = listY + SEARCH_H + 6;
            int sbX     = listX + LIST_W - 5;

            if (x >= sbX && x <= sbX + 3 && y >= treeTop && y <= listY + listH
                    && entry.flat.size() > listVisibleRows) {
                UISounds.play(UISounds.UI_SCROLL);
                listScrollDragging = true; listScrollDragStartY = y; listScrollDragStartOffset = listScrollOffset;
                return true;
            }
            if (x >= listX && x < listX + LIST_W && y >= treeTop && y < listY + listH) {
                int flatIdx = (y - treeTop) / ROW_H + listScrollOffset;
                if (flatIdx >= 0 && flatIdx < entry.flat.size()) {
                    TreeNode node = entry.flat.get(flatIdx);
                    if (node.type == NodeType.CHAPTER) {
                        UISounds.play(UISounds.UI_CHAPTER);
                        node.expanded = !node.expanded; rebuildFlat(entry); }
                    else {
                        if (selectedNode != node) {
                            descScrollOffset = 0;
                            tasksScrollOffset = 0;
                            rewardPopupOpen = false;
                        }
                        UISounds.play(UISounds.UI_CLICK);
                        selectedNode = node;
                    }
                    return true;
                }
            }
        }

        if (selectedNode != null && selectedNode.type == NodeType.QUEST) {
            int dsbX = detailX + detailW - 6;
            if (x >= dsbX && x <= dsbX + 3 && y >= dTasksY && y <= dTasksY + dTasksH
                    && selectedNode.quest.getTasks().size() > MAX_VIS_TASKS) {
                UISounds.play(UISounds.UI_SCROLL);
                tasksScrollDragging = true; tasksScrollDragStartY = y; tasksScrollDragStartOffset = tasksScrollOffset;
                return true;
            }
            if (x >= dsbX && x <= dsbX + 3 && y >= dDescY && y <= dDescY + dDescH) {
                UISounds.play(UISounds.UI_SCROLL);
                descScrollDragging = true; descScrollDragStartY = y; descScrollDragStartOffset = descScrollOffset;
                return true;
            }
            if (x >= detailX && x < detailX + detailW - 6 && y >= dTasksY && y < dTasksY + dTasksH) {
                int TASK_ROW = dTasksH / MAX_VIS_TASKS;
                int ti = (y - dTasksY) / TASK_ROW;
                int taskIdx = ti + tasksScrollOffset;
                List<Task> taskList = selectedNode.quest.getTasksAsList();
                if (taskIdx >= 0 && taskIdx < taskList.size()) {
                    Task task = taskList.get(taskIdx);
                    if (task instanceof dev.ftb.mods.ftbquests.quest.task.CheckmarkTask
                            || (task instanceof ItemTask it && it.consumesResources() && !it.isTaskScreenOnly())) {
                        NetworkManager.sendToServer(new SubmitTaskMessage(task.id));
                        return true;
                    }
                }
            }

            List<Reward> rewards = new ArrayList<>(selectedNode.quest.getRewards());
            if (rewards.size() > MAX_INLINE_REWARDS) {
                int btnX = detailX + 8 + (ICON_SIZE + 5) * MAX_INLINE_REWARDS;
                int btnY = dRewardY + 4 + font.lineHeight + 3;
                if (x >= btnX && x < btnX + ICON_SIZE + 4
                        && y >= btnY && y < btnY + ICON_SIZE) {
                    rewardPopupOpen = !rewardPopupOpen;
                    return true;
                }
            }

            if (hoveredReward != null) {
                UISounds.play(UISounds.UI_CLICK);
                if (hoveredReward instanceof ChoiceReward choiceReward) {
                    new SelectChoiceRewardScreen(choiceReward).openGui();
                } else {
                    NetworkManager.sendToServer(new ClaimRewardMessage(hoveredReward.id, true));
                }
                return true;
            }

            if (x >= trackBtnX && x < trackBtnX + TRACK_BTN_W && y >= trackBtnY && y < trackBtnY + TRACK_BTN_H) {
                UISounds.play(UISounds.UI_NAVIGATE);
                long tid = UIQuestConfig.TRACKED_QUEST_ID.get();
                UIQuestConfig.saveTrackedQuestId(tid == selectedNode.quest.id ? -1L : selectedNode.quest.id);
                return true;
            }
            if (x >= openBtnX && x < openBtnX + OPEN_BTN_W && y >= openBtnY && y < openBtnY + OPEN_BTN_H) {
                UISounds.play(UISounds.UI_CLICK);
                suppressOpenSound = true;
                ClientQuestFile.openGui(selectedNode.quest, false);
                return true;
            }
        }

        if (x >= settingsBtnX && x < settingsBtnX + SETTINGS_BTN_SIZE
                && y >= settingsBtnY && y < settingsBtnY + SETTINGS_BTN_SIZE) {
            minecraft.setScreen(new SettingsScreen(this));
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        int y = (int)my;
        GroupEntry entry = groups.isEmpty() ? null : groups.get(activeGroupIdx);

        if (listScrollDragging && entry != null) {
            int sbH = listH - 4;
            int maxS = Math.max(1, entry.flat.size() - listVisibleRows);
            listScrollOffset = Math.max(0, Math.min(maxS,
                    listScrollDragStartOffset + (y - listScrollDragStartY) * maxS / sbH));
            return true;
        }
        if (tasksScrollDragging && selectedNode != null) {
            int maxS = Math.max(1, selectedNode.quest.getTasks().size() - MAX_VIS_TASKS);
            tasksScrollOffset = Math.max(0, Math.min(maxS,
                    tasksScrollDragStartOffset + (y - tasksScrollDragStartY) * maxS / dTasksH));
            return true;
        }
        if (descScrollDragging && selectedNode != null) {
            int totalPixels = calcDescTotalPixels(selectedNode.quest);
            int maxS = Math.max(0, totalPixels - dDescH + font.lineHeight);
            descScrollOffset = Math.max(0, Math.min(maxS,
                    descScrollDragStartOffset + (y - descScrollDragStartY) * maxS / dDescH));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    private int calcDescTotalPixels(Quest quest) {
        int total = 0;
        for (Component line : quest.getDescription()) {
            Object img = ImageComponentAccess.findIn(line);
            if (img != null) {
                int h = ImageComponentAccess.getHeight(img);
                total += (h > 0 ? h : 64) + 4;
            } else {
                total += font.split(line, detailW - 20).size() * font.lineHeight;
            }
        }
        return total;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        listScrollDragging = descScrollDragging = tasksScrollDragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int x = (int)mx, y = (int)my;
        GroupEntry entry = groups.isEmpty() ? null : groups.get(activeGroupIdx);

        int treeTop = listY + SEARCH_H + 6;
        if (entry != null && x >= listX && x < listX + LIST_W && y >= treeTop && y < listY + listH) {
            playScrollSound();
            listScrollOffset = (int)Math.max(0, Math.min(
                    Math.max(0, entry.flat.size() - listVisibleRows), listScrollOffset - sy));
            return true;
        }
        if (selectedNode != null && selectedNode.type == NodeType.QUEST) {
            if (x >= detailX && x < detailX + detailW && y >= dTasksY && y < dTasksY + dTasksH) {
                playScrollSound();
                tasksScrollOffset = (int)Math.max(0, Math.min(
                        Math.max(0, selectedNode.quest.getTasks().size() - MAX_VIS_TASKS),
                        tasksScrollOffset - sy));
                return true;
            }
            if (x >= detailX && x < detailX + detailW && y >= dDescY && y < dDescY + dDescH) {
                playScrollSound();
                int totalPixels = calcDescTotalPixels(selectedNode.quest);
                descScrollOffset = (int)Math.max(0, Math.min(
                        Math.max(0, totalPixels - dDescH), descScrollOffset - sy * font.lineHeight));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private int statusColor(QuestStatus s) {
        return switch (s) {
            case CAN_START   -> COL_CAN_START;
            case IN_PROGRESS -> COL_IN_PROG;
            case CAN_CLAIM   -> COL_CAN_CLAIM;
            case COMPLETED   -> COL_COMPLETED;
            case LOCKED      -> COL_LOCKED;
        };
    }

    private String statusLabel(QuestStatus s) {
        return switch (s) {
            case CAN_START   -> UIQuestLang.get("uiquest.screen.status.can_start");
            case IN_PROGRESS -> UIQuestLang.get("uiquest.screen.status.in_progress");
            case CAN_CLAIM   -> UIQuestLang.get("uiquest.screen.status.can_claim");
            case COMPLETED   -> UIQuestLang.get("uiquest.screen.status.completed");
            case LOCKED      -> UIQuestLang.get("uiquest.screen.status.locked");
        };
    }

    private String getQuestType(Quest quest) {
        Chapter ch = quest.getChapter();
        if (ch == null) return "";
        ChapterGroup g = ch.getGroup();
        String gName = (g != null) ? g.getTitle().getString() : "";
        String cName = ch.getTitle().getString();
        return gName.isBlank() ? cName : gName + " · " + cName;
    }

    private String trimLabel(String text, int maxWidth) {
        if (text == null) return "";
        if (font.width(text) <= maxWidth) return text;
        while (!text.isEmpty() && font.width(text + "…") > maxWidth)
            text = text.substring(0, text.length() - 1);
        return text + "…";
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(gfx, mouseX, mouseY, partialTick);
    }

    public void refreshQuests() {
        Set<Long> expandedChapterIds = new HashSet<>();
        for (GroupEntry g : groups)
            for (TreeNode n : g.nodes)
                if (n.type == NodeType.CHAPTER && n.expanded)
                    expandedChapterIds.add(n.chapter.id);

        TreeNode prevSelected = selectedNode;
        buildGroups();

        for (GroupEntry g : groups)
            for (TreeNode n : g.nodes)
                if (n.type == NodeType.CHAPTER && expandedChapterIds.contains(n.chapter.id))
                    n.expanded = true;

        if (!groups.isEmpty())
            rebuildFlat(groups.get(activeGroupIdx));

        if (prevSelected != null && prevSelected.type == NodeType.QUEST) {
            long prevId = prevSelected.quest.id;
            for (GroupEntry entry : groups) {
                for (TreeNode n : entry.nodes) {
                    if (n.type == NodeType.QUEST && n.quest.id == prevId) {
                        selectedNode = n;
                        return;
                    }
                }
            }
            selectedNode = null;
        }
    }

    @Override
    public void onClose() {
        savedGroupIdx = activeGroupIdx;
        savedQuestId = (selectedNode != null && selectedNode.type == NodeType.QUEST)
                ? selectedNode.quest.id : -1L;
        savedListScroll = listScrollOffset;
        savedDescScroll = descScrollOffset;
        savedTasksScroll = tasksScrollOffset;
        savedExpandedChapters.clear();
        for (GroupEntry g : groups)
            for (TreeNode n : g.nodes)
                if (n.type == NodeType.CHAPTER && n.expanded)
                    savedExpandedChapters.add(n.chapter.id);

        UISounds.play(UISounds.UI_BASE);
        super.onClose();
    }

    private void playScrollSound() {
        long now = System.currentTimeMillis();
        if (now - lastScrollSoundTime >= SCROLL_SOUND_COOLDOWN) {
            UISounds.play(UISounds.UI_SCROLL);
            lastScrollSoundTime = now;
        }
    }

    private String searchQuery = "";
    private boolean searchFocused = false;
    private static final int SEARCH_H = 18;
    private static final int SEARCH_PAD_X = 6;

    private int tabScrollOffset = 0;
    private static final int TAB_ARROW_W = 16;

    private int subtitleH = 0;

    private void renderSearchBar(GuiGraphics gfx, int mouseX, int mouseY) {
        int sx = listX;
        int sy = listY + 3;
        int sw = LIST_W - 8;
        int sh = SEARCH_H;

        boolean hovered = mouseX >= sx && mouseX < sx + sw
                && mouseY >= sy && mouseY < sy + sh;

        int borderCol = searchFocused ? COL_GOLD
                : hovered       ? 0xFF888880
                :                 0xFF505050;

        gfx.fill(sx - 1, sy - 1, sx + sw + 1, sy + sh + 1, borderCol);
        gfx.fill(sx,     sy,     sx + sw,     sy + sh,     0xFF252525);

        String display = searchQuery.isEmpty() && !searchFocused
                ? UIQuestLang.get("uiquest.screen.search.hint")
                : searchQuery + (searchFocused ? "|" : "");
        int textCol = searchQuery.isEmpty() && !searchFocused ? 0xFF605848 : COL_TEXT;

        gfx.enableScissor(sx + 2, sy, sx + sw - 2, sy + sh);
        gfx.drawString(font, display, sx + SEARCH_PAD_X + 10, sy + (sh - font.lineHeight) / 2 + 1, textCol, false);
        gfx.disableScissor();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == 259) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    listScrollOffset = 0;
                    if (!groups.isEmpty()) rebuildFlat(groups.get(activeGroupIdx));
                }
                return true;
            }
            if (keyCode == 256) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = "";
                    listScrollOffset = 0;
                    if (!groups.isEmpty()) rebuildFlat(groups.get(activeGroupIdx));
                } else {
                    searchFocused = false;
                }
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (searchFocused) {
            if (c >= 32) {
                searchQuery += c;
                listScrollOffset = 0;
                if (!groups.isEmpty()) rebuildFlat(groups.get(activeGroupIdx));
            }
            return true;
        }
        return super.charTyped(c, modifiers);
    }
}