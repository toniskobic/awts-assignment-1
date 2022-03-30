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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
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

public class ServerAerodroma {
	int port;
	int maksCekaca;
	Socket veza = null;
	List<Aerodrom> aerodromi = new ArrayList<>();

	static public Konfiguracija konfig = null;

	public ServerAerodroma(int port, int maksCekaca) {
		super();
		this.port = port;
		this.maksCekaca = maksCekaca;
	}

	private static void ucitavanjePodataka(String nazivDatoteke) {
		try {
			konfig = KonfiguracijaApstraktna.preuzmiKonfiguraciju(nazivDatoteke);
		} catch (NeispravnaKonfiguracija e) {
			e.printStackTrace();
		}
	}

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

	public void obradaZahtjeva() {

		try (ServerSocket ss = new ServerSocket(this.port, this.maksCekaca)) {
			while (true) {
				System.out.println("Čekam korisnika."); // TODO kasnije obrisati
				this.veza = ss.accept();

				DretvaAerodroma dretvaAerodroma = new DretvaAerodroma(this, konfig, veza);
				dretvaAerodroma.start();
			}

		} catch (IOException ex) {
			Logger.getLogger(ServerGlavni.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

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
		String nazivDatotekeAerodromPodataka = konfig.dajPostavku("datoteka.aerodroma");

		ServerAerodroma sa = new ServerAerodroma(port, maksCekaca);
		sa.pripremiAerodrome(nazivDatotekeAerodromPodataka);
		System.out.println("Broj podataka: " + sa.aerodromi.size());
		sa.obradaZahtjeva();
	}

	private class DretvaAerodroma extends Thread {
		ServerAerodroma serverAerodroma = null;
		Konfiguracija konfig = null;
		Socket veza = null;
		String aero = "^AIRPORT$";
		String aeroIcao = "^AIRPORT ([A-Z]{4})$";
		String aeroIcaoUdaljenost = "^AIRPORT ([A-Z]{4}) (\\d{1,3})$";

		// TODO pogledati za naziv dretve
		public DretvaAerodroma(ServerAerodroma serverAerodroma, Konfiguracija konfig, Socket veza) {
			super();
			this.serverAerodroma = serverAerodroma;
			this.konfig = konfig;
			this.veza = veza;
		}

		@Override
		public synchronized void start() {
			// TODO Auto-generated method stub
			super.start();
		}

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
				System.out.println(tekst.toString()); // TODO kasnije obrisati
				this.veza.shutdownInput();

				if (provjeraSintakseObrada(tekst.toString(), aero)) {
					izvrsiAero(osw, tekst.toString());
				} else if (provjeraSintakseObrada(tekst.toString(), aeroIcao)) {
					// TODO metoda za icao
				} else {
					ispisGreske(osw, "ERROR 20 Sintaksa komande nije uredu.");
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		private boolean provjeraSintakseObrada(String komanda, String regularniIzraz) {
			Pattern izraz = Pattern.compile(regularniIzraz);
			Matcher rezultatUsporedbe = izraz.matcher(komanda);

			return rezultatUsporedbe.matches();
		}

		private void izvrsiAero(OutputStreamWriter osw, String komanda) {
			String odgovor = "OK ";

			for (Aerodrom aerodrom : aerodromi) {
				odgovor = odgovor + aerodrom.getIcao() + "; ";
			}
			try {
				osw.write(odgovor);
				osw.flush();
				osw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void ispisGreske(OutputStreamWriter osw, String odgovor) {
			try {
				osw.write(odgovor);
				osw.flush();
				osw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void interrupt() {
			// TODO Auto-generated method stub
			super.interrupt();
		}

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

		private void ispis(String message) {
			System.out.println(message);

		}
	}
}
