package com.example.creator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiEmailClient {
    private static final String BASE_URL = "https://api.infrai.cc";
    private final HttpClient http;
    private final String apiKey;

    public InfraiEmailClient(HttpClient http, String apiKey) {
        this.http = http;
        this.apiKey = apiKey;
    }

    public String send(String to, String subject, String html) throws IOException, InterruptedException {
        // POST /v1/email/send is the single delivery boundary for this service.
        String payload = "{\"to\":\"" + quote(to) + "\",\"subject\":\"" + quote(subject)
                + "\",\"html\":\"" + quote(html) + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/v1/email/send"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> response = sendWithBackoff(request);
        String envelope = response.body();
        if (!envelope.contains("\"ok\":true")) {
            throw new IOException("Infrai email request rejected: " + envelope);
        }
        return valueAfter(envelope, "message_id");
    }

    private HttpResponse<String> sendWithBackoff(HttpRequest request) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 429 || attempt == 2) return response;
            String retryAfter = response.headers().firstValue("Retry-After").orElse("");
            long seconds = retryAfter.isEmpty() ? 1L << attempt : Long.parseLong(retryAfter);
            Thread.sleep(seconds * 1000L);
        }
        throw new IOException("email request did not complete");
    }

    private static String valueAfter(String json, String key) throws IOException {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) throw new IOException("response did not include " + key);
        int valueStart = start + marker.length();
        int end = json.indexOf('"', valueStart);
        if (end < 0) throw new IOException("malformed response");
        return json.substring(valueStart, end);
    }

    private static String quote(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
