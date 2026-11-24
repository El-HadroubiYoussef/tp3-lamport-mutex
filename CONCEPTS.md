# Tableau global — Concepts cles des communications distribuees

| Concept                   | Definition                              | Mecanisme / Protocole                          | Exemple dans le TP                 |
| ------------------------- | --------------------------------------- | ---------------------------------------------- | ---------------------------------- |
| **Communication**         | Echange de messages entre processus     | Message passing, sockets, RPC                  | `CanalReseau` (delais aleatoires)  |
| **Synchronisation**       | Coordination temporelle des evenements  | Horloges physiques (NTP) ou logiques (Lamport) | `tick()`, `majHorloge()`           |
| **Ordonnancement causal** | Respect de « A precede B »              | Horloges logiques / vectorielles               | Si A → B alors L(A) < L(B)         |
| **Exclusion mutuelle**    | Un seul processus en SC a la fois       | Centralise, Token Ring, Ricart–Agrawala        | File de requetes + `REPLY` de tous |
| **Ordre total**           | Tous les processus voient le meme ordre | Tri lexicographique (horloge, id)              | `Comparator` dans `fileRequetes`   |
| **Coherence**             | Copies de donnees compatibles           | 2PL, quorum, replication synchrone             | Hors scope TP3 (cf. chapitre 4)    |
| **Consensus**             | Accord sur une valeur malgre les pannes | Paxos, Raft                                    | Mentionne, non implemente          |
| **Tolerance aux pannes**  | Resister aux defaillances               | Timeouts, leases, replication                  | `enPanne`, `TIMEOUT_QUEUE_MS`      |
| **Detection de panne**    | Identifier un processus defaillant      | Heartbeats, timeouts                           | Implicite via timeouts             |
| **Atomicite**             | Tout ou rien                            | Transactions distribuees                       | Hors scope                         |
| **Deadlock**              | Blocage mutuel                          | Timeouts, ordre global des ressources          | Evite par lease timeout            |
| **Famine**                | Un processus n'accede jamais            | Equite FIFO dans la file                       | File triee par (horloge, id)       |
| **Latence**               | Delai de transmission                   | Simule par `CanalReseau`                       | 50–150 ms                          |
| **Perte de messages**     | Message non delivre                     | Retransmission sur timeout                     | `probabilitePerte` + resend        |
