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

import java.util.ArrayList;
import java.util.List;

public class EditorScreenHandler extends GenericContainerScreenHandler {
    private final GuiDefinition guiDef;
    private final String guiId;

    public EditorScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, int rows, GuiDefinition guiDef, String guiId) {
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
        inventory.clear();
        if (guiDef.buttons == null) return;
        for (GuiButton btn : guiDef.buttons) {
            if (btn.slot >= 0 && btn.slot < guiDef.rows * 9) {
                inventory.setStack(btn.slot, createItemStack(btn.item));
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
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (!player.getWorld().isClient()) {
            List<GuiButton> newButtons = new ArrayList<>();
            for (int i = 0; i < guiDef.rows * 9; i++) {
                ItemStack stack = this.getInventory().getStack(i);
                if (!stack.isEmpty()) {
                    GuiButton btn = new GuiButton();
                    btn.slot = i;
                    btn.item = new GuiItem();
                    btn.item.id = Registries.ITEM.getId(stack.getItem()).toString();
                    if (stack.contains(DataComponentTypes.CUSTOM_NAME)) {
                        btn.item.name = stack.get(DataComponentTypes.CUSTOM_NAME).getString().replace("§", "&");
                    }
                    
                    if (guiDef.buttons != null) {
                        for (GuiButton old : guiDef.buttons) {
                            if (old.slot == i) {
                                btn.action = old.action;
                                btn.is_holder = old.is_holder;
                                break;
                            }
                        }
                    }
                    newButtons.add(btn);
                }
            }
            guiDef.buttons = newButtons.toArray(new GuiButton[0]);
            GuiManager.save(guiId);
            
            player.sendMessage(Text.literal("§a[EzGUI] Menu saved! You can assign actions using the commands below:"), false);
            for (GuiButton btn : newButtons) {
                String cmd = "/ezgui action " + guiId + " " + btn.slot;
                player.sendMessage(Text.literal("§eSlot " + btn.slot + " -> §7" + btn.item.id)
                    .append(Text.literal(" §b[Set Command]").styled(style -> 
                        style.withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.SUGGEST_COMMAND, cmd + " command true say %player%"))
                             .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.literal("Click to assign a Command")))
                    ))
                    .append(Text.literal(" §c[Set Close]").styled(style -> 
                        style.withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.SUGGEST_COMMAND, cmd + " close"))
                             .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.literal("Click to make it a Close button")))
                    ))
                    .append(Text.literal(" §a[Set Page]").styled(style -> 
                        style.withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.SUGGEST_COMMAND, cmd + " page other_page"))
                             .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.literal("Click to assign a Page redirection")))
                    ))
                , false);
            }
            player.sendMessage(Text.literal("§a[EzGUI] Done! Edit completed."), false);
        }
    }

    public static void openEditor(ServerPlayerEntity player, String guiId, GuiDefinition guiDef) {
        Text title = Text.literal("§c[EDITOR] §r" + (guiDef.title != null ? guiDef.title.replace("&", "§") : "GUI"));
        SimpleInventory inventory = new SimpleInventory(guiDef.rows * 9);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) -> 
            new EditorScreenHandler(syncId, inv, inventory, guiDef.rows, guiDef, guiId), title));
    }
}
