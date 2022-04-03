package org.foi.nwtis.tskobic.zadaca_1;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Klasa za klijenta KorisnikGlavni.
 */
public class KorisnikGlavni {
	
	/** broj porta. */
	int port;
	
	/** maksimalno čekanje na odgovor poslužitelja. */
	int cekanje;
	
	/** komanda. */
	String komanda;
	
	/** adresa. */
	String adresa;

	/**
	 * Slanje komande poslužitelju.
	 *
	 * @param adresa adresa
	 * @param port broj porta
	 * @param cekanje maksimalno čekanje na odgovor poslužitelja
	 * @param komanda komanda
	 * @return the string
	 */
	public String posaljiKomandu(String adresa, int port, int cekanje, String komanda) {
		InputStreamReader isr = null;
		OutputStreamWriter osw = null;

		try (Socket veza = new Socket()) {
			InetSocketAddress isa = new InetSocketAddress(adresa, port);
			veza.connect(isa, cekanje);
			isr = new InputStreamReader(veza.getInputStream(), Charset.forName("UTF-8"));
			osw = new OutputStreamWriter(veza.getOutputStream(), Charset.forName("UTF-8"));

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
		} catch (IOException e) {
			ispis(e.getMessage());
		} finally {
			try {
				isr.close();
				osw.close();
			} catch (IOException e) {
				ispis(e.getMessage());
			}
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

	/**
	 * Pretvaranje aero komandi u format koji odgovara poslužitelju.
	 *
	 * @param komanda komanda
	 * @param args argumenti
	 * @return the string
	 */
	private String pretvoriAeroKomandu(String komanda, String[] args) {
		switch (args.length) {
		case 11: {
			komanda = komanda + " AIRPORT";
			break;
		}
		case 12: {
			komanda = komanda + " AIRPORT " + args[11];
			break;
		}
		case 14: {
			komanda = komanda + " AIRPORT " + args[11] + " " + args[13];
			break;
		}
		default:
			return null;
		}

		return komanda;
	}

	/**
	 * Pretvaranje meteo komandi u format koji odgovara poslužitelju.
	 *
	 * @param komanda komanda
	 * @param args argumenti
	 * @return the string
	 */
	private String pretvoriMeteoKomandu(String komanda, String[] args) {
		switch (args.length) {
		case 12: {
			komanda = komanda + " METEO " + args[11];
			break;
		}
		case 14: {
			komanda = komanda + " METEO " + args[11] + args[13];
			break;
		}
		default: {
			return null;
		}
		}

		return komanda;
	}

	/**
	 * Pretvaranje temp komandi u format koji odgovara poslužitelju.
	 *
	 * @param komanda komanda
	 * @param args argumenti
	 * @return the string
	 */
	private String pretvoriTempKomandu(String komanda, String[] args) {
		switch (args.length) {
		case 14: {
			komanda = komanda + " TEMP " + args[11] + " " + args[13];
			break;
		}
		case 16: {
			String datum = args[15];
			try {
				datum = pretvoriDatum(datum);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			komanda = komanda + " TEMP " + args[11] + " " + args[13] + datum;
			break;
		}
		default: {
			return null;
		}
		}

		return komanda;
	}

	/**
	 * Pretvaranje datuma u format koji odgovara poslužitelju.
	 *
	 * @param unos unos
	 * @return the string
	 * @throws ParseException the parse exception
	 */
	private String pretvoriDatum(String unos) throws ParseException {
		SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy");
		SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd");
		Date datum = format1.parse(unos);
		return format2.format(datum);
	}

	/**
	 * Pretvaranje udaljenost komandi i format koji odgovara poslužitelju.
	 *
	 * @param komanda komanda
	 * @param args argumenti
	 * @return the string
	 */
	private String pretvoriUdaljenostKomandu(String komanda, String[] args) {
		switch (args.length) {
		case 12: {
			if (args[11].equals("--isprazni")) {
				komanda = komanda + " DISTANCE CLEAR";
			}
			break;
		}
		case 15: {
			komanda = komanda + " DISTANCE " + args[12] + " " + args[14];
			break;
		}
		default: {
			return null;
		}
		}
		return komanda;
	}
	
	/**
	 * Provjera predmetnog dijela unesenih argumenata.
	 *
	 * @param komanda komanda
	 * @param args argumenti
	 * @return the string
	 */
	public String provjeraPredmeta (String komanda, String[] args) {
		if (args[10].equals("--aerodrom")) {
			komanda = pretvoriAeroKomandu(komanda, args);
		} else if (args[10].equals("--meteo")) {
			komanda = pretvoriMeteoKomandu(komanda, args);
		} else if (args[10].equals("--tempOd")) {
			komanda = pretvoriTempKomandu(komanda, args);
		} else if (args[10].equals("--udaljenost")) {
			komanda = pretvoriUdaljenostKomandu(komanda, args);
		} else if (args[10].equals("--spremi") && args.length == 11) {
			komanda = komanda + " CACHE BACKUP";
		} else if (args[10].equals("--vrati") && args.length == 11) {
			komanda = komanda + " CACHE RESTORE";
		} else if (args[10].equals("--isprazni") && args.length == 11) {
			komanda = komanda + " CACHE CLEAR";
		} else if (args[10].equals("--statistika") && args.length == 11) {
			komanda = komanda + " CACHE STAT";
		} else {
			komanda = null;
		}
		return komanda;
	}

	/**
	 * Glavna metoda
	 *
	 * @param args argumenti
	 */
	public static void main(String[] args) {
		if (args.length < 11) {
			System.out.println("Nije dovoljan broj argumenata.");
			return;
		}
		if (args.length > 16) {
			System.out.println("Previše unesenih argumenata.");
			return;
		}

		KorisnikGlavni kg = new KorisnikGlavni();

		if (args[0].equals("-k") && args[2].equals("-l") && args[4].equals("-s") && args[6].equals("-p")
				&& args[6].equals("-p") && args[8].equals("-t")) {
			kg.komanda = "USER " + args[1] + " PASSWORD " + args[3];
			kg.adresa = args[5];

			try {
				kg.port = Integer.valueOf(args[7]);
				kg.cekanje = Integer.valueOf(args[9]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}

			if (kg.port < 8000 || kg.port > 9999) {
				System.out.println("Neispravna vrijednost porta.");
				return;
			}

			kg.komanda = kg.provjeraPredmeta(kg.komanda, args);
			
			if (kg.komanda == null) {
				System.out.println("Neispravan unos argumenata.");
				return;
			}

		}

		String odgovor = kg.posaljiKomandu(kg.adresa, kg.port, kg.cekanje, kg.komanda);
		kg.ispis(odgovor);
	}
}
