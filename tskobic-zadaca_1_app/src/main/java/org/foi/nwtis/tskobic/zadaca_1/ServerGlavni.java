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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.foi.nwtis.tskobic.vjezba_03.konfiguracije.Konfiguracija;
import org.foi.nwtis.tskobic.vjezba_03.konfiguracije.KonfiguracijaApstraktna;
import org.foi.nwtis.tskobic.vjezba_03.konfiguracije.NeispravnaKonfiguracija;

public class ServerGlavni {
	int port;
	int maksCekaca;
	Socket veza = null;
	List<Korisnik> korisnici = new ArrayList<>();
	String meteoIcao = "^METEO ([A-Z]{4})$";
	String meteoIcaoDatum = "^METEO ([A-Z]{4}) (\\d{4}-\\d{2}-\\d{2})$";

	static public Konfiguracija konfig = null;
	static SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

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
		String nazivDatotekeKorisnika = konfig.dajPostavku("datoteka.korisnika");

		ServerGlavni sm = new ServerGlavni(port, maksCekaca);
		sm.pripremiKorisnici(nazivDatotekeKorisnika);
		System.out.println("Broj podataka: " + sm.korisnici.size());
		sm.obradaZahtjeva();

	}

	private void pripremiKorisnici(String nazivDatotekeKorisnika) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(nazivDatotekeKorisnika, Charset.forName("UTF-8")));
			while (true) {
				String linija = br.readLine();
				if (linija == null || linija.isEmpty()) {
					break;
				}
				String[] p = linija.split(";");
				Korisnik k;
				k = new Korisnik(p[0], p[1], p[2], p[3]);
				korisnici.add(k);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void ucitavanjePodataka(String nazivDatoteke) {
		try {
			konfig = KonfiguracijaApstraktna.preuzmiKonfiguraciju(nazivDatoteke);
		} catch (NeispravnaKonfiguracija e) {
			e.printStackTrace();
		}
	}

	public ServerGlavni(int port, int maksCekaca) {
		super();
		this.port = port;
		this.maksCekaca = maksCekaca;
	}

	public void obradaZahtjeva() {

		try (ServerSocket ss = new ServerSocket(this.port, this.maksCekaca)) {
			while (true) {
				System.out.println("Čekam korisnika."); // TODO kasnije obrisati
				this.veza = ss.accept();
				
				DretvaZahtjeva dretvaZahtjeva = new DretvaZahtjeva(this, konfig, veza);
				dretvaZahtjeva.start();
			}

		} catch (IOException ex) {
			Logger.getLogger(ServerGlavni.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

}
