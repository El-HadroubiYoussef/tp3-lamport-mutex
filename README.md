# TP3 — Coordination et Coherence dans les Systemes Distribues

Implementation en Java de l'**exclusion mutuelle distribuee** via les
**horloges logiques de Lamport** (algorithme de Ricart–Agrawala simplifie).

## Arborescence

```
tp3-lamport-mutex/
├── src/tp3/partieA/    Partie A : version basique (3 processus)
│   ├── Message.java
│   ├── Processus.java
│   └── LamportExclusion.java
├── src/tp3/partieB/    Partie B : version avancee (reseau + pannes)
│   ├── Message.java
│   ├── CanalReseau.java
│   ├── Processus.java
│   ├── LamportDistributedMutex.java
│   └── Benchmark.java
├── build.sh
├── run_parteA.sh
├── run_parteB.sh
├── run_benchmark.sh
├── Makefile
├── CONCEPTS.md
└── README.md
```

## Compilation & execution

```bash
bash build.sh

# Partie A : version basique
bash run_parteA.sh

# Partie B : version avancee (0.05 = 5 % de perte de messages)
bash run_parteB.sh 0.05

# Benchmark (graphe messages vs nombre de processus)
bash run_benchmark.sh
```

## Partie A — version basique

Trois processus (`Processus`) heritent de `Thread`. Chacun :

1. possede une **horloge logique de Lamport** (`tick`, `majHorloge`) ;
2. diffuse un `REQUEST` horodate a tous les autres ;
3. attend les `REPLY` puis verifie qu'il est **premier dans sa file** ;
4. entre en SC, simule un traitement, sort, diffuse `RELEASE`.

**Sortie attendue** (extrait) :

```
Processus 0 demande SC a t=1
Processus 1 demande SC a t=1
Processus 2 demande SC a t=2
==> Processus 1 entre en SC (t=3)
<== Processus 1 quitte SC (t=4)
==> Processus 0 entre en SC (t=5)
<== Processus 0 quitte SC (t=6)
==> Processus 2 entre en SC (t=7)
<== Processus 2 quitte SC (t=8)
```

## Partie B — version avancee

Ajouts par rapport a la Partie A :

| Fonctionnalite        | Implementation                                                           |
| --------------------- | ------------------------------------------------------------------------ |
| Canal reseau simule   | `CanalReseau` : delai 50–150 ms, spawn d'un thread par envoi             |
| Simulation de panne   | `Processus.setEnPanne(true)` → le processus ignore les messages entrants |
| Compteur de messages  | `AtomicInteger` dans `CanalReseau`                                       |
| Affichage de la file  | `afficherFile()` appelee a chaque ajout / retrait                        |
| Perte de messages     | probabilite configurable (argument CLI, defaut 5 %)                      |
| Temps d'attente moyen | `totalAttenteMs` / `nbEntrees` par processus                             |
| Evitement de deadlock | Timeouts + **lease** (eviction apres 4 s)                                |

### Reduction des exigences du sujet

1. **Observer le comportement en cas de perte de messages**
   → parametre `perte` (defaut `0.05`). Les retransmissions sont declenchees
   par les timeouts (`TIMEOUT_REPLY_MS = 1500`). Les messages perdus
   sont comptes et affiches en fin de simulation.
2. **Modifier la probabilite de panne**
   → `P2` tombe en panne a `t ≈ 6 s`. Il suffit de changer
   `tous.get(1).setEnPanne(true)` en un autre processus, ou de deplacer
   le `Thread.sleep`. Les timeouts empechent tout blocage permanent.
3. **Compteur de temps moyen d'attente pour entrer en SC**
   → chaque processus accumule `totalAttenteMs` et `nbEntrees`. La moyenne
   par processus et la moyenne globale sont affichees en fin de simulation.
4. **Graphe du nombre de messages echanges selon N**
   → `Benchmark.java` execute la simulation pour `N ∈ {2,…,7}` et produit
   un **graphe ASCII** + un tableau. Voir `run_benchmark.sh`.
5. **Tableau global des concepts cles**
   → `CONCEPTS.md`.

## Notes de conception

- **Horloge de Lamport** : `tick()` retourne la valeur courante apres
  incrementation, garantissant que deux `REQUEST` consecutifs d'un meme
  processus ont bien des timestamps strictement croissants.
- **Ordre total** : tri des requetes par `(horloge, idSource)` — la
  comparaison est identique sur tous les processus.
- **Deduplication** : un `REQUEST` identique (meme `idSource` + meme
  `horloge`) n'est pas ajoute deux fois a la file ; un `REPLY` est en
  revanche toujours renvoye (au cas ou le precedent aurait ete perdu).
- **Lease timeout** (`TIMEOUT_QUEUE_MS = 4000`) : si la requete en tete
  de file depasse 4 s, elle est evincee — cela evite qu'un processus
  tombe en panne en SC ne bloque tout le monde indefiniment.
- **Pas de reprise apres panne** dans cette version : `P2` reste mort.
  Ajouter la reprise demanderait de reinitialiser les requetes en cours ;
  c'est laisse en exercice.
