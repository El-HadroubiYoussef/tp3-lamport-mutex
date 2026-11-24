# Compte Rendu – TP3 : Coordination et Cohérence dans les Systèmes Distribués

**Nom :** El Hadroubi Youssef
**Module :** Architecture Distribuée & Cloud Computing  
**Sujet :** Exclusion Mutuelle Distribuée via Horloges de Lamport

## 1. Introduction

L’objectif de ce TP est de comprendre les mécanismes de coordination dans un système distribué où il n’existe ni mémoire partagée ni horloge globale. Nous avons implémenté l’**algorithme de Ricart-Agrawala** simplifié, utilisant les **horloges logiques de Lamport** pour garantir l’exclusion mutuelle lors de l’accès à une section critique (SC).

Le projet est divisé en deux parties :

- **Partie A :** Simulation basique avec communication directe entre threads.
- **Partie B :** Simulation avancée intégrant un canal réseau réaliste (délais, pertes) et la tolérance aux pannes.

## 2. Partie A : Simulation Basique

Dans cette première partie, nous avons simulé 3 processus (`Processus`) s’exécutant comme des threads Java.

### Fonctionnement

Chaque processus possède une horloge logique locale. Lorsqu’il souhaite entrer en Section Critique :

1.  Il incrémente son horloge et diffuse un message `REQUEST` à tous les autres.
2.  Il attend de recevoir un `REPLY` de chaque autre processus.
3.  Il vérifie qu’il est le premier dans sa file d’attente locale (triée par timestamp Lamport).
4.  Il entre en SC, puis diffuse un `RELEASE` à la sortie.

### Résultat

La simulation montre que les processus accèdent à la SC un par un, sans conflit. L’ordre d’accès est déterminé par l’ordre total défini par `(timestamp, ID_processus)`.

## 3. Partie B : Simulation Avancée (Réseau & Pannes)

Cette partie introduit des contraintes réalistes via la classe `CanalReseau`.

### Améliorations Implémentées

- **Canal Réseau Simulé :** Les messages ne sont plus instantanés. Un délai aléatoire (50–150 ms) est appliqué à chaque envoi pour simuler la latence réseau.
- **Tolérance aux Pannes :** Un processus peut tomber en panne (`setEnPanne(true)`). Dans ce cas, il ignore les messages entrants et ne répond plus aux requêtes.
- **Gestion des Pertes de Messages :** Le canal simule une perte de paquets (probabilité configurable, ex: 5%). Notre implémentation utilise des **Timeouts** pour retransmettre les requêtes `REQUEST` si les `REPLY` ne sont pas reçus à temps.
- **Prévention des Deadlocks :** Un mécanisme de "Lease" (bail) a été ajouté. Si une requête reste en tête de file trop longtemps (sans entrer en SC), elle est évictée pour éviter qu’un processus mort ne bloque le système indéfiniment.

## 4. Réponses aux Questions du Travail Demandé

### Q1. Observer le comportement en cas de perte de messages

Avec une probabilité de perte de 5%, nous observons que certains messages `REQUEST` ou `REPLY` n’arrivent jamais à destination.

- **Solution :** Grâce au mécanisme de **retransmission sur timeout** (`TIMEOUT_REPLY_MS = 1500ms`), le processus émetteur renvoie sa demande après un délai d’attente. Cela garantit la progression du système malgré les pertes, au prix d’une augmentation du nombre total de messages échangés.

### Q2. Modifier la probabilité de panne

Nous avons simulé la panne du Processus 2 (`P2`) après 6 secondes de fonctionnement.

- **Observation :** `P2` cesse d’envoyer des `REPLY`. Les processus restants (`P1` et `P3`) détectent implicitement cette absence via le comptage des réponses attendues (`autres.size() - nbPannes()`). Ils continuent à s’échanger la SC entre eux sans attendre `P2`, démontrant la **tolérance aux pannes**.

### Q3. Compteur de temps moyen d’attente pour entrer en SC

Nous avons ajouté des compteurs (`totalAttenteMs` et `nbEntrees`) dans la classe `Processus`.

- **Résultat typique :**
  - Attente moyenne globale : ~800ms (varie selon la charge et les délais réseau).
  - Cette métrique permet d’évaluer l’efficacité de l’algorithme : plus le réseau est lent ou chargé, plus l’attente augmente.

### Q4. Graphe du nombre de messages échangés selon N

Nous avons utilisé la classe `Benchmark.java` pour faire varier le nombre de processus $N$ de 2 à 7. Chaque processus effectue 2 entrées en SC.

| N (Processus) | Messages Envoyés | Messages Perdus (5%) |
| :------------ | :--------------- | :------------------- |
| 2             | 12               | 0                    |
| 3             | 44               | 1                    |
| 4             | 115              | 8                    |
| 5             | 142              | 4                    |
| 6             | 333              | 23                   |
| 7             | 467              | 28                   |

_(Voir le fichier `benchmark_output.txt` ou l’exécution de `run_benchmark.sh` pour le graphe ASCII généré)_.
On observe une croissance quasi-quadratique du nombre de messages, ce qui est caractéristique des algorithmes de type Ricart-Agrawala ($O(N)$ messages par entrée en SC).

### Q5. Tableau global des concepts clés

Voir le fichier **`CONCEPTS.md`** inclus dans le dépôt. Il résume les notions de Communication, Synchronisation, Ordonnancement Causal, Exclusion Mutuelle, et Tolérance aux Pannes avec leurs exemples concrets dans ce TP.

## 5. Conclusion

Ce TP nous a permis de mettre en pratique la théorie des horloges de Lamport. Nous avons constaté que :

1.  L’absence d’horloge globale nécessite un ordonnancement logique pour coordonner les accès.
2.  La fiabilité d’un système distribué repose sur la gestion explicite des échecs (pertes de messages, pannes de nœuds).
3.  L’algorithme de Ricart-Agrawala offre une bonne équité mais génère un trafic réseau important ($2(N-1)$ messages par entrée en SC), ce qui peut limiter la scalabilité pour un très grand nombre de processus.
