package tp3.partieB;

import java.util.*;

/**
 * Simulation avancee :
 *  - canal reseau avec delais aleatoires
 *  - simulation de panne (P2 tombe en panne)
 *  - compteur de messages echanges
 *  - affichage de la file de requetes a chaque changement
 */
public class LamportDistributedMutex {
    public static void main(String[] args) throws InterruptedException {
        int N = 3;
        double perte = args.length > 0 ? Double.parseDouble(args[0]) : 0.05;
        CanalReseau canal = new CanalReseau(perte, 50, 150);

        List<Processus> tous = new ArrayList<>();
        for (int i = 1; i <= N; i++) tous.add(new Processus(i, canal));
        for (Processus p : tous) p.setAutres(new ArrayList<>(tous));

        System.out.println("=== Simulation " + N + " processus, perte=" +
                (perte * 100) + "% ===\n");

        List<Thread> threads = new ArrayList<>();
        for (Processus p : tous) {
            Thread t = new Thread(p);
            threads.add(t);
            t.start();
        }

        Thread.sleep(6000);
        System.out.println("\n>>> P2 tombe en panne !\n");
        tous.get(1).setEnPanne(true);

        Thread.sleep(12000);

        System.out.println("\n>>> Arret de la simulation...\n");
        for (Processus p : tous) p.stop();
        for (Thread t : threads) t.join(3000);

        System.out.println("\n===== STATISTIQUES =====");
        System.out.println("Messages echanges : " + canal.getNombreMessages());
        System.out.println("Messages perdus   : " + canal.getMessagesPerdus());
        int totA = 0, totE = 0;
        for (Processus p : tous) {
            totA += p.getTotalAttenteMs();
            totE += p.getNbEntrees();
            int moy = p.getNbEntrees() > 0 ? p.getTotalAttenteMs() / p.getNbEntrees() : 0;
            System.out.println("P" + p.getId() + " : " + p.getNbEntrees()
                    + " entrees, attente moy " + moy + "ms");
        }
        System.out.println("Attente moyenne globale : "
                + (totE > 0 ? totA / totE : 0) + "ms");
        System.out.println("========================");
    }
}
