# Security / Sécurité

**FR** — L'application ouvre une connexion ADB sur la boucle locale (`127.0.0.1`) uniquement, via le protocole ADB TCP classique. Le débogage doit être activé par l'utilisateur, et la TV doit autoriser la clé RSA. Aucun contournement de permissions ni activation automatique du débogage n'est effectué.

La saisie de commandes libres permet des modifications du système et comporte un risque de perte de données. Chaque commande est prévisualisée ; celles considérées comme sensibles sont signalées. N'utilisez jamais le port ADB depuis un réseau public.

Les mises à jour vérifient le package, la version et l'identité du certificat de signature ; le déploiement est validé par le programme d'installation Android. Les secrets de signature ne doivent pas figurer dans le dépôt.

**EN** — The ADB connection is restricted to localhost and requires prior user-enabled debugging plus RSA authorization. Custom commands may be destructive. Updates verify signing identity and Android requires installation confirmation. Signing secrets must never be committed to GitHub.
