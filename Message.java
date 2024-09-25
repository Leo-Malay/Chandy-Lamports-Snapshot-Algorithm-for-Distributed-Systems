import java.io.*;
import java.util.*;

enum MessageType {
    APPLICATION,
    MARKER,
    MARKER_REPLY,
    MARKER_REJECTION,
    END_SNAPSHOT,
    DEMARKER,
    DEMARKER_REPLY,
    DEMARKER_REJECTION,
    CUSTOM_END
};

public class Message implements Serializable {
    private static final long serialVersionUID = 1L; // For serialization

    public MessageType messageType;
    public int senderId = -1;
    public int messagesSent;
    public int messagesReceived;
    public String message;
    public boolean state;
    public Vector<Integer> clock;
    public Map<Integer, Vector<Integer>> localSnapshots;
    public Set<Integer> parents;

    public Message(int senderId, Vector<Integer> timestamp, String message) {
        this.messageType = MessageType.APPLICATION;
        this.message = message;
        this.clock = timestamp;
        this.senderId = senderId;
    }

    public Message(int senderId) {
        this.messageType = MessageType.MARKER;
        this.senderId = senderId;
    }

    public Message() {
        this.messageType = MessageType.MARKER_REJECTION;
    }

    public Message(int senderId, int senderId1) {
        this.messageType = MessageType.CUSTOM_END;
    }

    public Message(int senderId, Map<Integer, Vector<Integer>> localSnapshots, boolean state,
            Integer messagesSent, Integer messagesReceived) {
        this.messageType = MessageType.MARKER_REPLY;
        this.senderId = senderId;
        this.localSnapshots = localSnapshots;
        this.state = state;
        this.messagesSent = messagesSent;
        this.messagesReceived = messagesReceived;
    }

    public Message(String message, Set<Integer> parents) {
        this.messageType = MessageType.END_SNAPSHOT;
        this.message = message;
        this.parents = parents;
    }

    // Convert current instance of Message to byte array
    public byte[] toMessageBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(this);
        }
        return byteArrayOutputStream.toByteArray();
    }

    // Retrieve Message from byte array
    public static Message fromByteArray(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (Message) objectInputStream.readObject();
        }
    }
}
