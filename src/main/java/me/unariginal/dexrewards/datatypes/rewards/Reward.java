package me.unariginal.dexrewards.datatypes.rewards;

import net.minecraft.server.network.ServerPlayerEntity;

public class Reward {
    public String name;
    public String type;
    public String displayName;

    public Reward(String name, String type, String displayName) {
        this.name = name;
        this.type = type;
        this.displayName = displayName;
    }

    public void distribute_reward(ServerPlayerEntity player) {
    }
}
