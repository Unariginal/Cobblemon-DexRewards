package me.unariginal.dexrewards.config;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.datatypes.*;
import me.unariginal.dexrewards.utils.ConfigUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;

public class Config {
    public static ConfigData configData;

    public static class ConfigData {
        public boolean debug;
        @SerializedName(value = "default_dex_type", alternate = "defaultDexType")
        public String defaultDexType;
        @SerializedName(value = "implemented_only", alternate = "implementedOnly")
        public boolean implementedOnly;
        @SerializedName(value = "allow_invalid_species", alternate = "allowInvalidSpecies")
        public boolean allowInvalidSpecies;

        public ConfigData(boolean debug, String defaultDexType, boolean implementedOnly, boolean allowInvalidSpecies) {
            this.debug = debug;
            this.defaultDexType = defaultDexType;
            this.implementedOnly = implementedOnly;
            this.allowInvalidSpecies = allowInvalidSpecies;
        }
    }

    public static void load() throws IOException {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards").toFile();
        if (!rootFolder.exists()) rootFolder.mkdirs();

        File configFile = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/config.json").toFile();
        String json = "{}";
        if (!configFile.exists()) ConfigUtils.create(configFile, "/dr_config/config.json");
        if (configFile.exists()) json = JsonParser.parseReader(new FileReader(configFile)).toString();

        configData = gson.fromJson(json, ConfigData.class);
    }

    // TODO: Handle old data somewhere else
//    public Config() {
//        try {
//            loadOldRewards();
//            loadOldGUI();
//            loadOldPlayerData();
//        } catch (IOException e) {
//            DexRewards.LOGGER.error("[DexRewards] Failed to load config files!", e);
//        }
//    }

//    private void loadOldRewards() throws IOException {
//        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards").toFile();
//        if (!rootFolder.exists())
//            rootFolder.mkdirs();
//
//        File rewardsFile = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/rewards.json").toFile();
//        JsonObject newRoot = new JsonObject();
//        JsonObject root = new JsonObject();
//        if (rewardsFile.exists())
//            root = JsonParser.parseReader(new FileReader(rewardsFile)).getAsJsonObject();
//
//        JsonObject reward_groups = new JsonObject();
//        if (root.has("reward_groups"))
//            reward_groups = root.get("reward_groups").getAsJsonObject();
//
//        List<RewardGroup> rewardGroups = DexTypesConfig.getRewardGroups(reward_groups);
//
//        newRoot.add("reward_groups", reward_groups);
//
////        this.rewardGroups = rewardGroups;
//
//        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
//        Writer writer = new FileWriter(rewardsFile);
//        gson.toJson(newRoot, writer);
//        writer.close();
//    }
//
//    private void loadOldGUI() throws IOException {
//        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards").toFile();
//        if (!rootFolder.exists())
//            rootFolder.mkdirs();
//
//        File rewardGuiFile = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/reward_gui.json").toFile();
//
//        File messagesFile = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/messages.json").toFile();
//        JsonObject root = new JsonObject();
//        if (messagesFile.exists())
//            root = JsonParser.parseReader(new FileReader(messagesFile)).getAsJsonObject();
//
//        if (root.has("gui")) {
//            JsonObject gui = root.get("gui").getAsJsonObject();
//
//            JsonObject newRoot = RewardGUIConfig.guiMovementCompatMethod(gui);
//
//            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
//            Writer writer = new FileWriter(rewardGuiFile);
//            gson.toJson(newRoot, writer);
//            writer.close();
//        }
//    }

    // TODO: Translate old data format to new data format
//    private void loadOldPlayerData() throws IOException {
//        File oldPlayerDataFolder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/players").toFile();
//        if (!oldPlayerDataFolder.exists())
//            oldPlayerDataFolder.mkdirs();
//
//        for (File file : Objects.requireNonNull(oldPlayerDataFolder.listFiles())) {
//            if (file.getName().endsWith(".json")) {
//                JsonObject newRoot = new JsonObject();
//                FileReader reader = new FileReader(file);
//                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
//
//                if (!(root.has("uuid") && root.has("username"))) continue;
//
//                UUID uuid = UUID.fromString(root.get("uuid").getAsString());
//                newRoot.addProperty("uuid", uuid.toString());
//
//                String username = root.get("username").getAsString();
//                newRoot.addProperty("username", username);
//
//                int caught_count = 0;
//                if (root.has("caught_count"))
//                    caught_count = root.get("caught_count").getAsInt();
//                newRoot.addProperty("caught_count", caught_count);
//
//                JsonArray claimed = new JsonArray();
//                if (root.has("claimed_rewards"))
//                    root.get("claimed_rewards").getAsJsonArray();
//
//                List<String> claimed_rewards = new ArrayList<>();
//                for (JsonElement reward : claimed) {
//                    claimed_rewards.add(reward.getAsString());
//                }
//                claimed = new JsonArray();
//                for (String reward : claimed_rewards) {
//                    claimed.add(reward);
//                }
//                newRoot.add("claimed_rewards", claimed);
//
//                JsonArray claimable = new JsonArray();
//                if (root.has("claimable_rewards"))
//                    root.get("claimable_rewards").getAsJsonArray();
//
//                List<String> claimable_rewards = new ArrayList<>();
//                for (JsonElement reward : claimable) {
//                    claimable_rewards.add(reward.getAsString());
//                }
//                claimable = new JsonArray();
//                for (String reward : claimable_rewards) {
//                    claimable.add(reward);
//                }
//                newRoot.add("claimable_rewards", claimable);
//
//                PlayerData.ProgressTracker tracker = new PlayerData.ProgressTracker("national", caught_count, claimed_rewards, claimable_rewards);
//
//                PlayerDataConfig.player_data.add(new PlayerData(uuid, username, List.of(tracker)));
//
//                reader.close();
//
//                File playerDataFolder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/player_data/").toFile();
//                if (!playerDataFolder.exists()) {
//                    playerDataFolder.mkdir();
//                }
//
//                File newPlayerDataFile = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/player_data/" + uuid + ".json").toFile();
//
//                try {
//                    if (!file.delete()) DexRewards.LOGGER.error("[DexRewards] Failed to delete old player data file.");
//                } catch (Exception e) {
//                    DexRewards.LOGGER.error("Could not delete old player data file.", e);
//                }
//                newPlayerDataFile.delete();
//                newPlayerDataFile.createNewFile();
//
//                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
//                Writer writer = new FileWriter(newPlayerDataFile);
//                gson.toJson(newRoot, writer);
//                writer.close();
//            }
//        }
//        try {
//            if (!oldPlayerDataFolder.delete()) DexRewards.LOGGER.error("[DexRewards] Failed to delete old player data folder.");
//        } catch (Exception e) {
//            DexRewards.LOGGER.error("Could not delete old player data folder.", e);
//        }
//    }

    public static void generateImplementation(String id, String generation) {
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
