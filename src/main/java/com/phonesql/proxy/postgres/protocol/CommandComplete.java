package com.phonesql.proxy.postgres.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommandComplete {

    private static final String WHITESPACE = " ";

    private static final String INSERT = "INSERT";
    private static final String DELETE = "DELETE";
    private static final String UPDATE = "UPDATE";
    private static final String MERGE = "MERGE";
    private static final String SELECT = "SELECT";
    private static final String CREATE_TABLE_AS = "CREATE TABLE AS";
    private static final String MOVE = "MOVE";
    private static final String FETCH = "FETCH";
    private static final String COPY = "COPY";
    private static final String BEGIN = "BEGIN";
    private static final String SET = "SET";

    private final String sql;
    private final int rows;

    public ByteBuf byteBuf(final Channel channel) {

        final String tag = getTag();

        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeByte('C');
        byteBuf.writeInt(4 + tag.getBytes().length + 1);
        byteBuf.writeBytes(tag.getBytes());
        byteBuf.writeByte('\0');

        return byteBuf;
    }

    public String getTag() {

        if (sql.substring(0, Math.min(INSERT.length(), sql.length())).equalsIgnoreCase(INSERT)) {
            return INSERT + WHITESPACE + 0 + WHITESPACE + rows;
        }

        if (sql.substring(0, Math.min(DELETE.length(), sql.length())).equalsIgnoreCase(DELETE)) {
            return DELETE + WHITESPACE + rows;
        }

        if (sql.substring(0, Math.min(UPDATE.length(), sql.length())).equalsIgnoreCase(UPDATE)) {
            return UPDATE + WHITESPACE + rows;
        }

        if (sql.substring(0, Math.min(MERGE.length(), sql.length())).equalsIgnoreCase(MERGE)) {
            return MERGE + WHITESPACE + rows;
        }

        if (sql.substring(0, Math.min(SELECT.length(), sql.length())).equalsIgnoreCase(SELECT)) {
            return SELECT + WHITESPACE + rows;
        }

        if (sql.substring(0, Math.min(CREATE_TABLE_AS.length(), sql.length()))
                .equalsIgnoreCase(CREATE_TABLE_AS)) {
            return CREATE_TABLE_AS + WHITESPACE + rows;
        }

        if (sql.substring(0, Math.min(MOVE.length(), sql.length())).equalsIgnoreCase(MOVE)) {
            return MOVE + WHITESPACE + rows;
        }

        if (sql.substring(0, Math.min(FETCH.length(), sql.length())).equalsIgnoreCase(FETCH)) {
            return FETCH + WHITESPACE + rows;
        }

        if (sql.substring(0, Math.min(COPY.length(), sql.length())).equalsIgnoreCase(COPY)) {
            return COPY + WHITESPACE + rows;
        }

        if (sql.substring(0, Math.min(BEGIN.length(), sql.length())).equalsIgnoreCase(BEGIN)) {
            return BEGIN;
        }

        if (sql.substring(0, Math.min(SET.length(), sql.length())).equalsIgnoreCase(SET)) {
            return SET + WHITESPACE + rows;
        }

        return WHITESPACE;
    }
}
