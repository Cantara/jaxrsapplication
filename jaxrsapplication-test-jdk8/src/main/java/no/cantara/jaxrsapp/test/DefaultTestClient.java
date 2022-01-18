package no.cantara.jaxrsapp.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.sso.application.mappers.ApplicationTagMapper;
import net.whydah.sso.application.types.Tag;
import no.cantara.security.authentication.whydah.WhydahAuthenticationManagerFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultTestClient implements TestClient {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultTestClient.class);

    // TODO configure with more support (jsr310, etc.) and/or allow client configuration
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final int CONNECT_TIMEOUT_MS = 3000;
    public static final int SOCKET_TIMEOUT_MS = 10000;

    private final Map<String, String> defaultHeaderByKey = new ConcurrentHashMap<>();
    private final String scheme;
    private final String host;
    private final int port;

    private DefaultTestClient(String scheme, String host, int port) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
    }

    public static DefaultTestClient newClient(String scheme, String host, int port) {
        return new DefaultTestClient(scheme, host, port);
    }

    @Override
    public DefaultTestClient useAuthorization(String authorization) {
        defaultHeaderByKey.put(HttpHeaders.AUTHORIZATION, authorization);
        return this;
    }

    @Override
    public DefaultFakeApplicationAuthorizationBuilder useFakeApplicationAuth() {
        return new DefaultFakeApplicationAuthorizationBuilder();
    }

    public DefaultTestClient useFakeApplicationAuth(String applicationId) {
        return new DefaultFakeApplicationAuthorizationBuilder().applicationId(applicationId).endFakeApplication();
    }

    public DefaultFakeUserAuthorizationBuilder useFakeUserAuth() {
        return new DefaultFakeUserAuthorizationBuilder();
    }

    public DefaultTestClient useHeader(String header, String value) {
        defaultHeaderByKey.put(header, value);
        return this;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public URI getBaseURI() {
        return URI.create(scheme + "://" + host + ":" + port);
    }

    URI toUri(String pathAndQuery) {
        return URI.create(scheme + "://" + host + ":" + port + pathAndQuery);
    }

    public DefaultRequestBuilder get() {
        return new DefaultRequestBuilder().method(HttpMethod.GET);
    }

    public DefaultRequestBuilder post() {
        return new DefaultRequestBuilder().method(HttpMethod.POST);
    }

    public DefaultRequestBuilder put() {
        return new DefaultRequestBuilder().method(HttpMethod.PUT);
    }

    public DefaultRequestBuilder options() {
        return new DefaultRequestBuilder().method(HttpMethod.OPTIONS);
    }

    public DefaultRequestBuilder head() {
        return new DefaultRequestBuilder().method(HttpMethod.HEAD);
    }

    public DefaultRequestBuilder delete() {
        return new DefaultRequestBuilder().method(HttpMethod.DELETE);
    }

    public DefaultRequestBuilder patch() {
        return new DefaultRequestBuilder().method(HttpMethod.PATCH);
    }

    public DefaultRequestBuilder trace() {
        return new DefaultRequestBuilder().method(HttpMethod.TRACE);
    }

    public class DefaultRequestBuilder implements RequestBuilder {
        private HttpMethod method;
        private String path;
        private HttpEntity entity;
        private Map<String, String> headers = new LinkedHashMap<>(defaultHeaderByKey);
        private List<NameValuePair> queryParams = new LinkedList<>();
        private int connectTimeout = CONNECT_TIMEOUT_MS;
        private int socketTimeout = SOCKET_TIMEOUT_MS;

        public DefaultRequestBuilder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public DefaultRequestBuilder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public DefaultRequestBuilder socketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public DefaultRequestBuilder authorization(String authorization) {
            return header(HttpHeaders.AUTHORIZATION, authorization);
        }

        public DefaultRequestBuilder authorizationBearer(String token) {
            return header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        public DefaultFakeApplicationAuthorizationBuilder fakeApplicationAuth() {
            return new DefaultFakeApplicationAuthorizationBuilder();
        }

        public DefaultRequestBuilder fakeApplicationAuth(String applicationId) {
            return fakeApplicationAuth()
                    .applicationId(applicationId)
                    .endFakeApplication();
        }

        public DefaultFakeUserAuthorizationBuilder fakeUserAuth() {
            return new DefaultFakeUserAuthorizationBuilder();
        }

        public class DefaultFakeApplicationAuthorizationBuilder implements FakeApplicationAuthorizationBuilder {
            private String applicationId;

            public DefaultFakeApplicationAuthorizationBuilder applicationId(String applicationId) {
                this.applicationId = applicationId;
                return this;
            }

            public DefaultRequestBuilder endFakeApplication() {
                header(HttpHeaders.AUTHORIZATION, "Bearer fake-application-id: " + applicationId);
                return DefaultRequestBuilder.this;
            }
        }

        public class DefaultFakeUserAuthorizationBuilder implements FakeUserAuthorizationBuilder {
            private String userId;
            private String username;
            private String usertokenId;
            private String customerRef;
            private final Map<String, String> roles = new LinkedHashMap<>();

            public DefaultFakeUserAuthorizationBuilder userId(String userId) {
                this.userId = userId;
                return this;
            }

            public DefaultFakeUserAuthorizationBuilder username(String username) {
                this.username = username;
                return this;
            }

            public DefaultFakeUserAuthorizationBuilder usertokenId(String usertokenId) {
                this.usertokenId = usertokenId;
                return this;
            }

            public DefaultFakeUserAuthorizationBuilder customerRef(String customerRef) {
                this.customerRef = customerRef;
                return this;
            }

            public DefaultFakeUserAuthorizationBuilder addRole(String name, String value) {
                this.roles.put(name, value);
                return this;
            }

            public DefaultRequestBuilder endFakeUser() {
                if (userId == null) {
                    throw new IllegalArgumentException("userId cannot be null");
                }
                if (customerRef == null) {
                    throw new IllegalArgumentException("customerRef cannot be null");
                }
                final StringBuilder sb = new StringBuilder();
                sb.append("Bearer ");
                sb.append("fake-sso-id: ").append(userId);
                if (username != null) {
                    sb.append(", fake-username: ").append(username);
                }
                if (usertokenId != null) {
                    sb.append(", fake-usertoken-id: ").append(usertokenId);
                }
                sb.append(", fake-customer-ref: ").append(customerRef);
                {
                    sb.append(", fake-roles: ");
                    String delim = "";
                    for (Map.Entry<String, String> role : roles.entrySet()) {
                        sb.append(delim).append(role.getKey()).append("=").append(role.getValue());
                        delim = ",";
                    }
                }
                header(HttpHeaders.AUTHORIZATION, sb.toString());
                return DefaultRequestBuilder.this;
            }
        }

        public DefaultRequestBuilder path(String path) {
            this.path = path;
            return this;
        }

        public DefaultRequestBuilder query(String key, String value) {
            queryParams.add(new BasicNameValuePair(key, value));
            return this;
        }

        public DefaultRequestBuilder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public DefaultRequestBuilder bodyJson(Object body) {
            String json;
            if (body instanceof String) {
                json = (String) body;
            } else {
                try {
                    json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(body);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            entity = new StringEntity(json, ContentType.APPLICATION_JSON);
            return this;
        }

        public DefaultRequestBuilder bodyJson(String body) {
            entity = new StringEntity(body, ContentType.APPLICATION_JSON);
            return this;
        }

        public DefaultRequestBuilder body(String body) {
            entity = new StringEntity(body, ContentType.create("text/plain", StandardCharsets.UTF_8));
            return this;
        }

        public DefaultRequestBuilder body(String body, String mimeType) {
            entity = new StringEntity(body, ContentType.create(mimeType));
            return this;
        }

        public DefaultRequestBuilder body(String body, String mimeType, Charset charset) {
            entity = new StringEntity(body, ContentType.create(mimeType, charset));
            return this;
        }

        public DefaultRequestBuilder bodyJson(InputStream body) {
            entity = new InputStreamEntity(body, ContentType.APPLICATION_JSON);
            return this;
        }

        public DefaultRequestBuilder body(InputStream body) {
            entity = new InputStreamEntity(body, ContentType.APPLICATION_OCTET_STREAM);
            return this;
        }

        public DefaultRequestBuilder body(InputStream body, String mimeType) {
            entity = new InputStreamEntity(body, ContentType.create(mimeType));
            return this;
        }

        public DefaultRequestBuilder body(InputStream body, String mimeType, Charset charset) {
            entity = new InputStreamEntity(body, ContentType.create(mimeType, charset));
            return this;
        }

        public DefaultRequestBuilder bodyJson(byte[] body) {
            entity = new ByteArrayEntity(body, ContentType.APPLICATION_JSON);
            return this;
        }

        public DefaultRequestBuilder body(byte[] body) {
            entity = new ByteArrayEntity(body, ContentType.APPLICATION_OCTET_STREAM);
            return this;
        }

        public DefaultRequestBuilder body(byte[] body, String mimeType) {
            entity = new ByteArrayEntity(body, ContentType.create(mimeType));
            return this;
        }

        public DefaultRequestBuilder body(byte[] body, String mimeType, Charset charset) {
            entity = new ByteArrayEntity(body, ContentType.create(mimeType, charset));
            return this;
        }

        public DefaultRequestBuilder bodyJson(File body) {
            entity = new FileEntity(body, ContentType.APPLICATION_JSON);
            return this;
        }

        public DefaultRequestBuilder body(File body) {
            entity = new FileEntity(body);
            return this;
        }

        public DefaultRequestBuilder body(File body, String mimeType) {
            entity = new FileEntity(body, ContentType.create(mimeType));
            return this;
        }

        public DefaultRequestBuilder body(File body, String mimeType, Charset charset) {
            entity = new FileEntity(body, ContentType.create(mimeType, charset));
            return this;
        }

        public DefaultFormBuilder bodyForm() {
            return new DefaultFormBuilder();
        }

        public class DefaultFormBuilder implements FormBuilder {

            Charset charset = StandardCharsets.UTF_8;
            final List<NameValuePair> pairs = new ArrayList<>();

            public DefaultFormBuilder charset(Charset charset) {
                this.charset = charset;
                return this;
            }

            public DefaultFormBuilder put(String name, String value) {
                pairs.add(new BasicNameValuePair(name, value));
                return this;
            }

            public DefaultRequestBuilder endForm() {
                entity = new UrlEncodedFormEntity(pairs, charset);
                return DefaultRequestBuilder.this;
            }
        }

        public ResponseHelper execute() {
            try {
                String queryString = URLEncodedUtils.format(queryParams, StandardCharsets.UTF_8);
                String pathAndQuery = path + (queryString.isEmpty() ? "" : (path.contains("?") ? "&" : "?") + queryString);
                URI uri = toUri(pathAndQuery);
                Request request;
                switch (method) {
                    case GET:
                        request = Request.Get(uri);
                        break;
                    case POST:
                        request = Request.Post(uri);
                        break;
                    case PUT:
                        request = Request.Put(uri);
                        break;
                    case OPTIONS:
                        request = Request.Options(uri);
                        break;
                    case HEAD:
                        request = Request.Head(uri);
                        break;
                    case DELETE:
                        request = Request.Delete(uri);
                        break;
                    case PATCH:
                        request = Request.Patch(uri);
                        break;
                    case TRACE:
                        request = Request.Trace(uri);
                        break;
                    default:
                        throw new IllegalArgumentException("HttpMethod not supported: " + method);
                }
                request.connectTimeout(connectTimeout);
                request.socketTimeout(socketTimeout);
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    request.addHeader(header.getKey(), header.getValue());
                }
                if (entity != null) {
                    request.body(entity);
                }
                Response response = request
                        .execute();
                return new ResponseHelper(response);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public class DefaultFakeApplicationAuthorizationBuilder implements FakeApplicationAuthorizationBuilder {
        private String applicationId;
        private List<Tag> tags = new LinkedList<>();
        private List<String> authGroups = new LinkedList<>();

        public DefaultFakeApplicationAuthorizationBuilder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public DefaultFakeApplicationAuthorizationBuilder addTag(String tagName, String tagValue) {
            tags.add(new Tag(tagName, tagValue));
            return this;
        }

        public DefaultFakeApplicationAuthorizationBuilder addTag(String tagValue) {
            tags.add(new Tag(Tag.DEFAULTNAME, tagValue));
            return this;
        }

        public DefaultFakeApplicationAuthorizationBuilder addAccessGroup(String group) {
            authGroups.add(group);
            return this;
        }

        public DefaultTestClient endFakeApplication() {
            if (authGroups.size() > 0) {
                String accessGroups = String.join(" ", authGroups);
                tags.add(new Tag(WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_APPLICATION_TAG_NAME, accessGroups));
            }
            StringBuilder sb = new StringBuilder("Bearer fake-application-id: ").append(applicationId);
            if (tags.size() > 0) {
                sb.append(", fake-tags: ").append(ApplicationTagMapper.toApplicationTagString(tags));
            }
            useAuthorization(sb.toString());
            return DefaultTestClient.this;
        }
    }

    public class DefaultFakeUserAuthorizationBuilder implements FakeUserAuthorizationBuilder {
        private String userId;
        private String username;
        private String usertokenId;
        private String customerRef;
        private final Map<String, String> roles = new LinkedHashMap<>();
        private final List<String> authGroups = new LinkedList<>();

        public FakeUserAuthorizationBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public FakeUserAuthorizationBuilder username(String username) {
            this.username = username;
            return this;
        }

        public FakeUserAuthorizationBuilder usertokenId(String usertokenId) {
            this.usertokenId = usertokenId;
            return this;
        }

        public FakeUserAuthorizationBuilder customerRef(String customerRef) {
            this.customerRef = customerRef;
            return this;
        }

        public FakeUserAuthorizationBuilder addRole(String name, String value) {
            this.roles.put(name, value);
            return this;
        }

        public FakeUserAuthorizationBuilder addAccessGroup(String group) {
            this.authGroups.add(group);
            return this;
        }

        public DefaultTestClient endFakeUser() {
            if (userId == null) {
                throw new IllegalArgumentException("userId cannot be null");
            }
            if (customerRef == null) {
                throw new IllegalArgumentException("customerRef cannot be null");
            }
            if (authGroups.size() > 0) {
                String accessGroups = String.join(" ", authGroups);
                roles.put(WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_USER_ROLE_NAME_FIX, accessGroups);
            }
            final StringBuilder sb = new StringBuilder();
            sb.append("Bearer ");
            sb.append("fake-sso-id: ").append(userId);
            if (username != null) {
                sb.append(", fake-username: ").append(username);
            }
            if (usertokenId != null) {
                sb.append(", fake-usertoken-id: ").append(usertokenId);
            }
            sb.append(", fake-customer-ref: ").append(customerRef);
            {
                sb.append(", fake-roles: ");
                String delim = "";
                for (Map.Entry<String, String> role : roles.entrySet()) {
                    sb.append(delim).append(role.getKey()).append("=").append(role.getValue());
                    delim = ",";
                }
            }
            useAuthorization(sb.toString());
            return DefaultTestClient.this;
        }
    }
}
