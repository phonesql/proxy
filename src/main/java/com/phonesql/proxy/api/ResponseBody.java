package com.phonesql.proxy.api;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseBody {

    @Singular private List<Error> errors;

    @Singular private List<ResultSet> resultSets;
}
