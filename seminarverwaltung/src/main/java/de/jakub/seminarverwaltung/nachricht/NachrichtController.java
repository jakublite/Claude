package de.jakub.seminarverwaltung.nachricht;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nachrichten")
public class NachrichtController {

    private final NachrichtRepository nachrichtRepository;

    public NachrichtController(NachrichtRepository nachrichtRepository) {
        this.nachrichtRepository = nachrichtRepository;
    }

    @GetMapping
    public ResponseEntity<Object> suche(@RequestParam(required = false) String empfaenger) {
        if (empfaenger == null) {
            return ResponseEntity.ok(nachrichtRepository.findAllByOrderByIdDesc());
        }
        return ResponseEntity.ok(nachrichtRepository.findByEmpfaengerContainingIgnoreCaseOrderByIdDesc(empfaenger));
    }
}
