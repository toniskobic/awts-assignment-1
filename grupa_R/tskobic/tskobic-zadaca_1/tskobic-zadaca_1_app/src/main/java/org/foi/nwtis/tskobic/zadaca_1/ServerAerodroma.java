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
 * Glavna klasa poslužitelja ServerAerodrom
 */
public class ServerAerodroma {
	
	/** broj porta. */
	int port;
	
	/** maksimalni broj čekača. */
	int maksCekaca;
	
	/** maksimalno čekanje na spajanje klijenta. */
	int maksCekanje;
	
	/** veza. */
	Socket veza = null;
	
	/** Kolekcija aerodroma. */
	volatile List<Aerodrom> aerodromi = new ArrayList<>();

	/** Konfiguracijski podaci. */
	static public Konfiguracija konfig = null;

	/**
	 * Konstruktor klase.
	 *
	 * @param port broj   porta
	 * @param maksCekaca  maksimalan broj čekača
	 * @param maksCekanje maksimalno čekanje na spajanje klijenta
	 */
	public ServerAerodroma(int port, int maksCekaca, int maksCekanje) {
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
	 * Pripremi (učitavanje) aerodroma
	 *
	 * @param nazivDatotekeAerodromPodataka naziv datoteke aerodrom podataka
	 */
	private void pripremiAerodrome(String nazivDatotekeAerodromPodataka) {
		try {
			BufferedReader br = new BufferedReader(
					new FileReader(nazivDatotekeAerodromPodataka, Charset.forName("UTF-8")));
			while (true) {
				String linija = br.readLine();
				if (linija == null || linija.isEmpty()) {
					break;
				}
				String[] p = linija.split(";");
				Aerodrom aero;
				aero = new Aerodrom(p[0], p[1], p[2], p[3]);
				aerodromi.add(aero);
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
			ss.setSoTimeout(maksCekanje);
			while (true) {
				this.veza = ss.accept();

				DretvaAerodroma dretvaAerodroma = new DretvaAerodroma(konfig, veza);
				dretvaAerodroma.start();
			}

		} catch (IOException ex) {
			Logger.getLogger(ServerGlavni.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	/**
	 * Glavna metoda
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
		String nazivDatotekeAerodromPodataka = konfig.dajPostavku("datoteka.aerodroma");

		ServerAerodroma sa = new ServerAerodroma(port, maksCekaca, maksCekanje);
		sa.pripremiAerodrome(nazivDatotekeAerodromPodataka);
		System.out.println("Broj podataka: " + sa.aerodromi.size());
		sa.obradaZahtjeva();
	}

	/**
	 * Klasa dretva DretvaAerodroma.
	 */
	private class DretvaAerodroma extends Thread {
		
		/** konfiguracijski podaci. */
		volatile Konfiguracija konfig = null;
		
		/** veza. */
		Socket veza = null;
		
		/** dozvoljeni izraz za naredbu aero. */
		String aero = "^AIRPORT$";
		
		/** dozvoljeni izraz za naredbu aero. icao. */
		String aeroIcao = "^AIRPORT ([A-Z]{4})$";
		
		/** dozvoljeni izraz za naredbu aero icao udaljenost. */
		String aeroIcaoUdaljenost = "^AIRPORT ([A-Z]{4}) (\\d{1,5})$";

		/**
		 * Konstruktor klase
		 *
		 * @param konfig konfiguracijski podaci
		 * @param veza veza
		 */
		public DretvaAerodroma(Konfiguracija konfig, Socket veza) {
			super();
			this.konfig = konfig;
			this.veza = veza;
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

				if (provjeraSintakseObrada(tekst.toString(), aero)) {
					izvrsiAero(osw, tekst.toString());
				} else if (provjeraSintakseObrada(tekst.toString(), aeroIcao)) {
					izvrsiAeroIcao(osw, tekst.toString());
				} else if (provjeraSintakseObrada(tekst.toString(), aeroIcaoUdaljenost)) {
					izvrsiAeroIcaoUdaljenost(osw, tekst.toString());
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
		 * @param komanda komanda
		 * @param regularniIzraz dozvoljeni izraz
		 * @return true, ako uspješno
		 */
		private boolean provjeraSintakseObrada(String komanda, String regularniIzraz) {
			Pattern izraz = Pattern.compile(regularniIzraz);
			Matcher rezultatUsporedbe = izraz.matcher(komanda);

			return rezultatUsporedbe.matches();
		}

		/**
		 * Izvršavanje naredbe aero
		 *
		 * @param osw izlazni tok podataka
		 * @param komanda komanda
		 */
		private void izvrsiAero(OutputStreamWriter osw, String komanda) {
			String odgovor = "OK ";
			synchronized (aerodromi) {
				if (aerodromi.isEmpty()) {
					ispisGreske(osw, "ERROR 21 Interna kolekcija aerodroma je prazna.");
				}
				for (Aerodrom aerodrom : aerodromi) {
					odgovor = odgovor + aerodrom.getIcao() + "; ";
				}
			}

			try {
				osw.write(odgovor);
				osw.flush();
				osw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Izvrsšavanje naredbe aero icao
		 *
		 * @param osw izlazni tok podataka
		 * @param komanda komanda
		 */
		private void izvrsiAeroIcao(OutputStreamWriter osw, String komanda) {
			String p[] = komanda.split(" ");
			String icao = p[1];

			String odgovor = "";

			Aerodrom aerodrom = null;

			synchronized (aerodromi) {
				aerodrom = aerodromi.stream().filter(a -> a.getIcao().equals(icao)).findAny().orElse(null);
			}

			if (aerodrom == null) {
				ispisGreske(osw, "ERROR 21 Aerodrom '" + icao + "' ne postoji.");
			} else {
				odgovor = "OK " + aerodrom.getIcao() + " \"" + aerodrom.getNaziv() + "\" " + aerodrom.getGpsGS() + " "
						+ aerodrom.getGpsGD();

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
		 * Izvršavanje naredbe aero icao udaljenost.
		 *
		 * @param osw izlazni tok podataka
		 * @param komanda komanda
		 */
		private void izvrsiAeroIcaoUdaljenost(OutputStreamWriter osw, String komanda) {
			String p[] = komanda.split(" ");
			String icao = p[1];
			double maksUdaljenost = Double.valueOf(p[2]);

			String odgovor = "";

			Aerodrom aerodrom = null;

			synchronized (aerodromi) {
				aerodrom = aerodromi.stream().filter(a -> a.getIcao().equals(icao)).findAny().orElse(null);
			}

			if (aerodrom == null) {
				ispisGreske(osw, "ERROR 21 Aerodrom '" + icao + "' ne postoji.");
			} else {
				String adresaServerUdaljenosti = "";
				int portServerUdaljenosti = 0;
				synchronized (konfig) {
					adresaServerUdaljenosti = this.konfig.dajPostavku("server.udaljenosti.adresa");
					portServerUdaljenosti = Integer.parseInt(this.konfig.dajPostavku("server.udaljenosti.port"));
				}
				odgovor = "OK";
				for (Aerodrom am : aerodromi) {
					String o = posaljiKomandu(adresaServerUdaljenosti, portServerUdaljenosti,
							"DISTANCE " + icao + " " + am.getIcao());
					String polje[] = o.split(" ");
					int udaljenost = Integer.parseInt(polje[1]);
					if (udaljenost < maksUdaljenost && udaljenost > 0) {
						odgovor = odgovor + " " + am.getIcao() + " " + udaljenost + ";";
					}
				}
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
		 * Metoda za prekidanje rada dretve.
		 */
		@Override
		public void interrupt() {
			super.interrupt();
		}

		/**
		 * Slanje komande serveru
		 *
		 * @param adresa adresa servera
		 * @param port port servera
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
		 * Ispisivanje poruke na konzolu.
		 *
		 * @param message poruka
		 */
		private void ispis(String message) {
			System.out.println(message);

		}
	}
}
