package de.jakub.seminarverwaltung.seminar;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.jakub.seminarverwaltung.wetter.WetterService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/seminare")
public class SeminarController {

    private final SeminarRepository seminarRepository;
    private final SeminarService seminarService;
    private final WetterService wetterService;

    public SeminarController(SeminarRepository seminarRepository, SeminarService seminarService,
            WetterService wetterService) {
        this.seminarRepository = seminarRepository;
        this.seminarService = seminarService;
        this.wetterService = wetterService;
    }

    @GetMapping
    public ResponseEntity<Object> suche(@RequestParam(required = false) String titel) {
        if (titel == null) {
            return ResponseEntity.ok(seminarRepository.findAll());
        }
        return ResponseEntity.ok(seminarRepository.findByTitelContainingIgnoreCase(titel));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> lade(@PathVariable Long id) {
        return seminarRepository.findById(id)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Object> anlegen(@Valid @RequestBody Seminar seminar) {
        seminar.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(seminarRepository.save(seminar));
    }

    /** Teilnehmer und Warteliste bleiben erhalten, sie ändern sich nur über /teilnehmer und /warteliste. */
    @PutMapping("/{id}")
    public ResponseEntity<Object> aendern(@PathVariable Long id, @Valid @RequestBody Seminar seminar) {
        Optional<Seminar> bisher = seminarRepository.findById(id);
        if (bisher.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (bisher.get().getClass() != seminar.getClass()) {
            return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                    "Der Seminartyp kann nicht geändert werden"));
        }
        seminar.setId(id);
        seminar.setTeilnehmer(bisher.get().getTeilnehmer());
        seminar.setWarteliste(bisher.get().getWarteliste());
        return ResponseEntity.ok(seminarService.speichern(seminar));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> loeschen(@PathVariable Long id) {
        if (!seminarRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        seminarRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/teilnehmer/{benutzerId}")
    public ResponseEntity<Object> anmelden(@PathVariable Long id, @PathVariable Long benutzerId) {
        return ResponseEntity.ok(seminarService.anmelden(id, benutzerId));
    }

    @DeleteMapping("/{id}/teilnehmer/{benutzerId}")
    public ResponseEntity<Object> abmelden(@PathVariable Long id, @PathVariable Long benutzerId) {
        return ResponseEntity.ok(seminarService.abmelden(id, benutzerId));
    }

    @PostMapping("/{id}/warteliste/{benutzerId}")
    public ResponseEntity<Object> aufWarteliste(@PathVariable Long id, @PathVariable Long benutzerId) {
        return ResponseEntity.ok(seminarService.aufWarteliste(id, benutzerId));
    }

    @DeleteMapping("/{id}/warteliste/{benutzerId}")
    public ResponseEntity<Object> vonWarteliste(@PathVariable Long id, @PathVariable Long benutzerId) {
        return ResponseEntity.ok(seminarService.vonWarteliste(id, benutzerId));
    }

    @GetMapping("/{id}/teilnehmerliste.csv")
    public ResponseEntity<Object> teilnehmerliste(@PathVariable Long id) {
        return seminarRepository.findById(id)
                .<ResponseEntity<Object>>map(s -> ResponseEntity.ok()
                        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"teilnehmer-seminar-" + id + ".csv\"")
                        .body(TeilnehmerlisteCsv.erzeuge(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/wetter")
    public ResponseEntity<Object> wetter(@PathVariable Long id) {
        return ladePraesenzSeminar(id)
                .<ResponseEntity<Object>>map(s -> ResponseEntity.ok(wetterService.ermittleWetter(s.getOrt())))
                .orElse(ResponseEntity.notFound().build());
    }

    /** HTML-Schnipsel zum Einbinden der Wetteranzeige, z. B. in eine Detailansicht. */
    @GetMapping(value = "/{id}/wetter/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Object> wetterHtml(@PathVariable Long id) {
        return ladePraesenzSeminar(id)
                .<ResponseEntity<Object>>map(s -> ResponseEntity.ok(wetterService.ermittleWetter(s.getOrt()).alsHtml()))
                .orElse(ResponseEntity.notFound().build());
    }

    private Optional<PraesenzSeminar> ladePraesenzSeminar(Long id) {
        return seminarRepository.findById(id)
                .filter(PraesenzSeminar.class::isInstance)
                .map(PraesenzSeminar.class::cast);
    }
}
