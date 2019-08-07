import java.util.HashMap;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Random;

/*
 * find  no nash equilibrium
 */

class Location {

    private int length;
    private int width;
    private int[][] lsta;
    private int[][] lcli;
    private double[][] dus;

    Location(int sta, int cli, int len, int wid) {
        length = len;
        width = wid;
        lsta = new int[length][width];
        lcli = new int[length][width];
        dus = new double[sta][cli];
        fillLsta();
        generateLcliDus(cli);
    }

    public int getLength() {
        return length;
    }

    public int getWidth() {
        return width;
    }

    public int[][] getLsta() {
        return lsta;
    }

    public int[][] getLcli() {
        return lcli;
    }

    public double[][] getDus() {
        return dus;
    }

    private void fillLsta() {
        for (int i = 100; i < length; i += 200) {
            for (int j = 87; j < width; j += 346) {
                lsta[i][j] = 1;
            }
        }
        for (int i = 200; i < length; i += 200) {
            for (int j = 260; j < width; j += 346) {
                lsta[i][j] = 1;
            }
        }
    }

    public void generateLcliDus(int clients) {
        int ds = 0;
        lcli = new int[length][width];

        for (int dc = 0; dc < clients; dc++) {
            ds = 0;
            Random rand = new Random();
            int cx = rand.nextInt(length);
            int cy = rand.nextInt(width);
            lcli[cx][cy] = 1;
            for (int i = 100; i < length; i += 100) {
                for (int j = 87; j < width; j += 173) {
                    if (lsta[i][j] == 1) {
                        Path s = new Path();
                        s.x = (double) i + 10;
                        s.y = (double) j + 10;
                        Path c = new Path();
                        c.x = (double) (cx + 2);
                        c.y = (double) (cy + 2);
                        this.dus[ds][dc] = s.getDistance(c);
                        ds++;
                    }
                }
            }
        }
    }

    class Path {

        double x;
        double y;

        public double getDistance(Path other) {
            return Math.sqrt((other.x - x) * (other.x - x) + (other.y - y) * (other.y - y));
        }
    }
}

class Draw extends Frame {

    private static final long serialVersionUID = 1L;
    private Location location;

    Draw(Location loc) {
        super("painting");
        location = loc;
        setSize(location.getLength(), location.getWidth());
        setVisible(true);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        int[][] ls = location.getLsta();
        int[][] lc = location.getLcli();

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

    public void redraw() {
        repaint();
    }
}

public class NNmap{

    private int stations;
    private int clients;
    private int length;
    private int width;
    private Location location;
    private Draw draw;

    private double[][] Qij;
    private double[][] DisUsertoSta;
    private static double[][] Weight;
    private static HashMap<Integer, Integer> allocation = new HashMap<Integer, Integer>();
    private static ArrayList<String> configuration = new ArrayList<String>();

    NNmap(int sta, int cli) {
        if (Math.pow(Math.sqrt(sta), 2) != (double) sta) {
            System.err.println("Station quantity invalid");
            System.exit(1);
        }
        stations = sta;
        clients = cli;
        length = (2 * (int) Math.sqrt(sta) + 1) * 100;
        width = (int) Math.sqrt(sta) * 173;
        location = new Location(stations, clients, length, width);
        draw = new Draw(location);
        draw.addWindowListener(new HandlerWindowClosing());
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

    public void generateCigema() {
        DisUsertoSta = location.getDus();
        Qij = new double[length][width];
        for (int i = 0; i < stations; i++) {
            for (int j = 0; j < clients; j++) {
                Qij[i][j] = calcSignalStrength(DisUsertoSta[i][j]);
            }
        }
    }

    public void generateWeight() {
        Weight = new double[stations][clients];
        for (int i = 0; i < stations; i++) {
            for (int j = 0; j < clients; j++) {
                Weight[i][j] = Qij[i][j];
            }
        }
    }

    public boolean allDisplay() {
        boolean status = false;
        int counter = 0; // TODO: clean counter

        for (int i = 0; i < Math.pow(stations, clients); i++) {
            String allocCase = "";
            counter++;// TODO: clean counter
            allocation.clear();
            configuration.clear();

            int remainder = i;
            for (int j = 0; j < clients; j++) {
                int machineNo = remainder % stations;
                allocation.put(j, machineNo);
                allocCase = allocCase + machineNo + ", ";
                remainder = remainder / stations;
            }

            configuration.add(allocCase);
            status = balancing();
            if (status) {
                System.out.println("counter = " + counter);
                return status;
            }
        }

        System.out.println("counter = " + counter);
        return status;
    }

    public boolean balancing() {
        int sum;
        while (true) {
            sum = 0;
            for (int i = 0; i < clients; i++) {
                int status = change(i);
                if (status == 2) {
                    return false;
                } else {
                    sum += status;
                }
            }
            if (sum == 0) {
                return true;
            }
        }
    }

    public int change(int clientNo) {
        int currentMachine = allocation.get(clientNo);
        double currentLoad = getMachineLoad(currentMachine);
        double currentThroughtput = Qij[currentMachine][clientNo] / currentLoad;
        double newLoad, newThroughtput;
        String str = "";
        int ret = 0;

        for (int i = 0; i < stations; i++) {
            if (i != currentMachine) {
                newLoad = getMachineLoad(i) + Weight[i][clientNo];
                newThroughtput = Qij[i][clientNo] / newLoad;

                if (((newThroughtput - currentThroughtput) / currentThroughtput) > 0.1) {
                    allocation.put(clientNo, i);

                    for (int j : allocation.keySet()) {
                        str = str + allocation.get(j) + ", ";
                    }

                    for (String str1 : configuration) {
                        if (str.equals(str1)) {
                            return 2;
                        }
                    }
                    configuration.add(str);
                    ret = 1;
                    break;
                }
            }
        }

        return ret;
    }

    public double getMachineLoad(int machineNo) {
        double load = 0;

        for (int clientNo : allocation.keySet()) {
            if (allocation.get(clientNo).equals(machineNo)) {
                load += Weight[machineNo][clientNo];
            }
        }

        return load;
    }

    public void printAllocation(int machineNo) {
        ArrayList<Integer> clientList = new ArrayList<>();
        ArrayList<Double> weightList = new ArrayList<>();

        for (int clientNo : allocation.keySet()) {
            if (allocation.get(clientNo).equals(machineNo)) {
                clientList.add(clientNo);
                weightList.add(Weight[machineNo][clientNo]);
            }
        }

        System.out.println("Machine" + machineNo + ": ");
        System.out.println(clientList);
        System.out.println(weightList);
    }

    static class HandlerWindowClosing extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
            System.exit(0);
        }
    }

    public void run() {
        boolean status = false;

        do {
            location.generateLcliDus(clients);
            draw.redraw();
            generateCigema();
            generateWeight();
            status = allDisplay();
            for (int j = 0; j < stations; j++) {
                printAllocation(j);
            }
        } while (status);

        for (int j = 0; j < stations; j++) {
            printAllocation(j);
        }
        System.out.println("END");
    }

    public static void main(String[] args) {
        new NNmap(4, 8).run();
    }
}
