package me.unariginal.dexrewards.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.datatypes.Messages;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;

public class MessagesConfig {
    public MessagesConfig() {
        try {
            loadMessages();
        } catch (IOException e) {
            DexRewards.LOGGER.error("Failed to load messages config!", e);
        }
    }

    private void loadMessages() throws IOException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards").toFile();
        if (!rootFolder.exists())
            rootFolder.mkdirs();

        File messagesFile = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/messages.json").toFile();
        JsonObject newRoot = new JsonObject();
        JsonObject root = new JsonObject();
        if (messagesFile.exists())
            root = JsonParser.parseReader(new FileReader(messagesFile)).getAsJsonObject();

        if (root.has("prefix"))
            Messages.prefix = root.get("prefix").getAsString();
        newRoot.addProperty("prefix", Messages.prefix);

        JsonObject messages = new JsonObject();
        if (root.has("messages"))
            messages = root.get("messages").getAsJsonObject();

        if (messages.has("rewards_to_claim"))
            Messages.rewards_to_claim = messages.get("rewards_to_claim").getAsString();
        messages.addProperty("rewards_to_claim", Messages.rewards_to_claim);

        if (messages.has("reward_claimable"))
            Messages.reward_claimable = messages.get("reward_claimable").getAsString();
        messages.addProperty("reward_claimable", Messages.reward_claimable);

        if (messages.has("rewards_claimed"))
            Messages.rewards_claimed = messages.get("rewards_claimed").getAsString();
        messages.addProperty("rewards_claimed", Messages.rewards_claimed);

        if (messages.has("update_command"))
            Messages.update_command = messages.get("update_command").getAsString();
        messages.addProperty("update_command", Messages.update_command);

        if (messages.has("reset_command_sender"))
            Messages.reset_sender = messages.get("reset_command_sender").getAsString();
        messages.addProperty("reset_command_sender", Messages.reset_sender);

        if (messages.has("reset_command_target"))
            Messages.reset_target = messages.get("reset_command_target").getAsString();
        messages.addProperty("reset_command_target", Messages.reset_target);

        if (messages.has("reload_command"))
            Messages.reload = messages.get("reload_command").getAsString();
        messages.addProperty("reload_command", Messages.reload);
        newRoot.add("messages", messages);

        messagesFile.delete();
        messagesFile.createNewFile();

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(messagesFile);
        gson.toJson(newRoot, writer);
        writer.close();
    }
}
