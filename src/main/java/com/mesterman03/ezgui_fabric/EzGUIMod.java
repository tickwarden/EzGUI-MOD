package com.mesterman03.ezgui_fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EzGUIMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ezgui");
    public static final String MOD_ID = "ezgui";

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing EzGUI Fabric Mod!");
        
        GuiManager.init();
        
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            EzGUICommand.register(dispatcher);
        });
    }
}
