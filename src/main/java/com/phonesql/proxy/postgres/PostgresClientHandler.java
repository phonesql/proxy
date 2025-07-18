package com.phonesql.proxy.postgres;

import com.phonesql.proxy.ClientSession;
import com.phonesql.proxy.Constants;
import com.phonesql.proxy.phone.PhoneChannels;
import com.phonesql.proxy.postgres.protocol.AuthenticationCleartextPassword;
import com.phonesql.proxy.postgres.protocol.ErrorResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class PostgresClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Integer STARTUP_MESSAGE_CODE = 196608;

    private final ConcurrentMap<String, ClientSession> sessions;
    private final PhoneChannels phoneChannels;

    public PostgresClientHandler(
            ConcurrentMap<String, ClientSession> sessions, PhoneChannels phoneChannels) {
        this.sessions = sessions;
        this.phoneChannels = phoneChannels;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf)
            throws IOException, SQLException {

        final String session =
                (String) ctx.channel().attr(AttributeKey.valueOf(Constants.SESSION_ATTR_KEY)).get();

        if (Objects.isNull(session)) {
            while (byteBuf.isReadable()) {

                if (!byteBuf.isReadable(4)) {

                    ctx.channel()
                            .attr(AttributeKey.valueOf(Constants.PENDING_BYTE_BUF_ATTR_KEY))
                            .set(byteBuf.copy());
                    break;
                }

                int len = byteBuf.getInt(0);

                if (!byteBuf.isReadable(len)) {

                    ctx.channel()
                            .attr(AttributeKey.valueOf(Constants.PENDING_BYTE_BUF_ATTR_KEY))
                            .set(byteBuf.copy());
                    break;
                }

                int code = byteBuf.getInt(4);

                if (code == STARTUP_MESSAGE_CODE) {

                    handleStartupMessage(ctx.channel(), byteBuf);
                }

                byteBuf.readerIndex(byteBuf.readerIndex() + len);
            }
        } else {
            ((PostgresClientSession) sessions.get(session)).handleClient(byteBuf);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.fireChannelInactive();
    }

    private void handleStartupMessage(final Channel channel, final ByteBuf byteBuf) {

        final int len = byteBuf.getInt(0);
        final String str = byteBuf.toString(8, len - 8, Charset.defaultCharset());

        final Map<String, String> params = parse(str);
        final String user = params.get("user");
        final String database = params.get("database");

        final Channel phoneChannel = phoneChannels.findByDatabase(database);

        if (Objects.isNull(phoneChannel)) {

            final ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.putSeverity("ERROR");
            errorResponse.putMessage("database " + database + " not active");

            channel.writeAndFlush(new ErrorResponse().byteBuf(channel));
            return;
        }

        final String session = UUID.randomUUID().toString();

        channel.attr(AttributeKey.valueOf(Constants.SESSION_ATTR_KEY)).set(session);

        PostgresClientSession postgresClientSession = (PostgresClientSession) sessions.get(session);

        if (postgresClientSession == null) {
            postgresClientSession = new PostgresClientSession();
            postgresClientSession.setSession(session);
            postgresClientSession.setDatabase(database);
            postgresClientSession.setUsername(user);

            sessions.put(session, postgresClientSession);
        }
        postgresClientSession.setPhoneChannel(phoneChannel);
        postgresClientSession.setClientChannel(channel);

        channel.writeAndFlush(new AuthenticationCleartextPassword().byteBuf(channel));
    }

    private Map<String, String> parse(final String parameters) {

        final Map<String, String> map = new HashMap<>();

        final String[] parts = parameters.split("\\u0000");
        for (int i = 0; i < parts.length / 2; i++) {
            map.put(parts[2 * i], parts[2 * i + 1]);
        }

        return map;
    }
}
