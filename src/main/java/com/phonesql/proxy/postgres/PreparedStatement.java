package com.phonesql.proxy.postgres;

import com.phonesql.proxy.postgres.queryhandler.QueryHandler;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreparedStatement {

    private String sql;
    private List<Integer> oidParams = new ArrayList<>();

    private QueryHandler queryHandler;
}
