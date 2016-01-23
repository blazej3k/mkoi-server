package blake.mkoi.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.StringTokenizer;

import blake.mkoi.server.crypto.Cipher;

public class Obsluga implements Runnable {

	private int id;
	private String login="";
	private boolean loggedin=false;
	private Server server;
	private Socket socketCl;
	DataInputStream dis;
	DataOutputStream dos;
	private Cipher cipher;
	private Charset charset = Server.charset;

	public Obsluga(Socket socketCl, Server server, Cipher cipher) {
		this.server = server;
		this.socketCl = socketCl;
		this.cipher=cipher;
		id = socketCl.getPort();
	}

    public void run()
    {
        loggedin = false;
        boolean obsluzony=false;
        while(!obsluzony)
        {
        	String request = receiveText();
        	StringTokenizer st = new StringTokenizer(request);
        	String command = st.nextToken();
        	System.out.println("Komenda w serwerze: " + command + " od klienta " + id);

        	if(!loggedin) {
        		switch (command) {
        		case Protokol.LOGIN: {
        			if(st.hasMoreTokens()) {
        				login = st.nextToken();
        				sendMessage(Protokol.LOGGEDIN);
        				loggedin = true;
        			}
        			break;
        		}
        		case Protokol.BYE: {
        			System.out.println("Było BYE");
        			sendMessage("No joł ziomeczek.");

        			loggedin=false;
        			obsluzony=true;

        			break;
        		}
        			
        		case Protokol.ERROR: {
        			System.out.println("Klient zgłasza błąd: "+ st.nextToken());
        			obsluzony=true;
        			break;
        		}
        		default: {
        			sendMessage(Protokol.ERROR+ " "+Protokol.LOGIN);
        		}

        		}
        	}
        	//po pomyślnym zalogowaniu
        	else if(loggedin)
        	{
        		switch (command) {
        		case Protokol.SEND_PRE: {
        			System.out.println("Bede odbierac.");
        			
        			int rozmiar = Integer.parseInt(st.nextToken());
        			String nazwa = st.nextToken();
        			
        			sendMessage(Protokol.SEND_ACC);
        			receiveFile(rozmiar, nazwa);
        			
        			break;
        		}
        		case Protokol.GET_PRE: {
        			String nazwa=st.nextToken();
        			File plik = new File("./"+nazwa);
        			
//        		    while ((c = in.read()) != -1) out.write(c);
        			
        			if (plik.exists()) {
        				sendMessage(Protokol.SEND_PRE +" "+plik.length() +" "+plik.getName());
        				String message=receiveText();
        				if (message.equals(Protokol.SEND_ACC)) {
        					sendFile(plik);
        				}
        			}
        			break;
        		}
        		case Protokol.BYE: {
        			System.out.println("Było BYE");
        			sendMessage("No joł ziomeczek.");

        			obsluzony=true;
        			break;
        		}
        		default: {
        			sendMessage(Protokol.ERROR+ " "+request);
        		}
        		}
        	}
        }
        close();
        server.removeClientService(this);
    }

    
    private String receiveText() {
    	byte[] bufor		= new byte[64];
    	byte[] received;
    	int ile = 0;
    	byte[] decrypted 	= new byte[64];
    	byte[] decryptedBlok = new byte[16];
      	byte[] blok = new byte[16];
      	
      	Arrays.fill(decrypted, (byte) 0);
      	
        try {
        	ile = dis.read(bufor);
        	System.out.println("Liczba odebranych: "+ile);
        	received = Arrays.copyOfRange(bufor, 0, ile);
        	System.out.println("Odebrano ciąg: "+Integer.toBinaryString(received[0]));
        }
        catch(IOException e) {
            System.err.println("Błąd IO odczytu poleceń klienta: "+id);
            return Protokol.ERROR;
        }
        catch(IllegalArgumentException e) {
            System.err.println("Błąd IllegalArgumentException odczytu poleceń klienta: "+id);
            return Protokol.ERROR;
        }

        for (int i=0; i<received.length; i+=16) {
        	System.arraycopy(received, i, blok, 0, blok.length);
        	decryptedBlok = cipher.Decrypt(blok);
        	System.arraycopy(decryptedBlok, 0, decrypted, i, decryptedBlok.length);
        }
       System.out.println(id+": "+ new String(decrypted, charset)); 
       return new String(decrypted, charset).trim();   
    }
    
    // receiveFile SERWERA nie odszyfrowywuje plików
    // przechowuje je zaszyfrowane
    private String receiveFile(int rozmiar, String nazwa) {
    	File folder = new File(".");
    	File plik = new File(folder.getParentFile(), nazwa);
    	DataOutputStream plikStrumienWy = null;

        try {
        	plik.createNewFile();
        	plikStrumienWy = new DataOutputStream(new FileOutputStream(plik));
        	
        } catch (Exception e) {
        	System.out.println("Błąd IO otwarcia pliku do zapisu lub otwarcia strumienia. "+nazwa);
        	e.printStackTrace();
        }
        
        try {
        	int c;
        	int odebrano=0;
        	byte[] bufor = new byte[16384];
            while ((c = dis.read(bufor)) != -1) {
            	plikStrumienWy.write(bufor);
            	odebrano+=c;
				System.out.println("Odebrałem: "+odebrano+" bajtów"+" rozmiar pliku: "+plik.length());
			}
			System.out.println("Wyszłem.");
            System.out.println("Deklarowany rozmiar: "+rozmiar+", odebrano: "+plik.length()+", odebrano ze strumienia="+odebrano);
            plikStrumienWy.close();
        }
        catch(IOException e) {
            System.err.println("Błąd IO odczytu/zapisu strumienia danych: "+id);
            return Protokol.ERROR;
        }
        catch(IllegalArgumentException e) {
            System.err.println("Błąd IllegalArgumentException odczytu/zapisu strumienia danych: "+id);
            return Protokol.ERROR;
        }

        System.out.println("Utworzono plik: "+plik.getName()+" o rozmiarze: "+plik.length());

       return new String("x");   
    }
   

    
    void send(byte[] wejscie, boolean ifText) {
		byte[] blok = new byte[16];
		byte[] encryptedBlok = new byte[16];
		byte[] encryptedTotal=null;
		if (wejscie.length%16!=0) {
			encryptedTotal = new byte[wejscie.length + (16 - wejscie.length%16)];
		}
		else {
			encryptedTotal = new byte[wejscie.length];
		}
    	Arrays.fill(encryptedTotal, (byte) 0);
    	byte[] wejsciePad = new byte[encryptedTotal.length];
    	wejsciePad = Arrays.copyOf(wejscie, wejsciePad.length);
    	
    	for (int i=0; i<wejsciePad.length; i+=16) {
    		System.arraycopy(wejsciePad, i, blok, 0, blok.length);
    		encryptedBlok = cipher.Encrypt(blok);
    		System.arraycopy(encryptedBlok, 0, encryptedTotal, i, encryptedBlok.length);
    	}
    	
    	try {
    		System.out.println(">"+id+": "+new String(wejscie, charset));
			dos.write(encryptedTotal);
//			dos.flush();
		} catch (IOException e) {
			System.out.println("Błąd wysyłania tablicy bajtów.");
			e.printStackTrace();
		}
    }
    
	private void sendMessage(String message) {
		send(message.getBytes(charset), true);
	}
	
	// sendFile serwera NIE ODSZYFROWYWUJE plikow, wysyła takie jakie ma
	// tymczasowo wysyła pliki sam, nie używa send()
	private void sendFile(File plik) {
		try {
			if (plik.exists()) {
				DataInputStream plikStrumien = new DataInputStream(new FileInputStream(plik));
				System.out.println("Rozmiar pliku: "+plikStrumien.available()+" bajtów.");		
				
				int ileB=0;
				byte[] bufor = new byte[8192];
				while((ileB = plikStrumien.read(bufor))!=-1) {
//					send(bufor, false);
					dos.write(bufor);
					dos.flush();
				}
				plikStrumien.close();			
			}
			else {
				System.out.println("Plik nie istnieje.");
			}

		} catch(FileNotFoundException e){
			System.out.println("Nie znaleziono pliku");
		} catch(IOException e){
			System.out.println("Błąd wejścia-wyjścia");
		}
	}
    
    void init() throws IOException
    {     
        dis = new DataInputStream(socketCl.getInputStream());
        dos = new DataOutputStream(socketCl.getOutputStream());
    }
    
    void close() {
    	System.out.println("Zamykam połączenie z: "+id+" "+login);
        try {
        	dis.close();
        	dos.close();
            socketCl.close();
        } catch(IOException | NullPointerException e) {
            System.err.println("Error closing client (" + id + ").");  
        } finally {
        	dis = null;
        	dos = null;
            socketCl = null;
        }
    }
}
