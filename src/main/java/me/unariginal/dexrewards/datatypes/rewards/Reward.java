package me.unariginal.dexrewards.datatypes.rewards;

import net.minecraft.server.network.ServerPlayerEntity;

public class Reward {
    public String name;
    public String type;

    public Reward(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public void distribute_reward(ServerPlayerEntity player) {
    }
}
