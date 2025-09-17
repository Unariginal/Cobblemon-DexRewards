package me.unariginal.dexrewards.datatypes.guielements;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.config.MessagesConfig;
import me.unariginal.dexrewards.config.PlayerDataConfig;
import me.unariginal.dexrewards.config.RewardGUIConfig;
import me.unariginal.dexrewards.datatypes.DexType;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiLayout {
    public String title;
    public int size;

    public List<String> layout;
    public String backgroundSymbol;
    public String playerInfoSymbol;
    public String groupSymbol;
    public String previousPageSymbol;
    public String nextPageSymbol;

    public ItemStack backgroundItem;
    public ItemStack previousItem;
    public ItemStack nextItem;

    public GuiLayout(String title,
                     int size,
                     List<String> layout,
                     String backgroundSymbol,
                     String playerInfoSymbol,
                     String groupSymbol,
                     String previousPageSymbol,
                     String nextPageSymbol,
                     ItemStack backgroundItem,
                     ItemStack previousItem,
                     ItemStack nextItem) {
        this.title = title;
        this.size = size;
        this.layout = layout;
        this.backgroundSymbol = backgroundSymbol;
        this.playerInfoSymbol = playerInfoSymbol;
        this.groupSymbol = groupSymbol;
        this.previousPageSymbol = previousPageSymbol;
        this.nextPageSymbol = nextPageSymbol;
        this.backgroundItem = backgroundItem;
        this.previousItem = previousItem;
        this.nextItem = nextItem;
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

    public void createGui(ServerPlayerEntity player, int page, DexType dexType) {
        PlayerData playerData = PlayerDataConfig.getPlayerData(player.getUuid());
        if (playerData == null) return;
        PlayerData.ProgressTracker progressTracker = playerData.getProgress(dexType.name);
        if (progressTracker == null) return;

        SimpleGui gui = new SimpleGui(getType(), player, false);
        gui.setTitle(TextUtils.deserialize(TextUtils.parse(title, dexType, player)));

        setSlots(gui, backgroundSymbol, new GuiElementBuilder(backgroundItem).build());

        ItemStack playerInfoItem = Items.PLAYER_HEAD.getDefaultStack();
        GuiElement playerInfoElement = null;
        for (GuiElement element : RewardGUIConfig.guiElements) {
            if (element.key().equals("player_info")) {
                playerInfoElement = element;
            }
        }
        playerInfoItem.applyComponentsFrom(ComponentMap.builder()
                        .add(DataComponentTypes.PROFILE, new ProfileComponent(player.getGameProfile()))
                        .build());
        if (playerInfoElement != null) {
            playerInfoItem.applyComponentsFrom(playerInfoElement.getComponentMap(player, null, dexType));
        }
        setSlots(gui, playerInfoSymbol, new GuiElementBuilder(playerInfoItem).build());

        List<Integer> groupSlots = getGroupSlots();
        int setGroups = groupSlots.size() * page;
        int count = groupSlots.size() * page;
        if (!groupSlots.isEmpty()) {
            for (int slot : groupSlots) {
                if (count >= setGroups) {
                    if (count < dexType.rewardGroups.size()) {
                        RewardGroup group = dexType.rewardGroups.get(count);
                        ItemStack icon = group.icon;

                        String status = "locked";
                        if (progressTracker.claimableRewards.contains(group.name))
                            status = "claimable";
                        else if (progressTracker.claimedRewards.contains(group.name))
                            status = "claimed";

                        GuiElement groupElement = null;
                        for (GuiElement element : RewardGUIConfig.guiElements) {
                            if (element.key().equals(status + "_group"))
                                groupElement = element;
                        }

                        if (groupElement != null)
                            icon.applyComponentsFrom(groupElement.getComponentMap(player, group, dexType));

                        String finalStatus = status;
                        gui.setSlot(slot, new GuiElementBuilder(icon)
                                .setCallback((i, clickType, slotActionType) -> {
                                    if (finalStatus.equals("claimable")) {
                                        try {
                                            progressTracker.claimableRewards.removeIf(reward -> reward.equalsIgnoreCase(group.name));
                                            progressTracker.claimedRewards.add(group.name);

                                            group.distributeRewards(player);

                                            PlayerDataConfig.updatePlayerData(playerData);

                                            player.sendMessage(TextUtils.deserialize(TextUtils.parse(MessagesConfig.getMessage("rewards_claimed"), group)));

                                            createGui(player, page, dexType);
                                        } catch (IOException e) {
                                            DexRewards.LOGGER.error("[DexRewards] Failed to update player data for player \"{}\"", player.getNameForScoreboard(), e);
                                        }
                                    }
                                })
                                .build());
                        setGroups++;
                    } else {
                        gui.setSlot(slot, new GuiElementBuilder(backgroundItem).build());
                    }
                }
                count++;
            }

            if (setGroups < dexType.rewardGroups.size()) {
                setSlots(gui, nextPageSymbol, new GuiElementBuilder(nextItem).setCallback((i, clickType, slotActionType) -> createGui(player, page + 1, dexType)).build());
            } else {
                setSlots(gui, nextPageSymbol, new GuiElementBuilder(backgroundItem).build());
            }

            if (page > 0) {
                setSlots(gui, previousPageSymbol, new GuiElementBuilder(previousItem).setCallback((i, clickType, slotActionType) -> createGui(player, page - 1, dexType)).build());
            } else {
                setSlots(gui, previousPageSymbol, new GuiElementBuilder(backgroundItem).build());
            }
        }

        gui.open();
    }

    public List<Integer> getGroupSlots() {
        List<Integer> slots = new ArrayList<>();

        int slot = 0;
        for (String layout : layout) {
            for (char c : layout.toCharArray()) {
                if (c == groupSymbol.charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }

        return slots;
    }

    public void setSlots(SimpleGui gui, String symbol, eu.pb4.sgui.api.elements.GuiElement guiElement) {
        int slot = 0;
        for (String layout : layout) {
            for (char c : layout.toCharArray()) {
                if (c == symbol.charAt(0)) {
                    gui.setSlot(slot, guiElement);
                }
                slot++;
            }
        }
    }
}
