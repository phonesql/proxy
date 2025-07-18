package com.phonesql.proxy;

import org.apache.commons.lang3.StringUtils;

public class ApplicationProperties {

    private static final String PHONESQL_URI_NAME = "PHONESQL_URI";
    private static final String PHONESQL_PROXY_URI_NAME = "PHONESQL_PROXY_URI";

    private static final String HTTP_SERVER_PORT_NAME = "PHONESQL_PROXY_HTTP_SERVER_PORT";
    private static final String POSTGRES_SERVER_PORT_NAME = "PHONESQL_PROXY_POSTGRES_SERVER_PORT";

    private static final String SSL_CERTIFICATE_PATH_NAME = "PHONESQL_PROXY_SSL_CERTIFICATE_PATH";
    private static final String SSL_CERTIFICATE_KEY_PATH_NAME =
            "PHONESQL_PROXY_SSL_CERTIFICATE_KEY_PATH";

    private static final String OAUTH2_CLIENT_ID_NAME = "PHONESQL_PROXY_OAUTH2_CLIENT_ID";
    private static final String OAUTH2_CLIENT_SECRET_NAME = "PHONESQL_PROXY_OAUTH2_CLIENT_SECRET";

    public static String PHONESQL_URI = "http://localhost:8080";
    public static String PHONESQL_PROXY_URI = "http://localhost:20080";

    public static Integer HTTP_SERVER_PORT = 20080;
    public static Integer POSTGRES_SERVER_PORT = 25432;

    public static String SSL_CERTIFICATE_PATH;
    public static String SSL_CERTIFICATE_KEY_PATH;

    public static String OAUTH2_CLIENT_ID;
    public static String OAUTH2_CLIENT_SECRET;

    static {
        final String phoneSQLUri = System.getenv(PHONESQL_URI_NAME);

        if (StringUtils.isNotBlank(phoneSQLUri)) {
            PHONESQL_URI = phoneSQLUri;
        }

        final String phoneSQLProxyUri = System.getenv(PHONESQL_PROXY_URI_NAME);

        if (StringUtils.isNotBlank(phoneSQLProxyUri)) {
            PHONESQL_PROXY_URI = phoneSQLProxyUri;
        }

        final String httpServerPortStr = System.getenv(HTTP_SERVER_PORT_NAME);

        if (StringUtils.isNotBlank(httpServerPortStr)) {
            HTTP_SERVER_PORT = Integer.parseInt(httpServerPortStr);
        }

        final String postgresServerPortStr = System.getenv(POSTGRES_SERVER_PORT_NAME);

        if (StringUtils.isNotBlank(postgresServerPortStr)) {
            POSTGRES_SERVER_PORT = Integer.parseInt(postgresServerPortStr);
        }

        final String sslCertificatePath = System.getenv(SSL_CERTIFICATE_PATH_NAME);

        if (StringUtils.isNotBlank(sslCertificatePath)) {
            SSL_CERTIFICATE_PATH = sslCertificatePath;
        }

        final String sslCertificateKeyPath = System.getenv(SSL_CERTIFICATE_KEY_PATH_NAME);

        if (StringUtils.isNotBlank(sslCertificateKeyPath)) {
            SSL_CERTIFICATE_KEY_PATH = sslCertificateKeyPath;
        }

        final String oauth2ClientId = System.getenv(OAUTH2_CLIENT_ID_NAME);

        if (StringUtils.isNotBlank(oauth2ClientId)) {
            OAUTH2_CLIENT_ID = oauth2ClientId;
        }

        final String oauth2ClientSecret = System.getenv(OAUTH2_CLIENT_SECRET_NAME);

        if (StringUtils.isNotBlank(oauth2ClientSecret)) {
            OAUTH2_CLIENT_SECRET = oauth2ClientSecret;
        }
    }
}
