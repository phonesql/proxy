package com.phonesql.proxy.api;

import java.util.List;
import lombok.Data;

@Data
public class ApiRequest {

    private String database;

    private List<Query> queries;
}
