package com.phonesql.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonesql.proxy.phone.PhoneChannels;
import com.samskivert.mustache.Mustache;
import io.netty.bootstrap.Bootstrap;
import io.netty.util.internal.PlatformDependent;
import java.util.concurrent.ConcurrentMap;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationContext {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TemplateLoader templateLoader = new TemplateLoader();

    private final Mustache.Compiler compiler = Mustache.compiler().withLoader(templateLoader);

    private final ConcurrentMap<String, PreAuthorization> preAuthorizations =
            PlatformDependent.newConcurrentHashMap();

    private final PhoneChannels phoneChannels = new PhoneChannels();

    private final ClientChannels clientChannels = new ClientChannels();

    private final ConcurrentMap<String, ClientSession> clientSessions =
            PlatformDependent.newConcurrentHashMap();

    public Bootstrap mainServerClient = new Bootstrap();
    public final ConcurrentMap<String, PhoneSQLServerRequestType> mainServerRequests =
            PlatformDependent.newConcurrentHashMap();
}
