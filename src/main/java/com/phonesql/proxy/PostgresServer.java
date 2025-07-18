package com.phonesql.proxy;

import com.phonesql.proxy.postgres.PostgresClientHandler;
import com.phonesql.proxy.postgres.PostgresSslHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import java.util.Objects;

public class PostgresServer {

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

                                ch.pipeline()
                                        .addLast(
                                                new PostgresSslHandler(
                                                        Objects.nonNull(sslContext)));

                                if (Objects.nonNull(sslContext)) {

                                    ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));
                                }

                                ch.pipeline()
                                        .addLast(
                                                new PostgresClientHandler(
                                                        applicationContext.getClientSessions(),
                                                        applicationContext.getPhoneChannels()));
                            }
                        })
                .option(ChannelOption.SO_BACKLOG, 128)
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
