package com.phonesql.proxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public class OAuth2DatabaseRegistrationResponseHandler
        extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final ApplicationContext applicationContext;

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final FullHttpResponse msg)
            throws IOException, ParseException {

        final String requestId = msg.headers().get("X-Request-ID");

        if (!(Objects.nonNull(requestId)
                && PhoneSQLServerRequestType.OAUTH2_DATABASE_REGISTRATION.equals(
                        applicationContext.getMainServerRequests().get(requestId)))) {
            ctx.fireChannelRead(msg.retain());
            return;
        }

        applicationContext.getMainServerRequests().remove(requestId);

        byte[] bytes = new byte[msg.content().readableBytes()];
        msg.content().readBytes(bytes);

        final Response response =
                applicationContext.getObjectMapper().readValue(bytes, Response.class);

        final ChannelId phoneChannel =
                applicationContext.getPhoneChannels().getUnverifiedChannels().get(requestId);

        for (final Database database : response.getDatabases()) {

            applicationContext.getPhoneChannels().addDatabase(database.getId(), phoneChannel);
        }

        applicationContext.getPhoneChannels().getUnverifiedChannels().remove(requestId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {

        private List<Database> databases;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Database {

        private String id;

        private Long version;

        private String name;

        private String description;

        private String userId;
    }
}
