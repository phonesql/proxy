package com.phonesql.proxy.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonesql.proxy.ClientSession;
import com.phonesql.proxy.api.Error;
import com.phonesql.proxy.api.ResponseBody;
import com.phonesql.proxy.api.ResultSet;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import proto.PhoneProxyProtocol;

@Setter
@Getter
@RequiredArgsConstructor
public class WebSocketClientSession extends ClientSession {

    protected ObjectMapper objectMapper;
    protected ResponseBody.ResponseBodyBuilder responseBodyBuilder;

    @Override
    public void startResponse(PhoneProxyProtocol.PhoneEnvelope phoneEnvelope) {

        responseBodyBuilder = ResponseBody.builder();
    }

    public void finishResponse(final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope) {

        try {
            final String frameText = objectMapper.writeValueAsString(responseBodyBuilder.build());

            clientChannel.writeAndFlush(new TextWebSocketFrame(frameText));

        } catch (final JsonProcessingException e) {

            clientChannel.writeAndFlush(
                    new CloseWebSocketFrame(WebSocketCloseStatus.INTERNAL_SERVER_ERROR));

            close();
        }
    }

    @Override
    public void handleUnAuthenticated(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage) {

        clientChannel.writeAndFlush(new CloseWebSocketFrame(WebSocketCloseStatus.POLICY_VIOLATION));

        close();
    }

    @Override
    public void handleUnauthorized(
            PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            PhoneProxyProtocol.PhoneMessage phoneMessage) {

        clientChannel.writeAndFlush(new CloseWebSocketFrame(WebSocketCloseStatus.POLICY_VIOLATION));

        close();
    }

    @Override
    public void handleParseResult(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage) {

        responseBodyBuilder.resultSet(
                ResultSet.builder()
                        .columns(phoneMessage.getParseResult().getColumnsList())
                        .build());
    }

    @Override
    public void handleResultSet(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage) {

        final PhoneProxyProtocol.ResultSet resultSet = phoneMessage.getResultSet();

        responseBodyBuilder.resultSet(
                ResultSet.builder()
                        .id(resultSet.getId())
                        .columns(resultSet.getColumnsList())
                        .rows(
                                resultSet.getRowsList().stream()
                                        .map(PhoneProxyProtocol.Row::getColumnsList)
                                        .collect(Collectors.toList()))
                        .build());
    }

    @Override
    public void handleError(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage) {

        responseBodyBuilder.error(
                Error.builder()
                        .code(phoneMessage.getError().getCode())
                        .message(phoneMessage.getError().getMessage())
                        .build());
    }
}
