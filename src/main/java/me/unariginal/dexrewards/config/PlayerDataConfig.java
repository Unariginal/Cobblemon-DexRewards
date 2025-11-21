package me.unariginal.dexrewards.config;

import com.google.gson.*;
import me.unariginal.dexrewards.datatypes.DexType;
import me.unariginal.dexrewards.datatypes.PlayerData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDataConfig {
    public static List<PlayerData> playerData = new ArrayList<>();

    public static PlayerData loadPlayerData(ServerPlayerEntity player) throws IOException {
        File playerDataFolder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/players/").toFile();
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdir();
        }

        File playerDataFile = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/players/" + player.getUuidAsString() + ".json").toFile();

        JsonObject root = new JsonObject();
        JsonObject newRoot = new JsonObject();
        if (playerDataFile.exists()) root = JsonParser.parseReader(new FileReader(playerDataFile)).getAsJsonObject();

        String uuid = player.getUuidAsString();
        String username = player.getNameForScoreboard();
        List<PlayerData.ProgressTracker> pokedexProgress = new ArrayList<>();

        if (root.has("uuid")) uuid = root.get("uuid").getAsString();
        newRoot.addProperty("uuid", uuid);

        if (root.has("username")) username = root.get("username").getAsString();
        newRoot.addProperty("username", username);

        JsonObject pokedexProgressObject = new JsonObject();
        if (!root.has("pokedex_progress")) {
            // Old data format, convert to new
            JsonObject nationalPokedexProgressObject = new JsonObject();

            int caughtCount = 0;
            List<String> claimedRewards = new ArrayList<>();
            List<String> claimableRewards = new ArrayList<>();

            if (root.has("caught_count")) caughtCount = root.get("caught_count").getAsInt();
            nationalPokedexProgressObject.addProperty("caught_count", caughtCount);

            if (root.has("claimed_rewards")) claimedRewards = new ArrayList<>(root.get("claimed_rewards").getAsJsonArray().asList().stream().map(element -> element.getAsString().toLowerCase()).toList());
            JsonArray claimedRewardsArray = new JsonArray();
            for (String claimedRewardString : claimedRewards) {
                claimedRewardsArray.add(claimedRewardString.toLowerCase());
            }
            nationalPokedexProgressObject.add("claimed_rewards", claimedRewardsArray);

            if (root.has("claimable_rewards")) claimableRewards = new ArrayList<>(root.get("claimable_rewards").getAsJsonArray().asList().stream().map(element -> element.getAsString().toLowerCase()).toList());
            JsonArray claimableRewardsArray = new JsonArray();
            for (String claimableRewardString : claimableRewards) {
                claimableRewardsArray.add(claimableRewardString.toLowerCase());
            }
            nationalPokedexProgressObject.add("claimable_rewards", claimableRewardsArray);

            pokedexProgressObject.add("national", nationalPokedexProgressObject);

            pokedexProgress.add(new PlayerData.ProgressTracker("national", caughtCount, claimedRewards, claimableRewards));
        } else {
            pokedexProgressObject = root.get("pokedex_progress").getAsJsonObject();
            for (String dexTypeID : pokedexProgressObject.keySet()) {
                JsonObject dexProgressObject = pokedexProgressObject.get(dexTypeID).getAsJsonObject();

                int caughtCount = 0;
                List<String> claimedRewards = new ArrayList<>();
                List<String> claimableRewards = new ArrayList<>();

                if (dexProgressObject.has("caught_count")) caughtCount = dexProgressObject.get("caught_count").getAsInt();
                dexProgressObject.addProperty("caught_count", caughtCount);
                if (dexProgressObject.has("claimed_rewards")) claimedRewards = new ArrayList<>(dexProgressObject.get("claimed_rewards").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList());
                JsonArray claimedRewardsArray = new JsonArray();
                for (String claimed : claimedRewards) {
                    claimedRewardsArray.add(claimed.toLowerCase());
                }
                dexProgressObject.add("claimed_rewards", claimedRewardsArray);
                if (dexProgressObject.has("claimable_rewards")) claimableRewards = new ArrayList<>(dexProgressObject.get("claimable_rewards").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList());
                JsonArray claimableRewardsArray = new JsonArray();
                for (String claimable : claimableRewards) {
                    claimableRewardsArray.add(claimable.toLowerCase());
                }
                dexProgressObject.add("claimable_rewards", claimableRewardsArray);

                pokedexProgressObject.add(dexTypeID, dexProgressObject);

                pokedexProgress.add(new PlayerData.ProgressTracker(dexTypeID, caughtCount, claimedRewards, claimableRewards));
            }
        }

        newRoot.add("pokedex_progress", pokedexProgressObject);

        playerData.removeIf(data -> data.uuid.equals(player.getUuid()));
        PlayerData data = new PlayerData(UUID.fromString(uuid), username, pokedexProgress);
        playerData.add(data);
        return data;
    }

    public static void updatePlayerData(PlayerData playerData) throws IOException {
        File playerDataFolder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/players/").toFile();
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdir();
        }

        File playerDataFile = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/players/" + playerData.uuid.toString() + ".json").toFile();
        playerDataFile.createNewFile();

        JsonObject root = new JsonObject();
        root.addProperty("uuid", playerData.uuid.toString());
        root.addProperty("username", playerData.username);

        JsonObject pokedexProgressObject = new JsonObject();
        for (DexType dexType : DexTypesConfig.dexTypes) {
            PlayerData.ProgressTracker progressTracker = playerData.getProgress(dexType.name);
            if (progressTracker == null) {
                progressTracker = new PlayerData.ProgressTracker(dexType.name, 0, List.of(), List.of());
                playerData.pokedexProgress.add(progressTracker);
            }
            JsonObject dexProgress = new JsonObject();
            dexProgress.addProperty("caught_count", progressTracker.progressCount);
            JsonArray claimedRewards = new JsonArray();
            for (String group : progressTracker.claimedRewards) {
                claimedRewards.add(group.toLowerCase());
            }
            dexProgress.add("claimed_rewards", claimedRewards);
            JsonArray claimableRewards = new JsonArray();
            for (String group : progressTracker.claimableRewards) {
                claimableRewards.add(group.toLowerCase());
            }
            dexProgress.add("claimable_rewards", claimableRewards);

            pokedexProgressObject.add(progressTracker.dexType, dexProgress);

        }
        root.add("pokedex_progress", pokedexProgressObject);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Writer writer = new FileWriter(playerDataFile);
        gson.toJson(root, writer);
        writer.close();

        PlayerDataConfig.playerData.removeIf(data -> data.uuid.equals(playerData.uuid));
        PlayerDataConfig.playerData.add(playerData);
    }

    public static PlayerData getPlayerData(UUID uuid) {
        for (PlayerData data : playerData) {
            if (data.uuid.equals(uuid)) {
                return data;
            }
        }
        return null;
    }
}
