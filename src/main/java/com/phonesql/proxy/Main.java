package com.phonesql.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.File;
import java.util.Objects;

public class Main {

    final ApplicationContext applicationContext = new ApplicationContext();

    EventLoopGroup parentGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    EventLoopGroup childGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

    private void doRun() throws Exception {

        SslContext sslContext = null;
        Channel httpServerChannel = null;
        Channel httpServerSslChannel = null;
        Channel postgresServerChannel = null;
        Channel postgresServerSslChannel = null;

        if (Objects.nonNull(ApplicationProperties.SSL_CERTIFICATE_PATH)
                && Objects.nonNull(ApplicationProperties.SSL_CERTIFICATE_KEY_PATH)) {

            sslContext =
                    SslContextBuilder.forServer(
                                    new File(ApplicationProperties.SSL_CERTIFICATE_PATH),
                                    new File(ApplicationProperties.SSL_CERTIFICATE_KEY_PATH))
                            .build();
        }

        try {

            if (Objects.nonNull(sslContext)) {

                final ServerBootstrap phoneServerSsl =
                        new HttpServer()
                                .bootstrap(parentGroup, childGroup, applicationContext, sslContext);
                httpServerSslChannel =
                        phoneServerSsl
                                .bind(ApplicationProperties.HTTP_SERVER_PORT)
                                .sync()
                                .channel();
            } else {

                final ServerBootstrap phoneServer =
                        new HttpServer().bootstrap(parentGroup, childGroup, applicationContext);
                httpServerChannel =
                        phoneServer.bind(ApplicationProperties.HTTP_SERVER_PORT).sync().channel();
            }

            if (Objects.nonNull(sslContext)) {

                final ServerBootstrap postgresServerSsl =
                        new PostgresServer()
                                .bootstrap(parentGroup, childGroup, applicationContext, sslContext);
                postgresServerSslChannel =
                        postgresServerSsl
                                .bind(ApplicationProperties.POSTGRES_SERVER_PORT)
                                .sync()
                                .channel();
            } else {
                final ServerBootstrap postgresServer =
                        new PostgresServer().bootstrap(parentGroup, childGroup, applicationContext);
                postgresServerChannel =
                        postgresServer
                                .bind(ApplicationProperties.POSTGRES_SERVER_PORT)
                                .sync()
                                .channel();
            }

            final Bootstrap bootstrap = applicationContext.getMainServerClient();
            EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.handler(new LoggingHandler(LogLevel.INFO));
            bootstrap.handler(
                    new PhoneSQLServerClientInitializer(
                            SslContextBuilder.forClient().build(), applicationContext));

        } finally {

            if (Objects.nonNull(httpServerChannel)) {

                httpServerChannel.closeFuture().sync();
            }

            if (Objects.nonNull(httpServerSslChannel)) {

                httpServerSslChannel.closeFuture().sync();
            }

            if (Objects.nonNull(postgresServerChannel)) {

                postgresServerChannel.closeFuture().sync();
            }

            if (Objects.nonNull(postgresServerSslChannel)) {

                postgresServerSslChannel.closeFuture().sync();
            }

            childGroup.shutdownGracefully();
            parentGroup.shutdownGracefully();
        }
    }

    public static void main(final String[] args) throws Exception {
        new Main().doRun();
    }
}
