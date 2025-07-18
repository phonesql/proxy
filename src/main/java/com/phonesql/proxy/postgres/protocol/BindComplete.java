package com.phonesql.proxy.postgres.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class BindComplete {

    public ByteBuf byteBuf(final Channel channel) {

        final ByteBuf byteBuf = channel.alloc().buffer(5);
        byteBuf.writeByte('2');
        byteBuf.writeInt(4);

        return byteBuf;
    }
}
