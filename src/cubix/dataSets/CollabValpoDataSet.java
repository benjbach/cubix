package cubix.dataSets;

import com.opencsv.*;
import cubix.CubixVis;
import cubix.data.CEdge;
import cubix.data.CNode;
import cubix.data.CTime;
import cubix.data.TimeGraph;
import cubix.data.TimeGraphUtils;
import cubix.helper.Log;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class CollabValpoDataSet{

	
	protected File dir;
	
	protected HashMap<String, Color> groupColors = new HashMap<String, Color>();
		
	static DateFormat df = new SimpleDateFormat("yyyy");

	private static TimeGraphUtils<CNode, CEdge, CTime> utils;


	private static float maxWeight;
	private static float minWeight;


	public static boolean selfEdges = true;

	private static String disc;

	
	public static TimeGraph<CNode, CEdge, CTime> load(File file, String discipline)
	{
		disc = discipline;
		maxWeight = -1;
		minWeight = 100000;
		TimeGraph<CNode, CEdge, CTime> tGraph = new TimeGraph<CNode, CEdge, CTime>();
		utils = new TimeGraphUtils<CNode, CEdge, CTime>();
				
		loadCSVFile(file, tGraph);	
		
		CubixVis.WEIGHT_MAX = maxWeight;
		CubixVis.WEIGHT_MIN = minWeight;
		return tGraph;
	}
	
	
	
	private static void loadCSVFile(File file, TimeGraph<CNode, CEdge, CTime> tGraph) 
	{
		
		HashMap<String,CNode> vertices = new HashMap<String, CNode>();
	   
	        float weight = 0;
	        try {
	    	    CSVReader r = new CSVReader(new FileReader(file), '\t');
	    	    String[] line = r.readNext();
	    	    CTime t = null;
	    	    String timelabel;

	    		    	    // START PARSE
	    	    CEdge edge;
	    	    CNode source, target;
	    	    boolean found = false;
	    	    while((line = r.readNext()) != null)
	            {
	    	    	if(!line[3].equals(disc))
	    	    		continue;

	    	    	weight = Float.parseFloat(line[2]);
	    	    	if(weight > 0){
		    	    	timelabel = line[4];
		    	    	found = false;
		    	    	for(CTime ts : tGraph.getTimes())
		    	    	{
		    	    		if(ts.getLabel().equals(timelabel))
		    	    		{
		    	    			found = true;
		    	    			t = ts;
		    	    			break;
		    	    		}
		    	    	}
		    	    	if(!found){
		    	    		t = new CTime(df.parse(timelabel).getTime());
		    	    		t.setDateFormat(df);
		    	    		tGraph.createSliceGraph(t);
		    	    	}
		    	    	
		    	    	if(vertices.containsKey(line[0]))
		    	    		source = vertices.get(line[0]);
		    	    	else{
		    	    		source = new CNode(line[0]);
		    	    		vertices.put(line[0], source);
		    	    		tGraph.addVertex(source, t);
		    	    	}
		    	    	if(vertices.containsKey(line[1]))
		    	    		target = vertices.get(line[1]);
		    	    	else{
		    	    		target = new CNode(line[1]);
		    	    		vertices.put(line[1], target);
		    	    		tGraph.addVertex(target, t);
		    	    	}
		    	    
						edge = new CEdge(source.getID() + "--" + target.getID());
				    	edge.setWeight(weight);
				    	tGraph.addEdge(edge, source, target, t, true);
				    	edge = new CEdge(target.getID() + "--" + source.getID());
				    	edge.setWeight(weight);
				    	tGraph.addEdge(edge, target, source, t, true);
				    	maxWeight = Math.max(weight, maxWeight);
						minWeight = Math.min(weight, minWeight);
					 }
			    }
	        }
	        catch (IOException ex){
	            System.err.println("Error loading file " + file);
	            ex.printStackTrace();
	        } catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
}
