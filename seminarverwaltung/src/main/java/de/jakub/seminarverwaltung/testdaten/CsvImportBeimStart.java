package de.jakub.seminarverwaltung.testdaten;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import de.jakub.seminarverwaltung.benutzer.Benutzer;
import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;

/**
 * Lädt beim Start die CSV-Dateien aus testdaten.ordner in die H2-Datenbank und gibt allen Benutzern
 * das Passwort aus testdaten.demo-passwort. Abschaltbar über testdaten.csv-import=false.
 */
@Component
@ConditionalOnProperty(name = "testdaten.csv-import", havingValue = "true")
public class CsvImportBeimStart implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CsvImportBeimStart.class);

    private final TestdatenImportService importService;
    private final BenutzerRepository benutzerRepository;
    private final PasswordEncoder passwordEncoder;
    private final String ordner;
    private final String demoPasswort;

    public CsvImportBeimStart(TestdatenImportService importService, BenutzerRepository benutzerRepository,
            PasswordEncoder passwordEncoder, @Value("${testdaten.ordner}") String ordner,
            @Value("${testdaten.demo-passwort}") String demoPasswort) {
        this.importService = importService;
        this.benutzerRepository = benutzerRepository;
        this.passwordEncoder = passwordEncoder;
        this.ordner = ordner;
        this.demoPasswort = demoPasswort;
    }

    @Override
    public void run(ApplicationArguments args) {
        LOG.info("CSV-Daten aus '{}' importiert: {}", ordner, importService.importiere(CsvTestdaten.lese(ordner)));

        // Nur einmal hashen, BCrypt ist absichtlich langsam
        String hash = passwordEncoder.encode(demoPasswort);
        List<Benutzer> benutzer = benutzerRepository.findAll();
        benutzer.forEach(b -> b.setPasswortHash(hash));
        benutzerRepository.saveAll(benutzer);
    }
}
