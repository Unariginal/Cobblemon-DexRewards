package me.unariginal.dexrewards.config;

import com.cobblemon.mod.common.CobblemonItems;
import me.unariginal.dexrewards.datatypes.DexType;
import me.unariginal.dexrewards.datatypes.rewards.ItemReward;
import me.unariginal.dexrewards.datatypes.rewards.RewardGroup;

import java.util.ArrayList;
import java.util.List;

public class DexTypesConfig {
    public List<DexType> dexTypes = new ArrayList<>(
            List.of(
                    new DexType("national", "National Dex", "cobblemon:national", false, false, false, false,
                            List.of(
                                    new RewardGroup(
                                            "trainer",
                                            CobblemonItems.POKE_BALL.getDefaultStack(),
                                            5.0,
                                            "Trainer",
                                            List.of(
                                                    new ItemReward(
                                                            "example_item_reward",
                                                            "item",
                                                            "%item_count%x %item%",
                                                            CobblemonItems.POKE_BALL.getDefaultStack().copyWithCount(16)
                                                    )
                                            )
                                    ),
                                    new RewardGroup(
                                            "rookie",
                                            CobblemonItems.FAST_BALL.getDefaultStack(),
                                            10.0,
                                            "Rookie",
                                            List.of(
                                                    new ItemReward(
                                                            "example_item_reward",
                                                            "item",
                                                            "%item_count%x %item%",
                                                            CobblemonItems.GREAT_BALL.getDefaultStack().copyWithCount(16)
                                                    )
                                            )
                                    ),
                                    new RewardGroup(
                                            "beginner",
                                            CobblemonItems.GREAT_BALL.getDefaultStack(),
                                            15.0,
                                            "Beginner",
                                            List.of(
                                                    new ItemReward(
                                                            "example_item_reward",
                                                            "item",
                                                            "%item_count%x %item%",
                                                            CobblemonItems.THUNDER_STONE.getDefaultStack().copyWithCount(2)
                                                    )
                                            )
                                    )
                            )
                    )
            )
    );

    public DexType getDexType(String name) {
        for (DexType dexType : dexTypes) {
            if (dexType.name.equals(name)) {
                return dexType;
            }
        }
        return null;
    }
}
