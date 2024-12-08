package xyz.funtimes909.serverseekerv2_core.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HTTPUtils {
    public static String run(String ip) {
        try (HttpClient client = HttpClient.newHttpClient()) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/" + ip + "?fields=status,message,continent,countryCode,org,as,reverse,query"))
                    .build();

            HttpResponse<String> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join();
            if (response.statusCode() == 429) return null;
            return response.body();
        }
    }
}
