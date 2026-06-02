package com.mesterman03.ezgui_fabric;

public class GuiAction {
    public String type; // "command", "toggle", "close", "open_page"
    public String command; 
    public String command_off;
    public boolean as_server;
    public GuiItem toggled_item;
    public String page;
    
    // NEW FEATURES
    public String sound; 
    public int required_xp;
    public int required_permission;
    public String deny_message;
    
    public GuiAction() {}
    public GuiAction(String type, String command, boolean as_server) {
        this.type = type;
        this.command = command;
        this.as_server = as_server;
    }
}
