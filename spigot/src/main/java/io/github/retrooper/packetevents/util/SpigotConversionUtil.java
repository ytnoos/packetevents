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

package io.github.retrooper.packetevents.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.HumanoidArm;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.Dimension;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.MainHand;
import org.jetbrains.annotations.Nullable;

public class SpigotConversionUtil {
    public static Location fromBukkitLocation(org.bukkit.Location location) {
        return new Location(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static org.bukkit.Location toBukkitLocation(org.bukkit.World world, Location location) {
        return new org.bukkit.Location(world, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static PotionType fromBukkitPotionEffectType(org.bukkit.potion.PotionEffectType potionEffectType) {
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();
        int id = potionEffectType.getId();
        if (version.isNewerThanOrEquals(ServerVersion.V_1_20_2)) {
            id--;
        }
        return PotionTypes.getById(id, version);
    }

    public static org.bukkit.potion.PotionEffectType toBukkitPotionEffectType(PotionType potionType) {
        ClientVersion version = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
        int id = potionType.getId(version);
        if (version.isNewerThanOrEquals(ClientVersion.V_1_20_2)) {
            id++;
        }
        return org.bukkit.potion.PotionEffectType.getById(id);
    }

    public static GameMode fromBukkitGameMode(org.bukkit.GameMode gameMode) {
        return GameMode.getById(gameMode.getValue());
    }

    public static org.bukkit.GameMode toBukkitGameMode(GameMode gameMode) {
        return org.bukkit.GameMode.getByValue(gameMode.getId());
    }

    public static WrappedBlockState fromBukkitBlockData(BlockData blockData) {
        String string = blockData.getAsString(false);
        return WrappedBlockState.getByString(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(), string);
    }

    public static BlockData toBukkitBlockData(WrappedBlockState blockState) {
        return org.bukkit.Bukkit.createBlockData(blockState.toString());
    }

    public static EntityType fromBukkitEntityType(org.bukkit.entity.EntityType entityType) {
        ServerVersion serverVersion = PacketEvents.getAPI().getServerManager().getVersion();
        if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_14)) {
            return EntityTypes.getByName(entityType.getKey().toString());
        } else if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_13)) {
            return EntityTypes.getByName("minecraft:" + entityType.getName());
        } else {
            if (entityType.getTypeId() == -1) {
                return null;
            }
            return EntityTypes.getById(serverVersion.toClientVersion(), entityType.getTypeId());
        }
    }

    public static org.bukkit.entity.EntityType toBukkitEntityType(EntityType entityType) {
        ServerVersion serverVersion = PacketEvents.getAPI().getServerManager().getVersion();
        if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_13)) {
            return org.bukkit.entity.EntityType.fromName(entityType.getName().getKey());
        } else {
            return org.bukkit.entity.EntityType.fromId(entityType.getId(serverVersion.toClientVersion()));
        }
    }

    //This is sort of a lazy approach, but likely works.
    public static ItemType fromBukkitItemMaterial(org.bukkit.Material material) {
        org.bukkit.inventory.ItemStack bukkitStack = new org.bukkit.inventory.ItemStack(material);
        ItemStack stack = fromBukkitItemStack(bukkitStack);
        return stack.getType();
    }

    //This is a lazy approach, but likely works.
    public static org.bukkit.Material toBukkitItemMaterial(ItemType itemType) {
        ItemStack stack = ItemStack.builder().type(itemType).build();
        org.bukkit.inventory.ItemStack bukkitStack = toBukkitItemStack(stack);
        return bukkitStack.getType();
    }

    public static WrappedBlockState fromBukkitMaterialData(org.bukkit.material.MaterialData materialData) {
        int combinedID = SpigotReflectionUtil.getBlockDataCombinedId(materialData);
        ServerVersion serverVersion = PacketEvents.getAPI().getServerManager().getVersion();
        return WrappedBlockState.getByGlobalId(serverVersion.toClientVersion(), combinedID);
    }

    public static org.bukkit.material.MaterialData toBukkitMaterialData(WrappedBlockState state) {
        return SpigotReflectionUtil.getBlockDataByCombinedId(state.getGlobalId());
    }

    public static ItemStack fromBukkitItemStack(org.bukkit.inventory.ItemStack itemStack) {
        return SpigotReflectionUtil.decodeBukkitItemStack(itemStack);
    }

    public static org.bukkit.inventory.ItemStack toBukkitItemStack(ItemStack itemStack) {
        return SpigotReflectionUtil.encodeBukkitItemStack(itemStack);
    }

    public static Dimension fromBukkitWorld(World world) {
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_14)) {
            return new Dimension(world.getEnvironment().getId());
        } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_16)) {
            Object worldServer = SpigotReflectionUtil.convertBukkitWorldToWorldServer(world);
            return new Dimension(SpigotReflectionUtil.getDimensionId(worldServer));
        } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_16_2)) {
            Object worldServer = SpigotReflectionUtil.convertBukkitWorldToWorldServer(world);
            Dimension dimension = new Dimension(new NBTCompound());
            dimension.setDimensionName(SpigotReflectionUtil.getDimensionKey(worldServer));
            return dimension;
        } else {
            Object worldServer = SpigotReflectionUtil.convertBukkitWorldToWorldServer(world);
            Object nbt = SpigotReflectionUtil.convertWorldServerDimensionToNMSNbt(worldServer);
            return new Dimension(SpigotReflectionUtil.fromMinecraftNBT(nbt));
        }
    }

    public static ParticleType<?> fromBukkitParticle(Enum<?> particle) {
        return SpigotReflectionUtil.toPacketEventsParticle(particle);
    }

    public static Enum<?> toBukkitParticle(ParticleType<?> particle) {
        return SpigotReflectionUtil.fromPacketEventsParticle(particle);
    }

    /**
     * Access the Bukkit Entity associated to the Entity ID.
     * @param world The world they are in. This field is optional, but is recommended as it could boost performance.
     * @param entityId The associated Entity ID
     * @return The Bukkit Entity
     */
    public static org.bukkit.entity.Entity getEntityById(@Nullable World world, int entityId) {
        return SpigotReflectionUtil.getEntityById(world, entityId);
    }

    public static Pose toBukkitPose(EntityPose pose) {
        return Pose.values()[pose.ordinal()];
    }

    public static EntityPose fromBukkitPose(Pose pose) {
        return EntityPose.values()[pose.ordinal()];
    }

    public static MainHand toBukkitHand(HumanoidArm arm) {
        return MainHand.values()[arm.ordinal()];
    }
}
