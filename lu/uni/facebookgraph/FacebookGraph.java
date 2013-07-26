package lu.uni.facebookgraph;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import org.graphstream.algorithm.Toolkit;
import org.graphstream.algorithm.community.AltSawSharc;
import org.graphstream.algorithm.community.EpidemicCommunityAlgorithm;
import org.graphstream.algorithm.community.Leung;
import org.graphstream.algorithm.community.NewSawSharc;
import org.graphstream.algorithm.community.SawSharc;
import org.graphstream.algorithm.community.Sharc;
import org.graphstream.algorithm.community.SyncEpidemicCommunityAlgorithm;
import org.graphstream.algorithm.measure.Modularity;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.GraphParseException;
import org.graphstream.stream.ProxyPipe;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceDGS;
import org.graphstream.ui.layout.springbox.implementations.LinLog;
import org.graphstream.ui.swingViewer.Viewer;

import static org.graphstream.algorithm.Toolkit.*;

public class FacebookGraph {
	private Graph g;

	private LinLog layout;
	private double a = 0;
	private double r = -1.3;
	private double force = 3;
	private ProxyPipe fromViewer;

	protected Random rng;

	public FacebookGraph() throws ElementNotFoundException, IOException, GraphParseException {
		g = new SingleGraph("facebook") ;
		rng = new Random();
		//g.setAttribute("ui.stylesheet", "url('stylewhite.css')");

		System.out.println(Toolkit.averageClusteringCoefficient(g));
		System.out.println(Toolkit.diameter(g));

		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

		g.addAttribute("ui.quality");
		g.addAttribute("ui.antialias");
		Viewer viewer = g.display(false);
		fromViewer = viewer.newThreadProxyOnGraphicGraph();
		g.addAttribute("ui.screenshot", "graph.png");

		layout = new LinLog(false);	
		layout.configure(a, r, true, force);	
		layout.addSink(g);		
		layout.setQuality(1);
		g.addSink(layout);	
		fromViewer.addSink(g);	
		g.read("graph.dgs");

		g.removeNode("1360969633");
		Modularity mod = new Modularity("ui.class");
		mod.init(g);
		//System.out.println(Toolkit.averageClusteringCoefficient(g));
		//System.out.println(Toolkit.diameter(g));

		Double bestMod = Double.MIN_VALUE;
		for (int i = 0; i < 1000; i++){
			System.out.println(i);
			//Leung algo = new Leung(g
			//EpidemicCommunityAlgorithm algo = new EpidemicCommunityAlgorithm(g);
			//SyncEpidemicCommunityAlgorithm algo = new SyncEpidemicCommunityAlgorithm(g);
			Sharc algo = new Sharc(g);
			//SawSharc algo = new SawSharc(g);
			//AltSawSharc algo = new AltSawSharc(g);
			//NewSawSharc algo = new NewSawSharc(g);

			algo.init(g);
			algo.compute();
			mod.compute();
			Double curMod = mod.getMeasure();

			if (bestMod <= curMod){
				backupSolution();
				bestMod = curMod;
			}
		}
		restoreSolution();

		for (Node n : g.getNodeSet()){
			double cc = Toolkit.clusteringCoefficient(n);
			//n.setAttribute("label", n.getAttribute("name")+" "+String.format("%.2g%n", cc)+" "+n.getAttribute("ui.class"));
			n.setAttribute("label", n.getAttribute("name"));
			//n.setAttribute("ui.size", n.getEdgeSet().size());
			//n.setAttribute("ui.size", 35 - (cc * 30 ));

			//if (n.getEdgeSet().size() == 1){
			//	n.setAttribute("ui.class", "alone");
			//	n.setAttribute("ui.size", 20);
			//}

		}

		g.addAttribute("stylesheet", stylesheet());
		System.out.println(stylesheet());
		printCommunities();
		printBridges();
		System.out.println("Mod = "+bestMod);
		while(! g.hasAttribute("ui.viewClosed")) {		// 4
			fromViewer.pump();				// 3
			layout.compute();
		}
	}

	private HashMap<String, String> backup;
	private void backupSolution(){
		backup = new HashMap<String, String>();
		for (Node n : g.getNodeSet()){
			backup.put(n.getId(), (String)n.getAttribute("ui.class"));
		}
	}

	private void restoreSolution(){
		for (Node n : g.getNodeSet()){
			n.setAttribute("ui.class", backup.get(n.getId()));
		}
	}

	public void printCommunities(){
		HashSet<String> communities = new HashSet<String>();
		for (Node n : g.getNodeSet()){
			communities.add((String) n.getAttribute("ui.class"));
		}

		for (String s : communities){
			System.out.println(s);
			for (Node n : g.getNodeSet()){
				if (((String)n.getAttribute("ui.class")).compareTo(s) == 0){
					System.out.println("\t"+n.getAttribute("name"));
				}
				//else
				//System.out.println("*** "+n.getAttribute("ui.class"));
			}

		}
	}

	public void printBridges(){
		int cpt = 0;
		for (Node n : g.getNodeSet()){
			Iterator<Node> it = n.getNeighborNodeIterator();
			HashSet<String> communities = new HashSet<String>();
			while (it.hasNext()){
				Node cur = it.next();
				communities.add((String)cur.getAttribute("ui.class"));
			}
			if (communities.size() > 1){
				n.setAttribute("ui.size", 10*communities.size());
				n.setAttribute("label", n.getAttribute("name")+" "+communities.size());
				System.out.println(n.getAttribute("name")+" "+communities.size());
				cpt ++;
			}
			else
				n.setAttribute("ui.size", 10);
		}
		System.out.println("Bridges % "+(double)cpt/(double)g.getNodeCount()*100);
	}

	public String stylesheet(){
		HashSet<String> communities = new HashSet<String>();
		for (Node n : g.getNodeSet()){
			communities.add((String) n.getAttribute("ui.class"));
		}

		String style = new String();
		style += "graph { fill-color: white; padding: 10px;}\n";
		style += "node {size-mode: dyn-size; ";
		//style += "text-alignment: above; text-background-mode: rounded-box; text-background-color: lightgrey; text-color: white; text-style: bold; text-padding: 3px, 2px;}\n";
		style += "text-alignment: left; text-color: #111; text-style: italic; }\n";
		style += "edge {fill-color: lightgrey;}\n";
		for (String com : communities){
			style += "node."+com+" { fill-color: rgb("
					+ rng.nextInt(255) + "," + rng.nextInt(255) + ","
					+ rng.nextInt(255) + "); }\n";
		}
		return style;
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws GraphParseException 
	 * @throws ElementNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ElementNotFoundException, GraphParseException {
		new FacebookGraph();
	}



}
