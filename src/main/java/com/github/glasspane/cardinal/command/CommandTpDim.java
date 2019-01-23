/*
 * Cardinal
 * Copyright (C) 2019-2019 GlassPane
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; If not, see <https://www.gnu.org/licenses>.
 */
package com.github.glasspane.cardinal.command;

import com.github.glasspane.cardinal.util.CardinalTeleportmanager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.arguments.*;
import net.minecraft.server.command.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.dimension.DimensionType;

public class CommandTpDim {

    public static void onCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(ServerCommandManager.literal("tp_dimension").requires(source -> source.hasPermissionLevel(3)).then(ServerCommandManager.argument("dimension", DimensionArgumentType.create()).executes(context -> {
            DimensionType dimensionType = DimensionArgumentType.getDimensionArgument(context, "dimension");
            return transferPlayerTo(context.getSource().getPlayer(), dimensionType);
        }).then(ServerCommandManager.argument("player", EntityArgumentType.onePlayer()).executes(context -> {
            DimensionType dimensionType = DimensionArgumentType.getDimensionArgument(context, "dimension");
            ServerPlayerEntity playerEntity = (ServerPlayerEntity) EntityArgumentType.method_9313(context, "player");
            return transferPlayerTo(playerEntity, dimensionType);
        }))));
    }

    private static int transferPlayerTo(ServerPlayerEntity player, DimensionType dimensionType) {
        CardinalTeleportmanager.teleportPlayer(player, dimensionType, false);
        return 1;
    }
}
