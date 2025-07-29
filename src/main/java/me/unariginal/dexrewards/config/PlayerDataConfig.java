package me.unariginal.dexrewards.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.datatypes.PlayerData;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDataConfig {
    public static List<PlayerData> player_data = new ArrayList<>();

    public static void updatePlayerData(PlayerData playerData) {
        try {
            File player_data_folder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/player_data/").toFile();
            if (!player_data_folder.exists()) {
                player_data_folder.mkdir();
            }

            File player_data_file = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/player_data/" + playerData.uuid.toString() + ".json").toFile();
            player_data_file.createNewFile();

            JsonObject root = new JsonObject();
            root.addProperty("uuid", playerData.uuid.toString());
            root.addProperty("username", playerData.username);

            JsonObject pokedex_progress = new JsonObject();
            for (PlayerData.ProgressTracker progressTracker : playerData.pokedex_progress) {
                JsonObject dex_progress = new JsonObject();
                dex_progress.addProperty("caught_count", progressTracker.progress_count);
                JsonArray claimed_rewards = new JsonArray();
                for (String group : progressTracker.claimed_rewards) {
                    claimed_rewards.add(group);
                }
                dex_progress.add("claimed_rewards", claimed_rewards);
                JsonArray claimable_rewards = new JsonArray();
                for (String group : progressTracker.claimable_rewards) {
                    claimable_rewards.add(group);
                }
                dex_progress.add("claimable_rewards", claimable_rewards);

                pokedex_progress.add(progressTracker.dex_type, dex_progress);
            }
            root.add("pokedex_progress", pokedex_progress);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Writer writer = new FileWriter(player_data_file);
            gson.toJson(root, writer);
            writer.close();
        } catch (IOException e) {
            DexRewards.LOGGER.error("Error while updating player data file. UUID: {}, Username: {}.", playerData.uuid, playerData.username, e);
        }

        player_data.removeIf(data -> data.uuid.equals(playerData.uuid));
        player_data.add(playerData);
    }

    public static PlayerData getPlayerData(UUID uuid) {
        for (PlayerData data : player_data) {
            if (data.uuid.equals(uuid)) {
                return data;
            }
        }
        return null;
    }
}
