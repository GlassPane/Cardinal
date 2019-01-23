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
package com.github.glasspane.cardinal;

import com.github.glasspane.cardinal.command.CommandTpDim;
import com.github.glasspane.mesh.util.CalledByReflection;
import com.github.glasspane.mesh.util.logging.PrefixMessageFactory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.commands.CommandRegistry;
import org.apache.logging.log4j.*;

@CalledByReflection
public class Cardinal implements ModInitializer {

    public static final String MODID = "cardinal";
    public static final String MOD_NAME = "Cardinal";
    public static final String VERSION = "${version}";

    private static final Logger log = LogManager.getLogger(MODID, new PrefixMessageFactory(MOD_NAME));

    public static Logger getLogger() {
        return log;
    }

    @Override
    public void onInitialize() {
        log.info("Loading Cardinal System v{}", VERSION);
        CommandRegistry.INSTANCE.register(false, CommandTpDim::onCommand);
    }
}
