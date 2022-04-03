package org.foi.nwtis.tskobic.zadaca_1;

import lombok.Getter;
import lombok.Setter;
import lombok.NonNull;
import lombok.AllArgsConstructor;

/**
 * 
 * Klasa za korisnika
 *
 */
@AllArgsConstructor()
public class Korisnik {
    @Getter
    @Setter 
    @NonNull 
    private String prezime;
    @Getter
    @Setter
    @NonNull 
    private String ime;
    @Getter
    @Setter 
    @NonNull 
    private String korisnickoIme;
    @Getter
    @Setter 
    @NonNull 
    private String lozinka;
}
