package com.phonesql.proxy.postgres;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Portal {

    private PreparedStatement statement;

    private short nParams;
    private List<Short> paramFormats = new ArrayList<>();
    private List<String> params = new ArrayList<>();
    private List<Short> resultFormats = new ArrayList<>();

    public boolean isTextParam(final int index) {

        if (paramFormats.isEmpty()) {
            return true;
        }

        if (index < paramFormats.size()) {
            return paramFormats.get(index) == (short) 0;
        }

        return paramFormats.getFirst() == (short) 0;
    }
}
