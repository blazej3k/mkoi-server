package blake.mkoi.server;

public class AppS {

	public static void main(String[] args) {
		String adres 	= "127.0.0.1";
		int port		= 20716;
		String klucz = "WlazlKotekNaPlot";

		Server serwer = new Server(adres, port, klucz);
		serwer.run();
		
		System.out.println("Ale jaja!");
		
	}
}
