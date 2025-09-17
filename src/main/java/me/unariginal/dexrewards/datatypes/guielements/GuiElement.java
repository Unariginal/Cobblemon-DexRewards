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
        String parsedName = name;
        parsedName = TextUtils.parse(parsedName, player, group, dexType);

        List<String> parsedLore;
        parsedLore = new ArrayList<>();
        for (String line : lore) {
            parsedLore.add(TextUtils.parse(line, player, group, dexType));
        }

        List<Text> loreText = new ArrayList<>();
        for (String line : parsedLore) {
            loreText.add(TextUtils.deserialize(line));
        }

        return ComponentMap.builder()
                .add(DataComponentTypes.CUSTOM_NAME, TextUtils.deserialize(parsedName))
                .add(DataComponentTypes.LORE, new LoreComponent(loreText))
                .add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, glint)
                .build();
    }
}
