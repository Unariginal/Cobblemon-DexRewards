package me.unariginal.dexrewards.config;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.datatypes.guielements.GuiElement;
import me.unariginal.dexrewards.datatypes.guielements.GuiLayout;
import me.unariginal.dexrewards.utils.MiscUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.ItemStack;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RewardGUIConfig {
    public static List<GuiElement> gui_elements = new ArrayList<>();
    public static GuiLayout gui_layout;

    public RewardGUIConfig() {
        try {
            loadGui();
        } catch (IOException e) {
            DexRewards.LOGGER.error("Failed to load reward gui config!", e);
        }
    }

    private void loadGui() throws IOException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("DexRewards").toFile();
        if (!rootFolder.exists())
            rootFolder.mkdirs();

        File rewardGuiFile = FabricLoader.getInstance().getConfigDir().resolve("DexRewards/reward_gui.json").toFile();
        JsonObject root = new JsonObject();
        if (rewardGuiFile.exists())
            root = JsonParser.parseReader(new FileReader(rewardGuiFile)).getAsJsonObject();

        JsonObject newRoot = guiMovementCompatMethod(root);

        rewardGuiFile.delete();
        rewardGuiFile.createNewFile();

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Writer writer = new FileWriter(rewardGuiFile);
        gson.toJson(newRoot, writer);
        writer.close();
    }

    public static JsonObject guiMovementCompatMethod(JsonObject root) {
        JsonObject newRoot = new JsonObject();
        String title = "<gold><bold>%reward_name% Rewards";
        if (root.has("title"))
            title = root.get("title").getAsString();
        newRoot.addProperty("title", title);

        int size = 6;
        if (root.has("size"))
            size = root.get("size").getAsInt();
        newRoot.addProperty("size", size);

        List<String> page_layout = new ArrayList<>(
                List.of(
                        "#########",
                        "####I####",
                        "#GGGGGGG#",
                        "#GGGGGGG#",
                        "#GGGGGGG#",
                        "P#######N"
                )
        );
        if (root.has("page_layout"))
            page_layout = root.get("page_layout").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        JsonArray layout = new JsonArray();
        for (String line : page_layout) {
            layout.add(line);
        }
        newRoot.add("page_layout", layout);

        JsonObject page_layout_symbols = new JsonObject();
        if (root.has("page_layout_symbols"))
            page_layout_symbols = root.get("page_layout_symbols").getAsJsonObject();

        String background_symbol = "#";
        if (page_layout_symbols.has("background"))
            background_symbol = page_layout_symbols.get("background").getAsString();
        page_layout_symbols.addProperty("background", background_symbol);

        String player_info_symbol = "I";
        if (page_layout_symbols.has("player_info"))
            player_info_symbol = page_layout_symbols.get("player_info").getAsString();
        page_layout_symbols.addProperty("player_info", player_info_symbol);

        String group_symbol = "G";
        if (page_layout_symbols.has("group"))
            group_symbol = page_layout_symbols.get("group").getAsString();
        page_layout_symbols.addProperty("group", group_symbol);

        String previous_page_symbol = "P";
        if (page_layout_symbols.has("previous_page"))
            previous_page_symbol = page_layout_symbols.get("previous_page").getAsString();
        page_layout_symbols.addProperty("previous_page", previous_page_symbol);

        String next_page_symbol = "N";
        if (page_layout_symbols.has("next_page"))
            next_page_symbol = page_layout_symbols.get("next_page").getAsString();
        page_layout_symbols.addProperty("next_page", next_page_symbol);
        newRoot.add("page_layout_symbols", page_layout_symbols);


        JsonObject background = new JsonObject();
        if (root.has("background"))
            background = root.get("background").getAsJsonObject();

        String background_item_id = "minecraft:gray_stained_glass_pane";
        if (background.has("item"))
            background_item_id = background.get("item").getAsString();
        background.addProperty("item", background_item_id);

        String background_name = "";
        if (background.has("name"))
            background_name = background.get("name").getAsString();
        background.addProperty("name", background_name);

        List<String> background_lore = new ArrayList<>();
        if (background.has("lore"))
            background_lore = background.get("lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        JsonArray background_lore_array = new JsonArray();
        for (String line : background_lore) {
            background_lore_array.add(line);
        }
        background.add("lore", background_lore_array);

        ComponentChanges background_item_data = ComponentChanges.EMPTY;
        JsonElement item_data = new JsonObject();
        if (background.has("item_data")) {
            item_data = background.get("item_data");
            if (item_data != null) {
                background_item_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, item_data).getOrThrow().getFirst();
            }
        }
        background.add("item_data", item_data);

        ItemStack background_item = MiscUtils.generateItem(background_item_id, background_name, background_lore, background_item_data);

        newRoot.add("background", background);

        String[] elements = {
                "player_info",
                "claimed_group",
                "claimable_group",
                "locked_group"
        };

        for (String element : elements) {
            JsonObject element_object = new JsonObject();
            if (root.has(element))
                element_object = root.get(element).getAsJsonObject();

            String name = switch (element) {
                case "player_info" -> "<light_purple>%reward_name% Progress Info";
                case "claimed_group" -> "<green><bold>%group.name% (%group.percent%%)";
                case "claimable_group" -> "<gold><bold>%group.name% (%group.percent%%)";
                case "locked_group" -> "<red><bold>%group.name% (%group.percent%%";
                default -> "";
            };
            if (element_object.has("name"))
                name = element_object.get("name").getAsString();
            element_object.addProperty("name", name);

            List<String> lore = switch (element) {
                case "player_info" -> List.of(
                        "<gray>Latest Reward: %player.reward_group%",
                        "<gray>Progress: %player.dex_count%/%pokedex.total% (%player.dex_percent%%)"
                );
                case "claimed_group" -> List.of(
                        "<blue>Already Claimed!"
                );
                case "claimable_group" -> List.of(
                        "<green>Claimable!"
                );
                case "locked_group" -> List.of(
                        "<red>Not Unlocked!"
                );
                default -> List.of();
            };

            if (element_object.has("lore"))
                lore = element_object.get("lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
            JsonArray lore_array = new JsonArray();
            for (String line : lore) {
                lore_array.add(line);
            }
            element_object.add("lore", lore_array);

            boolean glint = element.equals("claimable_group") || element.equals("player_info");
            if (element_object.has("show_enchantment_glint"))
                glint = element_object.get("show_enchantment_glint").getAsBoolean();
            element_object.addProperty("show_enchantment_glint", glint);

            newRoot.add(element, element_object);

            gui_elements.add(new GuiElement(element, name, lore, glint));
        }

        JsonObject navigation = new JsonObject();
        if (root.has("navigation"))
            navigation = root.get("navigation").getAsJsonObject();

        String previous_item_id = "minecraft:arrow";
        if (navigation.has("previous_item"))
            previous_item_id = navigation.get("previous_item").getAsString();
        navigation.addProperty("previous_item", previous_item_id);

        String previous_name = "<gray>Previous";
        if (navigation.has("previous_name"))
            previous_name = navigation.get("previous_name").getAsString();
        navigation.addProperty("previous_name", previous_name);

        List<String> previous_lore = new ArrayList<>();
        if (navigation.has("previous_lore"))
            previous_lore = navigation.get("previous_lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        JsonArray previous_lore_array = new JsonArray();
        for (String line : previous_lore) {
            previous_lore_array.add(line);
        }
        navigation.add("previous_lore", previous_lore_array);

        ComponentChanges previous_item_data = ComponentChanges.EMPTY;
        item_data = new JsonObject();
        if (navigation.has("previous_item_data")) {
            item_data = background.get("item_data");
            if (item_data != null) {
                previous_item_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, item_data).getOrThrow().getFirst();
            }
        }
        navigation.add("previous_item_data", item_data);

        ItemStack previous_item = MiscUtils.generateItem(previous_item_id, previous_name, previous_lore, previous_item_data);

        String next_item_id = "minecraft:arrow";
        if (navigation.has("next_item"))
            next_item_id = navigation.get("next_item").getAsString();
        navigation.addProperty("next_item", next_item_id);

        String next_name = "<gray>Next";
        if (navigation.has("next_name"))
            next_name = navigation.get("next_name").getAsString();
        navigation.addProperty("next_name", next_name);

        List<String> next_lore = new ArrayList<>();
        if (navigation.has("next_lore"))
            next_lore = navigation.get("next_lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        JsonArray next_lore_array = new JsonArray();
        for (String line : next_lore) {
            next_lore_array.add(line);
        }
        navigation.add("next_lore", next_lore_array);

        ComponentChanges next_item_data = ComponentChanges.EMPTY;
        item_data = new JsonObject();
        if (navigation.has("next_item_data")) {
            item_data = background.get("item_data");
            if (item_data != null) {
                next_item_data = ComponentChanges.CODEC.decode(JsonOps.INSTANCE, item_data).getOrThrow().getFirst();
            }
        }
        navigation.add("next_item_data", item_data);

        ItemStack next_item = MiscUtils.generateItem(next_item_id, next_name, next_lore, next_item_data);

        gui_layout = new GuiLayout(title, size, page_layout, background_symbol, player_info_symbol, group_symbol, previous_page_symbol, next_page_symbol, background_item, previous_item, next_item);

        newRoot.add("navigation", navigation);

        return newRoot;
    }
}
