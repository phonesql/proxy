package com.phonesql.proxy.postgres;

import java.io.IOException;
import java.sql.SQLException;
import java.util.TimeZone;
import org.postgresql.core.Encoding;
import org.postgresql.core.Oid;
import org.postgresql.geometric.PGbox;
import org.postgresql.geometric.PGpoint;
import org.postgresql.jdbc.PgArray;
import org.postgresql.jdbc.TimestampUtils;
import org.postgresql.jdbc.UUIDArrayAssistant;
import org.postgresql.util.ByteConverter;
import org.postgresql.util.PGbytea;

public class BinaryToStringConverter {

    public static String convert(final int oid, final byte[] bytes)
            throws SQLException, IOException {

        final TimestampUtils ts = new TimestampUtils(true, TimeZone::getDefault);

        switch (oid) {
            case Oid.INT2:
                final short s = ByteConverter.int2(bytes, 0);
                return Short.toString(s);
            case Oid.INT4:
                final int i = ByteConverter.int4(bytes, 0);
                return Integer.toString(i);
            case Oid.INT8:
                final long l = ByteConverter.int8(bytes, 0);
                return Long.toString(l);
            case Oid.FLOAT4:
                final float f = ByteConverter.float4(bytes, 0);
                if (Float.isNaN(f)) {
                    return "('NaN'::real)";
                }
                return Float.toString(f);
            case Oid.FLOAT8:
                final double d = ByteConverter.float8(bytes, 0);
                if (Double.isNaN(d)) {
                    return "('NaN'::double precision)";
                }
                return Double.toString(d);
            case Oid.NUMERIC:
            case Oid.MONEY:
                final Number n = ByteConverter.numeric(bytes);
                if (n instanceof Double) {
                    assert ((Double) n).isNaN();
                    return "('NaN'::numeric)";
                }
                return n.toString();

            case Oid.BOOL:
            case Oid.BIT:
                return Boolean.toString(ByteConverter.bool(bytes, 0));
            case Oid.BYTEA:
                return PGbytea.toPGString(bytes);
            case Oid.OID:
                return Integer.toString(ByteConverter.int4(bytes, 0));
            case Oid.UUID:
                return new UUIDArrayAssistant().buildElement(bytes, 0, 16).toString();
            case Oid.POINT:
                final PGpoint pgPoint = new PGpoint();
                pgPoint.setByteValue(bytes, 0);
                return pgPoint.toString();
            case Oid.BOX:
                final PGbox pgBox = new PGbox();
                pgBox.setByteValue(bytes, 0);
                return pgBox.toString();

            case Oid.TIME:
                return ts.toString(ts.toLocalTimeBin(bytes));
            case Oid.TIMETZ:
                return ts.toStringOffsetTimeBin(bytes);
            case Oid.DATE:
                return ts.toString(ts.toLocalDateBin(bytes));
            case Oid.TIMESTAMP:
                return ts.toString(ts.toLocalDateTimeBin(bytes));
            case Oid.TIMESTAMPTZ:
                return ts.toStringOffsetDateTime(bytes);

            case Oid.UNSPECIFIED:
            case Oid.TEXT:
            case Oid.VARCHAR:
            case Oid.BPCHAR:
            case Oid.NAME:
            case Oid.INTERVAL:
            case Oid.CHAR:
            case Oid.VARBIT:
            case Oid.XML:
            case Oid.JSONB:
            case Oid.JSON:
            case Oid.LINE:
            case Oid.LSEG:
            case Oid.PATH:
            case Oid.POLYGON:
            case Oid.CIRCLE:
            case Oid.CIDR:
            case Oid.INET:
            case Oid.MACADDR:
            case Oid.MACADDR8:
            case Oid.TSVECTOR:
            case Oid.TSQUERY:
                return Encoding.defaultEncoding().decode(bytes);

            case Oid.INT2_ARRAY:
            case Oid.INT4_ARRAY:
            case Oid.INT8_ARRAY:
            case Oid.TEXT_ARRAY:
            case Oid.NUMERIC_ARRAY:
            case Oid.FLOAT4_ARRAY:
            case Oid.FLOAT8_ARRAY:
            case Oid.BOOL_ARRAY:
            case Oid.DATE_ARRAY:
            case Oid.TIME_ARRAY:
            case Oid.TIMETZ_ARRAY:
            case Oid.TIMESTAMP_ARRAY:
            case Oid.TIMESTAMPTZ_ARRAY:
            case Oid.BYTEA_ARRAY:
            case Oid.VARCHAR_ARRAY:
            case Oid.OID_ARRAY:
            case Oid.BPCHAR_ARRAY:
            case Oid.MONEY_ARRAY:
            case Oid.NAME_ARRAY:
            case Oid.BIT_ARRAY:
            case Oid.INTERVAL_ARRAY:
            case Oid.CHAR_ARRAY:
            case Oid.VARBIT_ARRAY:
            case Oid.UUID_ARRAY:
            case Oid.XML_ARRAY:
            case Oid.POINT_ARRAY:
            case Oid.BOX_ARRAY:
            case Oid.JSONB_ARRAY:
            case Oid.JSON_ARRAY:
            case Oid.REF_CURSOR_ARRAY:
                return new PgArray(null, oid, bytes).toString();
            case Oid.VOID:
            case Oid.REF_CURSOR:
            default:
                return "?";
        }
    }
}
