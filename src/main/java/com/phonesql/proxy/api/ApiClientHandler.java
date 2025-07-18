package com.phonesql.proxy.api;

import com.nimbusds.jwt.SignedJWT;
import com.phonesql.proxy.ApplicationContext;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ApiClientHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final ApplicationContext applicationContext;

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest httpRequest)
            throws Exception {

        if (!httpRequest.uri().startsWith("/api")) {

            ctx.fireChannelRead(httpRequest.retain());
            return;
        }

        try (final ByteBufInputStream inputStream = new ByteBufInputStream(httpRequest.content())) {

            final ApiRequest apiRequest =
                    applicationContext
                            .getObjectMapper()
                            .readValue((InputStream) inputStream, ApiRequest.class);

            final String database = apiRequest.getDatabase();

            String session = httpRequest.headers().get("SESSION");
            ApiClientSession apiClientSession = null;

            if (session != null) {
                apiClientSession =
                        (ApiClientSession) applicationContext.getClientSessions().get(session);

                apiClientSession.startRequest();
                for (final Query query : apiRequest.getQueries()) {

                    apiClientSession.query(query);
                }
                apiClientSession.finishRequest();
            }

            if (apiClientSession == null) {

                session = UUID.randomUUID().toString();

                apiClientSession = new ApiClientSession();
                apiClientSession.setObjectMapper(applicationContext.getObjectMapper());
                apiClientSession.setSession(session);
                apiClientSession.setDatabase(database);

                apiClientSession.setPhoneChannel(
                        applicationContext.getPhoneChannels().findByDatabase(database));
                apiClientSession.setClientChannel(ctx.channel());
                apiClientSession.setAuthentication(extractAuthentication(httpRequest));

                applicationContext.getClientSessions().put(session, apiClientSession);

                apiClientSession.startRequest();
                for (final Query query : apiRequest.getQueries()) {

                    apiClientSession.query(query);
                }
                apiClientSession.finishRequest();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    public Authentication extractAuthentication(final FullHttpRequest httpRequest)
            throws ParseException {

        final String authorizationHeader = httpRequest.headers().get(HttpHeaderNames.AUTHORIZATION);

        if (authorizationHeader.toLowerCase().startsWith("basic ")) {

            final String base64Credentials =
                    authorizationHeader.substring("Basic ".length()).trim();
            final byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
            final String[] credentials =
                    new String(decodedBytes, StandardCharsets.UTF_8).split(":", 2);

            return new BasicAuthentication(credentials[0], credentials[1]);
        }

        final String bearerToken = authorizationHeader.substring("Bearer ".length()).trim();

        return new OAuth2Authentication(SignedJWT.parse(bearerToken));
    }
}
