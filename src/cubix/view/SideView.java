package cubix.view;

import cubix.CubixVis;
import cubix.data.MatrixCube;
import cubix.helper.Log;
import cubix.helper.Utils;
import cubix.vis.Slice;

public class SideView extends CubeView {

	public SideView() {
		super();
		name = "Side";
		cameraPos = new float[]{-1,0,0}; // Camera position vector. Needs to be streched or skewed when cube coordinats are ready
		cameraLookAt = new float[]{0,0,0};
		viewAngle = ANGLE_ORTHO;
		
	}
	@Override
	public void init(CubixVis vis)
	{
		super.init(vis);
		
		MatrixCube mc = vis.getMatrixCube();

		int num = mc.getTimeSlices().size();
		int num2 = mc.getHNodeSlices().size();
		int step = 1;
		int count = 0;
		float[] pos;
 		for(Slice<?,?> s : mc.getVNodeSlices())
		{
			pos = new float[]{-num/2 + step*count, -num2/2 + step*count - vis.CELL_UNIT/2, (num/2+5) * vis.CELL_UNIT};
			labelPosR.put(s, pos.clone());
			labelAlignR.put(s, Align.CENTER);
			labelPosL.put(s, pos.clone());
			labelAlignL.put(s, Align.CENTER);	
			count++;
		}
	
	
		float d = (float) ((mc.getRowCount() * vis.CELL_UNIT * 2) / (2 * Math.tan(ANGLE_PERSP * Math.PI / 360)));
		cameraPos = new float[]{-d, 0, 0};
	}

}
