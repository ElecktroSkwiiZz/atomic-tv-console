# ATOMIC TV Console — JB Atomic Industries

![ATOMIC TV Console — official app logo](branding/console-icon.webp)

![Android TV launcher banner](branding/console-banner.webp)

**FR** · [English](#english)

Application Android TV permettant de consulter et d'exécuter des commandes ADB depuis la télécommande, sans saisir systématiquement de ligne de commande.

### Fonctionnalités

- **118 commandes prédéfinies**, classées dans 10 catégories : système, écran, réseau, audio, applications, stockage, télécommande, performances, paramètres et diagnostics.
- **11 actions groupées** : diagnostic complet, réseau, HDMI/eARC, mémoire, stockage, animations et gestion des caches temporaires.
- **Gestion des applications facultatives** : sélection manuelle des paquets système non protégés, désactivation réversible pour l’utilisateur principal et restauration par le menu. **Aucun paquet n’est désactivé automatiquement, aucun effacement de données et aucun bouton « tout supprimer ».** Les paquets essentiels sont exclus par une liste de protection conservatrice, qui ne garantit toutefois pas l’absence de dépendances constructeur.
- **Recherche immédiate** parmi les noms, descriptions et commandes.
- **Mode avancé** pour saisir une commande personnalisée `adb shell`.
- **Interface conçue pour Android TV**, boutons accessibles au D-pad, textes lisibles à distance.
- **Français et anglais**, sélecteur intégré avec préférence mémorisée.
- Prévisualisation et confirmation avant chaque commande ; avertissement pour les opérations sensibles.
- **Mises à jour officielles** : vérification des GitHub Releases, téléchargement et vérification de la signature de l'APK, puis installation après autorisation Android.
- Branding officiel **JB Atomic Industries**.

### Débloat sans risque inutile

Ouvrez **Gérer les applications indésirables** : l’application liste les paquets système éligibles, sans rien cocher. Sélectionnez uniquement les paquets identifiés, relisez la liste et confirmez deux fois. L’action exécute `pm disable-user --user 0 NOM_DU_PAQUET`. Pour annuler, utilisez **Réactiver des applications** (`pm enable --user 0 NOM_DU_PAQUET`).

Ces actions sont **réversibles en principe** mais peuvent perturber des fonctions constructeur. Ne désactivez jamais le lanceur d’applications, les paramètres ou les services de mise à jour. Le code empêche les paquets critiques les plus connus, sans pouvoir garantir toutes les dépendances des fabricants.

### Avant la première utilisation

Une application Android standard ne dispose pas des droits ADB. **ATOMIC TV Console utilise un client ADB TCP authentifié**, connecté uniquement à `127.0.0.1` (la TV elle-même).

1. Activez les *Options développeur* et le débogage ADB réseau/TCP si votre firmware le permet.
2. Assurez-vous qu'ADB TCP est accessible localement (port 5555 par défaut ; modifiable dans l'application).
3. Cliquez sur **Tester la connexion**. Validez l'autorisation de la clé RSA sur la TV.
4. Choisissez une catégorie ou lancez une recherche, vérifiez la commande et confirmez.

**Compatibilité :** Android TV / Google TV Android 11+, selon les limitations du fabricant. Les firmwares n'exposant que le débogage sans fil TLS avec code d'appairage **ne sont pas compatibles avec ce client TCP traditionnel**. Le port 5555 n'est pas activé automatiquement et l'application ne contourne aucune restriction système. Certaines commandes nécessitent des permissions absentes, des API constructeur ou le root : elles pourront échouer.

**Sécurité :** le client ne se connecte qu'à la boucle locale. Les clés RSA privées sont enregistrées dans l'espace privé de l'application. Les commandes personnalisées sont potentiellement destructrices : elles exigent une confirmation explicite. N'exposez jamais ADB TCP sur un réseau non fiable.

### Mises à jour et signatures

L'application recherche les versions dans [GitHub Releases](https://github.com/ElecktroSkwiiZz/atomic-tv-console/releases). Elle compare les versions, vérifie la signature de chaque APK avec celle déjà installée, puis demande l'autorisation d'installation. Elle ne peut pas installer des applications en silence.

Le dépôt compile automatiquement une APK de **développement (debug)** à chaque changement. La publication d'une APK **Release signée** demande d'abord une clé de signature *propre à ce logiciel*, installée comme secrets GitHub :
`TV_CONSOLE_KEYSTORE_BASE64`, `TV_CONSOLE_STORE_PASSWORD`, `TV_CONSOLE_KEY_PASSWORD`, `TV_CONSOLE_KEY_ALIAS`.
Conservez définitivement la même clé. Après vérification du certificat, renseignez son SHA-256 public dans `SIGNATURE_PUBLIQUE.txt` pour bloquer tout changement accidentel de signature.

Un tag `v1.0.0` (conforme à `versionName`) déclenche alors la Release signée. **Ne publiez pas d'APK debug comme version officielle.**

### Développement

JDK 17, Android SDK 35, Gradle 8.10.2 :
```bash
gradle --no-daemon assembleDebug
```

Identifiant Android : `com.jbatomic.tvconsole`. Dépendance ADB : [tananaev/adblib 1.3](https://github.com/tananaev/adblib) (BSD-3-Clause). Projet et éléments graphiques : © 2026 JB Atomic Industries, tous droits réservés sous réserve des licences tierces.

---

## English

**ATOMIC TV Console** is a remote-friendly Android TV application that lets you run categorized ADB shell commands without relying on a computer.

- **118 preconfigured commands**, organized into 10 categories, plus a custom shell command option.
- **11 guided workflows** for performance, storage, HDMI/eARC, networking, temporary cache trimming and reversible animation changes.
- **Optional preinstalled app management**: manually select individual packages, disable for the primary user and restore them later. **No automatic bulk removal, no app-data deletion.** Critical-package filtering is deliberately conservative but cannot detect every OEM dependency.
- Instant **search** by name, command text or description.
- **French / English selector** with saved preference.
- Android TV **D-pad navigation**, confirmation dialogs, readable command output.
- Official GitHub Release update checker with **APK signing certificate verification** and Android installation confirmation.

**Requirements:** Android TV / Google TV 11+, a **previously enabled and authorized legacy TCP ADB endpoint on localhost** (default port 5555), and device firmware that permits it. Android 11+ wireless pairing with TLS is a different transport and is **not supported** by the app's legacy ADB client. No privilege escalation is attempted. Not all commands will work on every TV.

**Updates:** the repository builds a test APK automatically. Official signed releases require a dedicated, securely stored keystore and the GitHub Actions secrets detailed above. After official signing is configured, the app can offer in-place updates from this repository's GitHub Releases.

Copyright © 2026 JB Atomic Industries — https://jbatomic.com/
