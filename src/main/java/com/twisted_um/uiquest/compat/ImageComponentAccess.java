package com.twisted_um.uiquest.compat;

import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public class ImageComponentAccess {

    private static Class<?> IMAGE_COMPONENT_CLASS = null;
    private static Method GET_IMAGE = null;
    private static Method GET_WIDTH = null;
    private static Method GET_HEIGHT = null;
    private static Method GET_ALIGN = null;
    private static boolean initialized = false;

    private static void init() {
        if (initialized) return;
        initialized = true;
        try {
            IMAGE_COMPONENT_CLASS = Class.forName("dev.ftb.mods.ftblibrary.util.client.ImageComponent");
            GET_IMAGE  = IMAGE_COMPONENT_CLASS.getMethod("getImage");
            GET_WIDTH  = IMAGE_COMPONENT_CLASS.getMethod("getWidth");
            GET_HEIGHT = IMAGE_COMPONENT_CLASS.getMethod("getHeight");
            GET_ALIGN  = IMAGE_COMPONENT_CLASS.getMethod("getAlign");
        } catch (Exception e) {
            IMAGE_COMPONENT_CLASS = null;
        }
    }

    @Nullable
    public static Object findIn(Component c) {
        init();
        if (IMAGE_COMPONENT_CLASS == null) return null;
        if (IMAGE_COMPONENT_CLASS.isInstance(c.getContents())) return c.getContents();
        for (Component sibling : c.getSiblings()) {
            if (IMAGE_COMPONENT_CLASS.isInstance(sibling.getContents())) return sibling.getContents();
        }
        return null;
    }

    public static Icon getImage(Object img) {
        try { return (Icon) GET_IMAGE.invoke(img); } catch (Exception e) { return Icon.empty(); }
    }

    public static int getWidth(Object img) {
        try { return (int) GET_WIDTH.invoke(img); } catch (Exception e) { return 64; }
    }

    public static int getHeight(Object img) {
        try { return (int) GET_HEIGHT.invoke(img); } catch (Exception e) { return 64; }
    }

    public static String getAlignName(Object img) {
        try {
            Object align = GET_ALIGN.invoke(img);
            return align.toString().toUpperCase();
        } catch (Exception e) { return "CENTER"; }
    }
}