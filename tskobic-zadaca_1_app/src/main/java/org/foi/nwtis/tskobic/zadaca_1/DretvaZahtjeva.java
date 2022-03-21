package org.foi.nwtis.tskobic.zadaca_1;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.foi.nwtis.tskobic.vjezba_03.konfiguracije.Konfiguracija;

public class DretvaZahtjeva extends Thread {
	Konfiguracija konfig = null;
	Socket veza = null;
	String meteoIcao = "^METEO ([A-Z]{4})$";
	String meteoIcaoDatum = "^METEO ([A-Z]{4}) (\\d{4}-\\d{2}-\\d{2})$";

	
	// TODO pogledati za naziv dretve
	public DretvaZahtjeva(Konfiguracija konfig, Socket veza) {
		super();
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

	private void krivaKomanda(OutputStreamWriter osw, String string) {
		// TODO Auto-generated method stub
		
	}

	private void izvrsiMeteoIcao(OutputStreamWriter osw, String string) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void interrupt() {
		// TODO Auto-generated method stub
		super.interrupt();
	}

}
