package org.foi.nwtis.tskobic.zadaca_1;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.foi.nwtis.tskobic.vjezba_03.konfiguracije.Konfiguracija;

public class DretvaZahtjeva extends Thread {
	ServerGlavni serverGlavni = null;
	Konfiguracija konfig = null;
	Socket veza = null;
	String meteoIcao = "^METEO ([A-Z]{4})$";
	String meteoIcaoDatum = "^METEO ([A-Z]{4}) (\\d{4}-\\d{2}-\\d{2})$";

	
	// TODO pogledati za naziv dretve
	public DretvaZahtjeva(ServerGlavni serverGlavni, Konfiguracija konfig, Socket veza) {
		super();
		this.serverGlavni = serverGlavni;
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

			// TODO prepoznati komande
			Pattern pMeteoIcao = Pattern.compile(meteoIcao);
			Pattern pMeteoIcaoDatum = Pattern.compile(meteoIcaoDatum);
			Matcher mMeteoIcao = pMeteoIcao.matcher(tekst.toString());
			Matcher mMeteoIcaoDatum = pMeteoIcaoDatum.matcher(tekst.toString());

			if (mMeteoIcao.matches()) {
				izvrsiMeteoIcao(osw, tekst.toString());
			} else if (mMeteoIcaoDatum.matches()) {
				// TODO metoda za icao i datum
			} else {
				krivaKomanda(osw, "ERROR 10 Sintaksa komande nije uredu.");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void izvrsiMeteoIcao(OutputStreamWriter osw, String komanda) {
		String p[] = komanda.split(" ");
		String icao = p[1];
		String adresaMeteoServer = this.konfig.dajPostavku("server.meteo.adresa");
		int portMeteoServer = Integer.parseInt(this.konfig.dajPostavku("server.meteo.port"));
		String odgovor = this.posaljiKomandu(adresaMeteoServer, portMeteoServer, komanda);
		try {
			osw.write(odgovor);
			osw.flush();
		} catch (IOException e) {		
			e.printStackTrace();
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

	@Override
	public void interrupt() {
		// TODO Auto-generated method stub
		super.interrupt();
	}

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

	private void ispis(String message) {
		System.out.println(message);
		
	}
	
}
