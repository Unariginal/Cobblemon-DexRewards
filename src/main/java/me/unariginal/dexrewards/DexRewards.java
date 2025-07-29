package me.unariginal.dexrewards;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokedex.entry.DexEntries;
import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import kotlin.Unit;
import me.unariginal.dexrewards.commands.DexCommands;
import me.unariginal.dexrewards.config.*;
import me.unariginal.dexrewards.datatypes.PlayerData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DexRewards implements ModInitializer {
    public final static String MODID = "dexrewards";
    public final static Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static DexRewards INSTANCE;
    public static boolean DEBUG = false;

    private FabricServerAudiences audience;
    private MinecraftServer server;
    private Config config;
    private MessagesConfig messages;
    private RewardGUIConfig rewardGUIConfig;
    private DexTypesConfig dexTypes;

    public static int DEX_TOTAL = DexEntries.INSTANCE.getEntries().size();
    public static List<Identifier> VALID_DEX_IDS = new ArrayList<>();

    @Override
    public void onInitialize() {
        INSTANCE = this;

        new DexCommands();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            this.audience = FabricServerAudiences.of(server);

            reload();

//            Placeholders.register(Identifier.of("pokedex", "total"), (ctx, arg) -> PlaceholderResult.value(String.valueOf(DEX_TOTAL)));
//
//            Placeholders.register(Identifier.of("player", "caught_count"), (ctx, arg) -> {
//                if (!ctx.hasPlayer())
//                    return PlaceholderResult.invalid("No Player!");
//
//                ServerPlayerEntity player = ctx.player();
//                if (player != null)
//                    return PlaceholderResult.value(String.valueOf(config.getPlayerData(player.getUuid()).caught_count));
//                else
//                    return PlaceholderResult.invalid("No Player!");
//            });
//
//            Placeholders.register(Identifier.of("player", "rank"), (ctx, arg) -> {
//                if (!ctx.hasPlayer()) {
//                    return PlaceholderResult.invalid("No Player!");
//                }
//
//                ServerPlayerEntity player = ctx.player();
//                if (player != null) {
//                    PlayerData playerData = config.getPlayerData(player.getUuid());
//                    if (playerData != null) {
//                        String rank = "None";
//                        for (RewardGroup group : DexRewards.INSTANCE.config().reward_groups) {
//                            if (playerData.claimed_rewards.contains(group.name)) {
//                                rank = group.name;
//                            }
//                        }
//
//                        return PlaceholderResult.value(rank);
//                    } else {
//                        return PlaceholderResult.invalid("No Player Data!");
//                    }
//                } else {
//                    return PlaceholderResult.invalid("No Player!");
//                }
//            });
//
//            Placeholders.register(Identifier.of("player", "caught_percent"), (ctx, arg) -> {
//                if (!ctx.hasPlayer()) {
//                    return PlaceholderResult.invalid("No Player!");
//                }
//
//                ServerPlayerEntity player = ctx.player();
//                if (player != null) {
//                    PlayerData playerData = config.getPlayerData(player.getUuid());
//                    if (playerData != null) {
//                        String percent = new DecimalFormat("#.##").format(((double) playerData.caught_count / DexRewards.DEX_TOTAL) * 100);
//                        return PlaceholderResult.value(percent);
//                    } else {
//                        return PlaceholderResult.invalid("No Player Data!");
//                    }
//                } else {
//                    return PlaceholderResult.invalid("No Player!");
//                }
//            });
//
//            Placeholders.register(Identifier.of("pokedex", "total_reward_groups"), (ctx, arg) -> PlaceholderResult.value(String.valueOf(config.reward_groups.size())));
        });

        ServerPlayConnectionEvents.JOIN.register((serverPlayNetworkHandler, packetSender, server) -> {
            ServerPlayerEntity player = serverPlayNetworkHandler.getPlayer();
            if (player != null) {
                PlayerData playerData = PlayerDataConfig.getPlayerData(player.getUuid());

                if (playerData == null)
                    PlayerDataConfig.updatePlayerData(new PlayerData(player.getUuid(), player.getNameForScoreboard(), List.of()));

                playerData = PlayerDataConfig.getPlayerData(player.getUuid());

                if (playerData != null) {
                    playerData.updateCaughtCount();
                    playerData.updateClaimableRewards();

//                    if (!playerData.claimable_rewards.isEmpty()) {
//                        player.sendMessage(TextUtils.deserialize(Messages.parse(Messages.rewards_to_claim)));
//                    }

                    PlayerDataConfig.updatePlayerData(playerData);
                }
            }
        });

        CobblemonEvents.POKEMON_GAINED.subscribe(Priority.HIGHEST, event -> {
            UUID player_uuid = event.getPlayerId();

            PlayerData playerData = PlayerDataConfig.getPlayerData(player_uuid);

            if (playerData != null) {
                playerData.updateCaughtCount();
                playerData.updateClaimableRewards();
                PlayerDataConfig.updatePlayerData(playerData);
            }

            return Unit.INSTANCE;
        });
    }

    public void reload() {
        config = new Config();
        messages = new MessagesConfig();
        rewardGUIConfig = new RewardGUIConfig();
        dexTypes = new DexTypesConfig();

        int total = 0;
        int invalid_total = 0;
        int unimplemented_total = 0;

        List<Identifier> valid_identifiers = new ArrayList<>();

        for (Map.Entry<Identifier, PokedexEntry> entry : DexEntries.INSTANCE.getEntries().entrySet()) {
            Species species = PokemonSpecies.INSTANCE.getByIdentifier(entry.getKey());
            if (species != null) {
                if (!config.implemented_only || species.getImplemented()) {
                    if (!config.species_blacklist.contains(species)) {
                        boolean valid = true;
                        for (String label : species.getLabels()) {
                            if (config.label_blacklist.contains(label)) {
                                valid = false;
                            }
                            if (config.generation_blacklist.contains(label)) {
                                valid = false;
                            }
                        }

                        if (valid) {
                            total++;
                            valid_identifiers.add(entry.getKey());
                        } else {
                            invalid_total++;
                        }
                    } else {
                        invalid_total++;
                    }
                }

                if (!species.getImplemented()) {
                    unimplemented_total++;
                }
            } else {
                if (config.allow_invalid_species) {
                    total++;
                }
                invalid_total++;
            }
        }
        DEX_TOTAL = total;

        logInfo("[DexRewards] Total Valid Pokedex Entries: " + DEX_TOTAL);
        logInfo("[DexRewards] Total Invalid Entries: " + invalid_total);
        logInfo("[DexRewards] Total Unimplemented: " + unimplemented_total);
        logInfo("[DexRewards] Total Pokedex Entries: " + (DEX_TOTAL + invalid_total + unimplemented_total));

        VALID_DEX_IDS = valid_identifiers;

        List<PlayerData> new_data = new ArrayList<>(PlayerDataConfig.player_data);

        for (PlayerData data : new_data) {
            data.updateCaughtCount();
            data.updateClaimableRewards();
            PlayerDataConfig.updatePlayerData(data);
        }
    }

    public FabricServerAudiences audience() {
        return audience;
    }

    public MinecraftServer server() {
        return server;
    }

    public Config config() {
        return config;
    }

    public DexTypesConfig dexTypes() {
        return dexTypes;
    }

    public RewardGUIConfig rewardGUIConfig() {
        return rewardGUIConfig;
    }

    public void logInfo(String message) {
        if (DEBUG) {
            LOGGER.info(message);
        }
    }
}
