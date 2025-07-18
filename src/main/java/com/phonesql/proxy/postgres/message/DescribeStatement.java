package com.phonesql.proxy.postgres.message;

import com.phonesql.proxy.postgres.PreparedStatement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DescribeStatement implements Message {

    private PreparedStatement statement;
}
