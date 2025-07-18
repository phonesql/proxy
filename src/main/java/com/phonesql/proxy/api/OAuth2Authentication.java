package com.phonesql.proxy.api;

import com.nimbusds.jwt.SignedJWT;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OAuth2Authentication implements Authentication {

    private SignedJWT jwt;

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final OAuth2Authentication that = (OAuth2Authentication) o;
        return Objects.equals(jwt.getSignature(), that.jwt.getSignature());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(jwt.getSignature());
    }
}
