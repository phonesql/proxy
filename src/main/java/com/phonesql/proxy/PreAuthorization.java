package com.phonesql.proxy;

import com.nimbusds.jwt.SignedJWT;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreAuthorization {

    private Type type;
    private String database;

    private String username;
    private String password;

    private String state;
    private SignedJWT token;
    private Channel channel;

    @Getter
    public enum Type {
        USERNAME_PASSWORD,
        OAUTH2
    }
}
