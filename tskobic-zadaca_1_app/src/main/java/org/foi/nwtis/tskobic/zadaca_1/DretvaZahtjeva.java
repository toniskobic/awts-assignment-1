package org.foi.nwtis.tskobic.zadaca_1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.foi.nwtis.tskobic.vjezba_03.konfiguracije.Konfiguracija;

/**
 * Klasa dretva DretvaZahtjeva.
 */
public class DretvaZahtjeva extends Thread {

	/** broj aktivnih dretvi. */
	private volatile static int brojAktivnihDretvi = 0;

	/** naziv dretve. */
	String nazivDretve;

	/** međuspremnik. */
	static ConcurrentHashMap<String, Medjuspremnik> chm = new ConcurrentHashMap<String, Medjuspremnik>();

	/** Objekt klase ServerGlavni. */
	ServerGlavni serverGlavni = null;

	/** Konfiguracijski podaci. */
	Konfiguracija konfig = null;

	/** veza. */
	Socket veza = null;

	/** Dozvoljeni izraz za aero naredbu. */
	String aero = "^AIRPORT$";

	/** Dozvoljeni izraz za aero icao naredbu. */
	String aeroIcao = "^AIRPORT ([A-Z]{4})$";

	/** Dozvoljeni izraz za icao udaljenost naredbu. */
	String aeroIcaoUdaljenost = "^AIRPORT ([A-Z]{4}) (\\d{1,5})$";

	/** Dozvoljeni izraz za meteo icao naredbu. */
	String meteoIcao = "^METEO ([A-Z]{4})$";

	/** Dozvoljeni izraz za meteo icao datum naredbu. */
	String meteoIcaoDatum = "^METEO ([A-Z]{4}) (\\d{4}-\\d{2}-\\d{2})$";

	/** Dozvoljeni izraz za meteo temp naredbu. */
	String meteoTemp = "^TEMP (-?\\d{1,3},\\d{1}) (-?\\d{1,3},\\d{1})$";

	/** Dozvoljeni izraz za meteo temp datum naredbu. */
	String meteoTempDatum = "^TEMP (-?\\d{1,3},\\d{1}) (-?\\d{1,3},\\d{1}) (\\d{4}-\\d{2}-\\d{2})$";

	/** Dozvoljeni izraz za udaljenost naredbu. */
	String udaljenostIcao = "^DISTANCE ([A-Z]{4}) ([A-Z]{4})$";

	/** Dozvoljeni izraz za udaljenost ocisti naredbu. */
	String udaljenostOcisti = "^DISTANCE CLEAR$";

	/** Dozvoljeni izraz za međuspremnik ocisti naredbu. */
	String medjuspremnikSpremi = "^CACHE BACKUP$";

	/** Dozvoljeni izraz za međuspremnik vrati naredbu. */
	String medjuspremnikVrati = "^CACHE RESTORE$";

	/** Dozvoljeni izraz za međuspremnik očisti naredbu. */
	String medjuspremnikOcisti = "^CACHE CLEAR$";

	/** Dozvoljeni izraz za međuspremnik statistika naredbu. */
	String medjuspremnikStat = "^CACHE STAT$";

	/** Dozvoljeni izraz za cijelu komandu zaprimljenu od klijenta. */
	String kompletnaKomanda = "^(?<autentikacija>USER ([A-Za-z0-9_-]{3,10}) PASSWORD ([A-Za-z0-9#!_-]{3,10})) "
			+ "(?<predmet>(AIRPORT)|(AIRPORT ([A-Z]{4}))|(AIRPORT ([A-Z]{4}) (\\d{1,5}))|(METEO ([A-Z]{4}))|"
			+ "(METEO ([A-Z]{4}) (\\d{4}-\\d{2}-\\d{2}))|(METEO ([A-Z]{4}) (\\d{4}-\\d{2}-\\d{2}))|"
			+ "(TEMP (-?\\d{1,3},\\d{1}) (-?\\d{1,3},\\d{1}))|(TEMP (-?\\d{1,3},\\d{1}) (-?\\d{1,3},\\d{1}) (\\d{4}-\\d{2}-\\d{2}))|"
			+ "(DISTANCE ([A-Z]{4}) ([A-Z]{4}))|(DISTANCE CLEAR)|(CACHE BACKUP)|(CACHE RESTORE)|(CACHE CLEAR)|(CACHE STAT))$";

	/**
	 * Konstruktor klase
	 *
	 * @param serverGlavni objekt klase ServerGlavni
	 * @param konfig       konfiguracijski podaci
	 * @param veza         veza
	 */
	public DretvaZahtjeva(ServerGlavni serverGlavni, Konfiguracija konfig, Socket veza) {
		super("tskobic_" + (brojAktivnihDretvi + 1));
		this.serverGlavni = serverGlavni;
		this.nazivDretve = super.getName();
		this.konfig = konfig;
		this.veza = veza;
		synchronized (this) {
			brojAktivnihDretvi++;
		}
	}

	/**
	 * Metoda za pokretanje dretve
	 */
	@Override
	public synchronized void start() {
		super.start();
	}

	/**
	 * Glavna metoda za rad dretve
	 */
	@Override
	public void run() {
		try (InputStreamReader isr = new InputStreamReader(this.veza.getInputStream(), Charset.forName("UTF-8"));
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

			if (!provjeraSintakseObrada(tekst.toString(), kompletnaKomanda)) {
				ispisPoruke(osw, "ERROR 40 Sintaksa komande nije uredu.");
			} else {
				obradaZahtjeva(osw, tekst.toString());
			}

		} catch (Exception e) {
			ispis(e.getMessage());
		}
		synchronized (this) {
			brojAktivnihDretvi--;
		}
	}

	/**
	 * Obrada zahtjeva.
	 *
	 * @param osw     izlazni tok podataka
	 * @param komanda omanda
	 */
	private void obradaZahtjeva(OutputStreamWriter osw, String komanda) {
		String p[] = podijeliKomandu(komanda);
		if (autentikacijaKorisnika(p[0])) {
			if (provjeraSintakseObrada(p[1], meteoIcao) || provjeraSintakseObrada(p[1], meteoIcaoDatum)
					|| provjeraSintakseObrada(p[1], meteoTemp) || provjeraSintakseObrada(p[1], meteoTempDatum)) {
				izvrsiNaredbu(osw, p[1], "server.meteo.adresa", "server.meteo.port", 'M');
			} else if (provjeraSintakseObrada(p[1], aero) || provjeraSintakseObrada(p[1], aeroIcao)
					|| provjeraSintakseObrada(p[1], aeroIcaoUdaljenost)) {
				izvrsiNaredbu(osw, p[1], "server.aerodroma.adresa", "server.aerodroma.port", 'A');
			} else if (provjeraSintakseObrada(p[1], udaljenostIcao) || provjeraSintakseObrada(p[1], udaljenostOcisti)) {
				izvrsiNaredbu(osw, p[1], "server.udaljenosti.adresa", "server.udaljenosti.port", 'U');
			} else if (provjeraSintakseObrada(p[1], medjuspremnikSpremi)
					|| provjeraSintakseObrada(p[1], medjuspremnikVrati)
					|| provjeraSintakseObrada(p[1], medjuspremnikOcisti)
					|| provjeraSintakseObrada(p[1], medjuspremnikStat)) {
				izvrsiMedjuspremnikNaredbu(osw, p[1], "datoteka.meduspremnika");
			} else {
				ispisPoruke(osw, "ERROR 40 Sintaksa komande nije uredu.");
			}
		} else {
			ispisPoruke(osw, "ERROR 41 Unijeli ste pogrešno korisničko ime ili lozinku.");
		}
	}

	/**
	 * Podjela komande na autentikacijski i predmetni dio.
	 *
	 * @param komanda komanda
	 * @return the string[]
	 */
	private String[] podijeliKomandu(String komanda) {
		String polje[] = new String[2];
		Pattern uzorak = Pattern.compile(kompletnaKomanda);
		Matcher m = uzorak.matcher(komanda);
		boolean status = m.matches();
		if (status) {
			polje[0] = m.group("autentikacija");
			polje[1] = m.group("predmet");
		}

		return polje;
	}

	/**
	 * Autentikacija korisnika.
	 *
	 * @param komanda komanda
	 * @return true, if successful
	 */
	private boolean autentikacijaKorisnika(String komanda) {
		String p[] = komanda.split(" ");
		Korisnik korisnik = serverGlavni.korisnici.stream()
				.filter(a -> a.getKorisnickoIme().equals(p[1]) && a.getLozinka().equals(p[3])).findAny().orElse(null);
		if (korisnik == null) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Provjera sintakse dozvoljenog izraza.
	 *
	 * @param komanda        komanda
	 * @param regularniIzraz dozvoljeni izraz
	 * @return true, if successful
	 */
	private boolean provjeraSintakseObrada(String komanda, String regularniIzraz) {
		Pattern izraz = Pattern.compile(regularniIzraz);
		Matcher rezultatUsporedbe = izraz.matcher(komanda);

		return rezultatUsporedbe.matches();
	}

	/**
	 * Izvršavanje međuspremnik naredbe.
	 *
	 * @param osw       izlazni tok podataka
	 * @param komanda   komanda
	 * @param konfigDat the konfig dat
	 */
	@SuppressWarnings("unchecked")
	private void izvrsiMedjuspremnikNaredbu(OutputStreamWriter osw, String komanda, String konfigDat) {
		if (provjeraSintakseObrada(komanda, medjuspremnikOcisti)) {
			chm.clear();
			ispisPoruke(osw, "OK");
		} else if (provjeraSintakseObrada(komanda, medjuspremnikSpremi)) {
			String datoteka = "";
			synchronized (konfig) {
				datoteka = this.konfig.dajPostavku(konfigDat);
			}
			serijalizacija(osw, datoteka, chm);
		} else if (provjeraSintakseObrada(komanda, medjuspremnikVrati)) {
			String datoteka = "";
			synchronized (konfig) {
				datoteka = this.konfig.dajPostavku(konfigDat);
			}
			Object podaci = deserijalizacija(osw, datoteka);
			if (podaci != null) {
				chm = (ConcurrentHashMap<String, Medjuspremnik>) podaci;
			} else {
				ispisPoruke(osw, "ERROR 49 Deserijalizacija podataka međuspremnika iz datoteke nije uspjela.");
			}
		} else if (provjeraSintakseObrada(komanda, medjuspremnikStat)) {
			// TODO CACHE STAT
		}
	}

	/**
	 * Serijalizacija međuspremnika u datoteku.
	 *
	 * @param osw      izlazni tok podataka
	 * @param datoteka naziv datoteke
	 * @param obj      objekt koji se serijalizira
	 */
	private void serijalizacija(OutputStreamWriter osw, String datoteka, Object obj) {
		FileOutputStream out = null;

		try {
			out = new FileOutputStream(datoteka);
		} catch (FileNotFoundException e) {
			ispis(e.getMessage());
		}

		try {
			ObjectOutputStream s = new ObjectOutputStream(out);
			s.writeObject(obj);
			s.close();
			out.close();
			ispisPoruke(osw, "OK");
		} catch (IOException e) {
			ispis(e.getMessage());
		}
	}

	/**
	 * Deserijalizacija podataka iz datoteke u međuspremnik.
	 *
	 * @param osw      izlazni tok podataka
	 * @param datoteka datoteka
	 * @return the object
	 */
	private Object deserijalizacija(OutputStreamWriter osw, String datoteka) {
		File f = new File(datoteka);
		if (f.exists() && !f.isDirectory()) {
			try {
				FileInputStream in = new FileInputStream(datoteka);
				ObjectInputStream s = new ObjectInputStream(in);
				Object obj = s.readObject();
				s.close();
				in.close();
				ispisPoruke(osw, "OK");
				return obj;
			} catch (IOException | ClassNotFoundException e) {
				ispis(e.getMessage());
			}
			return null;
		} else {
			return null;
		}
	}

	/**
	 * Izvršavanje naredbe zaprimljene od klijenta.
	 *
	 * @param osw          izlazni tok podataka
	 * @param komanda      komanda
	 * @param konfigAdresa adresa servera
	 * @param konfigPort   broj porta
	 * @param server       oznaka koja predstavlja server
	 */
	private void izvrsiNaredbu(OutputStreamWriter osw, String komanda, String konfigAdresa, String konfigPort,
			char server) {
		String adresaServer = "";
		int portServer = 0;
		synchronized (konfig) {
			adresaServer = this.konfig.dajPostavku(konfigAdresa);
			portServer = Integer.parseInt(this.konfig.dajPostavku(konfigPort));
		}
		String odgovor = this.posaljiKomandu(adresaServer, portServer, komanda);
		if (odgovor == null) {
			if (server == 'M') {
				ispisPoruke(osw, "ERROR 42 Server ServerMeteo ne radi.");
			} else if (server == 'A') {
				ispisPoruke(osw, "ERROR 43 Server ServerAerodroma ne radi.");
			} else if (server == 'U') {
				ispisPoruke(osw, "ERROR 44 Server ServerUdaljenosti ne radi.");
			}
		} else {
			if (server == 'A' || (server == 'U' && !komanda.equals("DISTANCE CLEAR"))) {
				izvrsiSpremanje(komanda);
			}
			ispisPoruke(osw, odgovor);
		}
	}

	/**
	 * Izvrsšavanje spremanja u međuspremnik
	 *
	 * @param komanda komanda
	 */
	private void izvrsiSpremanje(String komanda) {
		if (chm.containsKey(komanda)) {
			chm.computeIfPresent(komanda, (k, v) -> new Medjuspremnik(v.brojKoristenja + 1, new Date()));
		} else {
			chm.put(komanda, new Medjuspremnik(1, new Date()));
		}
	}

	/**
	 * Ispis poruke klijentu.
	 *
	 * @param osw     izlazni tok podataka
	 * @param odgovor odgovor
	 */
	private void ispisPoruke(OutputStreamWriter osw, String odgovor) {
		try {
			osw.write(odgovor);
			osw.flush();
			osw.close();
		} catch (IOException e) {
			ispis(e.getMessage());
		}
	}

	/**
	 * Metoda za prekidanje rada dretve.
	 */
	@Override
	public void interrupt() {
		super.interrupt();
	}

	/**
	 * Slanje komande serveru
	 *
	 * @param adresa  adresa servera
	 * @param port    broj porta
	 * @param komanda komanda
	 * @return the string
	 */
	public String posaljiKomandu(String adresa, int port, String komanda) {
		try (Socket veza = new Socket(adresa, port);
				InputStreamReader isr = new InputStreamReader(veza.getInputStream(), Charset.forName("UTF-8"));
				OutputStreamWriter osw = new OutputStreamWriter(veza.getOutputStream(), Charset.forName("UTF-8"));) {

			osw.write(komanda);
			osw.flush();
			veza.shutdownOutput();
			StringBuilder tekst = new StringBuilder();
			while (true) {
				int i = isr.read();
				if (i == -1) {
					break;
				}
				tekst.append((char) i);
			}
			veza.shutdownInput();
			veza.close();
			return tekst.toString();
		} catch (SocketException e) {
			ispis(e.getMessage());
		} catch (IOException ex) {
			ispis(ex.getMessage());
		}
		return null;
	}

	/**
	 * Ispis poruke na konzolu.
	 *
	 * @param message poruka
	 */
	private void ispis(String message) {
		System.out.println(message);
	}
}
