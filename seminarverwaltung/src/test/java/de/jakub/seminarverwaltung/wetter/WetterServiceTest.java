package de.jakub.seminarverwaltung.wetter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/** Prüft das Zusammenspiel Geocoding → Forecast ohne echten Netzwerkzugriff. */
class WetterServiceTest {

    private MockRestServiceServer server;
    private WetterService wetterService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        wetterService = new WetterService(builder, "https://geo.test", "https://wetter.test");
    }

    @Test
    void liefertAktuellesWetterFuerOrt() {
        server.expect(requestTo(allOf(containsString("https://geo.test/v1/search"), containsString("name=Augsburg"))))
                .andRespond(withSuccess("""
                        { "results": [ { "name": "Augsburg", "latitude": 48.37, "longitude": 10.89, "country": "Deutschland" } ] }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(allOf(containsString("https://wetter.test/v1/forecast"), containsString("latitude=48.37"))))
                .andRespond(withSuccess("""
                        { "current": { "temperature_2m": 9.24, "precipitation_probability": 30, "wind_speed_10m": 12.0 } }
                        """, MediaType.APPLICATION_JSON));

        Wetter wetter = wetterService.ermittleWetter("Augsburg");

        assertThat(wetter.ort()).isEqualTo("Augsburg");
        assertThat(wetter.niederschlagswahrscheinlichkeitProzent()).isEqualTo(30);
        assertThat(wetter.temperaturText()).isEqualTo("9,2 °C");
        assertThat(wetter.windText()).isEqualTo("12,0 km/h");
        assertThat(wetter.alsHtml()).contains("Wetter in Augsburg", "9,2 °C", "30 %");
        server.verify();
    }

    @Test
    void unbekannterOrtLiefert404() {
        server.expect(requestTo(containsString("name=Nirgendwo")))
                .andRespond(withSuccess("{ }", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> wetterService.ermittleWetter("Nirgendwo"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("nicht gefunden");
    }
}
