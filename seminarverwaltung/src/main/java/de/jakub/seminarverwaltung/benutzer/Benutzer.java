package de.jakub.seminarverwaltung.benutzer;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Benutzer {

    @Id
    @GeneratedValue
    private Long id;

    @NotBlank
    private String vorname;

    @NotBlank
    private String nachname;

    @NotBlank
    @Email
    @Column(unique = true)
    private String email;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Rolle rolle;

    /** BCrypt-Hash. Weder im JSON sichtbar noch per JSON setzbar. */
    @JsonIgnore
    private String passwortHash;
}
