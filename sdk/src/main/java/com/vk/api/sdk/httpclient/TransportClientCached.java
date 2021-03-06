package com.vk.api.sdk.httpclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.TransportClient;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpStatus;

/**
 * Class or Interface description.
 * <p>
 * Additional info
 *
 * @author Mikhail Yakushin (driver733@me.com)
 * @version $Id$
 * @since 0.1
 */
public class TransportClientCached implements TransportClient {

    /**
     * Cached request`s response.
     */
    private final JsonElement result;

    /**
     * Request with cached response.
     * @param response Cached request`s response (value of the response json field).
     */
    public TransportClientCached(final String response) {
        this.result = new JsonParser().parse(
            new JsonReader(
                new StringReader(
                    response
                )
            )
        );
    }

    @Override
    public ClientResponse post(
        final String url,
        final String body
    ) throws IOException {
        return new ClientResponse(
            HttpStatus.SC_OK,
            this.result.toString(),
            headers()
        );
    }

    @Override
    public ClientResponse post(
        final String url, final String fileName,
        final File file
    ) throws IOException {
        return new ClientResponse(
            HttpStatus.SC_OK,
            this.result.toString(),
            headers()
        );
    }

    @Override
    public ClientResponse post(
        final String url
    ) throws IOException {
        return new ClientResponse(
            HttpStatus.SC_OK,
            this.result.toString(),
            headers()
        );
    }

    @Override
    public boolean isCached() {
        return true;
    }

    @Override
    public boolean isTest() {
        return false;
    }

    /**
     * Basic HTTP headers that satisfy {@link com.vk.api.sdk.client.ApiRequest}.
     * @return A {@link Map} with HTTP headers.
     */
    private static Map<String, String> headers() {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        return headers;
    }

}


