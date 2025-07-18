package com.phonesql.proxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PhoneSQLServerClientInitializer extends ChannelInitializer<SocketChannel> {

    private static final int MAX_CONTENT_LENGTH = 65536;

    private final SslContext sslCtx;
    private final ApplicationContext applicationContext;

    @Override
    public void initChannel(SocketChannel ch) throws URISyntaxException {
        ChannelPipeline p = ch.pipeline();

        final URI phoneSQLUri = new URI(ApplicationProperties.PHONESQL_URI);

        if (Objects.equals(phoneSQLUri.getScheme(), "https")) {
            p.addLast(sslCtx.newHandler(ch.alloc(), phoneSQLUri.getHost(), 443));
        }

        p.addLast(new HttpClientCodec());
        p.addLast(new HttpContentDecompressor(MAX_CONTENT_LENGTH));
        p.addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH));
        p.addLast(new OAuth2AuthorizationCodeResponseHandler(applicationContext));
        p.addLast(new OAuth2DatabaseRegistrationResponseHandler(applicationContext));
    }
}
