import java.util.HashMap;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Random;

public class map {

	private static int basestation = 3;
	private static int clients = 5;

	private static double[][] Qij = new double[basestation][clients];
	private static double[][] weight = new double[basestation][clients];
	private static HashMap<Integer, Integer> allocation = new HashMap<>();// key is clientNo, value is basestationNo
	private static ArrayList<String> configuration = new ArrayList<>();

	private static double[][] DisUsertoSta; // distance of user to base station
	public static void main(String[] args) {
		data dt = new data(basestation, clients);
		map ld = new map();
		draw dr = new draw(dt);
		DisUsertoSta = dt.getDus();

		dr.addWindowListener(new HandlerWindowClosing());
		ld.generateCigema();

		ld.generateWeight();
		ld.randomAllocation();

		System.out.println("\n**********Initial Allocation**********");
		for (int i = 0; i < basestation; i++) {
			ld.printAllocation(i);
		}

		ld.balancing();
	}

	public void generateWeight() {

		System.out.println("Qij: ");
		for (int i = 0; i < basestation; i++) {
			for (int j = 0; j < clients; j++) {
				weight[i][j] = Qij[i][j];
				System.out.print(Qij[i][j] + "\t");
			}
			System.out.println();
		}
	}
	
	static void choices(int i, int numP,int numL, int[] using){
		for(int d=numP-1;d>=0;d--){
			using[d] = (using[d]+1)%numL; 
			if(using[d]!=0)break;
		}

	}
	
	public void generateCigema() {
		for (int i = 0; i < basestation; i++) {
			for (int j = 0; j < clients; j++) {
				Qij[i][j] = calcSignalStrength(DisUsertoSta[i][j]); // randomly generate Xij from 0 to 1
			}
		}
	}

	public void randomAllocation() {

		Random r = new Random();
		int machineNo;
		String str = "";

		for (int i = 0; i < clients; i++) {
			machineNo = r.nextInt(basestation); // randomly allocate to machine 0 to 9
			allocation.put(i, machineNo);
			str = str + machineNo + ", ";
		}

		configuration.add(str);
	}

	public void printAllocation(int machineNo) {

		ArrayList<Integer> clientList = new ArrayList<>();
		ArrayList<Double> weightList = new ArrayList<>();

		for (int clientNo : allocation.keySet()) {
			if (allocation.get(clientNo).equals(machineNo)) {
				clientList.add(clientNo);
				weightList.add(weight[machineNo][clientNo]);
			}
		}

		System.out.println("Machine" + machineNo + ": ");
		System.out.println(clientList);
		System.out.println(weightList);
	}

	public void balancing() {

		map ld = new map();
		int sum;

		while (true) {
			sum = 0;

			for (int i = 0; i < clients; i++)
				sum += ld.willChange(i);

			if (sum == 0)
				break;
		}
	}

	public int willChange(int clientNo) {

		map ld = new map();
		int currentMachine = allocation.get(clientNo);
		double currentLoad = ld.getMachineLoad(currentMachine);
		double currentThroughtput = Qij[currentMachine][clientNo] / currentLoad;
		double newLoad, newThroughtput;
		int change = 0;
		String str = "";

		for (int i = 0; i < basestation; i++) {
			if (i != currentMachine) {
				newLoad = ld.getMachineLoad(i) + weight[i][clientNo];
				newThroughtput = Qij[i][clientNo] / newLoad;

				if (((newThroughtput - currentThroughtput) / currentThroughtput) > 0.1) {
					allocation.put(clientNo, i);

					System.out.println("\n**********New Allocation**********");
					for (int j = 0; j < basestation; j++) {
						ld.printAllocation(j);
					}

					for (int j : allocation.keySet()) {
						str = str + allocation.get(j) + ", ";
					}

					for (String str1 : configuration) {
						if (str.equals(str1)) {
							System.out.println("\n��THERE IS NO PURE NASH EQUILIBRIUM!��");
							System.exit(0);
						}
					}
					configuration.add(str);
					change = 1;
					break;
				}
			}
		}
		return change;
	}

	public double getMachineLoad(int machineNo) {

		double load = 0;

		for (int clientNo : allocation.keySet()) {
			if (allocation.get(clientNo).equals(machineNo)) {
				load += weight[machineNo][clientNo];
			}
		}

		return load;
	}

	public double calcSignalStrength(double d) {

		double d0 = 1.0;
		double a = 4.6, b = 0.0075, c = 12.6;
		double hTx = 7.0, hRx = 1.5;
		double f = 28000.0, y = 299792458.0 / (f * 1000000.0);
		double power = 1.0;

		double PLd0 = 20.0 * Math.log10((4.0 * Math.PI * d0) / y);
		double n = a - b * hTx + c / hTx;
		double Xfc = 6.0 * Math.log10(f / 2000.0);
		double XRx = -10.8 * Math.log10(hRx / 2.0);

		double PLSUId = PLd0 + 10.0 * n * Math.log10(d / d0) + Xfc + XRx;
		double signalStrength = power * Math.pow(10.0, -PLSUId / 10.0);

		return signalStrength;
	}

	static class HandlerWindowClosing extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			System.exit(0);
		}
	}
}

class data {

	private int b = 0;
	private int m = 1010;
	private int n = 1010;

	private int sta;
	private int cli;
	private int[][] ls = new int[m][n];
	private int[][] lc = new int[m][n];
	private double[][] dus;

	public int getM() {
		return m;
	}

	public int getN() {
		return n;
	}

	public int getSta() {
		return sta;
	}

	public int getCli() {
		return cli;
	}

	public int[][] getLs() {
		return ls;
	}

	public int[][] getLc() {
		return lc;
	}

	public double[][] getDus() {
		return dus;
	}

	public data(int sta, int cli) {
		this.sta = sta;
		this.cli = cli;
		this.dus = new double[sta][cli];

		for (int i = 20; i < 950; i = i + 200) {
			for (int j = 40; j < 950; j = j + 346) {
				this.ls[i][j] = 1;
			}
		}
		for (int i = 120; i < 950; i = i + 200) {
			for (int j = 213; j < 950; j = j + 346) {
				this.ls[i][j] = 1;
			}
		}

		for (int a = 0; a < this.cli; a++) {
			b = 0;
			Random r = new Random();
			int c = r.nextInt(1000);
			int d = r.nextInt(1000);
			this.lc[c][d] = 1;
			for (int i = 10; i < 960; i = i + 10) {
				for (int j = 1; j < 960; j++) {
					if (this.ls[i + 10][j + 10] == 1) {
						Path p1 = new Path();
						p1.x = (double) i;
						p1.y = (double) j;
						Path p2 = new Path();
						p2.x = (double) (c + 2);
						p2.y = (double) (d + 2);
						double x = p1.getDistance(p2);
						this.dus[b][a] = x;
						b++;
					}
				}
			}
		}
	}

	class Path {
		double x;
		double y;

		public double getDistance(Path p2) {
			double r = Math.sqrt((p2.x - x) * (p2.x - x) + (p2.y - y) * (p2.y - y));
			return r;
		}
	}
}

class draw extends Frame {

	private static final long serialVersionUID = 1L;

	private static data d;

	public draw(data d) {
		super("painting");
		setSize(1000, 1000);
		setVisible(true);
		draw.d = d;
	}

	public void paint(Graphics g) {
		int[][] ls = d.getLs();
		int[][] lc = d.getLc();

		g.setColor(Color.RED);
		for (int i = 0; i < ls.length; i++) {
			for (int j = 0; j < ls[i].length; j++) {
				if (ls[i][j] == 1) {
					g.fillOval(i, j, 20, 20);
				}
			}
		}

		g.setColor(Color.BLUE);
		for (int i = 0; i < lc.length; i++) {
			for (int j = 0; j < lc[i].length; j++) {
				if (lc[i][j] == 1) {
					g.fillOval(i, j, 4, 4);
				}
			}
		}
	}
}
