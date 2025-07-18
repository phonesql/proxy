package com.phonesql.proxy.postgres.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DataRow {

    private final String[] columns;

    public final ByteBuf byteBuf(final Channel channel) {

        int len = 4 + 2;
        for (final String col : columns) {

            len += col == null ? 4 : col.getBytes().length + 4;
        }

        final ByteBuf msg = channel.alloc().buffer(len + 1);
        msg.writeByte('D');
        msg.writeInt(len);
        msg.writeShort(columns.length);

        for (final String col : columns) {

            if (col == null) {
                msg.writeInt(0);
                continue;
            }
            msg.writeInt(col.getBytes().length);
            msg.writeBytes(col.getBytes());
        }

        return msg;
    }
}
