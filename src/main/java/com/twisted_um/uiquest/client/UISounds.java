package com.twisted_um.uiquest.client;

import com.twisted_um.uiquest.UIQuestConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;

public class UISounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, "uiquest");

    public static final DeferredHolder<SoundEvent, SoundEvent> UI_BASE =
            register("ui_base");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_NAVIGATE =
            register("ui_navigate");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_CHAPTER =
            register("ui_chapter");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_CLICK =
            register("ui_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_CONFIG_CLICK =
            register("ui_config_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_SCROLL =
            register("ui_scroll");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUD_REWARD_GET =
            register("hud_reward_get");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUD_POPUP_OPEN =
            register("hud_popup_open");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () ->
                SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath("uiquest", name)));
    }

    public static void play(DeferredHolder<SoundEvent, SoundEvent> sound) {
        if (!UIQuestConfig.SOUND_ENABLED.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            float volume = (float)(double) UIQuestConfig.SOUND_VOLUME.get();
            mc.getSoundManager().play(
                    SimpleSoundInstance.forUI(sound.get(), 1.0f, volume));
        }
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}