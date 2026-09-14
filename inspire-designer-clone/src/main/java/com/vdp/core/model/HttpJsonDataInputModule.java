package com.vdp.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdp.core.model.ExecutionContext.DataType;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads a JSON array from an HTTP GET endpoint.
 *
 * Example endpoint:
 * https://jsonplaceholder.typicode.com/users
 *
 * Example JSON path:
 * $              -> root JSON array
 * $.data         -> "data" array inside the root object
 * $.data.users   -> nested "users" array
 */
public class HttpJsonDataInputModule extends BaseDataInputModule {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient httpClient;
    private final Map<String, String> headers = new LinkedHashMap<>();

    private String endpointUrl;
    private String rootArrayName = "Records";
    private String jsonArrayPath = "$";
    private int timeoutMilliseconds = 10_000;

    public HttpJsonDataInputModule() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    public HttpJsonDataInputModule(HttpClient httpClient) {
        super("HttpJsonDataInput1");
        this.httpClient = httpClient;
    }

    @Override
    protected void validateConfiguration(List<String> errors) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            errors.add("API URL is missing.");
        } else {
            try {
                URI uri = URI.create(endpointUrl);

                if (!"http".equalsIgnoreCase(uri.getScheme())
                        && !"https".equalsIgnoreCase(uri.getScheme())) {
                    errors.add("API URL must use HTTP or HTTPS.");
                }
            } catch (IllegalArgumentException exception) {
                errors.add("API URL is invalid.");
            }
        }

        if (rootArrayName == null || rootArrayName.isBlank()) {
            errors.add("Root array name cannot be blank.");
        }

        if (jsonArrayPath == null || jsonArrayPath.isBlank()) {
            errors.add("JSON array path cannot be blank.");
        } else if (!jsonArrayPath.equals("$")
                && !jsonArrayPath.startsWith("$.")) {
            errors.add("JSON path must be $ or start with $.");
        }

        if (timeoutMilliseconds <= 0) {
            errors.add("Timeout must be greater than zero.");
        }
    }

    @Override
    protected DataInputResult readData() throws Exception {
        String responseBody = downloadJson();
        JsonNode root = JSON.readTree(responseBody);

        if (root == null) {
            throw new IllegalStateException("The API returned an empty JSON document.");
        }

        JsonNode arrayNode = resolveJsonPath(root, jsonArrayPath);

        if (arrayNode == null || arrayNode.isMissingNode()) {
            throw new IllegalStateException(
                    "JSON path was not found: " + jsonArrayPath
            );
        }

        if (!arrayNode.isArray()) {
            throw new IllegalStateException(
                    "JSON path does not point to an array: " + jsonArrayPath
            );
        }

        List<JsonNode> objects = new ArrayList<>();
        Set<String> fieldNames = new LinkedHashSet<>();

        for (JsonNode record : arrayNode) {
            if (!record.isObject()) {
                throw new IllegalStateException(
                        "Every record in the selected JSON array must be an object."
                );
            }

            objects.add(record);
            record.fieldNames().forEachRemaining(fieldNames::add);
        }

        String[] columnNames = fieldNames.toArray(String[]::new);
        List<String[]> records = new ArrayList<>();

        for (JsonNode object : objects) {
            String[] row = new String[columnNames.length];

            for (int index = 0; index < columnNames.length; index++) {
                JsonNode value = object.get(columnNames[index]);
                row[index] = valueToString(value);
            }

            records.add(row);
        }

        DataType[] types = inferColumnTypes(columnNames, objects);

        return new DataInputResult(
                rootArrayName,
                columnNames,
                types,
                records
        );
    }

    /**
     * Separate method so tests can override it without making real HTTP calls.
     */
    protected String downloadJson() throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .GET()
                .timeout(Duration.ofMillis(timeoutMilliseconds))
                .header("Accept", "application/json");

        for (Map.Entry<String, String> header : headers.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }

        HttpResponse<String> response = httpClient.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString()
        );

        int statusCode = response.statusCode();

        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException(
                    "HTTP request failed with status " + statusCode
            );
        }

        return response.body();
    }

    private JsonNode resolveJsonPath(JsonNode root, String path) {
        if ("$".equals(path.trim())) {
            return root;
        }

        String normalizedPath = path.trim().substring(2);
        JsonNode current = root;

        for (String key : normalizedPath.split("\\.")) {
            if (key.isBlank() || current == null) {
                return null;
            }

            current = current.get(key);
        }

        return current;
    }

    private String valueToString(JsonNode value) {
        if (value == null || value.isNull()) {
            return "";
        }

        if (value.isValueNode()) {
            return value.asText();
        }

        return value.toString();
    }

    private DataType[] inferColumnTypes(
            String[] columnNames,
            List<JsonNode> objects) {

        DataType[] types = new DataType[columnNames.length];
        Arrays.fill(types, DataType.STRING);

        for (int index = 0; index < columnNames.length; index++) {
            boolean hasValue = false;
            boolean allValuesAreNumbers = true;

            for (JsonNode object : objects) {
                JsonNode value = object.get(columnNames[index]);

                if (value == null || value.isNull()) {
                    continue;
                }

                hasValue = true;

                if (!value.isNumber()) {
                    allValuesAreNumbers = false;
                    break;
                }
            }

            if (hasValue && allValuesAreNumbers) {
                types[index] = DataType.NUMBER;
            }
        }

        return types;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public String getRootArrayName() {
        return rootArrayName;
    }

    public String getJsonArrayPath() {
        return jsonArrayPath;
    }

    public int getTimeoutMilliseconds() {
        return timeoutMilliseconds;
    }

    public Map<String, String> getHeaders() {
        return Map.copyOf(headers);
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public void setRootArrayName(String rootArrayName) {
        this.rootArrayName = rootArrayName;
    }

    public void setJsonArrayPath(String jsonArrayPath) {
        this.jsonArrayPath = jsonArrayPath;
    }

    public void setTimeoutMilliseconds(int timeoutMilliseconds) {
        this.timeoutMilliseconds = timeoutMilliseconds;
    }

    public void addHeader(String name, String value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("HTTP header name cannot be blank.");
        }

        if (value == null) {
            throw new IllegalArgumentException("HTTP header value cannot be null.");
        }

        headers.put(name, value);
    }

    public void removeHeader(String name) {
        headers.remove(name);
    }
}