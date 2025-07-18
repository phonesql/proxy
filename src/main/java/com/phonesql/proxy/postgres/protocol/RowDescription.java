package com.phonesql.proxy.postgres.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.util.List;

public class RowDescription {

    private static final int PER_COLUMN_OVERHEAD = 18;

    private final List<String> columns;

    public RowDescription(List<String> columns) {
        this.columns = columns;
    }

    public ByteBuf byteBuf(final Channel ctx) {

        int len = 6;

        for (final String col : columns) {

            len += PER_COLUMN_OVERHEAD + col.getBytes().length + 1;
        }

        final ByteBuf msg = ctx.alloc().buffer(len + 1);

        msg.writeByte('T');
        msg.writeInt(len);
        msg.writeShort(columns.size());

        for (final String col : columns) {

            msg.writeBytes(col.getBytes());
            msg.writeByte('\0');

            msg.writeInt(0);
            msg.writeShort(0);

            msg.writeInt(1043); // VARCHAR OID
            msg.writeShort(-1);

            msg.writeInt(-1);
            msg.writeShort(0);
        }

        return msg;
    }
}
