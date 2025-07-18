package com.phonesql.proxy.api;

import java.util.List;
import lombok.Data;

@Data
public class Query {

    private String id;

    private String sql;
    private List<String> params;
}
