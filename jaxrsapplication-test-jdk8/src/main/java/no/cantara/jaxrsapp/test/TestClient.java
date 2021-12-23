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
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TestClient {

    private static final Logger LOG = LoggerFactory.getLogger(TestClient.class);

    // TODO configure with more support (jsr310, etc.) and/or allow client configuration
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final int CONNECT_TIMEOUT_MS = 3000;
    public static final int SOCKET_TIMEOUT_MS = 10000;

    private final Map<String, String> defaultHeaderByKey = new ConcurrentHashMap<>();
    private final String host;
    private final int port;

    private TestClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static TestClient newClient(String host, int port) {
        return new TestClient(host, port);
    }

    public TestClient useAuthorization(String authorization) {
        defaultHeaderByKey.put(HttpHeaders.AUTHORIZATION, authorization);
        return this;
    }

    public FakeApplicationAuthorizationBuilder useFakeApplicationAuth() {
        return new FakeApplicationAuthorizationBuilder();
    }

    public TestClient useFakeApplicationAuth(String applicationId) {
        return new FakeApplicationAuthorizationBuilder().applicationId(applicationId).endFakeApplication();
    }

    public FakeUserAuthorizationBuilder useFakeUserAuth() {
        return new FakeUserAuthorizationBuilder();
    }

    public TestClient useHeader(String header, String value) {
        defaultHeaderByKey.put(header, value);
        return this;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    URI toUri(String uri) {
        try {
            return new URI("http://" + host + ":" + port + uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public RequestBuilder get() {
        return new RequestBuilder().method(HttpMethod.GET);
    }

    public RequestBuilder post() {
        return new RequestBuilder().method(HttpMethod.POST);
    }

    public RequestBuilder put() {
        return new RequestBuilder().method(HttpMethod.PUT);
    }

    public RequestBuilder options() {
        return new RequestBuilder().method(HttpMethod.OPTIONS);
    }

    public RequestBuilder head() {
        return new RequestBuilder().method(HttpMethod.HEAD);
    }

    public RequestBuilder delete() {
        return new RequestBuilder().method(HttpMethod.DELETE);
    }

    public RequestBuilder patch() {
        return new RequestBuilder().method(HttpMethod.PATCH);
    }

    public RequestBuilder trace() {
        return new RequestBuilder().method(HttpMethod.TRACE);
    }

    public enum HttpMethod {
        GET, POST, PUT, OPTIONS, HEAD, DELETE, PATCH, TRACE;
    }

    public class RequestBuilder {
        private HttpMethod method;
        private String path;
        private HttpEntity entity;
        private Map<String, String> headers = new LinkedHashMap<>(defaultHeaderByKey);
        private List<NameValuePair> queryParams = new LinkedList<>();
        private int connectTimeout = CONNECT_TIMEOUT_MS;
        private int socketTimeout = SOCKET_TIMEOUT_MS;

        public RequestBuilder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public RequestBuilder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public RequestBuilder socketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public RequestBuilder authorization(String authorization) {
            return header(HttpHeaders.AUTHORIZATION, authorization);
        }

        public RequestBuilder authorizationBearer(String token) {
            return header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        public FakeApplicationAuthorizationBuilder fakeApplicationAuth() {
            return new FakeApplicationAuthorizationBuilder();
        }

        public RequestBuilder fakeApplicationAuth(String applicationId) {
            return fakeApplicationAuth()
                    .applicationId(applicationId)
                    .endFakeApplication();
        }

        public FakeUserAuthorizationBuilder fakeUserAuth() {
            return new FakeUserAuthorizationBuilder();
        }

        public class FakeApplicationAuthorizationBuilder {
            private String applicationId;

            public FakeApplicationAuthorizationBuilder applicationId(String applicationId) {
                this.applicationId = applicationId;
                return this;
            }

            public RequestBuilder endFakeApplication() {
                header(HttpHeaders.AUTHORIZATION, "Bearer fake-application-id: " + applicationId);
                return RequestBuilder.this;
            }
        }

        public class FakeUserAuthorizationBuilder {
            private String userId;
            private String username;
            private String usertokenId;
            private String customerRef;
            private final Map<String, String> roles = new LinkedHashMap<>();

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

            public RequestBuilder endFakeUser() {
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
                return RequestBuilder.this;
            }
        }

        public RequestBuilder path(String path) {
            this.path = path;
            return this;
        }

        public RequestBuilder query(String key, String value) {
            queryParams.add(new BasicNameValuePair(key, value));
            return this;
        }

        public RequestBuilder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public RequestBuilder bodyJson(Object body) {
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

        public RequestBuilder bodyJson(String body) {
            entity = new StringEntity(body, ContentType.APPLICATION_JSON);
            return this;
        }

        public RequestBuilder body(String body) {
            entity = new StringEntity(body, ContentType.create("text/plain", StandardCharsets.UTF_8));
            return this;
        }

        public RequestBuilder body(String body, String mimeType) {
            entity = new StringEntity(body, ContentType.create(mimeType));
            return this;
        }

        public RequestBuilder body(String body, String mimeType, Charset charset) {
            entity = new StringEntity(body, ContentType.create(mimeType, charset));
            return this;
        }

        public RequestBuilder bodyJson(InputStream body) {
            entity = new InputStreamEntity(body, ContentType.APPLICATION_JSON);
            return this;
        }

        public RequestBuilder body(InputStream body) {
            entity = new InputStreamEntity(body, ContentType.APPLICATION_OCTET_STREAM);
            return this;
        }

        public RequestBuilder body(InputStream body, String mimeType) {
            entity = new InputStreamEntity(body, ContentType.create(mimeType));
            return this;
        }

        public RequestBuilder body(InputStream body, String mimeType, Charset charset) {
            entity = new InputStreamEntity(body, ContentType.create(mimeType, charset));
            return this;
        }

        public RequestBuilder bodyJson(byte[] body) {
            entity = new ByteArrayEntity(body, ContentType.APPLICATION_JSON);
            return this;
        }

        public RequestBuilder body(byte[] body) {
            entity = new ByteArrayEntity(body, ContentType.APPLICATION_OCTET_STREAM);
            return this;
        }

        public RequestBuilder body(byte[] body, String mimeType) {
            entity = new ByteArrayEntity(body, ContentType.create(mimeType));
            return this;
        }

        public RequestBuilder body(byte[] body, String mimeType, Charset charset) {
            entity = new ByteArrayEntity(body, ContentType.create(mimeType, charset));
            return this;
        }

        public RequestBuilder bodyJson(File body) {
            entity = new FileEntity(body, ContentType.APPLICATION_JSON);
            return this;
        }

        public RequestBuilder body(File body) {
            entity = new FileEntity(body);
            return this;
        }

        public RequestBuilder body(File body, String mimeType) {
            entity = new FileEntity(body, ContentType.create(mimeType));
            return this;
        }

        public RequestBuilder body(File body, String mimeType, Charset charset) {
            entity = new FileEntity(body, ContentType.create(mimeType, charset));
            return this;
        }

        public FormBuilder bodyForm() {
            return new FormBuilder();
        }

        public class FormBuilder {

            Charset charset = StandardCharsets.UTF_8;
            final List<NameValuePair> pairs = new ArrayList<>();

            public FormBuilder charset(Charset charset) {
                this.charset = charset;
                return this;
            }

            public FormBuilder put(String name, String value) {
                pairs.add(new BasicNameValuePair(name, value));
                return this;
            }

            public RequestBuilder endForm() {
                entity = new UrlEncodedFormEntity(pairs, charset);
                return RequestBuilder.this;
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

    public class FakeApplicationAuthorizationBuilder {
        private String applicationId;
        private List<Tag> tags = new LinkedList<>();
        private List<String> authGroups = new LinkedList<>();

        public FakeApplicationAuthorizationBuilder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public FakeApplicationAuthorizationBuilder addTag(String tagName, String tagValue) {
            tags.add(new Tag(tagName, tagValue));
            return this;
        }

        public FakeApplicationAuthorizationBuilder addTag(String tagValue) {
            tags.add(new Tag(Tag.DEFAULTNAME, tagValue));
            return this;
        }

        public FakeApplicationAuthorizationBuilder addAccessGroup(String group) {
            authGroups.add(group);
            return this;
        }

        public TestClient endFakeApplication() {
            if (authGroups.size() > 0) {
                String accessGroups = String.join(" ", authGroups);
                tags.add(new Tag(WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_APPLICATION_TAG_NAME, accessGroups));
            }
            StringBuilder sb = new StringBuilder("Bearer fake-application-id: ").append(applicationId);
            if (tags.size() > 0) {
                sb.append(", fake-tags: ").append(ApplicationTagMapper.toApplicationTagString(tags));
            }
            useAuthorization(sb.toString());
            return TestClient.this;
        }
    }

    public class FakeUserAuthorizationBuilder {
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

        public TestClient endFakeUser() {
            if (userId == null) {
                throw new IllegalArgumentException("userId cannot be null");
            }
            if (customerRef == null) {
                throw new IllegalArgumentException("customerRef cannot be null");
            }
            if (authGroups.size() > 0) {
                String accessGroups = String.join(" ", authGroups);
                roles.put(WhydahAuthenticationManagerFactory.DEFAULT_AUTH_GROUP_USER_ROLE_NAME, accessGroups);
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
            return TestClient.this;
        }
    }
}
