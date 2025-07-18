package com.phonesql.proxy.postgres;

public enum MessageType {
    Query,
    Parse,
    Bind,
    DescribeStatement,
    DescribePortal,
    Execute,
    Close,
    Sync,
    Flush
}
