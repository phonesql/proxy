package com.phonesql.proxy;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;

public class LoginPageHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final ApplicationContext applicationContext;

    public LoginPageHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {

        if (!(GET.equals(req.method()) && ("/".equals(req.uri()) || "/login".equals(req.uri())))) {
            ctx.fireChannelRead(req.retain());
            return;
        }

        final String body =
                applicationContext
                        .getCompiler()
                        .compile(applicationContext.getTemplateLoader().getTemplate("login"))
                        .execute(
                                new HashMap<>() {
                                    {
                                        put(
                                                "databaseListUri",
                                                ApplicationProperties.PHONESQL_URI
                                                        + "/database-list");
                                    }
                                });
        ByteBuf content = ctx.alloc().buffer();
        content.writeCharSequence(body, Charset.defaultCharset());
        FullHttpResponse res = new DefaultFullHttpResponse(req.protocolVersion(), OK, content);

        final Cookie sessionCookie = new DefaultCookie("SESSION", "");
        sessionCookie.setPath("/");
        sessionCookie.setHttpOnly(true);

        res.headers().set(SET_COOKIE, ServerCookieEncoder.LAX.encode(sessionCookie));
        res.headers().set(CONTENT_TYPE, HttpHeaderValues.TEXT_HTML);
        HttpUtil.setContentLength(res, content.readableBytes());

        sendHttpResponse(ctx, req, res);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    private static void sendHttpResponse(
            ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {

        HttpResponseStatus responseStatus = res.status();
        if (responseStatus.code() != 200) {
            ByteBufUtil.writeUtf8(res.content(), responseStatus.toString());
            HttpUtil.setContentLength(res, res.content().readableBytes());
        }

        boolean keepAlive = HttpUtil.isKeepAlive(req) && responseStatus.code() == 200;
        HttpUtil.setKeepAlive(res, keepAlive);
        ChannelFuture future = ctx.writeAndFlush(res);
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
