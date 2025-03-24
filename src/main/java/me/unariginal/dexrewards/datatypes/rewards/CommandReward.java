package me.unariginal.dexrewards.datatypes.rewards;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Objects;

public class CommandReward extends Reward {
    List<String> commands;

    public CommandReward(String name, String type, List<String> commands) {
        super(name, type);
        this.commands = commands;
    }

    @Override
    public void distribute_reward(ServerPlayerEntity player) {
        for (String command : commands) {
            command = command.replaceAll("%player%", player.getNameForScoreboard());
            CommandManager cmdManager = Objects.requireNonNull(player.getServer()).getCommandManager();
            ServerCommandSource source = player.getServer().getCommandSource();
            cmdManager.executeWithPrefix(source, command);
        }
    }
}
