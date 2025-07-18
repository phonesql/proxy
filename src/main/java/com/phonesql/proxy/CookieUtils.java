package com.phonesql.proxy;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

public class CookieUtils {

    public static String getSessionFromCookie(final HttpHeaders headers) {

        final String cookieString = headers.get("Cookie");

        if (cookieString == null) {
            return null;
        }
        final Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieString);

        for (final Cookie cookie : cookies) {

            if (Constants.SESSION_ATTR_KEY.equalsIgnoreCase(cookie.name())) {
                return new String(
                        Base64.getDecoder()
                                .decode(cookie.value().getBytes(StandardCharsets.UTF_8)));
            }
        }

        return null;
    }
}
