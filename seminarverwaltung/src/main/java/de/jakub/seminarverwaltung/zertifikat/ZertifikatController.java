package de.jakub.seminarverwaltung.zertifikat;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/zertifikate")
public class ZertifikatController {

    private final ZertifikatRepository zertifikatRepository;
    private final ZertifikatService zertifikatService;

    public ZertifikatController(ZertifikatRepository zertifikatRepository, ZertifikatService zertifikatService) {
        this.zertifikatRepository = zertifikatRepository;
        this.zertifikatService = zertifikatService;
    }

    @GetMapping
    public ResponseEntity<Object> suche(@RequestParam(required = false) Long seminarId) {
        if (seminarId == null) {
            return ResponseEntity.ok(zertifikatRepository.findAll());
        }
        return ResponseEntity.ok(zertifikatRepository.findBySeminarId(seminarId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> lade(@PathVariable Long id) {
        return zertifikatRepository.findById(id)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Object> anlegen(@Valid @RequestBody Zertifikat zertifikat) {
        zertifikat.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(zertifikatService.ausstellen(zertifikat));
    }

    @GetMapping(value = "/{id}/dokument", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Object> dokument(@PathVariable Long id) {
        return zertifikatRepository.findById(id)
                .<ResponseEntity<Object>>map(z -> ResponseEntity.ok(ZertifikatDokument.erzeuge(z)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> aendern(@PathVariable Long id, @Valid @RequestBody Zertifikat zertifikat) {
        if (!zertifikatRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        zertifikat.setId(id);
        return ResponseEntity.ok(zertifikatRepository.save(zertifikat));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> loeschen(@PathVariable Long id) {
        if (!zertifikatRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        zertifikatRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
