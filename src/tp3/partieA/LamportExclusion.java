package tp3.partieA;

import java.util.*;

public class LamportExclusion {
    public static void main(String[] args) throws InterruptedException {
        int n = 3;
        List<Processus> liste = new ArrayList<>();
        for (int i = 0; i < n; i++) liste.add(new Processus(i));
        for (Processus p : liste) p.setAutres(liste);
        for (Processus p : liste) p.start();
        for (Processus p : liste) p.join();
        System.out.println("\nSimulation terminee.");
    }
}
