package de.jakub.seminarverwaltung.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.PageTitle;

import de.jakub.seminarverwaltung.sicherheit.Angemeldet;
import jakarta.annotation.security.PermitAll;

/** Rahmen für alle Views: Seitenleiste mit Navigation, oben der Titel der aktuellen View und das Konto. */
@Layout
@PermitAll
public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final H1 viewTitel = new H1();

    public MainLayout(Angemeldet angemeldet) {
        setPrimarySection(Section.DRAWER);

        viewTitel.addClassName("view-titel");
        Span email = new Span(angemeldet.email());
        email.addClassName("gedimmt");
        Button ausloggen = new Button("Ausloggen", VaadinIcon.SIGN_OUT.create(), e -> angemeldet.ausloggen());
        ausloggen.addThemeVariants(ButtonVariant.TERTIARY);
        Div konto = new Div(email, ausloggen);
        konto.addClassName("konto");
        addToNavbar(new DrawerToggle(), viewTitel, konto);

        Div marke = new Div(VaadinIcon.ACADEMY_CAP.create(), new Span("Seminarverwaltung"));
        marke.addClassName("marke");

        SideNav navigation = new SideNav();
        navigation.addItem(
                new SideNavItem("Übersicht", DashboardView.class, VaadinIcon.DASHBOARD.create()),
                new SideNavItem("Seminare", SeminareView.class, VaadinIcon.CALENDAR.create()));
        if (angemeldet.istKursleiter()) {
            navigation.addItem(new SideNavItem("Benutzer", BenutzerView.class, VaadinIcon.USERS.create()));
        }
        navigation.addItem(
                new SideNavItem("Zertifikate", ZertifikateView.class, VaadinIcon.DIPLOMA.create()),
                new SideNavItem("Unterlagen", UnterlagenView.class, VaadinIcon.BOOK.create()));
        if (angemeldet.istKursleiter()) {
            navigation.addItem(
                    new SideNavItem("Postausgang", PostausgangView.class, VaadinIcon.ENVELOPE.create()),
                    new SideNavItem("Vorlagen", VorlagenView.class, VaadinIcon.FILE_TEXT.create()));
        }

        addToDrawer(marke, new Scroller(navigation));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        PageTitle titel = getContent().getClass().getAnnotation(PageTitle.class);
        viewTitel.setText(titel == null ? "" : titel.value());
    }
}
