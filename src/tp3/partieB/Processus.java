package tp3.partieB;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Processus distribue avec horloge de Lamport, gestion de panne,
 * compteur d'attente, affichage de la file de requetes.
 */
public class Processus implements Runnable {
    private final int id;
    private volatile int horlogeLamport = 0;
    private final CanalReseau canal;
    private volatile List<Processus> autres = new ArrayList<>();
    private volatile boolean enPanne = false;
    private volatile boolean running = true;
    private int maxIterations = Integer.MAX_VALUE;

    private final Queue<Message> fileMessages = new ConcurrentLinkedQueue<>();
    private final Set<Integer> reponsesRecues = ConcurrentHashMap.newKeySet();
    private final List<Message> fileRequetes = Collections.synchronizedList(new ArrayList<>());

    private static final long TIMEOUT_REPLY_MS = 1500;
    private static final long TIMEOUT_QUEUE_MS = 4000;

    private int csMin = 500, csMax = 1000;

    private final AtomicInteger totalAttenteMs = new AtomicInteger(0);
    private final AtomicInteger nbEntrees      = new AtomicInteger(0);

    public Processus(int id, CanalReseau canal) {
        this.id = id;
        this.canal = canal;
    }

    public int getId() { return id; }
    public void setAutres(List<Processus> autres) { this.autres = autres; }
    public void setEnPanne(boolean v) { this.enPanne = v; }
    public void stop() { this.running = false; }
    public void setMaxIterations(int n) { this.maxIterations = n; }
    public void setCsDuration(int min, int max) { this.csMin = min; this.csMax = max; }
    public int getTotalAttenteMs() { return totalAttenteMs.get(); }
    public int getNbEntrees()      { return nbEntrees.get(); }

    private synchronized int tick() { horlogeLamport++; return horlogeLamport; }
    private synchronized int horloge() { return horlogeLamport; }
    private synchronized void majHorloge(int autre) {
        horlogeLamport = Math.max(horlogeLamport, autre) + 1;
    }

    public synchronized void recevoirMessage(Message msg) {
        if (enPanne) return;
        majHorloge(msg.horloge);
        fileMessages.add(msg);
        traiterMessages();
    }

    private void traiterMessages() {
        Message m;
        while ((m = fileMessages.poll()) != null) {
            final Message currentMsg = m;
            switch (currentMsg.type) {
                case REQUEST: {
                    boolean dejaLa = false;
                    synchronized (fileRequetes) {
                        for (Message x : fileRequetes) {
                            if (x.idSource == currentMsg.idSource && x.horloge == currentMsg.horloge) {
                                dejaLa = true; break;
                            }
                        }
                        if (!dejaLa) {
                            fileRequetes.add(currentMsg);
                            fileRequetes.sort(Comparator
                                .comparingInt((Message x) -> x.horloge)
                                .thenComparingInt(x -> x.idSource));
                        }
                    }
                    if (!dejaLa) afficherFile();
                    envoyerAu(currentMsg.idSource,
                              new Message(Message.Type.REPLY, horloge(), id));
                    break;
                }
                case REPLY:
                    reponsesRecues.add(currentMsg.idSource);
                    break;
                case RELEASE:
                    synchronized (fileRequetes) {
                        fileRequetes.removeIf(x -> x.idSource == currentMsg.idSource);
                    }
                    afficherFile();
                    break;
            }
        }
    }

    private void envoyerAu(int destId, Message msg) {
        for (Processus p : autres) {
            if (p.id == destId && !p.enPanne) {
                canal.envoyerMessage(this, p, msg);
                return;
            }
        }
    }

    private void diffuser(Message.Type type) {
        diffuserAvecHorloge(type, tick());
    }

    private void diffuserAvecHorloge(Message.Type type, int ts) {
        Message m = new Message(type, ts, id);
        for (Processus p : autres) {
            if (p.id != this.id && !p.enPanne) {
                canal.envoyerMessage(this, p, m);
            }
        }
    }

    private int nbPannes() {
        int n = 0;
        for (Processus p : autres) {
            if (p.id != this.id && p.enPanne) n++;
        }
        return n;
    }

    public void demanderSectionCritique() throws InterruptedException {
        if (enPanne) return;
        long debut = System.nanoTime();

        int ts = tick();
        Message demande = new Message(Message.Type.REQUEST, ts, id);
        synchronized (fileRequetes) { fileRequetes.add(demande); }
        afficherFile();
        reponsesRecues.clear();
        reponsesRecues.add(id);
        diffuserAvecHorloge(Message.Type.REQUEST, ts);

        long deadline = System.currentTimeMillis() + TIMEOUT_REPLY_MS;

        // ---- Phase 1 : attendre les REPLY (avec resend en cas de timeout) ----
        while (running) {
            if (enPanne) { cleanup(); return; }

            int attendus = Math.max(0, autres.size() - 1 - nbPannes());
            if (reponsesRecues.size() >= attendus + 1) break;

            if (System.currentTimeMillis() > deadline) {
                diffuserAvecHorloge(Message.Type.REQUEST, ts);
                deadline = System.currentTimeMillis() + TIMEOUT_REPLY_MS;
            }
            Thread.sleep(50);
        }

        // ---- Phase 2 : attendre d'etre premier dans la file ----
        while (running) {
            if (enPanne) { cleanup(); return; }

            Message premier;
            synchronized (fileRequetes) {
                premier = fileRequetes.isEmpty() ? null : fileRequetes.get(0);
            }
            if (premier != null && premier.idSource == id) break;

            // Lease : si la requete en tete attend trop longtemps, on la retire
            if (premier != null &&
                System.currentTimeMillis() - premier.envoiMs > TIMEOUT_QUEUE_MS) {
                synchronized (fileRequetes) {
                    fileRequetes.remove(premier);
                }
                afficherFile();
            }

            if (System.currentTimeMillis() > deadline) {
                diffuserAvecHorloge(Message.Type.REQUEST, ts);
                deadline = System.currentTimeMillis() + TIMEOUT_REPLY_MS;
            }
            Thread.sleep(50);
        }

        if (!running) { cleanup(); return; }

        long attenteMs = (System.nanoTime() - debut) / 1_000_000;
        totalAttenteMs.addAndGet((int) attenteMs);
        nbEntrees.incrementAndGet();

        entrerSectionCritique(attenteMs);
        sortirSectionCritique();
    }

    private void cleanup() {
        synchronized (fileRequetes) {
            fileRequetes.removeIf(m -> m.idSource == id);
        }
    }

    private void entrerSectionCritique(long attenteMs) throws InterruptedException {
        System.out.println("==> P" + id + " entre en SC (horloge=" + horloge()
                + ", attente=" + attenteMs + "ms)");
        Thread.sleep(csMin + new Random().nextInt(Math.max(1, csMax - csMin + 1)));
    }

    private void sortirSectionCritique() {
        System.out.println("<== P" + id + " quitte la SC");
        synchronized (fileRequetes) {
            fileRequetes.removeIf(m -> m.idSource == id);
        }
        diffuser(Message.Type.RELEASE);
        afficherFile();
    }

    private void afficherFile() {
        StringBuilder sb = new StringBuilder("  File P" + id + " : [");
        synchronized (fileRequetes) {
            for (int i = 0; i < fileRequetes.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(fileRequetes.get(i));
            }
        }
        sb.append("]");
        System.out.println(sb);
    }

    @Override
    public void run() {
        try {
            int it = 0;
            while (running && it < maxIterations) {
                if (enPanne) { Thread.sleep(300); continue; }
                Thread.sleep(300 + new Random().nextInt(500));
                if (!running) break;
                demanderSectionCritique();
                it++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
