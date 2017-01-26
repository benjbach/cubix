package cubix.dataSets;

import java.awt.Color;
import java.awt.FileDialog;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
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

public class NewComb{

	
	public static final String GRAPH_FILE_EXT = "csv"; //CSV-Geaph
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
	private static boolean init = true;

	static CNode[] nodes = new CNode[0];
	
	static String[] names={
		"Alma",
		"Friedrich",
		"Marc",
		"Ernest",
		"George",
		"Bertholt",
		"Maria",
		"Ana",
		"Salvador",
		"Pablo",
		"Amedeo",
		"Max",
		"Milton",
		"Dino",
		"Jenna",
		"Archie",
		"Dell",
		"Erika",
		"Zachary",
		"Irena",
		"Suk",
		"Vernice",
		"Laveta",
	};

	public static TimeGraph loadData(File dir) 
	{
		maxWeight = -1;
		minWeight = 100000;
		TimeGraph<CNode, CEdge, CTime> tGraph = new TimeGraph<CNode, CEdge, CTime>();
		utils = new TimeGraphUtils<CNode, CEdge, CTime>();
		
		if(!dir.exists()){
			Log.err("Directory does not exist");
		}

		File[] files = dir.listFiles(new FilenameContainsFilter(new String[]{"csv"}));
		
		for(int i = 0; i < files.length; i++)
		{
			loadCSVFile(files[i], tGraph, i);	
		}
		CubixVis.WEIGHT_MAX = maxWeight;
		CubixVis.WEIGHT_MIN = minWeight;
		return tGraph;
	}
	
	
	private static void loadCSVFile(File file, TimeGraph<CNode, CEdge, CTime> tGraph, int time)
	{
	        try {
	    	    CSVReader r = new CSVReader(new FileReader(file), '\t');
	    	    String[] line;
	    	    CTime t = null;
	    	    String timelabel;
	    	    CNode source, target;
	    	    CEdge edge, edge2;
	    	    int nodeNum =0;
	    	    float weight = 0;
	    	    String[] cells;
	    	    
	    	    // Create nodes
	    	    int row=0, col=0;
	    	    timelabel = "" + time;
	    	    t = new CTime(System.currentTimeMillis());
	    	    tGraph.createSliceGraph(t);
	    	    t.setLabel(timelabel);
	    	    row=0;
	    	    while((line = r.readNext()) != null)
	    	    {
	    	    	if(line.equals("")) break; 
	    	    	line[0] = line[0].replace("  ", ",");
	    	    	line[0] = line[0].replace(" ", ",");
	    	    	line[0] = line[0].substring(1);
	    	    	cells = line[0].split(",");

	    	    	if(init){
	    	    		nodeNum = cells.length;
	    	    		nodes = new CNode[nodeNum];
	    	    		for(int i=0 ; i<nodeNum ; i++){
	    	    			nodes[i] = new CNode(i + "");
	    	    			tGraph.addVertex(nodes[i], t);
	    	    			tGraph.setNodeLabel(nodes[i], names[i]);
	    	    		}
	    	    		init=false;
	    	    	}
	    	    	
	    	    	
	    	    	col=-1;
	    	    	for(String w : cells)
	    	    	{
	    	    		col++;
	    	    		if(row == col) continue;
	    	    		if(w.equals("")) continue;
	    	    		source = nodes[row];
	    	    		target = nodes[col];
	    	    		weight = 18 - Float.parseFloat(w);
	    	    		Log.out(weight + "");
	    	    		edge = new CEdge(source.getID() + "--" + target.getID());
	    	    		tGraph.addEdge(edge, source, target, t, true);
	    	    		edge.setWeight(weight);
	    	    		if(row>col){
		    	    		edge2 = tGraph.getGraph(t).findEdge(target, source);
		    	    		edge2.setWeight2(weight);
		    	    		edge.setWeight2(edge2.getWeight());
		    	    	}
	    	    	}
	    	    	row++;
				 }
	        }catch (IOException ex){
		        System.err.println("Error loading file " + file);
		        ex.printStackTrace();
		    } 
	        maxWeight = 17;
	    	minWeight = 0;
	   }
	
	
	
	public static void exportGraph(TimeGraph graph, JFrame frame, String fileName)
	{
		File f;
		File d = new File("export");
		if(!d.exists()){
			d.mkdir();
		}
		f = new File(d.getAbsolutePath() + "/" + fileName + ".csv" );
		
		System.out.println("[CSVGDataSet] export file: " + f.getAbsolutePath());
		boolean success = exportGraph(f, graph);
		
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
	protected static boolean exportGraph(File f, TimeGraph<CNode, CEdge, CTime> timeGraph) 
	{
		int fields = 8;
		
		header = new String[fields];
		header[0] = "time";
		header[1] = "n1";
		header[2] = "n2";
		header[3] = "weight";
		
		CSVWriter fw = null;
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
	}

}
