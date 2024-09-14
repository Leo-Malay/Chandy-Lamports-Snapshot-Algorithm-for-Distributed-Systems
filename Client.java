import java.util.*;
import java.io.*;
import java.net.*;

public class Client {

    List<Socket> channelList;
    Node node;

    public Client(Node node) {
        this.node = node;
        synchronized (node) {
            this.channelList = connectChannels(node);
        }
    }

    public List<Socket> connectChannels(Node node) {
        System.out.println("[INFO] Making Client Channel Array....");
        List<Socket> channelList = new ArrayList<>();
        for (Integer neighbour : node.neighbours.get(node.id)) {
            String host = node.getHost(neighbour);
            int port = node.getPort(neighbour);
            try {
                Socket client = new Socket();
                client.connect(new InetSocketAddress(host, port));
                client.setKeepAlive(true);
                channelList.add(client);
                node.idToChannelMap.put(node.hostToId_PortMap.get(host).get(0), client);
                System.out.println("[INFO] Connected to " + host + ":" + port);
            } catch (IOException error) {
                System.out.println("[ERRO] Unable to connect to " + host + ":" + port);
                // error.printStackTrace();
            }
        }
        return channelList;
    }

    public void sendApplicationMessageLogic() {
        while (true) {
            if (node.msgSent >= node.maxNumber) {
                System.out.println("[STATE CHANGE] Node sent maximum number of messages. Going permanently passive");
                node.state = false;
                node.printNodeVectorClock();
                break;
            }

            if (node.state == true) {
                Random random = new Random();
                int count = random.nextInt(node.maxPerActive - node.minPerActive + 1) + node.minPerActive;
                sendBatchMessages(count);
            } else {
                try {
                    System.out.println(String.format("Node is temporarily passive (Messages sent: [%d/%d]",
                            node.msgSent, node.maxNumber));
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        ;
        // System.out.println("Exiting sendLogic() method.");
    }

    public void sendBatchMessages(int count) {

        // This method sends 'count' number of messages to a randomly chosen neighbor
        System.out.println(String.format("Sending a batch of %d messages", count));
        Random random = new Random();

        for (int i = 0; i < count; i++) {

            if (node.msgSent >= node.maxNumber) {
                node.state = false;
                break;
            }

            int randomNumber = random.nextInt(this.channelList.size());
            int destination = node.neighbours.get(node.id).get(randomNumber);
            node.sndClk.set(destination, node.sndClk.get(destination) + 1);
            Socket channel = channelList.get(randomNumber);

            String messageString = String.format(
                    "Hi from %s! (%d/%d)", node.name, node.msgSent + 1, node.maxNumber);
            Message msg = new Message(node.id, node.clock, messageString);

            synchronized (node) { // This block increments the value of vector clock after sending a message
                Client.send_message(msg, channel, node);
            }

            try {
                Thread.sleep(node.minSendDelay);
                // System.out.println(String.format("Delaying sending messages for %d
                // milliseconds", node.minSendDelay));
                // System.out.println();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // System.out.println("[STATE CHANGE] Flipping node state from active to passive
        // because a batch of messages are sent to neighbors.");
        System.out.println(String.format("[DEBUG][Node:%d] (%d) After sending: ", node.id, node.msgSent) + node.clock);
        node.changeState();

    }

    public static void send_message(Message msg, Socket channel, Node node) {

        try {
            OutputStream out = channel.getOutputStream();
            DataOutputStream dataOut = new DataOutputStream(out);

            byte[] messageBytes = msg.toMessageBytes();
            dataOut.writeInt(messageBytes.length); // Send message length
            dataOut.write(messageBytes); // Send message
            dataOut.flush();

            if (msg.messageType == MessageType.APPLICATION) {
                int prevEntry = node.clock.get(node.id);
                node.clock.set(node.id, prevEntry + 1);
                node.msgSent++;
            }
        } catch (IOException error) {
            error.printStackTrace();
        }
    }

    public void init() {
        Thread client = new Thread(() -> {
            System.out.println("[INFO] Client Starting...");
            try {
                if (node.id == 0) {
                    node.changeState();
                }
                node.client.sendApplicationMessageLogic();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        client.start();
    }
}