package de.jakub.seminarverwaltung.unterlage;

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
@RequestMapping("/api/unterlagen")
public class SchulungsunterlageController {

    private final SchulungsunterlageRepository unterlageRepository;

    public SchulungsunterlageController(SchulungsunterlageRepository unterlageRepository) {
        this.unterlageRepository = unterlageRepository;
    }

    @GetMapping
    public ResponseEntity<Object> suche(@RequestParam(required = false) String titel) {
        if (titel == null) {
            return ResponseEntity.ok(unterlageRepository.findAll());
        }
        return ResponseEntity.ok(unterlageRepository.findByTitelContainingIgnoreCase(titel));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> lade(@PathVariable Long id) {
        return unterlageRepository.findById(id)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Object> anlegen(@Valid @RequestBody Schulungsunterlage unterlage) {
        unterlage.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(unterlageRepository.save(unterlage));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> aendern(@PathVariable Long id, @Valid @RequestBody Schulungsunterlage unterlage) {
        if (!unterlageRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        unterlage.setId(id);
        return ResponseEntity.ok(unterlageRepository.save(unterlage));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> loeschen(@PathVariable Long id) {
        if (!unterlageRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        unterlageRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
