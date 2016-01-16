package blake.mkoi.server;

public class AppS {

	public static void main(String[] args) {
		String adres 	= "127.0.0.1";
		int port		= 20716;		

		new Server(adres, port);
		
		System.out.println("Ale jaja!");
	}
}
