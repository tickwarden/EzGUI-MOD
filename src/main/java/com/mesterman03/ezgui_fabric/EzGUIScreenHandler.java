package com.mesterman03.ezgui_fabric;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundCategory;

import java.util.HashMap;
import java.util.Map;

public class EzGUIScreenHandler extends GenericContainerScreenHandler {
    private final GuiDefinition guiDef;
    private final String guiId;
    private final Map<Integer, Boolean> toggleStates = new HashMap<>();

    public EzGUIScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, int rows, GuiDefinition guiDef, String guiId) {
        super(getTypeForRows(rows), syncId, playerInventory, inventory, rows);
        this.guiDef = guiDef;
        this.guiId = guiId;
        refreshItems(inventory);
    }

    private static ScreenHandlerType<?> getTypeForRows(int rows) {
        return switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            case 6 -> ScreenHandlerType.GENERIC_9X6;
            default -> ScreenHandlerType.GENERIC_9X3;
        };
    }

    private void refreshItems(Inventory inventory) {
        if (guiDef.buttons == null) return;
        for (GuiButton btn : guiDef.buttons) {
            // If it's a holder, we only set it initially if it's empty, or we leave it empty.
            // For simplicity, holders start with their defined item.
            if (btn.slot >= 0 && btn.slot < guiDef.rows * 9) {
                boolean isToggled = toggleStates.getOrDefault(btn.slot, false);
                GuiItem itemDef = (isToggled && btn.action != null && btn.action.toggled_item != null) 
                    ? btn.action.toggled_item 
                    : btn.item;
                inventory.setStack(btn.slot, createItemStack(itemDef));
            }
        }
    }

    private ItemStack createItemStack(GuiItem itemDef) {
        if (itemDef == null || itemDef.id == null) return ItemStack.EMPTY;
        Identifier id = Identifier.tryParse(itemDef.id);
        if (id == null) return ItemStack.EMPTY;
        
        ItemStack stack = new ItemStack(Registries.ITEM.get(id));
        if (itemDef.name != null) {
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(itemDef.name.replace("&", "§")));
        }
        return stack;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        boolean isProtected = true;
        
        if (slotIndex >= 0 && slotIndex < guiDef.rows * 9) {
            if (guiDef.buttons != null) {
                for (GuiButton btn : guiDef.buttons) {
                    if (btn.slot == slotIndex) {
                        if (btn.is_holder) {
                            isProtected = false;
                        }
                        break;
                    }
                }
            }
            
            if (isProtected) {
                handleButtonClick(slotIndex, (ServerPlayerEntity) player);
                player.currentScreenHandler.sendContentUpdates();
                return;
            }
        }
        
        super.onSlotClick(slotIndex, button, actionType, player);
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    private void handleButtonClick(int slot, ServerPlayerEntity player) {
        if (guiDef.buttons == null) return;
        for (GuiButton btn : guiDef.buttons) {
            if (btn.slot == slot && btn.action != null) {
                executeAction(btn.action, btn.slot, player);
                break;
            }
        }
    }

    private void executeAction(GuiAction action, int slot, ServerPlayerEntity player) {
        if (action.type == null) return;
        
        // 1. Conditions check
        if (action.required_permission > 0 && !player.hasPermissionLevel(action.required_permission)) {
            player.sendMessage(Text.literal(action.deny_message != null ? action.deny_message.replace("&", "§") : "§cYou don't have enough permissions!"), false);
            return;
        }
        if (action.required_xp > 0 && player.experienceLevel < action.required_xp) {
            player.sendMessage(Text.literal(action.deny_message != null ? action.deny_message.replace("&", "§") : "§cNot enough XP!"), false);
            return;
        }
        
        // 2. Play sound
        if (action.sound != null) {
            Identifier soundId = Identifier.tryParse(action.sound);
            if (soundId != null) {
                SoundEvent soundEvent = Registries.SOUND_EVENT.get(soundId);
                if (soundEvent != null) {
                    player.playSound(soundEvent, SoundCategory.MASTER, 1.0f, 1.0f);
                }
            }
        }
        
        // 3. Take XP cost
        if (action.required_xp > 0) {
            player.addExperienceLevels(-action.required_xp);
        }

        switch (action.type) {
            case "close":
                player.closeHandledScreen();
                break;
            case "open_page":
                if (action.page != null) {
                    GuiDefinition nextGui = GuiManager.GUIS.get(action.page);
                    if (nextGui != null) {
                        player.server.execute(() -> openGui(player, action.page, nextGui));
                    }
                }
                break;
            case "command":
                if (action.command != null) {
                    executeCommand(action.command, action.as_server, player);
                }
                break;
            case "toggle":
                boolean currentState = toggleStates.getOrDefault(slot, false);
                toggleStates.put(slot, !currentState);
                refreshItems(this.getInventory());
                this.sendContentUpdates();
                
                if (!currentState && action.command != null) {
                    executeCommand(action.command, action.as_server, player);
                } else if (currentState && action.command_off != null) {
                    executeCommand(action.command_off, action.as_server, player);
                }
                break;
        }
    }

    private void executeCommand(String command, boolean asServer, ServerPlayerEntity player) {
        command = command.replace("%player%", player.getEntityName());
        if (asServer) {
            player.server.getCommandManager().executeWithPrefix(player.server.getCommandSource(), command);
        } else {
            player.server.getCommandManager().executeWithPrefix(player.getCommandSource(), command);
        }
    }

    public static void openGui(ServerPlayerEntity player, String guiId, GuiDefinition guiDef) {
        Text title = Text.literal(guiDef.title != null ? guiDef.title.replace("&", "§") : "GUI");
        SimpleInventory inventory = new SimpleInventory(guiDef.rows * 9);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) -> 
            new EzGUIScreenHandler(syncId, inv, inventory, guiDef.rows, guiDef, guiId), title));
    }
}
