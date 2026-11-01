# Seminarverwaltung

Lernprojekt: Seminarverwaltung mit Anmeldung. Spring Boot 4.1, Java 21, JPA mit In-Memory-H2, Lombok, Vaadin 25 (Aura-Theme).

## Starten

Voraussetzung ist ein JDK 21 mit gesetztem `JAVA_HOME`. Node oder eine eigene Maven-Installation sind nicht nötig.

`start.cmd` per Doppelklick starten oder in PowerShell:

```powershell
.\start.cmd
```

Die App läuft, sobald im Fenster `Started SeminarverwaltungApplication` steht. Beenden mit `Strg+C`. Meldet der Start `Port 8080 was already in use`, läuft noch eine Instanz, die zuerst beendet werden muss. Nach Code-Änderungen die App neu starten.

- Oberfläche: http://localhost:8080
- API: http://localhost:8080/api/...
- H2-Console: http://localhost:8080/h2-console (nur als Kursleiter angemeldet, JDBC URL `jdbc:h2:mem:seminar`, User `sa`, kein Passwort)

## Anmelden

Alle Demo-Benutzer haben das Passwort `demo1234` (`testdaten.demo-passwort`). Es steht offen im Repository und ist nur zum Ausprobieren gedacht.

| Rolle | Beispiel-Login | Darf |
|---|---|---|
| Kursleiter | `anna.berger@example.com` | alles, inklusive Benutzer, Postausgang, Vorlagen und REST-API |
| Teilnehmer | `hannah.vogel@example.com` | Seminare ansehen, sich selbst an- und abmelden, eigene Zertifikate und Unterlagen sehen |

Passwörter vergibt ein Kursleiter unter Benutzer → Bearbeiten (8 bis 72 Zeichen). Gespeichert wird nur der BCrypt-Hash. Wer kein Passwort hat, z. B. nach dem JSON-Import oder `POST /api/benutzer`, kann sich nicht anmelden. Eine geänderte Rolle gilt erst nach dem nächsten Login.

## Daten

Die Datenbank liegt nur im Speicher. Jeder Neustart verwirft alle Änderungen und lädt die Daten neu.

| Ordner unter `src/main/resources/` | Zweck | Inhalt |
|---|---|---|
| `demodaten/` | die App (`testdaten.ordner`) | 20 Benutzer, 14 Seminare, 15 Zertifikate, 15 Unterlagen |
| `testdaten/` | die automatischen Tests, die feste Werte prüfen | 6 Benutzer, 4 Seminare, 3 Zertifikate, 4 Unterlagen |

Die Demodaten haben feste Daten, Stand November 2026. Mit der Zeit werden aus kommenden Seminaren also vergangene.

## Lokal testen

### Oberfläche

Als Teilnehmer, z. B. `hannah.vogel@example.com`:

| Was ausprobieren | Wo | Erwartung |
|---|---|---|
| Falsches Passwort | Login | Fehlermeldung, Formular bleibt offen |
| Menü | Seitenleiste | nur Übersicht, Seminare, Zertifikate, Unterlagen |
| Selbst anmelden | Seminare → „Clean Code Workshop“ | „Meine Teilnahme“ mit Anmelden, danach Abmelden. Keine anderen Personen sichtbar |
| Warteliste | Seminare → „Spring Boot kompakt“ | „Auf Warteliste“, danach Anzeige des eigenen Platzes |
| Gesperrte Seite | http://localhost:8080/benutzer direkt aufrufen | „Could not navigate to 'benutzer'“ |
| Zertifikate | Zertifikate | nur die eigenen, ohne Bearbeiten |
| Ausloggen | oben rechts | zurück zum Login |

Als Kursleiter, z. B. `anna.berger@example.com`:

| Was ausprobieren | Wo | Erwartung |
|---|---|---|
| Passwort vergeben | Benutzer → Neuer Benutzer, Passwort mit mindestens 8 Zeichen | neue Person kann sich anmelden |
| Kennzahlen | Übersicht | 14 Seminare, 43 / 187 Plätze, 4 auf Warteliste, 20 Benutzer, 3 überfällige Ausleihen |
| Nachrücken | Seminare → „Vaadin Flow Praxis“, bei Murat Yilmaz auf das rote X | Paul Weber rückt nach, Nina Koch bleibt auf der Warteliste |
| Warteliste | Seminare → „Spring Boot kompakt“, Benutzer wählen, „Auf Warteliste“ | Person steht unten auf der Warteliste |
| Mehr Plätze | Seminare → „Spring Boot kompakt“ bearbeiten, Max. Teilnehmer auf 5 | Lena Hofmann rückt automatisch nach |
| E-Mail | Postausgang, Zeile anklicken | Text der E-Mail mit eingesetzten Namen und Daten |
| Vorlage ändern | Vorlagen → Anmeldung bearbeiten, danach jemanden anmelden | neuer Text erscheint im Postausgang |
| Zertifikat drucken | Zertifikate → Drucker-Symbol | druckbare Seite im neuen Tab, auch als PDF speicherbar |
| Teilnehmerliste | Seminar öffnen → „Teilnehmerliste als CSV“ | Download, öffnet sich in Excel mit korrekten Umlauten |
| Anmelden | Seminare → „Clean Code Workshop“ (0 / 8) | Auslastung steigt auf 1 / 8 |
| Ausgebucht | Seminare → „Spring Boot kompakt“ | Badge „Ausgebucht“, Anmelden gesperrt |
| Wetter | Seminare → „SQL für Einsteiger“ | aktuelles Wetter in Augsburg (braucht Internet) |
| Ort unbekannt | Seminare → „Inhouse-Schulung Architektur“ | „Wetter gerade nicht verfügbar“ |
| Pflichtfelder | Seminare → „Online-Seminar“, leer speichern | Fehlermeldungen, Dialog bleibt offen |
| Benutzt löschen | Benutzer → „Anna Berger“ löschen | Meldung, dass der Datensatz noch verwendet wird |
| Abgelaufen | Zertifikate → „Git Grundlagen“ | „abgelaufen“ in Rot, bei Hannah Vogel und Jan Richter |
| Überfällig | Unterlagen → „Nur überfällige“ | 3 Einträge |

### REST mit Insomnia

`insomnia-seminarverwaltung.json` über **Import** laden. Die 64 Requests liegen nach Themen sortiert in Ordnern, der erwartete Statuscode steht im Namen, z. B. `Fehler: Seminar ausgebucht [409]`. Die Zugangsdaten für HTTP Basic stehen im Environment.

Die Requests bauen auf den Demodaten auf und ändern sie. Einmal von oben nach unten ausgeführt, liefert jeder den genannten Status. Für einen zweiten Durchlauf die App neu starten, denn z. B. `Benutzer anlegen` meldet sonst 409.

Tests: `.\mvnw.cmd test` (REST, Fehlerantworten, Login und Rollen, Wetter mit gemocktem Dienst, Vaadin-Views per Browserless-Test)

Produktions-Jar bauen und starten (kein Node nötig, das Profil lässt `vaadin-dev` weg):

```powershell
.\mvnw.cmd clean package -Pproduction
& "$env:JAVA_HOME\bin\java.exe" -jar target\seminarverwaltung-0.0.1-SNAPSHOT.jar
```

## Entitäten

| Entität | Package | Besonderheiten |
|---|---|---|
| Benutzer | `benutzer` | Rolle KURSLEITER / TEILNEHMER, E-Mail eindeutig |
| Online-Seminar, Präsenz-Seminar | `seminar` | gemeinsame Basis `Seminar` (Single Table), im JSON unterschieden über `typ` = `ONLINE` / `PRAESENZ` |
| Zertifikat | `zertifikat` | gehört zu Seminar und Teilnehmer, Niveau A / B / C |
| Schulungsunterlage | `unterlage` | Art BUCH / PDF / EBOOK / VIDEO, Bezugsart LEIHEN / KAUFEN, `verfuegbarBis` Pflicht beim Leihen, `ueberfaellig` berechnet |

## Oberfläche (Package `ui`)

| View | Route | Inhalt |
|---|---|---|
| Übersicht | `/` | Kennzahlen, nächste Seminare mit Auslastung, überfällige Ausleihen |
| Seminare | `/seminare` | Suche, Anlegen (online/Präsenz), Bearbeiten, Löschen. Klick auf eine Zeile öffnet die 360°-Sicht |
| Benutzer | `/benutzer` | Visitenkarten-Darstellung, Rolle als Badge |
| Zertifikate | `/zertifikate` | Filter nach Seminar, Niveau als Badge, abgelaufene rot |
| Unterlagen | `/unterlagen` | Status-Badge (Überfällig / Bezahlt / Offen), Filter „Nur überfällige“ |
| Postausgang | `/postausgang` | simulierte E-Mails, Suche nach Empfänger, Klick zeigt den Text |
| Vorlagen | `/vorlagen` | E-Mail-Texte je Anlass bearbeiten, mit Platzhaltern |

360°-Sicht eines Seminars: Eckdaten mit Auslastung, Kursleiter als Visitenkarte, Wetter (Präsenz) oder Meeting-Link (online), Teilnehmer an- und abmelden, Warteliste, Teilnehmerliste als CSV, Zertifikate und Unterlagen des Seminars.

## Sicherheit (Package `sicherheit`)

| Bereich | Anmeldung | Zugriff |
|---|---|---|
| Oberfläche | Login-Formular mit Session | Annotationen an den Views: `@PermitAll` oder `@RolesAllowed("KURSLEITER")` |
| `/api/**` | HTTP Basic, ohne Session | nur Kursleiter |
| `/api/zertifikate/{id}/dokument` | HTTP Basic oder Session | Kursleiter und der Besitzer des Zertifikats |
| `/api/seminare/{id}/teilnehmerliste.csv` | HTTP Basic oder Session | nur Kursleiter |
| `/h2-console` | Session | nur Kursleiter, von H2 zusätzlich auf localhost beschränkt |

Die REST-API ignoriert das Session-Cookie, deshalb kann eine fremde Webseite über einen angemeldeten Browser nichts ändern (CSRF). 401 kommt bewusst ohne `WWW-Authenticate`-Header, damit der Browser keinen Basic-Dialog zeigt und sich keine Zugangsdaten merkt. HTTP Basic überträgt das Passwort bei jedem Request, außerhalb von localhost wäre HTTPS Pflicht.

## Abläufe

**Warteliste:** Bei einem ausgebuchten Seminar kommt man auf die Warteliste. Wird ein Platz frei, durch eine Abmeldung oder weil `maxTeilnehmer` erhöht wurde, rückt automatisch der Erste nach. Die Regeln liegen in `SeminarService`.

**E-Mails:** Anmeldung, Abmeldung, Warteliste, Nachrücken und ein neues Zertifikat erzeugen je eine E-Mail. Es gibt keinen Mailserver, sie landen nur im Postausgang (Entität `Nachricht`). Die Texte kommen aus dem Enum `Anlass`, bis eine Vorlage bearbeitet und damit in der Tabelle `Vorlage` gespeichert wird. Platzhalter: `{vorname}`, `{nachname}`, `{seminar}`, `{zeitraum}`, `{ort}`. Der CSV- und JSON-Import erzeugt keine E-Mails.

Hinweise:
- Theme: Aura (`AppShell`), eigene Styles in `src/main/resources/META-INF/resources/styles.css`.
- `vaadin-dev` ist nur für den Entwicklungsmodus da. Im Profil `production` hat es Scope `test`, denn `provided` würde das Spring-Boot-Plugin mit ins Jar packen.
- Alle Tabellen sind sortierbar, die Seminarliste standardmäßig nach Startdatum. In Handy-Breite stapeln sich Kacheln und Suchfelder, breite Tabellen scrollen horizontal.
- `Seminar.teilnehmer` wird eager geladen, weil Vaadin-Requests nicht durch Open-Session-in-View laufen.

## Endpunkte

Alle Endpunkte brauchen einen Login per HTTP Basic, siehe [Sicherheit](#sicherheit-package-sicherheit). Ohne Login kommt 401, ohne Berechtigung 403.

Jede Entität hat CRUD: `GET` (Liste mit optionalem Suchparameter), `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`.

| Pfad | Suchparameter |
|---|---|
| `/api/benutzer` | `nachname` |
| `/api/seminare` | `titel` |
| `/api/zertifikate` | `seminarId` |
| `/api/unterlagen` | `titel` |

Zusätzlich:

| Methode | Pfad | Zweck |
|---|---|---|
| POST | `/api/seminare/{id}/teilnehmer/{benutzerId}` | Anmeldung, 409 wenn ausgebucht, schon angemeldet oder Kursleiter des Seminars |
| DELETE | `/api/seminare/{id}/teilnehmer/{benutzerId}` | Abmeldung, der Erste der Warteliste rückt nach |
| POST | `/api/seminare/{id}/warteliste/{benutzerId}` | auf die Warteliste, 409 wenn noch Plätze frei sind oder die Person schon dabei ist |
| DELETE | `/api/seminare/{id}/warteliste/{benutzerId}` | von der Warteliste nehmen |
| GET | `/api/seminare/{id}/teilnehmerliste.csv` | Teilnehmer und Warteliste als CSV |
| GET | `/api/zertifikate/{id}/dokument` | druckbares Zertifikat als HTML |
| GET | `/api/nachrichten` | Postausgang, optional `?empfaenger=<TEXT>` |
| GET | `/api/seminare/{id}/wetter` | aktuelles Wetter am Kursort (nur Präsenz) als JSON |
| GET | `/api/seminare/{id}/wetter/html` | dasselbe als HTML-Schnipsel |
| POST | `/api/import/json` | JSON-Import, Beispiel: `src/main/resources/testdaten/schnittstelle.json` |

### Beispiel: Online-Seminar anlegen

```json
{
  "typ": "ONLINE",
  "titel": "<TITEL>",
  "startDatum": "2027-01-01",
  "endDatum": "2027-01-02",
  "maxTeilnehmer": 10,
  "kursleiter": { "id": <BENUTZER_ID> },
  "meetingLink": "https://meet.example.com/<RAUM>"
}
```

Verweise: `kursleiter`, `teilnehmer` und `benutzer` brauchen nur die ID. Ein Verweis auf ein Seminar braucht zusätzlich den Typ, z. B. `"seminar": { "id": 5, "typ": "ONLINE" }`, sonst kommt 400. Die Antwort auf `POST` und `PUT` zeigt bei Verweisen teils nur die gesendeten Felder, `GET` liefert danach den vollständigen Datensatz. Nur `POST /api/zertifikate` lädt die Verweise und antwortet vollständig.

Fehler: Datenbankkonflikte (Datensatz wird noch verwendet, doppelte E-Mail) ergeben 409, ungültige Daten beim Speichern 400, jeweils als `ProblemDetail` (`RestFehlerHandler`).

## Wetter

Läuft über [Open-Meteo](https://open-meteo.com) (kostenlos, ohne API-Key): zuerst Ortsname → Koordinaten (Geocoding-API), dann Koordinaten → aktuelle Temperatur, Niederschlagswahrscheinlichkeit und Windgeschwindigkeit. Es kommt immer das aktuelle Wetter, nicht das Wetter am Seminartag. Timeouts: 3 s Verbindung, 5 s Lesen.

## Ideen zum Ausbauen

Aus der Aufgabenliste noch offen: echter E-Mail-Versand, Newsletter, Selbstregistrierung, eigenes Passwort ändern, Online-Terminvergabe.
