package com.twisted_um.uiquest.client;

import com.twisted_um.uiquest.UIQuest;
import com.twisted_um.uiquest.client.screen.QuestBrowserScreen;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = UIQuest.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.screen != null) return;

        if (KeyBindings.OPEN_QUEST_UI.consumeClick()) {
            if (!ClientQuestFile.exists()) {
                mc.player.displayClientMessage(
                        Component.literal("[UIQuest] FTB Quests no data"),
                        true
                );
                return;
            }
            mc.setScreen(new QuestBrowserScreen());
        }
    }
}