package com.phonesql.proxy.postgres.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class AuthenticationCleartextPassword {

    public ByteBuf byteBuf(final Channel channel) {

        final ByteBuf byteBuf = channel.alloc().buffer(9);
        byteBuf.writeByte('R');
        byteBuf.writeInt(8);
        byteBuf.writeInt(3);

        return byteBuf;
    }
}
