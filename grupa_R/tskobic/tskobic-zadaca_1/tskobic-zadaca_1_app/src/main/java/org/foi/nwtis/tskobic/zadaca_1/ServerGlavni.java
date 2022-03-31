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

/**
 * Glavna klasa poslužitelja ServerGlavni.
 */
public class ServerGlavni {

	/** broj porta. */
	int port;

	/** maksimalni broj čekača. */
	int maksCekaca;

	/** veza. */
	Socket veza = null;

	/** kolekcija korisnika. */
	List<Korisnik> korisnici = new ArrayList<>();

	/** konfiguracijski podaci. */
	public Konfiguracija konfig = null;

	/** iso format za datum. */
	static SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	/**
	 * Konstruktor klase.
	 *
	 * @param port       broj porta
	 * @param maksCekaca maksimalan broj čekača
	 */
	public ServerGlavni(int port, int maksCekaca) {
		super();
		this.port = port;
		this.maksCekaca = maksCekaca;
	}

	/**
	 * Učitavanje konfiguracijskih podataka.
	 *
	 * @param nazivDatoteke naziv datoteke konfiguracijskih podataka
	 */
	public void ucitavanjePodataka(String nazivDatoteke) {
		try {
			this.konfig = KonfiguracijaApstraktna.preuzmiKonfiguraciju(nazivDatoteke);
		} catch (NeispravnaKonfiguracija e) {
			e.printStackTrace();
		}
	}

	/**
	 * Priprema (učitavanje) korisnika.
	 *
	 * @param nazivDatotekeKorisnika naziv datoteke korisnika
	 */
	public void pripremiKorisnici(String nazivDatotekeKorisnika) {
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

	/**
	 * Obrada zahtjeva.
	 */
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
		ServerGlavni sg = new ServerGlavni(0, 0);

		sg.ucitavanjePodataka(args[0]);

		if (sg.konfig == null) {
			System.out.println("Problem s konfiguracijom.");
			return;
		}

		int port = Integer.parseInt(sg.konfig.dajPostavku("port"));
		int maksCekaca = Integer.parseInt(sg.konfig.dajPostavku("maks.cekaca"));
		String nazivDatotekeKorisnika = sg.konfig.dajPostavku("datoteka.korisnika");

		sg.port = port;
		sg.maksCekaca = maksCekaca;

		sg.pripremiKorisnici(nazivDatotekeKorisnika);
		System.out.println("Broj podataka: " + sg.korisnici.size());
		sg.obradaZahtjeva();
	}
}
