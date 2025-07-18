package com.phonesql.proxy.postgres.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class ErrorResponse {

    private final Map<Byte, String> fields = new LinkedHashMap<>();

    public void putSeverity(final String severity) {

        fields.put((byte) 'S', severity);
    }

    public void putMessage(final String message) {

        fields.put((byte) 'M', message);
    }

    public void putDetail(final String detail) {

        fields.put((byte) 'D', detail);
    }

    public final ByteBuf byteBuf(final Channel channel) {

        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeByte('E');
        byteBuf.writeInt(0);

        for (final Map.Entry<Byte, String> entry : fields.entrySet()) {

            byteBuf.writeByte(entry.getKey());

            byte[] value = entry.getValue().getBytes();
            byteBuf.writeBytes(value);

            byteBuf.writeByte('\0');
        }
        byteBuf.setInt(1, byteBuf.readableBytes());
        return byteBuf;
    }
}
