package de.jakub.seminarverwaltung.ui;

import java.math.BigDecimal;
import java.util.Comparator;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.jakub.seminarverwaltung.benutzer.Benutzer;
import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.seminar.Seminar;
import de.jakub.seminarverwaltung.seminar.SeminarRepository;
import de.jakub.seminarverwaltung.unterlage.Art;
import de.jakub.seminarverwaltung.unterlage.Bezugsart;
import de.jakub.seminarverwaltung.unterlage.Schulungsunterlage;
import de.jakub.seminarverwaltung.unterlage.SchulungsunterlageRepository;

@Route("unterlagen")
@PageTitle("Schulungsunterlagen")
public class UnterlagenView extends VerticalLayout {

    private final SchulungsunterlageRepository unterlageRepository;
    private final SeminarRepository seminarRepository;
    private final BenutzerRepository benutzerRepository;
    private final Grid<Schulungsunterlage> grid = new Grid<>();
    private final TextField suche;
    private final Checkbox nurUeberfaellige = new Checkbox("Nur überfällige");

    public UnterlagenView(SchulungsunterlageRepository unterlageRepository, SeminarRepository seminarRepository,
            BenutzerRepository benutzerRepository) {
        this.unterlageRepository = unterlageRepository;
        this.seminarRepository = seminarRepository;
        this.benutzerRepository = benutzerRepository;
        addClassName("seite");
        setSizeFull();

        suche = UiHilfe.suchfeld("Titel suchen …", t -> aktualisiere());
        nurUeberfaellige.addValueChangeListener(e -> aktualisiere());
        HorizontalLayout toolbar = new HorizontalLayout(suche, nurUeberfaellige,
                UiHilfe.primaer("Neue Unterlage", VaadinIcon.PLUS, () -> bearbeite(neueUnterlage())));
        toolbar.addClassName("toolbar");
        toolbar.setAlignItems(Alignment.CENTER);

        grid.addColumn(Schulungsunterlage::getTitel).setHeader("Titel")
                .setComparator(u -> u.getTitel().toLowerCase()).setWidth("200px").setFlexGrow(2);
        grid.addColumn(u -> artText(u.getArt())).setHeader("Art").setComparator(Schulungsunterlage::getArt).setAutoWidth(true);
        grid.addColumn(u -> u.getBezugsart() == Bezugsart.LEIHEN ? "Leihen" : "Kaufen").setHeader("Bezug")
                .setComparator(Schulungsunterlage::getBezugsart).setAutoWidth(true);
        grid.addComponentColumn(UnterlagenView::verfuegbarBis).setHeader("Verfügbar bis")
                .setComparator(Comparator.comparing(Schulungsunterlage::getVerfuegbarBis, Comparator.nullsLast(Comparator.naturalOrder())))
                .setAutoWidth(true);
        grid.addColumn(u -> UiHilfe.betrag(u.getGebuehr())).setHeader("Gebühr")
                .setComparator(Schulungsunterlage::getGebuehr).setAutoWidth(true);
        grid.addComponentColumn(UnterlagenView::statusBadge).setHeader("Status")
                .setComparator(u -> u.isUeberfaellig() ? 0 : u.isBezahlt() ? 2 : 1).setAutoWidth(true);
        grid.addColumn(u -> u.getBenutzer() == null ? "" : u.getBenutzer().getVorname() + " " + u.getBenutzer().getNachname())
                .setHeader("Benutzer").setAutoWidth(true);
        grid.addComponentColumn(u -> UiHilfe.zeilenAktionen(() -> bearbeite(u), () -> loesche(u)))
                .setWidth("110px").setFlexGrow(0);
        grid.setSizeFull();

        add(toolbar, grid);
        aktualisiere();
    }

    /** Überfällig hat Vorrang vor dem Zahlungsstatus. */
    static Badge statusBadge(Schulungsunterlage unterlage) {
        Badge badge;
        if (unterlage.isUeberfaellig()) {
            badge = new Badge("Überfällig", VaadinIcon.WARNING.create());
            badge.addThemeVariants(BadgeVariant.ERROR);
        } else if (unterlage.isBezahlt()) {
            badge = new Badge("Bezahlt", VaadinIcon.CHECK.create());
            badge.addThemeVariants(BadgeVariant.SUCCESS);
        } else {
            badge = new Badge("Offen");
            badge.addThemeVariants(BadgeVariant.WARNING);
        }
        return badge;
    }

    private static Component verfuegbarBis(Schulungsunterlage unterlage) {
        Span datum = new Span(UiHilfe.datum(unterlage.getVerfuegbarBis()));
        if (unterlage.isUeberfaellig()) {
            datum.addClassName("ueberfaellig");
        }
        return datum;
    }

    private static String artText(Art art) {
        return switch (art) {
            case BUCH -> "Buch";
            case PDF -> "PDF";
            case EBOOK -> "E-Book";
            case VIDEO -> "Video";
        };
    }

    private void aktualisiere() {
        String titel = suche.getValue();
        grid.setItems((titel.isBlank() ? unterlageRepository.findAll()
                : unterlageRepository.findByTitelContainingIgnoreCase(titel)).stream()
                .filter(u -> !nurUeberfaellige.getValue() || u.isUeberfaellig())
                .toList());
    }

    private Schulungsunterlage neueUnterlage() {
        Schulungsunterlage unterlage = new Schulungsunterlage();
        unterlage.setBezugsart(Bezugsart.KAUFEN);
        unterlage.setGebuehr(BigDecimal.ZERO);
        return unterlage;
    }

    private void bearbeite(Schulungsunterlage unterlage) {
        TextField titel = new TextField("Titel");
        Select<Art> art = new Select<>();
        art.setLabel("Art");
        art.setItems(Art.values());
        art.setItemLabelGenerator(UnterlagenView::artText);
        RadioButtonGroup<Bezugsart> bezugsart = new RadioButtonGroup<>("Bezug");
        bezugsart.setItems(Bezugsart.values());
        bezugsart.setItemLabelGenerator(b -> b == Bezugsart.LEIHEN ? "Leihen" : "Kaufen");
        DatePicker verfuegbarBis = UiHilfe.datumsfeld("Verfügbar bis");
        BigDecimalField gebuehr = new BigDecimalField("Gebühr (€)");
        Checkbox bezahlt = new Checkbox("Bezahlt");
        ComboBox<Seminar> seminar = new ComboBox<>("Seminar");
        seminar.setItems(seminarRepository.findAll());
        seminar.setItemLabelGenerator(Seminar::getTitel);
        seminar.setClearButtonVisible(true);
        ComboBox<Benutzer> benutzer = new ComboBox<>("Ausgeliehen / gekauft von");
        benutzer.setItems(benutzerRepository.findAll());
        benutzer.setItemLabelGenerator(b -> b.getVorname() + " " + b.getNachname());
        benutzer.setClearButtonVisible(true);

        // "Verfügbar bis" nur beim Leihen anzeigen und dann Pflicht
        bezugsart.addValueChangeListener(e -> {
            boolean leihen = e.getValue() == Bezugsart.LEIHEN;
            verfuegbarBis.setVisible(leihen);
            verfuegbarBis.setRequiredIndicatorVisible(leihen);
            if (!leihen) {
                verfuegbarBis.clear();
            }
        });

        BeanValidationBinder<Schulungsunterlage> binder = new BeanValidationBinder<>(Schulungsunterlage.class);
        binder.forField(titel).bind("titel");
        binder.forField(art).bind("art");
        binder.forField(bezugsart).bind("bezugsart");
        binder.forField(verfuegbarBis)
                .withValidator(d -> bezugsart.getValue() != Bezugsart.LEIHEN || d != null, "Beim Leihen Pflicht")
                .bind("verfuegbarBis");
        binder.forField(gebuehr).bind("gebuehr");
        binder.forField(bezahlt).bind("bezahlt");
        binder.forField(seminar).bind("seminar");
        binder.forField(benutzer).bind("benutzer");

        FormLayout formular = new FormLayout(titel, art, bezugsart, verfuegbarBis, gebuehr, bezahlt, seminar, benutzer);
        formular.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("480px", 2));
        formular.setColspan(titel, 2);

        String dialogTitel = unterlage.getId() == null ? "Neue Unterlage" : "Unterlage bearbeiten";
        new FormDialog<>(dialogTitel, binder, unterlage, formular, u -> {
            unterlageRepository.save(u);
            aktualisiere();
        }).open();
    }

    private void loesche(Schulungsunterlage unterlage) {
        UiHilfe.loeschenBestaetigen(unterlage.getTitel(), () -> {
            unterlageRepository.deleteById(unterlage.getId());
            aktualisiere();
        });
    }
}
