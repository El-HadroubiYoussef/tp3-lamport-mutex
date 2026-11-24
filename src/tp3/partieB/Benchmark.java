package tp3.partieB;

import java.util.*;

/**
 * Benchmark : nombre de messages echanges en fonction du nombre de processus.
 * Chaque processus effectue K iterations de section critique.
 */
public class Benchmark {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Benchmark : messages vs nombre de processus ===\n");
        System.out.println("N\tEnvoyes\t\tPerdus");

        int[] ns = {2, 3, 4, 5, 6, 7};
        int[] envois = new int[ns.length];
        int[] perdus = new int[ns.length];

        for (int i = 0; i < ns.length; i++) {
            int[] r = runOnce(ns[i]);
            envois[i] = r[0];
            perdus[i] = r[1];
            System.out.printf("%d\t%d\t\t%d%n", ns[i], envois[i], perdus[i]);
        }

        System.out.println("\n=== Graphe ASCII ===\n");
        int max = 1;
        for (int m : envois) if (m > max) max = m;

        for (int i = 0; i < ns.length; i++) {
            int len = (int) Math.round((envois[i] * 50.0) / max);
            StringBuilder bar = new StringBuilder();
            for (int j = 0; j < len; j++) bar.append('#');
            System.out.printf("N=%d | %-50s %d%n", ns[i], bar, envois[i]);
        }
    }

    static int[] runOnce(int n) throws InterruptedException {
        CanalReseau canal = new CanalReseau(0.05, 10, 30);
        List<Processus> tous = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Processus p = new Processus(i, canal);
            p.setCsDuration(20, 40);
            p.setMaxIterations(2);
            tous.add(p);
        }
        for (Processus p : tous) p.setAutres(new ArrayList<>(tous));

        List<Thread> threads = new ArrayList<>();
        for (Processus p : tous) {
            Thread t = new Thread(p);
            threads.add(t);
            t.start();
        }

        // securite : timeout global de 60s
        long start = System.currentTimeMillis();
        for (Thread t : threads) {
            long reste = Math.max(1000, 60000 - (System.currentTimeMillis() - start));
            t.join(reste);
        }
        for (Processus p : tous) p.stop();

        return new int[]{ canal.getNombreMessages(), canal.getMessagesPerdus() };
    }
}
