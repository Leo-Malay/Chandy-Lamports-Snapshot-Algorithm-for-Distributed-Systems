import java.io.*;
import java.util.*;

enum MessageType {
    APPLICATION,
    MARKER,
    MARKER_REPLY,
    MARKER_REJECTION,
    END_SNAPSHOT,
    CUSTOM_END
};

public class Message implements Serializable {
    public int id = -1;
    public MessageType messageType;

    public int msgSent;
    public int msgReceived;
    
    public String message;
    public boolean state;
    
    public Vector<Integer> clock;
    public Map<Integer, Vector<Integer>> localSs;
    public Set<Integer> parents;

    public Message(int id, Vector<Integer> timestamp, String message) {
        this.messageType = MessageType.APPLICATION;
        this.id = id;
        this.clock = timestamp;
        this.message = message;
    }

    public Message(int id) {
        this.messageType = MessageType.MARKER;
        this.id = id;
    }

    public Message(int id, Map<Integer, Vector<Integer>> localSs, boolean state,
            Integer msgSent, Integer msgReceived) {
        this.messageType = MessageType.MARKER_REPLY;
        this.id = id;
        this.localSs = localSs;
        this.state = state;
        this.msgSent = msgSent;
        this.msgReceived = msgReceived;
    }

    public Message() {
        this.messageType = MessageType.MARKER_REJECTION;
    }

    public Message(String message, Set<Integer> parents) {
        this.messageType = MessageType.END_SNAPSHOT;
        this.message = message;
        this.parents = parents;
    }

    public Message(int id, int id1) {
        this.messageType = MessageType.CUSTOM_END;
    }

    public byte[] toMessageBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(this);
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static Message fromByteArray(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (Message) objectInputStream.readObject();
        }
    }
}
