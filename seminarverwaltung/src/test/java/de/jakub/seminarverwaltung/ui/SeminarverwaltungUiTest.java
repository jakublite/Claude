package de.jakub.seminarverwaltung.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.nachricht.Anlass;
import de.jakub.seminarverwaltung.nachricht.NachrichtRepository;
import de.jakub.seminarverwaltung.nachricht.VorlageRepository;
import de.jakub.seminarverwaltung.seminar.Seminar;
import de.jakub.seminarverwaltung.seminar.SeminarRepository;
import de.jakub.seminarverwaltung.wetter.Wetter;
import de.jakub.seminarverwaltung.wetter.WetterService;

/**
 * Testet die Vaadin-Views ohne Browser direkt auf Komponentenebene, Datenänderungen werden zurückgerollt.
 * Standard ist die Sicht eines Kursleiters, Tests zur Teilnehmer-Sicht überschreiben das.
 */
@SpringBootTest
@Transactional
@WithMockUser(username = "anna.berger@example.com", roles = "KURSLEITER")
class SeminarverwaltungUiTest extends SpringBrowserlessTest {

    @Autowired
    private SeminarRepository seminarRepository;

    @Autowired
    private BenutzerRepository benutzerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NachrichtRepository nachrichtRepository;

    @Autowired
    private VorlageRepository vorlageRepository;

    private static final String LENA = "lena.hofmann@example.com";

    @MockitoBean
    private WetterService wetterService;

    @Test
    void uebersichtZeigtKennzahlen() {
        navigate(DashboardView.class);

        assertThat($(Span.class).withText("5 / 32").exists()).isTrue();
        assertThat($(Span.class).withText("Java ist auch eine Insel").exists()).isTrue();
    }

    @Test
    void seminareSindNachStartdatumSortiert() {
        navigate(SeminareView.class);

        var grid = test(seminarGrid());
        assertThat(grid.size()).isEqualTo(4);
        assertThat(grid.getRow(0).getTitel()).isEqualTo("Java Grundlagen");
        assertThat(grid.getRow(3).getTitel()).isEqualTo("Clean Code Workshop");
    }

    @Test
    void detailZeigtWetterUndMeldetTeilnehmerAn() {
        when(wetterService.ermittleWetter("Augsburg")).thenReturn(new Wetter("Augsburg", 9.2, 10, 12.3));
        navigate(SeminareView.class);

        test(seminarGrid()).clickRow(zeileVon("SQL für Einsteiger"));
        assertThat($(Span.class).withText("9,2 °C").exists()).isTrue();

        test($(ComboBox.class).withPlaceholder("Benutzer anmelden …").single()).selectItem("Murat Yilmaz");
        test($(Button.class).withText("Anmelden").single()).click();

        assertThat($(Span.class).withText("Murat Yilmaz").exists()).isTrue();
        assertThat(seminarRepository.findByTitel("SQL für Einsteiger").orElseThrow().getAnzahlTeilnehmer()).isEqualTo(2);
    }

    @Test
    void beiAusgebuchtemSeminarIstAnmeldenGesperrt() {
        navigate(SeminareView.class);

        test(seminarGrid()).clickRow(zeileVon("Spring Boot kompakt"));

        assertThat(test($(Button.class).withText("Anmelden").single()).isUsable()).isFalse();
    }

    @Test
    void neuesOnlineSeminarAnlegen() {
        navigate(SeminareView.class);
        test($(Button.class).withText("Online-Seminar").single()).click();

        test($(TextField.class).withLabel("Titel").single()).setValue("Vaadin Einstieg");
        test($(ComboBox.class).withLabel("Kursleiter").single()).selectItem("Anna Berger");
        test($(DatePicker.class).withLabel("Start").single()).setValue(LocalDate.of(2027, 4, 1));
        test($(DatePicker.class).withLabel("Ende").single()).setValue(LocalDate.of(2027, 4, 2));
        test($(TextField.class).withLabel("Meeting-Link").single()).setValue("https://meet.example.com/vaadin");
        test($(Button.class).withText("Speichern").single()).click();

        assertThat(test(seminarGrid()).size()).isEqualTo(5);
        assertThat(seminarRepository.findByTitel("Vaadin Einstieg")).isPresent();
    }

    @Test
    void pflichtfelderVerhindernSpeichern() {
        navigate(SeminareView.class);
        test($(Button.class).withText("Online-Seminar").single()).click();

        test($(Button.class).withText("Speichern").single()).click();

        assertThat($(Dialog.class).single().isOpened()).isTrue();
        assertThat(seminarRepository.count()).isEqualTo(4);
    }

    @Test
    void filterNurUeberfaelligeUnterlagen() {
        navigate(UnterlagenView.class);
        var grid = test($(Grid.class).single());
        assertThat(grid.size()).isEqualTo(4);

        test($(Checkbox.class).withLabel("Nur überfällige").single()).check();

        assertThat(grid.size()).isEqualTo(1);
    }

    @Test
    void ausgebuchtKommtPerDialogAufWartelisteUndRueckenNach() {
        navigate(SeminareView.class);
        test(seminarGrid()).clickRow(zeileVon("Spring Boot kompakt"));

        test($(ComboBox.class).withPlaceholder("Benutzer anmelden …").single()).selectItem("Murat Yilmaz");
        test($(Button.class).withText("Auf Warteliste").single()).click();

        Seminar seminar = seminarRepository.findByTitel("Spring Boot kompakt").orElseThrow();
        assertThat(seminar.getWarteliste()).extracting("nachname").containsExactly("Yilmaz");
        assertThat($(Span.class).withText("Warteliste").exists()).isTrue();

        test(ersterButtonMitTooltip("Abmelden")).click();

        seminar =seminarRepository.findByTitel("Spring Boot kompakt").orElseThrow();
        assertThat(seminar.getTeilnehmer()).extracting("nachname").contains("Yilmaz");
        assertThat(seminar.getWarteliste()).isEmpty();
    }

    @Test
    void wartelisteButtonNurBeiAusgebuchtemSeminar() {
        navigate(SeminareView.class);

        test(seminarGrid()).clickRow(zeileVon("Clean Code Workshop"));

        assertThat($(Button.class).withText("Anmelden").exists()).isTrue();
        assertThat($(Button.class).withText("Auf Warteliste").exists()).isFalse();
    }

    @Test
    void anmeldungErscheintImPostausgang() {
        navigate(SeminareView.class);
        test(seminarGrid()).clickRow(zeileVon("Clean Code Workshop"));
        test($(ComboBox.class).withPlaceholder("Benutzer anmelden …").single()).selectItem("Murat Yilmaz");
        test($(Button.class).withText("Anmelden").single()).click();

        navigate(PostausgangView.class);

        var grid = test($(Grid.class).single());
        assertThat(grid.size()).isEqualTo(1);
        grid.clickRow(0);
        assertThat($(Pre.class).single().getText()).contains("An: murat.yilmaz@example.com", "Hallo Murat Yilmaz", "Clean Code Workshop");
    }

    @Test
    void vorlageBearbeitenUndSpeichern() {
        navigate(VorlagenView.class);
        var grid = test($(Grid.class).single());
        assertThat(grid.size()).isEqualTo(Anlass.values().length);

        test((Button) grid.getCellComponent(0, 2)).click();
        test($(TextField.class).withLabel("Betreff").single()).setValue("Neuer Betreff {seminar}");
        test($(Button.class).withText("Speichern").single()).click();

        assertThat(vorlageRepository.findById(Anlass.ANMELDUNG).orElseThrow().getBetreff()).isEqualTo("Neuer Betreff {seminar}");
    }

    @Test
    void neuesZertifikatImFormularLoestEmailAus() {
        navigate(ZertifikateView.class);
        test($(Button.class).withText("Neues Zertifikat").single()).click();

        test($(ComboBox.class).withLabel("Seminar").single()).selectItem("SQL für Einsteiger");
        test($(ComboBox.class).withLabel("Teilnehmer").single()).selectItem("Lena Hofmann");
        test($(RadioButtonGroup.class).withLabel("Niveau").single()).selectItem("A");
        test($(Button.class).withText("Speichern").single()).click();

        assertThat(nachrichtRepository.findAllByOrderByIdDesc().getFirst().getAnlass()).isEqualTo(Anlass.ZERTIFIKAT);
    }

    @Test
    void passwortImFormularSetzenSpeichertNurDenHash() {
        navigate(BenutzerView.class);
        test($(Button.class).withText("Neuer Benutzer").single()).click();

        test($(TextField.class).withLabel("Vorname").single()).setValue("Mia");
        test($(TextField.class).withLabel("Nachname").single()).setValue("Neu");
        test($(EmailField.class).withLabel("E-Mail").single()).setValue("mia.neu@example.com");
        test($(Select.class).withLabel("Rolle").single()).selectItem("Teilnehmer");
        test($(PasswordField.class).withLabel("Neues Passwort").single()).setValue("kurz");
        test($(Button.class).withText("Speichern").single()).click();
        assertThat(benutzerRepository.findByEmail("mia.neu@example.com")).isEmpty();

        test($(PasswordField.class).withLabel("Neues Passwort").single()).setValue("geheim-und-lang");
        test($(Button.class).withText("Speichern").single()).click();

        String hash = benutzerRepository.findByEmail("mia.neu@example.com").orElseThrow().getPasswortHash();
        assertThat(hash).isNotEqualTo("geheim-und-lang");
        assertThat(passwordEncoder.matches("geheim-und-lang", hash)).isTrue();
    }

    @Test
    void kursleiterSiehtAlleMenuepunkte() {
        navigate(DashboardView.class);

        assertThat($(SideNavItem.class).all()).extracting(SideNavItem::getLabel)
                .contains("Benutzer", "Postausgang", "Vorlagen");
    }

    @Test
    @WithMockUser(username = LENA, roles = "TEILNEHMER")
    void teilnehmerSiehtKeineVerwaltung() {
        navigate(SeminareView.class);

        assertThat($(SideNavItem.class).all()).extracting(SideNavItem::getLabel)
                .containsExactly("Übersicht", "Seminare", "Zertifikate", "Unterlagen");
        assertThat($(Button.class).withText("Online-Seminar").exists()).isFalse();
        assertThat($(Button.class).all()).noneMatch(b -> b.getTooltip() != null && "Löschen".equals(b.getTooltip().getText()));
    }

    @ParameterizedTest
    @ValueSource(classes = { BenutzerView.class, PostausgangView.class, VorlagenView.class })
    @WithMockUser(username = LENA, roles = "TEILNEHMER")
    void teilnehmerKommtNichtInVerwaltungsViews(Class<? extends Component> view) {
        assertThatThrownBy(() -> navigate(view)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @WithMockUser(username = LENA, roles = "TEILNEHMER")
    void teilnehmerSiehtImSeminarKeineAnderenPersonen() {
        navigate(SeminareView.class);

        test(seminarGrid()).clickRow(zeileVon("Java Grundlagen"));

        assertThat($(Span.class).withText("Du bist angemeldet.").exists()).isTrue();
        assertThat($(Span.class).withText("Murat Yilmaz").exists()).isFalse();
        assertThat($(Anchor.class).all()).extracting(Anchor::getText)
                .contains("Meeting öffnen").doesNotContain("Teilnehmerliste als CSV");
        assertThat($(Button.class).withText("Bearbeiten").exists()).isFalse();
        assertThat($(Button.class).withText("Löschen").exists()).isFalse();
    }

    @Test
    @WithMockUser(username = LENA, roles = "TEILNEHMER")
    void meetingLinkErstNachDerAnmeldung() {
        navigate(SeminareView.class);

        test(seminarGrid()).clickRow(zeileVon("Spring Boot kompakt"));

        assertThat($(Anchor.class).all()).extracting(Anchor::getText).doesNotContain("Meeting öffnen");
        assertThat($(Span.class).withText("Den Link siehst du nach der Anmeldung.").exists()).isTrue();
    }

    @Test
    @WithMockUser(username = LENA, roles = "TEILNEHMER")
    void teilnehmerMeldetNurSichSelbstAnUndAb() {
        navigate(SeminareView.class);
        test(seminarGrid()).clickRow(zeileVon("Clean Code Workshop"));
        assertThat($(ComboBox.class).withPlaceholder("Benutzer anmelden …").exists()).isFalse();

        test($(Button.class).withText("Anmelden").single()).click();
        assertThat(seminarRepository.findByTitel("Clean Code Workshop").orElseThrow().getTeilnehmer())
                .extracting("email").containsExactly(LENA);

        test($(Button.class).withText("Abmelden").single()).click();
        assertThat(seminarRepository.findByTitel("Clean Code Workshop").orElseThrow().getTeilnehmer()).isEmpty();
    }

    @Test
    @WithMockUser(username = LENA, roles = "TEILNEHMER")
    void teilnehmerGehtSelbstAufDieWarteliste() {
        navigate(SeminareView.class);
        test(seminarGrid()).clickRow(zeileVon("Spring Boot kompakt"));

        test($(Button.class).withText("Auf Warteliste").single()).click();
        assertThat($(Span.class).withText("Du stehst auf Platz 1 der Warteliste.").exists()).isTrue();

        test($(Button.class).withText("Von der Warteliste nehmen").single()).click();
        assertThat(seminarRepository.findByTitel("Spring Boot kompakt").orElseThrow().getWarteliste()).isEmpty();
    }

    @Test
    @WithMockUser(username = LENA, roles = "TEILNEHMER")
    void teilnehmerSiehtNurEigeneZertifikateOhneAktionen() {
        navigate(ZertifikateView.class);

        var grid = test($(Grid.class).single());
        assertThat(grid.size()).isEqualTo(2);
        assertThat($(Button.class).withText("Neues Zertifikat").exists()).isFalse();
    }

    @Test
    @WithMockUser(username = LENA, roles = "TEILNEHMER")
    void teilnehmerSiehtEigeneUndFreieUnterlagen() {
        navigate(UnterlagenView.class);

        var grid = test($(Grid.class).single());
        assertThat(grid.size()).isEqualTo(3);
        assertThat($(Button.class).withText("Neue Unterlage").exists()).isFalse();
    }

    @Test
    @WithMockUser(username = "murat.yilmaz@example.com", roles = "TEILNEHMER")
    void uebersichtZeigtTeilnehmernKeineFremdenAusleihen() {
        navigate(DashboardView.class);

        assertThat($(Span.class).withText("5 / 32").exists()).isTrue();
        assertThat($(Span.class).withText("Java ist auch eine Insel").exists()).isFalse();
    }

    private Button ersterButtonMitTooltip(String text) {
        return $(Button.class).all().stream()
                .filter(b -> b.getTooltip() != null && text.equals(b.getTooltip().getText()))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private Grid<Seminar> seminarGrid() {
        return $(Grid.class).single();
    }

    private int zeileVon(String titel) {
        var grid = test(seminarGrid());
        for (int i = 0; i < grid.size(); i++) {
            if (grid.getRow(i).getTitel().equals(titel)) {
                return i;
            }
        }
        throw new AssertionError("Seminar nicht im Grid: " + titel);
    }
}
