package com.mesterman03.ezgui_fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class EzGUICommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ezgui")
            .requires(source -> source.hasPermissionLevel(2))
            
            .then(CommandManager.literal("reload")
                .executes(context -> {
                    GuiManager.reload();
                    context.getSource().sendFeedback(() -> Text.literal("§aEzGUI files reloaded!"), false);
                    return 1;
                })
            )
            
            .then(CommandManager.literal("open")
                .then(CommandManager.argument("gui_id", StringArgumentType.word())
                    .executes(context -> {
                        String guiId = StringArgumentType.getString(context, "gui_id");
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player != null) {
                            GuiDefinition gui = GuiManager.GUIS.get(guiId);
                            if (gui != null) {
                                EzGUIScreenHandler.openGui(player, guiId, gui);
                            } else {
                                context.getSource().sendError(Text.literal("GUI not found: " + guiId));
                            }
                        }
                        return 1;
                    })
                )
            )
            
            .then(CommandManager.literal("edit")
                .then(CommandManager.argument("gui_id", StringArgumentType.word())
                    .executes(context -> {
                        String guiId = StringArgumentType.getString(context, "gui_id");
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player != null) {
                            GuiDefinition gui = GuiManager.GUIS.get(guiId);
                            if (gui != null) {
                                EditorScreenHandler.openEditor(player, guiId, gui);
                            } else {
                                context.getSource().sendError(Text.literal("GUI not found: " + guiId));
                            }
                        }
                        return 1;
                    })
                )
            )
            
            .then(CommandManager.literal("create")
                .then(CommandManager.argument("gui_id", StringArgumentType.word())
                    .then(CommandManager.argument("rows", IntegerArgumentType.integer(1, 6))
                        .then(CommandManager.argument("title", StringArgumentType.greedyString())
                            .executes(context -> {
                                String id = StringArgumentType.getString(context, "gui_id");
                                int rows = IntegerArgumentType.getInteger(context, "rows");
                                String title = StringArgumentType.getString(context, "title");
                                
                                GuiDefinition gui = new GuiDefinition();
                                gui.title = title;
                                gui.rows = rows;
                                gui.buttons = new GuiButton[0];
                                
                                GuiManager.GUIS.put(id, gui);
                                GuiManager.save(id);
                                
                                context.getSource().sendFeedback(() -> Text.literal("§aGUI created: " + id + ". Use /ezgui edit " + id + " to modify it."), false);
                                return 1;
                            })
                        )
                    )
                )
            )
            
            .then(CommandManager.literal("action")
                .then(CommandManager.argument("gui_id", StringArgumentType.word())
                    .then(CommandManager.argument("slot", IntegerArgumentType.integer(0, 53))
                        .then(CommandManager.literal("close")
                            .executes(context -> {
                                return setAction(context.getSource(), 
                                    StringArgumentType.getString(context, "gui_id"), 
                                    IntegerArgumentType.getInteger(context, "slot"), 
                                    new GuiAction("close", null, false));
                            })
                        )
                        .then(CommandManager.literal("clear")
                            .executes(context -> {
                                return setAction(context.getSource(), 
                                    StringArgumentType.getString(context, "gui_id"), 
                                    IntegerArgumentType.getInteger(context, "slot"), 
                                    null);
                            })
                        )
                        .then(CommandManager.literal("page")
                            .then(CommandManager.argument("page_id", StringArgumentType.word())
                                .executes(context -> {
                                    GuiAction action = new GuiAction();
                                    action.type = "open_page";
                                    action.page = StringArgumentType.getString(context, "page_id");
                                    return setAction(context.getSource(), 
                                        StringArgumentType.getString(context, "gui_id"), 
                                        IntegerArgumentType.getInteger(context, "slot"), 
                                        action);
                                })
                            )
                        )
                        .then(CommandManager.literal("command")
                            .then(CommandManager.argument("as_server", BoolArgumentType.bool())
                                .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        GuiAction action = new GuiAction("command", 
                                            StringArgumentType.getString(context, "command"), 
                                            BoolArgumentType.getBool(context, "as_server"));
                                        return setAction(context.getSource(), 
                                            StringArgumentType.getString(context, "gui_id"), 
                                            IntegerArgumentType.getInteger(context, "slot"), 
                                            action);
                                    })
                                )
                            )
                        )
                        .then(CommandManager.literal("set_sound")
                            .then(CommandManager.argument("sound_id", StringArgumentType.word())
                                .executes(context -> {
                                    return modifyAction(context.getSource(), 
                                        StringArgumentType.getString(context, "gui_id"), 
                                        IntegerArgumentType.getInteger(context, "slot"), 
                                        StringArgumentType.getString(context, "sound_id"), -1, -1);
                                })
                            )
                        )
                        .then(CommandManager.literal("set_xp_cost")
                            .then(CommandManager.argument("xp_cost", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    return modifyAction(context.getSource(), 
                                        StringArgumentType.getString(context, "gui_id"), 
                                        IntegerArgumentType.getInteger(context, "slot"), 
                                        null, IntegerArgumentType.getInteger(context, "xp_cost"), -1);
                                })
                            )
                        )
                        .then(CommandManager.literal("set_permission")
                            .then(CommandManager.argument("perm_level", IntegerArgumentType.integer(0, 4))
                                .executes(context -> {
                                    return modifyAction(context.getSource(), 
                                        StringArgumentType.getString(context, "gui_id"), 
                                        IntegerArgumentType.getInteger(context, "slot"), 
                                        null, -1, IntegerArgumentType.getInteger(context, "perm_level"));
                                })
                            )
                        )
                        .then(CommandManager.literal("set_holder")
                            .then(CommandManager.argument("is_holder", BoolArgumentType.bool())
                                .executes(context -> {
                                    return setHolder(context.getSource(), 
                                        StringArgumentType.getString(context, "gui_id"), 
                                        IntegerArgumentType.getInteger(context, "slot"), 
                                        BoolArgumentType.getBool(context, "is_holder"));
                                })
                            )
                        )
                    )
                )
            )
        );
    }

    private static int setAction(ServerCommandSource source, String guiId, int slot, GuiAction action) {
        GuiDefinition gui = GuiManager.GUIS.get(guiId);
        if (gui == null) {
            source.sendError(Text.literal("GUI not found: " + guiId));
            return 0;
        }
        
        boolean found = false;
        if (gui.buttons != null) {
            for (GuiButton btn : gui.buttons) {
                if (btn.slot == slot) {
                    if (action != null && btn.action != null) {
                        action.sound = btn.action.sound;
                        action.required_xp = btn.action.required_xp;
                        action.required_permission = btn.action.required_permission;
                    }
                    btn.action = action;
                    found = true;
                    break;
                }
            }
        }
        
        if (!found) {
            source.sendError(Text.literal("No item found in this slot! Use '/ezgui edit " + guiId + "' to add an item first."));
            return 0;
        }
        
        GuiManager.save(guiId);
        source.sendFeedback(() -> Text.literal("§aAction assigned to slot §e" + slot + " §aof GUI §f" + guiId), false);
        return 1;
    }

    private static int modifyAction(ServerCommandSource source, String guiId, int slot, String sound, int xp, int perm) {
        GuiDefinition gui = GuiManager.GUIS.get(guiId);
        if (gui == null) {
            source.sendError(Text.literal("GUI not found: " + guiId));
            return 0;
        }
        boolean found = false;
        if (gui.buttons != null) {
            for (GuiButton btn : gui.buttons) {
                if (btn.slot == slot) {
                    if (btn.action == null) btn.action = new GuiAction();
                    if (sound != null) btn.action.sound = sound;
                    if (xp != -1) btn.action.required_xp = xp;
                    if (perm != -1) btn.action.required_permission = perm;
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            source.sendError(Text.literal("No item found in this slot!"));
            return 0;
        }
        GuiManager.save(guiId);
        source.sendFeedback(() -> Text.literal("§aCondition/Sound updated for slot §e" + slot), false);
        return 1;
    }

    private static int setHolder(ServerCommandSource source, String guiId, int slot, boolean isHolder) {
        GuiDefinition gui = GuiManager.GUIS.get(guiId);
        if (gui == null) return 0;
        boolean found = false;
        if (gui.buttons != null) {
            for (GuiButton btn : gui.buttons) {
                if (btn.slot == slot) {
                    btn.is_holder = isHolder;
                    found = true;
                    break;
                }
            }
        }
        if (!found) return 0;
        GuiManager.save(guiId);
        source.sendFeedback(() -> Text.literal("§aItem Holder state set to §e" + isHolder + " §afor slot §e" + slot), false);
        return 1;
    }
}
