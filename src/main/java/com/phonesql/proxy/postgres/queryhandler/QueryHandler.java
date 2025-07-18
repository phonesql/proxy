package com.phonesql.proxy.postgres.queryhandler;

import java.util.List;

public interface QueryHandler {

    boolean isMatch(final String sql);

    List<String> getColumns(final String sql);

    List<List<String>> getRows(final String sql);
}
