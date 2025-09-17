package me.unariginal.dexrewards.mixin;

import com.cobblemon.mod.common.command.PokedexCommand;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.config.PlayerDataConfig;
import me.unariginal.dexrewards.datatypes.PlayerData;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.List;

@Mixin(value = PokedexCommand.class, remap = false)
public class PokedexCommandMixin {
    @Unique
    private void updateDataForPlayers(List<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            PlayerData data = PlayerDataConfig.getPlayerData(player.getUuid());
            if (data != null) {
                try {
                    data.updateCaughtCount();
                    data.updateClaimableRewards();
                    PlayerDataConfig.updatePlayerData(data);
                } catch (IOException e) {
                    DexRewards.LOGGER.error("[DexRewards] Failed to update player data for player \"{}\"", player.getNameForScoreboard(), e);
                }
            }
        }
    }

    @Inject(method = "executeGrantOnly", at = @At("TAIL"))
    private void updateOnGrantOnly(CommandContext<ServerCommandSource> ctx, CallbackInfoReturnable<Integer> cir, @Local List<ServerPlayerEntity> players) {
        updateDataForPlayers(players);
    }

    @Inject(method = "executeRemoveOnly", at = @At("TAIL"))
    private void updateOnRemoveOnly(CommandContext<ServerCommandSource> ctx, CallbackInfoReturnable<Integer> cir, @Local List<ServerPlayerEntity> players) {
        updateDataForPlayers(players);
    }

    @Inject(method = "executeGrantAll", at = @At("TAIL"))
    private void updateOnGrantAll(CommandContext<ServerCommandSource> ctx, CallbackInfoReturnable<Integer> cir, @Local List<ServerPlayerEntity> players) {
        updateDataForPlayers(players);
    }

    @Inject(method = "executeRemoveAll", at = @At("TAIL"))
    private void updateOnRemoveAll(CommandContext<ServerCommandSource> ctx, CallbackInfoReturnable<Integer> cir, @Local List<ServerPlayerEntity> players) {
        updateDataForPlayers(players);
    }
}
