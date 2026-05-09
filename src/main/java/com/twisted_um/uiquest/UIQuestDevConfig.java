package com.twisted_um.uiquest;

import net.minecraftforge.common.ForgeConfigSpec;

public class UIQuestDevConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<String> PROGRESS_MODE;
    public static final ForgeConfigSpec.IntValue LIMIT_LENGTH;
    public static final ForgeConfigSpec.ConfigValue<String> INCLUDE_HIDDEN;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        PROGRESS_MODE = builder
                .comment(
                        "Progress display mode (ALL, LIMIT)",
                        "ALL: show overall progress [completed / total quests]",
                        "LIMIT: show linear segment only (max length defined below)",
                        "Any other value will display the full branching chain without limits"
                )
                .define("progress_mode", "");

        LIMIT_LENGTH = builder
                .comment("Only used when progress_mode = LIMIT",
                        "Default = 5"
                )
                .defineInRange("limit_length", 5, 1, 1024);

        INCLUDE_HIDDEN = builder
                .comment(
                        "Include hidden quests in progress calculation (SHOW, HIDE, UNKNOWN)",
                        "SHOW    = Include hidden quests in progress",
                        "HIDE    = Exclude hidden quests from progress",
                        "UNKNOWN = Adds a '+' after the total if there are hidden quests",
                        "Default = HIDE"
                )
                .define("include_hidden", "HIDE");

        SPEC = builder.build();
    }
}