package de.jakub.seminarverwaltung;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import de.jakub.seminarverwaltung.benutzer.Benutzer;
import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.zertifikat.Zertifikat;
import de.jakub.seminarverwaltung.zertifikat.ZertifikatRepository;

/** Die Tests melden sich wie ein echter Client per HTTP Basic an, mit den Zugängen aus den Testdaten. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SicherheitTest {

    private static final String PASSWORT = "demo1234";
    private static final String KURSLEITER = "anna.berger@example.com";
    private static final String LENA = "lena.hofmann@example.com";
    private static final String MURAT = "murat.yilmaz@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BenutzerRepository benutzerRepository;

    @Autowired
    private ZertifikatRepository zertifikatRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void ohneLoginGibt401OhneBrowserDialog() throws Exception {
        mockMvc.perform(get("/api/seminare"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
        mockMvc.perform(post("/api/benutzer").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void falschesPasswortGibt401OhneBrowserDialog() throws Exception {
        mockMvc.perform(get("/api/seminare").with(httpBasic(KURSLEITER, "falsch")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void kursleiterDarfDieApiNutzen() throws Exception {
        mockMvc.perform(get("/api/benutzer").with(als(KURSLEITER))).andExpect(status().isOk());
    }

    @Test
    void teilnehmerDarfDieApiNichtNutzen() throws Exception {
        mockMvc.perform(get("/api/seminare").with(als(LENA))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/benutzer").with(als(LENA))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/nachrichten").with(als(LENA))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/import/json").with(als(LENA)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void benutzerOhnePasswortKannSichNichtAnmelden() throws Exception {
        Benutzer ohnePasswort = benutzerRepository.findByEmail(MURAT).orElseThrow();
        ohnePasswort.setPasswortHash(null);
        benutzerRepository.saveAndFlush(ohnePasswort);

        mockMvc.perform(get("/api/seminare").with(httpBasic(MURAT, PASSWORT))).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seminare").with(httpBasic(MURAT, ""))).andExpect(status().isUnauthorized());
    }

    @Test
    void passwortHashStehtNieImJson() throws Exception {
        mockMvc.perform(get("/api/benutzer").with(als(KURSLEITER)))
                .andExpect(content().string(not(containsString("passwort"))));
        mockMvc.perform(get("/api/seminare").with(als(KURSLEITER)))
                .andExpect(content().string(not(containsString("passwort"))));
    }

    @Test
    void passwortHashLaesstSichPerJsonNichtSetzen() throws Exception {
        mockMvc.perform(post("/api/benutzer").with(als(KURSLEITER)).contentType(MediaType.APPLICATION_JSON).content("""
                { "vorname": "Mia", "nachname": "Neu", "email": "mia.neu@example.com", "rolle": "KURSLEITER", "passwortHash": "untergeschoben" }
                """)).andExpect(status().isCreated());

        assertThat(benutzerRepository.findByEmail("mia.neu@example.com").orElseThrow().getPasswortHash()).isNull();
    }

    @Test
    void putBehaeltDasPasswort() throws Exception {
        Benutzer lena = benutzerRepository.findByEmail(LENA).orElseThrow();

        mockMvc.perform(put("/api/benutzer/{id}", lena.getId()).with(als(KURSLEITER)).contentType(MediaType.APPLICATION_JSON).content("""
                { "vorname": "Lena", "nachname": "Neu", "email": "lena.hofmann@example.com", "rolle": "TEILNEHMER", "passwortHash": "untergeschoben" }
                """)).andExpect(status().isOk());

        benutzerRepository.flush();
        Benutzer danach = benutzerRepository.findByEmail(LENA).orElseThrow();
        assertThat(danach.getNachname()).isEqualTo("Neu");
        assertThat(passwordEncoder.matches(PASSWORT, danach.getPasswortHash())).isTrue();
    }

    @Test
    void zertifikatDarfNurDerBesitzerOderEinKursleiterOeffnen() throws Exception {
        String pfad = "/api/zertifikate/" + zertifikatVonLena().getId() + "/dokument";

        mockMvc.perform(get(pfad)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(pfad).with(als(LENA))).andExpect(status().isOk());
        mockMvc.perform(get(pfad).with(als(KURSLEITER))).andExpect(status().isOk());
        mockMvc.perform(get(pfad).with(als(MURAT))).andExpect(status().isForbidden());
    }

    @Test
    void zertifikatLinkFunktioniertMitBrowserSession() throws Exception {
        String pfad = "/api/zertifikate/" + zertifikatVonLena().getId() + "/dokument";

        mockMvc.perform(get(pfad).with(user(LENA).roles("TEILNEHMER"))).andExpect(status().isOk());
        mockMvc.perform(get(pfad).with(user(MURAT).roles("TEILNEHMER"))).andExpect(status().isForbidden());
    }

    @Test
    void teilnehmerlisteNurFuerKursleiter() throws Exception {
        long seminarId = zertifikatVonLena().getSeminar().getId();

        mockMvc.perform(get("/api/seminare/{id}/teilnehmerliste.csv", seminarId)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seminare/{id}/teilnehmerliste.csv", seminarId).with(als(LENA))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/seminare/{id}/teilnehmerliste.csv", seminarId).with(als(KURSLEITER))).andExpect(status().isOk());
    }

    @Test
    void oberflaecheLeitetOhneLoginZumFormular() throws Exception {
        mockMvc.perform(get("/seminare")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    void h2ConsoleNurFuerKursleiter() throws Exception {
        mockMvc.perform(get("/h2-console/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/h2-console/").with(user(LENA).roles("TEILNEHMER"))).andExpect(status().isForbidden());
    }

    private static RequestPostProcessor als(String email) {
        return httpBasic(email, PASSWORT);
    }

    private Zertifikat zertifikatVonLena() {
        return zertifikatRepository.findAll().stream()
                .filter(z -> LENA.equals(z.getTeilnehmer().getEmail()))
                .findFirst()
                .orElseThrow();
    }
}
