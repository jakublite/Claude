package de.jakub.seminarverwaltung.seminar;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

@Entity
@Getter
@Setter
public class OnlineSeminar extends Seminar {

    @NotBlank
    @URL
    private String meetingLink;

    /** z. B. Teams, Zoom, Webex */
    private String plattform;

    @Override
    public String getTyp() {
        return "ONLINE";
    }
}
