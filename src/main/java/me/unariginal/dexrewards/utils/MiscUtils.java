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
    public static ItemStack generateItem(String id, String name, List<String> lore, ComponentChanges itemData) {
        ItemStack stack = Registries.ITEM.get(Identifier.of(id)).getDefaultStack();

        List<Text> loreText = new ArrayList<>();
        for (String line : lore) {
            loreText.add(TextUtils.deserialize(line));
        }

        stack.applyComponentsFrom(ComponentMap.builder()
                .add(DataComponentTypes.CUSTOM_NAME, TextUtils.deserialize(name))
                .add(DataComponentTypes.LORE, new LoreComponent(loreText))
                .build());
        stack.applyChanges(itemData);
        return stack;
    }
}
