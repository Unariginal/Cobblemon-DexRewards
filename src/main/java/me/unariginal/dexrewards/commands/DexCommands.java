package me.unariginal.dexrewards.commands;

import com.cobblemon.mod.common.api.pokedex.entry.DexEntries;
import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.labels.CobblemonPokemonLabels;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.config.*;
import me.unariginal.dexrewards.datatypes.DexType;
import me.unariginal.dexrewards.datatypes.PlayerData;
import me.unariginal.dexrewards.utils.TextUtils;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.Map;

public class DexCommands {
    public DexCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            LiteralCommandNode<ServerCommandSource> node = dispatcher.register(
                    CommandManager.literal("dex")
                            .then(
                                    CommandManager.literal("reload")
                                            .requires(Permissions.require("dexrewards.reload", 4))
                                            .executes(ctx -> {
                                                DexRewards.INSTANCE.reload();
                                                ctx.getSource().sendMessage(TextUtils.deserialize(TextUtils.parse(MessagesConfig.getMessage("reload_command"))));
                                                return 1;
                                            })
                            )
                            .then(
                                    CommandManager.literal("rewards")
                                            .executes(ctx -> {
                                                DexType dexType = DexTypesConfig.getDexType(Config.configData.defaultDexType);
                                                if (dexType == null) dexType = DexTypesConfig.dexTypes.getFirst();
                                                return rewards(ctx, dexType);
                                            })
                                            .then(
                                                    CommandManager.argument("dex-type", StringArgumentType.string())
                                                            .suggests((ctx, builder) -> {
                                                                for (DexType dexType : DexTypesConfig.dexTypes) {
                                                                    builder.suggest(dexType.name);
                                                                }
                                                                return builder.buildFuture();
                                                            })
                                                            .executes(ctx -> {
                                                                String dexTypeName = StringArgumentType.getString(ctx, "dex-type");
                                                                DexType dexType = DexTypesConfig.getDexType(dexTypeName);
                                                                if (dexType != null) {
                                                                    return rewards(ctx, dexType);
                                                                }
                                                                return 0;
                                                            })
                                            )
                            )
                            .then(
                                    CommandManager.literal("generate")
                                            .requires(Permissions.require("dexrewards.generate", 4))
                                            .executes(this::generate)
                            )
                            .then(
                                    CommandManager.literal("reset")
                                            .requires(Permissions.require("dexrewards.reset", 4))
                                            .then(
                                                    CommandManager.argument("player", EntityArgumentType.player())
                                                            .executes(this::reset)
                                            )

                            )
                            .then(
                                    CommandManager.literal("update")
                                            .requires(Permissions.require("dexrewards.update", 4))
                                            .then(
                                                    CommandManager.argument("player", EntityArgumentType.player())
                                                            .executes(this::update)
                                            )
                            )
            );
            dispatcher.register(CommandManager.literal("dexrewards").redirect(node));
        });
    }

    private int rewards(CommandContext<ServerCommandSource> ctx, DexType dexType) {
        if (ctx.getSource().isExecutedByPlayer()) {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player != null) {
                try {
                    RewardGUIConfig.guiLayout.createGui(player, 0, dexType);
                } catch (Exception e) {
                    DexRewards.LOGGER.error("[DexRewards] Error running rewards command.", e);
                }
            }
        }
        return 1;
    }

    private int reset(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        if (target != null) {
            PlayerData data = PlayerDataConfig.getPlayerData(target.getUuid());
            if (data != null) {
                try {
                    for (PlayerData.ProgressTracker progressTracker : data.pokedexProgress) {
                        progressTracker.progressCount = 0;
                        progressTracker.claimableRewards.clear();
                        progressTracker.claimedRewards.clear();
                    }

                    data.updateCaughtCount();
                    data.updateClaimableRewards();

                    PlayerDataConfig.updatePlayerData(data);
                    ctx.getSource().sendMessage(TextUtils.deserialize(TextUtils.parse(MessagesConfig.getMessage("reset_command_sender"), target)));
                    target.sendMessage(TextUtils.deserialize(TextUtils.parse(MessagesConfig.getMessage("reset_command_target"), target)));
                } catch (IOException e) {
                    DexRewards.LOGGER.error("[DexRewards] Failed to update player data for player \"{}\"", data.username, e);
                }
            }
        }
        return 1;
    }

    private int update(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        if (target != null) {
            PlayerData data = PlayerDataConfig.getPlayerData(target.getUuid());
            if (data != null) {
                try {
                    data.updateCaughtCount();
                    data.updateClaimableRewards();

                    PlayerDataConfig.updatePlayerData(data);
                    ctx.getSource().sendMessage(TextUtils.deserialize(TextUtils.parse(MessagesConfig.getMessage("update_command"), target)));
                } catch (IOException e) {
                    DexRewards.LOGGER.error("[DexRewards] Failed to update player data for player \"{}\"", data.username, e);
                }
            }
        }
        return 1;
    }

    private int generate(CommandContext<ServerCommandSource> ctx) {
        int unimplemented_total = 0;
        for (Map.Entry<Identifier, PokedexEntry> entry : DexEntries.INSTANCE.getEntries().entrySet()) {
            Species species = PokemonSpecies.INSTANCE.getByIdentifier(entry.getKey());
            if (species != null) {
                if (!species.getImplemented()) {
                    DexRewards.INSTANCE.logInfo("[DexRewards] Unimplemented: " + entry.getKey());
                    unimplemented_total++;

                    String generation = "none";
                    if (species.getLabels().contains(CobblemonPokemonLabels.GENERATION_1)) {
                        generation = "generation1";
                    } else if (species.getLabels().contains(CobblemonPokemonLabels.GENERATION_2)) {
                        generation = "generation2";
                    } else if (species.getLabels().contains(CobblemonPokemonLabels.GENERATION_3)) {
                        generation = "generation3";
                    } else if (species.getLabels().contains(CobblemonPokemonLabels.GENERATION_4)) {
                        generation = "generation4";
                    } else if (species.getLabels().contains(CobblemonPokemonLabels.GENERATION_5)) {
                        generation = "generation5";
                    } else if (species.getLabels().contains(CobblemonPokemonLabels.GENERATION_6)) {
                        generation = "generation6";
                    } else if (species.getLabels().contains(CobblemonPokemonLabels.GENERATION_7) || species.getLabels().contains(CobblemonPokemonLabels.GENERATION_7B)) {
                        generation = "generation7";
                    } else if (species.getLabels().contains(CobblemonPokemonLabels.GENERATION_8) || species.getLabels().contains(CobblemonPokemonLabels.GENERATION_8A)) {
                        generation = "generation8";
                    } else if (species.getLabels().contains(CobblemonPokemonLabels.GENERATION_9)) {
                        generation = "generation9";
                    }
                    Config.generateImplementation(entry.getKey().toString(), generation);
                }
            }
        }
        DexRewards.INSTANCE.logInfo("[DexRewards] Total Unimplemented: " + unimplemented_total);

        return 1;
    }
}
