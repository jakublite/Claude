package de.jakub.seminarverwaltung.benutzer;

import java.util.Optional;

import org.springframework.http.HttpStatus;
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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/benutzer")
public class BenutzerController {

    private final BenutzerRepository benutzerRepository;

    public BenutzerController(BenutzerRepository benutzerRepository) {
        this.benutzerRepository = benutzerRepository;
    }

    @GetMapping
    public ResponseEntity<Object> suche(@RequestParam(required = false) String nachname) {
        if (nachname == null) {
            return ResponseEntity.ok(benutzerRepository.findAll());
        }
        return ResponseEntity.ok(benutzerRepository.findByNachnameContainingIgnoreCase(nachname));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> lade(@PathVariable Long id) {
        return benutzerRepository.findById(id)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Object> anlegen(@Valid @RequestBody Benutzer benutzer) {
        benutzer.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(benutzerRepository.save(benutzer));
    }

    /** Das Passwort bleibt erhalten, es kommt nicht über JSON. */
    @PutMapping("/{id}")
    public ResponseEntity<Object> aendern(@PathVariable Long id, @Valid @RequestBody Benutzer benutzer) {
        Optional<Benutzer> bisher = benutzerRepository.findById(id);
        if (bisher.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        benutzer.setId(id);
        benutzer.setPasswortHash(bisher.get().getPasswortHash());
        return ResponseEntity.ok(benutzerRepository.save(benutzer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> loeschen(@PathVariable Long id) {
        if (!benutzerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        benutzerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
