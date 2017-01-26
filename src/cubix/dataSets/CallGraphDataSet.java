package cubix.dataSets;

import cubix.data.CEdge;
import cubix.data.CNode;
import cubix.data.CTime;
import cubix.data.TimeGraph;
import cubix.data.TimeGraphUtils;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class CallGraphDataSet{

	
	protected File dir;
	
	protected String COL_PACKAGE = "package";
	protected String TAB_CLASS = "class";
	
	static final String EDGE_SYMB = "->";
	
	protected HashMap<String, Color> groupColors = new HashMap<String, Color>();
		
	static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

	private static TimeGraphUtils<CNode, CEdge, CTime> utils;

	public static TimeGraph<CNode, CEdge, CTime> load(File dir)
	{
		TimeGraph<CNode, CEdge, CTime> tGraph = new TimeGraph<CNode, CEdge, CTime>();
		utils = new TimeGraphUtils<CNode, CEdge, CTime>();
		
		if(!dir.exists()) {	
			System.err.println("[CallGraphData] File not found: " + dir.getAbsolutePath());
			return null;
		}
		File[] files = dir.listFiles(new FilenameContainsFilter("dot"));
		int timeAmount = files.length; // one is .svn, the other is description file
		for(int t = 0; t < timeAmount ; t++)
		{
			loadDotFile(files[t], tGraph);	
		}
		return tGraph;
	}
	
	
	
	private static void loadDotFile(File file, TimeGraph<CNode, CEdge, CTime> tGraph) 
	{
		HashMap<String,CNode> vertices = new HashMap<String, CNode>();
	        ArrayList<String> sources = new ArrayList<String>();
	        ArrayList<String> targets = new ArrayList<String>();

	        try {
	            FileInputStream fis = new FileInputStream(file);
	    	    BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
	    	    String line = br.readLine();
	    	    CNode node;
	    	    String groupname, nodeLabel;
	    	    String[] splits;
	    	    String[] s = file.getName().split("_");
	    	    CTime t;
				try {
					t = new CTime(df.parse(s[1]).getTime());
				} catch (ParseException e) {
					e.printStackTrace();
					return;
				}

				
				// START PARSE
				boolean parse = true;
	            while (line != null)
	            {
	                if (line.length() > 0)
	                {
	                	// PARSE NODES AND EDGES, 
	                	// create nodes but no edges
	                	if(line.startsWith("  Node"))
		                {
	                		// EDGE FOUND
		                	if(line.contains(EDGE_SYMB))
		                	{
		                		String[] tokens = line.trim().split(EDGE_SYMB);
		                		sources.add(tokens[0].replace(" ", ""));
		                		targets.add(tokens[1].split(" ")[1].replace(" ", ""));
		                	
		                	}else{
		                		// NODE FOUND
		                		splits = line.split("\"");
		                		nodeLabel = splits[1].replace(" ", "");
		                		node = new CNode(nodeLabel);
		                		node = tGraph.addVertex(node, t);
		                		vertices.put(splits[0].split(" ")[2].replace(" ", ""), node);
		                		tGraph.setNodeLabel(node, nodeLabel);
		                		
		                		
		                		
//		            			try{					
//		            				groupname = nodename.split("\\.")[1];
//		            			
//			            			dataModel.addEntry(node, TAB_CLASS);
//			            			dataModel.setAttribute(node, TAB_CLASS, COL_PACKAGE, groupname);
//			            			if(!groupColors.containsKey(groupname)){
//			            				System.out.println("[CallGraphDescription] groupname " + groupname);
//			            			  	groupColors.put(groupname, new Color((int)(Math.random() * 255), (int)(Math.random() * 255), (int) (Math.random() * 255)));
//			            			}
//		            			}catch(ArrayIndexOutOfBoundsException e){
//		            				line = br.readLine();
//		            				continue;
//		            			}
		            		}
		                }else if (line.contains("}"))
	                	{
	                		// CREATE EDGES
	                		CNode l1, l2;
	                		String edge;
	                		for(int i=0 ; i<sources.size() ; i++)
	                		{
	                			l1 = vertices.get(sources.get(i));
	                			l2 = vertices.get(targets.get(i));
	                			edge = utils.createEdgeHash(l1.getID(), l2.getID()) + "";
//	                		   	tGraph.addEdge(new CEdge(edge), l1, l2, t, true);     
	                		   	edge = utils.createEdgeHash(l2.getID(), l1.getID()) + "";
	                		   	tGraph.addEdge(new CEdge(edge), l2, l1, t, false);     
	           	            }
	                		sources.clear();
	                		targets.clear();
	                		vertices.clear();
	                	}
	                }
	                line = br.readLine();
	            }
	            fis.close();            
	        }
	        catch (IOException ex){
	            System.err.println("Error loading file " + file);
	            ex.printStackTrace();
	        }
	    }
}
