package socs.network.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class ServerThread extends Thread{
	ServerSocket serverSocket;
	Router rd;
	private volatile boolean running = true;
	
	public ServerThread(ServerSocket s, Router r){
		serverSocket = s;
		rd = r;
	}
	public void run(){
	    while(running){
	    	try{
	    		//System.out.println("Waiting for client on port" + serverSocket.getLocalPort() + "...");
	    		Socket newserver = serverSocket.accept();
	    		ServerSubThread server = new ServerSubThread(newserver, rd);
	    		server.start();
	    	}
	    	catch(IOException e){
	    		e.printStackTrace();
	    		break;
	    	}
	    }
	}
	public void shutdown() throws IOException{
		running = false;
		serverSocket.close();
	}
}


