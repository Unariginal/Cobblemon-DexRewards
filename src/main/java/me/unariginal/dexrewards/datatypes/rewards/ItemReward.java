package me.unariginal.dexrewards.datatypes.rewards;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class ItemReward extends Reward {
    public ItemStack rewardItem;

    public ItemReward(String name, String type, String displayName, ItemStack rewardItem) {
        super(name, type, displayName);
        this.rewardItem = rewardItem;
    }

    @Override
    public void distributeReward(ServerPlayerEntity player) {
        player.getInventory().offerOrDrop(rewardItem);
    }
}
