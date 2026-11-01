package de.jakub.seminarverwaltung.nachricht;

/** Wofür eine E-Mail verschickt wird, mit dem Standardtext der Vorlage. */
public enum Anlass {

    ANMELDUNG("Anmeldung", "Anmeldung bestätigt: {seminar}", """
            Hallo {vorname} {nachname},

            deine Anmeldung für „{seminar}“ ist bestätigt.
            Zeitraum: {zeitraum}
            Ort: {ort}

            Viele Grüße
            Deine Seminarverwaltung"""),
    ABMELDUNG("Abmeldung", "Abmeldung bestätigt: {seminar}", """
            Hallo {vorname} {nachname},

            du bist von „{seminar}“ abgemeldet.

            Viele Grüße
            Deine Seminarverwaltung"""),
    WARTELISTE("Warteliste", "Warteliste: {seminar}", """
            Hallo {vorname} {nachname},

            „{seminar}“ ist ausgebucht. Du stehst jetzt auf der Warteliste und rückst automatisch nach, sobald ein Platz frei wird.

            Viele Grüße
            Deine Seminarverwaltung"""),
    NACHRUECKEN("Nachgerückt", "Platz frei: {seminar}", """
            Hallo {vorname} {nachname},

            gute Nachricht: Für „{seminar}“ ist ein Platz frei geworden und du bist von der Warteliste nachgerückt.
            Zeitraum: {zeitraum}
            Ort: {ort}

            Viele Grüße
            Deine Seminarverwaltung"""),
    ZERTIFIKAT("Zertifikat", "Dein Zertifikat: {seminar}", """
            Hallo {vorname} {nachname},

            herzlichen Glückwunsch! Dein Zertifikat für „{seminar}“ wurde ausgestellt.

            Viele Grüße
            Deine Seminarverwaltung""");

    private final String bezeichnung;
    private final String standardBetreff;
    private final String standardInhalt;

    Anlass(String bezeichnung, String standardBetreff, String standardInhalt) {
        this.bezeichnung = bezeichnung;
        this.standardBetreff = standardBetreff;
        this.standardInhalt = standardInhalt;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public String getStandardBetreff() {
        return standardBetreff;
    }

    public String getStandardInhalt() {
        return standardInhalt;
    }
}
