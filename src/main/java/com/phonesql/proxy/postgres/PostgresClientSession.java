package com.phonesql.proxy.postgres;

import com.phonesql.proxy.ClientSession;
import com.phonesql.proxy.Constants;
import com.phonesql.proxy.api.BasicAuthentication;
import com.phonesql.proxy.postgres.message.DescribePortal;
import com.phonesql.proxy.postgres.message.DescribeStatement;
import com.phonesql.proxy.postgres.message.Execute;
import com.phonesql.proxy.postgres.message.Message;
import com.phonesql.proxy.postgres.message.SimpleQuery;
import com.phonesql.proxy.postgres.protocol.AuthenticationOk;
import com.phonesql.proxy.postgres.protocol.BindComplete;
import com.phonesql.proxy.postgres.protocol.CommandComplete;
import com.phonesql.proxy.postgres.protocol.DataRow;
import com.phonesql.proxy.postgres.protocol.EmptyQueryResponse;
import com.phonesql.proxy.postgres.protocol.ErrorResponse;
import com.phonesql.proxy.postgres.protocol.NoData;
import com.phonesql.proxy.postgres.protocol.ParameterDescription;
import com.phonesql.proxy.postgres.protocol.ParameterStatus;
import com.phonesql.proxy.postgres.protocol.ParseComplete;
import com.phonesql.proxy.postgres.protocol.ReadyForQuery;
import com.phonesql.proxy.postgres.protocol.RowDescription;
import com.phonesql.proxy.postgres.queryhandler.QueryHandler;
import com.phonesql.proxy.postgres.queryhandler.QueryHandlers;
import com.phonesql.proxy.postgres.querytranslator.QueryTranslators;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import proto.PhoneProxyProtocol;

@Setter
@Getter
@RequiredArgsConstructor
public class PostgresClientSession extends ClientSession {

    private String username;

    private Map<String, PreparedStatement> statements = new HashMap<>();
    private Map<String, Portal> portals = new HashMap<>();

    private ByteBuf remainingBuf;
    private Map<String, Message> pendingMessages = new HashMap<>();

    public void handleClient(final ByteBuf byteBuf0) throws IOException, SQLException {

        final ByteBuf byteBuf;

        if (Objects.nonNull(remainingBuf)) {

            byteBuf = remainingBuf;
            byteBuf.writeBytes(byteBuf0, byteBuf0.readerIndex(), byteBuf0.readableBytes());

            remainingBuf = null;
        } else {
            byteBuf = byteBuf0.copy(byteBuf0.readerIndex(), byteBuf0.readableBytes());
        }

        int index = 0;
        while (index < byteBuf.readableBytes()) {

            final char messageCode = (char) byteBuf.getByte(index);

            if (index + 5 > byteBuf.readableBytes()) {

                remainingBuf = byteBuf.copy(index, byteBuf.readableBytes() - index);
                break;
            }
            int messageLength = byteBuf.getInt(index + 1);

            if (index + 1 + messageLength > byteBuf.readableBytes()) {

                remainingBuf = byteBuf.copy(index, byteBuf.readableBytes() - index);
                break;
            }

            final ByteBuf message = byteBuf.copy(index, messageLength + 1);
            index += messageLength + 1;

            switch (messageCode) {
                case 'p':
                    final int length = message.getInt(1);
                    final String password =
                            message.toString(5, length - 5, Charset.defaultCharset());

                    this.authentication = new BasicAuthentication(username, password);
                    startRequest();
                    finishRequest();
                    break;
                case 'Q':
                    startRequest();
                    handleQuery(message);
                    finishRequest();
                    break;
                case 'P':
                    startRequest();
                    handleParse(message);
                    break;
                case 'B':
                    startRequest();
                    handleBind(message);
                    break;
                case 'D':
                    handleDescribe(message);
                    break;
                case 'E':
                    handleExecute(message);
                    break;
                case 'S':
                    if (envelopeBuilder.getMessagesList().isEmpty()) {
                        clientChannel.writeAndFlush(new ReadyForQuery().idleByteBuf(clientChannel));
                    } else {
                        finishRequest();
                    }
                    break;
                case 'F':
                    clientChannel.flush();
                    break;
                case 'X':
                    close();
                    break;
                default:
            }

            message.release();
        }

        byteBuf.release();
    }

    public void handleQuery(final ByteBuf byteBuf) {

        final int length = byteBuf.getInt(1);

        final String sql =
                QueryTranslators.translate(
                        byteBuf.toString(5, length - 5, Charset.defaultCharset()));

        if (sql.isEmpty()) {

            clientChannel.writeAndFlush(new EmptyQueryResponse().byteBuf(clientChannel));
            clientChannel.writeAndFlush(new ReadyForQuery().idleByteBuf(clientChannel));
            return;
        }

        final QueryHandler queryHandler = QueryHandlers.getQuery(sql);

        if (Objects.nonNull(queryHandler)) {

            final List<String> columns = queryHandler.getColumns(sql);

            if (!columns.isEmpty()) {

                clientChannel.writeAndFlush(new RowDescription(columns).byteBuf(clientChannel));
            }

            final List<List<String>> rows = queryHandler.getRows(sql);

            for (final List<String> row : rows) {

                clientChannel.writeAndFlush(
                        new DataRow(row.toArray(new String[0])).byteBuf(clientChannel));
            }

            clientChannel.writeAndFlush(
                    new CommandComplete(sql, rows.size()).byteBuf(clientChannel));

            clientChannel.writeAndFlush(new ReadyForQuery().idleByteBuf(clientChannel));

            return;
        }

        final String id = UUID.randomUUID().toString();

        final PhoneProxyProtocol.ProxyMessage.Builder builder =
                PhoneProxyProtocol.ProxyMessage.newBuilder();
        builder.setType(PhoneProxyProtocol.ProxyMessageType.QUERY);
        builder.setQuery(PhoneProxyProtocol.Query.newBuilder().setId(id).setSql(sql).build());

        envelopeBuilder.addMessages(builder.build());
        pendingMessages.put(id, new SimpleQuery(sql));
    }

    public void handleParse(final ByteBuf byteBuf) {

        final StringBuilder statementNameBuilder = new StringBuilder();
        int index = 5;
        while (byteBuf.getByte(index) != '\0') {
            statementNameBuilder.append((char) byteBuf.getByte(index));
            index++;
        }
        index++;
        final StringBuilder queryBuilder = new StringBuilder();
        while (byteBuf.getByte(index) != '\0') {
            queryBuilder.append((char) byteBuf.getByte(index));
            index++;
        }
        index++;
        final PreparedStatement preparedStatement = new PreparedStatement();
        preparedStatement.setSql(QueryTranslators.translate(queryBuilder.toString()));

        preparedStatement.setQueryHandler(QueryHandlers.getQuery(queryBuilder.toString()));

        short nDataTypes = byteBuf.getShort(index);

        index += 2;
        for (short i = 0; i < nDataTypes; i++) {
            preparedStatement.getOidParams().add(byteBuf.getInt(index));
            index += 4;
        }

        statements.put(statementNameBuilder.toString(), preparedStatement);

        clientChannel.writeAndFlush(new ParseComplete().byteBuf(clientChannel));
    }

    public void handleBind(final ByteBuf byteBuf) throws SQLException, IOException {

        final Portal portal = new Portal();

        final StringBuilder portalNameBuilder = new StringBuilder();
        int index = 5;
        while (byteBuf.getByte(index) != '\0') {
            portalNameBuilder.append((char) byteBuf.getByte(index));
            index++;
        }
        index++;
        final StringBuilder statementNameBuilder = new StringBuilder();
        while (byteBuf.getByte(index) != '\0') {
            statementNameBuilder.append((char) byteBuf.getByte(index));
            index++;
        }
        index++;
        portal.setStatement(statements.get(statementNameBuilder.toString()));

        short nFormatCodes = byteBuf.getShort(index);
        index += 2;
        for (short i = 0; i < nFormatCodes; i++) {
            portal.getParamFormats().add(byteBuf.getShort(index));
            index += 2;
        }

        short nParams = byteBuf.getShort(index);
        portal.setNParams(nParams);

        index += 2;
        for (int i = 0; i < nParams; i++) {
            int paramLen = byteBuf.getInt(index);
            index += 4;
            if (paramLen == -1) {
                portal.getParams().add(null);
            } else if (paramLen == 0) {
                portal.getParams().add("");
            } else {
                byte[] bytes = new byte[paramLen];
                byteBuf.getBytes(index, bytes, 0, paramLen);
                portal.getParams()
                        .add(
                                portal.isTextParam(i)
                                        ? new String(bytes)
                                        : BinaryToStringConverter.convert(
                                                portal.getStatement().getOidParams().get(i),
                                                bytes));
            }
            index += paramLen;
        }

        short nResultFormatCodes = byteBuf.getShort(index);
        index += 2;
        for (short i = 0; i < nResultFormatCodes; i++) {
            portal.getResultFormats().add(byteBuf.getShort(index));
            index += 2;
        }

        portals.put(portalNameBuilder.toString(), portal);
        clientChannel.writeAndFlush(new BindComplete().byteBuf(clientChannel));
    }

    public void handleDescribe(final ByteBuf byteBuf) {

        byte variant = byteBuf.getByte(5);

        final StringBuilder nameBuilder = new StringBuilder();
        int index = 6;
        while (byteBuf.getByte(index) != '\0') {
            nameBuilder.append((char) byteBuf.getByte(index));
            index++;
        }

        if (variant == 'S') {
            final PreparedStatement statement = statements.get(nameBuilder.toString());
            final DescribeStatement describeStatement = new DescribeStatement();
            describeStatement.setStatement(statement);

            if (Objects.nonNull(statement.getQueryHandler())) {

                clientChannel.writeAndFlush(
                        new ParameterDescription(statement.getOidParams()).byteBuf(clientChannel));

                final List<String> columns =
                        statement.getQueryHandler().getColumns(statement.getSql());

                if (columns.isEmpty()) {

                    clientChannel.writeAndFlush(new NoData().byteBuf(clientChannel));
                } else {

                    clientChannel.writeAndFlush(new RowDescription(columns).byteBuf(clientChannel));
                }
                return;
            }

            final String id = UUID.randomUUID().toString();

            final PhoneProxyProtocol.ProxyMessage.Builder builder =
                    PhoneProxyProtocol.ProxyMessage.newBuilder()
                            .setType(PhoneProxyProtocol.ProxyMessageType.PARSE)
                            .setParse(
                                    PhoneProxyProtocol.Parse.newBuilder()
                                            .setId(id)
                                            .setSql(statement.getSql()));

            pendingMessages.put(id, describeStatement);
            envelopeBuilder.addMessages(builder.build());

        } else {
            final Portal portal = portals.get(nameBuilder.toString());
            final DescribePortal describePortal = new DescribePortal();
            describePortal.setPortal(portal);

            if (Objects.nonNull(portal.getStatement().getQueryHandler())) {

                final List<String> columns =
                        portal.getStatement()
                                .getQueryHandler()
                                .getColumns(portal.getStatement().getSql());

                if (columns.isEmpty()) {

                    clientChannel.writeAndFlush(new NoData().byteBuf(clientChannel));
                } else {

                    clientChannel.writeAndFlush(new RowDescription(columns).byteBuf(clientChannel));
                }
                return;
            }

            final String id = UUID.randomUUID().toString();
            final PhoneProxyProtocol.ProxyMessage.Builder builder =
                    PhoneProxyProtocol.ProxyMessage.newBuilder()
                            .setType(PhoneProxyProtocol.ProxyMessageType.PARSE)
                            .setParse(
                                    PhoneProxyProtocol.Parse.newBuilder()
                                            .setId(id)
                                            .setSql(portal.getStatement().getSql()));

            envelopeBuilder.addMessages(builder.build());
            pendingMessages.put(id, describePortal);
        }
    }

    public void handleParseResult(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage) {

        final Message message = pendingMessages.get(phoneMessage.getParseResult().getId());

        if (message instanceof DescribePortal portal) {

            if (phoneMessage.getParseResult().getColumnsList().isEmpty()) {

                clientChannel.writeAndFlush(new NoData().byteBuf(clientChannel));

            } else {

                clientChannel.writeAndFlush(
                        new RowDescription(phoneMessage.getParseResult().getColumnsList())
                                .byteBuf(clientChannel));
            }

            pendingMessages.remove(phoneMessage.getParseResult().getId());
        }
    }

    public void handleResultSet(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage) {

        final Message message = pendingMessages.remove(phoneMessage.getResultSet().getId());

        if (message instanceof SimpleQuery query) {

            clientChannel.writeAndFlush(
                    new RowDescription(phoneMessage.getResultSet().getColumnsList())
                            .byteBuf(clientChannel));

            for (final PhoneProxyProtocol.Row row : phoneMessage.getResultSet().getRowsList()) {

                clientChannel.writeAndFlush(
                        new DataRow(row.getColumnsList().toArray(new String[0]))
                                .byteBuf(clientChannel));
            }

            clientChannel.writeAndFlush(
                    new CommandComplete(query.getSql(), phoneMessage.getResultSet().getRowsCount())
                            .byteBuf(clientChannel));

        } else if (message instanceof Execute execute) {

            for (final PhoneProxyProtocol.Row row : phoneMessage.getResultSet().getRowsList()) {

                clientChannel.writeAndFlush(
                        new DataRow(row.getColumnsList().toArray(new String[0]))
                                .byteBuf(clientChannel));
            }

            clientChannel.writeAndFlush(
                    new CommandComplete(
                                    execute.getPortal().getStatement().getSql(),
                                    phoneMessage.getResultSet().getRowsCount())
                            .byteBuf(clientChannel));
        }
    }

    @Override
    public void handleError(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage) {

        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.putSeverity("ERROR");
        errorResponse.putMessage(phoneMessage.getError().getCode());
        errorResponse.putDetail(phoneMessage.getError().getMessage());
        clientChannel.writeAndFlush(errorResponse.byteBuf(clientChannel));
    }

    public void handleExecute(final ByteBuf byteBuf) {

        int index = 5;
        final StringBuilder portalNameBuilder = new StringBuilder();

        while (byteBuf.getByte(index) != '\0') {
            portalNameBuilder.append((char) byteBuf.getByte(index));
            index++;
        }
        int maxRows = byteBuf.getInt(index);

        final Portal portal = portals.get(portalNameBuilder.toString());

        if (Objects.nonNull(portal.getStatement().getQueryHandler())) {

            final List<List<String>> rows =
                    portal.getStatement().getQueryHandler().getRows(portal.getStatement().getSql());

            for (final List<String> row : rows) {

                clientChannel.writeAndFlush(
                        new DataRow(row.toArray(new String[0])).byteBuf(clientChannel));
            }

            clientChannel.writeAndFlush(
                    new CommandComplete(portal.getStatement().getSql(), rows.size())
                            .byteBuf(clientChannel));

            return;
        }

        final String id = UUID.randomUUID().toString();

        final PhoneProxyProtocol.ProxyMessage.Builder builder =
                PhoneProxyProtocol.ProxyMessage.newBuilder()
                        .setType(PhoneProxyProtocol.ProxyMessageType.QUERY)
                        .setQuery(
                                PhoneProxyProtocol.Query.newBuilder()
                                        .setId(id)
                                        .setSql(portal.getStatement().getSql())
                                        .addAllParams(portal.getParams()));

        envelopeBuilder.addMessages(builder.build());
        pendingMessages.put(id, new Execute(portal, maxRows));
    }

    @Override
    public void handleAuthentication(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage) {

        if (phoneMessage.getAuthentication().getSuccess()) {

            authenticated = true;

            clientChannel.writeAndFlush(new AuthenticationOk().byteBuf(clientChannel));

            clientChannel.writeAndFlush(
                    new ParameterStatus(
                                    Constants.SERVER_VERSION_PARAM_NAME,
                                    Constants.SERVER_VERSION_PARAM_VALUE)
                            .byteBuf(clientChannel));
            clientChannel.writeAndFlush(
                    new ParameterStatus(
                                    Constants.CLIENT_ENCODING_PARAM_NAME,
                                    Constants.CLIENT_ENCODING_PARAM_VALUE)
                            .byteBuf(clientChannel));
        } else {
            handleUnAuthenticated(phoneEnvelope, phoneMessage);
        }
    }

    @Override
    public void handleUnAuthenticated(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage) {

        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.putSeverity("FATAL");
        errorResponse.putMessage("password authentication failed for user " + username);
        clientChannel.writeAndFlush(errorResponse.byteBuf(clientChannel));

        close();
    }

    @Override
    public void handleUnauthorized(
            final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope,
            final PhoneProxyProtocol.PhoneMessage phoneMessage) {

        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.putSeverity("ERROR");
        errorResponse.putMessage("Permission denied");
        clientChannel.writeAndFlush(errorResponse.byteBuf(clientChannel));
    }

    public void finishResponse(final PhoneProxyProtocol.PhoneEnvelope phoneEnvelope) {

        clientChannel.writeAndFlush(new ReadyForQuery().idleByteBuf(clientChannel));
    }
}
