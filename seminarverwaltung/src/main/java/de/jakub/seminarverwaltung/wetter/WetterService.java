package de.jakub.seminarverwaltung.wetter;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Holt das aktuelle Wetter für einen Ort über Open-Meteo (kostenlos, ohne API-Key).
 * Schritt 1: Ortsname → Koordinaten (Geocoding-API), Schritt 2: Koordinaten → Wetter (Forecast-API).
 */
@Service
public class WetterService {

    private final RestClient geocodingClient;
    private final RestClient forecastClient;

    @Autowired
    public WetterService(@Value("${wetter.geocoding-url}") String geocodingUrl,
            @Value("${wetter.forecast-url}") String forecastUrl) {
        this(RestClient.builder().requestFactory(mitTimeouts()), geocodingUrl, forecastUrl);
    }

    /** Für Tests: der Builder kann an einen MockRestServiceServer gebunden sein. */
    WetterService(RestClient.Builder builder, String geocodingUrl, String forecastUrl) {
        this.geocodingClient = builder.clone().baseUrl(geocodingUrl).build();
        this.forecastClient = builder.clone().baseUrl(forecastUrl).build();
    }

    /** Ohne Timeouts würde ein hängender Wetterdienst die Oberfläche blockieren. */
    private static SimpleClientHttpRequestFactory mitTimeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    public Wetter ermittleWetter(String ort) {
        GeoAntwort geo = geocodingClient.get()
                .uri(uri -> uri.path("/v1/search")
                        .queryParam("name", ort)
                        .queryParam("count", 1)
                        .queryParam("language", "de")
                        .build())
                .retrieve()
                .body(GeoAntwort.class);
        if (geo == null || geo.results() == null || geo.results().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ort '" + ort + "' nicht gefunden");
        }
        GeoTreffer treffer = geo.results().getFirst();

        WetterAntwort wetter = forecastClient.get()
                .uri(uri -> uri.path("/v1/forecast")
                        .queryParam("latitude", treffer.latitude())
                        .queryParam("longitude", treffer.longitude())
                        .queryParam("current", "temperature_2m,precipitation_probability,wind_speed_10m")
                        .queryParam("timezone", "Europe/Berlin")
                        .build())
                .retrieve()
                .body(WetterAntwort.class);

        Aktuell aktuell = wetter.current();
        return new Wetter(treffer.name(), aktuell.temperatur(), aktuell.niederschlagswahrscheinlichkeit(),
                aktuell.windgeschwindigkeit());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeoAntwort(List<GeoTreffer> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeoTreffer(String name, double latitude, double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WetterAntwort(Aktuell current) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Aktuell(
            @JsonProperty("temperature_2m") double temperatur,
            @JsonProperty("precipitation_probability") int niederschlagswahrscheinlichkeit,
            @JsonProperty("wind_speed_10m") double windgeschwindigkeit) {
    }
}
