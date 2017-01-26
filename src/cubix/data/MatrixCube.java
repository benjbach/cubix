package cubix.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

import cubix.helper.Log;
import cubix.ordering.ClusterOrdering;
import cubix.ordering.MatrixOrdering;
import cubix.ordering.TimeDistanceCalculator;
import cubix.vis.Cell;
import cubix.vis.TimeSlice;
import cubix.vis.HNodeSlice;
import cubix.vis.VNodeSlice;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;


/**
 * The matrix cube is a structure to store and access a three-dimensinal matrices.
 * The matrix cube represents a dynamic network. One dimension of the cube 
 * is time, the two others are nodes. The cube contains {@link cubix.vis.Cell} objects 
 * for any edge that exists in the dynamic networks at any time.
 * 
 * The matrix cube contains {@link cubix.vis.Slice} objects for any row, column and 
 * time objects. Slices are associated with nodes and time objects and can be used
 * to access cells. Thereby slices can be seen as matrices, pointing to a two-dimensional
 * field of cells. 
 * 
 * Slices can be permuated (re-ordered), which causes the cell fields inside
 * slice objects to be updated. For performance reasons the matrix cube stores
 * the order of objects as well as the order of corresponding slices. In 
 * case of reordering, both are updated.
 * 
 * TODO Make class and slices generic.
 * TODO Update cell fields in slices after reordering
 * 
 * @author benjamin.bach@inria.fr
 *
 * @param <CNode>
 * @param <CEdge>
 * @param <CTime>
 */
public class MatrixCube {

	//
	public static final int TIME = 0;
	public static final int ROW = 1;
	public static final int COL = 2;
	
	protected float maxWeight = 0;
	
	
	/// GRAPH
	private TimeGraph<CNode,CEdge,CTime> tGraph = null;


	/// SLICES
	private ArrayList<TimeSlice> tSlices = new ArrayList<TimeSlice>();
	private ArrayList<HNodeSlice> hSlices = new ArrayList<HNodeSlice>();
	private ArrayList<VNodeSlice> vSlices = new ArrayList<VNodeSlice>();

	/** Maps Time objects to Timeslices **/
	private HashMap<CTime, TimeSlice> tSliceMap = new HashMap<CTime, TimeSlice>();
	/** Maps Row objects to VNodeSlices**/
	private HashMap<CNode, VNodeSlice> vSliceMap = new HashMap<CNode, VNodeSlice>();
	/** Maps Column objects to HNodeSlices**/
	private HashMap<CNode, HNodeSlice> hSliceMap = new HashMap<CNode, HNodeSlice>();

	// CELLS
	/** All Cell objects inside the cube **/
	private HashMap<CEdge, Cell> cells = new HashMap<CEdge, Cell>();

	/** Stores the order of row elements **/
	protected ArrayList<CNode> rowOrder = new ArrayList<CNode>(); 
	/** Stores the order of column elements **/
	protected ArrayList<CNode> columnOrder = new ArrayList<CNode>(); 
	/** Stores the order of time elements **/
	protected ArrayList<CTime> timeOrder = new ArrayList<CTime>();

	private static final int CUTHILL = 0; 
	private static final int CLUSTER = 1;
    private int method = CLUSTER; 
	
	private HashMap<CTime, DoubleMatrix2D> distanceMatrices = new HashMap<CTime, DoubleMatrix2D>(); 
	
	
	public MatrixCube(TimeGraph<CNode,CEdge,CTime> tGraph, float cellSize)
	{
		// Init
		this.tGraph = tGraph;
		this.timeOrder.addAll(tGraph.getTimes());
    	this.rowOrder.addAll(tGraph.getVertices());
    	this.columnOrder.addAll(tGraph.getVertices());
    	int nodeCount = columnOrder.size();

    	
    	TimeSlice timeslice;
    	Graph<CNode,CEdge> graph;
    	ArrayList<String> nodeLabels = new ArrayList<String>();
		for(CNode n : rowOrder){
			nodeLabels.add(tGraph.getVertexLabel(n));
		}
		int timeCount = tGraph.getTimes().size();
		ArrayList<String> timeLabels = new ArrayList<String>();
		for(CTime time : tGraph.getTimes())
		{
			graph = tGraph.getGraph(time);
			timeslice = new TimeSlice(this, nodeCount,nodeCount, time);
			timeslice.setLabel(time.getLabel());
			timeslice.setRowLabels(nodeLabels);
			timeslice.setColumnLabels(nodeLabels);
			tSlices.add(timeslice);
        	tSliceMap.put(time, timeslice);
        	timeLabels.add(time.getLabel());
		}

		// CREATE NODE SLICES
		VNodeSlice vs;
		HNodeSlice hs;
		for(CNode n : tGraph.getVertices())
		{
			hs = new HNodeSlice(this, nodeCount, timeCount, n);
			hs.setLabel(tGraph.getVertexLabel(n));
			hs.setRowLabels(nodeLabels);
			hs.setColumnLabels(timeLabels);
			hSlices.add(hs);
			hSliceMap.put(n, hs);

			vs = new VNodeSlice(this, nodeCount, timeCount, n);
			vs.setLabel(tGraph.getVertexLabel(n));
			vs.setRowLabels(nodeLabels);
			vs.setColumnLabels(timeLabels);
        	vSlices.add(vs);
			vSliceMap.put(n, vs);
		}	
		
		// Create cells
		Cell c;
		CNode source, target;
		Pair<CNode> endPoints;
		Graph<CNode, CEdge> g;
		for(CTime t : tGraph.getTimes())
		{
			g = tGraph.getGraph(t);
			for(CEdge e : g.getEdges())
			{
				if(e.getWeight() == 0) 
					continue;
			
				this.maxWeight = Math.max(e.getWeight(), maxWeight);
				c = new Cell(cellSize, cellSize, cellSize);
				c.setOwner(e);
				cells.put(e,c);
	    		endPoints = g.getEndpoints(e);
	    		source = endPoints.getFirst();
	    		target = endPoints.getSecond();
	    		
	    		tSliceMap.get(t).setCell(c, rowOrder.indexOf(source), columnOrder.indexOf(target));
	    		vSliceMap.get(target).setCell(c, rowOrder.indexOf(source), timeOrder.indexOf(t));
	    		hSliceMap.get(source).setCell(c, columnOrder.indexOf(target), timeOrder.indexOf(t));
	    		
	    		c.setGraphSlice(tSliceMap.get(t));
	    		c.setVNodeSlice(vSliceMap.get(target));
	    		c.setHNodeSlice(hSliceMap.get(source));
			
			}
		}
	}
	
	////////////////
	/// ORDERING ///
	////////////////
	
	protected static int ordering = -1;
	public static final int ORDERING_GLOBAL = 0;
	public static final int ORDERING_LOCAL = 1;
	public static final int ORDERING_INDIVIDUAL = 2;
	

	/** Calculates an optimal ordering, taking into account the passed edges.
	 * of the graph and updates the slices.
	 * @param times
	 */
	public void reorderNodes(Collection<CEdge> validEdges)
	{
		ArrayList<CNode> nodeOrder = new ArrayList<CNode>();
        MatrixUtils mu = new MatrixUtils();
		if(method == CLUSTER)
		{
            nodeOrder = mu.reorderHierarchical(tGraph, validEdges);
		}else{
			nodeOrder = mu.reorderCutHillMcKee(tGraph, validEdges);
		}
		
		// Reverse order so that the node with larger degree comes first. 
		if(	tGraph.getNeighbors(nodeOrder.get(0)).size() < tGraph.getNeighbors(nodeOrder.get(nodeOrder.size()-1)).size() ){
			Collections.reverse(nodeOrder);
		}
		
		setNodeOrder(nodeOrder);
	}

	/** Sets the node ordering of the cube and updates cell positions in 
	 * all slices. 
	 * 
	 * @param nodes -- The new node order.
	 */
	public void setNodeOrder(ArrayList<CNode> nodes)
	{
		
		rowOrder = (ArrayList<CNode>) nodes.clone();		
		columnOrder  = (ArrayList<CNode>) nodes.clone();
		Log.out(this, "rowOrder.size()" + rowOrder.size());
		
		// update slice lists: 
		hSlices.clear();
		for(CNode n : nodes){ 
			hSlices.add(hSliceMap.get(n)); 
		}

		vSlices.clear();
		for(CNode n : nodes){ 
			vSlices.add(vSliceMap.get(n)); 
		}
	
		
		// Update slices
		Pair<CNode> endPoints;
		CNode source, target;
		CEdge e;
		CTime t;
		ArrayList<String> nodeLabels = new ArrayList<String>();
		for(CNode n : rowOrder){
			nodeLabels.add(tGraph.getVertexLabel(n));
		}

		for(TimeSlice s: tSlices)
		{
			t = s.getData();
			for(Cell c : s.getCells())
			{
				e = c.getData();
				endPoints = tGraph.getGraph(t).getEndpoints(e);
	    		source = endPoints.getFirst();
	    		target = endPoints.getSecond();
	    		
	    		try{
	    			tSliceMap.get(t).setCell(c, rowOrder.indexOf(source), columnOrder.indexOf(target));
	    		}catch(Exception ec){
	    			Log.err(this, source.getID()+ "-" + target.getID());
	    		}
	    		hSliceMap.get(source).setCell(c, columnOrder.indexOf(target), timeOrder.indexOf(t));
	    		vSliceMap.get(target).setCell(c, rowOrder.indexOf(source), timeOrder.indexOf(t));
	    		
	    		c.setGraphPos(tSliceMap.get(t).getRelGridCoords(rowOrder.indexOf(source), columnOrder.indexOf(target)));
	    		c.setHNodePos(hSliceMap.get(source).getRelGridCoords(columnOrder.indexOf(target), timeOrder.indexOf(t)));
				c.setVNodePos(vSliceMap.get(target).getRelGridCoords(rowOrder.indexOf(source), timeOrder.indexOf(t)));
				
	    		c.setGraphSlice(tSliceMap.get(t));
	    		c.setVNodeSlice(vSliceMap.get(target));
	    		c.setHNodeSlice(hSliceMap.get(source));

	    		tSliceMap.get(t).setRowLabels(nodeLabels);
	    		tSliceMap.get(t).setColumnLabels(nodeLabels);

	    		hSliceMap.get(source).setRowLabels(nodeLabels);
	    		vSliceMap.get(target).setRowLabels(nodeLabels);
	    		
			}
		}
	}
	
	
	public void reorderTimes(Collection<CEdge> validEdges)
	{
		ArrayList<CTime> timeOrder = new ArrayList<CTime>();
        MatrixUtils mu = new MatrixUtils();
        timeOrder = mu.reorderTimesHierarchical(tGraph, validEdges);
		
		setTimeOrder(timeOrder);
	}
	
	public void setTimeOrder(ArrayList<CTime> times)
	{
		this.timeOrder = times;
		
		// update slice lists: 
		tSlices.clear();
		for(CTime t : timeOrder){ 
			tSlices.add(tSliceMap.get(t)); 
		}
		
		int t;
		int r;
		for(Cell c : getCells()){
			VNodeSlice s = c.getVNodeSlice();
			t = timeOrder.indexOf(c.getTimeSlice().getData());
			r = rowOrder.indexOf(c.getHNodeSlice().getData());
			s.setCell(c, r, t);
			c.setVNodePos(s.getRelGridCoords(r, t));
		}
	}
	


	/////////////////
	/// GET & SET ///
	/////////////////
	
	public TimeSlice getTimeSlice(CTime t){ return tSliceMap.get(t);}
	public HNodeSlice getHNodeSlice(CNode n){ return hSliceMap.get(n);}
	public VNodeSlice getVNodeSlice(CNode n){ return vSliceMap.get(n);}
	public TimeSlice getTimeSlice(int t){ return tSliceMap.get(timeOrder.get(t));}
	public HNodeSlice getHNodeSlice(int n){ return hSliceMap.get(columnOrder.get(n));}
	public VNodeSlice getVNodeSlice(int n){ return vSliceMap.get(rowOrder.get(n));}

	/** Returns the time slices in order **/
	public ArrayList<TimeSlice> getTimeSlices(){ return tSlices; }
	public ArrayList<VNodeSlice> getVNodeSlices(){ return vSlices;}
	public ArrayList<HNodeSlice> getHNodeSlices(){ return hSlices; }

	public int getRowIndex(CNode node){ return rowOrder.indexOf(node); }
	public int getColumnIndex(CNode node){ return columnOrder.indexOf(node); }
	public int getTimeIndex(CTime time){ return timeOrder.indexOf(time); }
	
	public int getRowCount(){ return rowOrder.size();  }
	public int getColumnCount(){ return columnOrder.size();  }
	public int getTimeCount(){ return timeOrder.size();  }

	public Collection<Cell> getCells() { return cells.values(); }
	
	public TimeGraph<CNode,CEdge,CTime> getTimeGraph(){return tGraph;}

	public TimeSlice getLastTimeSlice() { return tSliceMap.get(getLastTime());}
	public TimeSlice getFirstTimeSlice() { return tSliceMap.get(timeOrder.get(0));}
	public VNodeSlice getLastVNodeSlice() { return vSliceMap.get(getLastRowNode());}
	public VNodeSlice getFirstVNodeSlice() { return vSliceMap.get(rowOrder.get(0));}
	public HNodeSlice getLastHNodeSlice() { return hSliceMap.get(getLastColumnNode());}
	public HNodeSlice getFirstHNodeSlice() { return hSliceMap.get(columnOrder.get(0));}
	
	public CTime getLastTime(){return timeOrder.get(timeOrder.size()-1);}
	public CNode getLastRowNode(){return rowOrder.get(rowOrder.size()-1);}
	public CNode getLastColumnNode(){return columnOrder.get(columnOrder.size()-1);}
	
	public int getOrdering() {return ordering;}


	public Object getCell(CEdge e) {
		return cells.get(e);
	}

	
}
