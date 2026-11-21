# Security / Sécurité

**FR** — L'application ouvre une connexion ADB sur la boucle locale (`127.0.0.1`) uniquement, via le protocole ADB TCP classique. Le débogage doit être activé par l'utilisateur, et la TV doit autoriser la clé RSA. Aucun contournement de permissions ni activation automatique du débogage n'est effectué.

La saisie de commandes libres permet des modifications du système et comporte un risque de perte de données. Chaque commande est prévisualisée ; celles considérées comme sensibles sont signalées. N'utilisez jamais le port ADB depuis un réseau public.

Les mises à jour vérifient le package, la version et l'identité du certificat de signature ; le déploiement est validé par le programme d'installation Android. Les secrets de signature ne doivent pas figurer dans le dépôt.

**EN** — The ADB connection is restricted to localhost and requires prior user-enabled debugging plus RSA authorization. Custom commands may be destructive. Updates verify signing identity and Android requires installation confirmation. Signing secrets must never be committed to GitHub.

## LAN web control / Portail web local (v1.2.0)

FR : Le portail sur le port **8767 par défaut** est **désactivé par défaut**. Activez-le depuis l'écran TV uniquement, sur un réseau privé de confiance. Un **PIN aléatoire à quatre chiffres** s'affiche sur la TV. Les connexions sont soumises à un verrouillage après six tentatives infructueuses, à une session temporaire HttpOnly/SameSite et à un jeton CSRF pour les requêtes POST. La génération d'un nouveau PIN révoque les sessions. Le serveur refuse les adresses de clients hors réseaux privés et les hôtes arbitraires. Le transport reste en HTTP **non chiffré** : ne faites **aucune redirection NAT du port du portail web** et ne l'utilisez pas sur un Wi-Fi public. L'accès au terminal donne accès aux droits du client ADB, donc la confirmation côté navigateur ne remplace pas la confiance envers la personne qui connaît le PIN.

EN: The port **8767 par défaut** LAN portal is **off by default**. Enable it from the TV on a trusted private LAN. The four-digit PIN, limited failed attempts, short-lived HttpOnly/SameSite session and CSRF token reduce unauthorized access. Regenerating the PIN revokes sessions. The transport is **unencrypted HTTP**: never port-forward it, use it on public Wi-Fi or share its PIN with untrusted users. Terminal commands run with the authenticated local ADB transport's privileges.

Le port du portail web est configurable. En cas de conflit, l'application essaie jusqu'à 20 ports consécutifs ; **ne publiez aucun de ces ports sur Internet**. Un PIN à quatre chiffres reste relativement faible et nécessite un réseau privé de confiance ; six tentatives infructueuses provoquent un verrouillage de dix minutes. / The port is configurable with a 20-port fallback. Never expose any selected port to the public Internet. A four-digit PIN is relatively weak and requires a trusted private LAN and the existing temporary lockout.
