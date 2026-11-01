package de.jakub.seminarverwaltung;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;

/**
 * Bewusst ohne @Transactional: Datenbankfehler wie Fremdschlüssel-Verletzungen treten erst beim Commit auf,
 * in einer Test-Transaktion mit Rollback würden sie nie ausgelöst. Beide Fälle ändern keine Daten.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AlsKursleiter.class)
class RestFehlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BenutzerRepository benutzerRepository;

    @Test
    void loeschenEinesVerwendetenBenutzersLiefert409() throws Exception {
        long kursleiterId = benutzerRepository.findByEmail("anna.berger@example.com").orElseThrow().getId();

        mockMvc.perform(delete("/api/benutzer/{id}", kursleiterId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("wird noch verwendet")));
    }

    @Test
    void ungueltigeDatenImImportLiefern400() throws Exception {
        mockMvc.perform(post("/api/import/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "benutzer": [ { "vorname": "Max", "nachname": "Muster", "email": "keine-mail", "rolle": "TEILNEHMER" } ] }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("email")));
    }
}
