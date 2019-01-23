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
package com.github.glasspane.cardinal.mixin.common;

import com.github.glasspane.cardinal.Cardinal;
import net.minecraft.server.*;
import net.minecraft.server.world.*;
import net.minecraft.util.profiler.DisableableProfiler;
import net.minecraft.world.*;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.*;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import java.util.*;
import java.util.concurrent.ExecutorService;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Shadow
    @Final
    private ExecutorService field_17200; //TODO mapping
    @Shadow
    @Final
    private DisableableProfiler profiler;
    @Shadow
    @Final
    private Map<DimensionType, ServerWorld> worlds;
    private WorldSaveHandler saveHandler;
    private WorldGenerationProgressListener worldGenerationProgressListener;

    @Inject(method = "createWorlds", at = @At("RETURN"))
    private void registerDimensions(WorldSaveHandler saveHandler, PersistentStateManager persistentStateManager, LevelProperties levelProperties, LevelInfo levelInfo, WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        this.saveHandler = saveHandler;
        this.worldGenerationProgressListener = worldGenerationProgressListener;
        Cardinal.getLogger().info("unloading dimensions...");
        for(ServerWorld world : worlds.values()) {
            DimensionType type = world.dimension.getType();
            if(type.getRawId() != 0) { //TODO blacklist/whitelist -> config
                unloadWorld(type, world, false);
            }
        }
        RegionFileCache.clear();
    }

    private void unloadWorld(DimensionType type, ServerWorld world, boolean printLogMessage) {
        if(printLogMessage) {
            Cardinal.getLogger().info("unloading dimension {}", DimensionType.getId(type));
        }
        try {
            world.save(null, true); //boolean: flush to disk immediately
            world.close();
        }
        catch (SessionLockException e) {
            Cardinal.getLogger().error(e.getMessage());
        }
    }

    @Inject(method = "getWorld", at = @At("RETURN"), cancellable = true)
    private void getWorld(DimensionType dimensionType, CallbackInfoReturnable<ServerWorld> cir) {
        if(dimensionType != DimensionType.OVERWORLD && cir.getReturnValue() == null) {
            ServerWorld overWorld = worlds.get(DimensionType.OVERWORLD);
            Validate.notNull(overWorld, "Cardinal: Overworld not loaded!");
            Cardinal.getLogger().info("Loading dimension {}", DimensionType.getId(dimensionType));
            ServerWorld world = new SecondaryServerWorld((MinecraftServer) (Object) this, this.field_17200, saveHandler, dimensionType, overWorld, this.profiler, this.worldGenerationProgressListener).initializeAsSecondaryWorld();
            this.worlds.put(dimensionType, world);
            world.registerListener(new ServerWorldListener((MinecraftServer) (Object) this, world));
            if(!this.isSinglePlayer()) {
                world.getLevelProperties().setGameMode(this.getDefaultGameMode());
            }
            cir.setReturnValue(world);
        }
    }

    @Shadow
    public abstract boolean isSinglePlayer();

    @Shadow
    public abstract GameMode getDefaultGameMode();

    @Inject(method = "save", at = @At(value = "HEAD"))
    private void save(boolean suppressLogMessages, boolean flushToDisk, CallbackInfo ci) {
        Iterator<DimensionType> iterator = this.worlds.keySet().iterator();
        while(iterator.hasNext()) {
            DimensionType type = iterator.next();
            ServerWorld world = worlds.get(type);
            if(world == null) {
                iterator.remove();
                continue;
            }
            if(world instanceof SecondaryServerWorld && world.players.size() == 0) {
                unloadWorld(type, world, !suppressLogMessages);
                iterator.remove();
                RegionFileCache.clear();
            }
        }
    }

    @Inject(method = "shutdown", at = @At("RETURN"))
    private void shutDown(CallbackInfo ci) {
        RegionFileCache.clear(); //release the lock on unloaded region files
    }
}
