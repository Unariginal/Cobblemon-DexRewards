package me.unariginal.dexrewards.config;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.datatypes.*;
import me.unariginal.dexrewards.datatypes.rewards.CommandReward;
import me.unariginal.dexrewards.datatypes.rewards.ItemReward;
import me.unariginal.dexrewards.datatypes.rewards.Reward;
import me.unariginal.dexrewards.datatypes.rewards.RewardGroup;
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

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Config {
    public boolean implemented_only = true;
    public boolean allow_invalid_species = false;
    public List<Species> species_blacklist = new ArrayList<>();
    public List<String> label_blacklist = new ArrayList<>();
    public List<String> generation_blacklist = new ArrayList<>();

    public List<RewardGroup> reward_groups = new ArrayList<>();

    public Config() {
        try {
            loadConfig();
            loadOldRewards();
            loadOldGUI();
            loadOldPlayerData();
        } catch (IOException e) {
            DexRewards.LOGGER.error("[DexRewards] Failed to load config files!", e);
        }
    }

    private void loadConfig() throws IOException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards").toFile();
        if (!rootFolder.exists())
            rootFolder.mkdirs();

        File configFile = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/config.json").toFile();
        JsonObject newRoot = new JsonObject();
        JsonObject root = new JsonObject();
        if (configFile.exists())
            root = JsonParser.parseReader(new FileReader(configFile)).getAsJsonObject();

        if (root.has("debug"))
            DexRewards.DEBUG = root.get("debug").getAsBoolean();
        newRoot.addProperty("debug", DexRewards.DEBUG);

        if (root.has("implemented_only"))
            implemented_only = root.get("implemented_only").getAsBoolean();
        newRoot.addProperty("implemented_only", implemented_only);

        if (root.has("allow_invalid_species"))
            allow_invalid_species = root.get("allow_invalid_species").getAsBoolean();
        newRoot.addProperty("allow_invalid_species", allow_invalid_species);

        JsonObject blacklist = new JsonObject();
        if (root.has("blacklist"))
            blacklist = root.get("blacklist").getAsJsonObject();

        JsonArray species_blacklist = new JsonArray();
        if (blacklist.has("species"))
            species_blacklist = blacklist.get("species").getAsJsonArray();

        this.species_blacklist.clear();
        for (JsonElement element : species_blacklist) {
            String species_id = element.getAsString();
            Species species = PokemonSpecies.INSTANCE.getByName(species_id);
            if (species != null) {
                this.species_blacklist.add(species);
            }
        }
        species_blacklist = new JsonArray();
        for (Species species : this.species_blacklist) {
            species_blacklist.add(species.getName());
        }
        blacklist.add("species", species_blacklist);

        if (blacklist.has("label"))
            label_blacklist = blacklist.get("label").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        JsonArray labelBlacklist = new JsonArray();
        for (String label : label_blacklist) {
            labelBlacklist.add(label);
        }
        blacklist.add("label", labelBlacklist);

        if (blacklist.has("generation"))
            generation_blacklist = blacklist.get("generation").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        JsonArray generationBlacklist = new JsonArray();
        for (String generation : generation_blacklist) {
            generationBlacklist.add(generation);
        }
        blacklist.add("generation", generationBlacklist);

        newRoot.add("blacklist", blacklist);

        configFile.delete();
        configFile.createNewFile();

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(configFile);
        gson.toJson(newRoot, writer);
        writer.close();
    }

    // TODO: Move to separate file, multiple reward files
    private void loadOldRewards() throws IOException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards").toFile();
        if (!rootFolder.exists())
            rootFolder.mkdirs();

        File rewardsFile = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/rewards.json").toFile();
        JsonObject newRoot = new JsonObject();
        JsonObject root = new JsonObject();
        if (rewardsFile.exists())
            root = JsonParser.parseReader(new FileReader(rewardsFile)).getAsJsonObject();

        JsonObject reward_groups = new JsonObject();
        if (root.has("reward_groups")) {
            reward_groups = root.get("reward_groups").getAsJsonObject();
        }

        List<RewardGroup> rewardGroups = new ArrayList<>();
        for (String key : reward_groups.keySet()) {
            JsonObject reward_group = reward_groups.get(key).getAsJsonObject();

            if (!reward_group.has("required_caught_percent")) continue;
            double required_percent = reward_group.get("required_caught_percent").getAsDouble();

            String displayName = key;
            if (reward_group.has("display_name"))
                displayName = reward_group.get("display_name").getAsString();
            reward_group.addProperty("display_name", displayName);

            String icon_id = "cobblemon:poke_ball";
            if (reward_group.has("icon"))
                icon_id = reward_group.get("icon").getAsString();
            reward_group.addProperty("icon", icon_id);
            ItemStack icon = Registries.ITEM.get(Identifier.of(icon_id)).getDefaultStack();

            JsonElement icon_data = new JsonObject();
            if (reward_group.has("icon_data")) {
                icon_data = reward_group.get("icon_data");
                if (icon_data != null) {
                    icon.applyChanges(ComponentChanges.CODEC.decode(JsonOps.INSTANCE, icon_data).getOrThrow().getFirst());
                }
            }
            reward_group.add("icon_data", icon_data);

            JsonObject rewards_object = reward_group.get("rewards").getAsJsonObject();
            List<Reward> rewards = new ArrayList<>();
            for (String reward_key : rewards_object.keySet()) {
                JsonObject reward = rewards_object.get(reward_key).getAsJsonObject();
                if (!reward.has("type")) continue;
                String type = reward.get("type").getAsString();

                String rewardDisplayName = reward_key;
                if (reward.has("display_name"))
                    rewardDisplayName = reward.get("display_name").getAsString();
                reward.addProperty("display_name", rewardDisplayName);

                if (type.equalsIgnoreCase("item")) {
                    if (!reward.has("item")) continue;
                    String item_id = reward.get("item").getAsString();

                    int count = 1;
                    if (reward.has("count"))
                        count = reward.get("count").getAsInt();
                    reward.addProperty("count", count);

                    String itemName = "";
                    if (reward.has("item_name"))
                        itemName = reward.get("item_name").getAsString();
                    reward.addProperty("item_name", itemName);

                    List<String> itemLore = new ArrayList<>();
                    if (reward.has("item_lore"))
                        itemLore = reward.getAsJsonArray("item_lore").asList().stream().map(JsonElement::getAsString).toList();
                    JsonArray itemLore_array = new JsonArray();
                    for (String lore : itemLore) {
                        itemLore_array.add(lore);
                    }
                    reward.add("item_lore", itemLore_array);

                    ComponentChanges itemDataChanges = ComponentChanges.EMPTY;
                    JsonElement itemData = new JsonObject();
                    if (reward.has("item_data")) {
                        itemData = reward.get("item_data");
                        if (itemData != null)
                            itemDataChanges = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, itemData).getOrThrow().getFirst();
                    }
                    reward.add("item_data", itemData);

                    List<Text> loreArray = new ArrayList<>();
                    for (String lore : itemLore) {
                        loreArray.add(TextUtils.deserialize(lore));
                    }

                    ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(item_id)), count);
                    item.applyComponentsFrom(
                            ComponentMap.builder()
                                    .add(DataComponentTypes.CUSTOM_NAME, TextUtils.deserialize(itemName))
                                    .add(DataComponentTypes.LORE, new LoreComponent(loreArray))
                                    .build()
                    );
                    item.applyChanges(itemDataChanges);

                    rewards.add(new ItemReward(reward_key, type, rewardDisplayName, item));
                } else if (type.equalsIgnoreCase("command")) {
                    if (!reward.has("commands")) continue;
                    List<String> commands = reward.get("commands").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
                    rewards.add(new CommandReward(reward_key, type, rewardDisplayName, commands));
                }
            }

            rewardGroups.add(new RewardGroup(key, icon, required_percent, displayName, rewards));
            reward_groups.add(key, reward_group);
        }

        newRoot.add("reward_groups", reward_groups);

        this.reward_groups = rewardGroups;

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(rewardsFile);
        gson.toJson(newRoot, writer);
        writer.close();
    }

    private void loadOldGUI() throws IOException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards").toFile();
        if (!rootFolder.exists())
            rootFolder.mkdirs();

        File rewardGuiFile = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/reward_gui.json").toFile();

        File messagesFile = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/messages.json").toFile();
        JsonObject root = new JsonObject();
        if (messagesFile.exists())
            root = JsonParser.parseReader(new FileReader(messagesFile)).getAsJsonObject();

        if (root.has("gui")) {
            JsonObject gui = root.get("gui").getAsJsonObject();

            JsonObject newRoot = RewardGUIConfig.guiMovementCompatMethod(gui);

            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            Writer writer = new FileWriter(rewardGuiFile);
            gson.toJson(newRoot, writer);
            writer.close();
        }
    }

    // TODO: Translate old data format to new data format
    private void loadOldPlayerData() throws IOException {
        File old_player_data_folder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/players").toFile();
        if (!old_player_data_folder.exists())
            old_player_data_folder.mkdirs();

        for (File file : Objects.requireNonNull(old_player_data_folder.listFiles())) {
            if (file.getName().endsWith(".json")) {
                JsonObject newRoot = new JsonObject();
                JsonObject root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

                if (!(root.has("uuid") && root.has("username"))) continue;

                UUID uuid = UUID.fromString(root.get("uuid").getAsString());
                newRoot.addProperty("uuid", uuid.toString());

                String username = root.get("username").getAsString();
                newRoot.addProperty("username", username);

                int caught_count = 0;
                if (root.has("caught_count"))
                    caught_count = root.get("caught_count").getAsInt();
                newRoot.addProperty("caught_count", caught_count);

                JsonArray claimed = new JsonArray();
                if (root.has("claimed_rewards"))
                    root.get("claimed_rewards").getAsJsonArray();

                List<String> claimed_rewards = new ArrayList<>();
                for (JsonElement reward : claimed) {
                    claimed_rewards.add(reward.getAsString());
                }
                claimed = new JsonArray();
                for (String reward : claimed_rewards) {
                    claimed.add(reward);
                }
                newRoot.add("claimed_rewards", claimed);

                JsonArray claimable = new JsonArray();
                if (root.has("claimable_rewards"))
                    root.get("claimable_rewards").getAsJsonArray();

                List<String> claimable_rewards = new ArrayList<>();
                for (JsonElement reward : claimable) {
                    claimable_rewards.add(reward.getAsString());
                }
                claimable = new JsonArray();
                for (String reward : claimable_rewards) {
                    claimable.add(reward);
                }
                newRoot.add("claimable_rewards", claimable);

                PlayerData.ProgressTracker tracker = new PlayerData.ProgressTracker("national", caught_count, claimed_rewards, claimable_rewards);

                PlayerDataConfig.player_data.add(new PlayerData(uuid, username, List.of(tracker)));

                File player_data_folder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/player_data/").toFile();
                if (!player_data_folder.exists()) {
                    player_data_folder.mkdir();
                }

                File new_player_data_file = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/player_data/" + uuid + ".json").toFile();

                try {
                    if (!file.delete()) {
                        DexRewards.LOGGER.error("[DexRewards] Failed to delete old player data file.");
                    }
                } catch (Exception e) {
                    DexRewards.LOGGER.error("Could not delete old player data file.", e);
                }
                new_player_data_file.delete();
                new_player_data_file.createNewFile();

                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                Writer writer = new FileWriter(new_player_data_file);
                gson.toJson(newRoot, writer);
                writer.close();
            }
        }
        try {
            if (!old_player_data_folder.delete()) {
                DexRewards.LOGGER.error("[DexRewards] Failed to delete old player data folder.");
            }
        } catch (Exception e) {
            DexRewards.LOGGER.error("Could not delete old player data folder.", e);
        }
    }

    public void generateImplementation(String id, String generation) {
        try {
            File generated_folder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/Generated").toFile();
            if (!generated_folder.exists()) {
                generated_folder.mkdirs();
            }

            File generation_folder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/Generated/" + generation).toFile();
            if (!generation_folder.exists()) {
                generation_folder.mkdirs();
            }

            File implementation_file = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/Generated/" + generation + "/" + id.replaceAll("cobblemon:", "") + ".json").toFile();
            if (implementation_file.createNewFile()) {

                JsonObject root = new JsonObject();
                root.addProperty("target", id);
                root.addProperty("implemented", true);

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Writer writer = new FileWriter(implementation_file);
                gson.toJson(root, writer);
                writer.close();
            }
        } catch (IOException e) {
            DexRewards.LOGGER.error("[DexRewards] Error while generating implementation files.", e);
        }
    }
}
