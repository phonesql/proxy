package com.phonesql.proxy.phone;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.PlatformDependent;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import lombok.Getter;

@Getter
public class PhoneChannels extends DefaultChannelGroup {

    private final ConcurrentMap<String, ChannelId> unverifiedChannels =
            PlatformDependent.newConcurrentHashMap();

    private final ConcurrentMap<String, ChannelId> databaseChannels =
            PlatformDependent.newConcurrentHashMap();

    public PhoneChannels() {
        super(GlobalEventExecutor.INSTANCE);
    }

    public void addDatabase(final String database, final ChannelId channelId) {

        databaseChannels.put(database, channelId);
    }

    public Channel findByDatabase(final String databaseId) {

        final ChannelId channelId = databaseChannels.get(databaseId);

        if (Objects.nonNull(channelId)) {

            return find(channelId);
        }

        return null;
    }
}
