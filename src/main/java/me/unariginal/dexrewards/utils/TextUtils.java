package me.unariginal.dexrewards.utils;

import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.config.PlayerDataConfig;
import me.unariginal.dexrewards.datatypes.DexType;
import me.unariginal.dexrewards.datatypes.Messages;
import me.unariginal.dexrewards.datatypes.PlayerData;
import me.unariginal.dexrewards.datatypes.rewards.Reward;
import me.unariginal.dexrewards.datatypes.rewards.RewardGroup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.text.DecimalFormat;

public class TextUtils {
    public static Text deserialize(String text) {
        Component component = MiniMessage.miniMessage().deserialize(text);
        return DexRewards.INSTANCE.audience().toNative(component);
    }

    public static String parse(String text) {
        return text.replaceAll("%prefix%", Messages.prefix);
    }

    public static String parse(String text, DexType dexType) {
        return parse(text)
                .replaceAll("%dex_type%", dexType.displayName)
                .replaceAll("%pokedex.total%", String.valueOf(dexType.getTotal()));
    }

    public static String parse(String text, ServerPlayerEntity player) {
        return parse(text)
                .replaceAll("%player%", player.getNameForScoreboard());
    }

    public static String parse(String text, RewardGroup rewardGroup) {
        return parse(text)
                .replaceAll("%group.name%", rewardGroup != null ? rewardGroup.displayName : "null")
                .replaceAll("%group.percent%", rewardGroup != null ? String.valueOf(rewardGroup.required_percent) : "null");
    }

    public static String parse(String text, Reward reward) {
        return parse(text)
                .replaceAll("%reward%", reward.displayName);
    }

    public static String parse(String text, ItemStack itemStack) {
        return parse(text)
                .replaceAll("%item%", itemStack.getName().getString())
                .replaceAll("%item_count%", String.valueOf(itemStack.getCount()));
    }

    public static String parse(String text, DexType dexType, ServerPlayerEntity player) {
        text = parse(text, dexType);
        PlayerData playerData = PlayerDataConfig.getPlayerData(player.getUuid());
        if (playerData != null) {
            PlayerData.ProgressTracker progressTracker = playerData.getProgress(dexType.name);
            if (progressTracker != null) {
                String group = "None";
                for (RewardGroup rewardGroup : dexType.rewardGroups) {
                    if (progressTracker.claimed_rewards.contains(rewardGroup.name)) {
                        group = rewardGroup.displayName;
                    }
                }

                text = text
                        .replaceAll("%player.reward_group%", group)
                        .replaceAll("%player.dex_count%", String.valueOf(progressTracker.progress_count))
                        .replaceAll("%player.dex_percent%", new DecimalFormat("#.##").format(((double) progressTracker.progress_count / dexType.getTotal()) * 100));
            }
        }
        return text;
    }

    public static String parse(String text, ServerPlayerEntity player, RewardGroup rewardGroup) {
        text = parse(text, rewardGroup);
        text = parse(text, player);
        return text;
    }

    public static String parse(String text, ServerPlayerEntity player, RewardGroup rewardGroup, DexType dexType) {
        text = parse(text, rewardGroup);
        text = parse(text, dexType, player);
        return text;
    }
}
