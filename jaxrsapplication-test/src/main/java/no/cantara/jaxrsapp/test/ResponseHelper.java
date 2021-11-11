package no.cantara.jaxrsapp.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResponseHelper {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final org.apache.http.client.fluent.Response response;
    private final HttpResponse httpResponse;
    private final AtomicReference<Object> bodyRef = new AtomicReference<>();

    ResponseHelper(org.apache.http.client.fluent.Response response) {
        this.response = response;
        try {
            this.httpResponse = response.returnResponse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int status() {
        return httpResponse.getStatusLine().getStatusCode();
    }

    public String statusReasonPhrase() {
        return httpResponse.getStatusLine().getReasonPhrase();
    }

    public String protocolVersion() {
        return httpResponse.getStatusLine().getProtocolVersion().toString();
    }

    public String header(String name) {
        return ofNullable(httpResponse.getFirstHeader(name)).map(Header::getValue).orElse(null);
    }

    public List<String> headerNames() {
        List<String> headerNames = new ArrayList<>();
        HeaderIterator it = httpResponse.headerIterator();
        while (it.hasNext()) {
            Header header = it.nextHeader();
            headerNames.add(header.getName());
        }
        return headerNames;
    }

    public <T> List<T> contentAsList(Class<T> clazz) {
        List<T> body = (List<T>) bodyRef.get();
        if (body == null) {
            HttpEntity entity = httpResponse.getEntity();
            if (entity == null) {
                return null;
            }
            try {
                CollectionType collectionType = TypeFactory.defaultInstance()
                        .constructCollectionType(List.class, clazz);
                try (InputStream content = entity.getContent()) {
                    body = mapper.readValue(content, collectionType);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            bodyRef.set(body);
        }
        return body;
    }

    public <T> T contentAsType(TypeReference<T> typeReference) {
        T body = (T) bodyRef.get();
        if (body == null) {
            HttpEntity entity = httpResponse.getEntity();
            if (entity == null) {
                return null;
            }
            try {
                try (InputStream content = entity.getContent()) {
                    body = mapper.readValue(content, typeReference);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            bodyRef.set(body);
        }
        return body;
    }

    public <T> T contentAsType(Class<T> entityClass) {
        T body = (T) bodyRef.get();
        if (body == null) {
            HttpEntity entity = httpResponse.getEntity();
            if (entity == null) {
                return null;
            }
            try {
                if (String.class.equals(entityClass)) {
                    body = (T) EntityUtils.toString(entity, StandardCharsets.UTF_8);
                } else if (byte[].class.equals(entityClass)) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                    entity.writeTo(baos);
                    body = (T) baos.toByteArray();
                } else if (ByteBuffer.class.equals(entityClass)) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                    entity.writeTo(baos);
                    body = (T) ByteBuffer.wrap(baos.toByteArray());
                } else {
                    try (InputStream content = entity.getContent()) {
                        body = mapper.readValue(content, entityClass);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            bodyRef.set(body);
        }
        return body;
    }

    public String contentAsString() {
        String value = contentAsType(String.class);
        if (value == null) {
            return "";
        }
        return value;
    }

    public InputStream content() {
        try {
            HttpEntity entity = httpResponse.getEntity();
            if (entity == null) {
                return null;
            }
            return entity.getContent();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Tells the length of the content, if known.
     *
     * @return the number of bytes of the content, or
     * a negative number if unknown. If the content length is known
     * but exceeds {@link java.lang.Long#MAX_VALUE Long.MAX_VALUE},
     * a negative number is returned.
     */
    public long contentLength() {
        HttpEntity entity = httpResponse.getEntity();
        if (entity == null) {
            return 0;
        }
        return entity.getContentLength();
    }

    /**
     * Obtains the Content-Type header, if known.
     * This is the header that should be used when sending the entity,
     * or the one that was received with the entity. It can include a
     * charset attribute.
     *
     * @return the Content-Type header for this entity, or
     * {@code null} if the content type is unknown
     */
    public String contentType() {
        HttpEntity entity = httpResponse.getEntity();
        if (entity == null) {
            return null;
        }
        return entity.getContentType().getValue();
    }

    /**
     * Obtains the Content-Encoding header, if known.
     * This is the header that should be used when sending the entity,
     * or the one that was received with the entity.
     * Wrapping entities that modify the content encoding should
     * adjust this header accordingly.
     *
     * @return the Content-Encoding header for this entity, or
     * {@code null} if the content encoding is unknown
     */
    public String contentEncoding() {
        HttpEntity entity = httpResponse.getEntity();
        if (entity == null) {
            return null;
        }
        return entity.getContentEncoding().getValue();
    }

    public ResponseHelper expectAnyOf(int... anyOf) {
        int matchingStatusCode = -1;
        for (int statusCode : anyOf) {
            if (httpResponse.getStatusLine().getStatusCode() == statusCode) {
                matchingStatusCode = statusCode;
            }
        }
        assertTrue(matchingStatusCode != -1, () -> "Actual statusCode was " + httpResponse.getStatusLine().getStatusCode() + " message: " + contentAsString());
        return this;
    }

    public ResponseHelper expectStatus(int statusCode) {
        assertEquals(statusCode, httpResponse.getStatusLine().getStatusCode(), this::contentAsString);
        return this;
    }

    public ResponseHelper expect403Forbidden() {
        assertEquals(HttpStatus.SC_FORBIDDEN, httpResponse.getStatusLine().getStatusCode(), this::contentAsString);
        return this;
    }

    public ResponseHelper expect401Unauthorized() {
        assertEquals(HttpStatus.SC_UNAUTHORIZED, httpResponse.getStatusLine().getStatusCode(), this::contentAsString);
        return this;
    }

    public ResponseHelper expect400BadRequest() {
        assertEquals(HttpStatus.SC_BAD_REQUEST, httpResponse.getStatusLine().getStatusCode(), this::contentAsString);
        return this;
    }

    public ResponseHelper expect404NotFound() {
        assertEquals(HttpStatus.SC_NOT_FOUND, httpResponse.getStatusLine().getStatusCode(), this::contentAsString);
        return this;
    }

    public ResponseHelper expect200Ok() {
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine().getStatusCode(), this::contentAsString);
        return this;
    }

    public ResponseHelper expect201Created() {
        assertEquals(HttpStatus.SC_CREATED, httpResponse.getStatusLine().getStatusCode(), this::contentAsString);
        return this;
    }

    public ResponseHelper expect204NoContent() {
        assertEquals(HttpStatus.SC_NO_CONTENT, httpResponse.getStatusLine().getStatusCode(), this::contentAsString);
        return this;
    }
}
