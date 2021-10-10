package no.cantara.jaxrsapp.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;

public final class TestClient {

    private static final Logger LOG = LoggerFactory.getLogger(TestClient.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    public static final int CONNECT_TIMEOUT_MS = 3000;
    public static final int SOCKET_TIMEOUT_MS = 10000;

    private final String host;
    private final int port;

    private TestClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static TestClient newClient(String host, int port) {
        return new TestClient(host, port);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    URI toUri(String path, String query) {
        try {
            return new URI("http", null, host, port, path, query, "");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    static String captureStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    Request withHeaders(Request request, String... headersKeyAndValue) {
        for (int i = 0; i + 1 < headersKeyAndValue.length; i += 2) {
            request.addHeader(headersKeyAndValue[i], headersKeyAndValue[i + 1]);
        }
        return request;
    }

    HttpEntity toJsonEntity(Object entity) {
        String json;
        if (entity instanceof String) {
            json = (String) entity;
        } else {
            try {
                json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(entity);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return new StringEntity(json, ContentType.APPLICATION_JSON);
    }

    RuntimeException coerceUnchecked(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }

    public ResponseHelper<String> options(String uri, String... headersKeyAndValue) {
        return options(String.class, uri, headersKeyAndValue);
    }

    public <R> ResponseHelper<R> options(Class<R> responseClazz, String uri, String... headersKeyAndValue) {
        try {
            Response response = withHeaders(Request.Options(toUri(uri, null))
                    .connectTimeout(CONNECT_TIMEOUT_MS)
                    .socketTimeout(SOCKET_TIMEOUT_MS), headersKeyAndValue)
                    .execute();
            return new ResponseHelper<>(responseClazz, response);
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw coerceUnchecked(e);
        }
    }

    public ResponseHelper<String> head(String uri) {
        return head(String.class, uri);
    }

    public <R> ResponseHelper<R> head(Class<R> responseClazz, String uri) {
        try {
            Response response = Request.Head(toUri(uri, null))
                    .connectTimeout(CONNECT_TIMEOUT_MS)
                    .socketTimeout(SOCKET_TIMEOUT_MS)
                    .execute();
            return new ResponseHelper<>(responseClazz, response);
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw coerceUnchecked(e);
        }
    }

    public <R> ResponseHelper<R> put(Class<R> responseClazz, String uri, String... headers) {
        try {
            Response response = withHeaders(Request.Put(toUri(uri, null))
                    .connectTimeout(CONNECT_TIMEOUT_MS)
                    .socketTimeout(SOCKET_TIMEOUT_MS), headers)
                    .execute();
            return new ResponseHelper<>(responseClazz, response);
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw coerceUnchecked(e);
        }
    }

    public <T, R> ResponseHelper<R> put(Class<R> responseClazz, String uri, T pojoBody, String... headers) {
        try {
            Response response = withHeaders(Request.Put(toUri(uri, null))
                    .connectTimeout(CONNECT_TIMEOUT_MS)
                    .socketTimeout(SOCKET_TIMEOUT_MS), headers)
                    .body(toJsonEntity(pojoBody))
                    .execute();
            return new ResponseHelper<>(responseClazz, response);
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw coerceUnchecked(e);
        }
    }

    public ResponseHelper<String> post(String uri, String... headers) {
        return post(String.class, uri, headers);
    }

    public <R> ResponseHelper<R> post(Class<R> responseClazz, String uri, String... headers) {
        try {
            Response response = withHeaders(Request.Put(toUri(uri, null))
                    .connectTimeout(CONNECT_TIMEOUT_MS)
                    .socketTimeout(SOCKET_TIMEOUT_MS), headers)
                    .execute();
            return new ResponseHelper<>(responseClazz, response);
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw coerceUnchecked(e);
        }
    }

    public <T, R> ResponseHelper<R> post(Class<R> responseClazz, String uri, T pojo, String... headers) {
        try {
            Response response = withHeaders(Request.Post(toUri(uri, null))
                    .connectTimeout(CONNECT_TIMEOUT_MS)
                    .socketTimeout(SOCKET_TIMEOUT_MS), headers)
                    .body(toJsonEntity(pojo))
                    .execute();
            return new ResponseHelper<>(responseClazz, response);
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw coerceUnchecked(e);
        }
    }

    public <R> ResponseHelper<R> get(Class<R> responseClazz, String uri, String... headersKeyAndValue) {
        try {
            Response response = withHeaders(Request.Get(toUri(uri, null))
                    .connectTimeout(CONNECT_TIMEOUT_MS)
                    .socketTimeout(SOCKET_TIMEOUT_MS), headersKeyAndValue)
                    .execute();
            return new ResponseHelper<>(responseClazz, response);
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw coerceUnchecked(e);
        }
    }

    public ResponseHelper<String> delete(String uri) {
        return delete(String.class, uri);
    }

    public <R> ResponseHelper<R> delete(Class<R> responseClazz, String uri, String... headersKeyAndValue) {
        try {
            Response response = withHeaders(Request.Delete(toUri(uri, null))
                    .connectTimeout(CONNECT_TIMEOUT_MS)
                    .socketTimeout(SOCKET_TIMEOUT_MS), headersKeyAndValue)
                    .execute();
            return new ResponseHelper<>(responseClazz, response);
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw coerceUnchecked(e);
        }
    }
}
