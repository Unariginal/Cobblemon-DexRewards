package me.unariginal.dexrewards;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.platform.events.PlatformEvents;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import kotlin.Unit;
import me.unariginal.dexrewards.commands.DexCommands;
import me.unariginal.dexrewards.config.*;
import me.unariginal.dexrewards.datatypes.CustomPokedexValueCalculators;
import me.unariginal.dexrewards.datatypes.DexType;
import me.unariginal.dexrewards.datatypes.PlayerData;
import me.unariginal.dexrewards.datatypes.rewards.RewardGroup;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class DexRewards implements ModInitializer {
    public final static String MOD_ID = "dexrewards";
    public final static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static DexRewards INSTANCE;
    public static boolean DEBUG = false;

    private FabricServerAudiences audience;
    private MinecraftServer server;

    public Map<DexType, Integer> dexTypeTotals = new HashMap<>();

    @Override
    public void onInitialize() {
        INSTANCE = this;

        new DexCommands();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            this.audience = FabricServerAudiences.of(server);

            reload();
        });

        PlatformEvents.SERVER_PLAYER_LOGIN.subscribe(Priority.LOW, event -> {
            ServerPlayerEntity player = event.getPlayer();
            try {
                PlayerDataConfig.loadPlayerData(player);
                PlayerData playerData = PlayerDataConfig.getPlayerData(player.getUuid());
                if (playerData != null) {
                    playerData.updateCaughtCount();
                    playerData.updateClaimableRewards();
                    PlayerDataConfig.updatePlayerData(playerData);
                }
            } catch (IOException e) {
                LOGGER.error("[DexRewards] Failed to load player data for player \"{}\".", player.getNameForScoreboard(), e);
            }
        });

        CobblemonEvents.POKEMON_GAINED.subscribe(Priority.HIGHEST, event -> {
            try {
                UUID playerUuid = event.getPlayerId();

                PlayerData playerData = PlayerDataConfig.getPlayerData(playerUuid);

                if (playerData != null) {
                    playerData.updateCaughtCount();
                    playerData.updateClaimableRewards();
                    PlayerDataConfig.updatePlayerData(playerData);
                }
            } catch (IOException e) {
                LOGGER.error("[DexRewards] Failed to update player data for player \"{}\"", event.getPlayerId(), e);
            }

            return Unit.INSTANCE;
        });
    }

    public void reload() {
        try {
            dexTypeTotals.clear();
            for (DexType dexType : DexTypesConfig.dexTypes) {
                Placeholders.remove(Identifier.of(dexType.name, "total"));
                Placeholders.remove(Identifier.of(dexType.name, "total_groups"));
                Placeholders.remove(Identifier.of("player", dexType.name + ".count"));
                Placeholders.remove(Identifier.of("player", dexType.name + ".percent"));
                Placeholders.remove(Identifier.of("player", dexType.name + ".latest_reward"));
            }

            Config.load();
            MessagesConfig.load();
            DexTypesConfig.load();
            RewardGUIConfig.load();

            List<PlayerData> newData = new ArrayList<>(PlayerDataConfig.playerData);

            for (PlayerData data : newData) {
                try {
                    data.updateCaughtCount();
                    data.updateClaimableRewards();
                    PlayerDataConfig.updatePlayerData(data);
                } catch (IOException e) {
                    LOGGER.error("[DexRewards] Failed to update player data for player \"{}\"", data.username, e);
                }
            }

            PlayerDataConfig.playerData = newData;

            loadPlaceholders();
        } catch (IOException e) {
            LOGGER.error("[DexRewards] Failed to load config files.", e);
        }
    }

    public void loadPlaceholders() {
        for (DexType dexType : DexTypesConfig.dexTypes) {
            int dexTotal = CustomPokedexValueCalculators.getDexSize(dexType);
            dexTypeTotals.put(dexType, dexTotal);

            // %national:total% -> 1025
            Placeholders.register(
                    Identifier.of(dexType.name, "total"),
                    (ctx, arg) -> PlaceholderResult.value(String.valueOf(dexTotal))
            );
            // %national:total_groups% -> 20
            Placeholders.register(
                    Identifier.of(dexType.name, "total_groups"),
                    (ctx, arg) -> PlaceholderResult.value(String.valueOf(dexType.rewardGroups.size()))
            );
            // %player:national.count% -> 342
            Placeholders.register(
                    Identifier.of("player", dexType.name + ".count"),
                    (ctx, arg) -> {
                        if (!ctx.hasPlayer()) {
                            return PlaceholderResult.invalid("No Player!");
                        }

                        ServerPlayerEntity player = ctx.player();
                        if (player != null) {
                            PlayerData playerData = PlayerDataConfig.getPlayerData(player.getUuid());
                            if (playerData != null) {
                                PlayerData.ProgressTracker progressTracker = playerData.getProgress(dexType.name);
                                if (progressTracker != null) {
                                    int count = progressTracker.progressCount;
                                    return PlaceholderResult.value(String.valueOf(count));
                                } else {
                                    return PlaceholderResult.invalid("No Valid Pokedex Progress!");
                                }
                            } else {
                                return PlaceholderResult.invalid("No Player Data!");
                            }
                        } else {
                            return PlaceholderResult.invalid("No Player!");
                        }
                    }
            );
            // %player:national.percent% -> 47.32
            Placeholders.register(
                    Identifier.of("player", dexType.name + ".percent"),
                    (ctx, arg) -> {
                        if (!ctx.hasPlayer()) {
                            return PlaceholderResult.invalid("No Player!");
                        }

                        ServerPlayerEntity player = ctx.player();
                        if (player != null) {
                            PlayerData playerData = PlayerDataConfig.getPlayerData(player.getUuid());
                            if (playerData != null) {
                                PlayerData.ProgressTracker progressTracker = playerData.getProgress(dexType.name);
                                if (progressTracker != null) {
                                    int count = progressTracker.progressCount;
                                    Integer total = dexTypeTotals.get(dexType);
                                    if (total == null) return PlaceholderResult.invalid("Null dex type total!");
                                    return PlaceholderResult.value(new DecimalFormat("#.##").format(((double) count / total) * 100));
                                } else {
                                    return PlaceholderResult.invalid("No Valid Pokedex Progress!");
                                }
                            } else {
                                return PlaceholderResult.invalid("No Player Data!");
                            }
                        } else {
                            return PlaceholderResult.invalid("No Player!");
                        }
                    }
            );
            // %player:national.latest_reward% -> 47.32
            Placeholders.register(
                    Identifier.of("player", dexType.name + ".latest_reward"),
                    (ctx, arg) -> {
                        if (!ctx.hasPlayer()) {
                            return PlaceholderResult.invalid("No Player!");
                        }

                        ServerPlayerEntity player = ctx.player();
                        if (player != null) {
                            PlayerData playerData = PlayerDataConfig.getPlayerData(player.getUuid());
                            if (playerData != null) {
                                PlayerData.ProgressTracker progressTracker = playerData.getProgress(dexType.name);
                                if (progressTracker != null) {
                                    String group = "None";
                                    for (RewardGroup rewardGroup : dexType.rewardGroups) {
                                        if (progressTracker.claimedRewards.contains(rewardGroup.name)) {
                                            group = rewardGroup.displayName;
                                        }
                                    }
                                    return PlaceholderResult.value(group);
                                } else {
                                    return PlaceholderResult.invalid("No Valid Pokedex Progress!");
                                }
                            } else {
                                return PlaceholderResult.invalid("No Player Data!");
                            }
                        } else {
                            return PlaceholderResult.invalid("No Player!");
                        }
                    }
            );
        }
    }

    public FabricServerAudiences audience() {
        return audience;
    }

    public MinecraftServer server() {
        return server;
    }

    public void logInfo(String message) {
        if (DEBUG) {
            LOGGER.info(message);
        }
    }
}
