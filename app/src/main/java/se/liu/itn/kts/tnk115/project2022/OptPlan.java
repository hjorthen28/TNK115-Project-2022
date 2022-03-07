package se.liu.itn.kts.tnk115.project2022;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class OptPlan {

    private ArrayList<Vertex> nodes;
    private ArrayList<Edge> edges;
    private List<Node> nodeList;
    private List<Link> linkList;
    private Dijkstra dijkstra;

    public OptPlan() {
        this.nodeList = null;
        this.linkList = null;
    }

    public void addList(List<Node> nodeList, List<Link> linklist) {
        this.nodeList = nodeList;
        this.linkList = linklist;
    }

    public void createPlan() {
        nodes = new ArrayList<Vertex>();
        edges = new ArrayList<Edge>();

        for (int i=0; i<nodeList.size(); i++) {
            Vertex location = new Vertex(String.valueOf(nodeList.get(i).id),"Nod #"+nodeList.get(i).id);
            nodes.add(location);
        }

        for (int i=0; i<linkList.size(); i++) {
            Edge arc = new Edge("c"+i,nodes.get(linkList.get(i).source-1),nodes.get(linkList.get(i).destination-1),linkList.get(i).dist);
            edges.add(arc);
        }

        Graph graph = new Graph(nodes,edges);

        dijkstra = new Dijkstra(graph);
    }

    public LinkedList<Vertex> movement(int start, int fin) {
        // Compute shortest path
        dijkstra.execute(nodes.get(start-1));
        // To fin node:
        LinkedList<Vertex> path = dijkstra.getPath(nodes.get(fin-1));

        return path;
    }

    public String getPath(int start, int fin) {
        // Compute shortest path
        dijkstra.execute(nodes.get(start-1));
        // To fin node:
        LinkedList<Vertex> path = dijkstra.getPath(nodes.get(fin-1));

        // Extract and print shortest path (node ids)
        String result = "\n"+path.get(0).getId();
        for (int i = 1; i < path.size(); i++){
            result = result + "->"+path.get(i).getId();
        }
        return result;
    }

}
