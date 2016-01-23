package blake.mkoi.server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

import blake.mkoi.server.crypto.Cipher;

public class Server implements Runnable {
	public static Charset charset = StandardCharsets.UTF_8;

//	String adresServ;
	private int portServ;
	private String klucz;
	private ServerSocket socketServ;
	private Boolean serwerIsRunning = false;
	private Vector<Obsluga> clients = new Vector<Obsluga>();
	private Cipher cipher;

	
	public Server(String adres, int port, String klucz)  {
		serwerIsRunning = false;
		this.klucz = klucz;
		
		try {
			socketServ = new ServerSocket(port);
			serwerIsRunning = true;
			cipher = new Cipher(klucz.getBytes(charset));
			
			System.out.println("Serwer stoi na "+socketServ.getInetAddress()+":"+socketServ.getLocalPort());
		} catch (IOException e) {
			System.out.println("Uruchomienie serwera niemożliwe.");
			e.printStackTrace();
		}
	}
	
	// nasłuchuje połączeń i rozpoczyna obsługę zgłoszeń
	@Override
	public void run() {
		Socket socketCl;
		Obsluga clientService;

		System.out.println("Serwer Runnable uruchomione.");
		
		while(true) {
			try {
				socketCl = socketServ.accept();
				clientService =  new Obsluga(socketCl, this, cipher);
				addClientService(clientService);

				System.out.println("Połączenie nawiązane z: "+socketCl.getInetAddress());

			} catch(IOException e) {
				System.err.println("Błąd nawiązania połączenia z nowym klientem.");
			}
		}		
	}
	
	private void addClientService(Obsluga clientService) throws IOException
	{
		clientService.init();
		clients.addElement(clientService);
		new Thread(clientService).start();
		System.out.println("Add. " + clients.size());
	}
	
	void removeClientService(Obsluga clientService)
	{
		clients.removeElement(clientService);
		System.out.println("Klient usunięty. Liczba zalogowanych: " + clients.size());
	}
	
}
