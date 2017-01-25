package cubix;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import cubix.data.CEdge;
import cubix.data.CNode;
import cubix.data.CTime;
import cubix.data.DataModel;
import cubix.data.MatrixCube;
import cubix.dataSets.ArcheologyDataSet;
import cubix.dataSets.AntennaDataSet;
import cubix.dataSets.BrainDataSet;
import cubix.dataSets.BrainDataSet2;
import cubix.dataSets.CSVGDataSet;
import cubix.dataSets.CollabValpoDataSet;
import cubix.dataSets.CallGraphDataSet;
import cubix.dataSets.FilenameContainsFilter;
import cubix.dataSets.InfovisDataSet;
import cubix.dataSets.LieveDataSet;
import cubix.dataSets.LoadCSVAdajacencyMatrix;
import cubix.dataSets.MigrationDataSet;
import cubix.dataSets.NewComb;
import cubix.dataSets.RawebDataSet;
import cubix.dataSets.TerritoryChangesDataSet;
import cubix.dataSets.TradeDataSet;
import cubix.dataSets.TradingCostsDataSet;
import cubix.dataSets.UNHCRDataSet;
import cubix.helper.Log;
import cubix.helper.Map;
import cubix.helper.Utils;
import cubix.helper.Constants.Align;
import cubix.transitions.TransitionManager;
import cubix.view.CView;
import cubix.view.CubeView;
import cubix.view.FrontView;
import cubix.view.GraphSMView;
import cubix.view.NodeSMView;
import cubix.view.SideView;
import cubix.view.ViewManager;
import cubix.vis.Cell;
import cubix.vis.HNodeSlice;
import cubix.vis.Slice;
import cubix.vis.TimeSlice;
import cubix.vis.VNodeSlice;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import java.awt.GridBagLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.JSplitPane;
import javax.swing.SpringLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JComboBox;
import javax.swing.JToggleButton;

public class Cubix {

	private static int SCREEN_WIDTH;
	private static int SCREEN_HEIGHT;
	private static int OFFSET_RIGHT = 000;
	private static int OFFSET_LEFT = 000;
	private static JToggleButton timeEncodingButton;
	private static CubixVisInteractive cubeVis1;
	private static CubixVisInteractive cubeVis2;

	// DATA SETS
	private static final int TRADE_1 = 19;
	private static final int TRADE_2 = 16;
	private static final int TRADE_3 = 21;
	private static final int TRADE_100 = 13;
	private static final int COLLAB_TAO = 8;
	private static final int BRAIN = 9;
	private static final int ARCH = 22;
	private static final int ANTENNAS_AMP = 23;
	private static final int ANTENNAS_DELAY = 24;
	private static final int ANTENNAS_RMS = 25;
	private static final int ANTENNAS_SNR = 26;
	private static final int TRADE_4 = 101;
	private static final int TRADE_5 = 102;
	private static final int COLLAB_VALPO_1 = 200;
	public static final int CONTROL_PANEL_WIDTH = 260;
	private static final int COLLAB_INFOVIS = 300;
	private static final int NEWCOMB = 400;
	private static final int LIEVE = 500;
	private static boolean CSV = true;

	// Brain data from Stephane and Habib
	private static final int BRAIN_1 = 3001;

	static Toolkit kit = Toolkit.getDefaultToolkit();
	public static JFrame frame;
	public static String dataSetName;

	public static final boolean DEPLOY = true;

	public static void main(String[] args) {
		System.out.println("start " + DEPLOY);
		// Create Frame
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] screens = ge.getScreenDevices();
		int SCREEN = screens.length - 1;
//		int SCREEN = 1;
		// if (SCREEN == 0 && screens.length == 1)
		// OFFSET_RIGHT = 0;

		SCREEN_WIDTH = screens[SCREEN].getDisplayMode().getWidth() - 10;
		SCREEN_HEIGHT = screens[SCREEN].getDisplayMode().getHeight() - 30;

		// SCREEN_WIDTH = screens[SCREEN].getDisplayMode().getWidth() - 10;
		// SCREEN_WIDTH = screens[SCREEN].getDisplayMode().getWidth() -500;
		// SCREEN_WIDTH = 1024;
		// SCREEN_HEIGHT = 768;
		// SCREEN_WIDTH = 1500;
		// SCREEN_HEIGHT = 1000;
		// SCREEN_WIDTH = 800;
		// SCREEN_HEIGHT = 600;

		frame = new JFrame(screens[SCREEN].getDefaultConfiguration());

		Rectangle bounds = screens[SCREEN].getDefaultConfiguration().getBounds();
		frame.setBounds(bounds.x, bounds.y, SCREEN_WIDTH - OFFSET_RIGHT, SCREEN_HEIGHT);
		frame.setBackground(Color.WHITE);
		frame.getContentPane().setLayout(new BorderLayout(0, 0));

		JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		frame.add(sp, BorderLayout.CENTER);

		// Create CubeVis
		int visWidth = (SCREEN_WIDTH - OFFSET_RIGHT - CONTROL_PANEL_WIDTH);
		// cubeVis1 = CubixVisInteractive.getInstance();
		cubeVis1 = new CubixVisInteractive();
		cubeVis1.setDimensions(visWidth, SCREEN_HEIGHT);
		cubeVis1.addGLEventListener(cubeVis1);
		cubeVis1.addMouseListener(cubeVis1);
		cubeVis1.addMouseMotionListener(cubeVis1);
		cubeVis1.addMouseWheelListener(cubeVis1);
		cubeVis1.addKeyListener(cubeVis1);
		cubeVis1.setPreferredSize(new Dimension(visWidth, SCREEN_HEIGHT));
		sp.setDividerLocation(SCREEN_WIDTH - CONTROL_PANEL_WIDTH);
		sp.setLeftComponent(cubeVis1);
		sp.setDividerSize(0);
		sp.setRightComponent(cubeVis1.getControlPanel());

		frame.setSize(frame.getContentPane().getPreferredSize());

		// Load Data
		DataModel<CNode, CEdge, CTime> model = DataModel.getInstance();
		int dataSet;

		// Select dataset by key on startup

		// DEMO DATA SETS:

		if (DEPLOY) {
			System.setProperty("apple.awt.fileDialogForDirectories", "true");
			JFileChooser fc = new JFileChooser();
			File here = new File("");
			File data = new File(here.getAbsolutePath() + "/data");
			if (!data.exists()) {
				data = here;
			}
			fc.setSelectedFile(data.getAbsoluteFile());
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			// FileDialog fd = new FileDialog(frame, "Choose a file",
			// FileDialog.LOAD);
			fc.validate();
			frame.setVisible(true);
			Log.out("open dialog " + fc.isVisible());
			int returnVal = fc.showOpenDialog(frame);
			frame.setVisible(false);
			Log.out("Return val " + returnVal);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File f = fc.getSelectedFile();
				// File f = new File(fd.getDirectory() +
				// System.getProperty("file.separator") + fd.getFile());
				if (f.isDirectory()) {
					// Load directory
					if (f.getName().contains(".csv")) {
						model.setTimeGraph(LoadCSVAdajacencyMatrix.load(f));
					} else {
						JOptionPane.showMessageDialog(frame, "Could not directory format.", "Loading error",
								JOptionPane.ERROR_MESSAGE);
						System.exit(0);
					}
				} else {
					String ext = f.getName();
					try {
						if (ext.contains(".brain")) {
							model.setTimeGraph(BrainDataSet2.loadData(f));
						} else {
							model.setTimeGraph(CSVGDataSet.loadData(f));
						}
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(frame, "Could not load file format.", "Loading error",
								JOptionPane.ERROR_MESSAGE);
						System.exit(0);
					}
				}

				if (f.getName().contains("."))
					dataSetName = f.getName().split("\\.")[0];
				else
					dataSetName = f.getName();
			}
		} else {
			
			dataSet = ANTENNAS_DELAY;
			// Object[] possibilities = {"Load CSV"};
			Object[] possibilities = { "Load CSV", "InfovisCollab", "NewComb", "Collab", "Delay", "Amp", "RMS", "SNR",
					"Trade-1", "Trade-2", "Trade-3", "Trade-4", "Trade-5", "Collab-Valpo-1" };
			String s = (String) JOptionPane.showInputDialog(frame, "Select Dataset:", "Customized Dialog",
					JOptionPane.PLAIN_MESSAGE, null, possibilities, "ham");
			if (s == null)
				System.exit(0);

			dataSetName = s;


			if (s.equals("Amp"))
				dataSet = ANTENNAS_AMP;
			if (s.equals("Delay"))
				dataSet = ANTENNAS_DELAY;
			if (s.equals("RMS"))
				dataSet = ANTENNAS_RMS;
			if (s.equals("SNR"))
				dataSet = ANTENNAS_SNR;
			if (s.equals("Collab"))
				dataSet = COLLAB_TAO;
			if (s.equals("Trade-1"))
				dataSet = TRADE_1;
			if (s.equals("Trade-2"))
				dataSet = TRADE_2;
			if (s.equals("Trade-3"))
				dataSet = TRADE_3;
			if (s.equals("Trade-4"))
				dataSet = TRADE_4;
			if (s.equals("Trade-5"))
				dataSet = TRADE_5;
			if (s.equals("Collab-Valpo-1"))
				dataSet = COLLAB_VALPO_1;
			if (s.equals("Brain_1"))
				dataSet = BRAIN_1;
			if (s.equals("InfovisCollab"))
				dataSet = COLLAB_INFOVIS;
			if (s.equals("NewComb"))
				dataSet = NEWCOMB;
			if (s.equals("Lieve"))
				dataSet = LIEVE;
			// else

			switch (dataSet) {
			case 0:
				model.setTimeGraph(CallGraphDataSet.load(new File("data/callgraphs/dynamicgraphvis-weeks")));
				break;
			case 1:
				model.setTimeGraph(CallGraphDataSet.load(new File("data/callgraphs/graphdiaries-dailylong")));
				break;
			case 2:
				model.setTimeGraph(RawebDataSet.load(new String[] { "insitu", "tao" }));
				break;
			case 3:
				model.setTimeGraph(RawebDataSet.load(2));
				break;
			case 4:
				model.setTimeGraph(RawebDataSet.load(new String[] { "tao" }));
				break;
			case 5:
				model.setTimeGraph(RawebDataSet.load(new String[] { "aviz" }));
				break;
			case 6:
				model.setTimeGraph(RawebDataSet.load(new String[] { "in-situ" }));
				break;
			case 7:
				model.setTimeGraph(RawebDataSet.load(new String[] { "ecoo" }));
				break;
			case COLLAB_TAO:
				model.setTimeGraph(RawebDataSet.load(new String[] { "runtime" }));
				break;
			case 9:
				model.setTimeGraph(BrainDataSet.load(new File("data/brains/"), "136524", 40, 10));
				break;
			case 10:
				model.setTimeGraph(BrainDataSet.load(new File("data/brains/"), "1to", 30, 2));
				break;// large, dense, 2clusters --> use for demo
			case 11:
				model.setTimeGraph(TradingCostsDataSet
						.load(new String[] { "Germany", "France", "United States", "Malawi", "India", "China" }, 5));
				break;
			case 12:
				model.setTimeGraph(TradeDataSet.load(new String[] { "Germany", "France", "United", "Russia",
						"Afganistan", "Malawi", "South Africa", "India", "Brasil", "China", "Japan", "Saudi Arabia" }));
				break;// large, dense,
			case 13:
				model.setTimeGraph(TradeDataSet.load(new String[] { "Germany", "France", "United Stat", "Italy",
						"Russia", "Japan", "Poland", "United Kingdom", "China", "India" }, 1900, 2000, 1));
				break;// large, dense,
			case 14:
				model.setTimeGraph(TradeDataSet.load(new String[] { "Germany", "France", "Russia", "United", "Japan",
						"Italy", "Brazil", "Canada", "Austria" }, 1910, 1939, 2));
				break;// large, dense,
			case 15:
				model.setTimeGraph(TradeDataSet.load(new String[] { "Germany", "France", "United Stat", "Italy",
						"Russia", "Japan", "Poland", "United Kingdom", "China", "India" }, 1900, 1950));
				break;
			case TRADE_2:
				model.setTimeGraph(
						TradeDataSet.load(new String[] { "Germany", "France", "United Stat", "Japan", "United Kingdom",
								"China", "Japan", "Spain", "Russia", "Italy", "Brazil", "india" }, 1980, 2010, 1));
				break;// large, dense,
			// case 17 : model.setTimeGraph(TradeDataSet.load(new
			// String[]{"Germany", "United Stat", "China", "Japan"}, 1998,
			// 2000));break;
			case 18:
				model.setTimeGraph(TradeDataSet
						.load(new String[] { "Germany", "France", "United States", "Malawi", "India", "China" }));
				break;// large, dense,
			case TRADE_1:
				model.setTimeGraph(TradingCostsDataSet.load(
						new String[] { "Germany", "France", "Malawi", "China", "Arabia", "Netherland", "Sudan",
								"United", "India", "Russia", "Chile", "Israel", "Pakistan", "South Africa", "Braszil",
								"Nigeria", "Corea", "Iran", "Iraq", "Eqypt", "Nicaragua", "Mexico", "Chile", "Cuba" },
						50));
				break;
			case 20:
				model.setTimeGraph(TradingCostsDataSet.load(new String[] { "Germany", "France", "Malawi", "China",
						"India", "Croatia", "Albania", "Greece", "Bugaria", "Denmark" }, 11));
				break;
			case TRADE_3:
				model.setTimeGraph(TradingCostsDataSet.load(new String[] { "Germany", "France", "United Stat", "Japan",
						"United Kingdom", "China", "Japan", "Spain", "Russia", "Italy", "Brazil", "india" }, 30));
				break;
			// case TRADE_6 : model.setTimeGraph(TradeDataSet.load(new
			// String[]{"German ","France","United Kingdom", "Spain","Russia",
			// "Italy", "Poland", "Romania", "Bulgaria", "Denmark","Sweden",
			// "Norway", "Austria", "Slovenia", "Netherlands",
			// "Belgium","Portugal", "Czechoslovakia", "Hungary", "Yugoslavia",
			// "Albania", "Swiss", "Ireland", "Greece"}, 1940, 1990, 1));
			// break;// large, dense,
			case ARCH:
				model.setTimeGraph(ArcheologyDataSet.load());
				break;
			case ANTENNAS_AMP:
				model.setTimeGraph(AntennaDataSet.load(new File("data/antennas-csv/"), "_amp"));
				break;
			case ANTENNAS_DELAY:
				model.setTimeGraph(AntennaDataSet.load(new File("data/antennas-csv/"), "_delay"));
				break;
			case ANTENNAS_RMS:
				model.setTimeGraph(AntennaDataSet.load(new File("data/antennas-csv/"), "_rms"));
				break;
			case ANTENNAS_SNR:
				model.setTimeGraph(AntennaDataSet.load(new File("data/antennas-csv/"), "_snr"));
				break;
			// case TRADE_4 : model.setTimeGraph(TradeDataSet.load(new
			// String[]{"Germany","France", "United Stat", "Japan","United
			// Kingdom", "China", "Japan", "Spain","Russia", "Italy", "Brazil",
			// "india", "Poland", "Romania", "Bulgaria", "Denmark","Sweden",
			// "Norway", "Austria", "Slovenia", "Netherlands", "Belgium",
			// "Luxemburg", "Monaco", "Portugal" }, 1980, 2010, 1)); break;//
			// large, dense,
			case TRADE_4:
				model.setTimeGraph(TradeDataSet.load(new String[] { "German ", "France", "United Kingdom", "Spain",
						"Russia", "Italy", "Poland", "Romania", "Bulgaria", "Denmark", "Sweden", "Norway", "Austria",
						"Slovenia", "Netherlands", "Belgium", "Portugal", "Czechoslovakia", "Hungary", "Yugoslavia",
						"Albania", "Swiss", "Ireland", "Greece" }, 1870, 1990, 2));
				break;// large, dense,
			case TRADE_5:
				model.setTimeGraph(TradeDataSet.load(new String[] { "Germany", "France", "United Kingdom", "Spain",
						"Russia", "United", "Poland", "Austria" }, 1920, 1939));
				break;// large, dense,
			case COLLAB_VALPO_1:
				model.setTimeGraph(CollabValpoDataSet.load(new File("./data/2001-2011-(all_cl_valpo.bib)-edgedef.csv"),
						"Cell Biology"));
			case COLLAB_INFOVIS:
				model.setTimeGraph(InfovisDataSet.load(frame));
			case NEWCOMB:
				model.setTimeGraph(NewComb.loadData(new File("./data/newcomb")));
			case LIEVE:
				model.setTimeGraph(LieveDataSet.load());
			}

		}

		frame.setTitle("Cubix " + dataSetName);

		// Visualize
		cubeVis1.createCube(model.getTimeGraph());
		cubeVis1.updateData();
		cubeVis1.display();
		frame.pack();
		frame.setVisible(true);

		// shutdown the program on windows close event
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent ev) {
				System.exit(0);
			}
		});

		exportSVGTimeslices();
		exportSVGNodeslices();

		// Export graph
	}

	public static void restart() {
		frame.setVisible(false);
		CubixVis.WEIGHT_MIN = 1000000;
		CubixVis.WEIGHT_MAX = 0;
		// CubixVisInteractive.destroyInstance();
		DataModel.destroyInstance();
		ViewManager.destroyInstance();

		main(new String[] {});
	}

	public static void createScreenshot() {
		BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		frame.printAll(g);
		image.flush();
		File here = new File("");
		File f = new File(here.getAbsolutePath() + "/screenshots/");
		if (!f.exists())
			f.mkdir();

		f = new File(here.getAbsolutePath() + "/screenshots/" + dataSetName + "/");
		if (!f.exists())
			f.mkdir();

		File[] files = f.listFiles(new FilenameContainsFilter(dataSetName + "_" + cubeVis1.getCurrentView().getName()));
		try {
			ImageIO.write(image, "png", new File(here.getAbsoluteFile() + "/screenshots/" + dataSetName + "/"
					+ dataSetName + "_" + cubeVis1.getCurrentView().getName() + "_" + files.length + ".png"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void exportSVGTimeslices() {

		File f = new File("");
		f = new File(f.getAbsolutePath() + "/svg/");
		if (!f.exists())
			f.mkdir();

		int num = f.listFiles(new FilenameContainsFilter("TimeSlices")).length;
		f = new File(f.getAbsolutePath() + "/" + dataSetName + "-TimeSlices-" + num + ".svg");
		try {
			FileWriter w = new FileWriter(f);
			w.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">\n");

			MatrixCube cube = cubeVis1.getMatrixCube();
			float[] sPos, cPos, fPos;
			GraphSMView v = (GraphSMView) ViewManager.getInstance().getView(ViewManager.VIEW_GRAPH_SM);
			String line;
			int ySpace, xSpace = 0;
			int sliceCount = 0;
			int GAP = 10;
			for (Slice<?, ?> s : cube.getTimeSlices()) {
				xSpace = (sliceCount % v.colAmount);
				xSpace *= GAP;
				ySpace = (sliceCount / v.colAmount);
				ySpace *= GAP;

				sPos = Utils.add(Utils.add(v.getSlicePosition(s), new float[] { 0f, -ySpace + 0.0f, xSpace + 0.0f }),
						new float[] { 0, cubeVis1.getSliceHeight(s) / 2f, -cubeVis1.getSliceWidth(s) / 2 });

				// Matrix frame
				line = "<rect x=\"" + sPos[2] + "\" y=\"" + -sPos[1] + "\" width=\"" + cubeVis1.getSliceWidth(s)
						+ "\" height=\"" + cubeVis1.getSliceHeight(s)
						+ "\" fill=\"none\" style=\"stroke:rgb(0,0,0);stroke-width:0.2\"/>";
				w.append("\t" + line + "\n");

				// FRAME CUT
				fPos = Utils.add(sPos, new float[] { 0, 3, -2 });
				line = "<rect x=\"" + fPos[2] + "\" y=\"" + -fPos[1] + "\" width=\"" + (cubeVis1.getSliceWidth(s) + 7)
						+ "\" height=\"" + (cubeVis1.getSliceHeight(s) + 3)
						+ "\" fill=\"none\" style=\"stroke:rgb(0,0,0);stroke-width:0.2\"/>";
				w.append("\t" + line + "\n");

				float width = 0;
				for (Cell c : s.getCells()) {
					cPos = Utils.add(c.getRelTimeSlicePos(),
							new float[] { sPos[2] + cubeVis1.getSliceWidth(s) / 2f - .5f,
									(sPos[1] - cubeVis1.getSliceHeight(s) / 2f + .5f), 0f });
					width = (float) Map.map(c.getData().getWeight(), cubeVis1.WEIGHT_MIN, cubeVis1.WEIGHT_MAX, 0.1, 1)
							* .8f;
					cPos = Utils.add(cPos, new float[] { .4f - width / 2f, -(.4f - width / 2f) });
					line = "<rect x=\"" + cPos[0] + "\" y=\"" + -cPos[1] + "\" width=\"" + width + "\" height=\""
							+ width + "\"/>";
					w.append("\t" + line + "\n");
				}

				// SLICE LABEL
				float[] p = new float[2];
				p[0] = sPos[2] + cubeVis1.getSliceWidth(s) + .5f;
				p[1] = (-sPos[1] - 1f);
				line = "<text x=\"" + p[0] + "\" y=\"" + p[1] + "\" font-size=\".9\">" + s.getLabel() + "</text>";
				w.append("\t" + line + "\n");

				// LABELS
				// NORTH
				Slice s2;
				float[] lPos;
				for (int col = 0; col < s.getColumnCount(); col++) {
					s2 = cube.getVNodeSlice(col);

					lPos = s.getRelGridCoords(0, col).clone();
					lPos[0] = sPos[2] + lPos[0] + cubeVis1.getSliceWidth(s) / 2f;
					lPos[1] = (-sPos[1] - .5f);
					line = "<text x=\"" + lPos[0] + "\" y=\"" + lPos[1] + "\" transform=\"rotate(-90 " + lPos[0] + ","
							+ lPos[1] + ")\" font-size=\".6\">" + s2.getLabel() + "</text>";
					w.append("\t" + line + "\n");
				}
				// EAST
				for (int row = 0; row < s.getRowCount(); row++) {
					s2 = cube.getHNodeSlice(row);

					lPos = s.getRelGridCoords(row, s.getColumnCount()).clone();
					lPos[0] = sPos[2] + lPos[0] + cubeVis1.getSliceWidth(s) / 2f;
					lPos[1] = -sPos[1] - lPos[1] + cubeVis1.getSliceHeight(s) / 2f;
					line = "<text x=\"" + lPos[0] + "\" y=\"" + lPos[1] + "\" font-size=\".6\">" + s2.getLabel()
							+ "</text>";
					w.append("\t" + line + "\n");
				}

				// HOLES
				w.append("\t<circle cx=\"" + (sPos[2] - 1) + "\" cy=\"" + (sPos[1] - .5f)
						+ "\" r=\"0.5\" fill-opacity=\"0.0\" style=\"stroke:rgb(0,0,0);stroke-width:0.1\"/>");
				w.append("\t<circle cx=\"" + (sPos[2] - 1) + "\" cy=\"" + (sPos[1] - (cubeVis1.getSliceHeight(s) - 1f))
						+ "\" r=\"0.5\" fill-opacity=\"0.0\" style=\"stroke:rgb(0,0,0);stroke-width:0.1\"/>");

				sliceCount++;
			}

			w.append("</svg>");
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void exportSVGNodeslices() {

		File f = new File("");
		f = new File(f.getAbsolutePath() + "/svg/");
		if (!f.exists())
			f.mkdir();

		int num = f.listFiles(new FilenameContainsFilter("NodeSlices")).length;
		f = new File(f.getAbsolutePath() + "/" + dataSetName + "-NodeSlices-" + num + ".svg");
		try {
			FileWriter w = new FileWriter(f);
			w.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">\n");

			MatrixCube cube = cubeVis1.getMatrixCube();
			float[] sPos, cPos, fPos;
			NodeSMView v = (NodeSMView) ViewManager.getInstance().getView(ViewManager.VIEW_NODE_SM);
			String line;
			int ySpace, xSpace = 0;
			int sliceCount = 0;
			int GAP = 10;
			float width = 0;
			for (Slice<?, ?> s : cube.getVNodeSlices()) {
				xSpace = (sliceCount % v.colAmount);
				xSpace *= GAP;
				ySpace = (sliceCount / v.colAmount);
				ySpace *= GAP;
				Log.out(xSpace + " " + ySpace);
				sPos = Utils.add(Utils.add(v.getSlicePosition(s), new float[] { xSpace + 0.0f, -ySpace + 0.0f, 0f }),
						new float[] { 0, cubeVis1.getSliceHeight(s) / 2f, -cubeVis1.getSliceWidth(s) / 2 });

				// Matrix frame
				line = "<rect x=\"" + sPos[0] + "\" y=\"" + -sPos[1] + "\" width=\"" + cubeVis1.getSliceWidth(s)
						+ "\" height=\"" + cubeVis1.getSliceHeight(s)
						+ "\" fill=\"none\" style=\"stroke:rgb(0,0,0);stroke-width:0.2\"/>";
				w.append("\t" + line + "\n");

				// FRAME CUT
				fPos = Utils.add(sPos, new float[] { -2, 3, 0 });
				line = "<rect x=\"" + fPos[0] + "\" y=\"" + -fPos[1] + "\" width=\"" + (cubeVis1.getSliceWidth(s) + 7)
						+ "\" height=\"" + (cubeVis1.getSliceHeight(s) + 3)
						+ "\" fill=\"none\" style=\"stroke:rgb(0,0,0);stroke-width:0.2\"/>";
				w.append("\t" + line + "\n");

				// CELLS
				for (Cell c : s.getCells()) {
					cPos = Utils.add(c.getRelVNodeSlicePos(),
							new float[] { 0, (sPos[1] - cubeVis1.getSliceHeight(s) / 2f + .5f),
									sPos[0] + cubeVis1.getSliceWidth(s) / 2f - .5f });
					width = (float) Map.map(c.getData().getWeight(), cubeVis1.WEIGHT_MIN, cubeVis1.WEIGHT_MAX, 0.1, 1)
							* .8f;
					cPos = Utils.add(cPos, new float[] { 0, -(.4f - width / 2f), .4f - width / 2f });
					line = "<rect x=\"" + cPos[2] + "\" y=\"" + -cPos[1] + "\" width=\"" + width + "\" height=\""
							+ width + "\"/>";
					w.append("\t" + line + "\n");
				}

				// SLICE LABEL
				float[] p = new float[2];
				p[0] = sPos[0] + cubeVis1.getSliceWidth(s) + .5f;
				p[1] = (-sPos[1] - 1f);
				line = "<text x=\"" + p[0] + "\" y=\"" + p[1] + "\" font-size=\".9\">" + s.getLabel() + "</text>";
				w.append("\t" + line + "\n");

				// NORTH
				Slice s2;
				float[] lPos;
				for (int col = 0; col < s.getColumnCount(); col++) {
					s2 = cube.getTimeSlice(col);

					lPos = s.getRelGridCoords(0, col).clone();
					lPos[0] = sPos[0] + lPos[2] + cubeVis1.getSliceWidth(s) / 2f;
					lPos[1] = (-sPos[1] - .5f);
					line = "<text x=\"" + lPos[0] + "\" y=\"" + lPos[1] + "\" transform=\"rotate(-90 " + lPos[0] + ","
							+ lPos[1] + ")\" font-size=\".6\">" + s2.getLabel() + "</text>";
					w.append("\t" + line + "\n");
				}
				// EAST
				for (int row = 0; row < s.getRowCount(); row++) {
					s2 = cube.getHNodeSlice(row);

					lPos = s.getRelGridCoords(row, s.getColumnCount()).clone();
					lPos[0] = sPos[0] + lPos[2] + cubeVis1.getSliceWidth(s) / 2f;
					lPos[1] = -sPos[1] - lPos[1] + cubeVis1.getSliceHeight(s) / 2f;
					line = "<text x=\"" + lPos[0] + "\" y=\"" + lPos[1] + "\" font-size=\".6\">" + s2.getLabel()
							+ "</text>";
					w.append("\t" + line + "\n");
				}
				// HOLES
				w.append("\t<circle cx=\"" + (sPos[0] - 1) + "\" cy=\"" + (-sPos[1] + 1f)
						+ "\" r=\"0.5\" fill-opacity=\"0.0\" style=\"stroke:rgb(0,0,0);stroke-width:0.1\"/>");
				w.append("\t<circle cx=\"" + (sPos[0] - 1) + "\" cy=\"" + (-sPos[1] + (cubeVis1.getSliceHeight(s) - 1))
						+ "\" r=\"0.5\" fill-opacity=\"0.0\" style=\"stroke:rgb(0,0,0);stroke-width:0.1\"/>");

				sliceCount++;
			}

			w.append("</svg>");
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
