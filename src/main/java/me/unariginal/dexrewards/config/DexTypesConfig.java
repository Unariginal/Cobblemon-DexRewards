package me.unariginal.dexrewards.config;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import me.unariginal.dexrewards.datatypes.DexType;
import me.unariginal.dexrewards.datatypes.rewards.CommandReward;
import me.unariginal.dexrewards.datatypes.rewards.ItemReward;
import me.unariginal.dexrewards.datatypes.rewards.Reward;
import me.unariginal.dexrewards.datatypes.rewards.RewardGroup;
import me.unariginal.dexrewards.utils.ConfigUtils;
import me.unariginal.dexrewards.utils.TextUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DexTypesConfig {
    public static List<DexType> dexTypes = new ArrayList<>();

    public static void load() throws IOException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/dex_types").toFile();
        if (!rootFolder.exists()) rootFolder.mkdirs();

        File[] files = rootFolder.listFiles();
        if (files == null || files.length == 0) {
            File configFile = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/dex_types/national.json").toFile();
            if (!configFile.exists()) ConfigUtils.create(configFile, "/dr_config/dex_types/national.json");
        }

        files = rootFolder.listFiles();
        if (files != null) {
            dexTypes.clear();
            for (File file : files) {
                if (file.getName().endsWith(".json")) loadDexType(file);
            }
        }
    }

    private static void loadDexType(File file) throws IOException {
        JsonObject root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

        String id = file.getName().substring(0, file.getName().lastIndexOf("."));
        String displayName = id;
        String pokedex = "cobblemon:national";
        boolean requireCaught = true;
        boolean requireShiny = false;
        List<String> validSpecies = new ArrayList<>();
        List<String> ignoredSpecies = new ArrayList<>();
        List<String> validLabels = new ArrayList<>();
        List<String> ignoredLabels = new ArrayList<>();
        List<RewardGroup> rewardGroups = new ArrayList<>();

        if (root.has("display_name")) displayName = root.get("display_name").getAsString();
        if (root.has("pokedex")) pokedex = root.get("pokedex").getAsString();
        if (root.has("require_caught")) requireCaught = root.get("require_caught").getAsBoolean();
        if (root.has("require_shiny")) requireShiny = root.get("require_shiny").getAsBoolean();
        if (root.has("valid_species")) validSpecies = root.get("valid_species").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        if (root.has("ignored_species")) ignoredSpecies = root.get("ignored_species").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        if (root.has("valid_labels")) validLabels = root.get("valid_labels").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        if (root.has("ignored_labels")) ignoredLabels = root.get("ignored_labels").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();

        JsonObject rewardGroupsObject = new JsonObject();
        if (root.has("reward_groups")) rewardGroupsObject = root.get("reward_groups").getAsJsonObject();
        for (String groupID : rewardGroupsObject.keySet()) {
            JsonObject rewardGroupObject = rewardGroupsObject.get(groupID).getAsJsonObject();

            if (!rewardGroupObject.has("required_percent")) continue;
            String rgDisplayName = groupID;
            String rgIcon = "cobblemon:pokeball";
            List<String> rgIconLore = new ArrayList<>();
            ComponentChanges rgIconData = ComponentChanges.EMPTY;
            List<Reward> rewards = new ArrayList<>();

            double requiredPercent = rewardGroupObject.get("required_percent").getAsDouble();
            if (rewardGroupObject.has("display_name")) rgDisplayName = rewardGroupObject.get("display_name").getAsString();
            if (rewardGroupObject.has("icon")) rgIcon = rewardGroupObject.get("icon").getAsString();
            if (rewardGroupObject.has("icon_lore")) rgIconLore = rewardGroupObject.get("icon_lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
            if (rewardGroupObject.has("icon_data")) rgIconData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, rewardGroupObject.get("icon_data")).getOrThrow().getFirst();

            List<Text> iconLore = new ArrayList<>();
            for (String line : rgIconLore) {
                iconLore.add(TextUtils.deserialize(line));
            }

            ItemStack iconItem = Registries.ITEM.get(Identifier.of(rgIcon)).getDefaultStack();
            iconItem.applyComponentsFrom(ComponentMap.builder()
                    .add(DataComponentTypes.CUSTOM_NAME, TextUtils.deserialize(rgDisplayName))
                    .add(DataComponentTypes.LORE, new LoreComponent(iconLore))
                    .build()
            );
            iconItem.applyChanges(rgIconData);

            JsonObject rgRewardsObject = new JsonObject();
            if (rewardGroupObject.has("rewards")) rgRewardsObject = rewardGroupObject.get("rewards").getAsJsonObject();
            for (String rewardID : rgRewardsObject.keySet()) {
                JsonObject rewardObject = rgRewardsObject.get(rewardID).getAsJsonObject();

                if (!rewardObject.has("type")) continue;
                String rewardDisplayName = rewardID;
                String type = rewardObject.get("type").getAsString();
                if (rewardObject.has("display_name")) rewardDisplayName = rewardObject.get("display_name").getAsString();

                switch (type) {
                    case "item" -> {
                        if (!(rewardObject.has("item"))) continue;
                        String item = rewardObject.get("item").getAsString();
                        String itemName = "";
                        List<String> itemLore = new ArrayList<>();
                        ComponentChanges itemData = ComponentChanges.EMPTY;
                        int count = 1;

                        if (rewardObject.has("item_name")) itemName = rewardObject.get("item_name").getAsString();
                        if (rewardObject.has("item_lore")) itemLore = rewardObject.get("item_lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
                        if (rewardObject.has("item_data")) itemData = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, rewardObject.get("item_data")).getOrThrow().getFirst();
                        if (rewardObject.has("count")) count = rewardObject.get("count").getAsInt();

                        List<Text> rewardLore = new ArrayList<>();
                        for (String line : itemLore) {
                            rewardLore.add(TextUtils.deserialize(line));
                        }

                        ItemStack rewardItem = Registries.ITEM.get(Identifier.of(item)).getDefaultStack();
                        rewardItem.applyComponentsFrom(ComponentMap.builder()
                                .add(DataComponentTypes.CUSTOM_NAME, TextUtils.deserialize(itemName))
                                .add(DataComponentTypes.LORE, new LoreComponent(rewardLore))
                                .build()
                        );
                        rewardItem.applyChanges(itemData);
                        rewardItem.setCount(count);

                        rewards.add(new ItemReward(
                                rewardID,
                                type,
                                rewardDisplayName,
                                rewardItem
                        ));
                    }
                    case "command" -> {
                        if (!rewardObject.has("commands")) continue;
                        List<String> commands = rewardObject.get("commands").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();

                        rewards.add(new CommandReward(
                                rewardID,
                                type,
                                rewardDisplayName,
                                commands
                        ));
                    }
                }
            }

            rewardGroups.add(new RewardGroup(
                    groupID,
                    iconItem,
                    requiredPercent,
                    rgDisplayName,
                    rewards
            ));
        }

        dexTypes.add(new DexType(
                id,
                displayName,
                pokedex,
                requireCaught,
                requireShiny,
                validSpecies,
                ignoredSpecies,
                validLabels,
                ignoredLabels,
                rewardGroups
        ));
    }

    public static DexType getDexType(String name) {
        for (DexType dexType : dexTypes) {
            if (dexType.name.equals(name)) {
                return dexType;
            }
        }
        return null;
    }
}
