/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2021 retrooper and contributors
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

package io.github.retrooper.packetevents.manager.player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.reflection.NestedClassUtil;
import com.github.retrooper.packetevents.util.reflection.Reflection;
import com.github.retrooper.packetevents.util.reflection.ReflectionObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import io.github.retrooper.packetevents.util.PlayerPingAccessorModern;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import io.github.retrooper.packetevents.util.SpigotVersionLookup_1_7;
import io.github.retrooper.packetevents.util.protocolsupport.ProtocolSupportUtil;
import io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class PlayerManagerImpl implements PlayerManager {
    private static Class<?> PROPERTY_MAP_CLASS;
    private static Method PROPERTY_MAP_GET_METHOD;

    @Override
    public int getPing(@NotNull Object player) {
        //Yay, we contributed this to Spigot and now we can use it on 1.17+ servers.
        if (SpigotReflectionUtil.V_1_17_OR_HIGHER) {
            return PlayerPingAccessorModern.getPing((Player) player);
        } else {
            return SpigotReflectionUtil.getPlayerPingLegacy((Player) player);
        }
    }

    @Override
    public @NotNull ClientVersion getClientVersion(@NotNull Object p) {
        Player player = (Player) p;
        User user = getUser(player);
        if (user.getClientVersion() == null) {
            int protocolVersion;
            if (ProtocolSupportUtil.isAvailable()) {
                protocolVersion = ProtocolSupportUtil.getProtocolVersion(user.getAddress());
                PacketEvents.getAPI().getLogManager().debug("Requested ProtocolSupport for user " + user.getAddress().getHostName() + "'s protocol version. Protocol version: " + protocolVersion);
            } else if (ViaVersionUtil.isAvailable()) {
                protocolVersion = ViaVersionUtil.getProtocolVersion(player);
                PacketEvents.getAPI().getLogManager().debug("Requested ViaVersion for " + player.getName() + "'s protocol version. Protocol version: " + protocolVersion);

            } else {
                if (PacketEvents.getAPI().getServerManager().getVersion() == ServerVersion.V_1_7_10) {
                    protocolVersion = SpigotVersionLookup_1_7.getProtocolVersion(player);
                    PacketEvents.getAPI().getLogManager().debug("Requested Spigot 1.7.10 for " + player.getName() + "'s protocol version. Protocol version: " + protocolVersion);
                } else {
                    //No protocol translation plugins available, the client must be the same version as the server.
                    protocolVersion = PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion();
                    PacketEvents.getAPI().getLogManager().debug("No protocol translation plugins are available. We will assume " + user.getAddress().getHostName() + "'s protocol version is the same as the server's protocol version. Protocol version: " + protocolVersion);
                }
            }
            ClientVersion version = ClientVersion.getById(protocolVersion);
            user.setClientVersion(version);
        }
        return user.getClientVersion();
    }

    @Override
    public Object getChannel(@NotNull Object player) {
        String username = ((Player) player).getName();
        Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(username);
        if (channel == null) {
            channel = SpigotReflectionUtil.getChannel((Player) player);
            if (channel != null) {
                ProtocolManager.CHANNELS.put(username, channel);
            }
        }
        return channel;
    }

    @Override
    public @NotNull User getUser(@NotNull Object player) {
        Player p = (Player) player;
        Object channel = getChannel(p);
        User user = PacketEvents.getAPI().getProtocolManager().getUser(channel);
        if (user == null) {
            user = new User(channel, ConnectionState.PLAY,
                    null, new UserProfile(p.getUniqueId(),
                    p.getName()));
            ProtocolManager.USERS.put(channel, user);
            PacketEvents.getAPI().getInjector().updateUser(channel, user);
        }
        UserProfile profile = user.getProfile();
        if (profile.getTextureProperties().isEmpty()) {
            if (PROPERTY_MAP_CLASS == null) {
                PROPERTY_MAP_CLASS = Reflection.getClassByNameWithoutException("" +
                        "com.mojang.authlib.properties.PropertyMap");
                if (PROPERTY_MAP_CLASS == null) {
                    PROPERTY_MAP_CLASS = Reflection.getClassByNameWithoutException("" +
                            "net.minecraft.util.com.mojang.authlib.properties.PropertyMap");
                }
                PROPERTY_MAP_GET_METHOD = Reflection.getMethod(PROPERTY_MAP_CLASS, "get", Collection.class, Object.class);
            }
            //Get the player's game profile in NMS
            Object nmsGameProfile = SpigotReflectionUtil.getGameProfile((Player) player);
            ReflectionObject reflectGameProfile = new ReflectionObject(nmsGameProfile);
            Object nmsPropertyMap = reflectGameProfile.readObject(0, PROPERTY_MAP_CLASS);
            //Convert the nms property map into a java one to avoid direct GSON access.
            Collection<Object> nmsProperties = null;
            try {
                nmsProperties = (Collection<Object>) PROPERTY_MAP_GET_METHOD.invoke(nmsPropertyMap, "textures");
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            for (Object nmsProperty : nmsProperties) {
                //Read the NMS texture property data
                ReflectionObject reflectProperty = new ReflectionObject(nmsProperty);
                String name = "textures"; //Save us a reflection call :)
                String value = reflectProperty.readString(1);
                String signature = reflectProperty.readString(2);
                TextureProperty textureProperty = new TextureProperty(name, value, signature);
                //Add it to our profile.
                profile.getTextureProperties().add(textureProperty);
            }
        }
        return user;
    }
}
