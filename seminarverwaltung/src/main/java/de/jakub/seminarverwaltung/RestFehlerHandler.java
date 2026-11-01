package de.jakub.seminarverwaltung;

import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

/** Übersetzt Datenbank- und Validierungsfehler in verständliche REST-Antworten statt HTTP 500. */
@RestControllerAdvice(annotations = RestController.class)
public class RestFehlerHandler {

    /** z. B. Benutzer löschen, der noch Kursleiter ist, oder doppelte E-Mail */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail datenkonflikt(DataIntegrityViolationException fehler) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Datensatz wird noch verwendet oder ist bereits vorhanden (z. B. gleiche E-Mail)");
    }

    /** Bean Validation beim Speichern über JPA, z. B. beim JSON-Import */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail ungueltig(ConstraintViolationException fehler) {
        String meldungen = fehler.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, meldungen);
    }
}
