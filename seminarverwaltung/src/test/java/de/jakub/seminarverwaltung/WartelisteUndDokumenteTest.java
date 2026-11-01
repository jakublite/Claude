package de.jakub.seminarverwaltung;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.nachricht.Anlass;
import de.jakub.seminarverwaltung.nachricht.Nachricht;
import de.jakub.seminarverwaltung.nachricht.NachrichtRepository;
import de.jakub.seminarverwaltung.nachricht.Vorlage;
import de.jakub.seminarverwaltung.nachricht.VorlageRepository;
import de.jakub.seminarverwaltung.seminar.SeminarRepository;
import de.jakub.seminarverwaltung.zertifikat.ZertifikatRepository;

/** "Spring Boot kompakt" ist in den Testdaten mit Sophie und Felix ausgebucht (2 Plätze). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WartelisteUndDokumenteTest {

    private static final String VOLL = "Spring Boot kompakt";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeminarRepository seminarRepository;

    @Autowired
    private BenutzerRepository benutzerRepository;

    @Autowired
    private ZertifikatRepository zertifikatRepository;

    @Autowired
    private NachrichtRepository nachrichtRepository;

    @Autowired
    private VorlageRepository vorlageRepository;

    @Test
    void importErzeugtKeineEmails() {
        assertThat(nachrichtRepository.count()).isZero();
    }

    @Test
    void ausgebuchtKommtAufWarteliste() throws Exception {
        mockMvc.perform(post(warteliste(VOLL, "lena.hofmann@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warteliste[0].email").value("lena.hofmann@example.com"))
                .andExpect(jsonPath("$.anzahlTeilnehmer").value(2));

        assertThat(letzteNachricht().getAnlass()).isEqualTo(Anlass.WARTELISTE);
    }

    @Test
    void wartelisteNurWennAusgebucht() throws Exception {
        mockMvc.perform(post(warteliste("Clean Code Workshop", "lena.hofmann@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("noch Plätze frei")));
    }

    @Test
    void nichtDoppeltUndNichtAlsTeilnehmerAufDieWarteliste() throws Exception {
        mockMvc.perform(post(warteliste(VOLL, "lena.hofmann@example.com"))).andExpect(status().isOk());

        mockMvc.perform(post(warteliste(VOLL, "lena.hofmann@example.com"))).andExpect(status().isConflict());
        mockMvc.perform(post(warteliste(VOLL, "sophie.wagner@example.com"))).andExpect(status().isConflict());
        mockMvc.perform(post(teilnehmer(VOLL, "lena.hofmann@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Benutzer steht bereits auf der Warteliste"));
    }

    @Test
    void abmeldungLaesstErstenDerWartelisteNachruecken() throws Exception {
        mockMvc.perform(post(warteliste(VOLL, "lena.hofmann@example.com"))).andExpect(status().isOk());
        mockMvc.perform(post(warteliste(VOLL, "murat.yilmaz@example.com"))).andExpect(status().isOk());

        mockMvc.perform(delete(teilnehmer(VOLL, "sophie.wagner@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anzahlTeilnehmer").value(2))
                .andExpect(jsonPath("$.teilnehmer[1].email").value("lena.hofmann@example.com"))
                .andExpect(jsonPath("$.warteliste.length()").value(1))
                .andExpect(jsonPath("$.warteliste[0].email").value("murat.yilmaz@example.com"));

        Nachricht nachricht = letzteNachricht();
        assertThat(nachricht.getAnlass()).isEqualTo(Anlass.NACHRUECKEN);
        assertThat(nachricht.getEmpfaenger()).isEqualTo("lena.hofmann@example.com");
        assertThat(nachricht.getInhalt()).contains("Lena Hofmann", VOLL).doesNotContain("{");
    }

    @Test
    void mehrPlaetzeLassenWartelisteNachruecken() throws Exception {
        mockMvc.perform(post(warteliste(VOLL, "lena.hofmann@example.com"))).andExpect(status().isOk());

        mockMvc.perform(put("/api/seminare/{id}", seminarId(VOLL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "typ": "ONLINE", "titel": "%s", "startDatum": "2026-12-01", "endDatum": "2026-12-02",
                                  "maxTeilnehmer": 3, "kursleiter": { "id": %d }, "meetingLink": "https://meet.example.com/x" }
                                """.formatted(VOLL, benutzerId("anna.berger@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anzahlTeilnehmer").value(3))
                .andExpect(jsonPath("$.warteliste.length()").value(0));
    }

    @Test
    void vonWartelisteNehmen() throws Exception {
        mockMvc.perform(post(warteliste(VOLL, "lena.hofmann@example.com"))).andExpect(status().isOk());

        mockMvc.perform(delete(warteliste(VOLL, "lena.hofmann@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warteliste.length()").value(0));
        mockMvc.perform(delete(warteliste(VOLL, "lena.hofmann@example.com"))).andExpect(status().isNotFound());
    }

    @Test
    void geaenderteVorlageGiltFuerNeueEmails() throws Exception {
        Vorlage vorlage = new Vorlage();
        vorlage.setAnlass(Anlass.ANMELDUNG);
        vorlage.setBetreff("Willkommen bei {seminar}");
        vorlage.setInhalt("Moin {vorname}");
        vorlageRepository.save(vorlage);

        mockMvc.perform(post(teilnehmer("Clean Code Workshop", "felix.schmid@example.com"))).andExpect(status().isOk());

        assertThat(letzteNachricht().getBetreff()).isEqualTo("Willkommen bei Clean Code Workshop");
        assertThat(letzteNachricht().getInhalt()).isEqualTo("Moin Felix");
    }

    @Test
    void nachrichtenPerRestNachEmpfaengerFiltern() throws Exception {
        mockMvc.perform(post(teilnehmer("Clean Code Workshop", "felix.schmid@example.com"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/nachrichten").param("empfaenger", "felix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].anlass").value("ANMELDUNG"));
        mockMvc.perform(get("/api/nachrichten").param("empfaenger", "niemand"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void neuesZertifikatLoestEmailAusUndLiefertVollstaendigeDaten() throws Exception {
        mockMvc.perform(post("/api/zertifikate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "seminar": { "id": %d, "typ": "ONLINE" }, "teilnehmer": { "id": %d },
                                  "niveau": "A", "ausstellungsdatum": "2026-12-02" }
                                """.formatted(seminarId(VOLL), benutzerId("sophie.wagner@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seminar.titel").value(VOLL))
                .andExpect(jsonPath("$.teilnehmer.nachname").value("Wagner"));

        Nachricht nachricht = letzteNachricht();
        assertThat(nachricht.getAnlass()).isEqualTo(Anlass.ZERTIFIKAT);
        assertThat(nachricht.getEmpfaenger()).isEqualTo("sophie.wagner@example.com");
    }

    @Test
    void zertifikatFuerUnbekanntesSeminarLiefert404() throws Exception {
        mockMvc.perform(post("/api/zertifikate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "seminar": { "id": 999999, "typ": "ONLINE" }, "teilnehmer": { "id": %d },
                                  "niveau": "A", "ausstellungsdatum": "2026-12-02" }
                                """.formatted(benutzerId("sophie.wagner@example.com"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void teilnehmerlisteAlsCsvMitWarteliste() throws Exception {
        mockMvc.perform(post(warteliste(VOLL, "lena.hofmann@example.com"))).andExpect(status().isOk());

        String csv = mockMvc.perform(get("/api/seminare/{id}/teilnehmerliste.csv", seminarId(VOLL)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv).startsWith("﻿Status;Vorname;Nachname;E-Mail\r\n")
                .contains("\"Teilnehmer\";\"Sophie\";\"Wagner\";\"sophie.wagner@example.com\"\r\n")
                .contains("\"Warteliste 1\";\"Lena\";\"Hofmann\";\"lena.hofmann@example.com\"\r\n");
    }

    @Test
    void csvEntschaerftFormelnUndAnfuehrungszeichen() throws Exception {
        var benutzer = benutzerRepository.findByEmail("felix.schmid@example.com").orElseThrow();
        benutzer.setVorname("=SUMME(A1)");
        benutzer.setNachname("Schmid \"Fix\"");

        String csv = mockMvc.perform(get("/api/seminare/{id}/teilnehmerliste.csv", seminarId(VOLL)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv).contains("\"'=SUMME(A1)\";\"Schmid \"\"Fix\"\"\"");
    }

    @Test
    void zertifikatDokumentIstEscaptesHtml() throws Exception {
        var zertifikat = zertifikatRepository.findAll().getFirst();
        zertifikat.getTeilnehmer().setVorname("<script>alert(1)</script>");

        mockMvc.perform(get("/api/zertifikate/{id}/dokument", zertifikat.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Niveau " + zertifikat.getNiveau())))
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<script>"))));
    }

    @Test
    void unbekannteDokumenteLiefern404() throws Exception {
        mockMvc.perform(get("/api/zertifikate/999999/dokument")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/seminare/999999/teilnehmerliste.csv")).andExpect(status().isNotFound());
    }

    private Nachricht letzteNachricht() {
        return nachrichtRepository.findAllByOrderByIdDesc().getFirst();
    }

    private String warteliste(String titel, String email) {
        return "/api/seminare/" + seminarId(titel) + "/warteliste/" + benutzerId(email);
    }

    private String teilnehmer(String titel, String email) {
        return "/api/seminare/" + seminarId(titel) + "/teilnehmer/" + benutzerId(email);
    }

    private long seminarId(String titel) {
        return seminarRepository.findByTitel(titel).orElseThrow().getId();
    }

    private long benutzerId(String email) {
        return benutzerRepository.findByEmail(email).orElseThrow().getId();
    }
}
