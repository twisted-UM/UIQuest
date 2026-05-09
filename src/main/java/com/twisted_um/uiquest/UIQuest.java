package com.twisted_um.uiquest;

import com.twisted_um.uiquest.client.UISounds;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod(UIQuest.MODID)
public class UIQuest {

    public static final String MODID = "uiquest";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ModConfig devConfig;

    public UIQuest() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        UISounds.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modEventBus);
        }

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, UIQuestConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, UIQuestDevConfig.SPEC, "uiquest-dev.toml");

        modEventBus.addListener((ModConfigEvent.Loading event) -> {
            if (event.getConfig().getFileName().equals("uiquest-dev.toml")) {
                devConfig = event.getConfig();
            }
        });
    }
}