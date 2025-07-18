package com.phonesql.proxy;

import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class ClientChannels extends DefaultChannelGroup {

    public ClientChannels() {
        super(GlobalEventExecutor.INSTANCE);
    }
}
