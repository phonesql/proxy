package com.phonesql.proxy;

import com.phonesql.proxy.api.ApiClientHandler;
import com.phonesql.proxy.phone.WebSocketPhoneHandler;
import com.phonesql.proxy.websocket.WebSocketClientHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import java.util.Objects;

public class HttpServer {

    private static final String WEBSOCKET_PATH = "/websocket";

    private static final int MAX_FRAME_SIZE = 10485760;

    public ServerBootstrap bootstrap(
            final EventLoopGroup parentGroup,
            final EventLoopGroup childGroup,
            final ApplicationContext applicationContext,
            final SslContext sslContext) {

        ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap
                .group(parentGroup, childGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(
                        new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) {
                                if (Objects.nonNull(sslContext)) {
                                    ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));
                                }
                                ch.pipeline().addLast(new HttpServerCodec());
                                ch.pipeline().addLast(new HttpObjectAggregator(MAX_FRAME_SIZE));
                                ch.pipeline()
                                        .addLast(
                                                new WebSocketServerCompressionHandler(
                                                        MAX_FRAME_SIZE));
                                ch.pipeline()
                                        .addLast(
                                                new WebSocketServerProtocolHandler(
                                                        WEBSOCKET_PATH,
                                                        null,
                                                        true,
                                                        MAX_FRAME_SIZE,
                                                        true,
                                                        true));
                                ch.pipeline()
                                        .addLast(new WebSocketPhoneHandler(applicationContext));
                                ch.pipeline()
                                        .addLast(new WebSocketClientHandler(applicationContext));
                                ch.pipeline().addLast(new ApiClientHandler(applicationContext));
                                ch.pipeline().addLast(new StaticResourceHandler());
                                ch.pipeline().addLast(new LoginPageHandler(applicationContext));
                                ch.pipeline()
                                        .addLast(
                                                new OAuth2AuthorizationCodeHandler(
                                                        applicationContext));
                                ch.pipeline().addLast(new LoginSubmitHandler(applicationContext));
                                ch.pipeline().addLast(new TerminalPageHandler(applicationContext));
                            }
                        })
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        return serverBootstrap;
    }

    public ServerBootstrap bootstrap(
            final EventLoopGroup parentGroup,
            final EventLoopGroup childGroup,
            final ApplicationContext applicationContext) {

        return bootstrap(parentGroup, childGroup, applicationContext, null);
    }
}
