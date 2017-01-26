package cubix.dataSets;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

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


public class InfovisDataSet{

		
	protected static HashMap<String, String> vertexNames = new HashMap<String, String>();
	
	private static int GROUPS_PER_DATA_SAMPLE = 2;
	protected static String dbName = "infovisCollab";
	protected static String user = "aviz";
	protected static String pw = "inria";
	
	public static String TABLE_AUTHORS = "author";
	
	protected static HashMap<String, Color> groupColors = new HashMap<String, Color>();
	protected static HashMap<CNode, Color> nodeColors = new HashMap<CNode, Color>();

	protected static HashMap<Integer, Integer>[] edgeWeights;
	
	protected String TAB_EDGE = "edgeAttributeTable";
	protected String COL_WEIGHT = "edgeAttributeWeight";
	private static HashMap<String, HashSet<String>> groups = new HashMap<String, HashSet<String>>();
	
	static SimpleDateFormat df = new SimpleDateFormat("yyyy");
	private static ArrayList<String> teams = new ArrayList<String>();
	private static ArrayList<String> usedTeams = new ArrayList<String>();


	
	public static TimeGraph<CNode,CEdge,CTime> load(JFrame frame)
	{
		/// INITIATE DATA BASE CONNECTION
		dbName = System.getProperty("db", "jdbc:mysql://localhost/" + dbName);
        user = System.getProperty("dbuser", user);
        pw = System.getProperty("dbpass", pw);
        
        TimeGraph<CNode,CEdge,CTime> tGraph = new TimeGraph<CNode,CEdge,CTime>();
        TimeGraphUtils<CNode,CEdge,CTime> utils = new TimeGraphUtils<CNode,CEdge,CTime>();
        
        String s = (String)JOptionPane.showInputDialog(
                frame,
                "Insert author names, sepeated by comma:",
                "Author names",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                "");

        boolean only = false;
        if(s.startsWith("ONLY")){
        	only = true;
        }
        s = s.replace("ONLY ", "");
        
        String[] names = new String[0];
		if ((s != null) && (s.length() > 0)) {
			names = s.split(",");
		}
        
        
		try {
			// PREPARE CONNECTION
			Connection conn = DriverManager.getConnection(dbName, user, pw);

			CTime t;
			float maxWeight = 0;
			
			Statement stmt = conn.createStatement();
			
			String authors = "";
			if(only){
				authors = " WHERE year>2001 AND (";
				for(String a : names){
					authors += " author1_name LIKE '%" + a + "%' OR ";
				}
				authors = authors.substring(0, authors.length()-3);
				authors += ") AND (";
				for(String a : names){
					authors += " author2_name LIKE '%" + a + "%' OR ";
				}
				authors = authors.substring(0, authors.length()-3);
				authors += ")";
			}else{
				authors = " WHERE ";
				for(String a : names){
					authors += " author1_name LIKE '%" + a + "%' OR ";
				}
				authors = authors.substring(0, authors.length()-3);
			}
		    
			
			String query = "SELECT * FROM coauthor " + authors;
			System.out.println("[RawebDataSet] query:" + query);
			ResultSet res = stmt.executeQuery(query);
		           
			// CREATE YEARS
			for(int y=1995 ; y <= 2014 ; y++){
				try {
					t = new CTime(df.parse(y+"").getTime());
					t.setDateFormat(df);
					t.setLabel(y+"");
				} catch (ParseException e) {
					e.printStackTrace();
					return null;
				}
				tGraph.createSliceGraph(t);
			}
						
			
		    CNode source, target;
		    String sourceId, targetId;
		    CEdge edge;
		    HashMap<String,CNode> nodes = new HashMap<String,CNode>();
		    int LIM = 2000;
		    int count = 0;
		    String y;
		    String[] l;
		    while(res.next() && count < LIM){
		    	count++;
		    	t = null;
		    	y = res.getString("year");
    			for(CTime t2 : tGraph.getTimes()){
		    		if(t2.getLabel().equals(y)){
		    			t = t2;
		    		}
		    	}
		    	sourceId = res.getString("author1_name");
		    	if(nodes.containsKey(sourceId))
		    		source = nodes.get(sourceId);
		    	else{
		    		source = new CNode(sourceId);
		    		nodes.put(sourceId, source);
		    		tGraph.addVertex(source, t);
		    		l = sourceId.split(" ");
		    		tGraph.setNodeLabel(source, l[l.length-1]);
		    	}
		    	targetId = res.getString("author2_name");
		    	if(nodes.containsKey(targetId))
		    		target = nodes.get(targetId);
		    	else{
		    		target = new CNode(targetId);
		    		nodes.put(targetId, target);
		    		tGraph.addVertex(target, t);
		    		l = targetId.split(" ");
		    		tGraph.setNodeLabel(target, l[l.length-1]);
		    	}
		    	float weight = 1;
		    	CEdge e = tGraph.getGraph(t).findEdge(source, target);
		    	if(e != null){
		    		e.setWeight(e.getWeight() +1);
			    	maxWeight = Math.max(maxWeight, e.getWeight());
			    	e = tGraph.getGraph(t).findEdge(target, source);
			    	e.setWeight(e.getWeight() +1);			    
			    	maxWeight = Math.max(maxWeight, e.getWeight());
		    	}else{
		    		edge = new CEdge(sourceId + "-" + targetId);
		    		edge.setWeight(1);
		    		tGraph.addEdge(edge, source, target, t, true);
		    		edge = new CEdge(targetId + "-" + sourceId);
		    		tGraph.addEdge(edge, target, source, t, true);
		    		edge.setWeight(1);
		    	}
		    }
		    
		    
		    if(!only)
		    {
		    	// CRAWL NEIGHBORS
		    	for(int i=0; i<1; i++)
		    	{
		    		authors = " (";
		    		for(CNode n : tGraph.getVertices()){
		    			authors += "author1_name LIKE '" + n.getID() + "' OR "; 
		    		}
		    		authors = authors.substring(0, authors.length()-3);
		    		authors += ") AND (";
		    		for(CNode n : tGraph.getVertices()){
		    			authors += "author2_name LIKE '" + n.getID() + "' OR "; 
		    		}
		    		authors = authors.substring(0, authors.length()-3);
		    		authors += ")";
		    		query = "SELECT * FROM coauthor WHERE " + authors;
		    		Log.out(query);
		    		res = stmt.executeQuery(query);
		    		while(res.next()){
		    			t = null;
		    			y = res.getString("year");
		    			for(CTime t2 : tGraph.getTimes()){
		    				if(t2.getLabel().equals(y)){
		    					t = t2;
		    				}
		    			}
		    			sourceId = res.getString("author1_name");
		    			if(nodes.containsKey(sourceId))
		    				source = nodes.get(sourceId);
		    			else{
		    				source = new CNode(sourceId);
		    				nodes.put(sourceId, source);
		    				tGraph.addVertex(source, t);
		    				l = sourceId.split(" ");
				    		tGraph.setNodeLabel(source, l[l.length-1]);
				    	}
		    			targetId = res.getString("author2_name");
		    			if(nodes.containsKey(targetId))
		    				target = nodes.get(targetId);
		    			else{
		    				target = new CNode(targetId);
		    				nodes.put(targetId, target);
		    				l = targetId.split(" ");
				    		tGraph.setNodeLabel(target, l[l.length-1]);
		    			}
		    			float weight = 1;
		    			CEdge e = tGraph.getGraph(t).findEdge(source, target);
		    			if(e != null){
		    				e.setWeight(e.getWeight() +1);
		    				maxWeight = Math.max(maxWeight, e.getWeight());
		    				e = tGraph.getGraph(t).findEdge(target, source);
		    				e.setWeight(e.getWeight() +1);			    
		    				maxWeight = Math.max(maxWeight, e.getWeight());
		    			}else{
		    				edge = new CEdge(sourceId + "-" + targetId);
		    				edge.setWeight(1);
		    				tGraph.addEdge(edge, source, target, t, true);
		    				edge = new CEdge(targetId + "-" + sourceId);
		    				tGraph.addEdge(edge, target, source, t, true);
		    				edge.setWeight(1);
		    			}
		    		}
		    	}
		    }
					
		    
			CubixVis.WEIGHT_MAX = maxWeight;
			CubixVis.WEIGHT_MIN = 0;
			conn.close();
		}
		catch (SQLException e) {
			System.err.println("[RawebDataSet] No connection to data base");
			e.printStackTrace();
		}		
		return tGraph;
	}
	
	 
	public static String getDataSetName() {
		String name = "";
		for(String s : usedTeams) name+= s + ", ";
		return name;
	}
	
}
