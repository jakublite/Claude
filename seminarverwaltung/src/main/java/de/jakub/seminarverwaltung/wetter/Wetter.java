package de.jakub.seminarverwaltung.wetter;

import java.util.Locale;

public record Wetter(String ort, double temperaturCelsius, int niederschlagswahrscheinlichkeitProzent,
        double windgeschwindigkeitKmh) {

    /** Feste Locale, damit die Anzeige nicht von der Sprache des Servers abhängt (9,2 statt 9.2). */
    public String temperaturText() {
        return String.format(Locale.GERMANY, "%.1f °C", temperaturCelsius);
    }

    public String windText() {
        return String.format(Locale.GERMANY, "%.1f km/h", windgeschwindigkeitKmh);
    }

    public String alsHtml() {
        return """
                <div class="wetter">
                  <h3>Wetter in %s</h3>
                  <ul>
                    <li>Temperatur: %s</li>
                    <li>Niederschlagswahrscheinlichkeit: %d %%</li>
                    <li>Windgeschwindigkeit: %s</li>
                  </ul>
                </div>
                """.formatted(ort, temperaturText(), niederschlagswahrscheinlichkeitProzent, windText());
    }
}
