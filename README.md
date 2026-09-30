# Bar Volatiana — version 2.0

Mise à jour rétrocompatible de l’application Android locale Bar Volatiana.

## Navigation

- Accueil : statistiques réelles, périodes, graphique, meilleures ventes et saisie des ventes.
- Versements : fournisseurs, montants, dates, notes, modification et suppression.
- Trosa : clients, emprunts multi-produits, paiements et solde automatique.
- Entana : produits fixes ou à montant libre, variantes et statut actif/inactif.
- Calculatrice : opérations complètes et historique local.

## Fonctionnement

- Hors ligne, sans compte et sans connexion Internet.
- Données enregistrées automatiquement sur le téléphone.
- Prix en ariary (Ar).
- Migration automatique des anciens produits et anciennes dettes sans effacement.
- Compatible Android 5.0 et versions suivantes.

## Générer l’APK

Utiliser Java 8, Gradle 2.2.1 et Android SDK API 22, puis lancer `gradle clean assembleDebug`.
L’APK est créé dans `app/build/outputs/apk/app-debug.apk`.

Sur l’ordinateur déjà configuré, ouvrir PowerShell dans le projet et lancer simplement : `./compiler.ps1`.

## Mettre à jour l’application sans perdre les données

1. Conserver l’identifiant `mg.barvolatiana.app` et la même clé de signature que la version installée.
2. Compiler cette version avec le même ordinateur et le même profil Windows que la version précédente.
3. Copier `app-debug.apk` sur le téléphone et l’installer par-dessus Bar Volatiana.
4. Ne pas désinstaller l’ancienne application avant la mise à jour : une désinstallation effacerait ses données locales.

Au premier démarrage, la migration ajoute les nouveaux champs aux produits existants et convertit les anciennes dettes vers Trosa. Les ventes, produits, dépenses et dettes d’origine sont conservés dans les mêmes préférences `bar_volatiana_data`.
