package com.phonesql.proxy.postgres.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ParameterDescription {

    private final List<Integer> oidParams;

    public ByteBuf byteBuf(final Channel channel) {

        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeByte('t');
        byteBuf.writeInt(4 + 2 + (4 * oidParams.size()));
        byteBuf.writeShort(oidParams.size());

        for (final Integer oid : oidParams) {

            byteBuf.writeInt(oid);
        }

        return byteBuf;
    }
}
