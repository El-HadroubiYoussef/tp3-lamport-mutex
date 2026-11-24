package tp3.partieA;

public class Message {
    public enum Type { REQUEST, REPLY, RELEASE }

    public final Type type;
    public final int senderId;
    public final int timestamp;

    public Message(Type type, int senderId, int timestamp) {
        this.type = type;
        this.senderId = senderId;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return type + "(P" + senderId + "@t=" + timestamp + ")";
    }
}
