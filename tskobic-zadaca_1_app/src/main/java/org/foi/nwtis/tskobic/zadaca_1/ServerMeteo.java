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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.foi.nwtis.tskobic.vjezba_03.konfiguracije.Konfiguracija;
import org.foi.nwtis.tskobic.vjezba_03.konfiguracije.KonfiguracijaApstraktna;
import org.foi.nwtis.tskobic.vjezba_03.konfiguracije.NeispravnaKonfiguracija;

public class ServerMeteo {
	int port;
	int maksCekaca;
	Socket veza = null;
	List<AerodromMeteo> aerodromiMeteo = new ArrayList<>();

	String meteoIcao = "^METEO ([A-Z]{4})$";
	String meteoIcaoDatum = "^METEO ([A-Z]{4}) (\\d{4}-\\d{2}-\\d{2})$";
	String meteoTemp = "^TEMP (\\d{1},\\d{1}) (\\d{1},\\d{1})$";
	String meteoTempDatum = "^TEMP (\\d{1},\\d{1}) (\\d{1},\\d{1}) (\\d{4}-\\d{2}-\\d{2})$";

	static public Konfiguracija konfig = null;
	static SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public ServerMeteo(int port, int maksCekaca) {
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

	public void obradaZahtjeva() {

		try (ServerSocket ss = new ServerSocket(this.port, this.maksCekaca)) {
			while (true) {
				System.out.println("Čekam korisnika."); // TODO kasnije obrisati
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
					System.out.println(tekst.toString()); // TODO kasnije obrisati
					this.veza.shutdownInput();

					// TODO prepoznati komande
					if (provjeraSintakseObrada(tekst.toString(), meteoIcao)) {
						izvrsiMeteoIcao(osw, tekst.toString());
					} else if (provjeraSintakseObrada(tekst.toString(), meteoIcaoDatum)) {
						izvrsiMeteoIcaoDatum(osw, tekst.toString());
					} else if (provjeraSintakseObrada(tekst.toString(), meteoTemp)) {
						// TODO metoda za temp
					} else if (provjeraSintakseObrada(tekst.toString(), meteoTempDatum)) {
						// TODO metoda za temp i datum
					} else {
						krivaKomanda(osw, "ERROR 10 Sintaksa komande nije uredu.");
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

	private boolean provjeraSintakseObrada(String komanda, String regularniIzraz) {
		Pattern izraz = Pattern.compile(regularniIzraz);
		Matcher rezultatUsporedbe = izraz.matcher(komanda);

		return rezultatUsporedbe.matches();
	}

	private void izvrsiMeteoIcao(OutputStreamWriter osw, String komanda) {
		String p[] = komanda.split(" ");
		String icao = p[1];
		String odgovor = null;

		List<AerodromMeteo> fAerodromiMeteo = aerodromiMeteo.stream().filter(c -> c.getIcao().equals(icao))
				.collect(Collectors.toList());
		if (fAerodromiMeteo.isEmpty()) {
			krivaKomanda(osw, "ERROR 11 Aerodrom '" + icao + "' ne postoji.");
		} else {
			AerodromMeteo am = fAerodromiMeteo.stream().max(Comparator.comparing(AerodromMeteo::getTime))
					.orElseThrow(NoSuchElementException::new);
			odgovor = "OK " + am.temp + " " + am.vlaga + " " + am.tlak + " " + am.vrijeme + ";";
			try {
				osw.write(odgovor);
				osw.flush();
				osw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void izvrsiMeteoIcaoDatum(OutputStreamWriter osw, String komanda) {
		String p[] = komanda.split(" ");
		String icao = p[1];
		String datum = p[2] + " 00:00:00.00";
		String odgovor = "";

		List<AerodromMeteo> fAerodromiMeteo = aerodromiMeteo.stream().filter(c -> c.getIcao().equals(icao))
				.collect(Collectors.toList());
		if (fAerodromiMeteo.isEmpty()) {
			krivaKomanda(osw, "ERROR 11 Aerodrom '" + icao + "' ne postoji.");
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

	private void krivaKomanda(OutputStreamWriter osw, String odgovor) {
		try {
			osw.write(odgovor);
			osw.flush();
			osw.close();
		} catch (IOException e) {
			e.printStackTrace();
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
		String nazivDatotekeMeteoPodataka = konfig.dajPostavku("datoteka.meteo");

		ServerMeteo sm = new ServerMeteo(port, maksCekaca);
		sm.pripremiMeteo(nazivDatotekeMeteoPodataka);
		System.out.println("Broj podataka: " + sm.aerodromiMeteo.size());
		sm.obradaZahtjeva();

	}
}
