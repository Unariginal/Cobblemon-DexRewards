package me.unariginal.dexrewards.datatypes.rewards;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class RewardGroup {
    public String name;
    public ItemStack icon;
    public double requiredPercent;
    public String displayName;
    public List<Reward> rewards;

    public RewardGroup(String name, ItemStack icon, double requiredPercent, String displayName, List<Reward> rewards) {
        this.name = name;
        this.icon = icon;
        this.requiredPercent = requiredPercent;
        this.displayName = displayName;
        this.rewards = rewards;
    }

    public void distributeRewards(ServerPlayerEntity player) {
        for (Reward reward : rewards) {
            reward.distributeReward(player);
        }
    }
}
