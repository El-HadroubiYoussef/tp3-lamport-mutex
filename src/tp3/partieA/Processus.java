package tp3.partieA;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Processus extends Thread {
    private final int id;
    private int clock = 0;
    private List<Processus> autres;
    private final Queue<Message> fileMessages = new ConcurrentLinkedQueue<>();
    private final Set<Integer> reponsesRecues = new HashSet<>();
    private final List<Message> fileDemandes = new ArrayList<>();

    public Processus(int id) { this.id = id; }

    public void setAutres(List<Processus> autres) { this.autres = autres; }

    private synchronized int tick() { clock++; return clock; }
    private synchronized int horloge() { return clock; }
    private synchronized void majHorloge(int autre) {
        clock = Math.max(clock, autre) + 1;
    }

    private void envoyer(Message m, Processus dest) {
        if (dest != null) dest.recoitMessage(m);
    }

    public synchronized void recoitMessage(Message m) {
        majHorloge(m.timestamp);
        fileMessages.add(m);
    }

    private void broadcast(Message.Type type) {
        int ts = tick();
        Message m = new Message(type, id, ts);
        for (Processus p : autres) {
            if (p.id != this.id) envoyer(m, p);
        }
    }

    private void demandeSC() throws InterruptedException {
        int ts = tick();
        System.out.println("Processus " + id + " demande SC a t=" + ts);
        Message demande = new Message(Message.Type.REQUEST, id, ts);
        fileDemandes.add(demande);
        broadcast(Message.Type.REQUEST);
        reponsesRecues.clear();
        reponsesRecues.add(id);

        while (reponsesRecues.size() < autres.size()) {
            traiterMessages();
            Thread.sleep(100);
        }

        while (true) {
            Message premier = fileDemandes.isEmpty() ? null : fileDemandes.get(0);
            if (premier != null && premier.senderId == id) break;
            traiterMessages();
            Thread.sleep(50);
        }

        entrerSC();
    }

    private void entrerSC() throws InterruptedException {
        System.out.println("==> Processus " + id + " entre en SC (t=" + horloge() + ")");
        Thread.sleep(1000);
        sortirSC();
    }

    private void sortirSC() {
        System.out.println("<== Processus " + id + " quitte SC (t=" + horloge() + ")");
        final int myId = this.id;
        fileDemandes.removeIf(m -> m.senderId == myId);
        broadcast(Message.Type.RELEASE);
    }

    private void traiterMessages() {
        Message m;
        while ((m = fileMessages.poll()) != null) {
            final Message currentMsg = m;
            switch (currentMsg.type) {
                case REQUEST:
                    fileDemandes.add(currentMsg);
                    fileDemandes.sort(Comparator.comparingInt(x -> x.timestamp));
                    envoyer(new Message(Message.Type.REPLY, id, horloge()),
                            getProcessus(currentMsg.senderId));
                    break;
                case REPLY:
                    reponsesRecues.add(currentMsg.senderId);
                    break;
                case RELEASE:
                    fileDemandes.removeIf(x -> x.senderId == currentMsg.senderId);
                    break;
            }
        }
    }

    private Processus getProcessus(int id) {
        for (Processus p : autres) if (p.id == id) return p;
        return null;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < 3; i++) {
                Thread.sleep(500 + new Random().nextInt(1000));
                demandeSC();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
