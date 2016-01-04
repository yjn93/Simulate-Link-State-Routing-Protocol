package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class LinkStateDatabase {

  //linkID => LSAInstance
  HashMap<String, LSA> _store = new HashMap<String, LSA>();

  private RouterDescription rd = null;

  public LinkStateDatabase(RouterDescription routerDescription) {
    rd = routerDescription;
    LSA l = initLinkStateDatabase();
    _store.put(l.linkStateID, l);
  }

  public WeightedGragh buildGragh(){
	WeightedGragh g = new WeightedGragh(_store.size());
	int vertex = 0;	
	for (LSA lsa: _store.values()){
		g.setLabel(vertex, lsa.linkStateID);
		vertex ++;
	}	
	for (LSA lsa: _store.values()){
		for(LinkDescription ld: lsa.links){
			g.addEdge(g.getVertex(lsa.linkStateID), g.getVertex(ld.linkID), ld.tosMetrics);
		}			
	}
//	System.out.println("Weighted Gragh:\n");
//	g.print();
	return g;
	  
  }
  
  private int minVertex(int[] dist, boolean[] visited){
	int min = Integer.MAX_VALUE;
	int dest = -1;
	for (int i = 0; i < dist.length; i++) {
		if (!visited[i] && dist[i] < min) {
			dest = i;
			min = dist[i];
		}
	}
	return dest;
  }
  
  public int[] Dijkstra(WeightedGragh wg){
	//LinkedList<LinkDescription> source = new LinkedList<LinkDescription>();
	int s = wg.getVertex(rd.simulatedIPAddress);
	int[] dist = new int[_store.size()]; // record the shortest distance to every node
	int[] prev = new int[_store.size()]; // record the previous node in the path
	for(int i = 0; i < prev.length; i ++){
		prev[i] = -1;
	}
	boolean[] visited = new boolean[_store.size()];
	for (int i = 0; i < dist.length; i++) {
		dist[i] = Integer.MAX_VALUE;
	}
	dist[s] = 0;
	for(int i = 0; i < dist.length; i ++){
		int next = minVertex(dist, visited);
		if(next == -1){
			return prev;
		}
		visited[next] = true;
		int[] neighbors = wg.neighbors(next);
		for(int j = 0; j < neighbors.length; j ++){
			int node = neighbors[j];
			int distance = dist[next] + wg.getWeight(next, node);
			if(dist[node] > distance){
				dist[node] = distance;
				prev[node] = next;
			}
		}
	}
	return prev;
  }
  /**
   * output the shortest path from this router to the destination with the given IP address
   */
  public String getShortestPath(String destinationIP) {
    //TODO: fill the implementation here
	WeightedGragh wg = buildGragh();
	int[] prev = Dijkstra(wg);
	int sourse = wg.getVertex(rd.simulatedIPAddress);
	int dest = wg.getVertex(destinationIP);
	if (dest != -1){
		List<LinkDescription> path = new ArrayList<LinkDescription>();
		int next_dest = prev[dest];
		if(next_dest == -1){
			System.out.println("Cannot reach this destination\n");
			return " ";
		}
		while(dest != sourse){
			LinkDescription newLink = new LinkDescription();
			newLink.linkID = wg.getLabel(dest);
			newLink.tosMetrics = wg.getWeight(next_dest, dest);
			path.add(0, newLink);
			dest = next_dest;
			next_dest = prev[next_dest];
		}
		System.out.println("The shortest path to " + destinationIP + " is: ");	
	    StringBuilder sb = new StringBuilder();
	    sb.append(rd.simulatedIPAddress);
	    for (LinkDescription ld: path) {
	      sb.append(" ->").append("(" + ld.tosMetrics + ") ").append(ld.linkID);
	    }
	    sb.append("\n");
	    return sb.toString();	
	}
	return " ";
  }
  

  //initialize the linkstate database by adding an entry about the router itself
  private LSA initLinkStateDatabase() {
    LSA lsa = new LSA();
    lsa.linkStateID = rd.simulatedIPAddress;
    lsa.lsaSeqNumber = Integer.MIN_VALUE;
    LinkDescription ld = new LinkDescription();
    ld.linkID = rd.simulatedIPAddress;
    ld.portNum = -1;
    ld.tosMetrics = 0;
    lsa.links.add(ld);
    return lsa;
  }

  //update one LSA in linkstate database by adding a new link
  public synchronized void addLinkToLSA(String ip, LinkDescription link){
	  if(_store.get(ip) == null){
		  System.out.println("No corresponding LSA, cannot update LinkStateDatabase.\n");
		  return;
	  }
	  LSA lsa = _store.get(ip);
	  lsa.lsaSeqNumber ++;
	  lsa.links.add(link);	  
	  return;
  }
  
  public synchronized void removeLinkFromLSA(String ip, String remove_ip){
	  if(_store.get(ip) == null){
		  System.out.println("No corresponding LSA, cannot update LinkStateDatabase.\n");
		  return;
	  }
	  LSA lsa_own = _store.get(ip);
	  for(LinkDescription l: lsa_own.links){
		  if(l.linkID.equals(remove_ip)){
			  lsa_own.links.remove(l);
			  break;
		  }
	  }
	  lsa_own.lsaSeqNumber ++;
	  return;
	  
  }
  
  public synchronized void removeLSA(String ip){
	  if(_store.get(ip) == null){
		  System.out.println("No corresponding LSA, cannot remove it.\n");
		  return;
	  }
	  _store.remove(ip);
	  return;
	  
  }
  
  public LinkDescription getLinkFromLSA(LSA lsa, String lk_ip){
	  for(LinkDescription l: lsa.links){
		  if(l.linkID.equals(lk_ip)){
			  return l;
		  }
	  }  
	  return null;
  }
  
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (LSA lsa: _store.values()) {
      sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
      for (LinkDescription ld : lsa.links) {
        sb.append(ld.linkID).append(",").append(ld.portNum).append(",").
                append(ld.tosMetrics).append("   ");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

}
