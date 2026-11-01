package de.jakub.seminarverwaltung;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.seminar.SeminarRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeminarverwaltungApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeminarRepository seminarRepository;

    @Autowired
    private BenutzerRepository benutzerRepository;

    @Test
    void csvTestdatenSindGeladen() throws Exception {
        mockMvc.perform(get("/api/seminare"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void anmeldungErhoehtTeilnehmeranzahl() throws Exception {
        long seminarId = seminarId("Clean Code Workshop");
        long benutzerId = benutzerId("felix.schmid@example.com");

        mockMvc.perform(post("/api/seminare/{id}/teilnehmer/{benutzerId}", seminarId, benutzerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anzahlTeilnehmer").value(1));
    }

    @Test
    void anmeldungBeiAusgebuchtemSeminarWirdAbgelehnt() throws Exception {
        long seminarId = seminarId("Spring Boot kompakt");
        long benutzerId = benutzerId("lena.hofmann@example.com");

        mockMvc.perform(post("/api/seminare/{id}/teilnehmer/{benutzerId}", seminarId, benutzerId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Seminar ist ausgebucht"));
    }

    @Test
    void kursleiterKannSichNichtSelbstAnmelden() throws Exception {
        long seminarId = seminarId("Java Grundlagen");
        long kursleiterId = benutzerId("anna.berger@example.com");

        mockMvc.perform(post("/api/seminare/{id}/teilnehmer/{benutzerId}", seminarId, kursleiterId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Der Kursleiter kann nicht Teilnehmer sein"));
    }

    @Test
    void aendernBehaeltTeilnehmer() throws Exception {
        long seminarId = seminarId("Java Grundlagen");

        mockMvc.perform(put("/api/seminare/{id}", seminarId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "typ": "ONLINE", "titel": "Java Grundlagen (neu)", "startDatum": "2026-11-16",
                                  "endDatum": "2026-11-18", "maxTeilnehmer": 12, "kursleiter": { "id": %d },
                                  "meetingLink": "https://meet.example.com/java" }
                                """.formatted(benutzerId("anna.berger@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titel").value("Java Grundlagen (neu)"))
                .andExpect(jsonPath("$.anzahlTeilnehmer").value(2));
    }

    @Test
    void seminartypKannNichtGewechseltWerden() throws Exception {
        long seminarId = seminarId("Java Grundlagen");

        mockMvc.perform(put("/api/seminare/{id}", seminarId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "typ": "PRAESENZ", "titel": "Java Grundlagen", "startDatum": "2026-11-16",
                                  "endDatum": "2026-11-18", "maxTeilnehmer": 12, "kursleiter": { "id": %d },
                                  "ort": "Augsburg" }
                                """.formatted(benutzerId("anna.berger@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Der Seminartyp kann nicht geändert werden"));
    }

    @Test
    void leihenOhneVerfuegbarBisWirdAbgelehnt() throws Exception {
        mockMvc.perform(post("/api/unterlagen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titel": "Testbuch", "art": "BUCH", "bezugsart": "LEIHEN", "gebuehr": 5.00 }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void abgelaufeneLeiheIstUeberfaellig() throws Exception {
        mockMvc.perform(get("/api/unterlagen").param("titel", "Insel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ueberfaellig").value(true));
    }

    @Test
    void jsonImportLegtDatenAn() throws Exception {
        String json = new ClassPathResource("testdaten/schnittstelle.json").getContentAsString(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/import/json").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seminare").value(2));

        mockMvc.perform(get("/api/seminare").param("titel", "Kubernetes"))
                .andExpect(jsonPath("$[0].typ").value("ONLINE"))
                .andExpect(jsonPath("$[0].kursleiter.email").value("clara.neumann@example.com"));
    }

    private long seminarId(String titel) {
        return seminarRepository.findByTitel(titel).orElseThrow().getId();
    }

    private long benutzerId(String email) {
        return benutzerRepository.findByEmail(email).orElseThrow().getId();
    }
}
