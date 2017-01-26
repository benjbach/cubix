package cubix.dataSets;

import com.opencsv.CSVReader;
import cubix.data.CEdge;
import cubix.data.CNode;
import cubix.data.CTime;
import cubix.data.TimeGraph;
import cubix.data.TimeGraphUtils;
import cubix.helper.Log;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import edu.uci.ics.jung.graph.util.EdgeType;

public class BrainDataSet{

	
	protected File dir;
	
	
	static final String EDGE_SYMB = "->";
	
	
	protected HashMap<String, Color> groupColors = new HashMap<String, Color>();
		
	static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

	private static TimeGraphUtils<CNode, CEdge, CTime> utils;

	
	public static TimeGraph<CNode, CEdge, CTime> load(File dir, String nameFilter, int fileAmount, int steps)
	{
		TimeGraph<CNode, CEdge, CTime> tGraph = new TimeGraph<CNode, CEdge, CTime>();
		utils = new TimeGraphUtils<CNode, CEdge, CTime>();
		
		if(!dir.exists()) {	
			System.err.println("[CallGraphData] File not found: " + dir.getAbsolutePath());
			return null;
		}
		File[] files = dir.listFiles(new FilenameContainsFilter("csv"));
		
		int timeAmount = files.length; // one is .svn, the other is description file
		int loaded = 0;
		for(int t = 0; t < timeAmount && loaded < fileAmount; t+=steps)
		{
			if(files[t].getName().contains(nameFilter))
			{
				loaded++;
				loadCSVFile(files[t], tGraph, t);	
			}
		}
		return tGraph;
	}
	
	
	
	private static void loadCSVFile(File file, TimeGraph<CNode, CEdge, CTime> tGraph, int timeStep) 
	{
		
		HashMap<String,CNode> vertices = new HashMap<String, CNode>();
	        ArrayList<CNode> nodes = new ArrayList<CNode>();

	        float weight = 0;
	        try {
	    	    CSVReader r = new CSVReader(new FileReader(file));
	    	    String[] line = r.readNext();
	    	    String groupname, nodeLabel;
	    	    CTime t;
				try {
					t = new CTime(df.parse("2012-" + timeStep + "-12").getTime());
				} catch (ParseException e) {
					e.printStackTrace();
					return;
				}

	    	    // CREATE NODES
	    	    CNode node;
	    	    for(int n=0 ; n < line.length ; n++){
	    	    	node = new CNode("ROI-" + (n+1));
	    	    	node = tGraph.addVertex(node, t);
	    	    	nodes.add(node);
	    	    	tGraph.setNodeLabel(node, ""+(n+1));
	    	    }
//	    	    Log.out(null, "Nodes: " + nodes.size());

	    	    // START PARSE
	    	    CEdge edge;
	    	    int sourceID = 1;
	    	    while((line = r.readNext()) != null)
	            {
			    	for(int targetID=1 ; targetID < line.length  ; targetID++){
			    		if(targetID == sourceID)
			    			continue;
			    		Log.out("line[targetID] " + line[targetID]);
			    		weight = Float.parseFloat(line[targetID]);
			    		if(weight > 0){
			    			edge = new CEdge(sourceID + "-" + targetID);
			    			edge.setWeight(weight);
			    			tGraph.addEdge(edge, nodes.get(sourceID-1), nodes.get(targetID-1), t, false);
			    		}
			    	}
			    	sourceID++;
	            }
	        }
	        catch (IOException ex){
	            System.err.println("Error loading file " + file);
	            ex.printStackTrace();
	        }
	    }
}
