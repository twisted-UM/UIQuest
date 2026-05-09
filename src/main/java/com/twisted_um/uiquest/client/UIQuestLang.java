package com.twisted_um.uiquest.client;

import net.minecraft.client.resources.language.I18n;

public class UIQuestLang {
    public static String get(String key) {
        return I18n.get(key);
    }
    public static String get(String key, Object... args) {
        return I18n.get(key, args);
    }
}