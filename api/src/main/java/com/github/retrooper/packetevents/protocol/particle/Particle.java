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

package com.github.retrooper.packetevents.protocol.particle;

import com.github.retrooper.packetevents.protocol.particle.data.ParticleData;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleType;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

public class Particle<T extends ParticleData> {

    private ParticleType<T> type;
    private T data;

    public Particle(ParticleType<T> type, T data) {
        this.type = type;
        this.data = data;
    }

    public Particle(ParticleType<T> type) {
        this(type, ParticleData.emptyData());
    }

    @SuppressWarnings("unchecked") // will work on runtime
    public static Particle<?> read(PacketWrapper<?> wrapper) {
        ParticleType<?> type = wrapper.readMappedEntity(ParticleTypes::getById);
        return new Particle<>((ParticleType<ParticleData>) type, type.readData(wrapper));
    }

    public static <T extends ParticleData> void write(PacketWrapper<?> wrapper, Particle<T> particle) {
        wrapper.writeMappedEntity(particle.type);
        particle.getType().writeData(wrapper, particle.data);
    }

    public ParticleType<T> getType() {
        return this.type;
    }

    public void setType(ParticleType<T> type) {
        this.type = type;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
