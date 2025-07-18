package com.phonesql.proxy.websocket;

import static com.phonesql.proxy.CookieUtils.getSessionFromCookie;

import com.phonesql.proxy.ApplicationContext;
import com.phonesql.proxy.ClientSession;
import com.phonesql.proxy.Constants;
import com.phonesql.proxy.PreAuthorization;
import com.phonesql.proxy.api.BasicAuthentication;
import com.phonesql.proxy.api.OAuth2Authentication;
import com.phonesql.proxy.api.Query;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;
import java.util.Objects;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final ApplicationContext applicationContext;

    public WebSocketClientHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {

        if (!(frame instanceof TextWebSocketFrame)
                || Objects.isNull(
                        applicationContext.getClientChannels().find(ctx.channel().id()))) {
            ctx.fireChannelRead(frame.retain());
            return;
        }

        final String session =
                ctx.channel()
                        .attr(AttributeKey.valueOf(Constants.SESSION_ATTR_KEY))
                        .get()
                        .toString();

        final ClientSession clientSession = applicationContext.getClientSessions().get(session);

        if (clientSession instanceof WebSocketClientSession webSocketClientSession) {

            final Query query =
                    applicationContext
                            .getObjectMapper()
                            .readValue(((TextWebSocketFrame) frame).text(), Query.class);

            webSocketClientSession.startRequest();
            webSocketClientSession.query(query);
            webSocketClientSession.finishRequest();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) {
        if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete handshake) {

            final String requestUri = handshake.requestUri();

            if (requestUri.startsWith("/websocket/client")) {

                applicationContext.getClientChannels().add(ctx.channel());

                final String session = getSessionFromCookie(handshake.requestHeaders());
                ctx.channel().attr(AttributeKey.valueOf(Constants.SESSION_ATTR_KEY)).set(session);

                final PreAuthorization preAuthorization =
                        applicationContext.getPreAuthorizations().get(session);

                if (Objects.isNull(preAuthorization)) {
                    return;
                }

                final WebSocketClientSession webSocketClientSession = new WebSocketClientSession();
                webSocketClientSession.setObjectMapper(applicationContext.getObjectMapper());
                webSocketClientSession.setSession(session);
                webSocketClientSession.setDatabase(preAuthorization.getDatabase());

                if (preAuthorization.getType() == PreAuthorization.Type.USERNAME_PASSWORD) {

                    webSocketClientSession.setAuthentication(
                            new BasicAuthentication(
                                    preAuthorization.getUsername(),
                                    preAuthorization.getPassword()));

                } else {

                    webSocketClientSession.setAuthentication(
                            new OAuth2Authentication(preAuthorization.getToken()));
                }
                webSocketClientSession.setPhoneChannel(
                        applicationContext
                                .getPhoneChannels()
                                .findByDatabase(preAuthorization.getDatabase()));
                webSocketClientSession.setClientChannel(ctx.channel());

                applicationContext.getClientSessions().put(session, webSocketClientSession);
            }
        }
        ctx.fireUserEventTriggered(event);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
