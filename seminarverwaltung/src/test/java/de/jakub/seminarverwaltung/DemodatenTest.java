package de.jakub.seminarverwaltung;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.seminar.Seminar;
import de.jakub.seminarverwaltung.seminar.SeminarRepository;
import de.jakub.seminarverwaltung.unterlage.SchulungsunterlageRepository;
import de.jakub.seminarverwaltung.zertifikat.Zertifikat;
import de.jakub.seminarverwaltung.zertifikat.ZertifikatRepository;

/** Stellt sicher, dass die Demodaten importierbar sind und die Fachregeln einhalten, die der Import nicht prüft. */
@SpringBootTest(properties = "testdaten.ordner=demodaten")
class DemodatenTest {

    @Autowired
    private BenutzerRepository benutzerRepository;

    @Autowired
    private SeminarRepository seminarRepository;

    @Autowired
    private ZertifikatRepository zertifikatRepository;

    @Autowired
    private SchulungsunterlageRepository unterlageRepository;

    @Test
    void allesWurdeImportiert() {
        assertThat(benutzerRepository.count()).isEqualTo(20);
        assertThat(seminarRepository.count()).isEqualTo(14);
        assertThat(zertifikatRepository.count()).isEqualTo(15);
        assertThat(unterlageRepository.count()).isEqualTo(15);
    }

    @Test
    void seminareHaltenAnmelderegelnEin() {
        for (Seminar seminar : seminarRepository.findAll()) {
            assertThat(seminar.getAnzahlTeilnehmer()).as(seminar.getTitel()).isLessThanOrEqualTo(seminar.getMaxTeilnehmer());
            assertThat(seminar.getTeilnehmer()).as(seminar.getTitel()).doesNotContain(seminar.getKursleiter()).doesNotHaveDuplicates();
            if (!seminar.getWarteliste().isEmpty()) {
                assertThat(seminar.isAusgebucht()).as(seminar.getTitel()).isTrue();
            }
            assertThat(seminar.getWarteliste()).as(seminar.getTitel())
                    .doesNotContain(seminar.getKursleiter()).doesNotHaveDuplicates()
                    .noneMatch(seminar.getTeilnehmer()::contains);
        }
    }

    @Test
    void zertifikateGehoerenZuTeilnehmernUndAbgeschlossenemSeminar() {
        for (Zertifikat zertifikat : zertifikatRepository.findAll()) {
            Seminar seminar = seminarRepository.findById(zertifikat.getSeminar().getId()).orElseThrow();
            String beschreibung = seminar.getTitel() + " / " + zertifikat.getTeilnehmer().getEmail();
            assertThat(seminar.getTeilnehmer()).as(beschreibung).extracting("id").contains(zertifikat.getTeilnehmer().getId());
            assertThat(zertifikat.getAusstellungsdatum()).as(beschreibung).isAfterOrEqualTo(seminar.getEndDatum());
        }
    }
}
