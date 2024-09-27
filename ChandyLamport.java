import java.util.*;
import java.io.*;
import java.net.*;

enum Color {
    BLUE, RED
};

public class ChandyLamport {
    public Node node;
    public int parentId;

    public int markersSent = 0;
    public int markerReceived = 0;

    public int msgSent = 0;
    public int msgReceived = 0;

    public Color color;

    public Map<Integer, Vector<Integer>> localSs = new HashMap<>();
    public boolean state;

    public ChandyLamport(Node node) {
        this.node = node;
        this.color = Color.BLUE;
        this.state = false;
    }

    public void initSpanningTree() throws Exception {
        System.out.println("[INIT] Snapshot Spanning process at NODE: " + this.node.id);

        this.color = Color.RED;

        this.printStatus();

        for (Map.Entry<Integer, Socket> entry : node.idToChannelMap.entrySet()) {
            Socket channel = entry.getValue();
            Message msg = new Message(node.id);
            System.out.println("[CL] Sending " + msg.messageType + " to Node" + entry.getKey());
            Client.sendMsg(msg, channel, node);
            this.markersSent += 1;
        }
    }

    public void printStatus() {
        System.out.println("========== Snapshot Status ==========");
        System.out.println("Color: " + color);
        System.out.println("MARKERS Sent: " + markersSent);
        System.out.println("MARKERS Received:" + markerReceived);
        System.out.println("====================================\n");
    }

    public void handleMarkerMessageFromParent(Message msg) throws Exception {
        if (this.color == Color.RED) {
            System.out.println("[REJECTED] MARKER message from Node " + msg.id);
            Message rejectMarker = new Message();
            Socket channel = this.node.idToChannelMap.get(msg.id);
            synchronized(node) {
                Client.sendMsg(rejectMarker, channel, node);
            }
            return;
        }

        this.color = Color.RED;
        this.parentId = msg.id;

        for (Map.Entry<Integer, Socket> entry : node.idToChannelMap.entrySet()) {
            Socket channel = entry.getValue();

            Message msg = new Message(node.id);
            synchronized (node) {
                Client.sendMsg(msg, channel, node);
                this.markersSent++;
            }
        }

        System.out.println("[ACCEPTED] MARKER message from Node " + msg.id);
        checkTreeCollapse();
    }

    public void handleMarkerRejectionMsg(Message msg) throws Exception {
        this.markerReceived += 1;
        checkTreeCollapse();
    }

    public void handleSnapshotResetMsg(Message msg) throws Exception {
        if (this.color == Color.BLUE) return;
        synchronized (node) {
            System.out.println("[SNAPSHOT] Snapshot Reset");
            this.reset();
        }

        for (Map.Entry<Integer, Socket> entry : node.idToChannelMap.entrySet()) {
            if (entry.getKey() == 0 || msg.parents.contains(entry.getKey())) continue;
            Socket channel = entry.getValue();

            Set<Integer> parents = new HashSet<>(msg.parents);
            parents.add(this.node.id);
            Message resetMsg = new Message(msg.message, parents);
            synchronized (node) {
                Client.sendMsg(resetMsg, channel, node);
            }
        }
    }

    public void handleMarkerRepliesFromChild(Message msg) throws Exception {
        this.localSs.putAll(msg.localSs);

        this.msgSent += msg.msgSent;
        this.msgReceived += msg.msgReceived;

        if (msg.state == true) {
            this.state = true;
        }

        this.markerReceived++;
        System.out.println("[ACCEPTED] MARKER message from Node " + msg.id);
        printStatus();
        checkTreeCollapse();
    };

    public void checkTreeCollapse() throws Exception {
        System.out.println("[COLLAPSE] Tree collapse at Node-" + node.id);
        if (markersSent == markerReceived) {
            this.localSs.put(node.id, node.clock);
            this.msgSent += node.msgSent;
            this.msgReceived += node.msgReceived;
            if (node.state == true) {
                System.out.println("[ALERT] Node is still active");
                this.state = true;
            }

            genOutput(node.id, node.clock);

            if (node.id == 0) {
                handleConvergence();
                return;
            }
            Message markerReplyMsg = new Message(
                    node.id,
                    localSs,
                    state,
                    msgSent,
                    msgReceived);
            Client.sendMsg(markerReplyMsg, node.idToChannelMap.get(parentId), node);
        }

    }

    public void handleConvergence() throws Exception {
        System.out.println("===============  Convergence  ===============");
        System.out.println("Snapshots(Local):   " + localSs);
        System.out.println("Messages sent:      " + msgSent);
        System.out.println("Messages received:  " + msgReceived);
        System.out.println("States gathered:    " + state);
        System.out.println("=============================================\n");
        verifyConsistency(localSs, node.totalNodes);
        this.initSnapshotReset();
    }

    public void initSnapshotReset() throws Exception {
        System.out.println("[INIT] Snapshot Reset");

        this.color = Color.BLUE;
        Boolean flag = false;

        for (Map.Entry<Integer, Socket> entry : node.idToChannelMap.entrySet()) {
            Socket channel = entry.getValue();
            String msgText;
            if (this.state == true || this.msgSent != this.msgReceived) {
                msgText = "System not terminated";
            } else {
                msgText = "System terminated";
                flag = true;
            }
            Set<Integer> parents = new HashSet<>();
            parents.add(0);
            Message msg = new Message(msgText, parents);
            synchronized (node) {
                Client.sendMsg(msg, channel, node);
            }
        }
        this.reset();
        if (node.id == 0 && !flag) {
            System.out.println("[SNAPSHOT] Not Terminated");
            try {
                System.out.println("[SNAPSHOT] Process delayed for " + node.snapshotDelay);
                Thread.sleep(this.node.snapshotDelay);
                initSpanningTree();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("[SNAPSHOT]: Terminated");
        }
    }

    public void reset() {
        this.state = false;
        this.color = Color.BLUE;
        this.markerReceived = 0;
        this.markersSent = 0;
        this.msgSent = 0;
        this.msgReceived = 0;
        this.localSs = new HashMap<>();
    }

    public static void verifyConsistency(Map<Integer, Vector<Integer>> gatheredlocalSs, int n) {
        boolean flag = true;
        for (Map.Entry<Integer, Vector<Integer>> entry : gatheredlocalSs.entrySet()) {
            int curr = entry.getKey();
            for (int i = 0; i < n; i++) {
                if (gatheredlocalSs.containsKey(i)) {
                    int ref = gatheredlocalSs.get(i).get(i);
                    for (int j = 0; j < n; j++) {
                        if (gatheredlocalSs.containsKey(j)) {
                            if (gatheredlocalSs.get(j).get(i) > ref) {
                                flag = false;
                            }
                        }
                    }
                }
            }
        }

        System.out.println("================================");
        System.out.println("Conistency:  " + (flag ? "VERIFIED" : "INVALID"));
        System.out.println("================================\n");
    }

    public static void genOutput(int nodeId, Vector<Integer> clock) throws Exception {
        String filename = "config-" + nodeId + ".out";

        FileOutputStream stream = new FileOutputStream(filename, true);
        PrintWriter writer = new PrintWriter(stream);

        for (Integer i : clock) {
            writer.print(i + " ");
        }
        writer.println();

        writer.close();
        stream.close();
    }
}