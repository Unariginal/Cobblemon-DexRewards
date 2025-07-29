package me.unariginal.dexrewards.datatypes.guielements;

import me.unariginal.dexrewards.datatypes.DexType;
import me.unariginal.dexrewards.datatypes.rewards.RewardGroup;
import me.unariginal.dexrewards.utils.TextUtils;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public record GuiElement(String key, String name, List<String> lore, boolean glint) {
    public ComponentMap getComponentMap(ServerPlayerEntity player, RewardGroup group, DexType dexType) {
        String parsed_name = name;
        parsed_name = TextUtils.parse(parsed_name, player, group, dexType);

        List<String> parsed_lore;
        parsed_lore = new ArrayList<>();
        for (String line : lore) {
            parsed_lore.add(TextUtils.parse(line, player, group, dexType));
        }

        List<Text> lore_text = new ArrayList<>();
        for (String line : parsed_lore) {
            lore_text.add(TextUtils.deserialize(line));
        }

        return ComponentMap.builder()
                .add(DataComponentTypes.CUSTOM_NAME, TextUtils.deserialize(parsed_name))
                .add(DataComponentTypes.LORE, new LoreComponent(lore_text))
                .add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, glint)
                .build();
    }
}
