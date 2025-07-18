package com.phonesql.proxy.phone;

import com.phonesql.proxy.ApplicationContext;
import com.phonesql.proxy.ApplicationProperties;
import com.phonesql.proxy.ClientSession;
import com.phonesql.proxy.PhoneSQLServerRequestType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import java.net.URI;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import proto.PhoneProxyProtocol;

public class WebSocketPhoneHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final PhoneChannels phoneChannels;
    private final ApplicationContext applicationContext;

    public WebSocketPhoneHandler(final ApplicationContext applicationContext) {
        this.phoneChannels = applicationContext.getPhoneChannels();
        this.applicationContext = applicationContext;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final WebSocketFrame msg)
            throws Exception {

        if (Objects.isNull(applicationContext.getPhoneChannels().find(ctx.channel().id()))
                || !(msg instanceof BinaryWebSocketFrame)) {
            ctx.fireChannelRead(msg.retain());
            return;
        }

        final ByteBuf byteBuf = msg.content();
        byte[] bytes = new byte[byteBuf.readableBytes()];
        int readerIndex = byteBuf.readerIndex();
        byteBuf.getBytes(readerIndex, bytes);

        final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope =
                PhoneProxyProtocol.PhoneEnvelope.parseFrom(bytes);

        if (StringUtils.isNotBlank(phoneEnvelope.getSession())) {

            final ClientSession clientSession =
                    applicationContext.getClientSessions().get(phoneEnvelope.getSession());

            if (clientSession != null) {

                clientSession.handlePhone(phoneEnvelope);
            }

            ctx.fireChannelRead(msg.retain());
            return;
        }

        for (final PhoneProxyProtocol.PhoneMessage phoneMessage : phoneEnvelope.getMessagesList()) {

            if (phoneMessage
                    .getType()
                    .equals(PhoneProxyProtocol.PhoneMessageType.SIMPLE_REGISTRATION)) {

                for (final String database :
                        (phoneMessage.getSimpleRegistration().getDatabasesList())) {

                    phoneChannels.addDatabase(database, ctx.channel().id());
                }
            } else if (phoneMessage
                    .getType()
                    .equals(PhoneProxyProtocol.PhoneMessageType.OAUTH2_REGISTRATION)) {

                final URI phoneSQLUri = new URI(ApplicationProperties.PHONESQL_URI);
                final URI phoneSQLProxyUri = new URI(ApplicationProperties.PHONESQL_PROXY_URI);

                final Channel channel =
                        applicationContext
                                .mainServerClient
                                .connect(
                                        phoneSQLUri.getHost(),
                                        phoneSQLUri.getPort() < 0
                                                ? (Objects.equals(phoneSQLUri.getScheme(), "https")
                                                        ? 443
                                                        : 80)
                                                : phoneSQLUri.getPort())
                                .sync()
                                .channel();

                final byte[] requestBody =
                        applicationContext
                                .getObjectMapper()
                                .writeValueAsBytes(
                                        new HashMap<>() {
                                            {
                                                put(
                                                        "ids",
                                                        phoneMessage
                                                                .getOauth2Registration()
                                                                .getDatabasesList());
                                            }
                                        });

                final String requestId = UUID.randomUUID().toString();

                DefaultFullHttpRequest request =
                        new DefaultFullHttpRequest(
                                HttpVersion.HTTP_1_1,
                                HttpMethod.POST,
                                "/api/database-list",
                                Unpooled.wrappedBuffer(requestBody));
                request.headers().set(HttpHeaderNames.HOST, phoneSQLProxyUri.getHost());
                request.headers()
                        .set(
                                HttpHeaderNames.AUTHORIZATION,
                                "Bearer " + phoneMessage.getOauth2Registration().getToken());
                request.headers().set("X-Request-ID", requestId);
                request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                request.headers()
                        .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);

                request.headers().set(HttpHeaderNames.CONTENT_LENGTH, requestBody.length);

                applicationContext
                        .getMainServerRequests()
                        .put(requestId, PhoneSQLServerRequestType.OAUTH2_DATABASE_REGISTRATION);
                applicationContext
                        .getPhoneChannels()
                        .getUnverifiedChannels()
                        .put(requestId, ctx.channel().id());

                channel.writeAndFlush(request);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) {
        if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete handshake) {

            final String requestUri = handshake.requestUri();

            if (requestUri.startsWith("/websocket/phone")) {
                applicationContext.getPhoneChannels().add(ctx.channel());
            }
        }
        ctx.fireUserEventTriggered(event);
    }
}
