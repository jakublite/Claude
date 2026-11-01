package de.jakub.seminarverwaltung.testdaten;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Lädt beim Start die CSV-Dateien aus testdaten.ordner in die H2-Datenbank.
 * Abschaltbar über testdaten.csv-import=false.
 */
@Component
@ConditionalOnProperty(name = "testdaten.csv-import", havingValue = "true")
public class CsvImportBeimStart implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CsvImportBeimStart.class);

    private final TestdatenImportService importService;
    private final String ordner;

    public CsvImportBeimStart(TestdatenImportService importService, @Value("${testdaten.ordner}") String ordner) {
        this.importService = importService;
        this.ordner = ordner;
    }

    @Override
    public void run(ApplicationArguments args) {
        LOG.info("CSV-Daten aus '{}' importiert: {}", ordner, importService.importiere(CsvTestdaten.lese(ordner)));
    }
}
