package cubix.view;

import java.util.ArrayList;
import java.util.Collection;

import cubix.CubixVis;
import cubix.data.MatrixCube;

public class ViewManager {

	
	public static final int VIEW_CUBE = 0;
	public static final int VIEW_FRONT = 1;
	public static final int VIEW_SIDE = 2;
	public static final int VIEW_GRAPH_SM = 3;
	public static final int VIEW_NODE_SM = 4;
	
	protected ArrayList<CView> views = new ArrayList<CView>();

	
	private static ViewManager instance;

	private ViewManager() 
	{
		// Init views
		views.clear();
        views.add(new CubeView());
        views.add(new FrontView());
        views.add(new SideView());
        views.add(new GraphSMView());
        views.add(new NodeSMView());
	}

	public static ViewManager getInstance() {
		if (null == instance) {
			instance = new ViewManager();
		}
		return instance;
	}
	
	public void init(CubixVis vis){
		for(CView v : views){
			v.init(vis);
		}
	}

	
	public CView getView(int view){return views.get(view);} 
	public Collection<CView> getViews() {return views;}

	public static void destroyInstance() {
		instance = null;
	}
}
