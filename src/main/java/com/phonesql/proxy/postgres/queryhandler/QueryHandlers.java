package com.phonesql.proxy.postgres.queryhandler;

public class QueryHandlers {

    static QueryHandler[] queryHandlers;

    static {
        queryHandlers =
                new QueryHandler[] {
                    new ShowTransactionIsolationLevel(),
                    new BeginTransaction(),
                    new CommitTransaction(),
                    new SetParameter(),
                    new PgGetKeywords()
                };
    }

    public static QueryHandler getQuery(final String sql) {

        for (final QueryHandler queryHandler : queryHandlers) {

            if (queryHandler.isMatch(sql)) {
                return queryHandler;
            }
        }
        return null;
    }
}
