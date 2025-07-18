package com.phonesql.proxy.api;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultSet {

    private String id;

    private List<String> columns;

    private List<List<String>> rows;
}
