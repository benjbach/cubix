package cubix.dataSets;

import com.opencsv.CSVReader;
import cubix.CubixVis;
import cubix.data.CEdge;
import cubix.data.CNode;
import cubix.data.CTime;
import cubix.data.TimeGraph;
import cubix.data.TimeGraphUtils;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class AntennaDataSet{

	
	protected File dir;
	
	
	static final String EDGE_SYMB = "->";
	
	
	protected HashMap<String, Color> groupColors = new HashMap<String, Color>();
		
	static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

	private static TimeGraphUtils<CNode, CEdge, CTime> utils;
	
	private static float maxWeight;
	private static float minWeight;

	public static boolean selfEdges = true;

	
	private static String[] antennaNames, quasarNames;


	private static boolean NAME_NODES = false;
	
	
	
	public static TimeGraph<CNode, CEdge, CTime> load(File dir){
		return load(dir, "", dir.list(new FilenameContainsFilter(new String[]{"csv"})).length, 1);
	}
	
	public static TimeGraph<CNode, CEdge, CTime> load(File dir, String nameFilter){
		return load(dir, nameFilter, dir.list(new FilenameContainsFilter(new String[]{"csv", nameFilter})).length, 1);
	}


	
	public static TimeGraph<CNode, CEdge, CTime> load(File dir, String nameFilter, int fileAmount, int steps)
	{
		// Set node and time names. 
		antennaNames = new String[]{"DA41", "DA46", "DA50", "DA56", "DV03", "DV04", "DV05", "DV07","DV11", "DV13", "DV14", "DV15", "DV17", "DV18", "DV19", "DV20","DV21", "DV22", "DV24", "DV25"};
		quasarNames = new String[]{"0132-169", "0006-063"
				  , "3c446"
				  , "0403-360"
				  , "2232+117"
				  , "0309+104"
				  , "0530+135"
				  , "0339-017"
				  , "2056-472"
				  , "2333-237"
				  , "0334-401"
				  , "3c111"
				  , "0637-752"
				  , "0538-440"
				  , "0510+180"
				  , "0238+166"
				  , "0539+145"
				  , "0519-454"
				  , "2258-279"
				  , "0522-364"
				  , "0423-013"
				  , "0607-085"
				  , "3c454.3"
				  , "0609-157"
				  , "2157-694"
				  , "0532+075"
				  , "3c84"
				  , "0457-234"
				  , "3c120"
				  , "0132-169"
				  , "0006-063"
				  , "3c446"
				  , "0403-360"
				  , "2232+117"
				  , "0309+104"
				  , "0530+135"
				  , "0339-017"
				  , "2056-472"
				  , "2333-237"
				  , "0334-401"
				  , "3c111"
				  , "0637-752"
				  , "0538-440"
				  , "0510+180"
				  , "0238+166"
				  , "0539+145"
				  , "0519-454"
				  , "2258-279"
				  , "0522-364"
				  , "0423-013"
				  , "0607-085"
				  , "3c454.3"
				  , "0609-157"
				  , "2157-694"
				  , "0532+075"
				  , "3c84"
				  , "0457-234"
				  , "3c120"};
		
		maxWeight = -1;
		minWeight = 100000;
		TimeGraph<CNode, CEdge, CTime> tGraph = new TimeGraph<CNode, CEdge, CTime>();
		utils = new TimeGraphUtils<CNode, CEdge, CTime>();
		
		if(!dir.exists()) {	
			System.err.println("[CSV] File not found: " + dir.getAbsolutePath());
			return null;
		}
		File[] files = dir.listFiles(new FilenameContainsFilter(new String[]{"csv", nameFilter}));
		
		int timeAmount = files.length; // one is .svn, the other is description file
		int loaded = 0;
		for(int t = 0; t < timeAmount && loaded < fileAmount; t+=steps)
		{
			loaded++;
			loadCSVFile(files[t], tGraph, t);	
		}
		CubixVis.WEIGHT_MAX = maxWeight;
		CubixVis.WEIGHT_MIN = minWeight;
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
	    	    CNode node;
	    	    if(line.length == 21)
	    	    	NAME_NODES  = true;
	    	    
	    	    String groupname, nodeLabel;
	    	    CTime t;
	    	    String timelabel;
				try {
					timelabel = "2012-" + (timeStep / 12)  + "-" + (timeStep % 12);
					t = new CTime(df.parse(timelabel).getTime());
					t.setDateFormat(df);
					if(NAME_NODES){
						t.setLabel(quasarNames[timeStep]);
					}else{
						t.setLabel("t_" + timeStep);
					}
				} catch (ParseException e) {
					e.printStackTrace();
					return;
				}
				
	    	    // CREATE NODES
	    	    for(int n=0 ; n < line.length-1 ; n++){
	    	    	node = new CNode("" + (n+1));
	    	    	node = tGraph.addVertex(node, t);
	    	    	nodes.add(node);
	    	    	if(!NAME_NODES){
	    	    		if(n < 9){
	    	    			tGraph.setNodeLabel(node, "0"+(n+1));
	    	    		}else
	    	    			tGraph.setNodeLabel(node, ""+(n+1));
	    	    	}else{
	    	    		tGraph.setNodeLabel(node, antennaNames[n]);
	    	    	}
	    	    }

	    	    // START PARSE
	    	    CEdge edge;
	    	    int sourceID = 1;
	    	    while((line = r.readNext()) != null)
	            {
			    	for(int targetID=1 ; targetID < line.length  ; targetID++){
			    		
					    if(line[targetID].length() != 0){
					    	weight = Float.parseFloat(line[targetID]);
					    	if(weight != 0){
					    		if(!selfEdges  && targetID == sourceID) continue;
						    	edge = new CEdge(sourceID + "->" + targetID);
				    			edge.setWeight(weight);
				    			tGraph.addEdge(edge, nodes.get(sourceID-1), nodes.get(targetID-1), t, true);
				    			maxWeight = Math.max(weight, maxWeight);
								minWeight = Math.min(weight, minWeight);
					    	}
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
