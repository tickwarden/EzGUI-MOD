package com.mesterman03.ezgui_fabric;

public class GuiItem {
    public String id;
    public String name;
    public String[] lore;
    public Integer custom_model_data;
    
    public GuiItem() {}
    public GuiItem(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
