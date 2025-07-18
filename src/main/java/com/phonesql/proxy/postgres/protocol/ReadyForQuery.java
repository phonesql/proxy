package com.phonesql.proxy.postgres.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class ReadyForQuery {

    public ByteBuf idleByteBuf(final Channel channel) {

        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeByte('Z');
        byteBuf.writeInt(5);
        byteBuf.writeByte('I');

        return byteBuf;
    }

    public ByteBuf transactionByteBuf(final Channel channel) {

        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeByte('Z');
        byteBuf.writeInt(5);
        byteBuf.writeByte('T');

        return byteBuf;
    }

    public ByteBuf errorByteBuf(final Channel channel) {

        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeByte('Z');
        byteBuf.writeInt(5);
        byteBuf.writeByte('E');

        return byteBuf;
    }
}
