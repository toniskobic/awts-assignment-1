package org.foi.nwtis.tskobic.zadaca_1;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServerGlavniTest {

	ServerGlavni serverGlavni = null;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		serverGlavni = new ServerGlavni(8003, 10);
	}

	@AfterEach
	void tearDown() throws Exception {
		serverGlavni = null;
	}

	@Test
	void testMain() {
	}

	@Test
	@Order(2)
	void testPripremiKorisnici() {
		assertEquals(0, serverGlavni.korisnici.size());
		serverGlavni.pripremiKorisnici("korisnici.csv");
		assertNotEquals(0, serverGlavni.korisnici.size());
	}

	@Test
	@Order(1)
	void testUcitavanjePodataka() {
		assertNull(serverGlavni.konfig);
		serverGlavni.ucitavanjePodataka("NWTiS_tskobic_4.txt");
		assertNotNull(serverGlavni.konfig);
		assertNotEquals(0, serverGlavni.konfig.dajSvePostavke().size());
	}

	@Test
	void testServerGlavni() {
	}

	@Test
	@Order(3)
	void testObradaZahtjeva() {
		serverGlavni.ucitavanjePodataka("NWTiS_tskobic_4.txt");
		assertNotNull(serverGlavni.konfig);
		
		ObradaZahtjevaDretva ozd = new ObradaZahtjevaDretva();
		ozd.start();

		String adresa = "localhost";
		int port = 8003;
		String komanda = "METEO LZDA";
		String odgovor = null;

		try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

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
			odgovor = tekst.toString();
		} catch (SocketException e) {
			ispis(e.getMessage());
		} catch (IOException ex) {
			ispis(ex.getMessage());
		}
		ispis(odgovor);
		assertTrue(odgovor.startsWith("OK "));
	}

	private void ispis(String message) {
		System.out.println(message);

	}

	public class ObradaZahtjevaDretva extends Thread {

		@Override
		public void run() {
			serverGlavni.ucitavanjePodataka("NWTiS_tskobic_4.txt");
			serverGlavni.obradaZahtjeva();
		}

	}

}
