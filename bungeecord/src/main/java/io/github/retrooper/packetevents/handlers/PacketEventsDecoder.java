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

package io.github.retrooper.packetevents.handlers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.EventCreationUtil;
import io.github.retrooper.packetevents.injector.ServerConnectionInitializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@ChannelHandler.Sharable
public class PacketEventsDecoder extends MessageToMessageDecoder<ByteBuf> {
    public User user;
    public ProxiedPlayer player;
    int catches = 0;
    boolean sent = false;

    public PacketEventsDecoder(User user) {
        this.user = user;
    }

    public void read(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> output) throws Exception {
        ByteBuf transformed = ctx.alloc().buffer().writeBytes(byteBuf);
        try {
            int firstReaderIndex = transformed.readerIndex();
            PacketReceiveEvent packetReceiveEvent = EventCreationUtil.createReceiveEvent(ctx.channel(),
                    user, player, transformed, false);
            int readerIndex = transformed.readerIndex();
            PacketEvents.getAPI().getEventManager().callEvent(packetReceiveEvent, () -> transformed.readerIndex(readerIndex));
            if (!packetReceiveEvent.isCancelled()) {
                if (packetReceiveEvent.getLastUsedWrapper() != null) {
                    ByteBufHelper.clear(packetReceiveEvent.getByteBuf());
                    packetReceiveEvent.getLastUsedWrapper().writeVarInt(packetReceiveEvent.getPacketId());
                    packetReceiveEvent.getLastUsedWrapper().write();
                } else {
                    transformed.readerIndex(firstReaderIndex);
                }
                output.add(transformed.retain());
            }
            if (packetReceiveEvent.hasPostTasks()) {
                for (Runnable task : packetReceiveEvent.getPostTasks()) {
                    task.run();
                }
            }
        } catch (Throwable e) {
            catches++;

            if (catches < 10) {
                e.printStackTrace();
            } else if (!sent) {
                System.out.println("Too many exceptions caught in PacketEventsDecoder from " + player.getName() + ", stopping stacktrace printing.");
                sent = true;
            }
        } finally {
            transformed.release();
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        if (byteBuf.isReadable()) {
            read(ctx, byteBuf, out);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
        ServerConnectionInitializer.destroyChannel(ctx.channel());
        super.channelInactive(ctx);
    }
}
