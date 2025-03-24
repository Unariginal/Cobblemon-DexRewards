package me.unariginal.dexrewards.datatypes.guielements;

import me.unariginal.dexrewards.datatypes.Messages;
import me.unariginal.dexrewards.datatypes.rewards.RewardGroup;
import me.unariginal.dexrewards.utils.TextUtils;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record GuiElement(String key, String name, List<String> lore, boolean glint) {
    public ComponentMap getComponentMap(UUID uuid, RewardGroup group) {
        String parsed_name = name;
        List<String> parsed_lore = lore;
        if (uuid != null) {
            parsed_name = Messages.parse(name, uuid);
            parsed_lore = new ArrayList<>();
            for (String line : lore) {
                parsed_lore.add(Messages.parse(line, uuid));
            }
        }

        List<String> more_parsed_lore = parsed_lore;
        if (group != null) {
            parsed_name = Messages.parse(name, group);
            parsed_lore = new ArrayList<>();
            for (String line : more_parsed_lore) {
                parsed_lore.add(Messages.parse(line, group));
            }
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
