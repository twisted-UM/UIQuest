package com.twisted_um.uiquest;

import com.twisted_um.uiquest.client.KeyBindings;
import com.twisted_um.uiquest.client.QuestHudRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import com.twisted_um.uiquest.client.screen.QuestBrowserScreen;

public class ClientSetup {
    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ClientSetup::onRegisterKeyMappings);

        NeoForge.EVENT_BUS.addListener(QuestHudRenderer::onRenderGuiOverlay);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onKeyInput);

        ObjectCompletedEvent.QUEST.register(event -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof QuestBrowserScreen screen) {
                mc.tell(screen::refreshQuests);
            }
            return dev.architectury.event.EventResult.pass();
        });

        ObjectCompletedEvent.TASK.register(event -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof QuestBrowserScreen screen) {
                mc.tell(screen::refreshQuests);
            }
            return dev.architectury.event.EventResult.pass();
        });

        UIQuest.LOGGER.info("[UIQuest] Client setup");
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_QUEST_UI);
        event.register(KeyBindings.OPEN_QUEST_ACTIONS);
    }

    private static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        if (KeyBindings.OPEN_QUEST_UI.consumeClick()) {
            if (QuestHudRenderer.isPendingConfirm()) {
                QuestHudRenderer.cancelPendingConfirm();
            } else {
                mc.setScreen(new com.twisted_um.uiquest.client.screen.QuestBrowserScreen());
            }
            return;
        }

        if (KeyBindings.OPEN_QUEST_ACTIONS.consumeClick()) {
            QuestHudRenderer.handleKeyPress();
        }
    }
}