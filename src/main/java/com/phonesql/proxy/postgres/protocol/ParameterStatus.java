package com.phonesql.proxy.postgres.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ParameterStatus {

    private final String name;

    private final String value;

    public ByteBuf byteBuf(final Channel channel) {

        final ByteBuf byteBuf = channel.alloc().buffer(5);
        byteBuf.writeByte('S');

        final byte[] nameBytes = name.getBytes();
        final byte[] valueBytes = value.getBytes();

        byteBuf.writeInt(4 + nameBytes.length + 1 + valueBytes.length + 1);

        byteBuf.writeBytes(nameBytes);
        byteBuf.writeByte('\0');

        byteBuf.writeBytes(valueBytes);
        byteBuf.writeByte('\0');

        return byteBuf;
    }
}
