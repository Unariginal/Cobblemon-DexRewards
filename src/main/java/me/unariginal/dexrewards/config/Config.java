package me.unariginal.dexrewards.config;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.datatypes.*;
import me.unariginal.dexrewards.datatypes.guielements.GuiElement;
import me.unariginal.dexrewards.datatypes.guielements.GuiLayout;
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
    public List<PlayerData> player_data = new ArrayList<>();
    public List<GuiElement> gui_elements = new ArrayList<>();
    public GuiLayout gui_layout;

    public Config() {
        try {
            checkFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }

        loadConfig();
        loadRewards();
        loadMessages();
        loadPlayerData();
    }

    private void checkFiles() throws IOException {
        File main_file = FabricLoader.getInstance().getConfigDir().resolve("DexRewards").toFile();
        if (!main_file.exists()) {
            main_file.mkdirs();
        }

        String[] files = {
                "config.json",
                "messages.json",
                "rewards.json"
        };

        for (String file : files) {
            File config_file = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/" + file).toFile();
            if (config_file.createNewFile()) {
                InputStream in = DexRewards.class.getResourceAsStream("/dr_config/" + file);
                OutputStream out = new FileOutputStream(config_file);
                assert in != null;

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                in.close();
                out.close();
            }
        }
    }

    private JsonObject getRoot(File file) {
        JsonElement root_element;
        try {
            root_element = JsonParser.parseReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return root_element.getAsJsonObject();
    }

    private void loadConfig() {
        File config_file = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/config.json").toFile();

        JsonObject root = getRoot(config_file);
        if (root == null) {
            return;
        }

        DexRewards.DEBUG = root.get("debug").getAsBoolean();
        implemented_only = root.get("implemented_only").getAsBoolean();
        allow_invalid_species = root.get("allow_invalid_species").getAsBoolean();

        JsonObject blacklist = root.get("blacklist").getAsJsonObject();

        JsonArray species_blacklist = blacklist.get("species").getAsJsonArray();
        for (JsonElement element : species_blacklist) {
            String species_id = element.getAsString();
            Species species = PokemonSpecies.INSTANCE.getByName(species_id);
            if (species != null) {
                this.species_blacklist.add(species);
            }
        }

        label_blacklist = blacklist.get("label").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        generation_blacklist = blacklist.get("generation").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
    }

    private void loadRewards() {
        File rewards_file = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/rewards.json").toFile();

        JsonObject root = getRoot(rewards_file);
        if (root == null) {
            return;
        }

        JsonObject reward_groups = root.get("reward_groups").getAsJsonObject();
        List<RewardGroup> rewardGroups = new ArrayList<>();
        for (String key : reward_groups.keySet()) {
            JsonObject reward_group = reward_groups.get(key).getAsJsonObject();

            String icon_id = reward_group.get("icon").getAsString();
            ItemStack icon = Registries.ITEM.get(Identifier.of(icon_id)).getDefaultStack();
            JsonElement icon_data = reward_group.get("icon_data");
            if (icon_data != null) {
                icon.applyChanges(ComponentChanges.CODEC.decode(JsonOps.INSTANCE, icon_data).getOrThrow().getFirst());
            }

            double required_percent = reward_group.get("required_caught_percent").getAsDouble();

            JsonObject rewards_object = reward_group.get("rewards").getAsJsonObject();
            List<Reward> rewards = new ArrayList<>();
            for (String reward_key : rewards_object.keySet()) {
                JsonObject reward = rewards_object.get(reward_key).getAsJsonObject();
                String type = reward.get("type").getAsString();
                if (type.equalsIgnoreCase("item")) {
                    String item_id = reward.get("item").getAsString();
                    int count = reward.get("count").getAsInt();

                    ItemStack item = new ItemStack(Registries.ITEM.get(Identifier.of(item_id)), count);

                    JsonElement item_data = reward.get("item_data");
                    if (item_data != null) {
                        item.applyChanges(ComponentChanges.CODEC.decode(JsonOps.INSTANCE, item_data).getOrThrow().getFirst());
                    }

                    rewards.add(new ItemReward(reward_key, type, item));
                } else if (type.equalsIgnoreCase("command")) {
                    List<String> commands = reward.get("commands").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
                    rewards.add(new CommandReward(reward_key, type, commands));
                }
            }

            rewardGroups.add(new RewardGroup(key, icon, required_percent, rewards));
        }

        this.reward_groups = rewardGroups;
    }

    private void loadMessages() {
        File rewards_file = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/messages.json").toFile();

        JsonObject root = getRoot(rewards_file);
        if (root == null) {
            return;
        }

        Messages.prefix = root.get("prefix").getAsString();

        JsonObject messages = root.get("messages").getAsJsonObject();
        Messages.rewards_to_claim = messages.get("rewards_to_claim").getAsString();
        Messages.reward_claimable = messages.get("reward_claimable").getAsString();
        Messages.rewards_claimed = messages.get("rewards_claimed").getAsString();
        Messages.update_command = messages.get("update_command").getAsString();
        Messages.reset_sender = messages.get("reset_command_sender").getAsString();
        Messages.reset_target = messages.get("reset_command_target").getAsString();
        Messages.reload = messages.get("reload_command").getAsString();

        JsonObject gui = root.get("gui").getAsJsonObject();
        String title = gui.get("title").getAsString();
        int size = gui.get("size").getAsInt();

        List<String> page_layout = gui.get("page_layout").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();

        JsonObject page_layout_symbols = gui.get("page_layout_symbols").getAsJsonObject();
        String background_symbol = page_layout_symbols.get("background").getAsString();
        String player_info_symbol = page_layout_symbols.get("player_info").getAsString();
        String group_symbol = page_layout_symbols.get("group").getAsString();
        String previous_page_symbol = page_layout_symbols.get("previous_page").getAsString();
        String next_page_symbol = page_layout_symbols.get("next_page").getAsString();

        JsonObject background = gui.get("background").getAsJsonObject();
        String background_item_id = background.get("item").getAsString();
        String background_name = background.get("name").getAsString();
        List<String> background_lore = background.get("lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        JsonElement background_item_data = background.get("item_data");
        ItemStack background_item = generateItem(background_item_id, background_name, background_lore, background_item_data);

        JsonObject navigation = gui.get("navigation").getAsJsonObject();
        String previous_item_id = navigation.get("previous_item").getAsString();
        String previous_name = navigation.get("previous_name").getAsString();
        List<String> previous_lore = navigation.get("previous_lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        JsonElement previous_item_data = navigation.get("previous_item_data");
        ItemStack previous_item = generateItem(previous_item_id, previous_name, previous_lore, previous_item_data);

        String next_item_id = navigation.get("next_item").getAsString();
        String next_name = navigation.get("next_name").getAsString();
        List<String> next_lore = navigation.get("next_lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        JsonElement next_item_data = navigation.get("next_item_data");
        ItemStack next_item = generateItem(next_item_id, next_name, next_lore, next_item_data);

        String[] elements = {
                "player_info",
                "claimed_group",
                "claimable_group",
                "locked_group"
        };

        for (String element : elements) {
            JsonObject element_object = gui.get(element).getAsJsonObject();
            String name = element_object.get("name").getAsString();
            List<String> lore = element_object.get("lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
            boolean glint = element_object.get("show_enchantment_glint").getAsBoolean();
            gui_elements.add(new GuiElement(element, name, lore, glint));
        }

        gui_layout = new GuiLayout(title, size, page_layout, background_symbol, player_info_symbol, group_symbol, previous_page_symbol, next_page_symbol, background_item, previous_item, next_item);
    }

    private ItemStack generateItem(String id, String name, List<String> lore, JsonElement item_data) {
        ItemStack stack = Registries.ITEM.get(Identifier.of(id)).getDefaultStack();

        List<Text> lore_text = new ArrayList<>();
        for (String line : lore) {
            lore_text.add(TextUtils.deserialize(line));
        }

        stack.applyComponentsFrom(ComponentMap.builder()
                .add(DataComponentTypes.CUSTOM_NAME, TextUtils.deserialize(name))
                .add(DataComponentTypes.LORE, new LoreComponent(lore_text))
                .build());
        if (item_data != null) {
            stack.applyChanges(ComponentChanges.CODEC.decode(JsonOps.INSTANCE, item_data).getOrThrow().getFirst());
        }
        return stack;
    }

    private void loadPlayerData() {
        File player_data_folder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/players").toFile();
        if (!player_data_folder.exists()) {
            player_data_folder.mkdirs();
        }

        for (File file : Objects.requireNonNull(player_data_folder.listFiles())) {
            if (file.getName().endsWith(".json")) {
                JsonObject root = getRoot(file);
                if (root == null) {
                    continue;
                }

                UUID uuid = UUID.fromString(root.get("uuid").getAsString());
                String username = root.get("username").getAsString();
                int caught_count = root.get("caught_count").getAsInt();

                JsonArray claimed = root.get("claimed_rewards").getAsJsonArray();
                List<String> claimed_rewards = new ArrayList<>();
                for (JsonElement reward : claimed) {
                    claimed_rewards.add(reward.getAsString());
                }

                JsonArray claimable = root.get("claimable_rewards").getAsJsonArray();
                List<String> claimable_rewards = new ArrayList<>();
                for (JsonElement reward : claimable) {
                    claimable_rewards.add(reward.getAsString());
                }

                player_data.add(new PlayerData(uuid, username, caught_count, claimed_rewards, claimable_rewards));
            }
        }
    }

    public void updatePlayerData(PlayerData playerData) {
        try {
            File player_data_file = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/players/" + playerData.uuid.toString() + ".json").toFile();
            player_data_file.createNewFile();

            JsonObject root = new JsonObject();
            root.addProperty("uuid", playerData.uuid.toString());
            root.addProperty("username", playerData.username);
            root.addProperty("caught_count", playerData.updateCaughtCount());

            JsonArray claimed_rewards = new JsonArray();
            for (String group : playerData.claimed_rewards) {
                claimed_rewards.add(group);
            }
            root.add("claimed_rewards", claimed_rewards);

            JsonArray claimable_rewards = new JsonArray();
            for (String group : playerData.claimable_rewards) {
                claimable_rewards.add(group);
            }
            root.add("claimable_rewards", claimable_rewards);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Writer writer = new FileWriter(player_data_file);
            gson.toJson(root, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        player_data.removeIf(data -> data.uuid.equals(playerData.uuid));
        player_data.add(playerData);
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
            e.printStackTrace();
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        for (PlayerData data : player_data) {
            if (data.uuid.equals(uuid)) {
                return data;
            }
        }
        return null;
    }
}
