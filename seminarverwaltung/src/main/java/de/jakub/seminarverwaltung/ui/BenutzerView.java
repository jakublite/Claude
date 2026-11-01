package de.jakub.seminarverwaltung.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.jakub.seminarverwaltung.benutzer.Benutzer;
import de.jakub.seminarverwaltung.benutzer.BenutzerRepository;
import de.jakub.seminarverwaltung.benutzer.Rolle;

@Route("benutzer")
@PageTitle("Benutzer")
public class BenutzerView extends VerticalLayout {

    private final BenutzerRepository benutzerRepository;
    private final Grid<Benutzer> grid = new Grid<>();
    private final TextField suche;

    public BenutzerView(BenutzerRepository benutzerRepository) {
        this.benutzerRepository = benutzerRepository;
        addClassName("seite");
        setSizeFull();

        suche = UiHilfe.suchfeld("Nachname suchen …", t -> aktualisiere());
        HorizontalLayout toolbar = new HorizontalLayout(suche,
                UiHilfe.primaer("Neuer Benutzer", VaadinIcon.PLUS, () -> bearbeite(new Benutzer())));
        toolbar.addClassName("toolbar");

        grid.addComponentColumn(BenutzerView::visitenkarte).setHeader("Name")
                .setComparator(b -> b.getNachname().toLowerCase()).setWidth("260px").setFlexGrow(2);
        grid.addComponentColumn(BenutzerView::rollenBadge).setHeader("Rolle")
                .setComparator(Benutzer::getRolle).setAutoWidth(true);
        grid.addComponentColumn(b -> UiHilfe.zeilenAktionen(() -> bearbeite(b), () -> loesche(b)))
                .setWidth("110px").setFlexGrow(0);
        grid.setSizeFull();

        add(toolbar, grid);
        aktualisiere();
    }

    private static Component visitenkarte(Benutzer benutzer) {
        String name = benutzer.getVorname() + " " + benutzer.getNachname();
        Span nameSpan = new Span(name);
        nameSpan.addClassName("zeile-titel");
        Span email = new Span(benutzer.getEmail());
        email.addClassName("gedimmt");
        Div text = new Div(nameSpan, email);
        text.addClassName("visitenkarte-text");
        Div karte = new Div(new Avatar(name), text);
        karte.addClassName("visitenkarte");
        return karte;
    }

    private static Badge rollenBadge(Benutzer benutzer) {
        boolean kursleiter = benutzer.getRolle() == Rolle.KURSLEITER;
        Badge badge = new Badge(kursleiter ? "Kursleiter" : "Teilnehmer");
        if (kursleiter) {
            badge.addThemeVariants(BadgeVariant.SUCCESS);
        }
        return badge;
    }

    private void aktualisiere() {
        String nachname = suche.getValue();
        grid.setItems(nachname.isBlank() ? benutzerRepository.findAll()
                : benutzerRepository.findByNachnameContainingIgnoreCase(nachname));
    }

    private void bearbeite(Benutzer benutzer) {
        TextField vorname = new TextField("Vorname");
        TextField nachname = new TextField("Nachname");
        EmailField email = new EmailField("E-Mail");
        Select<Rolle> rolle = new Select<>();
        rolle.setLabel("Rolle");
        rolle.setItems(Rolle.values());
        rolle.setItemLabelGenerator(r -> r == Rolle.KURSLEITER ? "Kursleiter" : "Teilnehmer");

        BeanValidationBinder<Benutzer> binder = new BeanValidationBinder<>(Benutzer.class);
        binder.forField(vorname).bind("vorname");
        binder.forField(nachname).bind("nachname");
        binder.forField(email).bind("email");
        binder.forField(rolle).bind("rolle");

        FormLayout formular = new FormLayout(vorname, nachname, email, rolle);
        formular.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("480px", 2));

        String titel = benutzer.getId() == null ? "Neuer Benutzer" : "Benutzer bearbeiten";
        new FormDialog<>(titel, binder, benutzer, formular, b -> {
            benutzerRepository.save(b);
            aktualisiere();
        }).open();
    }

    private void loesche(Benutzer benutzer) {
        UiHilfe.loeschenBestaetigen(benutzer.getVorname() + " " + benutzer.getNachname(), () -> {
            benutzerRepository.deleteById(benutzer.getId());
            aktualisiere();
        });
    }
}
