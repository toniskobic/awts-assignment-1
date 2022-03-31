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

public class ServerUdaljenosti {
	int port;
	int maksCekaca;
	Socket veza = null;

	static public Konfiguracija konfig = null;

	public ServerUdaljenosti(int port, int maksCekaca) {
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

	public void obradaZahtjeva() {

		try (ServerSocket ss = new ServerSocket(this.port, this.maksCekaca)) {
			while (true) {
				System.out.println("Čekam korisnika."); // TODO kasnije obrisati
				this.veza = ss.accept();

				DretvaUdaljenosti dretvaUdaljenosti = new DretvaUdaljenosti(this, konfig, veza);
				dretvaUdaljenosti.start();
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

		ServerUdaljenosti su = new ServerUdaljenosti(port, maksCekaca);
		su.obradaZahtjeva();
	}

	private class DretvaUdaljenosti extends Thread {
		ServerUdaljenosti serverUdaljenosti = null;
		volatile Konfiguracija konfig = null;
		Socket veza = null;
		static volatile List<Aerodrom> aerodromi = new ArrayList<>();

		String udaljenostIcao = "^DISTANCE ([A-Z]{4}) ([A-Z]{4})$";
		String udaljenostOcisti = "^DISTANCE CLEAR$";

		// TODO pogledati za naziv dretve
		public DretvaUdaljenosti(ServerUdaljenosti serverUdaljenosti, Konfiguracija konfig, Socket veza) {
			super();
			this.serverUdaljenosti = serverUdaljenosti;
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

		private boolean provjeraSintakseObrada(String komanda, String regularniIzraz) {
			Pattern izraz = Pattern.compile(regularniIzraz);
			Matcher rezultatUsporedbe = izraz.matcher(komanda);

			return rezultatUsporedbe.matches();
		}

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
					aerodrom1 = izvrsiAerodromPretvorbu(odgovorAeroServer);
					synchronized (aerodromi) {
						aerodromi.add(aerodrom1);
					}
				}
				if (aerodrom2 == null) {
					String odgovorAeroServer = dobaviAerodrom(icao2);
					aerodrom2 = izvrsiAerodromPretvorbu(odgovorAeroServer);
					synchronized (aerodromi) {
						aerodromi.add(aerodrom2);
					}
				}
			}
			double udaljenost = (double)udaljenost(Float.valueOf(aerodrom1.getGpsGS()), Float.valueOf(aerodrom1.getGpsGD()),
					Float.valueOf(aerodrom2.getGpsGS()), Float.valueOf(aerodrom2.getGpsGD()));

			odgovor = "OK " + (int) Math.round(udaljenost);

			try {
				osw.write(odgovor);
				osw.flush();
				osw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

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
