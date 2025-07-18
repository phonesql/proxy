package com.phonesql.proxy.postgres.message;

import com.phonesql.proxy.postgres.Portal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DescribePortal implements Message {

    private Portal portal;
}
