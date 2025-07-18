package com.phonesql.proxy.postgres.querytranslator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FetchRowsTest {

    @Test
    public void run() {

        assertEquals("LIMIT 10", new FetchRows().translate("FETCH FIRST 10 ROWS ONLY"));
        assertEquals(
                "SELECT * FROM TEST LIMIT 10",
                new FetchRows().translate("SELECT * FROM TEST FETCH FIRST 10 ROWS ONLY"));
        assertEquals(
                "SELECT * FROM TEST LIMIT 10",
                new FetchRows().translate("SELECT * FROM TEST FETCH NEXT 10 ROWS ONLY"));
        assertEquals(
                "LIMIT 10",
                new FetchRows()
                        .translate(" FETCH    FIRST    10   ROWS    ONLY ")
                        .trim()
                        .replaceAll("\\s+", " "));
        assertEquals(
                "SELECT * FROM TEST LIMIT 10 OFFSET 10",
                new FetchRows().translate("SELECT * FROM TEST FETCH FIRST 10 ROWS ONLY OFFSET 10"));

        assertEquals(
                "select * from test LIMIT 10 offset 10",
                new FetchRows().translate("select * from test fetch first 10 rows only offset 10"));

        assertEquals(
                "select t1_0.name from tracks t1_0 where t1_0.mediaTypeId=$1 order by 1 desc LIMIT"
                        + " $2",
                new FetchRows()
                        .translate(
                                "select t1_0.name from tracks t1_0 where t1_0.mediaTypeId=$1 order"
                                        + " by 1 desc fetch first $2 rows only"));
    }
}
