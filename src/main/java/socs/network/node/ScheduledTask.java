package socs.network.node;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.TimerTask;
import java.util.Vector;

import socs.network.message.LSA;
import socs.network.message.SOSPFPacket;

public class ScheduledTask extends TimerTask {
	//send hello and expect ack
	Router router;
	public ScheduledTask( Router r){
		router = r;
	}
	public void run(){
		short portNum = 0;
		for(Link lk : router.ports){
			  if(lk != null && lk.router2.status == RouterStatus.TWO_WAY){
				  Socket client = null;
				try {
					client = new Socket(lk.router2.processIPAddress, lk.router2.processPortNumber);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					//System.out.println("ACK not received");
					//continue;
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					System.out.println(lk.router2.simulatedIPAddress+" is down.");
					System.out.print(">>");
					router.lsd.removeLinkFromLSA(router.rd.simulatedIPAddress, lk.router2.simulatedIPAddress);
					lk = null;
					router.ports[portNum] = null;
					//System.out.println("The linkstate database of " + router.rd.simulatedIPAddress + "  is:\n" + router.lsd.toString());
					try {
						router.lsdUpdate();
					} catch (UnknownHostException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}				
					continue;
				}
				  SOSPFPacket ClientPacket = new SOSPFPacket();
				  ClientPacket.srcProcessIP = router.rd.processIPAddress;
				  ClientPacket.srcProcessPort = router.rd.processPortNumber;
				  ClientPacket.srcIP = router.rd.simulatedIPAddress;	  
				  ClientPacket.dstIP = lk.router2.simulatedIPAddress; 
				  ClientPacket.sospfType = 2;
				  ClientPacket.routerID = router.rd.simulatedIPAddress;
				  ClientPacket.neighborID = router.rd.simulatedIPAddress;
				  ClientPacket.weight = lk.cost;
				  //System.out.println("Periodical hello to: " + ClientPacket.dstIP);
				  ObjectOutputStream outToServer = null;
				try {
					outToServer = new ObjectOutputStream(client.getOutputStream());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				  try {
					outToServer.writeObject(ClientPacket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				  try {
					 //wait ack for 5s
					  client.setSoTimeout(5000);
					  DataInputStream inFromServer = new DataInputStream(client.getInputStream());
					  //System.out.println("Received " + inFromServer.readUTF());
					} catch (SocketTimeoutException e) {
						// TODO Auto-generated catch block
						System.out.println(lk.router2.simulatedIPAddress+" is down.");	
						System.out.print(">>");
						router.lsd.removeLinkFromLSA(router.rd.simulatedIPAddress, lk.router2.simulatedIPAddress);
						lk = null;
						router.ports[portNum] = null;
						//Update ports and lsa
						try {
							router.lsdUpdate();
							continue;
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				  try {
					client.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
				  portNum ++;
			  }
		}
		
	}


}
