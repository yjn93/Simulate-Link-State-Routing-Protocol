package socs.network.node;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Vector;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;

public class ServerSubThread extends Thread{
	Socket server;
	Router router;
	
	ServerSubThread(Socket s, Router r){
		server = s;
		router = r;
	}
	@SuppressWarnings("null")
	public void run(){
		//System.out.println("Just connected to " + server.getRemoteSocketAddress());
		SOSPFPacket receivedMessage = null;
		try {
			receivedMessage = (SOSPFPacket) new ObjectInputStream(server.getInputStream()).readObject();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 	
		//received Hello message
		if(receivedMessage.sospfType == 0){	
			String remoteIP = receivedMessage.neighborID;
			//check if already exist this neighbor
			boolean first_hello = true;
			for(Link lk : router.ports){
				if(lk != null && lk.router2.simulatedIPAddress.equals(remoteIP))	{
					first_hello = false;
					break;
				}
			}
			
			//change the status of the remote router
			//First time received Hello, add remote router to one port and set its status as INIT
			if(first_hello){
				System.out.println("received Hello from " + receivedMessage.neighborID);
				RouterDescription remoteRouter = new RouterDescription();
				remoteRouter.processIPAddress = receivedMessage.srcProcessIP;
				remoteRouter.processPortNumber = receivedMessage.srcProcessPort;
				remoteRouter.simulatedIPAddress = remoteIP;
				remoteRouter.status = RouterStatus.INIT;
				System.out.println("set " + remoteIP + " state to INIT;\n");
				//System.out.println(">> ");
				//send a Hello message to Client
				SOSPFPacket sendingMessage = new SOSPFPacket();
				sendingMessage.sospfType = 0;
				sendingMessage.srcProcessIP = router.rd.processIPAddress;
				sendingMessage.srcProcessPort = router.rd.processPortNumber;
				sendingMessage.srcIP = router.rd.simulatedIPAddress;
				sendingMessage.dstIP = remoteIP;
				sendingMessage.routerID = router.rd.simulatedIPAddress;
				sendingMessage.neighborID = router.rd.simulatedIPAddress;
				ObjectOutputStream out = null;
				try {
					out = new ObjectOutputStream(server.getOutputStream());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					out.writeObject(sendingMessage);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				SOSPFPacket getSecondHello = null;
				try {
					getSecondHello = (SOSPFPacket) new ObjectInputStream(server.getInputStream()).readObject();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(getSecondHello.sospfType == 0){
					System.out.println("received Hello from " + getSecondHello.neighborID);
					remoteRouter.status = RouterStatus.TWO_WAY;
					System.out.println("set " + remoteRouter.simulatedIPAddress + " state to TWO_WAY;\n");
					for(int i = 0; i < 4; i ++){
						if(router.ports[i] == null){
							router.ports[i] = new Link(router.rd, remoteRouter, receivedMessage.weight);
							break;
						}						
					}
					router.portsNum ++;				
				}
			}
			System.out.print(">> ");
		}
		//periodical check hello, send ack back
		else if(receivedMessage.sospfType == 2){
			String remoteIP = receivedMessage.neighborID;
			//System.out.println("Periodical hello from: " + remoteIP);

			DataOutputStream out = null;
			try {
				out = new DataOutputStream(server.getOutputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				out.writeUTF("ACK from " + router.rd.simulatedIPAddress);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		else if(receivedMessage.sospfType == 1){
			System.out.println("received LSAUPDATE from " + receivedMessage.routerID);
			boolean update = false;
			boolean updateNeighbor = false;
			boolean removeNeighbor = false;
			short neighbor = 0;
			LSA neighbor_lsa = null;
			//Get the lsa array in LSAUPDATE message, check seq number, see whether there is some update.
				for (Enumeration<LSA> e = receivedMessage.lsaArray.elements(); e.hasMoreElements();){
					
					LSA new_lsa = (LSA) e.nextElement();

				if(router.lsd._store.get(new_lsa.linkStateID) != null){
					//System.out.println("Debug receive lsa old:"+new_lsa.linkStateID);
					LSA old_lsa = router.lsd._store.get(new_lsa.linkStateID);
					if (old_lsa.lsaSeqNumber<new_lsa.lsaSeqNumber){
						update = true;
						router.lsd._store.remove(old_lsa.linkStateID);
						router.lsd._store.put(new_lsa.linkStateID, new_lsa);
						//System.out.println("update "+ new_lsa.linkStateID);
						//check if the lsa comes from neighbor
						for(short i = 0; i < 4; i ++){
							if(router.ports[i] != null && router.ports[i].router2.simulatedIPAddress.equals(new_lsa.linkStateID)
									&& router.ports[i].router2.status == RouterStatus.TWO_WAY){
								LinkDescription ld = router.lsd.getLinkFromLSA(new_lsa, router.rd.simulatedIPAddress);
								if(ld != null){
									updateNeighbor = true;
									neighbor_lsa = new_lsa;
									neighbor = i;
								}
								else{
								 	router.lsd.removeLinkFromLSA(router.rd.simulatedIPAddress, router.ports[i].router2.simulatedIPAddress);
									router.ports[i] = null; //this link is disconnected
								} 
								break;
							}
						}						
					}
				}

				else{
					//System.out.println("Debug receive lsa new:"+new_lsa.linkStateID);
					update = true;
					router.lsd._store.put(new_lsa.linkStateID, new_lsa);
					//check if the lsa comes from neighbor
					for(short i = 0; i < 4; i ++){
						if(router.ports[i] != null && router.ports[i].router2.simulatedIPAddress.equals(new_lsa.linkStateID)
								&& router.ports[i].router2.status == RouterStatus.TWO_WAY){
							LinkDescription ld = router.lsd.getLinkFromLSA(new_lsa, router.rd.simulatedIPAddress);
							if(ld != null){
								updateNeighbor = true;
								neighbor_lsa = new_lsa;
								neighbor = i;
							}
							else{
							 	router.lsd.removeLinkFromLSA(router.rd.simulatedIPAddress, router.ports[i].router2.simulatedIPAddress);
								router.ports[i] = null;  //this link is disconnected
							}
							break;
						}
					}
				}
			}
			//if there is update, send it to all neighbors except the one which send the update
			if(update){		
				//System.out.println(router.rd.simulatedIPAddress + " Update lsa: " + "  is:\n" + router.lsd.toString());
				for(Link lk: router.ports){
					if(lk != null && lk.router2.simulatedIPAddress.equals(receivedMessage.routerID) == false
							&& lk.router2.status == RouterStatus.TWO_WAY) {
						Socket client = null;
						try {
							client = new Socket(lk.router2.processIPAddress, lk.router2.processPortNumber);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							System.out.println(lk.router2.simulatedIPAddress+" is down.");
							System.out.print(">>");
							router.lsd.removeLinkFromLSA(router.rd.simulatedIPAddress, lk.router2.simulatedIPAddress);
							lk = null;
							try {
								router.lsdUpdate();
							} catch (IOException e11) {
								System.out.println(lk.router2.simulatedIPAddress+" is down.");
								System.out.print(">>");
								router.lsd.removeLinkFromLSA(router.rd.simulatedIPAddress, lk.router2.simulatedIPAddress);
								lk = null;
								try {
									router.lsdUpdate();
								} catch (UnknownHostException e) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}
						}
						SOSPFPacket sendingMessage = new SOSPFPacket();
						sendingMessage.sospfType = 1;
						sendingMessage.srcProcessIP = router.rd.processIPAddress;
						sendingMessage.srcProcessPort = router.rd.processPortNumber;
						sendingMessage.srcIP = router.rd.simulatedIPAddress;
						sendingMessage.dstIP = lk.router2.simulatedIPAddress;
						sendingMessage.routerID = router.rd.simulatedIPAddress;
						sendingMessage.neighborID = router.rd.simulatedIPAddress;
						sendingMessage.lsaArray = new Vector<LSA>();
						for(LSA update_lsa: router.lsd._store.values()){
							sendingMessage.lsaArray.addElement(update_lsa);
						}
						ObjectOutputStream out = null;
						try {
							out = new ObjectOutputStream(client.getOutputStream());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							System.out.println(lk.router2.simulatedIPAddress+" is down.");
							System.out.print(">>");
							router.lsd.removeLinkFromLSA(router.rd.simulatedIPAddress, lk.router2.simulatedIPAddress);
							lk = null;
							try {
								router.lsdUpdate();
							} catch (UnknownHostException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
						try {
							System.out.println("sending LSAUPDATE to " + sendingMessage.dstIP);
							out.writeObject(sendingMessage);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							System.out.println(lk.router2.simulatedIPAddress+" is down.");
							System.out.print(">>");
							router.lsd.removeLinkFromLSA(router.rd.simulatedIPAddress, lk.router2.simulatedIPAddress);
							lk = null;
							try {
								router.lsdUpdate();
							} catch (UnknownHostException e11) {
								// TODO Auto-generated catch block
								e11.printStackTrace();
							} catch (IOException e11) {
								// TODO Auto-generated catch block
								e11.printStackTrace();
							}
						}
						try {
							client.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			//if a new lsa of neighbors have updated, check own lsa
			if(updateNeighbor){
				LSA own_lsa = router.lsd._store.get(router.rd.simulatedIPAddress);
				//check whether own lsa contains this neighbor
				LinkDescription ld = router.lsd.getLinkFromLSA(own_lsa, neighbor_lsa.linkStateID);
				if(ld == null){
					ld = new LinkDescription();
				 	ld.linkID = neighbor_lsa.linkStateID;
					ld.portNum = neighbor;
					short cost = router.lsd.getLinkFromLSA(neighbor_lsa, router.rd.simulatedIPAddress).tosMetrics;
					ld.tosMetrics = cost;
					router.ports[neighbor].cost = cost;
					router.lsd.addLinkToLSA(router.rd.simulatedIPAddress, ld);
				}

				//System.out.println(router.rd.simulatedIPAddress + " Update lsa: " + "  is:\n" + router.lsd.toString());
				  for(Link lk: router.ports){
					  if(lk != null && lk.router2.status == RouterStatus.TWO_WAY){
						  Socket client = null;
						try {
							client = new Socket(lk.router2.processIPAddress, lk.router2.processPortNumber);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							System.out.println(lk.router2.simulatedIPAddress+" is down.");
							System.out.print(">>");
							router.lsd.removeLinkFromLSA(router.rd.simulatedIPAddress, lk.router2.simulatedIPAddress);
							lk = null;
							try {
								router.lsdUpdate();
							} catch (UnknownHostException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
						  SOSPFPacket ClientPacket = new SOSPFPacket();
						  ClientPacket.srcProcessIP = router.rd.processIPAddress;
						  ClientPacket.srcProcessPort = router.rd.processPortNumber;
						  ClientPacket.srcIP = router.rd.simulatedIPAddress;	  
						  ClientPacket.dstIP = lk.router2.simulatedIPAddress; 
						  ClientPacket.sospfType = 1;
						  ClientPacket.routerID = router.rd.simulatedIPAddress;
						  ClientPacket.neighborID = router.rd.simulatedIPAddress;
						  ClientPacket.lsaArray = new Vector<LSA>();
						  for(LSA update_lsa: router.lsd._store.values()){
							  ClientPacket.lsaArray.addElement(update_lsa);
						  }			  
						  ObjectOutputStream outToServer = null;
						try {
							outToServer = new ObjectOutputStream(client.getOutputStream());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							System.out.println(lk.router2.simulatedIPAddress+" is down.");
							System.out.print(">>");
							router.lsd.removeLinkFromLSA(router.rd.simulatedIPAddress, lk.router2.simulatedIPAddress);
							lk = null;
							try {
								router.lsdUpdate();
							}catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
						  System.out.println("sending LSAUPDATE to " + ClientPacket.dstIP);
						  try {
							outToServer.writeObject(ClientPacket);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							System.out.println(lk.router2.simulatedIPAddress+" is down.");
							System.out.print(">>");
							router.lsd.removeLinkFromLSA(router.rd.simulatedIPAddress, lk.router2.simulatedIPAddress);
							lk = null;
							try {
								router.lsdUpdate();
							}catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
						  		  
						  try {
							client.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						  
					  }
				  }
			}
			System.out.print(">> ");
		}			
		try {
			server.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 //System.out.print(">> ");
	}
}
