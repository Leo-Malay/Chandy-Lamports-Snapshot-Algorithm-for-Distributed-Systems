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
        if (msg.senderId != -1)
            System.out.println("[SERVER] Message received from Node " + msg.senderId);
        // Message Handler

        if (msg.messageType == MessageType.APPLICATION) {
            wakeNodeIfPassive(msg);
            for (int i = 0; i < node.totalNodes; i++) {
                int value = Math.max(node.clock.get(i), msg.clock.get(i));
                node.clock.set(i, value);
            }
            node.rcvClk.set(msg.senderId, node.rcvClk.get(msg.senderId) + 1);
            node.msgReceived += 1;
        } else if (msg.messageType == MessageType.CUSTOM_END) {
            node.custom_end++;
            if (node.pem_passive && node.custom_end == node.neighbours.get(node.id).size())
                node.printNodeVectorClock();

        } else if (msg.messageType == MessageType.MARKER) {

            System.out.println("[SERVER] Message type: MARKER");
            try {
                node.snapshot.receiveMarkerMessageFromParent(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (msg.messageType == MessageType.MARKER_REJECTION) {
            System.out.println("[SERVER] Message type: MARKER_REJECTION");
            try {
                node.snapshot.receiveMarkerRejectionMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (msg.messageType == MessageType.MARKER_REPLY) {

            System.out.println("[SERVER] Message type: MARKER_REPLY");
            try {
                node.snapshot.receiveMarkerRepliesFromChildren(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (msg.messageType == MessageType.END_SNAPSHOT) {

            System.out.println("[SERVER] Message type: END_SNAPSHOT");
            try {
                node.snapshot.receiveSnapshotResetMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void listen() {
        try {
            this.server = new ServerSocket(port);
            System.out.println("[SERVER] Started @ port: " + port);

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
                                synchronized (node) {
                                    handleMessage(msg);
                                }
                            } catch (EOFException e) {
                                System.out.println("[SERVER] Connection closed by client");
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
            System.out.println("[SERVER] Starting...");
            try {
                node.server.listen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        server.start();
    }
}
