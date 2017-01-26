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


public class RawebDataSet{

	
	static private String[] babyNames = new String[]{ 
		"Nathan",
		"Louise",
		"Lea",	 
		"Lucas",
		"Emma",
		"Enzo",
		"Camille",	
		"Sarah",	 
		"Chloe",	
		"Hugo",
		"Gabriel",
		"Ethan",
		"Mathi",
		"Manon",	
		"Jules",
		"Ines",	 
		"Lola",	
		"Louis",
		"Jade",	 
		"Leo",
		"Arno",
		"Fritz",
		"Hugo",
		"Lena",
		"Anton",
		"Emil",
		"Rosa",
		"Egon",
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
		"Kyong",
		"Deon",
		"Michele",
		"Jack",
		"Rico",
		"Adan",
		"Lorinda",
		"Anissa",
		"Brittney",
		"Marybelle",
		"Chana",
		"Timothy",
		"Tatiana",
		"Trisha",
		"Raeann",
		"Karima",
		"Kandace",
		"Barbra",
		"Murray",
		"Aleisha",
		"Nettie",
		"Steve",
		"Octavia",
		"Molly",
		"Vincent",
		"Nikki",
		"Stanton",
		"Rolando",
		"Loren",
		"Shalon",
		"Malissa",
		"Lyndsay",
		"Irving",
		"Jaleesa",
		"Madalene",
		"Winifred",
		"Maranda"
		};

	
	protected static HashMap<String, String> vertexNames = new HashMap<String, String>();
	
	private static int GROUPS_PER_DATA_SAMPLE = 2;
	protected static String dbName = "raweb";
	protected static String user = "aviz";
	protected static String pw = "inria";
	
	public static String TABLE_AUTHORS = "Authors";
	
	protected static HashMap<String, Color> groupColors = new HashMap<String, Color>();
	protected static HashMap<CNode, Color> nodeColors = new HashMap<CNode, Color>();

	protected static HashMap<Integer, Integer>[] edgeWeights;
	
	protected String TAB_EDGE = "edgeAttributeTable";
	protected String COL_WEIGHT = "edgeAttributeWeight";
	private static HashMap<String, HashSet<String>> groups = new HashMap<String, HashSet<String>>();
	
	static SimpleDateFormat df = new SimpleDateFormat("yyyy");
	private static String[] GROUP_NAMES;
	private static ArrayList<String> teams = new ArrayList<String>();
	private static ArrayList<String> usedTeams = new ArrayList<String>();

	
	public static TimeGraph<CNode,CEdge,CTime> load(int groupNum){
		GROUPS_PER_DATA_SAMPLE = groupNum;
		return load();
	}
	public static TimeGraph<CNode,CEdge,CTime> load(String[] groupNames){
		teams = new ArrayList<String>();
		for(int i=0 ; i<groupNames.length ; i++ ){ teams.add(groupNames[i]); }
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

//			System.out.println("[SQLLoader]	-------->TIME ANALYSIS: ----------------");
//			System.out.println("[SQLLoader] \t Time_min: " + new Date(timeMin) + ", " + new Time(timeMin));
//			System.out.println("[SQLLoader] \t Ttime_max: " + new Date(timeMax) + ", " + new Time(timeMax));
//			System.out.println("[SQLLoader] \t Time span.: " + (timeDiff / (TimeManager.DAY)) + " days, " + (timeDiff / (TimeManager.HOUR)) + " hours");
//			System.out.println("[SQLLoader] \t Slice span.: " + (timesliceDuration  / (TimeManager.DAY)) + " days," + (timesliceDuration  / (TimeManager.HOUR)) + " hours");
//			System.out.println("[SQLLoader] \t Timeslices: " + sliceAmount );
//			
			
			String teamList = "";
			if(teams.size() == 0)
			{
				// LOAD GROUP NAMES
				Statement stmtTeams = conn.createStatement();
				String query = "SELECT * FROM team;";
				ResultSet resTeams = stmtTeams.executeQuery(query);
				teams = new ArrayList<String>();
				while(resTeams.next()){
					teams.add(resTeams.getString("id").split("-")[0]);
				}
				Collections.shuffle(teams);
				for(int i = 0 ; i < GROUPS_PER_DATA_SAMPLE ; i++){
					teamList += " OR author_biblio.key LIKE '" + teams.get(i) + "%'"; 
					usedTeams.add(teams.get(i));
				}
			}else{
				for(int i = 0 ; i < teams.size() ; i++){
					teamList += " OR author_biblio.key LIKE '" + teams.get(i) + "%'"; 
				}
				usedTeams = teams;
			}
			teamList = teamList.substring(4);

			// LOAD TIME SLICES
			int yearMin = 2000;
			int yearMax = 2013;

//			int yearMin = 2005;
//			int yearMax = 2006;
					
			int timeCount = 0;
			
			CTime t;
			float maxWeight = 0, minWeight = 99999;
			
			for(int time = yearMin; time <= yearMax; time++)
			{
				try {
					t = new CTime(df.parse(time + "").getTime());
					t.setDateFormat(df);
				} catch (ParseException e) {
					e.printStackTrace();
					return null;
				}
				timeCount++;
//				edgeWeights[timeCount] = new HashMap<Integer, Integer>();
				
				/// CLEAN GRAPH TIME
				// LOAD NEW DATA
				Statement stmt = conn.createStatement();
				String query = "" +
				"SELECT DISTINCT * " +
				"FROM author_biblio  " +
				"WHERE author_biblio.year='"+ time +"' " +
				" 		AND( " + teamList + ");";
				System.out.println("[RawebDataSet] query:" + query);
				ResultSet res = stmt.executeQuery(query);
			           
			    HashMap<String, HashSet<CNode>> collabs = new HashMap<String, HashSet<CNode>>();
			    HashSet<CNode> authors;
			    CNode vertex;
			    String nodeId;
			    String bibname, groupname;
			    while(res.next()) 
			    {
			    	// get results
//			    	nodeId = res.getString("initial") + res.getString("lastname");
			    	nodeId = res.getString("lastname");
			    	if(nodeId.contains("untinas")) continue;
			    	if(nodeId.contains("rache")) continue;
//			    	if(nodeId.contains("acrenier")) continue;
			    	if(nodeId.contains("unet")) continue;
			    	if(nodeId.contains("anjean")) continue;
					
			    	if(!vertexNames.containsKey(nodeId)){
			    		vertexNames.put(nodeId, babyNames[vertexNames.size()]);
			    	}
			    	nodeId = vertexNames.get(nodeId);
			    	
			    	
			    	vertex = new CNode(nodeId);
			    	bibname = res.getString("bibname");
			        
			        // create author and group nodes.
			        groupname = res.getString("key").split("-")[0];
			        HashSet<String> members = groups.get(groupname);
			        if(members == null){
			        	members = new HashSet<String>();
			        	groups.put(groupname, members);
			        }
			        
			        // CREATE VERTICES
			        vertex = tGraph.addVertex(vertex, t);
			        tGraph.setNodeLabel(vertex, vertex.getID());
			    	if(!groupColors.containsKey(groupname)){
			    		groupColors.put(groupname, Color.getHSBColor((float) Math.random(), .3f, .8f));				        	
//			        	groupColors.put(groupname, new Color((int)(Math.random() * 255), (int)(Math.random() * 255), (int) (Math.random() * 255)));
			        }
			    	
			        nodeColors.put(vertex, groupColors.get(groupname));
			        
			        
			        // set collaborators
			        authors = collabs.get(bibname);
			        if(authors == null)
			        {
			        	authors = new HashSet<CNode>();
			        	collabs.put(bibname, authors);
			        }
			        authors.add(vertex);
			    }
			    
				
				/// CREATE EDGES
				int edgeCount = 0;
				int edgeId =0;
				int n1Code, n2Code;
				CEdge e;
				float weight;
				for(HashSet<CNode> group : collabs.values())
				{
					for(CNode v1: group){
						for(CNode v2: group){
							if(v1 != v2 || group.size() == 1)
							{
								// set edge weight
								if( tGraph.getGraph(t).findEdge(v1, v2) != null )
								{
									weight = tGraph.getGraph(t).findEdge(v1, v2).getWeight() +1;
									tGraph.getGraph(t).findEdge(v1, v2).setWeight(weight);
									if(v1!=v2)
										tGraph.getGraph(t).findEdge(v2, v1).setWeight(weight);
									
								}else if( tGraph.getGraph(t).findEdge(v2, v1) != null )
								{
									weight = tGraph.getGraph(t).findEdge(v1, v2).getWeight() +1;
									tGraph.getGraph(t).findEdge(v1, v2).setWeight(weight);
									if(v1!=v2)
										tGraph.getGraph(t).findEdge(v2, v1).setWeight(weight);
								}
								else
								{
									e = new CEdge(utils.createEdgeID(v1.getID(), v2.getID(), t, v1!=v2));
									weight = 1;
									e.setWeight(weight);
									tGraph.addEdge(e, v1, v2, t, v1!=v2);

									if(v1!=v2){
										e = new CEdge(utils.createEdgeID(v2.getID(), v1.getID(), t, v1!=v2));
										e.setWeight(weight);
										tGraph.addEdge(e, v2, v1, t, v1!=v2);
									}
								}
								maxWeight = Math.max(weight, maxWeight);
								minWeight = Math.min(weight, minWeight);
							}
						}
					}
				}
//				tGraph.setTimeLabel(t, time + "");
			}        
			CubixVis.WEIGHT_MAX = maxWeight;
			CubixVis.WEIGHT_MIN = minWeight;
				
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
