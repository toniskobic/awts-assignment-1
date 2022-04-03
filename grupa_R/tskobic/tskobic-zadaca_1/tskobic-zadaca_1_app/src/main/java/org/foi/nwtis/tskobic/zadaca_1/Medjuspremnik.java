package org.foi.nwtis.tskobic.zadaca_1;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 *
 * Klasa za međuspremnik
 */
@AllArgsConstructor()
public class Medjuspremnik implements Serializable {
	private static final long serialVersionUID = 1L;
	@Getter
	@Setter
	int brojKoristenja;
	@Getter
	@Setter
	@NonNull
	Date zadnjeVrijeme;
}
