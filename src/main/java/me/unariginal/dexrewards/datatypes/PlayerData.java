package me.unariginal.dexrewards.datatypes;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.*;
import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.datatypes.rewards.RewardGroup;
import me.unariginal.dexrewards.utils.TextUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import me.unariginal.dexrewards.datatypes.CustomPokedexValueCalculators.*;
import net.minecraft.util.Identifier;

import java.util.*;

public class PlayerData {
    public UUID uuid;
    public String username;
    public List<ProgressTracker> pokedex_progress;

    public static class ProgressTracker {
        public String dex_type;
        public int caught_count;
        public List<String> claimed_rewards;
        public List<String> claimable_rewards;

        public ProgressTracker(String dex_type, int caught_count, List<String> claimed_rewards, List<String> claimable_rewards) {
            this.dex_type = dex_type;
            this.caught_count = caught_count;
            this.claimed_rewards = claimed_rewards;
            this.claimable_rewards = claimable_rewards;
        }
    }

    public PlayerData(UUID uuid, String username, List<ProgressTracker> pokedex_progress) {
        this.uuid = uuid;
        this.username = username;
        this.pokedex_progress = pokedex_progress;
    }

    public void updateCaughtCount() {
        PokedexManager dex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(uuid);

        for (ProgressTracker progressTracker : pokedex_progress) {
            DexType dexType = DexRewards.INSTANCE.dexTypes().getDexType(progressTracker.dex_type);
            if (dexType == null) continue;

            int seenCount = 0;
            if (dexType.countSeen)
                seenCount = dex.getDexCalculatedValue(Identifier.of(dexType.pokedex), SeenCount.INSTANCE);

            int shinyCount = 0;
            if (dexType.countShiny)
                shinyCount = dex.getDexCalculatedValue(Identifier.of(dexType.pokedex), new ShinyCount());

            int caughtCount = dex.getDexCalculatedValue(Identifier.of(dexType.pokedex), CaughtCount.INSTANCE);

            progressTracker.caught_count = caughtCount + seenCount + shinyCount;
        }
    }

    public void updateClaimableRewards() {
        for (ProgressTracker progressTracker : pokedex_progress) {
            DexType dexType = DexRewards.INSTANCE.dexTypes().getDexType(progressTracker.dex_type);
            if (dexType == null) continue;

            int oldSize = progressTracker.claimable_rewards.size();

            ServerPlayerEntity player = DexRewards.INSTANCE.server().getPlayerManager().getPlayer(uuid);
            if (player != null) {
                List<String> newClaimableRewards = new ArrayList<>();
                for (RewardGroup group : dexType.rewardGroups) {
                    if (!progressTracker.claimed_rewards.contains(group.name)) {
                        // TODO: Temporary "Dex Total", remember to get the correct number
                        double percentComplete = ((double) progressTracker.caught_count / DexRewards.DEX_TOTAL) * 100.0;
                        if (percentComplete >= group.required_percent) {
                            newClaimableRewards.add(group.name);
                            if (!progressTracker.claimable_rewards.contains(group.name)) {
                                player.sendMessage(TextUtils.deserialize(Messages.parse(Messages.reward_claimable, group)));
                            }
                        }
                    }
                }

                progressTracker.claimable_rewards = newClaimableRewards;
                if (oldSize < progressTracker.claimable_rewards.size())
                    player.sendMessage(TextUtils.deserialize(Messages.parse(Messages.rewards_to_claim)));
            }
        }
    }
}
