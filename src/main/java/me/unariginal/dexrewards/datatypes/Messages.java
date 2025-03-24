package me.unariginal.dexrewards.datatypes;

import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.datatypes.rewards.RewardGroup;
import net.minecraft.server.network.ServerPlayerEntity;

import java.text.DecimalFormat;
import java.util.UUID;

public class Messages {
    public static String prefix = "<dark_gray>[<blue>DexRewards<dark_gray>]";
    public static String rewards_to_claim = "%prefix% You have rewards to claim!";
    public static String reward_claimable = "%prefix% You can claim the %group.name% rewards!";
    public static String rewards_claimed = "%prefix% You've claimed rewards for %group.name%!";
    public static String reset_sender = "%prefix% You've reset %player%'s pokedex reward progress!";
    public static String reset_target = "%prefix% Your pokedex reward progress has been reset!";
    public static String update_command = "%prefix% You've updated the pokedex reward progress of %player%";
    public static String reload = "%prefix% Reloaded!";

    public static String parse(String message) {
        message = message.replaceAll("%prefix%", prefix);
        message = message.replaceAll("%pokedex.total%", String.valueOf(DexRewards.DEX_TOTAL));
        return message;
    }

    public static String parse(String message, RewardGroup group) {
        message = parse(message);
        message = message.replaceAll("%group.name%", group.name);
        message = message.replaceAll("%group.percent%", String.valueOf(group.required_percent));
        return message;
    }

    public static String parse(String message, UUID player_uuid) {
        message = parse(message);

        ServerPlayerEntity player = DexRewards.INSTANCE.server().getPlayerManager().getPlayer(player_uuid);
        if (player != null) {
            message = message.replaceAll("%player%", player.getNameForScoreboard());
        }

        PlayerData player_data = null;
        for (PlayerData data : DexRewards.INSTANCE.config().player_data) {
            if (data.uuid.equals(player_uuid)) {
                player_data = data;
                break;
            }
        }

        if (player_data != null) {
            String rank = "None";
            for (RewardGroup group : DexRewards.INSTANCE.config().reward_groups) {
                if (player_data.claimed_rewards.contains(group.name)) {
                    rank = group.name;
                }
            }
            message = message.replaceAll("%player.rank%", rank);
            message = message.replaceAll("%player.caught_count%", String.valueOf(player_data.caught_count));
            String percent = new DecimalFormat("#.##").format(((double) player_data.caught_count / DexRewards.DEX_TOTAL) * 100);
            message = message.replaceAll("%player.caught_percent%", percent);
        }

        return message;
    }
}
