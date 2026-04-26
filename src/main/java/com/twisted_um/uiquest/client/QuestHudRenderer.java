package com.twisted_um.uiquest.client;

import com.twisted_um.uiquest.UIQuestConfig;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.SelectChoiceRewardScreen;
import dev.ftb.mods.ftbquests.net.ClaimRewardMessage;
import dev.ftb.mods.ftbquests.net.SubmitTaskMessage;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.reward.ChoiceReward;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.CheckmarkTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.ftb.mods.ftblibrary.icon.Icon;

import dev.ftb.mods.ftbquests.quest.reward.XPReward;
import dev.ftb.mods.ftbquests.quest.reward.XPLevelsReward;

public class QuestHudRenderer {

    private static final int PAD_X      = 4;
    private static final int PAD_Y      = 4;
    private static final int HUD_W      = 120;
    private static final int ICON_SIZE  = 16;
    private static final int COL_GOLD   = 0xFFC8AA64;
    private static final int COL_TEXT   = 0xFFFEFCF5;
    private static final int COL_FAINT  = 0xFFA08060;
    private static final int COL_DONE   = 0xFFA08060;

    private static boolean pendingConfirm = false;
    private static Task pendingTask = null;
    private static List<Reward> pendingRewards = new ArrayList<>();

    public static void handleKeyPress() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        long trackedId = UIQuestConfig.TRACKED_QUEST_ID.get();
        if (trackedId < 0) { pendingConfirm = false; return; }

        ClientQuestFile file = ClientQuestFile.INSTANCE;
        if (file == null || file.selfTeamData == null) return;
        TeamData td = file.selfTeamData;
        UUID uuid = mc.player.getUUID();

        var base = file.getBase(trackedId);
        if (!(base instanceof Quest quest)) return;

        if (!pendingConfirm) {
            boolean allTasksDone = quest.getTasksAsList().stream().allMatch(td::isCompleted);

            if (!allTasksDone) {
                List<Task> submittableTasks = findAllSubmittableTasks(mc, td, quest);
                if (!submittableTasks.isEmpty()) {
                    pendingConfirm = true;
                    UISounds.play(UISounds.HUD_POPUP_OPEN);
                    pendingTask = submittableTasks.get(0);
                    pendingRewards = new ArrayList<>();
                }
            } else {
                pendingRewards = findAllClaimableRewards(td, quest, uuid);
                if (!pendingRewards.isEmpty()) {
                    pendingConfirm = true;
                    UISounds.play(UISounds.HUD_POPUP_OPEN);
                    pendingTask = null;
                }
            }
        } else {
            if (pendingTask != null) {
                List<Task> submittableTasks = findAllSubmittableTasks(mc, td, quest);
                for (Task task : submittableTasks) {
                    NetworkManager.sendToServer(new SubmitTaskMessage(task.id));
                }
            } else if (!pendingRewards.isEmpty()) {
                boolean openedChoice = false;
                for (Reward reward : pendingRewards) {
                    if (reward instanceof ChoiceReward cr && !openedChoice) {
                        new SelectChoiceRewardScreen(cr).openGui();
                        openedChoice = true;
                    } else if (!(reward instanceof ChoiceReward)) {
                        NetworkManager.sendToServer(new ClaimRewardMessage(reward.id, true));
                    }
                }
            }
            pendingConfirm = false;
            UISounds.play(UISounds.HUD_REWARD_GET);
            pendingTask = null;
            pendingRewards = new ArrayList<>();
        }
    }

    private static List<Task> findCheckmarkTasks(TeamData td, Quest quest) {
        return quest.getTasksAsList().stream()
                .filter(t -> t instanceof CheckmarkTask && !td.isCompleted(t))
                .toList();
    }

    private static List<Task> findSubmittableItemTasks(Minecraft mc, TeamData td, Quest quest) {
        if (mc.player == null) return List.of();
        List<Task> result = new ArrayList<>();
        for (Task task : quest.getTasksAsList()) {
            if (td.isCompleted(task)) continue;
            if (task instanceof ItemTask it && it.consumesResources() && !it.isTaskScreenOnly()) {
                for (ItemStack stack : mc.player.getInventory().items) {
                    if (it.test(stack) && !stack.isEmpty()) {
                        result.add(task);
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static List<Task> findAllSubmittableTasks(Minecraft mc, TeamData td, Quest quest) {
        List<Task> checkmarks = findCheckmarkTasks(td, quest);
        if (!checkmarks.isEmpty()) return checkmarks;
        return findSubmittableItemTasks(mc, td, quest);
    }

    private static List<Reward> findAllClaimableRewards(TeamData td, Quest quest, UUID uuid) {
        if (!td.hasUnclaimedRewards(uuid, quest)) return List.of();
        List<Reward> result = new ArrayList<>();
        for (Reward reward : new ArrayList<>(quest.getRewards())) {
            if (td.isRewardClaimed(uuid, reward)) continue;
            result.add(reward);
        }
        return result;
    }

    private static boolean hasInventorySpace(Minecraft mc, ItemStack stack) {
        if (mc.player == null) return false;
        for (ItemStack slot : mc.player.getInventory().items) {
            if (slot.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(slot, stack)
                    && slot.getCount() + stack.getCount() <= slot.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public static void onRenderGuiOverlay(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }

        if (!UIQuestConfig.HUD_ENABLED.get()) return;

        long trackedId = UIQuestConfig.TRACKED_QUEST_ID.get();
        if (trackedId < 0) return;

        ClientQuestFile file = ClientQuestFile.INSTANCE;
        if (file == null) return;
        TeamData td = file.selfTeamData;
        if (td == null) return;
        if (mc.player == null) return;
        UUID uuid = mc.player.getUUID();

        var base = file.getBase(trackedId);
        if (!(base instanceof Quest quest)) {
            UIQuestConfig.TRACKED_QUEST_ID.set(-1L);
            return;
        }

        QuestStatus status = getStatus(td, quest, uuid);
        if (status == QuestStatus.COMPLETED) {
            if (UIQuestConfig.HUD_AUTO_TRACK.get()) {
                List<Quest> nextQuests = findNextQuests(td, quest, uuid);
                if (nextQuests.isEmpty()) {
                    UIQuestConfig.saveTrackedQuestId(-1L);
                } else if (nextQuests.size() == 1) {
                    UIQuestConfig.saveTrackedQuestId(nextQuests.get(0).id);
                } else {
                    if (UIQuestConfig.HUD_AUTO_TRACK_MULTI.get()) {
                        UIQuestConfig.saveTrackedQuestId(nextQuests.get(0).id);
                    } else {
                        UIQuestConfig.saveTrackedQuestId(-1L);
                    }
                }
            } else {
                UIQuestConfig.saveTrackedQuestId(-1L);
            }
        }

        GuiGraphics gfx = event.getGuiGraphics();
        Font font = mc.font;

        List<Task> tasks = quest.getTasksAsList();
        int lineH = font.lineHeight + 2;

        List<Task> undoneTasks = tasks.stream()
                .filter(t -> !td.isCompleted(t))
                .toList();
        boolean allDone = undoneTasks.isEmpty();

        List<Task> displayTasks;
        if (allDone) {
            displayTasks = List.of();
        } else if (UIQuestConfig.HUD_SHOW_COMPLETED_TASKS.get()) {
            List<Task> combined = new ArrayList<>(undoneTasks);
            tasks.stream()
                    .filter(td::isCompleted)
                    .forEach(combined::add);
            displayTasks = combined;
        } else {
            displayTasks = undoneTasks;
        }
        int taskCount = allDone ? 0 : Math.min(displayTasks.size(), 5);

        int taskAreaH = 0;
        for (int i = 0; i < taskCount; i++) {
            Task t = displayTasks.get(i);
            taskAreaH += lineH;
        }
        boolean questAllDone = tasks.stream().allMatch(td::isCompleted);
        List<Task> submittableTasks = questAllDone ? List.of() : findAllSubmittableTasks(mc, td, quest);
        boolean showHint = questAllDone
                ? td.hasUnclaimedRewards(uuid, quest)
                : !submittableTasks.isEmpty();

        boolean hasBottomText = allDone || displayTasks.size() > 5;
        int hudH = PAD_Y
                + font.lineHeight + 2
                + font.lineHeight + 4
                + taskAreaH
                + (hasBottomText ? font.lineHeight : 0)
                + (showHint ? font.lineHeight + 2 : 0)
                + PAD_Y;

        int hx = UIQuestConfig.HUD_POS_X.get();
        int hy = UIQuestConfig.HUD_POS_Y.get();

        int steps = 60;
        for (int i = 0; i < steps; i++) {
            int sliceX1 = hx + (HUD_W * i / steps);
            int sliceX2 = hx + (HUD_W * (i + 1) / steps);
            int alpha = (int)(0xBB * (1.0f - (float)i / steps));
            int color = (alpha << 24) | 0x101010;
            gfx.fill(sliceX1, hy, sliceX2, hy + hudH, color);
        }
        gfx.fill(hx, hy, hx + 2, hy + hudH, COL_GOLD);

        int drawY = hy + PAD_Y;
        int textX = hx + 6;

        Chapter chapter = quest.getChapter();
        if (chapter != null) {
            gfx.drawString(font, trimText(font, chapter.getTitle().getString(), HUD_W - 12),
                    textX, drawY, COL_FAINT, false);
        }
        drawY += font.lineHeight + 2;

        gfx.drawString(font, trimText(font, quest.getTitle().getString(), HUD_W - 12),
                textX, drawY, COL_TEXT, false);
        drawY += font.lineHeight + 4;

        gfx.fill(textX, drawY - 2, hx + HUD_W - 6, drawY - 1, COL_GOLD);

        for (int i = 0; i < taskCount; i++) {
            Task task = displayTasks.get(i);
            boolean done = td.isCompleted(task);
            long max = Math.max(task.getMaxProgress(), 1);
            long cur = td.getProgress(task);

            int bx = textX, by = drawY + (lineH - 5) / 2;
            gfx.fill(bx, by, bx + 5, by + 5, COL_GOLD);
            if (done) gfx.fill(bx + 1, by + 1, bx + 4, by + 4, COL_DONE);
            else      gfx.fill(bx + 1, by + 1, bx + 4, by + 4, 0xFF1E1E1E);

            String tName = task.getRawTitle();
            if (tName == null || tName.isBlank()) {
                if (task instanceof ItemTask it) tName = it.getItemStack().getHoverName().getString();
                else tName = task.getTitle().getString();
            }

            String progStr = (max > 1) ? " " + cur + "/" + max : "";
            int nameMaxW = HUD_W - 12 - 8 - font.width(progStr);
            gfx.drawString(font, trimText(font, tName, nameMaxW), textX + 8, drawY + 2,
                    done ? COL_DONE : COL_TEXT, false);

            if (max > 1) {
                gfx.drawString(font, progStr,
                        hx + HUD_W - 6 - font.width(progStr),
                        drawY + 2, done ? COL_DONE : COL_FAINT, false);
            }

            drawY += lineH;
        }

        if (allDone) {
            gfx.drawString(font, UIQuestLang.get("uiquest.hud.quest_complete"), textX, drawY, COL_GOLD, false);
            drawY += font.lineHeight + 2;
        } else if (displayTasks.size() > 5) {
            gfx.drawString(font, UIQuestLang.get("uiquest.hud.more_tasks", displayTasks.size() - 5),
                    textX, drawY + 2, COL_FAINT, false);
            drawY += font.lineHeight + 2;
        } else {
            drawY += 2;
        }

        if (pendingConfirm) {
            String jKeyName = KeyBindings.OPEN_QUEST_UI.getTranslatedKeyMessage().getString();
            gfx.drawString(font, UIQuestLang.get("uiquest.hud.hint.cancel", jKeyName), textX, drawY + 2,
                    COL_FAINT, false);
        } else if (questAllDone) {
            if (td.hasUnclaimedRewards(uuid, quest)) {
                String keyName = KeyBindings.OPEN_QUEST_ACTIONS.getTranslatedKeyMessage().getString();
                gfx.drawString(font, UIQuestLang.get("uiquest.hud.hint.reward", keyName), textX, drawY,
                        COL_GOLD, false);
            }
        } else {
            if (!submittableTasks.isEmpty()) {
                List<Task> checkmarks = findCheckmarkTasks(td, quest);
                String keyName = KeyBindings.OPEN_QUEST_ACTIONS.getTranslatedKeyMessage().getString();
                String hintText = !checkmarks.isEmpty()
                        ? UIQuestLang.get("uiquest.hud.hint.complete", keyName)
                        : UIQuestLang.get("uiquest.hud.hint.submit", keyName);
                gfx.drawString(font, hintText, textX, drawY + 2, COL_GOLD, false);
            }
        }

        if (pendingConfirm) {
            renderConfirmPopup(gfx, font, hx, hy + hudH + 2, td, quest, uuid, mc);
        }
    }

    private static void renderConfirmPopup(GuiGraphics gfx, Font font, int hx, int popY,
                                           TeamData td, Quest quest, UUID uuid, Minecraft mc) {
        String keyName = KeyBindings.OPEN_QUEST_ACTIONS.getTranslatedKeyMessage().getString();
        boolean isReward = !pendingRewards.isEmpty();

        int popupH = font.lineHeight + 4 + ICON_SIZE + 6;
        int steps = 60;
        for (int i = 0; i < steps; i++) {
            int sliceX1 = hx + (HUD_W * i / steps);
            int sliceX2 = hx + (HUD_W * (i + 1) / steps);
            int alpha = (int)(0xBB * (1.0f - (float)i / steps));
            int color = (alpha << 24) | 0x101010;
            gfx.fill(sliceX1, popY, sliceX2, popY + popupH, color);
        }
        gfx.fill(hx, popY, hx + 2, popY + popupH, COL_GOLD);

        int textX = hx + 6;
        int drawY = popY + 2;

        if (isReward) {
            gfx.drawString(font, UIQuestLang.get("uiquest.hud.popup.reward", keyName), textX, drawY, COL_GOLD, false);
            drawY += font.lineHeight + 2;

            int rx = textX;
            int maxIcons = (HUD_W - 14) / (ICON_SIZE + 2);
            int displayCount = Math.min(pendingRewards.size(), maxIcons - 1);
            boolean hasMore = pendingRewards.size() > displayCount;

            for (int i = 0; i < displayCount; i++) {
                Reward reward = pendingRewards.get(i);
                if (reward instanceof ItemReward ir) {
                    gfx.renderItem(ir.getItem(), rx, drawY);
                    int cnt = ir.getCount();
                    if (cnt > 1) {
                        gfx.pose().pushPose();
                        gfx.pose().translate(0, 0, 200);
                        String cntStr = String.valueOf(cnt);
                        gfx.drawString(font, cntStr,
                                rx + ICON_SIZE - font.width(cntStr),
                                drawY + ICON_SIZE - font.lineHeight + 1,
                                COL_TEXT, true);
                        gfx.pose().popPose();
                    }
                } else {
                    if (!(reward instanceof XPReward) && !(reward instanceof XPLevelsReward)) {
                        gfx.fill(rx, drawY, rx + ICON_SIZE, drawY + ICON_SIZE, 0xFF2A2A2A);
                    }
                    Icon icon = reward.getIcon();
                    if (!icon.isEmpty()) icon.draw(gfx, rx, drawY, ICON_SIZE, ICON_SIZE);
                    else gfx.drawString(font, "?",
                            rx + (ICON_SIZE - font.width("?")) / 2,
                            drawY + (ICON_SIZE - font.lineHeight) / 2,
                            COL_GOLD, false);

                    if (reward instanceof XPReward xr) {
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
                                drawY + ICON_SIZE - font.lineHeight + 1,
                                0xFFECEAE0, true);
                        gfx.pose().popPose();
                    } else if (reward instanceof XPLevelsReward xlr) {
                        String amtStr = xlr.getButtonText();
                        gfx.pose().pushPose();
                        gfx.pose().translate(0, 0, 200);
                        gfx.drawString(font, amtStr,
                                rx + ICON_SIZE - font.width(amtStr),
                                drawY + ICON_SIZE - font.lineHeight + 1,
                                0xFF7CFC00, true);
                        gfx.pose().popPose();
                    }
                }
                rx += ICON_SIZE + 2;
            }

            if (hasMore) {
                String moreStr = "+" + (pendingRewards.size() - displayCount);
                gfx.fill(rx, drawY, rx + ICON_SIZE, drawY + ICON_SIZE, 0xFF2A2A2A);
                gfx.drawString(font, moreStr,
                        rx + (ICON_SIZE - font.width(moreStr)) / 2,
                        drawY + (ICON_SIZE - font.lineHeight) / 2,
                        COL_GOLD, false);
            }

        } else {
            List<Task> checkmarks = findCheckmarkTasks(td, quest);
            String actionLabel = !checkmarks.isEmpty()
                    ? UIQuestLang.get("uiquest.hud.popup.complete", keyName)
                    : UIQuestLang.get("uiquest.hud.popup.submit", keyName);
            gfx.drawString(font, actionLabel, textX, drawY, COL_GOLD, false);
            drawY += font.lineHeight + 2;

            List<Task> submittable = findAllSubmittableTasks(mc, td, quest);
            int rx = textX;
            for (Task task : submittable) {
                if (task instanceof ItemTask it) {
                    gfx.renderItem(it.getItemStack(), rx, drawY);
                    long needed = Math.max(1, it.getMaxProgress() - td.getProgress(task));
                    if (needed > 1) {
                        gfx.pose().pushPose();
                        gfx.pose().translate(0, 0, 200);
                        String cntStr = String.valueOf(needed);
                        gfx.drawString(font, cntStr,
                                rx + ICON_SIZE - font.width(cntStr),
                                drawY + ICON_SIZE - font.lineHeight + 1,
                                COL_TEXT, true);
                        gfx.pose().popPose();
                    }
                    rx += ICON_SIZE + 2;
                } else if (task instanceof CheckmarkTask) {
                    gfx.fill(rx, drawY, rx + ICON_SIZE, drawY + ICON_SIZE, 0xFF2A2A2A);
                    gfx.fill(rx + 1, drawY + 1, rx + ICON_SIZE - 1, drawY + ICON_SIZE - 1, COL_GOLD);
                    gfx.drawString(font, "✔",
                            rx + (ICON_SIZE - font.width("✔")) / 2,
                            drawY + (ICON_SIZE - font.lineHeight) / 2,
                            0xFF1E1E1E, false);
                    rx += ICON_SIZE + 2;
                }
                if (rx + ICON_SIZE > hx + HUD_W - 4) break;
            }
        }
    }

    private static QuestStatus getStatus(TeamData td, Quest quest, UUID uuid) {
        if (td.hasUnclaimedRewards(uuid, quest)) return QuestStatus.CAN_CLAIM;
        if (td.isCompleted(quest))               return QuestStatus.COMPLETED;
        if (td.isStarted(quest))                 return QuestStatus.IN_PROGRESS;
        if (!quest.isVisible(td))                return QuestStatus.LOCKED;
        if (td.canStartTasks(quest))             return QuestStatus.CAN_START;
        return QuestStatus.LOCKED;
    }

    private enum QuestStatus {
        CAN_START, IN_PROGRESS, CAN_CLAIM, COMPLETED, LOCKED
    }

    private static List<Quest> findNextQuests(TeamData td, Quest current, UUID uuid) {
        List<Quest> result = new ArrayList<>();
        for (QuestObject dependant : current.getDependants()) {
            if (!(dependant instanceof Quest next)) continue;
            if (td.isCompleted(next)) continue;
            QuestStatus st = getStatus(td, next, uuid);
            if (st != QuestStatus.LOCKED) {
                result.add(next);
            } else {
                Quest blockedBy = findBlockingDependency(td, next, uuid);
                if (blockedBy != null) result.add(blockedBy);
            }
        }
        return result;
    }

    private static Quest findBlockingDependency(TeamData td, Quest locked, UUID uuid) {
        return findBlockingDependencyInner(td, locked, uuid, 0);
    }

    private static Quest findBlockingDependencyInner(TeamData td, Quest locked, UUID uuid, int depth) {
        if (depth > 10) return null;
        for (QuestObject dep : locked.streamDependencies().toList()) {
            if (!(dep instanceof Quest depQuest)) continue;
            if (td.isCompleted(depQuest)) continue;
            QuestStatus st = getStatus(td, depQuest, uuid);
            if (st != QuestStatus.LOCKED) return depQuest;
            Quest deeper = findBlockingDependencyInner(td, depQuest, uuid, depth + 1);
            if (deeper != null) return deeper;
        }
        return null;
    }

    private static String trimText(Font font, String text, int maxWidth) {
        if (text == null) return "";
        if (font.width(text) <= maxWidth) return text;
        while (!text.isEmpty() && font.width(text + "…") > maxWidth)
            text = text.substring(0, text.length() - 1);
        return text + "…";
    }
    public static boolean isPendingConfirm() {
        return pendingConfirm;
    }

    public static void cancelPendingConfirm() {
        pendingConfirm = false;
        UISounds.play(UISounds.UI_CHAPTER);
        pendingTask = null;
        pendingRewards = new ArrayList<>();
    }
}