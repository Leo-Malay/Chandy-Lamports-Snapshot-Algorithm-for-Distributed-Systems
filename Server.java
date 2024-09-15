import java.io.*;
import java.net.*;

public class Server {
    int port;
    Node node;
    ServerSocket server;

    public Server(int port, Node node) {
        this.port = port;
        this.node = node;
    }

    public void wakeNodeIfPassive(Message msg) {
        if (!node.state && node.msgSent < node.maxNumber) {
            node.changeState();
        }
    }

    public void handleMessage(Message msg) {
        System.out.println("Message received from " + msg.senderId);
        // Message Handler
        switch (msg.messageType) {
            case MessageType.APPLICATION:
                wakeNodeIfPassive(msg);
                for (int i = 0; i < node.totalNodes; i++) {
                    int value = Math.max(node.clock.get(i), msg.clock.get(i));
                    node.clock.set(i, value);
                }
                node.rcvClk.set(msg.senderId, node.rcvClk.get(msg.senderId) + 1);
                node.msgReceived += 1;
                break;
            case MessageType.CUSTOM_END:
                node.custom_end++;

                System.out.println("Node Id:" + node.id + " | Neighbour: " + node.neighbours.size());
                System.out.println("# of Custom End: " + node.custom_end + " | # of Neighbours: "
                        + node.neighbours.get(node.id).size());
                if (node.custom_end == node.neighbours.get(node.id).size())
                    node.printNodeVectorClock();

                break;
            case MessageType.MARKER:

                System.out.println("[MARKER : received] Received MARKER message from NODE: " + msg.senderId);
                // this.app.snapshot.receiveMarkerMessageFromParent(msg);
                break;
            case MessageType.MARKER_REJECTION:
                System.out.println(
                        "[MARKER_REJECTION : received] Received MARKER_REJECTION message from " + msg.senderId);
                // this.app.snapshot.receiveMarkerRejectionMessage(msg);
                break;
            case MessageType.MARKER_REPLY:

                System.out.println("[MARKER_REPLY : received] Received MARKER_REPLY message from " + msg.senderId);
                // this.app.snapshot.receiveMarkerRepliesFromChildren(msg);
                break;
            case MessageType.END_SNAPSHOT:

                System.out.println("[END_SNAPSHOT: received] Received END_SNAPSHOT message from " + msg.senderId);
                // this.app.snapshot.receiveSnapshotResetMessage(msg);
                break;
        }
    }

    public void listen() {
        try {
            this.server = new ServerSocket(port);
            System.out.println("[INFO]: Server started on " + port);

            while (true) {
                Socket client = server.accept();
                // Start a new thread to handle the client connection
                Thread listener = new Thread(() -> {
                    try {
                        InputStream clientInputStream = client.getInputStream();
                        DataInputStream dataInputStream = new DataInputStream(clientInputStream);

                        while (!client.isClosed()) {

                            try {
                                // Reading Incoming Message.
                                int length = dataInputStream.readInt();
                                byte[] buffer = new byte[length];
                                dataInputStream.readFully(buffer);
                                Message msg = Message.fromByteArray(buffer);
                                // node.rcvBefClk.set(msg.senderId, node.rcvBefClk.get(msg.senderId) + 1);
                                synchronized (node) {
                                    handleMessage(msg);
                                }
                            } catch (EOFException e) {
                                System.out.println("[ERROR]: Connection closed by client.");
                                break;
                            } catch (IOException | ClassNotFoundException e) {
                                e.printStackTrace();
                                break;
                            }

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                listener.start();
            }
        } catch (

        IOException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        Thread server = new Thread(() -> {
            System.out.println("[INFO] Server Starting...");
            try {
                node.server.listen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        server.start();
    }
}
