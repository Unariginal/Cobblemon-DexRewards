package me.unariginal.dexrewards.datatypes.rewards;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class ItemReward extends Reward {
    public ItemStack reward_item;

    public ItemReward(String name, String type, ItemStack reward_item) {
        super(name, type);
        this.reward_item = reward_item;
    }

    @Override
    public void distribute_reward(ServerPlayerEntity player) {
        if (!player.giveItemStack(reward_item)) {
            player.dropStack(reward_item);
        }
    }
}
