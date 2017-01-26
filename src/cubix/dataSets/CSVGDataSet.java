package cubix.dataSets;

import java.awt.Color;
import java.awt.FileDialog;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import cubix.CubixVis;
import cubix.data.CEdge;
import cubix.data.CNode;
import cubix.data.CTime;
import cubix.data.TimeGraph;
import cubix.data.TimeGraphUtils;
import cubix.helper.Log;
import edu.uci.ics.jung.graph.Graph;

import com.opencsv.*;

public class CSVGDataSet{

	
	public static final String TAB_NODES = "node table";
	public static final String COL_GROUP = "node groups";
	public static final String GRAPH_FILE_EXT = "csvg"; //CSV-Geaph
	public static final String DLM = "!"; //CSV delimiter
		
	
	protected File dir;
	protected ArrayList<Color> possibleColors = new ArrayList<Color>();
	private HashMap<Color, String> colors;
	protected static HashMap<String, Color> groupColors = new HashMap<String, Color>();
	protected static HashMap<Color, Integer> colorShape = new HashMap<Color, Integer>();
	protected static int RECTANGLE = 0;
	protected static int CIRCLE = 1;
	
	protected static String header[];
	private static float maxWeight;
	private static float  minWeight;
	
	private static TimeGraphUtils<CNode, CEdge, CTime> utils; 
	
	static DateFormat df1 = new SimpleDateFormat("yyyy");
	static DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");


	public static TimeGraph loadData(File file) 
	{
		maxWeight = -1;
		minWeight = 100000;
		TimeGraph<CNode, CEdge, CTime> tGraph = new TimeGraph<CNode, CEdge, CTime>();
		utils = new TimeGraphUtils<CNode, CEdge, CTime>();
		
		int loaded = 0;
		loadCSVFile(file, tGraph);	
		CubixVis.WEIGHT_MAX = maxWeight;
		CubixVis.WEIGHT_MIN = minWeight;
		return tGraph;
	}
	
	private static void loadCSVFile(File file, TimeGraph<CNode, CEdge, CTime> tGraph) 
	{
		
		HashMap<String,CNode> vertices = new HashMap<String, CNode>();
	        ArrayList<CNode> nodes = new ArrayList<CNode>();

	        float weight = 0;
	        try {
	    	    CSVReader r = new CSVReader(new FileReader(file));
	    	    String[] line = r.readNext();
	    	    String groupname, nodeLabel;
	    	    CTime t = null;
	    	    String timelabel;
	    	    CNode source, target;
	    	    boolean found;
	    	    CEdge edge;
	    	    while((line = r.readNext()) != null)
	    	    {
	    	    	if(line[0].contains("_"))
	    	    		timelabel = (1950 + line[0].split("_")[1]) + "";
	    	    	else if(line[0].contains("_")){
	    	    		timelabel = line[0].split("-")[0];
	    	    	}
	    	    	timelabel = line[0];
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
	    	    		try{
	    	    			t = new CTime(df1.parse(timelabel).getTime());
//	    	    			t.setDateFormat(l);
	    	    			t.setLabel(timelabel);
	    	    		}catch(ParseException ex){
	    	    			try{
	    	    				t = new CTime(df2.parse(timelabel).getTime());
	    	    				t.setLabel(timelabel);
	    	    			}catch(ParseException ex2){
	    	    				t = new CTime(System.currentTimeMillis());
	    	    				t.setLabel(timelabel);
	    	    			}
	    	    		}
	    	    		tGraph.createSliceGraph(t);
	    	    	}
	    	    	
	    	    	if(vertices.containsKey(line[1]))
	    	    		source = vertices.get(line[1]);
	    	    	else{
	    	    		source = new CNode(line[1]);
	    	    		vertices.put(line[1], source);
	    	    		tGraph.addVertex(source, t);
	    	    		tGraph.setNodeLabel(source, line[1]);
	    	    	}
	    	    	if(vertices.containsKey(line[2]))
	    	    		target = vertices.get(line[2]);
	    	    	else{
	    	    		target = new CNode(line[2]);
	    	    		vertices.put(line[2], target);
	    	    		tGraph.addVertex(target, t);
	    	    		tGraph.setNodeLabel(target, line[2]);
	    	    	}
	    	    	
	    	    	weight = Float.parseFloat(line[3]);
	    	    	  
					edge = new CEdge(source.getID() + "--" + target.getID());
			    	edge.setWeight(weight);
			    	tGraph.addEdge(edge, source, target, t, true);
			    	edge = new CEdge(target.getID() + "--" + source.getID());
			    	edge.setWeight(weight);
			    	tGraph.addEdge(edge, target, source, t, true);
			    	maxWeight = Math.max(weight, maxWeight);
					minWeight = Math.min(weight, minWeight);
				 }
	        }catch (IOException ex){
		        System.err.println("Error loading file " + file);
		        ex.printStackTrace();
		    } 
	   }
	
	
	
	public static void exportGraph(TimeGraph graph, JFrame frame, String fileName, boolean singleFile)
	{
		File f;
		File d = new File("export");
		if(!d.exists()){
			d.mkdir();
		}
		if(singleFile)
			f = new File(d.getAbsolutePath() + "/" + fileName + ".csv" );
		else
			f = new File(d.getAbsolutePath() + "/" + fileName );
			
		System.out.println("[CSVGDataSet] export file: " + f.getAbsolutePath());
		boolean success = exportGraph(f, graph, singleFile);
		
		if(!fileName.equals(""))
			return;
		if(success){
			JOptionPane.showMessageDialog(frame, 
			"Export successful");
		}else{
			JOptionPane.showMessageDialog(frame, 
			"Export faliure");
		}
	}
	
	
	public static void exportOBJ(TimeGraph graph, JFrame frame, String fileName, boolean singleFile)
	{
		File f;
		File d = new File("export");
		if(!d.exists()){
			d.mkdir();
		}
		if(singleFile)
			f = new File(d.getAbsolutePath() + "/" + fileName + ".csv" );
		else
			f = new File(d.getAbsolutePath() + "/" + fileName );
			
		System.out.println("[CSVGDataSet] export file: " + f.getAbsolutePath());
		boolean success = exportOBJ(f, graph, singleFile);
		
		if(!fileName.equals(""))
			return;
		if(success){
			JOptionPane.showMessageDialog(frame, 
			"Export successful");
		}else{
			JOptionPane.showMessageDialog(frame, 
			"Export faliure");
		}
	}
	
	//////////////////////
	/// EXPORT/IMPPORT ///
	//////////////////////
	
	/** Exports the given graph into the passed directory. For each time slices
	 * a file of the format <code>time_</code><em>number<em> is created.*/
	protected static boolean exportGraph(File f, TimeGraph<CNode, CEdge, CTime> timeGraph, boolean singleFile) 
	{
		int fields = 4;
		
		header = new String[fields];
		header[0] = "time";
		header[1] = "n1";
		header[2] = "n2";
		header[3] = "weight";
		
		
		CSVWriter fw = null;
		
		if(singleFile){
			try {
				fw = new CSVWriter(new FileWriter(f));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			fw.writeNext(header);
			Graph<CNode, CEdge> g;

			try {
				String[] line;
				for(CTime t : timeGraph.getTimes())
				{
					g = timeGraph.getGraph(t);
					for(CEdge e : g.getEdges())
					{
						// WRITE GRAPH ATTRIBUTES
						line = new String[fields];
						line[0] = t.getLabel();
						line[1] = timeGraph.getVertexLabel(g.getEndpoints(e).getFirst());
						line[2] = timeGraph.getVertexLabel(g.getEndpoints(e).getSecond());
						line[3] = e.getWeight() + "";
						fw.writeNext(line);
					}
				}
				fw.close();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}else
		{
			if(!f.exists())
				f.mkdir();
			
			String[] line;
			for(CTime t : timeGraph.getTimes())
			{
				try {
					File ft = new File(f.getAbsoluteFile() + "/" + t.getLabel()+ ".csv");
					fw = new CSVWriter(new FileWriter(ft));
					fw.writeNext(header);
					Graph<CNode, CEdge> g;
					g = timeGraph.getGraph(t);
					for(CEdge e : g.getEdges())
					{
						// WRITE GRAPH ATTRIBUTES
						line = new String[fields];
						line[0] = t.getLabel();
						line[1] = timeGraph.getVertexLabel(g.getEndpoints(e).getFirst());
						line[2] = timeGraph.getVertexLabel(g.getEndpoints(e).getSecond());
						line[3] = e.getWeight() + "";
						fw.writeNext(line);
					}
					fw.flush();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		return true;
	}
	
	/** Exports the given graph into the passed directory. For each time slices
	 * a file of the format <code>time_</code><em>number<em> is created.*/
	protected static boolean exportOBJ(File f, TimeGraph<CNode, CEdge, CTime> timeGraph, boolean singleFile) 
	{
		int fields = 4;
		
		header = new String[fields];
		header[0] = "time";
		header[1] = "n1";
		header[2] = "n2";
		header[3] = "weight";
		
		
		CSVWriter fw = null;
		
		if(singleFile){
			try {
				fw = new CSVWriter(new FileWriter(f));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			fw.writeNext(header);
			Graph<CNode, CEdge> g;

			try {
				String[] line;
				for(CTime t : timeGraph.getTimes())
				{
					g = timeGraph.getGraph(t);
					for(CEdge e : g.getEdges())
					{
						// WRITE GRAPH ATTRIBUTES
						line = new String[fields];
						line[0] = t.getLabel();
						line[1] = timeGraph.getVertexLabel(g.getEndpoints(e).getFirst());
						line[2] = timeGraph.getVertexLabel(g.getEndpoints(e).getSecond());
						line[3] = e.getWeight() + "";
						fw.writeNext(line);
					}
				}
				fw.close();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}else
		{
			if(!f.exists())
				f.mkdir();
			
			String[] line;
			for(CTime t : timeGraph.getTimes())
			{
				try {
					File ft = new File(f.getAbsoluteFile() + "/" + t.getLabel()+ ".csv");
					fw = new CSVWriter(new FileWriter(ft));
					fw.writeNext(header);
					Graph<CNode, CEdge> g;
					g = timeGraph.getGraph(t);
					for(CEdge e : g.getEdges())
					{
						// WRITE GRAPH ATTRIBUTES
						line = new String[fields];
						line[0] = t.getLabel();
						line[1] = timeGraph.getVertexLabel(g.getEndpoints(e).getFirst());
						line[2] = timeGraph.getVertexLabel(g.getEndpoints(e).getSecond());
						line[3] = e.getWeight() + "";
						fw.writeNext(line);
					}
					fw.flush();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		return true;
	}
	
	
	
}
