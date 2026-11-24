package tp3.partieB;

public class Message {
    public enum Type { REQUEST, REPLY, RELEASE }

    public final Type type;
    public final int horloge;
    public final int idSource;
    public final long envoiMs;

    public Message(Type type, int horloge, int idSource) {
        this.type = type;
        this.horloge = horloge;
        this.idSource = idSource;
        this.envoiMs = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return type + "(P" + idSource + "@t=" + horloge + ")";
    }
}
