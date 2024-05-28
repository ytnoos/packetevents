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

package com.github.retrooper.packetevents.protocol.stats;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import com.github.retrooper.packetevents.util.mappings.MappingHelper;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;

public class Statistics {

    private static final Map<String, Statistic> STATISTIC_MAP = new HashMap<>();

    public static Statistic getById(String id) {
        return STATISTIC_MAP.get(id);
    }

    static {
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();

        if (version.isOlderThan(ServerVersion.V_1_12_2)) {

            NBTCompound mapping = MappingHelper.decompress("mappings/stats/statistics");

            if (version.isOlderThanOrEquals(ServerVersion.V_1_8_3)) {
                mapping = mapping.getCompoundTagOrThrow("V_1_8");
            } else {
                mapping = mapping.getCompoundTagOrThrow("V_1_12");
            }

            for (Map.Entry<String, NBT> entry : mapping.getTags().entrySet()) {
                Component value = AdventureSerializer.parseComponent(((NBTString) entry.getValue()).getValue());

                Statistic statistic = new Statistic() {
                    @Override
                    public String getId() {
                        return entry.getKey();
                    }

                    @Override
                    public Component display() {
                        return value;
                    }

                    @Override
                    public boolean equals(Object obj) {
                        if (obj instanceof Statistic) {
                            return ((Statistic) obj).getId().equals(this.getId());
                        }
                        return false;
                    }
                };

                STATISTIC_MAP.put(entry.getKey(), statistic);
            }
        }
    }

}
