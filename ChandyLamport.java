import java.util.*;
import java.io.*;
import java.net.*;

enum ProcessColor {
    BLUE, RED
};

public class ChandyLamport {
    public Node node;
    public int parentId;

    public int markersSent = 0;
    public int markerReceived = 0;

    public int msgSent = 0;
    public int msgReceived = 0;

    public ProcessColor color;

    public Map<Integer, Vector<Integer>> localSnapshots = new HashMap<>();
    public boolean localState;

    public ChandyLamport(Node node) {
        this.node = node;
        this.color = ProcessColor.BLUE;
        this.localState = false;
    }

    public void resetSnapshot() {
        this.markersSent = 0;
        this.markerReceived = 0;
        this.color = ProcessColor.BLUE;
        this.localState = false;
        this.localSnapshots = new HashMap<>();
        this.msgSent = 0;
        this.msgReceived = 0;
    }

    public void initSpanningTree() throws Exception {
        System.out.println("[INIT] Snapshot Spanning process at NODE: " + this.node.id);

        this.color = ProcessColor.RED;

        this.snapshotStatus();
        // System.out.println("[TRACE] Channels are "+node.idToChannelMap);
        for (Map.Entry<Integer, Socket> entry : node.idToChannelMap.entrySet()) {
            Socket channel = entry.getValue();
            Message msg = new Message(node.id); // MARKER Message Constructor
            System.out.println("[TRACE] Sending " + msg.messageType + " to " + entry.getKey());
            Client.sendMsg(msg, channel, node);
            this.markersSent += 1;
        }
    }

    public void snapshotStatus() {
        System.out.println("[PROCESS COLOR]: " + this.color);
        System.out.println(String.format("[SNAPSHOT] MARKERS Sent=%d | REPLIES Received=%d", markersSent,
                markerReceived));
    }

    public void receiveMarkerMessageFromParent(Message marker) throws Exception {
        // System.out.println("[COLOR]: "+this.color);

        if (this.color == ProcessColor.RED) {
            Message rejectMarker = new Message();
            Socket channel = this.node.idToChannelMap.get(marker.senderId);
            Client.sendMsg(rejectMarker, channel, node);
            System.out.println("[REJECTED] MARKER message from NODE-" + marker.senderId);
            // snapshotStatus();
            return;
        }

        this.color = ProcessColor.RED;
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
        // snapshotStatus();
        checkTreeCollapseStatus();
    }

    public void receiveMarkerRejectionMessage(Message markerRejectionMsg) throws Exception {
        this.markerReceived += 1;
        checkTreeCollapseStatus();
    }

    public void receiveSnapshotResetMessage(Message resetMessage) throws Exception {
        if (this.color == ProcessColor.BLUE) {
            // System.out.println("[REJECTED] END_SNAPSHOT from" + node.id);
            return;
        }
        synchronized (node) {
            this.resetSnapshot();
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

    public void receiveMarkerRepliesFromChildren(Message markerReply) throws Exception {

        this.localSnapshots.putAll(markerReply.localSnapshots);

        this.msgSent += markerReply.messagesSent;
        this.msgReceived += markerReply.messagesReceived;

        if (markerReply.state == true) {
            this.localState = true;
        }

        this.markerReceived++;
        System.out.println("[MARKER REPLY ACCEPTED]");
        snapshotStatus();

        checkTreeCollapseStatus();
        // System.out.println("[CHANNEL INPUT RESPONSE] MARKER_REPLY message is
        // handled");
    };

    public void checkTreeCollapseStatus() throws Exception {
        System.out.println("[COLLAPSE] Tree collapse identified at NODE:" + this.node.id);
        if (this.markersSent == this.markerReceived) {
            this.localSnapshots.put(this.node.id, node.clock);
            this.msgSent += this.node.msgSent;
            this.msgReceived += this.node.msgReceived;
            if (this.node.state == true) {
                System.out.println("[ALERT] Node is still active");
                this.localState = true;
            }

            writeOutput(this.node.id, this.node.clock);

            if (this.node.id == 0) {
                handleConvergence();
                return;
            }
            Message markerReplyMsg = new Message(
                    this.node.id,
                    this.localSnapshots,
                    this.localState,
                    this.msgSent,
                    this.msgReceived);
            Client.sendMsg(markerReplyMsg, this.node.idToChannelMap.get(this.parentId), node);
        } else {
            System.out.println("Waiting for SENT and RECEIVE to be EQUAL");
        }

    }

    public void handleConvergence() throws Exception {
        System.out.println("[CONVERGENCE] Euler Traversal successfully completed at node 0.");
        System.out.println("[CONVERGENCE] Local Snapshots = " + this.localSnapshots);
        System.out.println("[CONVERGENCE] Total messages sent = " + this.msgSent);
        System.out.println("[CONVERGENCE] Total messages received = " + this.msgReceived);
        System.out.println("[CONVERGENCE] Node state gathered = " + this.localState);
        verifyConsistency(this.localSnapshots, this.node.totalNodes);
        this.initiateSnapshotReset();
        // this.initiateDemarkationProcess();
    }

    public void initiateSnapshotReset() throws Exception {
        System.out.println("[INIT] Snapshot Reset");

        this.color = ProcessColor.BLUE;

        Boolean TERMINATED = false;

        for (Map.Entry<Integer, Socket> entry : node.idToChannelMap.entrySet()) {
            Socket channel = entry.getValue();

            String messageText;
            if (this.localState == true || this.msgSent != this.msgReceived) {
                messageText = "**** SYSTEM IS NOT TERMINATED ****";
            } else {
                messageText = "**** YOU ARE TERMINATED ****";
                TERMINATED = true;
            }

            Set<Integer> parents = new HashSet<>();
            parents.add(0);
            Message msg = new Message(messageText, parents); // END_SNAPSHOT Message Constructor
            synchronized (node) {
                Client.sendMsg(msg, channel, node);
            }
        }

        this.resetSnapshot();

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
            System.out.println("SNAPSHOT PROTOCOL DETECTED TERMINATION. NOT FURTHER SPANNING;");
        }
    }

    // Additional Helper Functions
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
            ;
        }
        ;

        if (consistent) {
            System.out.println("**************** CONSISTENCY VERIFIED ****************");
        } else {
            System.out.println("**************** CONSISTENCY FAILED ****************");
        }
    }

    public static void writeOutput(int nodeId, Vector<Integer> clock) throws Exception {

        String filename = String.format("config-%d.out", nodeId);

        FileOutputStream stream = new FileOutputStream(filename, true);
        PrintWriter writer = new PrintWriter(stream);

        for (Integer i : clock) {
            writer.print(i);
            writer.print(" ");
        }
        writer.println();
        writer.close();
        stream.close();
    }
}