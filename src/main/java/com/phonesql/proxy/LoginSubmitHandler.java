package com.phonesql.proxy;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.SET_COOKIE;
import static io.netty.handler.codec.http.HttpMethod.POST;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class LoginSubmitHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final ApplicationContext applicationContext;

    public LoginSubmitHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest)
            throws URISyntaxException {

        if (!(POST.equals(httpRequest.method()) && "/login".equals(httpRequest.uri()))) {
            ctx.fireChannelRead(httpRequest.retain());
            return;
        }

        final QueryStringDecoder bodyDecoder =
                new QueryStringDecoder(
                        "?" + httpRequest.content().toString(Charset.defaultCharset()));

        final String type = bodyDecoder.parameters().get("type").getFirst();
        final String database = bodyDecoder.parameters().get("database").getFirst();
        final String username = bodyDecoder.parameters().get("username").getFirst();
        final String password = bodyDecoder.parameters().get("password").getFirst();

        final String id = UUID.randomUUID().toString();

        final PreAuthorization preAuthorization = new PreAuthorization();
        if (type.equals("username_password")) {

            preAuthorization.setType(PreAuthorization.Type.USERNAME_PASSWORD);
            preAuthorization.setDatabase(database);
            preAuthorization.setUsername(username);
            preAuthorization.setPassword(password);

            applicationContext.getPreAuthorizations().put(id, preAuthorization);

            final FullHttpResponse response =
                    new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND, Unpooled.EMPTY_BUFFER);

            final Cookie sessionCookie =
                    new DefaultCookie(
                            "SESSION",
                            Base64.getEncoder()
                                    .encodeToString(id.getBytes(StandardCharsets.UTF_8)));
            sessionCookie.setPath("/");
            sessionCookie.setHttpOnly(true);

            response.headers().set(SET_COOKIE, ServerCookieEncoder.LAX.encode(sessionCookie));

            response.headers().setInt(CONTENT_LENGTH, 0);
            response.headers().add(HttpHeaderNames.LOCATION, "/terminal");

            ctx.channel().writeAndFlush(response);
        } else {

            preAuthorization.setType(PreAuthorization.Type.OAUTH2);
            preAuthorization.setDatabase(database);
            preAuthorization.setState(id);

            applicationContext.getPreAuthorizations().put(id, preAuthorization);

            final FullHttpResponse response =
                    new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND, Unpooled.EMPTY_BUFFER);

            response.headers().setInt(CONTENT_LENGTH, 0);
            response.headers().add(HttpHeaderNames.LOCATION, getRedirectUri(id));

            ctx.channel().writeAndFlush(response);
        }
    }

    private static String getRedirectUri(String id) throws URISyntaxException {
        final QueryStringEncoder encoder =
                new QueryStringEncoder(
                        new URI(ApplicationProperties.PHONESQL_URI + "/oauth2/authorize")
                                .toString());
        encoder.addParam("response_type", "code");
        encoder.addParam("client_id", ApplicationProperties.OAUTH2_CLIENT_ID);
        encoder.addParam(
                "redirect_uri",
                new URI(ApplicationProperties.PHONESQL_PROXY_URI + "/login/oauth2/code")
                        .toString());
        encoder.addParam("scope", "openid database.read");
        encoder.addParam("state", id);
        return encoder.toString();
    }
}
