package me.unariginal.dexrewards.datatypes;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.Dexes;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry;
import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.config.DexTypesConfig;
import me.unariginal.dexrewards.config.MessagesConfig;
import me.unariginal.dexrewards.datatypes.rewards.RewardGroup;
import me.unariginal.dexrewards.utils.TextUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import me.unariginal.dexrewards.datatypes.CustomPokedexValueCalculators.*;
import net.minecraft.util.Identifier;

import java.util.*;

public class PlayerData {
    public UUID uuid;
    public String username;
    public List<ProgressTracker> pokedexProgress;

    public static class ProgressTracker {
        public String dexType;
        public int progressCount;
        public List<String> claimedRewards;
        public List<String> claimableRewards;

        public ProgressTracker(String dexType, int progressCount, List<String> claimedRewards, List<String> claimableRewards) {
            this.dexType = dexType;
            this.progressCount = progressCount;
            this.claimedRewards = claimedRewards;
            this.claimableRewards = claimableRewards;
        }
    }

    public PlayerData(UUID uuid, String username, List<ProgressTracker> pokedexProgress) {
        this.uuid = uuid;
        this.username = username;
        this.pokedexProgress = pokedexProgress;
    }

    public ProgressTracker getProgress(String dexType) {
        for (ProgressTracker tracker : pokedexProgress) {
            if (tracker.dexType.equals(dexType)) {
                return tracker;
            }
        }
        return null;
    }

    public void updateCaughtCount() {
        PokedexManager dex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(uuid);

        for (ProgressTracker progressTracker : pokedexProgress) {
            DexType dexType = DexTypesConfig.getDexType(progressTracker.dexType);
            if (dexType == null) continue;

            Dexes.INSTANCE.getDexEntryMap().values().forEach(dexEntry ->
                    dex.getDexCalculatedValue(dexEntry.getId(), com.cobblemon.mod.common.api.pokedex.CaughtPercent.INSTANCE)
            );
            List<PokedexEntry> pokedexEntries = Dexes.INSTANCE.getDexEntryMap().get(Identifier.of(dexType.pokedex)).getEntries();
            Map<Identifier, PokedexEntry> pokedexEntryMap = new HashMap<>();
            for (PokedexEntry pokedexEntry : pokedexEntries) {
                pokedexEntryMap.put(pokedexEntry.getId(), pokedexEntry);
            }

            int count;
            if (dexType.requireCaught && dexType.requireShiny) {
                count = new CaughtShinyCount().calculate(dexType, dex, pokedexEntryMap);
            } else if (dexType.requireCaught) {
                count = new CaughtCount().calculate(dexType, dex, pokedexEntryMap);
            } else if (dexType.requireShiny) {
                count = new SeenShinyCount().calculate(dexType, dex, pokedexEntryMap);
            } else {
                count = new SeenCount().calculate(dexType, dex, pokedexEntryMap);
            }

            progressTracker.progressCount = count;
        }
    }

    public void updateClaimableRewards() {
        for (ProgressTracker progressTracker : pokedexProgress) {
            DexType dexType = DexTypesConfig.getDexType(progressTracker.dexType);
            if (dexType == null) continue;

            int oldSize = progressTracker.claimableRewards.size();

            ServerPlayerEntity player = DexRewards.INSTANCE.server().getPlayerManager().getPlayer(uuid);
            if (player != null) {
                List<String> newClaimableRewards = new ArrayList<>();
                for (RewardGroup group : dexType.rewardGroups) {
                    if (!progressTracker.claimedRewards.contains(group.name)) {
                        double percentComplete = ((double) progressTracker.progressCount / DexRewards.INSTANCE.dexTypeTotals.get(dexType)) * 100.0;
                        if (percentComplete >= group.requiredPercent) {
                            newClaimableRewards.add(group.name);
                            if (!progressTracker.claimableRewards.contains(group.name)) {
                                player.sendMessage(TextUtils.deserialize(TextUtils.parse(MessagesConfig.getMessage("reward_claimable"), group)));
                            }
                        }
                    }
                }

                progressTracker.claimableRewards = newClaimableRewards;
                if (oldSize < progressTracker.claimableRewards.size())
                    player.sendMessage(TextUtils.deserialize(TextUtils.parse(MessagesConfig.getMessage("rewards_to_claim"))));
            }
        }
    }
}
