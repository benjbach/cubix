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

import cubix.CubixVis;
import cubix.data.CEdge;
import cubix.data.CNode;
import cubix.data.CTime;
import cubix.data.TimeGraph;
import cubix.data.TimeGraphUtils;
import cubix.helper.Log;

import edu.uci.ics.jung.graph.Graph;


public class LieveDataSet{

	
	protected static String dbName = "lieveletters";
	protected static String user = "lieve";
	protected static String pw = "vanhoof";
	
	public static String TABLE = "relations";
	
	
	
	public static TimeGraph<CNode,CEdge,CTime> load()
	{
		/// INITIATE DATA BASE CONNECTION
		dbName = System.getProperty("db", "jdbc:mysql://localhost/" + dbName);
        user = System.getProperty("dbuser", user);
        pw = System.getProperty("dbpass", pw);
        
        TimeGraph<CNode,CEdge,CTime> tGraph = new TimeGraph<CNode,CEdge,CTime>();
        TimeGraphUtils<CNode,CEdge,CTime> utils = new TimeGraphUtils<CNode,CEdge,CTime>();
        
		try {
			// PREPARE CONNECTION
			Connection conn = DriverManager.getConnection(dbName, user, pw);
			
			// GET CONNECTION TYPES
			Statement stmt = conn.createStatement();
			String query = "" +
					"SELECT type " +
					"FROM relations " +
					"GROUP BY type";
			
			ResultSet res = stmt.executeQuery(query);
			ArrayList<String> relationTypes = new ArrayList<String>();
			while(res.next()) 
		    {	
				relationTypes.add(res.getString(0));
		    }
			
			
			// LOAD TYPE SLICES
			CTime t;
			int typeCount=0;
			for(String type : relationTypes)
			{
				t = new CTime(typeCount++);
				
				/// CLEAN GRAPH TIME
				// LOAD NEW DATA
				stmt = conn.createStatement();
				query = "" +
					"SELECT * " +
					"FROM relations " +
					"WHERE type = " + type + ");";
				
				res = stmt.executeQuery(query);
			           
			    HashMap<String, HashSet<CNode>> collabs = new HashMap<String, HashSet<CNode>>();
			    HashSet<CNode> authors;
			    CNode v1, v2;
			    int weight;
			    while(res.next()) 
			    {
			    	// CREATE VERTICES
			    	v1 = new CNode(res.getString("source"));
			    	v1 = tGraph.addVertex(v1, t);
			    	tGraph.setNodeLabel(v1, v1.getID());
			    	v2 = new CNode(res.getString("target"));
			    	v2 = tGraph.addVertex(v2, t);
			    	tGraph.setNodeLabel(v2, v2.getID());

			    	CEdge e = new CEdge(utils.createEdgeID(v1.getID(), v2.getID(), t, true));
			    	tGraph.addEdge(e, v1, v2, t, true);
			    	
				}
			}        

			CubixVis.WEIGHT_MAX = 1;
			CubixVis.WEIGHT_MIN = 0;
				
			conn.close();
		}
		catch (SQLException e) {
			System.err.println("[RawebDataSet] No connection to data base");
			e.printStackTrace();
		}
		
		return tGraph;
	}
	
	private static String getTimeLabel(long t){
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(t);
		return c.get(Calendar.YEAR) + "";
	}
	public static String getDataSetName() 
	{
		return "UNHCR";
	}
	
	

}
