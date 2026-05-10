package com.twisted_um.uiquest;

import net.minecraftforge.common.ForgeConfigSpec;

public class UIQuestConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.LongValue TRACKED_QUEST_ID;

    public static final ForgeConfigSpec.BooleanValue SHOW_COMPLETED_QUESTS;
    public static final ForgeConfigSpec.BooleanValue HUD_SHOW_COMPLETED_TASKS;

    public static final ForgeConfigSpec.IntValue HUD_POS_X;
    public static final ForgeConfigSpec.IntValue HUD_POS_Y;

    public static final ForgeConfigSpec.BooleanValue HUD_AUTO_TRACK;
    public static final ForgeConfigSpec.BooleanValue HUD_AUTO_TRACK_MULTI;

    public static final ForgeConfigSpec.BooleanValue SHOW_COMPLETED_CHAPTERS;
    public static final ForgeConfigSpec.BooleanValue HUD_ENABLED;

    public static final ForgeConfigSpec.BooleanValue SOUND_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SOUND_VOLUME;

    public static final ForgeConfigSpec.BooleanValue SHOW_CHAIN_IN_LIST;
    public static final ForgeConfigSpec.BooleanValue SHOW_CHAIN_IN_HUD;

    public static final ForgeConfigSpec.BooleanValue SHOW_EMPTY_CHAPTERS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("gui");
        SHOW_COMPLETED_QUESTS = builder.define("showCompletedQuests", false);
        SHOW_COMPLETED_CHAPTERS = builder
                .comment("Show chapters where all quests are completed")
                .define("showCompletedChapters", false);
        SHOW_CHAIN_IN_LIST = builder
                .comment("Show quest chain progress in quest list")
                .define("showChainInList", false);
        SHOW_EMPTY_CHAPTERS = builder
                .comment("Show chapters with no visible quests")
                .define("showEmptyChapters", false);
        builder.pop();

        builder.push("hud");
        HUD_ENABLED = builder
                .comment("Show the HUD overlay")
                .define("hudEnabled", true);
        HUD_SHOW_COMPLETED_TASKS = builder.define("hudShowCompletedTasks", false);
        HUD_POS_X = builder.defineInRange("hudPosX", 4, 0, 1920);
        HUD_POS_Y = builder.defineInRange("hudPosY", 104, 0, 1080);
        SHOW_CHAIN_IN_HUD = builder
                .comment("Show quest chain progress in HUD")
                .define("showChainInHud", false);
        builder.pop();

        builder.push("general");
        TRACKED_QUEST_ID = builder.defineInRange("trackedQuestId", -1L, -1L, Long.MAX_VALUE);
        HUD_AUTO_TRACK = builder.define("hudAutoTrack", true);
        HUD_AUTO_TRACK_MULTI = builder.define("hudAutoTrackMulti", true);
        SOUND_ENABLED = builder
                .comment("Enable UI sounds")
                .define("soundEnabled", false);
        SOUND_VOLUME = builder
                .comment("UI sound volume (0.0 ~ 1.0)")
                .defineInRange("soundVolume", 0.5, 0.0, 2.0);
        builder.pop();

        SPEC = builder.build();
    }

    public static void saveTrackedQuestId(long id) {
        TRACKED_QUEST_ID.set(id);
        SPEC.save();
    }

    public static void saveSettings() {
        SPEC.save();
    }
}
