package me.unariginal.dexrewards.datatypes;

import me.unariginal.dexrewards.datatypes.rewards.RewardGroup;
import net.minecraft.util.Identifier;

import java.util.List;

public class DexType {
    public String name;
    public String displayName;
    public String pokedex;
    public boolean countSeen;
    public boolean seenOnly;
    public boolean countShiny;
    public boolean shinyOnly;
    public List<RewardGroup> rewardGroups;

    public DexType(String name, String displayName, String pokedex, boolean countSeen, boolean seenOnly, boolean countShiny, boolean shinyOnly, List<RewardGroup> rewardGroups) {
        this.name = name;
        this.displayName = displayName;
        this.pokedex = pokedex;
        this.countSeen = countSeen;
        this.seenOnly = seenOnly;
        this.countShiny = countShiny;
        this.shinyOnly = shinyOnly;
        this.rewardGroups = rewardGroups;
    }

    public int getTotal() {
        int total = CustomPokedexValueCalculators.getTotalEntries(Identifier.of(pokedex));
        if (countShiny && !shinyOnly) {
            total += CustomPokedexValueCalculators.getTotalShinyEntries(Identifier.of(pokedex));
        } else if (countShiny) {
            total = CustomPokedexValueCalculators.getTotalShinyEntries(Identifier.of(pokedex));
        }
        return total;
    }

    public RewardGroup getGroup(String groupName) {
        for (RewardGroup group : rewardGroups) {
            if (group.name.equals(groupName)) {
                return group;
            }
        }
        return null;
    }
}
