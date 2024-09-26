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

    public Map<Integer, Vector<Integer>> localSnapshots = new HashMap<>();
    public boolean state;

    public ChandyLamport(Node node) {
        this.node = node;
        this.color = Color.BLUE;
        this.state = false;
    }

    public void reset() {
        this.state = false;
        this.color = Color.BLUE;
        this.markerReceived = 0;
        this.markersSent = 0;
        this.msgSent = 0;
        this.msgReceived = 0;
        this.localSnapshots = new HashMap<>();
    }

    public void initSpanningTree() throws Exception {
        System.out.println("[INIT] Snapshot Spanning process at NODE: " + this.node.id);

        this.color = Color.RED;

        this.printStatus();
        // System.out.println("[TRACE] Channels are "+node.idToChannelMap);
        for (Map.Entry<Integer, Socket> entry : node.idToChannelMap.entrySet()) {
            Socket channel = entry.getValue();
            Message msg = new Message(node.id); // MARKER Message Constructor
            System.out.println("[TRACE] Sending " + msg.messageType + " to " + entry.getKey());
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

    public void receiveMarkerMessageFromParent(Message marker) throws Exception {
        if (this.color == Color.RED) {
            Message rejectMarker = new Message();
            Socket channel = this.node.idToChannelMap.get(marker.senderId);
            Client.sendMsg(rejectMarker, channel, node);
            System.out.println("[REJECTED] MARKER message from NODE-" + marker.senderId);
            // printStatus();
            return;
        }

        this.color = Color.RED;
        this.parentId = marker.senderId;

        for (Map.Entry<Integer, Socket> entry : node.idToChannelMap.entrySet()) {
            Socket channel = entry.getValue();

            Message msg = new Message(node.id); // MARKER Message Constructor
            synchronized (node) {
                Client.sendMsg(msg, channel, node);
                this.markersSent++;
            }
        }

        System.out.println("[ACCEPTED] MARKER message from NODE-" + marker.senderId);
        // printStatus();
        checkTreeCollapse();
    }

    public void receiveMarkerRejectionMsg(Message markerRejectionMsg) throws Exception {
        this.markerReceived += 1;
        checkTreeCollapse();
    }

    public void receiveSnapshotResetMsg(Message resetMessage) throws Exception {
        if (this.color == Color.BLUE) {
            // System.out.println("[REJECTED] END_SNAPSHOT from" + node.id);
            return;
        }
        synchronized (node) {
            this.reset();
        }
        System.out.println("[SNAPSHOT] Snapshot Reset");

        for (Map.Entry<Integer, Socket> entry : node.idToChannelMap.entrySet()) {
            if (entry.getKey() == 0 || resetMessage.parents.contains(entry.getKey())) {
                System.out.println("[REFRAIN] Refraining from sending end snapshot message to Node " + entry.getKey());
                continue;
            }
            Socket channel = entry.getValue();

            Set<Integer> parents = new HashSet<>(resetMessage.parents);
            parents.add(this.node.id);
            Message msg = new Message(resetMessage.message, parents); // RESET SNAPSHOT Message Constructor
            synchronized (node) {
                Client.sendMsg(msg, channel, node);
            }
        }
    }

    public void receiveMarkerRepliesFromChild(Message markerReply) throws Exception {

        this.localSnapshots.putAll(markerReply.localSnapshots);

        this.msgSent += markerReply.messagesSent;
        this.msgReceived += markerReply.messagesReceived;

        if (markerReply.state == true) {
            this.state = true;
        }

        this.markerReceived++;
        System.out.println("[MARKER REPLY ACCEPTED]");
        printStatus();

        checkTreeCollapse();
        // System.out.println("[CHANNEL INPUT RESPONSE] MARKER_REPLY message is
        // handled");
    };

    public void checkTreeCollapse() throws Exception {
        System.out.println("[COLLAPSE] Tree collapse at Node-" + node.id);
        if (markersSent == markerReceived) {
            this.localSnapshots.put(node.id, node.clock);
            this.msgSent += node.msgSent;
            this.msgReceived += node.msgReceived;
            if (node.state == true) {
                System.out.println("[ALERT] Node is still active");
                this.state = true;
            }

            writeOutput(node.id, node.clock);

            if (node.id == 0) {
                handleConvergence();
                return;
            }
            Message markerReplyMsg = new Message(
                    node.id,
                    localSnapshots,
                    state,
                    msgSent,
                    msgReceived);
            Client.sendMsg(markerReplyMsg, node.idToChannelMap.get(parentId), node);
        }

    }

    public void handleConvergence() throws Exception {
        System.out.println("===============  Convergence  ===============");
        System.out.println("Snapshots(Local):   " + localSnapshots);
        System.out.println("Messages sent:      " + msgSent);
        System.out.println("Messages received:  " + msgReceived);
        System.out.println("States gathered:    " + state);
        System.out.println("=============================================\n");
        verifyConsistency(localSnapshots, node.totalNodes);
        this.initSnapshotReset();
    }

    public void initSnapshotReset() throws Exception {
        System.out.println("[INIT] Snapshot Reset");

        this.color = Color.BLUE;

        Boolean TERMINATED = false;

        for (Map.Entry<Integer, Socket> entry : node.idToChannelMap.entrySet()) {
            Socket channel = entry.getValue();

            String messageText;
            if (this.state == true || this.msgSent != this.msgReceived) {
                messageText = "System not terminated";
            } else {
                messageText = "System terminated";
                TERMINATED = true;
            }

            Set<Integer> parents = new HashSet<>();
            parents.add(0);
            Message msg = new Message(messageText, parents); // END_SNAPSHOT Message Constructor
            synchronized (node) {
                Client.sendMsg(msg, channel, node);
            }
        }

        this.reset();

        if (node.id == 0 && !TERMINATED) {
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

    /**
     * This function is used to verfiy the consistency of the all the gathered
     * states
     */
    public static void verifyConsistency(Map<Integer, Vector<Integer>> gatheredLocalSnapshots, int n) {
        boolean consistent = true;

        for (Map.Entry<Integer, Vector<Integer>> entry : gatheredLocalSnapshots.entrySet()) {
            int current = entry.getKey();

            for (int i = 0; i < n; i++) {
                if (gatheredLocalSnapshots.containsKey(i)) {
                    int reference = gatheredLocalSnapshots.get(i).get(i);
                    for (int j = 0; j < n; j++) {
                        if (gatheredLocalSnapshots.containsKey(j)) {
                            if (gatheredLocalSnapshots.get(j).get(i) > reference) {
                                consistent = false;
                            }
                        }
                    }
                }
            }
        }

        System.out.println("================================");
        System.out.println("Conistency:  " + (consistent ? "VERIFIED" : "INVALID"));
        System.out.println("================================\n");
    }

    /**
     * This function is used for writing clock value to output file
     */
    public static void writeOutput(int nodeId, Vector<Integer> clock) throws Exception {
        // Specifying dynamic name for file
        String filename = "config-" + nodeId + ".out";
        // Opening file or creating if not exist
        FileOutputStream stream = new FileOutputStream(filename, true);
        PrintWriter writer = new PrintWriter(stream);
        // Writing clock in the file
        for (Integer i : clock) {
            writer.print(i + " ");
        }
        writer.println();
        // Closing file object
        writer.close();
        stream.close();
    }
}