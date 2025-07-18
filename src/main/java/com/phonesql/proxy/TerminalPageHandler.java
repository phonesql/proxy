package com.phonesql.proxy;

import static com.phonesql.proxy.CookieUtils.getSessionFromCookie;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import com.phonesql.proxy.websocket.WebSocketClientSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import java.nio.charset.Charset;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TerminalPageHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final ApplicationContext applicationContext;

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest httpRequest)
            throws Exception {

        if (!httpRequest.uri().startsWith("/terminal")) {

            ctx.fireChannelRead(httpRequest.retain());
            return;
        }

        String session = getSessionFromCookie(httpRequest.headers());
        WebSocketClientSession webSocketClientSession = null;

        if (session != null) {
            webSocketClientSession =
                    (WebSocketClientSession) applicationContext.getClientSessions().get(session);
        }

        final PreAuthorization preAuthorization =
                applicationContext.getPreAuthorizations().get(session);

        if (preAuthorization == null && webSocketClientSession == null) {

            final FullHttpResponse response =
                    new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND, Unpooled.EMPTY_BUFFER);

            response.headers().setInt(CONTENT_LENGTH, 0);
            response.headers().add(HttpHeaderNames.LOCATION, "/login");

            ctx.channel().writeAndFlush(response);
            return;
        }

        String body =
                applicationContext
                        .getCompiler()
                        .compile(applicationContext.getTemplateLoader().getTemplate("terminal"))
                        .execute(null);
        ByteBuf content = ctx.alloc().buffer();
        content.writeCharSequence(body, Charset.defaultCharset());
        FullHttpResponse res =
                new DefaultFullHttpResponse(httpRequest.protocolVersion(), OK, content);

        res.headers().set(CONTENT_TYPE, HttpHeaderValues.TEXT_HTML);
        HttpUtil.setContentLength(res, content.readableBytes());

        ctx.channel().writeAndFlush(res);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
