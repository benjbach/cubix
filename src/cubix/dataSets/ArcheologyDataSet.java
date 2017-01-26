package cubix.dataSets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import cubix.data.CEdge;
import cubix.data.CNode;
import cubix.data.CTime;
import cubix.data.TimeGraph;

import edu.uci.ics.jung.graph.util.EdgeType;

public class ArcheologyDataSet{

	private static String dbName = "sevilla";
	private static String user = "sevilla";
	private static String pw = "cordoba";
	private static String graphName = "archeology";
	private static HashMap<String, CNode> nodes = new HashMap<String, CNode>();

	/// SCHEMA
	public static final String TYPE_SITE = "T:Site";
	public static final String TYPE_CERAMIC = "T:Ceramic";
	public static final String TYPE_ARCHITECTURE = "T:Architecture";
	public static final String TYPE_WEAPON = "T:Weapon";
	
	
	public static TimeGraph<CNode, CEdge, CTime> load() 
	{
		dbName = System.getProperty("db", "jdbc:mysql://localhost:3306/" + dbName);
		user = System.getProperty("dbuser", user);
		pw = System.getProperty("dbpass", pw);

		TimeGraph graph = new TimeGraph();
		CNode n, target;
		CEdge e;
		CTime t;
		t = new CTime(System.currentTimeMillis());
		try {
			Connection conn = DriverManager.getConnection(dbName, user, pw);
			Statement stmtTeams = conn.createStatement();
			
			// GATHER SITES
			t = new CTime(System.currentTimeMillis());
			String query = "SELECT * FROM tbl_sites;";
			ResultSet resSites = stmtTeams.executeQuery(query);
			while(resSites.next()){
				n = new CNode(resSites.getString("siteID"));
				graph.addVertex(n, t);
//				n = GraphFactory.addNode(graph, resSites.getString("site_name"), TYPE_SITE);
				nodes.put(resSites.getString("siteID"), n);
//				Log.out(this, "type: " + graph.getNodeType(n));
			}
			
			
			t = new CTime(System.currentTimeMillis());
			query = "SELECT * FROM tbl_ceramics;";
			ResultSet resCeramics = stmtTeams.executeQuery(query);
			String id;
			while(resCeramics.next())
			{
				id = getCeramicsID(resCeramics);
				n = nodes.get(id);
				if(n == null){
					target = nodes.get(resCeramics.getString("siteID"));
					if(target != null)
					{
//						graph.addEdge(target, n, true, "HAS_CERAMIC");
					}
				}
				
				// Create edges
			}
		}
		


			
//			// GATHER CERAMICS
////			
////			// GATHER ARCHITECTURE
////			query = "SELECT * FROM tbl_architecture;";
////			ResultSet resArch = stmtTeams.executeQuery(query);
////			while(resArch.next())
////			{
////				id = getArchID(resArch);
////				n = nodes.get(id);
////				if(n == null){
////					n = new ANode();
////					nodes.put(id, n);
////					graph.addNode(n, id, "ARCHITECTURE");
////				}
////				
////				// Create edges
////				target = nodes.get(resArch.getString("siteID"));
////				if(target != null)
////				{
////					graph.addEdge(target, n, true, "HAS_ARCHITECTURE");
////				}
////			}
////			
//////			// GATHER WEAPONS
//////			query = "SELECT * FROM tbl_coin_min;";
//////			ResultSet resCoin = stmtTeams.executeQuery(query);
//////			while(resCoin.next())
//////			{
//////				n = nodes.get(resCoin.getString("weapon"));
//////				if(n == null){
//////					n = new ANode(resCoin.getString("weapon"), "WEAPON");
//////					graph.addVertex(n);
//////					nodes.put(n.getID(), n);
//////				}
//////				
//////				// Create edges
//////				target = nodes.get(resCoin.getString("siteID"));
//////				if(target != null)
//////				{
//////					e = new AEdge(target.getID() + "->" + n.getID(), "HAS_WEAPON");
//////					graph.addEdge(e, target, n, EdgeType.DIRECTED);
//////				}
//////			}
//
		catch (SQLException ex) { ex.printStackTrace();
		}

		return graph;
	}

		

	
	protected static String getCeramicsID(ResultSet res) throws SQLException
	{
		String id = "";
		int i=7;
		while(i > 0 && res.getString("ceramics_component" + i).startsWith("/")){
			i--;
		}
		id = res.getString("ceramics_component" + i);
		
//		return res.getString("ceramics_component1") 
//		+ "-" + res.getString("ceramics_component2") 
//		+ "-" + res.getString("ceramics_component3")
		return id;
	}
	protected static String getArchID(ResultSet res) throws SQLException
	{
		String id = "";
		int i=2;
		while(i > 0 && res.getString("architecture_component" + i).startsWith("/")){
			i--;
		}
		id = res.getString("architecture_component" + i);
//		return res.getString("architecture_component1") 
//		+ "-" + res.getString("architecture_component2") 
//		+ "-" + res.getString("architecture_component3") 
		return id;
	}
		
		
}
