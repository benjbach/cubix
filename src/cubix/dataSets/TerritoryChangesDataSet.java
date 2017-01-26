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


public class TerritoryChangesDataSet{

	
	private static int GROUPS_PER_DATA_SAMPLE = 2;
	protected static String dbName = "COW";
	protected static String user = "aviz";
	protected static String pw = "inria";
	
	public static String TABLE = "territorial_changes";
	
	protected static int _timeSteps = 1;

	
	static DateFormat df = new SimpleDateFormat("yyyy");
	static String[] queryCountries = new String[]{};
	private static int edgeMin = 0;
	private static int time_min = 1990;
	private static int time_max = 2000;
	
	public static TimeGraph<CNode,CEdge,CTime> load(String[] countries){
		queryCountries = countries;
		return load();
	}
	
	public static TimeGraph<CNode,CEdge,CTime> load(String[] countries, int timeSteps){
		queryCountries = countries;
		_timeSteps = Math.min(timeSteps, _timeSteps);
		return load();
	}
	public static TimeGraph<CNode,CEdge,CTime> load(String[] countries, int timeMin, int timeMax){
		queryCountries = countries;
		time_min = timeMin;
		time_max = timeMax;
		return load();
	}
	
	public static TimeGraph<CNode,CEdge,CTime> load(String[] countries, int timeMin, int timeMax, int timeSteps){
		queryCountries = countries;
		time_min = timeMin;
		time_max = timeMax;
		_timeSteps = timeSteps;
		return load();
	}
	
	public static TimeGraph<CNode,CEdge,CTime> load(int min){
		edgeMin = min;
		return load();
	}

	
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
			
			String countryListOrigin = "";
			String countryListDest = "";
			if(queryCountries.length > 0){
				for(String c : queryCountries){
					countryListOrigin += " OR gainer LIKE '" + c + "%'"; 
					countryListDest += " OR loser LIKE '" + c + "%'"; 
				}
				countryListOrigin = countryListOrigin.substring(4);
				countryListDest = countryListDest.substring(4);
			}

			// LOAD TIME SLICES
			int timeCount = 0;
			CTime t;
			int numMax = 0; 
			int numMin = 99999999;
			for(int time = time_min; time <= time_max; time+=_timeSteps)
			{
				try {
					t = new CTime(df.parse(time + "").getTime());
				} catch (ParseException e) {
					e.printStackTrace();
					return null;
				}
				timeCount++;
				
				/// CLEAN GRAPH TIME
				// LOAD NEW DATA
				Statement stmt = conn.createStatement();
				String query = "";
				if(queryCountries.length > 0){
					query = "" +
					"SELECT * " +
					"FROM " + TABLE + " " +
					"WHERE (" + countryListDest + ")" +
					"	 	AND( " + countryListOrigin + ")" +
					"		AND( year = " + time + ");";
				}
				Log.out(null, "Query: " + query);

				ResultSet res = stmt.executeQuery(query);
			           
			    HashMap<String, HashSet<CNode>> collabs = new HashMap<String, HashSet<CNode>>();
			    HashSet<CNode> authors;
			    CNode v1, v2;
			    int weight;
			    while(res.next()) 
			    {
			    	// CREATE VERTICES
			    	v1 = new CNode(res.getString("gainer"));
			    	v1 = tGraph.addVertex(v1, t);
			    	tGraph.setNodeLabel(v1, v1.getID());
			    	v2 = new CNode(res.getString("loser"));
			    	v2 = tGraph.addVertex(v2, t);
			    	tGraph.setNodeLabel(v2, v2.getID());

			    	weight = res.getInt("area");

			    	CEdge e = new CEdge(utils.createEdgeID(v1.getID(), v2.getID(), t, true));
			    	e.setWeight(weight);
			    	numMax = Math.max(numMax, weight);
			    	numMin = Math.min(numMin, weight);
			    	tGraph.addEdge(e, v1, v2, t, true);
				}
//				tGraph.setTimeLabel(t, time + "");
			}        

			CubixVis.WEIGHT_MAX = numMax;
			CubixVis.WEIGHT_MIN = numMin;
				
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
