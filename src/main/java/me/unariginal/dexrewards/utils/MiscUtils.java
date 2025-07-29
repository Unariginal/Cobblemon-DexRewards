package me.unariginal.dexrewards.utils;

import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class MiscUtils {
    public static ItemStack generateItem(String id, String name, List<String> lore, ComponentChanges item_data) {
        ItemStack stack = Registries.ITEM.get(Identifier.of(id)).getDefaultStack();

        List<Text> lore_text = new ArrayList<>();
        for (String line : lore) {
            lore_text.add(TextUtils.deserialize(line));
        }

        stack.applyComponentsFrom(ComponentMap.builder()
                .add(DataComponentTypes.CUSTOM_NAME, TextUtils.deserialize(name))
                .add(DataComponentTypes.LORE, new LoreComponent(lore_text))
                .build());
        stack.applyChanges(item_data);
        return stack;
    }
}
