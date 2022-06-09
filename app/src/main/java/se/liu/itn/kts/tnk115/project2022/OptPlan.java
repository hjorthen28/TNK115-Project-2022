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
        if (paveRate == 0 && elevRate == 0 && airRate == 0 && ttRate == 0 && tempRate == 0 && noiseRate == 0) {
            minDist = MainActivity.linkDao.getMinDist();
            maxDist = MainActivity.linkDao.getMaxDist();
            Log.d("OptPlan","Dist: "+minDist+" "+maxDist);

            for (int i=0; i<linkList.size(); i++) {
                // The default link cost
                //cost = linkList.get(i).dist;

                distNorm = norm(linkList.get(i).dist,maxDist,minDist);

                // If the user has opted to remove stairs:
                if (mode == 2 && linkList.get(i).wcpave >= 5.0) {
                    // The links with stairs get a huge penalty
                    cost = 1000.0+distNorm;
                } else {
                    cost = distNorm;
                }

                //Log.d("OptPlan","S:"+linkList.get(i).source+"->D:"+linkList.get(i).destination+" Path cost: "+cost);

                Edge arc = new Edge("c"+i,nodes.get(linkList.get(i).source-1),nodes.get(linkList.get(i).destination-1),cost);
                edges.add(arc);
            }
        } else {
            ArrayList<Double> tt = new ArrayList<Double>();

            minDist = MainActivity.linkDao.getMinDist();
            maxDist = MainActivity.linkDao.getMaxDist();
            Log.d("OptPlan","Dist: "+minDist+" "+maxDist);

            /*if (mode == 1) {
                minPave = MainActivity.linkDao.getMinPed();
                maxPave = MainActivity.linkDao.getMaxPed();
            } else if (mode == 2) {
                minPave = MainActivity.linkDao.getMinWC();
                maxPave = MainActivity.linkDao.getMaxWC();
            } else {
                minPave = MainActivity.linkDao.getMinBike();
                maxPave = MainActivity.linkDao.getMaxBike();
            }*/
            minPave = MainActivity.linkDao.getMinPave();
            maxPave = MainActivity.linkDao.getMaxPave();
            Log.d("OptPlan","Pave: "+minPave+" "+maxPave);

            minElev = MainActivity.linkDao.getMinElev();
            maxElev = MainActivity.linkDao.getMaxElev();
            Log.d("OptPlan","Elev: "+minElev+" "+maxElev);
            minAir = MainActivity.linkDao.getMinAir();
            maxAir = MainActivity.linkDao.getMaxAir();
            Log.d("OptPlan","Air: "+minAir+" "+maxAir);
            minTemp = MainActivity.linkDao.getMinTemp();
            maxTemp = MainActivity.linkDao.getMaxTemp();
            Log.d("OptPlan","Temp: "+minTemp+" "+maxTemp);
            minNoise = MainActivity.linkDao.getMinNoise();
            maxNoise = MainActivity.linkDao.getMaxNoise();
            Log.d("OptPlan","Noise: "+minNoise+", "+maxNoise);

            minTT = Double.MAX_VALUE;
            maxTT = Double.MIN_VALUE;
            for (int i=0; i<linkList.size(); i++) {
                double value = 0.0;
                // The calculation of travel time based on which mode the user has chosen
                /*if (mode == 1) {
                    value = ((linkList.get(i).dist) / (1.5*linkList.get(i).ttcong*linkList.get(i).ttelev));
                } else if (mode == 2) {
                    value = ((linkList.get(i).dist) / (1.2*linkList.get(i).ttcong*linkList.get(i).ttelev*linkList.get(i).ttwc));
                } else {
                    value = ((linkList.get(i).dist) / (5.5*linkList.get(i).ttcong*linkList.get(i).ttelev*linkList.get(i).ttcycle));
                }*/
                // The modified version:
                value = ((linkList.get(i).dist) / (1.5*linkList.get(i).ttcong*linkList.get(i).ttelev));
                if (value >= maxTT) maxTT = value;
                if (value <= minTT) minTT = value;
                tt.add(value);
            }

            Log.d("OptPlan","TT: "+minTT+" "+maxTT);

            for (int i=0; i<linkList.size(); i++) {
                /*if (mode == 1) paveNorm = norm(linkList.get(i).pedp,maxPave,minPave);
                else if (mode == 2) paveNorm = norm(linkList.get(i).wcpave,maxPave,minPave);
                else paveNorm = norm(linkList.get(i).bikep,maxPave,minPave);*/

                //paveNorm = norm(linkList.get(i).pave,maxPave,minPave);
                paveNorm = linkList.get(i).pave;
                elevNorm = norm(linkList.get(i).elev,maxElev,minElev);
                airNorm = norm(linkList.get(i).air,maxAir,minAir);
                distNorm = norm(linkList.get(i).dist,maxDist,minDist);
                ttNorm = norm(tt.get(i),maxTT,minTT);
                tempNorm = norm(linkList.get(i).temp,maxTemp,minTemp);
                noiseNorm = norm(linkList.get(i).noise,maxNoise,minNoise);

                if (mode == 2 && linkList.get(i).wcpave >= 5.0) {
                    cost = ((paveNorm*paveRate+elevNorm*elevRate+airNorm*airRate+noiseNorm*noiseRate+tempNorm*tempRate)*distNorm+ttNorm*ttRate+1000.0);
                } else {
                    cost = ((paveNorm*paveRate+elevNorm*elevRate+airNorm*airRate+noiseNorm*noiseRate+tempNorm*tempRate)*distNorm+ttNorm*ttRate);
                }

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

    private double norm(double value, double max, double min) {
        if (value > max) return 1000.0;
        else if (max != min) return ((value-min)/(max-min));
        else return 1.0;
    }

}
