package me.unariginal.dexrewards.datatypes;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.datatypes.rewards.RewardGroup;
import me.unariginal.dexrewards.utils.TextUtils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerData {
    public UUID uuid;
    public String username;
    public int caught_count;
    public List<String> claimed_rewards;
    public List<String> claimable_rewards;

    public PlayerData(UUID uuid, String username, int caught_count, List<String> claimed_rewards, List<String> claimable_rewards) {
        this.uuid = uuid;
        this.username = username;
        this.caught_count = caught_count;
        this.claimed_rewards = claimed_rewards;
        this.claimable_rewards = claimable_rewards;
    }

    public int updateCaughtCount() {
        caught_count = 0;

        Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(uuid).getSpeciesRecords().values().forEach(speciesRecord -> {
            if (DexRewards.VALID_DEX_IDS.contains(speciesRecord.getId())) {
                if (speciesRecord.getKnowledge().equals(PokedexEntryProgress.CAUGHT)) {
                    caught_count++;
                }
            }
        });

        return caught_count;
    }

    public void updateClaimableRewards() {
        int old_size = claimable_rewards.size();
        ServerPlayerEntity player = DexRewards.INSTANCE.server().getPlayerManager().getPlayer(uuid);

        List<String> new_claimable_rewards = new ArrayList<>();
        for (RewardGroup group : DexRewards.INSTANCE.config().reward_groups) {
            if (!claimed_rewards.contains(group.name)) {
                double percent_caught = ((double) caught_count / DexRewards.DEX_TOTAL) * 100;
                if (percent_caught >= group.required_percent) {
                    new_claimable_rewards.add(group.name);
                    if (!claimable_rewards.contains(group.name)) {
                        if (player != null) {
                            player.sendMessage(TextUtils.deserialize(Messages.parse(Messages.reward_claimable, group)));
                        }
                    }
                }
            }
        }
        claimable_rewards = new_claimable_rewards;
        if (old_size < claimable_rewards.size()) {
            if (player != null) {
                player.sendMessage(TextUtils.deserialize(Messages.parse(Messages.rewards_to_claim)));
            }
        }
    }
}
