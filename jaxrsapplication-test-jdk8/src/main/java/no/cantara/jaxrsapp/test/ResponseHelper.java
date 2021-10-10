package no.cantara.jaxrsapp.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Response;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResponseHelper<T> {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final Class<T> entityClass;
    private final Response response;
    private final HttpResponse httpResponse;
    private final AtomicReference<T> bodyRef = new AtomicReference<>();

    public ResponseHelper(Class<T> entityClass, Response response) {
        this.entityClass = entityClass;
        this.response = response;
        try {
            this.httpResponse = response.returnResponse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Response response() {
        return response;
    }

    public HttpResponse httpResponse() {
        return httpResponse;
    }

    public T body() {
        T body = bodyRef.get();
        if (body == null) {
            try {
                if (String.class.equals(entityClass)) {
                    body = (T) EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                } else if (byte[].class.equals(entityClass)) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                    httpResponse.getEntity().writeTo(baos);
                    return (T) baos.toByteArray();
                } else {
                    body = mapper.readValue(httpResponse.getEntity().getContent(), entityClass);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            bodyRef.set(body);
        }
        return body;
    }

    private String bodyAsString() {
        try {
            return EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseHelper<T> expectAnyOf(int... anyOf) {
        int matchingStatusCode = -1;
        for (int statusCode : anyOf) {
            if (httpResponse.getStatusLine().getStatusCode() == statusCode) {
                matchingStatusCode = statusCode;
            }
        }
        assertTrue(matchingStatusCode != -1, () -> "Actual statusCode was " + httpResponse.getStatusLine().getStatusCode() + " message: " + body());
        return this;
    }

    public ResponseHelper<T> expect403Forbidden() {
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine().getStatusCode(), this::bodyAsString);
        return this;
    }

    public ResponseHelper<T> expect401Unauthorized() {
        assertEquals(HttpStatus.SC_UNAUTHORIZED, httpResponse.getStatusLine().getStatusCode(), this::bodyAsString);
        return this;
    }

    public ResponseHelper<T> expect400BadRequest() {
        assertEquals(HttpStatus.SC_BAD_REQUEST, httpResponse.getStatusLine().getStatusCode(), this::bodyAsString);
        return this;
    }

    public ResponseHelper<T> expect404NotFound() {
        assertEquals(HttpStatus.SC_NOT_FOUND, httpResponse.getStatusLine().getStatusCode(), this::bodyAsString);
        return this;
    }

    public ResponseHelper<T> expect200Ok() {
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine().getStatusCode(), this::bodyAsString);
        return this;
    }

    public ResponseHelper<T> expect201Created() {
        assertEquals(HttpStatus.SC_CREATED, httpResponse.getStatusLine().getStatusCode(), this::bodyAsString);
        return this;
    }

    public ResponseHelper<T> expect204NoContent() {
        assertEquals(HttpStatus.SC_NO_CONTENT, httpResponse.getStatusLine().getStatusCode(), this::bodyAsString);
        return this;
    }
}
