//-----------------------------------------------
//	Group: 4
//	Jianan Yue (260668037)
//	Julie Roy Prevost (260532659)
//-----------------------------------------------
package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.Timer;
import java.util.Vector;

import ch.qos.logback.core.net.server.Client;


public class Router {

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();
  ServerSocket serverSocket;
  ServerThread serverThread;
  Timer time;
  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];
  
  //record the number of ports being occupied
  short portsNum = 0;

  public Router(Configuration config) throws IOException, ClassNotFoundException {
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");
    rd.processPortNumber = config.getShort("socs.network.router.portNum");
    rd.processIPAddress = "127.0.0.1";
    lsd = new LinkStateDatabase(rd);
	serverSocket = new ServerSocket(rd.processPortNumber);
	serverThread = new ServerThread(serverSocket, this); 
	serverThread.start();  
	time = new Timer(); 
	ScheduledTask st = new ScheduledTask(this); 
	time.schedule(st, 0, 10000); // Create Repetitively task for every 10 secs
  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {
	  if(destinationIP.equals(rd.simulatedIPAddress)){
		  System.out.println("This is your own IP, distance is (0)");
	  }
	  else{
		  System.out.println(lsd.getShortestPath(destinationIP));
	  }
  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
 * @throws Exception 
 * @throws UnknownHostException 
   */
  public void processDisconnect(short portNumber) throws UnknownHostException, Exception {
	  String remove_ip = ports[portNumber].router2.simulatedIPAddress;
	  if(remove_ip == rd.simulatedIPAddress){
		  System.out.println("This is your own ip, you cannot disconnect with yourself");
	  }
	  
	  lsd.removeLinkFromLSA(rd.simulatedIPAddress, remove_ip);

	  //System.out.println(rd.simulatedIPAddress + ":  lsd after disconnect  " + "  is:\n" + lsd.toString());
	  lsdUpdate();
	  ports[portNumber] = null;
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort,
                             String simulatedIP, short weight) {
	  if(portsNum < 4){
		  for(Link links : ports){
			 if(links != null){ 
				 if(links.router2.simulatedIPAddress.equals(simulatedIP)){
					 System.out.println("This connection is already established");
					 return;
				 }
				 if(links.router1.simulatedIPAddress.equals(simulatedIP)){
					 System.out.println("You cannot connect with yourself");
					 return;
				 }
			 }
		  }

		  for(int i = 0; i < 4; i ++){
			 if(ports[i] == null){ 
				  RouterDescription router2 = new RouterDescription();
				  router2.processIPAddress = processIP;
				  router2.processPortNumber = processPort;
				  router2.simulatedIPAddress = simulatedIP;	
				  router2.status = null;
				  ports[i] = new Link(rd, router2, weight);
				  portsNum ++;	
				  break;
			 }
		  }		  

	  }
	  else{
		  System.out.println("No extra port for new link!");
	  }
	 // System.out.println("The linkstate database of " + rd.simulatedIPAddress + "  is:\n" + lsd.toString());
  }

  /**
   * broadcast Hello to neighbors
 * @throws IOException 
 * @throws UnknownHostException 
 * @throws ClassNotFoundException 
   */
  private void processStart() throws UnknownHostException, IOException, ClassNotFoundException {
	  short count = 0;
	  for(Link lk : ports){
		  if(lk != null && lk.router2.status != RouterStatus.TWO_WAY){
			  Socket client = new Socket(lk.router2.processIPAddress, lk.router2.processPortNumber);
			  SOSPFPacket ClientPacket = new SOSPFPacket();
			  ClientPacket.srcProcessIP = rd.processIPAddress;
			  ClientPacket.srcProcessPort = rd.processPortNumber;
			  ClientPacket.srcIP = rd.simulatedIPAddress;	  
			  ClientPacket.dstIP = lk.router2.simulatedIPAddress; 
			  ClientPacket.sospfType = 0;
			  ClientPacket.routerID = rd.simulatedIPAddress;
			  ClientPacket.neighborID = rd.simulatedIPAddress;
			  ClientPacket.weight = lk.cost;
			  ObjectOutputStream outToServer = new ObjectOutputStream(client.getOutputStream());
			  outToServer.writeObject(ClientPacket);
			  ObjectInputStream inFromServer = new ObjectInputStream(client.getInputStream());
			  SOSPFPacket ServerPacket = (SOSPFPacket) inFromServer.readObject();
			  if(ServerPacket.sospfType == 0){
				  System.out.println("received Hello from " + ServerPacket.neighborID);
				  lk.router2.status = RouterStatus.TWO_WAY;
				  System.out.println("set " + lk.router2.simulatedIPAddress + " state to TWO_WAY;\n");
				  //ObjectOutputStream secondHello = new ObjectOutputStream(client.getOutputStream());
				  ObjectOutputStream secondHello = new ObjectOutputStream(client.getOutputStream());;
				  secondHello.writeObject(ClientPacket);
			  }
			  client.close();	
			  LinkDescription ld = new LinkDescription();
			  ld.linkID = lk.router2.simulatedIPAddress;
			  ld.portNum = count;			  
			  ld.tosMetrics = lk.cost;
			  lsd.addLinkToLSA(rd.simulatedIPAddress, ld);
		  }
		  count ++;
	  }
	  
	  //System.out.println("The linkstate database of " + rd.simulatedIPAddress + "  is:\n" + lsd.toString());
	  lsdUpdate();
	  
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
 * @throws IOException 
 * @throws ClassNotFoundException 
 * @throws UnknownHostException 
   */
  private void processConnect(String processIP, short processPort,
                              String simulatedIP, short weight) throws UnknownHostException, ClassNotFoundException, IOException {
	  processAttach(processIP, processPort, simulatedIP, weight);
	  processStart();

  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {
	  /**
	   Loop through links in ports
	   */
	  int count = 1;
	  for(Link links : ports){
		  
		 if(links != null && links.router2.status == RouterStatus.TWO_WAY){
			 System.out.println("IP Address of neighbor " + count + " is : " + links.router2.simulatedIPAddress);
			 count++;
		 }
	  }
  }

  /**
   * disconnect with all neighbors and quit the program
 * @throws Exception 
   */
  private void processQuit() throws Exception {
//	  serverThread.shutdown();
//
//	  //serverSocket.close();
//	  time.cancel();
	  System.exit(0);
  }
  
  public void lsdUpdate() throws UnknownHostException, IOException{
	  for(Link lk : ports){
		  if(lk != null && lk.router2.status == RouterStatus.TWO_WAY){
			  Socket client = new Socket(lk.router2.processIPAddress, lk.router2.processPortNumber);
			  SOSPFPacket ClientPacket = new SOSPFPacket();
			  ClientPacket.srcProcessIP = rd.processIPAddress;
			  ClientPacket.srcProcessPort = rd.processPortNumber;
			  ClientPacket.srcIP = rd.simulatedIPAddress;	  
			  ClientPacket.dstIP = lk.router2.simulatedIPAddress; 
			  ClientPacket.sospfType = 1;
			  ClientPacket.routerID = rd.simulatedIPAddress;
			  ClientPacket.neighborID = rd.simulatedIPAddress;
			  ClientPacket.lsaArray = new Vector<LSA>();		  
			  for(LSA update_lsa: lsd._store.values()){
				  ClientPacket.lsaArray.addElement(update_lsa);
			  }
			  ObjectOutputStream outToServer = new ObjectOutputStream(client.getOutputStream());
			  System.out.println("sending LSAUPDATE to " + ClientPacket.dstIP);
			  outToServer.writeObject(ClientPacket);
			  client.close();
	
		  }
	  }
	  return;
  }
  
  private void printLSD(){
	  System.out.println(rd.simulatedIPAddress + "  lsd is:\n" + lsd.toString());
  }
  
  public void terminal() {
    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      System.out.print(">> ");
      String command = br.readLine();
      while (true) {
        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("disconnect ")) {
          String[] cmdLine = command.split(" ");
          processDisconnect(Short.parseShort(cmdLine[1]));
        } else if (command.startsWith("quit")) {
          processQuit();
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.startsWith("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else if(command.equals("printLSD")){
          printLSD();
        } else {
          System.out.println("Invalid command, please try again");
          //break;
        }
        System.out.print(">> ");
        command = br.readLine();
      }
      //isReader.close();
      //br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
