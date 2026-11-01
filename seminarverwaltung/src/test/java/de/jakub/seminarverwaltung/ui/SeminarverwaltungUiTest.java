package de.jakub.seminarverwaltung.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;

import de.jakub.seminarverwaltung.nachricht.Anlass;
import de.jakub.seminarverwaltung.nachricht.NachrichtRepository;
import de.jakub.seminarverwaltung.nachricht.VorlageRepository;
import de.jakub.seminarverwaltung.seminar.Seminar;
import de.jakub.seminarverwaltung.seminar.SeminarRepository;
import de.jakub.seminarverwaltung.wetter.Wetter;
import de.jakub.seminarverwaltung.wetter.WetterService;

/** Testet die Vaadin-Views ohne Browser direkt auf Komponentenebene, Datenänderungen werden zurückgerollt. */
@SpringBootTest
@Transactional
class SeminarverwaltungUiTest extends SpringBrowserlessTest {

    @Autowired
    private SeminarRepository seminarRepository;

    @Autowired
    private NachrichtRepository nachrichtRepository;

    @Autowired
    private VorlageRepository vorlageRepository;

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
