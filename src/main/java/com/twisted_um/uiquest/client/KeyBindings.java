package com.twisted_um.uiquest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final String CATEGORY = "key.categories.uiquest";

    public static final KeyMapping OPEN_QUEST_UI = new KeyMapping(
            "key.uiquest.open",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            CATEGORY
    );

    public static final KeyMapping OPEN_QUEST_ACTIONS = new KeyMapping(
            "key.uiquest.quest_actions",
            GLFW.GLFW_KEY_K,
            "key.categories.uiquest"
    );
}