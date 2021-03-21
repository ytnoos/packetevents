package io.github.retrooper.packetevents.injector;

import org.bukkit.entity.Player;

public interface ChannelInjector {
    void inject();

    void eject();

    void injectPlayer(Player player);

    boolean hasInjected(Player player);

    void sendPacket(Object channel, Object rawNMSPacket);
}
