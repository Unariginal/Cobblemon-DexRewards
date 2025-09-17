package me.unariginal.dexrewards;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import kotlin.Unit;
import me.unariginal.dexrewards.commands.DexCommands;
import me.unariginal.dexrewards.config.*;
import me.unariginal.dexrewards.datatypes.CustomPokedexValueCalculators;
import me.unariginal.dexrewards.datatypes.DexType;
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

import java.io.IOException;
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

            for (DexType dexType : DexTypesConfig.dexTypes) {
                int dexTotal = CustomPokedexValueCalculators.getDexSize(dexType);
                dexTypeTotals.put(dexType, dexTotal);

                Placeholders.register(
                        Identifier.of(dexType.name, "total"),
                        (ctx, arg) -> PlaceholderResult.value(String.valueOf(dexTotal))
                );
            }
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
                try {
                    PlayerData playerData = PlayerDataConfig.loadPlayerData(player);
                    playerData.updateCaughtCount();
                    playerData.updateClaimableRewards();
                    PlayerDataConfig.updatePlayerData(playerData);
                } catch (IOException e) {
                    LOGGER.error("[DexRewards] Failed to load player data for player \"{}\".", player.getNameForScoreboard(), e);
                }
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
            Config.load();
            MessagesConfig.load();
            DexTypesConfig.load();
            RewardGUIConfig.load();
        } catch (IOException e) {
            LOGGER.error("[DexRewards] Failed to load config files.", e);
        }

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
