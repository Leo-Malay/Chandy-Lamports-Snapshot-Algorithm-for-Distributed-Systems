import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.regex.*;

public class Node {
    // Node
    int id;
    String name;
    String port;
    List<List<Integer>> neighbours = new ArrayList<>();
    // Config
    int totalNodes;
    int minPerActive;
    int maxPerActive;
    int minSendDelay;
    int snapshotDelay;
    int maxNumber;
    // Variable
    int msgSent = 0;
    int msgReceived = 0;
    int custom_end = 0;
    boolean state = false;

    Vector<Integer> clock = new Vector<>();
    Vector<Integer> sndClk = new Vector<>();
    Vector<Integer> rcvClk = new Vector<>();

    // Components
    Server server;
    Client client;

    // Helper
    Map<String, List<Integer>> hostToId_PortMap = new HashMap<>();
    Map<Integer, List<String>> idToHost_PortMap = new HashMap<>();
    Map<Integer, Socket> idToChannelMap = new HashMap<>();

    public Node(int id) {
        this.id = id;
    }

    public static void main(String[] args) {
        // Init Node
        Node node = new Node(Integer.parseInt(args[0]));
        // Parse the config file
        node.readConfig();
        // Init Vector Clock;
        node.initVectorClock();
        // Print details
        node.printNodeConfig();
        node.printNodeNeighbours();

        // Server
        node.server = new Server(node.getPort(), node);
        node.server.init();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Client
        node.client = new Client(node);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        node.client.init();

    }

    public void readConfig() {
        // Declring Variables
        String CONFIG_FILE_NAME = "config.txt";
        String line;
        int configLine = 0;
        // REGEX Pattern
        Pattern REGEX_PATTERN_CONFIG = Pattern.compile("^\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");

        try {
            // Creating a reader for config file
            BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE_NAME));
            // Looping over the whole file, reading line by line
            while ((line = reader.readLine()) != null) {
                // Ignoring comments and empty line
                line = line.split("#")[0].trim();
                if (line.isEmpty())
                    continue;

                // Match for config file.
                Matcher configMatcher = REGEX_PATTERN_CONFIG.matcher(line);
                if (configMatcher.matches()) {
                    this.totalNodes = Integer.parseInt(configMatcher.group(1));
                    this.minPerActive = Integer.parseInt(configMatcher.group(2));
                    this.maxPerActive = Integer.parseInt(configMatcher.group(3));
                    this.minSendDelay = Integer.parseInt(configMatcher.group(4));
                    this.snapshotDelay = Integer.parseInt(configMatcher.group(5));
                    this.maxNumber = Integer.parseInt(configMatcher.group(6));
                } else if (configLine <= totalNodes) {

                    // All this lines are in format [ XXXX XXXX XXXX ]
                    String[] nodeConf = line.split(" ");
                    // Extracting data for read node.
                    int node_Id = Integer.parseInt(nodeConf[0]);
                    String node_Host = nodeConf[1];
                    int node_Port = Integer.parseInt(nodeConf[2]);

                    List<String> valueA = new ArrayList<>();
                    valueA.add(node_Host);
                    valueA.add(String.valueOf(node_Port));
                    List<Integer> valueB = new ArrayList<>();
                    valueB.add(node_Id);
                    valueB.add(node_Port);

                    this.idToHost_PortMap.put(node_Id, valueA);
                    this.hostToId_PortMap.put(node_Host, valueB);

                } else {
                    // We know top (n + 1) lines are for config. Thenafter nth line contain nth node
                    String[] node_neighbours = line.split(" ");
                    List<Integer> valueA = new ArrayList<>();
                    for (String n : node_neighbours) {
                        valueA.add(Integer.parseInt(n));
                    }
                    this.neighbours.add(valueA);
                }
                configLine += 1;
            }
            // Closing reader
            reader.close();
        } catch (

        IOException e) {
            e.printStackTrace();
        }
    }

    public String getHost() {
        return idToHost_PortMap.get(id).get(0);
    }

    public String getHost(int id) {
        return idToHost_PortMap.get(id).get(0);
    }

    public int getPort() {
        return Integer.parseInt(idToHost_PortMap.get(id).get(1));
    }

    public int getPort(int id) {
        return Integer.parseInt(idToHost_PortMap.get(id).get(1));
    }

    public void initVectorClock() {
        for (int i = 0; i < totalNodes; i++) {
            this.clock.add(0);
            this.sndClk.add(0);
            this.rcvClk.add(0);
        }
    }

    public void changeState() {
        this.state = !state;
    }

    /* ========== HELPER FUNCTIONS ========== */
    public void printNodeConfig() {
        System.out.println("========== Node Config ==========");
        System.out.println("Node Id:        " + id);
        System.out.println("Node Host:      " + getHost());
        System.out.println("Node Port:      " + getPort());
        System.out.println("Total Nodes:    " + totalNodes);
        System.out.println("Min Per Active: " + minPerActive);
        System.out.println("Max Per Active: " + maxPerActive);
        System.out.println("Min Send Delay: " + minSendDelay);
        System.out.println("Snapshot Delay: " + snapshotDelay);
        System.out.println("Max Number:     " + maxNumber);
        System.out.println("=================================\n");
    }

    public void printNodeNeighbours() {
        System.out.println("======== Node Neighbours ========");
        for (Integer neighbour : neighbours.get(id)) {
            System.out.println("Id: " + neighbour + " | Host: " + idToHost_PortMap.get(neighbour).get(0) + " | Port: "
                    + idToHost_PortMap.get(neighbour).get(1));
        }
        System.out.println("=================================\n");
    }

    public void printNodeVectorClock() {
        int totalSent = 0, totalReceive = 0;
        System.out.println("======= Node Vector Clock =======");
        for (int i = 0; i < totalNodes; i++) {
            System.out.println("NodeId: " + i + " | Msg: " + clock.get(i) + " | Send: " + sndClk.get(i) + " | Recieve: "
                    + rcvClk.get(i));
            totalSent += sndClk.get(i);
            totalReceive += rcvClk.get(i);
        }
        System.out.println("Total Send: " + totalSent + " | Total Recieve: " + totalReceive + " | Diff: "
                + (totalSent - totalReceive));
        System.out.println("=================================\n");
    }

}