package se.liu.itn.kts.tnk115.project2022;


import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class OptPlan {

    private ArrayList<Vertex> nodes;
    private ArrayList<Edge> edges;
    private List<Node> nodeList;
    private List<Link> linkList;
    private Dijkstra dijkstra;
    private double cost;
    private double minDist, maxDist, minAir, maxAir, minPave, maxPave, minElev, maxElev, minTT, maxTT;
    private double paveNorm, elevNorm, airNorm, distNorm, ttNorm;
    private double minTemp, maxTemp, tempNorm, minNoise, maxNoise, noiseNorm;

    public OptPlan() {
        this.nodeList = null;
        this.linkList = null;
    }

    // Creating links and nodes
    public void createPlan(int mode, double paveRate, double elevRate, double airRate, double ttRate, double tempRate, double noiseRate) {
        this.nodeList = MainActivity.nodeDao.getAllNodes();
        this.linkList = MainActivity.linkDao.getAllLinks();
        nodes = new ArrayList<Vertex>();
        edges = new ArrayList<Edge>();

        // Converting the nodes to Vertex to be used in Dijkstra
        for (int i=0; i<nodeList.size(); i++) {
            Vertex location = new Vertex(String.valueOf(nodeList.get(i).id),"Nod #"+nodeList.get(i).id);
            nodes.add(location);
        }

        // If no weights are active the algorithm just takes the shortest distance
        if (paveRate == 0 && elevRate == 0 && airRate == 0 && ttRate == 0) {
            for (int i=0; i<linkList.size(); i++) {
                cost = (double)linkList.get(i).dist;

                //Log.d("OptPlan","S:"+linkList.get(i).source+"->D:"+linkList.get(i).destination+" Path cost: "+cost);

                Edge arc = new Edge("c"+i,nodes.get(linkList.get(i).source-1),nodes.get(linkList.get(i).destination-1),cost);
                edges.add(arc);
            }
        } else {
            ArrayList<Double> tt = new ArrayList<Double>();
            minDist = (double)MainActivity.linkDao.getMinDist();
            maxDist = (double)MainActivity.linkDao.getMaxDist();
            Log.d("OptPlan","Dist: "+minDist+" "+maxDist);
            minElev = (double)MainActivity.linkDao.getMinElev();
            maxElev = (double)MainActivity.linkDao.getMaxElev();
            Log.d("OptPlan","Elev: "+minElev+" "+maxElev);
            minAir = (double)MainActivity.linkDao.getMinAir();
            maxAir = (double)MainActivity.linkDao.getMaxAir();
            Log.d("OptPlan","Air: "+minAir+" "+maxAir);
            minTemp = (double)MainActivity.linkDao.getMinTemp();
            maxTemp = (double)MainActivity.linkDao.getMaxTemp();
            Log.d("OptPlan","Temp: "+minTemp+" "+maxTemp);
            minNoise = (double)MainActivity.linkDao.getMinNoise();
            maxNoise = (double)MainActivity.linkDao.getMaxNoise();
            Log.d("OptPlan","Noise: "+minNoise+" "+maxNoise);

            if (mode == 1) {
                minPave = (double)MainActivity.linkDao.getMinPed();
                maxPave = (double)MainActivity.linkDao.getMaxPed();
            } else if (mode == 2) {
                minPave = (double)MainActivity.linkDao.getMinWC();
                maxPave = (double)MainActivity.linkDao.getMaxWC();
            } else {
                minPave = (double)MainActivity.linkDao.getMinPave();
                maxPave = (double)MainActivity.linkDao.getMaxPave();
            }

            minTT = Double.MAX_VALUE;
            maxTT = Double.MIN_VALUE;
            for (int i=0; i<linkList.size(); i++) {
                double value = 0.0;
                if (mode == 1) {
                    value = (double)(((double)linkList.get(i).dist) / (1.5*linkList.get(i).ttcog*linkList.get(i).ttelev));
                } else if (mode == 2) {
                    value = (double)(((double)linkList.get(i).dist) / (1.2*linkList.get(i).ttcog*linkList.get(i).ttelev*linkList.get(i).ttwc));
                } else {
                    value = (double)(((double)linkList.get(i).dist) / (5.5*linkList.get(i).ttcog*linkList.get(i).ttelev*linkList.get(i).ttcycle));
                }
                if (value >= maxTT) maxTT = value;
                if (value <= minTT) minTT = value;
                tt.add(value);
            }

            Log.d("OptPlan","TT: "+minTT+" "+maxTT);

            for (int i=0; i<linkList.size(); i++) {
                if (mode == 1) {
                    paveNorm = (double)((double)linkList.get(i).pedp-minPave)/(maxPave-minPave);
                } else if (mode == 2) {
                    paveNorm = (double)((double)linkList.get(i).wcpave-minPave)/(maxPave-minPave);
                } else {
                    paveNorm = (double)((double)linkList.get(i).pave-minPave)/(maxPave-minPave);
                }

                elevNorm = ((double)linkList.get(i).elev-minElev)/(maxElev-minElev);
                airNorm = ((double)linkList.get(i).air-minAir)/(maxAir-minAir);
                distNorm = ((double)linkList.get(i).dist-minDist)/(maxDist-minDist);
                ttNorm = ((double)tt.get(i)-minTT)/(maxTT-minTT);
                if (minTemp == maxTemp) {
                    tempNorm = 1.0;
                } else {
                    tempNorm = (linkList.get(i).temp-minTemp)/(maxTemp-minTemp);
                }
                if (minNoise == maxNoise) {
                    noiseNorm = 1;
                } else {
                    noiseNorm = (linkList.get(i).noise - minNoise) / (maxNoise - minNoise);
                }

                cost = ((paveNorm*paveRate+elevNorm*elevRate+airNorm*airRate+noiseNorm*noiseRate+tempNorm*tempRate)*distNorm+ttNorm*ttRate);

                //Log.d("OptPlan","S:"+linkList.get(i).source+"->D:"+linkList.get(i).destination+" Path cost: "+cost);

                Edge arc = new Edge("c"+i,nodes.get(linkList.get(i).source-1),nodes.get(linkList.get(i).destination-1),cost);
                edges.add(arc);
            }
        }

        Graph graph = new Graph(nodes,edges);

        dijkstra = new Dijkstra(graph);
    }

    // Return the shortest path
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
