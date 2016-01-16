package blake.mkoi.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
//	String adresServ;
	int portServ;
	ServerSocket socketServ;
	Boolean serwerIsRunning = false;
	
	public Server(String adres, int port) {
		serwerIsRunning = false;

		try {
			socketServ = new ServerSocket(port);
			serwerIsRunning = true;
			System.out.println("Serwer stoi na "+socketServ.getInetAddress()+":"+socketServ.getLocalPort());
		} catch (IOException e) {
			System.out.println("Uruchomienie serwera niemożliwe.");
			e.printStackTrace();
		}
		
		if (serwerIsRunning) {
			nasluchujPolaczen();
		}
	}
	
	private void nasluchujPolaczen() {
		while(serwerIsRunning) {
			try {
				Socket socketCl = socketServ.accept();
				System.out.println("Połączenie nawiązane z: "+socketCl.getInetAddress());
				// przy tej konstrucji wywoływania obsługi zadań, można obsłużyć maksymalnie jednego klienta
				// jakby tworzył nowy obiekt obsługi to nie powinno się blokować
				obsluzZadanie(socketCl);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void obsluzZadanie(Socket socketCl) {
		try {
			BufferedReader inText = new BufferedReader(new InputStreamReader(socketCl.getInputStream()));
			PrintWriter outText = new PrintWriter(socketCl.getOutputStream(), true);
			
			String response="";
			while ((response = inText.readLine()) != null) {
				// drugi argument = 2 sprawia, że max na 2 elementy będzie podzielone
				
				if(response!="bye") {
					String[] polecenie = response.split(" ");
					System.out.println(socketCl.getInetAddress()+": "+ response);
					outText.println("Wydałeś polecenie: "+ response);
				} else {
					outText.println("No joł ziomeczek.");
					inText.close();
					outText.close();
					socketCl.close();
				}
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
