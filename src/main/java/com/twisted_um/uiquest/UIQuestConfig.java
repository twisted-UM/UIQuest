package com.twisted_um.uiquest;

import net.neoforged.neoforge.common.ModConfigSpec;

public class UIQuestConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.LongValue TRACKED_QUEST_ID;

    public static final ModConfigSpec.BooleanValue SHOW_COMPLETED_QUESTS;
    public static final ModConfigSpec.BooleanValue HUD_SHOW_COMPLETED_TASKS;

    public static final ModConfigSpec.IntValue HUD_POS_X;
    public static final ModConfigSpec.IntValue HUD_POS_Y;

    public static final ModConfigSpec.BooleanValue HUD_AUTO_TRACK;
    public static final ModConfigSpec.BooleanValue HUD_AUTO_TRACK_MULTI;

    public static final ModConfigSpec.BooleanValue SHOW_COMPLETED_CHAPTERS;
    public static final ModConfigSpec.BooleanValue HUD_ENABLED;

    public static final ModConfigSpec.BooleanValue SOUND_ENABLED;
    public static final ModConfigSpec.DoubleValue SOUND_VOLUME;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("GUI settings").push("gui");
        SHOW_COMPLETED_QUESTS = builder.define("showCompletedQuests", false);
        SHOW_COMPLETED_CHAPTERS = builder
                .comment("Show chapters where all quests are completed")
                .define("showCompletedChapters", false);
        builder.pop();

        builder.comment("HUD settings").push("hud");
        HUD_ENABLED = builder
                .comment("Show the HUD overlay")
                .define("hudEnabled", true);
        HUD_SHOW_COMPLETED_TASKS = builder.define("hudShowCompletedTasks", true);
        HUD_POS_X = builder.defineInRange("hudPosX", 4, 0, 1920);
        HUD_POS_Y = builder.defineInRange("hudPosY", 104, 0, 1080);
        builder.pop();

        builder.comment("General settings").push("general");
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