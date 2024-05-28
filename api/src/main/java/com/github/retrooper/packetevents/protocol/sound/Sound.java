/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2024 retrooper and contributors
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

package com.github.retrooper.packetevents.protocol.sound;

import com.github.retrooper.packetevents.protocol.mapper.MappedEntity;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.jetbrains.annotations.Nullable;

public interface Sound extends MappedEntity {

    ResourceLocation getSoundId();

    @Nullable
    Float getRange();

    static Sound read(PacketWrapper<?> wrapper) {
        return wrapper.readMappedEntityOrDirect(Sounds::getById, Sound::readDirect);
    }

    static Sound readDirect(PacketWrapper<?> wrapper) {
        ResourceLocation soundId = wrapper.readIdentifier();
        Float range = wrapper.readOptional(PacketWrapper::readFloat);
        return new StaticSound(soundId, range);
    }

    static void write(PacketWrapper<?> wrapper, Sound sound) {
        wrapper.writeMappedEntityOrDirect(sound, Sound::writeDirect);
    }

    static void writeDirect(PacketWrapper<?> wrapper, Sound sound) {
        wrapper.writeIdentifier(sound.getSoundId());
        wrapper.writeOptional(sound.getRange(), PacketWrapper::writeFloat);
    }
}
