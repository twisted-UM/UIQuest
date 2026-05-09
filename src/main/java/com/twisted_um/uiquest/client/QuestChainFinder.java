package com.twisted_um.uiquest.client;

import com.twisted_um.uiquest.UIQuestDevConfig;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class QuestChainFinder {

    public record ChainInfo(int position, int total, boolean hasHidden) {
        public String totalStr() {
            return hasHidden ? total + "+" : String.valueOf(total);
        }
    }

    public static ChainInfo find(Quest current) {
        if (current == null) return new ChainInfo(1, 1, false);

        ClientQuestFile file = ClientQuestFile.INSTANCE;
        if (file == null) return new ChainInfo(1, 1, false);
        TeamData td = file.selfTeamData;
        if (td == null) return new ChainInfo(1, 1, false);

        return find(current, td);
    }

    public static ChainInfo find(Quest current, TeamData td) {
        if (current == null || td == null) return new ChainInfo(1, 1, false);

        String mode = UIQuestDevConfig.PROGRESS_MODE.get().trim().toUpperCase();
        String includeHidden = UIQuestDevConfig.INCLUDE_HIDDEN.get().trim().toUpperCase();

        return switch (mode) {
            case "ALL"   -> calcAll(current, td, includeHidden);
            case "LIMIT" -> calcLimit(current, td, includeHidden);
            default      -> calcLinear(current, td, includeHidden);
        };
    }

    private static ChainInfo calcLinear(Quest current, TeamData td, String includeHidden) {
        Quest head = current;
        while (true) {
            List<Quest> parents = getQuestParents(head, td, includeHidden);
            if (parents.size() != 1) break;
            Quest parent = parents.get(0);
            if (getQuestChildren(parent, td, includeHidden).size() != 1) break;
            head = parent;
        }

        List<Quest> chain = new ArrayList<>();
        Quest node = head;
        while (true) {
            chain.add(node);
            List<Quest> children = getQuestChildren(node, td, includeHidden);
            if (children.size() != 1) break;
            Quest child = children.get(0);
            if (getQuestParents(child, td, includeHidden).size() != 1) break;
            node = child;
        }

        int index = chain.indexOf(current);
        if (index < 0) index = 0;

        boolean hasHidden = includeHidden.equals("UNKNOWN")
                && hasHiddenQuests(current, td, chain);

        return new ChainInfo(index + 1, chain.size(), hasHidden);
    }

    private static ChainInfo calcAll(Quest current, TeamData td, String includeHidden) {
        if (current.getChapter() == null) return new ChainInfo(1, 1, false);

        List<Quest> allQuests = getChapterQuests(current, td, includeHidden);
        boolean hasHidden = includeHidden.equals("UNKNOWN")
                && hasHiddenQuestsInChapter(current, td);

        long completed = allQuests.stream()
                .filter(q -> td.isCompleted(q))
                .count();

        return new ChainInfo((int) completed, allQuests.size(), hasHidden);
    }

    private static ChainInfo calcLimit(Quest current, TeamData td, String includeHidden) {
        int limit = UIQuestDevConfig.LIMIT_LENGTH.get();

        ChainInfo linear = calcLinear(current, td, includeHidden);

        if (linear.total() <= limit) {
            return linear;
        }

        int pos = linear.position();
        int total = linear.total();

        int windowStart = Math.max(1, pos - limit / 2);
        int windowEnd = windowStart + limit - 1;
        if (windowEnd > total) {
            windowEnd = total;
            windowStart = Math.max(1, windowEnd - limit + 1);
        }

        int newPos = pos - windowStart + 1;
        return new ChainInfo(newPos, limit, linear.hasHidden());
    }

    private static List<Quest> getChapterQuests(Quest current, TeamData td, String includeHidden) {
        List<Quest> result = new ArrayList<>();
        for (Quest q : current.getChapter().getQuests()) {
            if (includeHidden.equals("SHOW") || includeHidden.equals("UNKNOWN") || q.isVisible(td)) {
                result.add(q);
            }
        }
        return result;
    }

    private static boolean hasHiddenQuestsInChapter(Quest current, TeamData td) {
        if (current.getChapter() == null) return false;
        return current.getChapter().getQuests().stream()
                .anyMatch(q -> !q.isVisible(td));
    }

    private static boolean hasHiddenQuests(Quest current, TeamData td, List<Quest> chain) {
        Quest last = chain.get(chain.size() - 1);
        for (QuestObject obj : last.getDependants()) {
            if (obj instanceof Quest q && !q.isVisible(td)) return true;
        }
        return false;
    }

    private static List<Quest> getQuestChildren(Quest quest, TeamData td, String includeHidden) {
        List<Quest> result = new ArrayList<>();
        if (quest == null) return result;
        for (QuestObject obj : quest.getDependants()) {
            if (obj instanceof Quest q) {
                if (includeHidden.equals("SHOW") || includeHidden.equals("UNKNOWN") || q.isVisible(td)) {
                    result.add(q);
                }
            }
        }
        return result;
    }

    private static List<Quest> getQuestParents(Quest quest, TeamData td, String includeHidden) {
        List<Quest> result = new ArrayList<>();
        if (quest == null) return result;
        quest.streamDependencies().forEach(obj -> {
            if (obj instanceof Quest q) {
                if (includeHidden.equals("SHOW") || includeHidden.equals("UNKNOWN") || q.isVisible(td)) {
                    result.add(q);
                }
            }
        });
        return result;
    }
}