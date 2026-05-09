package com.twisted_um.uiquest.client;

import com.twisted_um.uiquest.UIQuestConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class UISounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "uiquest");

    public static final RegistryObject<SoundEvent> UI_BASE = registerSound("ui_base");
    public static final RegistryObject<SoundEvent> UI_NAVIGATE = registerSound("ui_navigate");
    public static final RegistryObject<SoundEvent> UI_CHAPTER = registerSound("ui_chapter");
    public static final RegistryObject<SoundEvent> UI_CLICK = registerSound("ui_click");
    public static final RegistryObject<SoundEvent> UI_CONFIG_CLICK = registerSound("ui_config_click");
    public static final RegistryObject<SoundEvent> UI_SCROLL = registerSound("ui_scroll");
    public static final RegistryObject<SoundEvent> HUD_REWARD_GET = registerSound("hud_reward_get");
    public static final RegistryObject<SoundEvent> HUD_POPUP_OPEN = registerSound("hud_popup_open");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () ->
                SoundEvent.createVariableRangeEvent(
                        new ResourceLocation("uiquest", name)));
    }

    public static void play(RegistryObject<SoundEvent> sound) {
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