package no.cantara.jaxrsapp.sample.greeter;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public class HttpRandomizerClient implements RandomizerClient {

    private final String baseUrl;

    public HttpRandomizerClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String getRandomString(String token, int maxLength) {
        try {
            Response response = Request.Get(baseUrl + "/str/" + maxLength)
                    .connectTimeout(5000)
                    .socketTimeout(10000)
                    .setHeader(HttpHeaders.AUTHORIZATION, token)
                    .execute();
            HttpResponse httpResponse = response.returnResponse();
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Bad return from randomizer");
            }
            String result = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public int getRandomInteger(String token, int upperBoundEx) {
        try {
            Response response = Request.Get(baseUrl + "/int/" + upperBoundEx)
                    .connectTimeout(5000)
                    .socketTimeout(10000)
                    .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .execute();
            HttpResponse httpResponse = response.returnResponse();
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new RuntimeException("Bad return from randomizer, statusCode: " + statusCode);
            }
            String resultAsString = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
            int result = Integer.parseInt(resultAsString);
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void reseed(String token, long seed) {
        try {
            Response response = Request.Put(baseUrl + "/seed/" + seed)
                    .connectTimeout(5000)
                    .socketTimeout(10000)
                    .setHeader(HttpHeaders.AUTHORIZATION, token)
                    .execute();
            HttpResponse httpResponse = response.returnResponse();
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Bad return from randomizer");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
