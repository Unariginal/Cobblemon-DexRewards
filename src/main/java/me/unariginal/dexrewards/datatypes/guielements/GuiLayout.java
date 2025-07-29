package me.unariginal.dexrewards.datatypes.guielements;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.config.PlayerDataConfig;
import me.unariginal.dexrewards.config.RewardGUIConfig;
import me.unariginal.dexrewards.datatypes.Messages;
import me.unariginal.dexrewards.datatypes.PlayerData;
import me.unariginal.dexrewards.datatypes.rewards.RewardGroup;
import me.unariginal.dexrewards.utils.TextUtils;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class GuiLayout {
    String title;
    int size;

    List<String> layout;
    String background_symbol;
    String player_info_symbol;
    String group_symbol;
    String previous_page_symbol;
    String next_page_symbol;

    ItemStack background_item;
    ItemStack previous_item;
    ItemStack next_item;

    public GuiLayout(String title,
                     int size,
                     List<String> layout,
                     String background_symbol,
                     String player_info_symbol,
                     String group_symbol,
                     String previous_page_symbol,
                     String next_page_symbol,
                     ItemStack background_item,
                     ItemStack previous_item,
                     ItemStack next_item) {
        this.title = title;
        this.size = size;
        this.layout = layout;
        this.background_symbol = background_symbol;
        this.player_info_symbol = player_info_symbol;
        this.group_symbol = group_symbol;
        this.previous_page_symbol = previous_page_symbol;
        this.next_page_symbol = next_page_symbol;
        this.background_item = background_item;
        this.previous_item = previous_item;
        this.next_item = next_item;
    }

    public ScreenHandlerType<?> getType() {
        return switch (size) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            default -> ScreenHandlerType.GENERIC_9X6;
        };
    }

    public void create_gui(ServerPlayerEntity player, int page) {
        SimpleGui gui = new SimpleGui(getType(), player, false);
        gui.setTitle(TextUtils.deserialize(title));

        setSlots(gui, background_symbol, new eu.pb4.sgui.api.elements.GuiElementBuilder(background_item).build());

        ItemStack player_info = Items.PLAYER_HEAD.getDefaultStack();
        GuiElement pi_element = null;
        for (GuiElement element : RewardGUIConfig.gui_elements) {
            if (element.key().equals("player_info")) {
                pi_element = element;
            }
        }
        player_info.applyComponentsFrom(ComponentMap.builder()
                        .add(DataComponentTypes.PROFILE, new ProfileComponent(player.getGameProfile()))
                        .build());
        if (pi_element != null) {
            player_info.applyComponentsFrom(pi_element.getComponentMap(player.getUuid(), null));
        }
        setSlots(gui, player_info_symbol, new GuiElementBuilder(player_info).build());

        List<Integer> groupSlots = getGroupSlots();
        int set_groups = groupSlots.size() * page;
        int count = groupSlots.size() * page;
        if (!groupSlots.isEmpty()) {
            for (int slot : groupSlots) {
                if (count >= set_groups) {
                    if (count < DexRewards.INSTANCE.config().reward_groups.size()) {
                        RewardGroup group = DexRewards.INSTANCE.config().reward_groups.get(count);
                        ItemStack icon = group.icon;

                        PlayerData playerData = PlayerDataConfig.getPlayerData(player.getUuid());
                        String status = "locked";
                        if (playerData != null) {
                            if (playerData.claimable_rewards.contains(group.name)) {
                                status = "claimable";
                            } else if (playerData.claimed_rewards.contains(group.name)) {
                                status = "claimed";
                            }
                        }

                        GuiElement group_element = null;
                        for (GuiElement element : RewardGUIConfig.gui_elements) {
                            if (element.key().equals(status + "_group")) {
                                group_element = element;
                            }
                        }

                        if (group_element != null) {
                            icon.applyComponentsFrom(group_element.getComponentMap(player.getUuid(), group));
                        }

                        String finalStatus = status;
                        gui.setSlot(slot, new GuiElementBuilder(icon)
                                .setCallback((i, clickType, slotActionType) -> {
                                    if (finalStatus.equals("claimable")) {
                                        playerData.claimable_rewards.removeIf(data -> data.equals(group.name));
                                        playerData.claimed_rewards.add(group.name);

                                        group.distribute_rewards(player);

                                        DexRewards.INSTANCE.config().updatePlayerData(playerData);

                                        player.sendMessage(TextUtils.deserialize(Messages.parse(Messages.rewards_claimed, group)));

                                        create_gui(player, page);
                                    }
                                })
                                .build());
                        set_groups++;
                    } else {
                        gui.setSlot(slot, new GuiElementBuilder(background_item).build());
                    }
                }
                count++;
            }

            if (set_groups < DexRewards.INSTANCE.config().reward_groups.size()) {
                setSlots(gui, next_page_symbol, new GuiElementBuilder(next_item).setCallback((i, clickType, slotActionType) -> create_gui(player, page + 1)).build());
            } else {
                setSlots(gui, next_page_symbol, new GuiElementBuilder(background_item).build());
            }

            if (page > 0) {
                setSlots(gui, previous_page_symbol, new GuiElementBuilder(previous_item).setCallback((i, clickType, slotActionType) -> create_gui(player, page - 1)).build());
            } else {
                setSlots(gui, previous_page_symbol, new GuiElementBuilder(background_item).build());
            }
        }

        gui.open();
    }

    public List<Integer> getGroupSlots() {
        List<Integer> slots = new ArrayList<>();

        int slot = 0;
        for (String layout : layout) {
            for (char c : layout.toCharArray()) {
                if (c == group_symbol.charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }

        return slots;
    }

    public void setSlots(SimpleGui gui, String symbol, eu.pb4.sgui.api.elements.GuiElement gui_element) {
        int slot = 0;
        for (String layout : layout) {
            for (char c : layout.toCharArray()) {
                if (c == symbol.charAt(0)) {
                    gui.setSlot(slot, gui_element);
                }
                slot++;
            }
        }
    }
}
