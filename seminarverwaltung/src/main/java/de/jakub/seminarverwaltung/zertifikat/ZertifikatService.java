package de.jakub.seminarverwaltung.zertifikat;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.jakub.seminarverwaltung.benutzer.Benutzer;
import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.nachricht.Anlass;
import de.jakub.seminarverwaltung.nachricht.NachrichtenService;
import de.jakub.seminarverwaltung.seminar.Seminar;
import de.jakub.seminarverwaltung.seminar.SeminarRepository;

@Service
public class ZertifikatService {

    private final ZertifikatRepository zertifikatRepository;
    private final SeminarRepository seminarRepository;
    private final BenutzerRepository benutzerRepository;
    private final NachrichtenService nachrichtenService;

    public ZertifikatService(ZertifikatRepository zertifikatRepository, SeminarRepository seminarRepository,
            BenutzerRepository benutzerRepository, NachrichtenService nachrichtenService) {
        this.zertifikatRepository = zertifikatRepository;
        this.seminarRepository = seminarRepository;
        this.benutzerRepository = benutzerRepository;
        this.nachrichtenService = nachrichtenService;
    }

    /** Lädt Seminar und Teilnehmer neu, denn aus dem JSON kommen oft nur Verweise mit ID, die E-Mail braucht die Namen. */
    @Transactional
    public Zertifikat ausstellen(Zertifikat zertifikat) {
        Seminar seminar = Optional.ofNullable(zertifikat.getSeminar().getId())
                .flatMap(seminarRepository::findById)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seminar nicht gefunden"));
        Benutzer teilnehmer = Optional.ofNullable(zertifikat.getTeilnehmer().getId())
                .flatMap(benutzerRepository::findById)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teilnehmer nicht gefunden"));
        zertifikat.setSeminar(seminar);
        zertifikat.setTeilnehmer(teilnehmer);
        Zertifikat gespeichert = zertifikatRepository.save(zertifikat);
        nachrichtenService.sende(Anlass.ZERTIFIKAT, teilnehmer, seminar);
        return gespeichert;
    }
}
