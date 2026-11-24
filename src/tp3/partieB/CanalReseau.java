package tp3.partieB;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Canal reseau simule : delais aleatoires + perte de messages optionnelle.
 */
public class CanalReseau {
    private final Random random = new Random();
    private final AtomicInteger compteurMessages = new AtomicInteger(0);
    private final AtomicInteger messagesPerdus  = new AtomicInteger(0);
    private final double probabilitePerte;
    private final int delaiMin, delaiMax;

    public CanalReseau() { this(0.0, 50, 150); }

    public CanalReseau(double probabilitePerte, int delaiMin, int delaiMax) {
        this.probabilitePerte = probabilitePerte;
        this.delaiMin = delaiMin;
        this.delaiMax = delaiMax;
    }

    public void envoyerMessage(Processus src, Processus dest, Message msg) {
        compteurMessages.incrementAndGet();
        if (dest == null) return;
        if (random.nextDouble() < probabilitePerte) {
            messagesPerdus.incrementAndGet();
            return;
        }
        int delai = delaiMin + random.nextInt(Math.max(1, delaiMax - delaiMin + 1));
        new Thread(() -> {
            try {
                Thread.sleep(delai);
                dest.recevoirMessage(msg);
            } catch (InterruptedException ignored) {}
        }).start();
    }

    public int getNombreMessages()  { return compteurMessages.get(); }
    public int getMessagesPerdus()   { return messagesPerdus.get(); }
}
