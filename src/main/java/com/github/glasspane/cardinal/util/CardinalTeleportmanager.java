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
package com.github.glasspane.cardinal.util;

import net.minecraft.client.network.packet.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.dimension.DimensionType;

public class CardinalTeleportmanager {

    private static final int MAX_WORLD_SIZE = 29999872;

    //TODO mappings
    public static void teleportPlayer(ServerPlayerEntity player, DimensionType destination, boolean shouldSpawnPortal) {
        MinecraftServer server = player.getServer();
        //noinspection ConstantConditions
        PlayerManager playerManager = server.getPlayerManager();
        ServerWorld fromWorld = (ServerWorld) player.world;
        ServerWorld toWorld = server.getWorld(destination);
        player.dimension = destination;
        player.networkHandler.sendPacket(new PlayerRespawnClientPacket(player.dimension, player.world.getDifficulty(), player.world.getLevelProperties().getGeneratorType(), player.interactionManager.getGameMode())); //client needs some extra information
        playerManager.method_14576(player); //update player command statuses
        fromWorld.method_8507(player); //invalidateEntity
        player.invalid = false;
        //teleport
        fromWorld.getProfiler().push("moving");
        {
            player.setPositionAndAngles(player.x, player.y, player.z, player.yaw, player.pitch);
            if(player.isValid()) {
                fromWorld.method_8553(player, false); //update entity?
            }
        }
        fromWorld.getProfiler().pop();
        toWorld.getProfiler().push("placing");
        {
            if(player.isValid()) {
                double x = MathHelper.clamp(player.x, -MAX_WORLD_SIZE, MAX_WORLD_SIZE);
                double z = MathHelper.clamp(player.z, -MAX_WORLD_SIZE, MAX_WORLD_SIZE);
                player.setPositionAndAngles(x, player.y, z, player.yaw, player.pitch);
                player.networkHandler.teleportRequest(player.x, player.y, player.z, player.yaw, player.pitch);
                player.networkHandler.syncWithPlayerPosition();
                toWorld.spawnEntity(player);
                toWorld.method_8553(player, false); //update entity?
            }
        }
        toWorld.getProfiler().pop();
        player.setWorld(toWorld);
        playerManager.method_14612(player, fromWorld); //notifyDimensionChange
        player.networkHandler.teleportRequest(player.x, player.y, player.z, player.yaw, player.pitch);
        player.interactionManager.setWorld(toWorld);
        player.networkHandler.sendPacket(new PlayerAbilitiesClientPacket(player.abilities)); //update the client
        playerManager.method_14606(player, toWorld); //update world flags on the client (worldborder, weather, etc)
        playerManager.method_14594(player); //update player equipment
        for(StatusEffectInstance effect : player.getPotionEffects()) { //update potion effects on the client
            player.networkHandler.sendPacket(new EntityPotionEffectClientPacket(player.getEntityId(), effect));
        }
        //this triggers the vanilla portal teleport sound
        //player.networkHandler.sendPacket(new WorldEventClientPacket(1032, BlockPos.ORIGIN, 0, false));
    }
}
