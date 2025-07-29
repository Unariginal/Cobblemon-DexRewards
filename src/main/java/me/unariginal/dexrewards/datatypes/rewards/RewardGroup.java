package me.unariginal.dexrewards.datatypes.rewards;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class RewardGroup {
    public String name;
    public ItemStack icon;
    public double required_percent;
    public String displayName;
    public List<Reward> rewards;

    public RewardGroup(String name, ItemStack icon, double required_percent, String displayName, List<Reward> rewards) {
        this.name = name;
        this.icon = icon;
        this.required_percent = required_percent;
        this.displayName = displayName;
        this.rewards = rewards;
    }

    public void distribute_rewards(ServerPlayerEntity player) {
        for (Reward reward : rewards) {
            reward.distribute_reward(player);
        }
    }
}
