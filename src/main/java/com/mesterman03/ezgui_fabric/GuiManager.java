package com.mesterman03.ezgui_fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class GuiManager {
    public static final Map<String, GuiDefinition> GUIS = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = new File(FabricLoader.getInstance().getConfigDir().toFile(), "ezgui");

    public static void init() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
            createExampleGui();
        }
        reload();
    }

    public static void reload() {
        GUIS.clear();
        File[] files = CONFIG_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    GuiDefinition gui = GSON.fromJson(reader, GuiDefinition.class);
                    String id = file.getName().replace(".json", "");
                    GUIS.put(id, gui);
                    EzGUIMod.LOGGER.info("Loaded GUI: " + id);
                } catch (Exception e) {
                    EzGUIMod.LOGGER.error("Failed to load GUI file: " + file.getName(), e);
                }
            }
        }
    }

    public static void save(String id) {
        GuiDefinition gui = GUIS.get(id);
        if (gui == null) return;
        File file = new File(CONFIG_DIR, id + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(gui, writer);
        } catch (Exception e) {
            EzGUIMod.LOGGER.error("Failed to save GUI: " + id, e);
        }
    }

    private static void createExampleGui() {
        File example = new File(CONFIG_DIR, "default_menu.json");
        try (FileWriter writer = new FileWriter(example)) {
            GuiDefinition gui = new GuiDefinition();
            gui.title = "&6Default GUI";
            gui.rows = 3;
            
            GuiButton btn = new GuiButton();
            btn.slot = 13;
            btn.item = new GuiItem("minecraft:diamond", "&bClick me!");
            btn.action = new GuiAction("command", "say Hello %player%!", false);
            
            GuiButton toggleBtn = new GuiButton();
            toggleBtn.slot = 14;
            toggleBtn.item = new GuiItem("minecraft:red_wool", "&cToggle Off");
            toggleBtn.action = new GuiAction("toggle", "say Turned On", true);
            toggleBtn.action.toggled_item = new GuiItem("minecraft:green_wool", "&aToggle On");
            toggleBtn.action.command_off = "say Turned Off";
            
            GuiButton closeBtn = new GuiButton();
            closeBtn.slot = 26;
            closeBtn.item = new GuiItem("minecraft:barrier", "&4Close");
            closeBtn.action = new GuiAction();
            closeBtn.action.type = "close";

            GuiButton pageBtn = new GuiButton();
            pageBtn.slot = 25;
            pageBtn.item = new GuiItem("minecraft:arrow", "&eNext Page");
            pageBtn.action = new GuiAction();
            pageBtn.action.type = "open_page";
            pageBtn.action.page = "second_page";

            gui.buttons = new GuiButton[]{btn, toggleBtn, closeBtn, pageBtn};
            
            GSON.toJson(gui, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
