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


public class UNHCRDataSet{

	
	private static int GROUPS_PER_DATA_SAMPLE = 2;
	protected static String dbName = "UNHCR";
	protected static String user = "aviz";
	protected static String pw = "inria";
	
	public static String TABLE = "REFUGEES";
	

	
	static DateFormat df = new SimpleDateFormat("yyyy");
	static String[] queryCountries = new String[]{};
	private static int edgeMin = 0;
	
	public static TimeGraph<CNode,CEdge,CTime> load(String[] countries){
		queryCountries = countries;
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
					countryListOrigin += " OR CountryOrigin LIKE '" + c + "%'"; 
					countryListDest += " OR CountryDestination LIKE '" + c + "%'"; 
				}
				countryListOrigin = countryListOrigin.substring(4);
				countryListDest = countryListDest.substring(4);
			}

			// LOAD TIME SLICES
			int yearMin = 1950;
			int yearMax = 2013;
			int timeCount = 0;
			CTime t;
			int numMax = 0; 
			int numMin = 99999999;
			for(int time = yearMin; time <= yearMax; time++)
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
				String query;
				if(queryCountries.length > 0){
					query = "" +
					"SELECT * " +
					"FROM Refugees  " +
					"WHERE Year='"+ time +"' " +
					" 		AND( " + countryListDest + ")" +
					"		AND( " + countryListOrigin + ");";
				}
				else{
					query = "" +
					"SELECT * " +
					"FROM Refugees  " +
					"WHERE Year='"+ time +"' " + 
					" 		AND( num > '" + edgeMin + "');";
				}
				Log.out(null, "Query: " + query);

				ResultSet res = stmt.executeQuery(query);
			           
			    HashMap<String, HashSet<CNode>> collabs = new HashMap<String, HashSet<CNode>>();
			    HashSet<CNode> authors;
			    CNode v1, v2;
			    int num;
			    while(res.next()) 
			    {
			    	// CREATE VERTICES
			    	v1 = new CNode(res.getString("CountryOrigin"));
			    	v1 = tGraph.addVertex(v1, t);
			    	tGraph.setNodeLabel(v1, v1.getID());
			    	v2 = new CNode(res.getString("CountryDestination"));
			    	v2 = tGraph.addVertex(v2, t);
			    	tGraph.setNodeLabel(v2, v2.getID());

			    	num = Integer.parseInt(res.getString("Num"));
			         
			    	CEdge e = new CEdge(utils.createEdgeID(v1.getID(), v2.getID(), t, false));
			    	e.setWeight(num);
			    	numMax = Math.max(numMax, num);
			    	numMin = Math.min(numMin, num);
			    	tGraph.addEdge(e, v1, v2, t, false);
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
