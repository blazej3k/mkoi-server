package blake.mkoi.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.StringTokenizer;

import blake.mkoi.server.crypto.Cipher;

public class Obsluga implements Runnable {

	private int id;
	private String login="";
	private boolean loggedin=false;
	private Server server;
	private Socket socketCl;
	private BufferedReader inText;
	private PrintWriter outText;
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

    void init() throws IOException
    {
        Reader reader = new InputStreamReader(socketCl.getInputStream());
        inText         = new BufferedReader(reader);
        outText        = new PrintWriter(socketCl.getOutputStream(), true);
        
        dis = new DataInputStream(socketCl.getInputStream());
        dos = new DataOutputStream(socketCl.getOutputStream());
    }
    
    void close() {
        try {
        	outText.close();
        	inText.close();
            socketCl.close();
        } catch(IOException e) {
            System.err.println("Error closing client (" + id + ").");  
        } finally {
        	outText = null;
        	inText = null;
            socketCl = null;
        }
    }
    
    private String receive() {
    	byte[] encrypted = new byte[16];
    	String plain="";

        try {
        	dis.readFully(encrypted);
        	plain = new String(cipher.Decrypt(encrypted), charset);
            return plain;
        }
        catch(IOException e) {
            System.err.println("Błąd odczytu poleceń klienta: "+id);
        }
        return Protokol.ERROR;
    }
   
    void send(String command) {
    	byte[] encrypted = cipher.Encrypt(command.getBytes(charset));
    	try {
			dos.write(encrypted);
			dos.flush();
		} catch (IOException e) {
			System.out.println("Błąd wysyłania tablicy bajtów.");
			e.printStackTrace();
		}
    }
    
    public void run()
    {
        loggedin = false;
        boolean obsluzony=false;
        while(!obsluzony)
        {
        	String request = receive();
        	StringTokenizer st = new StringTokenizer(request);
        	String command = st.nextToken();
        	System.out.println("Komenda w serwerze:" + command + " od klienta " + id);

        	//logowanie
        	if(command.equals(Protokol.LOGIN))
        	{
        		login = st.nextToken();
        		send(Protokol.LOGGEDIN);
        		loggedin = true;
        	}
        	else if (command.equals(Protokol.BYE)) 
        	{
    			System.out.println("Było BYE");
    			send("No joł ziomeczek.");
    			
    			loggedin=false;
    			obsluzony=true;
        	}
        	
        	

        	//po pomyúlnym zalogowaniu
        	if(loggedin)
        	{
        		switch (command) {
        		case Protokol.BYE: {
        			System.out.println("Było BYE");
        			send("No joł ziomeczek.");
        			
        			obsluzony=true;
        			break;
        		}

        		}
        	}
        }
        close();
        server.removeClientService(this);
    }
}
