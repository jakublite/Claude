package de.jakub.seminarverwaltung.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
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

/** Rahmen für alle Views: Seitenleiste mit Navigation, oben der Titel der aktuellen View. */
@Layout
public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final H1 viewTitel = new H1();

    public MainLayout() {
        setPrimarySection(Section.DRAWER);

        viewTitel.addClassName("view-titel");
        addToNavbar(new DrawerToggle(), viewTitel);

        Div marke = new Div(VaadinIcon.ACADEMY_CAP.create(), new Span("Seminarverwaltung"));
        marke.addClassName("marke");

        SideNav navigation = new SideNav();
        navigation.addItem(
                new SideNavItem("Übersicht", DashboardView.class, VaadinIcon.DASHBOARD.create()),
                new SideNavItem("Seminare", SeminareView.class, VaadinIcon.CALENDAR.create()),
                new SideNavItem("Benutzer", BenutzerView.class, VaadinIcon.USERS.create()),
                new SideNavItem("Zertifikate", ZertifikateView.class, VaadinIcon.DIPLOMA.create()),
                new SideNavItem("Unterlagen", UnterlagenView.class, VaadinIcon.BOOK.create()),
                new SideNavItem("Postausgang", PostausgangView.class, VaadinIcon.ENVELOPE.create()),
                new SideNavItem("Vorlagen", VorlagenView.class, VaadinIcon.FILE_TEXT.create()));

        addToDrawer(marke, new Scroller(navigation));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        PageTitle titel = getContent().getClass().getAnnotation(PageTitle.class);
        viewTitel.setText(titel == null ? "" : titel.value());
    }
}
