package com.spawnerbeacon;

import com.mojang.blaze3d.platform.InputConstants;
import com.spawnerbeacon.gui.ConfigScreen;
import com.spawnerbeacon.render.BeamRenderer;
import com.spawnerbeacon.spawner.SpawnerTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class SpawnerBeaconClient implements ClientModInitializer {
    public static final String MOD_ID = "spawnerbeacon";
    private static KeyMapping openMenuKey;
    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        BeaconConfig.load();
        SpawnerTracker.register();
        BeamRenderer.register();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.spawnerbeacon.open_menu", InputConstants.Type.KEYSYM, InputConstants.KEY_LBRACKET, category));
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.spawnerbeacon.toggle_beams", InputConstants.Type.KEYSYM, -1, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                if (client.screen == null && client.level != null) client.setScreen(new ConfigScreen());
            }
            while (toggleKey.consumeClick()) {
                BeaconConfig cfg = BeaconConfig.get();
                cfg.enabled = !cfg.enabled;
                cfg.save();
                if (client.player != null) client.player.sendOverlayMessage(
                        net.minecraft.network.chat.Component.translatable(cfg.enabled ? "message.spawnerbeacon.on" : "message.spawnerbeacon.off"));
            }
        });
    }
}
