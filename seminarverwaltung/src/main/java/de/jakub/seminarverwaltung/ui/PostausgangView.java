package de.jakub.seminarverwaltung.ui;

import java.time.format.DateTimeFormatter;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.jakub.seminarverwaltung.nachricht.Nachricht;
import de.jakub.seminarverwaltung.nachricht.NachrichtRepository;
import jakarta.annotation.security.RolesAllowed;

/** Zeigt die simulierten E-Mails, ein Klick auf eine Zeile öffnet den Text. */
@Route("postausgang")
@PageTitle("Postausgang")
@RolesAllowed("KURSLEITER")
public class PostausgangView extends VerticalLayout {

    private static final DateTimeFormatter ZEIT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final NachrichtRepository nachrichtRepository;
    private final Grid<Nachricht> grid = new Grid<>();
    private final TextField suche;

    public PostausgangView(NachrichtRepository nachrichtRepository) {
        this.nachrichtRepository = nachrichtRepository;
        addClassName("seite");
        setSizeFull();

        suche = UiHilfe.suchfeld("Empfänger suchen …", t -> aktualisiere());

        grid.addColumn(n -> ZEIT.format(n.getVersendetAm())).setHeader("Versendet")
                .setComparator(Nachricht::getVersendetAm).setAutoWidth(true);
        grid.addColumn(Nachricht::getEmpfaenger).setHeader("Empfänger")
                .setComparator(Nachricht::getEmpfaenger).setAutoWidth(true);
        grid.addComponentColumn(n -> new Badge(n.getAnlass().getBezeichnung())).setHeader("Anlass")
                .setComparator(Nachricht::getAnlass).setAutoWidth(true);
        grid.addColumn(Nachricht::getBetreff).setHeader("Betreff")
                .setComparator(Nachricht::getBetreff).setWidth("240px").setFlexGrow(2);
        grid.addItemClickListener(e -> zeige(e.getItem()));
        grid.setEmptyStateText("Noch keine E-Mails. Melde jemanden für ein Seminar an, dann erscheint hier die Bestätigung.");
        grid.setSizeFull();

        add(suche, grid);
        aktualisiere();
    }

    private void aktualisiere() {
        String empfaenger = suche.getValue();
        grid.setItems(empfaenger.isBlank() ? nachrichtRepository.findAllByOrderByIdDesc()
                : nachrichtRepository.findByEmpfaengerContainingIgnoreCaseOrderByIdDesc(empfaenger));
    }

    private void zeige(Nachricht nachricht) {
        Dialog dialog = new Dialog(nachricht.getBetreff());
        dialog.setWidth("min(640px, 96vw)");
        Pre text = new Pre("An: " + nachricht.getEmpfaenger() + "\n\n" + nachricht.getInhalt());
        text.addClassName("nachricht-text");
        dialog.add(text);
        dialog.getFooter().add(new Button("Schließen", e -> dialog.close()));
        dialog.open();
    }
}
