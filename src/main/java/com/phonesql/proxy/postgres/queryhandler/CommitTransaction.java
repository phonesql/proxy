package com.phonesql.proxy.postgres.queryhandler;

import java.util.List;
import java.util.regex.Pattern;

public class CommitTransaction implements QueryHandler {

    final Pattern pattern = Pattern.compile("^COMMIT.*", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean isMatch(String sql) {
        return pattern.matcher(sql).matches();
    }

    @Override
    public List<String> getColumns(String sql) {
        return List.of();
    }

    @Override
    public List<List<String>> getRows(String sql) {
        return List.of();
    }
}
