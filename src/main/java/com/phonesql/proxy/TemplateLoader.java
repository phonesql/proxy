package com.phonesql.proxy;

import com.samskivert.mustache.Mustache;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class TemplateLoader implements Mustache.TemplateLoader {

    final Map<String, String> templateCache = new HashMap<>();

    @Override
    public Reader getTemplate(final String name) throws Exception {
        if (templateCache.containsKey(name)) {
            return new StringReader(templateCache.get(name));
        }

        byte[] data;
        try (InputStream in = getClass().getResourceAsStream("/templates/" + name + ".mustache")) {
            assert in != null;
            data = in.readAllBytes();
        }

        return new StringReader(new String(data));
    }
}
