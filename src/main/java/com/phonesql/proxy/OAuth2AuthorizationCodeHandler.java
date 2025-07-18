package com.phonesql.proxy;

import static io.netty.handler.codec.http.HttpMethod.GET;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public class OAuth2AuthorizationCodeHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final ApplicationContext applicationContext;

    public OAuth2AuthorizationCodeHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest)
            throws InterruptedException, URISyntaxException {

        if (!(GET.equals(httpRequest.method())
                && httpRequest.uri().startsWith("/login/oauth2/code"))) {
            ctx.fireChannelRead(httpRequest.retain());
            return;
        }

        final QueryStringDecoder paramDecoder = new QueryStringDecoder(httpRequest.uri());

        final String authorizationCode = paramDecoder.parameters().get("code").getFirst();
        final String state = paramDecoder.parameters().get("state").getFirst();

        final PreAuthorization preAuthorization =
                applicationContext.getPreAuthorizations().get(state);

        if (Objects.isNull(preAuthorization)) {
            return;
        }

        final URI tokenUri = new URI(ApplicationProperties.PHONESQL_URI + "/oauth2/token");

        final Channel channel =
                applicationContext
                        .mainServerClient
                        .connect(
                                tokenUri.getHost(),
                                tokenUri.getPort() < 0
                                        ? (Objects.equals(tokenUri.getScheme(), "https") ? 443 : 80)
                                        : tokenUri.getPort())
                        .sync()
                        .channel();

        final String requestId = UUID.randomUUID().toString();

        byte[] requestBody = getRequestBody(state, authorizationCode);

        final DefaultFullHttpRequest request =
                new DefaultFullHttpRequest(
                        HttpVersion.HTTP_1_1,
                        HttpMethod.POST,
                        tokenUri.getPath(),
                        Unpooled.wrappedBuffer(requestBody));
        request.headers().set(HttpHeaderNames.HOST, tokenUri.getHost());
        request.headers()
                .set(
                        HttpHeaderNames.AUTHORIZATION,
                        "Basic "
                                + new String(
                                        Base64.getEncoder()
                                                .encode(
                                                        (ApplicationProperties.OAUTH2_CLIENT_ID
                                                                        + ":"
                                                                        + ApplicationProperties
                                                                                .OAUTH2_CLIENT_SECRET)
                                                                .getBytes())));
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        request.headers().set("X-Request-ID", requestId);
        request.headers()
                .set(
                        HttpHeaderNames.CONTENT_TYPE,
                        HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);

        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, requestBody.length);

        applicationContext
                .getMainServerRequests()
                .put(requestId, PhoneSQLServerRequestType.OAUTH2_AUTHORIZATION_CODE_GRANT);

        preAuthorization.setChannel(ctx.channel());

        channel.writeAndFlush(request);
        ctx.fireChannelRead(httpRequest.retain());
    }

    private byte[] getRequestBody(final String state, final String authorizationCode)
            throws URISyntaxException {

        final QueryStringEncoder queryStringEncoder = new QueryStringEncoder("");
        queryStringEncoder.addParam("state", state);
        queryStringEncoder.addParam("grant_type", "authorization_code");
        queryStringEncoder.addParam("code", authorizationCode);
        queryStringEncoder.addParam(
                "redirect_uri",
                new URI(ApplicationProperties.PHONESQL_PROXY_URI + "/login/oauth2/code")
                        .toString());

        return queryStringEncoder.toString().substring(1).getBytes();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
