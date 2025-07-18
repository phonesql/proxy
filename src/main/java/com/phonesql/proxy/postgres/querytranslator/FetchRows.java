package com.phonesql.proxy.postgres.querytranslator;

import java.util.regex.Pattern;

public class FetchRows implements QueryTranslator {

    final Pattern pattern =
            Pattern.compile(
                    "FETCH\\s*(?:FIRST|NEXT)\\s*(.*?)\\s*ROWS\\s*ONLY", Pattern.CASE_INSENSITIVE);

    @Override
    public int order() {
        return 0;
    }

    @Override
    public String translate(final String sql) {

        return pattern.matcher(sql).replaceFirst("LIMIT $1");
    }
}
