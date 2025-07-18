package com.phonesql.proxy;

import com.phonesql.proxy.api.Authentication;
import com.phonesql.proxy.api.BasicAuthentication;
import com.phonesql.proxy.api.OAuth2Authentication;
import com.phonesql.proxy.api.Query;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import java.util.Collections;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import proto.PhoneProxyProtocol;

@Getter
@Setter
public abstract class ClientSession {

    protected String session;
    protected String database;

    protected Channel phoneChannel;
    protected Channel clientChannel;

    protected Authentication authentication;

    protected boolean authenticated;
    protected boolean closed;

    protected Integer sequenceNumber = 0;

    protected PhoneProxyProtocol.ProxyEnvelope.Builder envelopeBuilder;

    public void startRequest() {

        if (Objects.nonNull(envelopeBuilder)) {
            return;
        }

        envelopeBuilder = PhoneProxyProtocol.ProxyEnvelope.newBuilder();
        envelopeBuilder.setSession(session);
        envelopeBuilder.setSequenceNumber(++sequenceNumber);

        if (authenticated) {
            return;
        }

        final PhoneProxyProtocol.ProxyMessage.Builder builder =
                PhoneProxyProtocol.ProxyMessage.newBuilder();

        if (authentication instanceof BasicAuthentication) {

            builder.setType(PhoneProxyProtocol.ProxyMessageType.USERNAME_PASSWORD_LOGIN);
            builder.setUsernamePasswordLogin(
                    PhoneProxyProtocol.UsernamePasswordLogin.newBuilder()
                            .setDatabase(database)
                            .setUsername(((BasicAuthentication) authentication).getUsername())
                            .setPassword(((BasicAuthentication) authentication).getPassword())
                            .build());

        } else {

            builder.setType(PhoneProxyProtocol.ProxyMessageType.OAUTH2_LOGIN);
            builder.setOauth2Login(
                    PhoneProxyProtocol.OAuth2Login.newBuilder()
                            .setDatabase(database)
                            .setToken(((OAuth2Authentication) authentication).getJwt().serialize())
                            .build());
        }
        envelopeBuilder.addMessages(builder.build());
    }

    public void startResponse(final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope) {}

    public void query(final Query query) {

        final PhoneProxyProtocol.ProxyMessage.Builder builder =
                PhoneProxyProtocol.ProxyMessage.newBuilder();
        builder.setType(PhoneProxyProtocol.ProxyMessageType.QUERY);
        builder.setQuery(
                PhoneProxyProtocol.Query.newBuilder()
                        .setId(query.getId())
                        .setSql(query.getSql())
                        .addAllParams(
                                query.getParams() == null
                                        ? Collections.emptyList()
                                        : query.getParams()));

        envelopeBuilder.addMessages(builder.build());
    }

    public void finishRequest() {

        if (!envelopeBuilder.getMessagesList().isEmpty()) {

            phoneChannel.writeAndFlush(
                    new BinaryWebSocketFrame(
                            Unpooled.wrappedBuffer(envelopeBuilder.build().toByteArray())));
        }
        envelopeBuilder = null;
    }

    public void finishResponse(final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope) {}

    public void handlePhone(final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope) {

        startResponse(phoneEnvelope);

        for (final PhoneProxyProtocol.PhoneMessage phoneMessage : phoneEnvelope.getMessagesList()) {

            if (closed) {
                return;
            }

            switch (phoneMessage.getType()) {
                case AUTHENTICATION:
                    handleAuthentication(phoneEnvelope, phoneMessage);
                    break;
                case UNAUTHORIZED:
                    handleUnauthorized(phoneEnvelope, phoneMessage);
                    break;
                case PARSE_RESULT:
                    handleParseResult(phoneEnvelope, phoneMessage);
                    break;
                case RESULT_SET:
                    handleResultSet(phoneEnvelope, phoneMessage);
                    break;
                case ERROR:
                    handleError(phoneEnvelope, phoneMessage);
                    break;
                default:
            }
        }
        finishResponse(phoneEnvelope);
    }

    public void handleAuthentication(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage) {

        if (phoneMessage.getAuthentication().getSuccess()) {
            authenticated = true;

        } else {
            handleUnAuthenticated(phoneEnvelope, phoneMessage);
        }
    }

    public abstract void handleUnAuthenticated(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage);

    public abstract void handleUnauthorized(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage);

    public abstract void handleParseResult(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage);

    public abstract void handleResultSet(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage);

    public abstract void handleError(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage);

    public void close() {

        closed = true;
        clientChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
}
