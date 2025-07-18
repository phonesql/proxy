package com.phonesql.proxy.postgres;

import com.phonesql.proxy.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PostgresSslHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Integer SSL_REQUEST_CODE = 80877103;

    private final boolean sslEnabled;

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final ByteBuf byteBuf0) {

        final String session =
                (String) ctx.channel().attr(AttributeKey.valueOf(Constants.SESSION_ATTR_KEY)).get();

        ByteBuf byteBuf;

        if (Objects.isNull(session)) {

            final ByteBuf pendingByteBuf =
                    (ByteBuf)
                            ctx.channel()
                                    .attr(AttributeKey.valueOf(Constants.PENDING_BYTE_BUF_ATTR_KEY))
                                    .getAndSet(null);

            if (Objects.nonNull(pendingByteBuf)) {

                byteBuf = pendingByteBuf.writeBytes(byteBuf0);
            } else {
                byteBuf = byteBuf0.copy();
            }

            if (byteBuf.readableBytes() < 8) {

                ctx.channel()
                        .attr(AttributeKey.valueOf(Constants.PENDING_BYTE_BUF_ATTR_KEY))
                        .set(byteBuf.copy());
                return;
            }

            if (byteBuf.getInt(4) == SSL_REQUEST_CODE) {
                handleSslNegotiation(ctx);

                byteBuf.readerIndex(byteBuf.readerIndex() + 8);
            }
        } else {
            byteBuf = byteBuf0.copy();
        }

        ctx.fireChannelRead(byteBuf);
    }

    private void handleSslNegotiation(final ChannelHandlerContext ctx) {

        final ByteBuf msg = ctx.alloc().buffer(1);
        msg.writeByte(sslEnabled ? 'S' : 'N');
        ctx.writeAndFlush(msg);
    }
}
