package com.phonesql.proxy.api;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonesql.proxy.ClientSession;
import com.phonesql.proxy.Constants;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import proto.PhoneProxyProtocol;

@Setter
@Getter
@RequiredArgsConstructor
public class ApiClientSession extends ClientSession {

    protected ObjectMapper objectMapper;
    private ResponseBody.ResponseBodyBuilder responseBodyBuilder;

    @Override
    public void startResponse(final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope) {

        responseBodyBuilder = ResponseBody.builder();
    }

    @Override
    public void finishResponse(final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope) {

        FullHttpResponse response;

        try {
            response =
                    new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1,
                            HttpResponseStatus.OK,
                            Unpooled.wrappedBuffer(
                                    objectMapper.writeValueAsBytes(responseBodyBuilder.build())));
        } catch (final JsonProcessingException e) {
            response =
                    new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1,
                            HttpResponseStatus.INTERNAL_SERVER_ERROR,
                            Unpooled.EMPTY_BUFFER);
        }

        response.headers()
                .set(CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                .setInt(CONTENT_LENGTH, response.content().readableBytes());

        response.headers().set(Constants.SESSION_ATTR_KEY, session);

        clientChannel.writeAndFlush(response);
    }

    @Override
    public void handleUnAuthenticated(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage) {

        final FullHttpResponse response =
                new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.UNAUTHORIZED,
                        Unpooled.EMPTY_BUFFER);

        response.headers().setInt(CONTENT_LENGTH, 0);
        response.headers().add(HttpHeaderNames.WWW_AUTHENTICATE, "");
        clientChannel.writeAndFlush(response);

        close();
    }

    @Override
    public void handleUnauthorized(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage) {

        final FullHttpResponse response =
                new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN, Unpooled.EMPTY_BUFFER);

        response.headers().setInt(CONTENT_LENGTH, 0);
        response.headers().set(Constants.SESSION_ATTR_KEY, session);
        clientChannel.writeAndFlush(response);

        close();
    }

    @Override
    public void handleParseResult(
            PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            PhoneProxyProtocol.PhoneMessage phoneMessage) {}

    public void handleResultSet(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage) {

        responseBodyBuilder.resultSet(
                ResultSet.builder()
                        .id(phoneMessage.getResultSet().getId())
                        .columns(phoneMessage.getResultSet().getColumnsList())
                        .rows(
                                phoneMessage.getResultSet().getRowsList().stream()
                                        .map((PhoneProxyProtocol.Row::getColumnsList))
                                        .collect(Collectors.toList()))
                        .build());
    }

    public void handleError(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage) {

        responseBodyBuilder.error(Error.builder().code(phoneMessage.getError().getCode()).build());
    }

    public void close() {

        closed = true;

        clientChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
}
