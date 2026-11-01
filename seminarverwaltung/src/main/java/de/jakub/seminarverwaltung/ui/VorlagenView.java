package de.jakub.seminarverwaltung.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.jakub.seminarverwaltung.nachricht.NachrichtenService;
import de.jakub.seminarverwaltung.nachricht.Vorlage;
import de.jakub.seminarverwaltung.nachricht.VorlageRepository;
import jakarta.annotation.security.RolesAllowed;

/** E-Mail-Vorlagen bearbeiten. Änderungen gelten für alle künftigen E-Mails. */
@Route("vorlagen")
@PageTitle("E-Mail-Vorlagen")
@RolesAllowed("KURSLEITER")
public class VorlagenView extends VerticalLayout {

    private final NachrichtenService nachrichtenService;
    private final VorlageRepository vorlageRepository;
    private final Grid<Vorlage> grid = new Grid<>();

    public VorlagenView(NachrichtenService nachrichtenService, VorlageRepository vorlageRepository) {
        this.nachrichtenService = nachrichtenService;
        this.vorlageRepository = vorlageRepository;
        addClassName("seite");
        setSizeFull();

        grid.addColumn(v -> v.getAnlass().getBezeichnung()).setHeader("Anlass").setAutoWidth(true);
        grid.addColumn(Vorlage::getBetreff).setHeader("Betreff").setFlexGrow(2);
        grid.addComponentColumn(v -> {
            Button bearbeiten = new Button(VaadinIcon.EDIT.create(), e -> bearbeite(v));
            bearbeiten.addThemeVariants(ButtonVariant.TERTIARY);
            bearbeiten.setTooltipText("Bearbeiten");
            return bearbeiten;
        }).setWidth("70px").setFlexGrow(0);
        grid.setSizeFull();

        add(grid);
        aktualisiere();
    }

    private void aktualisiere() {
        grid.setItems(nachrichtenService.alleVorlagen());
    }

    private void bearbeite(Vorlage vorlage) {
        TextField betreff = new TextField("Betreff");
        TextArea inhalt = new TextArea("Inhalt");
        inhalt.setMinRows(10);
        inhalt.setHelperText("Platzhalter: {vorname} {nachname} {seminar} {zeitraum} {ort}");

        BeanValidationBinder<Vorlage> binder = new BeanValidationBinder<>(Vorlage.class);
        binder.forField(betreff).bind("betreff");
        binder.forField(inhalt).bind("inhalt");

        FormLayout formular = new FormLayout(betreff, inhalt);
        formular.setResponsiveSteps(new ResponsiveStep("0", 1));

        new FormDialog<>("Vorlage: " + vorlage.getAnlass().getBezeichnung(), binder, vorlage, formular, v -> {
            vorlageRepository.save(v);
            aktualisiere();
        }).open();
    }
}
