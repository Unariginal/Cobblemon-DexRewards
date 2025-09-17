package me.unariginal.dexrewards.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import me.unariginal.dexrewards.utils.ConfigUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.util.Map;

public class MessagesConfig {
    public static MessagesData messages;

    public static class MessagesData {
        public String prefix;
        public Map<String, String> messages;

        public MessagesData(String prefix, Map<String, String> messages) {
            this.prefix = prefix;
            this.messages = messages;
        }
    }

    public static void load() throws IOException {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards").toFile();
        if (!rootFolder.exists()) rootFolder.mkdirs();

        File configFile = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/messages.json").toFile();
        String json = "{}";
        if (!configFile.exists()) ConfigUtils.create(configFile, "/dr_config/messages.json");
        if (configFile.exists()) json = JsonParser.parseReader(new FileReader(configFile)).toString();

        messages = gson.fromJson(json, MessagesData.class);
    }

    public static String getMessage(String key) {
        if (messages.messages.containsKey(key)) return messages.messages.get(key);
        return key;
    }
}
