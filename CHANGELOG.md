# Changelog / Historique

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
