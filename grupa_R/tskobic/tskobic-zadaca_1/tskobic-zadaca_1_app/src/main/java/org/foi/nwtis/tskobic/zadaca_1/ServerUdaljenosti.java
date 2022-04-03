package org.foi.nwtis.tskobic.zadaca_1;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.foi.nwtis.tskobic.vjezba_03.konfiguracije.Konfiguracija;
import org.foi.nwtis.tskobic.vjezba_03.konfiguracije.KonfiguracijaApstraktna;
import org.foi.nwtis.tskobic.vjezba_03.konfiguracije.NeispravnaKonfiguracija;

/**
 * Glavna klasa poslužitelja ServerUdaljenosti
 */
public class ServerUdaljenosti {

	/** broj porta. */
	int port;

	/** maksimalni broj čekača. */
	int maksCekaca;

	/** maksimalno čekanje na spajanje klijenta. */
	int maksCekanje;

	/** veza. */
	Socket veza = null;

	/** Konfiguracijski podaci. */
	static public Konfiguracija konfig = null;

	/**
	 * Konstruktor klase.
	 *
	 * @param port        port porta
	 * @param maksCekaca  maksimalni broj čekača
	 * @param maksCekanje maksimalno čekanje na spajanje klijenta
	 */
	public ServerUdaljenosti(int port, int maksCekaca, int maksCekanje) {
		super();
		this.port = port;
		this.maksCekaca = maksCekaca;
		this.maksCekanje = maksCekanje;
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
	 * Obrada zahtjeva.
	 */
	public void obradaZahtjeva() {

		try (ServerSocket ss = new ServerSocket(this.port, this.maksCekaca)) {
			ss.setSoTimeout(maksCekanje);
			while (true) {
				System.out.println("Čekam korisnika."); // TODO kasnije obrisati
				this.veza = ss.accept();

				DretvaUdaljenosti dretvaUdaljenosti = new DretvaUdaljenosti(konfig, veza);
				dretvaUdaljenosti.start();
			}

		} catch (IOException ex) {
			Logger.getLogger(ServerGlavni.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	/**
	 * Glavna metoda.
	 *
	 * @param args argumenti
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
		int maksCekanje = Integer.parseInt(konfig.dajPostavku("maks.cekanje"));

		ServerUdaljenosti su = new ServerUdaljenosti(port, maksCekaca, maksCekanje);
		su.obradaZahtjeva();
	}

	/**
	 * Klasa dretva DretvaUdaljenosti
	 */
	private class DretvaUdaljenosti extends Thread {

		/** konfiguracijski podaci. */
		volatile Konfiguracija konfig = null;

		/** veza. */
		Socket veza = null;

		/** lokalna kolekcija aerodroma. */
		static volatile List<Aerodrom> aerodromi = new ArrayList<>();

		/** dozvoljeni izraz za naredbu udaljenost icao. */
		String udaljenostIcao = "^DISTANCE ([A-Z]{4}) ([A-Z]{4})$";

		/** dozvoljeni izraz za naredbu udaljenost ocisti. */
		String udaljenostOcisti = "^DISTANCE CLEAR$";

		/**
		 * Konstruktor klase.
		 *
		 * @param konfig konfiguracijski podaci
		 * @param veza   veza
		 */
		// TODO pogledati za naziv dretve
		public DretvaUdaljenosti(Konfiguracija konfig, Socket veza) {
			super();
			this.konfig = konfig;
			this.veza = veza;
		}

		/**
		 * Metoda za pokretanje dretve
		 */
		@Override
		public synchronized void start() {
			// TODO Auto-generated method stub
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

				if (provjeraSintakseObrada(tekst.toString(), udaljenostIcao)) {
					izvrsiUdaljenostIcao(osw, tekst.toString());
				} else if (provjeraSintakseObrada(tekst.toString(), udaljenostOcisti)) {
					izvrsiBrisanjeSpremnika(osw, tekst.toString());
				} else {
					ispisGreske(osw, "ERROR 20 Sintaksa komande nije uredu.");
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		/**
		 * Provjera sintakse dozvoljenog izraza.
		 *
		 * @param komanda        komanda
		 * @param regularniIzraz dozvoljeni izraz
		 * @return true, ako uspješno
		 */
		private boolean provjeraSintakseObrada(String komanda, String regularniIzraz) {
			Pattern izraz = Pattern.compile(regularniIzraz);
			Matcher rezultatUsporedbe = izraz.matcher(komanda);

			return rezultatUsporedbe.matches();
		}

		/**
		 * Izvršavanje naredbe udaljenost icao.
		 *
		 * @param osw     izlazni tok podataka
		 * @param komanda komanda
		 */
		private void izvrsiUdaljenostIcao(OutputStreamWriter osw, String komanda) {
			String p[] = komanda.split(" ");
			String icao1 = p[1];
			String icao2 = p[2];

			String odgovor = "";

			Aerodrom aerodrom1 = null;
			Aerodrom aerodrom2 = null;

			synchronized (aerodromi) {
				aerodrom1 = aerodromi.stream().filter(a -> a.getIcao().equals(icao1)).findAny().orElse(null);
				aerodrom2 = aerodromi.stream().filter(a -> a.getIcao().equals(icao2)).findAny().orElse(null);
			}

			if (aerodrom1 == null || aerodrom2 == null) {
				if (aerodrom1 == null) {
					String odgovorAeroServer = dobaviAerodrom(icao1);
					if (odgovorAeroServer == null) {
						ispisGreske(osw, "ERROR 32 Server ServerAerodroma ne radi.");
						return;
					} else if (odgovorAeroServer.startsWith("ERROR")) {
						ispisGreske(osw, "ERROR 31 Ne postoji aerodrom za traženi " + icao1 + " aerodrom.");
						return;
					}
					aerodrom1 = izvrsiAerodromPretvorbu(odgovorAeroServer);
					synchronized (aerodromi) {
						aerodromi.add(aerodrom1);
					}
				}
				if (aerodrom2 == null) {
					String odgovorAeroServer = dobaviAerodrom(icao2);
					if (odgovorAeroServer == null) {
						ispisGreske(osw, "ERROR 32 Server ServerAerodroma ne radi.");
						return;
					} else if (odgovorAeroServer.startsWith("ERROR")) {
						ispisGreske(osw, "ERROR 31 Ne postoji aerodrom za traženi " + icao2 + " aerodrom.");
						return;
					}
					aerodrom2 = izvrsiAerodromPretvorbu(odgovorAeroServer);
					synchronized (aerodromi) {
						aerodromi.add(aerodrom2);
					}
				}
			}
			double udaljenost = (double) udaljenost(Float.valueOf(aerodrom1.getGpsGS()),
					Float.valueOf(aerodrom1.getGpsGD()), Float.valueOf(aerodrom2.getGpsGS()),
					Float.valueOf(aerodrom2.getGpsGD()));

			odgovor = "OK " + (int) Math.round(udaljenost);

			try {
				osw.write(odgovor);
				osw.flush();
				osw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		/**
		 * Izvršavanje naredbe brisanje spremnika.
		 *
		 * @param osw     izlazni tok podataka
		 * @param komanda komanda
		 */
		private void izvrsiBrisanjeSpremnika(OutputStreamWriter osw, String komanda) {
			synchronized (aerodromi) {
				aerodromi.clear();
			}
			String odgovor = "OK";

			try {
				osw.write(odgovor);
				osw.flush();
				osw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Metoda za izračunavanje udaljenosti dvije lokacije na Zemlji.
		 *
		 * @param gs1 geografska širina 1
		 * @param gd1 geografska duljina 1
		 * @param gs2 geografska širina 2
		 * @param gd2 geografska duljina 2
		 * @return the float
		 */
		private float udaljenost(float gs1, float gd1, float gs2, float gd2) {
			double earthRadius = 6371000; // meters
			double dLat = Math.toRadians(gs2 - gs1);
			double dLng = Math.toRadians(gd2 - gd1);
			double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(gs1))
					* Math.cos(Math.toRadians(gs2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
			double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
			float dist = (float) (earthRadius * c);
			dist = dist / 1000;

			return dist;
		}

		/**
		 * Dobavljanje areodroma
		 *
		 * @param icao icao aerodroma
		 * @return the string
		 */
		private String dobaviAerodrom(String icao) {
			String adresaAeroServer = "";
			int portMeteoServer = 0;

			synchronized (konfig) {
				adresaAeroServer = this.konfig.dajPostavku("server.aerodroma.adresa");
				portMeteoServer = Integer.parseInt(this.konfig.dajPostavku("server.aerodroma.port"));
			}

			String odgovorAeroServer = posaljiKomandu(adresaAeroServer, portMeteoServer, "AIRPORT " + icao);

			return odgovorAeroServer;
		}

		/**
		 * Izvršavanje pretvorbe dobavljenog aerodroma u tip klase Aerodrom
		 *
		 * @param odgovor odgovor poslužitelja ServerAerodroma
		 * @return the aerodrom
		 */
		private Aerodrom izvrsiAerodromPretvorbu(String odgovor) {
			String polje[] = new String[5];
			Pattern uzorak = Pattern
					.compile("^(OK) ([A-Z]{4}) \"(.*?)\" (-?\\d{1,3}\\.\\d{1,20}) (-?\\d{1,3}\\.\\d{1,20})$");
			Matcher m = uzorak.matcher(odgovor);
			while (m.find()) {
				for (int j = 1; j <= m.groupCount(); j++) {
					polje[j - 1] = m.group(j);
				}
			}

			String icao = polje[1];
			String naziv = polje[2];
			String gpsGS = polje[3];
			String gpsGD = polje[4];

			Aerodrom aerodrom = new Aerodrom(icao, naziv, gpsGS, gpsGD);
			return aerodrom;
		}

		/**
		 * Ispisivanje greške.
		 *
		 * @param osw     izlazni tok podataka
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
					OutputStreamWriter osw = new OutputStreamWriter(veza.getOutputStream(),
							Charset.forName("UTF-8"));) {

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
		 * Ispis poruke na konzolu
		 *
		 * @param message poruka
		 */
		private void ispis(String message) {
			System.out.println(message);

		}
	}
}
