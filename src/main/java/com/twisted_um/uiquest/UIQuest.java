package com.twisted_um.uiquest;

import com.twisted_um.uiquest.client.UISounds;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(UIQuest.MODID)
public class UIQuest {

    public static final String MODID = "uiquest";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UIQuest(IEventBus modEventBus, ModContainer modContainer) {
        UISounds.register(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modEventBus);
        }
        modContainer.registerConfig(ModConfig.Type.CLIENT, UIQuestConfig.SPEC);
    }
}