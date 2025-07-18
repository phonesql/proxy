package com.phonesql.proxy.postgres.querytranslator;

import java.util.Arrays;
import java.util.Comparator;

public class QueryTranslators {

    static QueryTranslator[] queryTranslators;

    static {
        queryTranslators = new QueryTranslator[] {new FetchRows()};
        Arrays.sort(queryTranslators, Comparator.comparing(QueryTranslator::order));
    }

    public static String translate(final String sql) {

        String translation = sql;

        for (final QueryTranslator queryTranslator : queryTranslators) {

            translation = queryTranslator.translate(translation);
        }
        return translation;
    }
}
