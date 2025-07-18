package com.phonesql.proxy.postgres.querytranslator;

public interface QueryTranslator {

    int order();

    String translate(final String sql);
}
