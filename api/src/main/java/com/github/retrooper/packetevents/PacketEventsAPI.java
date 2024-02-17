/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2022 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.retrooper.packetevents;

import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.netty.NettyManager;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.github.retrooper.packetevents.util.LogManager;
import com.github.retrooper.packetevents.util.PEVersion;
import com.github.retrooper.packetevents.util.updatechecker.UpdateChecker;

import java.util.logging.Logger;

public abstract class PacketEventsAPI<T> {
    private static final EventManager EVENT_MANAGER = new EventManager();
    private static final PacketEventsSettings SETTINGS = new PacketEventsSettings();
    private static final UpdateChecker UPDATE_CHECKER = new UpdateChecker();
    private final Logger LOGGER = Logger.getLogger(PacketEventsAPI.class.getName());
    private static final LogManager LOG_MANAGER = new LogManager();
    //TODO UPDATE
    private static final PEVersion VERSION = new PEVersion(2, 2, 1);
    
    public EventManager getEventManager() {
        return EVENT_MANAGER;
    }

    public PacketEventsSettings getSettings() {
        return SETTINGS;
    }

    public UpdateChecker getUpdateChecker() {
        return UPDATE_CHECKER;
    }

    public PEVersion getVersion() {
        return VERSION;
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public LogManager getLogManager() {
        return LOG_MANAGER;
    }

    public abstract void load();

    public abstract boolean isLoaded();

    public abstract void init();

    public abstract boolean isInitialized();

    public abstract void terminate();

    public abstract T getPlugin();

    public abstract ServerManager getServerManager();

    public abstract ProtocolManager getProtocolManager();

    public abstract PlayerManager getPlayerManager();

    public abstract NettyManager getNettyManager();

    public abstract ChannelInjector getInjector();
}
