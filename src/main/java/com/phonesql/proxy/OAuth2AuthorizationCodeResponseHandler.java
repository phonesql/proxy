package com.phonesql.proxy;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.SET_COOKIE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jwt.SignedJWT;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public class OAuth2AuthorizationCodeResponseHandler
        extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final ApplicationContext applicationContext;

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final FullHttpResponse msg)
            throws IOException, ParseException {

        final String requestId = msg.headers().get("X-Request-ID");

        if (!(Objects.nonNull(requestId)
                && PhoneSQLServerRequestType.OAUTH2_AUTHORIZATION_CODE_GRANT.equals(
                        applicationContext.getMainServerRequests().get(requestId)))) {
            ctx.fireChannelRead(msg.retain());
            return;
        }

        applicationContext.getMainServerRequests().remove(requestId);

        byte[] bytes = new byte[msg.content().readableBytes()];
        msg.content().readBytes(bytes);

        final Response response =
                applicationContext.getObjectMapper().readValue(bytes, Response.class);

        final SignedJWT accessToken = SignedJWT.parse(response.getAccessToken());
        final String state = accessToken.getJWTClaimsSet().getStringClaim("state");

        final PreAuthorization preAuthorization =
                applicationContext.getPreAuthorizations().get(state);

        preAuthorization.setToken(accessToken);

        final FullHttpResponse fullHttpResponse =
                new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND, Unpooled.EMPTY_BUFFER);

        final Cookie sessionCookie =
                new DefaultCookie(
                        "SESSION",
                        Base64.getEncoder().encodeToString(state.getBytes(StandardCharsets.UTF_8)));
        sessionCookie.setPath("/");
        sessionCookie.setHttpOnly(true);

        fullHttpResponse.headers().set(SET_COOKIE, ServerCookieEncoder.LAX.encode(sessionCookie));

        fullHttpResponse.headers().setInt(CONTENT_LENGTH, 0);
        fullHttpResponse.headers().add(HttpHeaderNames.LOCATION, "/terminal");

        preAuthorization.getChannel().writeAndFlush(fullHttpResponse);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Response {

        @JsonProperty("access_token")
        private String accessToken;
    }
}
