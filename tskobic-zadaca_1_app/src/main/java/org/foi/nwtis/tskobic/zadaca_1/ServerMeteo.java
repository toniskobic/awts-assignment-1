package org.foi.nwtis.tskobic.zadaca_1;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerMeteo {
	int port;
	int maksCekaca;
	Socket veza = null;

	public static void main(String[] args) {
		// TODO UÄŤitaj postavke u datoteke konfiguracije
		
		int port = 8000;
		int maksCekaca = 10;
		
		ServerMeteo sm = new ServerMeteo(port, maksCekaca);
		sm.obradaZahtjeva();
		
	}

	public ServerMeteo(int port, int maksCekaca) {
		super();
		this.port = port;
		this.maksCekaca = maksCekaca;
	}

	public void obradaZahtjeva() {

		try (ServerSocket ss = new ServerSocket(this.port, this.maksCekaca)) {
			while (true) {
				System.out.println("ÄŚekam korisnika."); // TODO kasnije obrisati
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
//TODO provjera sintakse, obrada komande i slanje odgovora

				} catch (SocketException e) {
					e.printStackTrace();
				}
			}

		} catch (IOException ex) {
			Logger.getLogger(ServerMeteo.class.getName()).log(Level.SEVERE, null, ex);
		}

	}
}
