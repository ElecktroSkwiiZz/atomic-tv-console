# Changelog / Historique

## 1.2.0 — Portail PC / smartphone et interface TV / Remote control dashboard

- Actions groupées remontées en haut de l’accueil Android TV, raccourcis de diagnostic directement accessibles.
- Portail HTTP local **désactivé par défaut** : activation et code PIN à huit chiffres affichés sur la TV.
- Interface PC, smartphone et tablette FR/EN : catalogue, recherche, terminal ADB, 11 workflows, suivi d'exécution.
- Historique partagé avec l’interface TV, copie et export TXT des résultats.
- Sécurité : PIN, limitation des essais, session avec cookie HttpOnly/SameSite, jeton CSRF, origine validée, session révocable et port 8767 non publié sur Internet.
- Exécution des commandes par ADB TCP local authentifié, sorties tronquées et séquences arrêtées à la première erreur.

## 1.1.0 — Maintenance Android TV / TV maintenance

- 118 commandes bilingues (42 nouvelles commandes de diagnostic).
- 11 workflows : audits système, mémoire, réseau, HDMI, performances, animations 0,5x/0x/1x et purge de caches temporaires.
- Sélection manuelle et double confirmation pour désactiver les applications préinstallées facultatives ; aucune suppression automatique.
- Restaurer les paquets désactivés ; filtrage conservateur des composants système essentiels.
- Suivi des codes de sortie ADB et arrêt des séquences au premier échec.



## 1.0.0 — Initial public source

- Native D-pad Android TV interface with a searchable bilingual command catalog (76 entries).
- Authenticated legacy TCP ADB client restricted to `127.0.0.1`.
- Command preview, warnings, output viewer and manual command entry.
- English/French language selection and persisted ADB TCP port.
- GitHub Releases updater that validates matching APK certificate.
- GitHub Actions build and optional signed official release workflow.

**Note:** release installation requires a separate, persistent Android signing key and its GitHub secrets.
