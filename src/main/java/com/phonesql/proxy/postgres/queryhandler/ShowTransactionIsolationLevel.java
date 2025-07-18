package com.phonesql.proxy.postgres.queryhandler;

import java.util.List;

public class ShowTransactionIsolationLevel implements QueryHandler {

    private static final String SQL = "SHOW TRANSACTION ISOLATION LEVEL";

    @Override
    public boolean isMatch(String sql) {
        return SQL.equalsIgnoreCase(sql);
    }

    @Override
    public List<String> getColumns(String sql) {
        return List.of("transaction_isolation");
    }

    @Override
    public List<List<String>> getRows(String sql) {

        return List.of(List.of("SERIALIZABLE"));
    }
}
