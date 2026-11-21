<div align="center">

![ATOMIC TV Console — JB Atomic Industries](branding/console-banner.webp)

# ATOMIC TV Console
### Commandes ADB, diagnostics Android TV et contrôle depuis PC ou smartphone

**Application Android TV / Google TV · 118 commandes · 11 actions groupées · Portail web local**

[Français](README.md) · [English](README.en.md) · [Español](README.es.md) · [Русский](README.ru.md)

[**Télécharger l’APK officielle**](https://github.com/ElecktroSkwiiZz/atomic-tv-console/releases/latest) · [Versions et mises à jour](https://github.com/ElecktroSkwiiZz/atomic-tv-console/releases) · [Site JB Atomic Industries](https://jbatomic.com/)

</div>

## Présentation

**ATOMIC TV Console** est une application de gestion et de diagnostic pour **Android TV et Google TV**, développée par **JB Atomic Industries**. Elle réunit un catalogue de commandes ADB, des opérations groupées et un terminal accessible depuis la télévision ou un navigateur sur le réseau local. Elle vise à rendre les diagnostics et les réglages Android TV plus accessibles, sans masquer les limites des autorisations système.

## Fonctionnalités

| Fonction | Description |
| --- | --- |
| **118 commandes ADB** | Recherche et catégories : système, réseau, HDMI/audio, écran, stockage, applications, performances, etc. |
| **11 actions groupées** | Diagnostics système, réseau, HDMI/eARC, mémoire et stockage ; animations et caches temporaires |
| **Portail web local** | Interface pour PC, smartphone ou tablette avec catalogue, terminal et export TXT |
| **Terminal ADB** | Saisie de commandes `adb shell` avec confirmation avant exécution |
| **Résultats exportables** | Affichage, copie et téléchargement TXT ; historique temporaire |
| **Gestion des applications** | Désactivation manuelle réversible de paquets sélectionnés, sans désinstallation automatique |
| **Interface TV** | Navigation à la télécommande avec focus visible et affichage français / anglais |
| **Mises à jour** | APK officielles GitHub Releases avec vérification du certificat Android |

## Télécharger et installer

**Version publiée : v1.2.1** — [Télécharger l’APK Android TV signée](https://github.com/ElecktroSkwiiZz/atomic-tv-console/releases/latest).

1. Installez l’APK officielle sur votre télévision Android TV / Google TV.
2. Activez et autorisez au préalable le **débogage ADB TCP** si votre télévision le prend en charge.
3. Dans l’application, testez la connexion ADB (par défaut `127.0.0.1:5555`).
4. Ouvrez les **Actions groupées**, une catégorie ou le terminal.

**Compatibilité :** Android 11+ (API 30+). L’application nécessite un endpoint **ADB TCP classique** préalablement activé et autorisé. L’appairage ADB sans fil **TLS** d’Android n’est pas pris en charge par le client actuel. Certaines commandes sont indisponibles selon les constructeurs ou nécessitent des privilèges supplémentaires.

## Contrôler Android TV depuis un ordinateur ou un téléphone

1. Sur la TV, activez **Contrôle depuis un PC ou un smartphone**.
2. Relevez l’**adresse locale** et le **PIN à 4 chiffres** affichés sur la TV.
3. Ouvrez l’adresse affichée sur un appareil connecté au même réseau privé.
4. Consultez les commandes, exécutez les actions groupées, utilisez le terminal et téléchargez les résultats au format `.txt`.

Le port du portail est configurable (**8767 par défaut**). Si le port choisi est occupé, l’application essaie jusqu’à **19 autres ports** et affiche celui effectivement utilisé. Le portail est désactivé par défaut.

**Sécurité :** le portail utilise **HTTP local non chiffré**. Ne l’utilisez que sur un réseau privé de confiance et **n’exposez jamais le port sur Internet**. Le PIN court est accompagné d’un verrouillage après plusieurs tentatives erronées, mais ne remplace pas un réseau sûr. Les commandes avancées peuvent modifier le système. Les applications préinstallées ne sont jamais supprimées automatiquement.

## Questions fréquentes

**Est-ce une application de root Android TV ?** Non. Elle ne donne pas de nouveaux privilèges et ne contourne pas les restrictions Android.

**Peut-on améliorer les FPS d’un jeu ou d’une console HDMI ?** Les réglages d’animation peuvent réduire la durée des transitions Android ; ils n’augmentent pas les FPS d’une console HDMI.

**Peut-on gérer la TV depuis un smartphone ?** Oui, au moyen du portail web local après activation et connexion avec le PIN.

**Peut-on récupérer les résultats sur Windows ?** Oui, par copie ou export TXT depuis le navigateur.

**L’application supprime-t-elle toutes les applications système ?** Non. Elle propose une sélection manuelle et une désactivation réversible ; la compatibilité des services constructeur doit être vérifiée.

## Développement et ressources

- **Application ID :** `com.jbatomic.tvconsole`
- **Technologies :** Java, Android SDK 35, JDK 17, Gradle 8.10.2, ADB
- **Compilation de test :** `gradle --no-daemon assembleDebug`
- [Historique des versions](CHANGELOG.md) · [Sécurité](SECURITY.md) · [Empreinte publique APK](SIGNATURE_PUBLIQUE.txt) · [Licences tierces](THIRD_PARTY_NOTICES.md)
- [Autre application JB Atomic Industries : ATOMIC TV Capture](https://github.com/ElecktroSkwiiZz/atomic-tv-capture)

**Éditeur : [JB Atomic Industries](https://jbatomic.com/)** · © 2026 JB Atomic Industries. Les logos et le code demeurent soumis aux mentions et licences indiquées dans le dépôt. La disponibilité du code source ne signifie pas qu’une licence de réutilisation a été accordée.
