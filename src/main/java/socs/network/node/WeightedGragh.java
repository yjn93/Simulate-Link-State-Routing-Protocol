package socs.network.node;

public class WeightedGragh {
	short[][] edges;
	String[] labels;
	public WeightedGragh(int n){
		edges = new short[n][n];
		labels = new String[n];
	}
	public void setLabel(int vertex, String label){
		labels[vertex] = label;
	}
	public String getLabel(int vertex){
		return labels[vertex];
	}
	public int getVertex(String label){
		for(int i = 0; i < labels.length; i ++){
			if(labels[i].equals(label))
				return i;
		}
		System.out.println("Error in geting vertex: no such label");
		return -1;
	}
	public void addEdge(int source, int target, short w) {
		edges[source][target] = w;
	}

	public boolean isEdge(int source, int target) {
		return edges[source][target] > 0;
	}

	public void removeEdge(int source, int target) {
		edges[source][target] = 0;
	}

	public short getWeight(int source, int target) {
		return edges[source][target];
	}
	
	public int[] neighbors(int vertex) {
		int count = 0;
		for (int i = 0; i < edges[vertex].length; i++) {
			if (edges[vertex][i] > 0)
				count++;
		}
		final int[] answer = new int[count];
		count = 0;
		for (int i = 0; i < edges[vertex].length; i++) {
			if (edges[vertex][i] > 0)
				answer[count++] = i;
		}
		return answer;
	}
	
	public void print() {
		for (int j = 0; j < edges.length; j++) {
			System.out.print(labels[j] + ": ");
			for (int i = 0; i < edges[j].length; i++) {
				if (edges[j][i] > 0)
					System.out.print(labels[i] + ":" + edges[j][i] + " ");
			}
			System.out.println();
		}
	}
	public void printA() {
		for (int j = 0; j < edges.length; j++) {
			System.out.print(labels[j] + ": ");
			for (int i = 0; i < edges[j].length; i++) {
					System.out.print(labels[i] + ":" + edges[j][i] + " ");
			}
			System.out.println();
		}
	}
}
