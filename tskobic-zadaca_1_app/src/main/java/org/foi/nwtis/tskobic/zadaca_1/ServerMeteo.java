package org.foi.nwtis.tskobic.zadaca_1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.foi.nwtis.tskobic.vjezba_03.konfiguracije.Konfiguracija;
import org.foi.nwtis.tskobic.vjezba_03.konfiguracije.KonfiguracijaApstraktna;
import org.foi.nwtis.tskobic.vjezba_03.konfiguracije.NeispravnaKonfiguracija;

/**
 * Glavna klasa poslužitelja ServerMeteo
 */
public class ServerMeteo {
	
	/** broj porta. */
	int port;
	
	/** maksimalni broj čekača. */
	int maksCekaca;
	
	/** veza. */
	Socket veza = null;
	
	/** Kolekcija meteoroloških podataka za aerodrome. */
	List<AerodromMeteo> aerodromiMeteo = new ArrayList<>();

	/** Dozvoljeni izraz za meteo icao naredbu. */
	String meteoIcao = "^METEO ([A-Z]{4})$";
	
	/** Dozvoljeni izraz za meteo icao datum naredbu. */
	String meteoIcaoDatum = "^METEO ([A-Z]{4}) (\\d{4}-\\d{2}-\\d{2})$";
	
	/** Dozvoljeni izraz za meteo temp naredbu. */
	String meteoTemp = "^TEMP (-?\\d{1,3},\\d{1}) (-?\\d{1,3},\\d{1})$";
	
	/** Dozvoljeni izraz za meteo temp datum naredbu. */
	String meteoTempDatum = "^TEMP (-?\\d{1,3},\\d{1}) (-?\\d{1,3},\\d{1}) (\\d{4}-\\d{2}-\\d{2})$";

	/** konfiguracijski podaci. */
	static public Konfiguracija konfig = null;
	
	/** iso format za datum. */
	static SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	/**
	 * Konstruktor klase.
	 *
	 * @param port       broj porta
	 * @param maksCekaca maksimalan broj čekača
	 */
	public ServerMeteo(int port, int maksCekaca) {
		super();
		this.port = port;
		this.maksCekaca = maksCekaca;
	}

	/**
	 * Učitavanje konfiguracijskih podataka.
	 *
	 * @param nazivDatoteke naziv datoteke konfiguracijskih podataka
	 */
	private static void ucitavanjePodataka(String nazivDatoteke) {
		try {
			konfig = KonfiguracijaApstraktna.preuzmiKonfiguraciju(nazivDatoteke);
		} catch (NeispravnaKonfiguracija e) {
			e.printStackTrace();
		}
	}

	/**
	 * Priprema (učitavanje) meteoroloških podataka za aerodrome.
	 *
	 * @param nazivDatotekeMeteoPodataka naziv datoteke meteo podataka
	 */
	private void pripremiMeteo(String nazivDatotekeMeteoPodataka) {
		try {
			BufferedReader br = new BufferedReader(
					new FileReader(nazivDatotekeMeteoPodataka, Charset.forName("UTF-8")));
			while (true) {
				String linija = br.readLine();
				if (linija == null || linija.isEmpty()) {
					break;
				}
				String[] p = linija.split(";");
				AerodromMeteo am;
				try {
					am = new AerodromMeteo(p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2]),
							Double.parseDouble(p[3]), p[4], isoFormat.parse(p[4]).getTime());
					aerodromiMeteo.add(am);
				} catch (NumberFormatException | ParseException e) {
					e.printStackTrace();
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Obrada zahtjeva.
	 */
	public void obradaZahtjeva() {

		try (ServerSocket ss = new ServerSocket(this.port, this.maksCekaca)) {
			while (true) {
				this.veza = ss.accept();

				try (InputStreamReader isr = new InputStreamReader(this.veza.getInputStream(),
						Charset.forName("UTF-8"));
						OutputStreamWriter osw = new OutputStreamWriter(this.veza.getOutputStream(),
								Charset.forName("UTF-8"));) {

					StringBuilder tekst = new StringBuilder();
					while (true) {
						int i = isr.read();
						if (i == -1) {
							break;
						}
						tekst.append((char) i);
					}
					this.veza.shutdownInput();

					if (provjeraSintakseObrada(tekst.toString(), meteoIcao)) {
						izvrsiMeteoIcao(osw, tekst.toString());
					} else if (provjeraSintakseObrada(tekst.toString(), meteoIcaoDatum)) {
						izvrsiMeteoIcaoDatum(osw, tekst.toString());
					} else if (provjeraSintakseObrada(tekst.toString(), meteoTemp)) {
						izvrsiMeteoTemp(osw, tekst.toString());
					} else if (provjeraSintakseObrada(tekst.toString(), meteoTempDatum)) {
						izvrsiMeteoTempDatum(osw, tekst.toString());
					} else {
						ispisGreske(osw, "ERROR 10 Sintaksa komande nije uredu.");
					}

				} catch (SocketException e) {
					e.printStackTrace();
				}
			}

		} catch (

		IOException ex) {
			Logger.getLogger(ServerMeteo.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	/**
	 * Provjera sintakse primljene naredbe.
	 *
	 * @param komanda komanda
	 * @param regularniIzraz dozvoljeni izraz
	 * @return true, ako je provjera uspješna
	 */
	private boolean provjeraSintakseObrada(String komanda, String regularniIzraz) {
		Pattern izraz = Pattern.compile(regularniIzraz);
		Matcher rezultatUsporedbe = izraz.matcher(komanda);

		return rezultatUsporedbe.matches();
	}

	/**
	 * Izvršavanje naredbe meteo icao
	 *
	 * @param osw izlazni tok podataka
	 * @param komanda komanda
	 */
	private void izvrsiMeteoIcao(OutputStreamWriter osw, String komanda) {
		String p[] = komanda.split(" ");
		String icao = p[1];
		String odgovor = null;

		List<AerodromMeteo> fAerodromiMeteo = aerodromiMeteo.stream().filter(c -> c.getIcao().equals(icao))
				.collect(Collectors.toList());
		if (fAerodromiMeteo.isEmpty()) {
			ispisGreske(osw, "ERROR 11 Aerodrom '" + icao + "' ne postoji.");
		} else {
			AerodromMeteo am = fAerodromiMeteo.stream().max(Comparator.comparing(AerodromMeteo::getTime))
					.orElseThrow(NoSuchElementException::new);
			odgovor = "OK " + zaokruzi(am.temp, 1) + " " + am.vlaga + " " + am.tlak + " " + am.vrijeme + ";";
			try {
				osw.write(odgovor);
				osw.flush();
				osw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Metoda za zaokruživanje na unesenu preciznost, tj. decimalu
	 *
	 * @param vrijednost unesena vrijednost
	 * @param decimala preciznost zaokruživanja
	 * @return the double
	 */
	private double zaokruzi(double vrijednost, int decimala) {
		int d = (int) Math.pow(10, decimala);
		return (double) Math.round(vrijednost * d) / d;
	}

	/**
	 * Izvršavanje naredbe meteo icao datum
	 *
	 * @param osw izlazni tok podataka
	 * @param komanda komanda
	 */
	private void izvrsiMeteoIcaoDatum(OutputStreamWriter osw, String komanda) {
		String p[] = komanda.split(" ");
		String icao = p[1];
		String datum = p[2] + " 00:00:00.00";
		String odgovor = "";

		List<AerodromMeteo> fAerodromiMeteo = aerodromiMeteo.stream().filter(c -> c.getIcao().equals(icao))
				.collect(Collectors.toList());
		if (fAerodromiMeteo.isEmpty()) {
			ispisGreske(osw, "ERROR 11 Aerodrom '" + icao + "' ne postoji.");
		} else {
			try {
				long uneseniDatum = isoFormat.parse(datum).getTime();
				fAerodromiMeteo = fAerodromiMeteo.stream()
						.filter(c -> c.getTime() > uneseniDatum && c.getTime() < uneseniDatum + 86400000)
						.collect(Collectors.toList());
				for (AerodromMeteo am : fAerodromiMeteo) {
					odgovor = odgovor + "OK " + (double) Math.round(am.temp * 10) / 10 + " " + am.vlaga + " " + am.tlak
							+ " " + am.vrijeme + "; ";
				}
				try {
					osw.write(odgovor);
					osw.flush();
					osw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			} catch (NumberFormatException | ParseException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Izvršavanje naredbe meteo temp
	 *
	 * @param osw izlazni tok podataka
	 * @param komanda komanda
	 */
	private void izvrsiMeteoTemp(OutputStreamWriter osw, String komanda) {
		String p[] = komanda.split(" ");
		String odgovor = "";

		try {
			double temp1 = izvrsiDoublePretvorbu(p[1]);
			double temp2 = izvrsiDoublePretvorbu(p[2]);
			Predicate<AerodromMeteo> izraz = c -> c.getTemp() >= temp1 && c.getTemp() <= temp2;

			if (temp1 > temp2) {
				izraz = c -> c.getTemp() >= temp2 && c.getTemp() <= temp1;
			}

			List<AerodromMeteo> fAerodromiMeteo = aerodromiMeteo.stream().filter(izraz).collect(Collectors.toList());
			if (fAerodromiMeteo.isEmpty()) {
				ispisGreske(osw,
						"ERROR 11 Meteo podaci s temperaturom u rasponu " + temp1 + " i " + temp2 + " ne postoje.");
			} else {
				for (AerodromMeteo am : fAerodromiMeteo) {
					odgovor = odgovor + "OK " + am.icao + " " + (double) Math.round(am.temp * 10) / 10 + " " + am.vlaga
							+ " " + am.tlak + " " + am.vrijeme + "; ";
				}

				try {
					osw.write(odgovor);
					osw.flush();
					osw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Izvršavanje naredbe meteo temp datum.
	 *
	 * @param osw izlazni tok podataka
	 * @param komanda komanda
	 */
	private void izvrsiMeteoTempDatum(OutputStreamWriter osw, String komanda) {
		String p[] = komanda.split(" ");
		String datum = p[3] + " 00:00:00.00";
		String odgovor = "";

		try {
			double temp1 = izvrsiDoublePretvorbu(p[1]);
			double temp2 = izvrsiDoublePretvorbu(p[2]);
			long uneseniDatum = isoFormat.parse(datum).getTime();

			Predicate<AerodromMeteo> izraz = c -> (c.getTemp() >= temp1 && c.getTemp() <= temp2)
					&& (c.getTime() > uneseniDatum && c.getTime() < uneseniDatum + 86400000);

			if (temp1 > temp2) {
				izraz = c -> (c.getTemp() >= temp2 && c.getTemp() <= temp1)
						&& (c.getTime() > uneseniDatum && c.getTime() < uneseniDatum + 86400000);
			}

			List<AerodromMeteo> fAerodromiMeteo = aerodromiMeteo.stream().filter(izraz).collect(Collectors.toList());
			if (fAerodromiMeteo.isEmpty()) {
				ispisGreske(osw, "ERROR 11 Meteo podaci s temperaturom u rasponu " + temp1 + " i " + temp2 + " na datum"
						+ datum + "ne postoje.");
			} else {
				for (AerodromMeteo am : fAerodromiMeteo) {
					odgovor = odgovor + "OK " + am.icao + " " + (double) Math.round(am.temp * 10) / 10 + " " + am.vlaga
							+ " " + am.tlak + " " + am.vrijeme + "; ";
				}

				try {
					osw.write(odgovor);
					osw.flush();
					osw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Izvršavanje pretvorbe iz double varijable sa zarezom u double varijablu sa točkom
	 *
	 * @param unos the unos
	 * @return the double
	 * @throws ParseException the parse exception
	 */
	private double izvrsiDoublePretvorbu(String unos) throws ParseException {
		NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
		double broj = format.parse(unos).doubleValue();
		return broj;
	}

	/**
	 * Ispisivanje greške.
	 *
	 * @param osw izlazni tok podataka
	 * @param odgovor odgovor
	 */
	private void ispisGreske(OutputStreamWriter osw, String odgovor) {
		try {
			osw.write(odgovor);
			osw.flush();
			osw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Glavna metoda
	 *
	 * @param argumenti
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Broj argumenata nije 1.");
			return;
		}
		ucitavanjePodataka(args[0]);

		if (konfig == null) {
			System.out.println("Problem s konfiguracijom.");
			return;
		}

		int port = Integer.parseInt(konfig.dajPostavku("port"));
		int maksCekaca = Integer.parseInt(konfig.dajPostavku("maks.cekaca"));
		String nazivDatotekeMeteoPodataka = konfig.dajPostavku("datoteka.meteo");

		ServerMeteo sm = new ServerMeteo(port, maksCekaca);
		sm.pripremiMeteo(nazivDatotekeMeteoPodataka);
		System.out.println("Broj podataka: " + sm.aerodromiMeteo.size());
		sm.obradaZahtjeva();
	}
}
