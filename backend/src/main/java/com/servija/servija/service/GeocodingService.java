package com.servija.servija.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private final ObjectMapper objectMapper;

    public record Coordinates(BigDecimal latitude, BigDecimal longitude) {}

    public Optional<Coordinates> geocodeCity(String city, String state) {
        try {
            String query = URLEncoder.encode(city + ", " + state + ", Brasil", StandardCharsets.UTF_8);
            String url = NOMINATIM_URL + "?format=json&q=" + query + "&limit=1";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "ServiJa/1.0")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode results = objectMapper.readTree(response.body());
            if (results.isArray() && !results.isEmpty()) {
                JsonNode first = results.get(0);
                BigDecimal lat = new BigDecimal(first.get("lat").asText());
                BigDecimal lon = new BigDecimal(first.get("lon").asText());
                return Optional.of(new Coordinates(lat, lon));
            }
        } catch (Exception e) {
            log.warn("Falha ao geocodificar cidade '{}'/'{}'': {}", city, state, e.getMessage());
        }
        return Optional.empty();
    }
}
