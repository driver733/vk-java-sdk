package com.vk.api.sdk.httpclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.TransportClient;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @param result Cached request`s response.
     */
    public TransportClientCached(final JsonElement result) {
        this.result = result;
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


