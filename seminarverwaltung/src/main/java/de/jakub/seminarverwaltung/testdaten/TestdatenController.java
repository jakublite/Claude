package de.jakub.seminarverwaltung.testdaten;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Simuliert eine externe Schnittstelle, die Testdaten als JSON liefert. */
@RestController
@RequestMapping("/api/import")
public class TestdatenController {

    private final TestdatenImportService importService;

    public TestdatenController(TestdatenImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/json")
    public ResponseEntity<Object> importiereJson(@RequestBody ImportDaten daten) {
        return ResponseEntity.ok(importService.importiere(daten));
    }
}
