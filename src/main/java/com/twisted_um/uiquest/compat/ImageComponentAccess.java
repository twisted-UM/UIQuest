package com.twisted_um.uiquest.compat;

import com.twisted_um.uiquest.UIQuest;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public class ImageComponentAccess {

    private static Class<?> IMAGE_COMPONENT_CLASS = null;
    private static Field FIELD_IMAGE = null;
    private static Field FIELD_WIDTH = null;
    private static Field FIELD_HEIGHT = null;
    private static Field FIELD_ALIGN = null;
    private static boolean initialized = false;

    private static void init() {
        if (initialized) return;
        initialized = true;
        try {
            IMAGE_COMPONENT_CLASS = Class.forName("dev.ftb.mods.ftblibrary.util.client.ImageComponent");
            FIELD_IMAGE  = IMAGE_COMPONENT_CLASS.getField("image");
            FIELD_WIDTH  = IMAGE_COMPONENT_CLASS.getField("width");
            FIELD_HEIGHT = IMAGE_COMPONENT_CLASS.getField("height");
            FIELD_ALIGN  = IMAGE_COMPONENT_CLASS.getField("align");
            UIQuest.LOGGER.info("ImageComponent class loaded successfully: {}", IMAGE_COMPONENT_CLASS);
        } catch (Exception e) {
            UIQuest.LOGGER.error("Failed to load ImageComponent class: {}", e.getMessage());
            IMAGE_COMPONENT_CLASS = null;
        }
    }

    @Nullable
    public static Object findIn(Component c) {
        init();
        if (IMAGE_COMPONENT_CLASS == null) return null;
        return findInRecursive(c, 0);
    }

    @Nullable
    private static Object findInRecursive(Component c, int depth) {
        if (depth > 5) return null;

        if (IMAGE_COMPONENT_CLASS.isAssignableFrom(c.getContents().getClass())) {
            return c.getContents();
        }

        for (Component sibling : c.getSiblings()) {
            Object result = findInRecursive(sibling, depth + 1);
            if (result != null) return result;
        }
        return null;
    }

    public static Icon getImage(Object img) {
        try { return (Icon) FIELD_IMAGE.get(img); } catch (Exception e) { return Icon.empty(); }
    }

    public static int getWidth(Object img) {
        try { return (int) FIELD_WIDTH.get(img); } catch (Exception e) { return 64; }
    }

    public static int getHeight(Object img) {
        try { return (int) FIELD_HEIGHT.get(img); } catch (Exception e) { return 64; }
    }

    public static String getAlignName(Object img) {
        try {
            Object align = FIELD_ALIGN.get(img);
            Field nameField = align.getClass().getField("name");
            return nameField.get(align).toString().toUpperCase();
        } catch (Exception e) { return "CENTER"; }
    }
}