package com.vk.api.sdk.httpclient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.queries.execute.ExecuteBatchQuery;

/**
 * Class or Interface description.
 * <p>
 * Additional info
 *
 * @author Mikhail Yakushin (driver733@me.com)
 * @version $Id$
 * @since 0.1
 */
public class TransportClientExecuteBatchCached implements TransportClient {

    private static final Logger LOG = LoggerFactory.getLogger(TransportClientHttp.class);

    private static final String ENCODING = "UTF-8";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String USER_AGENT = "Java VK SDK/0.5.2";

    private static final int MAX_SIMULTANEOUS_CONNECTIONS = 300;
    private static final int FULL_CONNECTION_TIMEOUT_S = 60;
    private static final int CONNECTION_TIMEOUT_MS = 5_000;
    private static final int SOCKET_TIMEOUT_MS = FULL_CONNECTION_TIMEOUT_S * 1000;

    private static final ConnectionsSupervisor SUPERVISOR = new ConnectionsSupervisor();
    private static TransportClientHttp instance;
    private static HttpClient httpClient;

    private final QueryResults cachedResults;

    public TransportClientExecuteBatchCached(final QueryResults cachedResults) {
        this.cachedResults = cachedResults;
        init();
    }

    private void init() {
        CookieStore cookieStore = new BasicCookieStore();
        RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(SOCKET_TIMEOUT_MS)
            .setConnectTimeout(CONNECTION_TIMEOUT_MS)
            .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
            .setCookieSpec(CookieSpecs.STANDARD)
            .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

        connectionManager.setMaxTotal(MAX_SIMULTANEOUS_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_SIMULTANEOUS_CONNECTIONS);

        httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .setDefaultCookieStore(cookieStore)
            .setUserAgent(USER_AGENT)
            .build();
    }

    @SuppressWarnings("unused")
    public static TransportClientHttp getInstance() {
        if (instance == null) {
            instance = new TransportClientHttp();
        }

        return instance;
    }

    private static Map<String, String> getHeaders(Header[] headers) {
        Map<String, String> result = new HashMap<>();
        for (Header header : headers) {
            result.put(header.getName(), header.getValue());
        }

        return result;
    }

    private ClientResponse call(HttpPost request) throws IOException {
        SocketException exception = null;
        if (this.cachedResults.fullyCached()) {
            Gson gson = new Gson();
            final HashMap<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            JsonObject obj = new JsonObject();
            final List<JsonElement> list = new ArrayList<>();
            for (JsonElement element : cachedResults.results()) {
                if (element.isJsonObject()) {
                    if (element.getAsJsonObject().has("response")) {
                        list.add(element.getAsJsonObject().get("response"));
                    } else {
                        list.add(element.getAsJsonObject()); // extra?
                    }
                } else {
                    list.add(element);
                }
            }
            obj.add("response", gson.toJsonTree(list));
            return new ClientResponse(200, gson.toJson(obj), headers);
        }

        for (int i = 0; i < 3; i++) {
            try {
                SUPERVISOR.addRequest(request);

                long startTime = System.currentTimeMillis();

                HttpResponse response = httpClient.execute(request);

                long endTime = System.currentTimeMillis();

                long resultTime = endTime - startTime;

                try (InputStream content = response.getEntity().getContent()) {
                    String result = IOUtils.toString(content, ENCODING);
                    Map<String, String> headers = getHeaders(response.getAllHeaders());
                    logRequest(request, response, headers, result, resultTime);

                    JsonReader jsonReader = new JsonReader(new StringReader(result));
                    JsonObject json = new JsonParser().parse(jsonReader).getAsJsonObject();
                    JsonArray array = json.get("response").getAsJsonArray();

                    for (final JsonElement element : cachedResults.results()) {
                        array.add(element);
                    }
                    json.add("response", array);

                    return new ClientResponse(response.getStatusLine().getStatusCode(), json.toString(), headers);
                } finally {
                    SUPERVISOR.removeRequest(request);
                }
            } catch (SocketException e) {
                logRequest(request);
                LOG.warn("Network troubles", e);
                exception = e;
            }
        }

        throw exception;
    }

    private void logRequest(HttpPost request) throws IOException {
        logRequest(request, null, null, null, null);
    }

    private void logRequest(HttpPost request, HttpResponse response, Map<String, String> headers, String body, Long time) throws IOException {
        if (LOG.isDebugEnabled()) {
            Header contentType = request.getFirstHeader(CONTENT_TYPE_HEADER);
            String payload = "-";
            if (contentType != null && contentType.getValue().equalsIgnoreCase(CONTENT_TYPE)) {
                if (request.getEntity() != null) {
                    payload = IOUtils.toString(request.getEntity().getContent(), StandardCharsets.UTF_8);
                }
            }

            StringBuilder builder = new StringBuilder("\n")
                .append("Request:\n")
                .append("\t").append("Method: ").append(request.getMethod()).append("\n")
                .append("\t").append("URI: ").append(request.getURI()).append("\n")
                .append("\t").append("Payload: ").append(payload).append("\n")
                .append("\t").append("Time: ").append(time != null ? time : "-").append("\n");

            if (response != null) {
                builder.append("Response:\n")
                    .append("\t").append("Status: ").append(response.getStatusLine().toString()).append("\n")
                    .append("\t").append("Headers: ").append(headers != null ? headers : "-").append("\n")
                    .append("\t").append("Body: ").append(body != null ? body : "-").append("\n");
            }

            LOG.debug(builder.toString());
        } else if (LOG.isInfoEnabled()) {
            StringBuilder builder = new StringBuilder().append("Request: ").append(request.getURI().toURL().toString());
            if (time != null) {
                builder.append("\t\t").append(time);
            }

            LOG.info(builder.toString());
        }
    }

    @Override
    public ClientResponse post(String url, String body) throws IOException {
        HttpPost request = new HttpPost(url);
        request.setHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE);
        if (body != null) {
            request.setEntity(new StringEntity(body));
        }

        return call(request);
    }

    @Override
    public ClientResponse post(String url) throws IOException {
        HttpPost request = new HttpPost(url);
        request.setHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE);
        return call(request);
    }

    @Override
    public boolean isCached() {
        return false;
    }

    @Override
    public boolean isTest() {
        return false;
    }

    @Override
    public ClientResponse post(String url, String fileName, File file) throws IOException {
        HttpPost request = new HttpPost(url);
        FileBody fileBody = new FileBody(file);
        HttpEntity entity = MultipartEntityBuilder
            .create()
            .addPart(fileName, fileBody).build();

        request.setEntity(entity);
        return call(request);
    }
}
