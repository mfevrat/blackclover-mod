# Black Clover : Grimoires — mod Forge 1.20.1

Mod de magie inspiré de l'animé : cérémonie de la Tour des Grimoires, 8 attributs magiques
(24 sorts), trèfles à 3 / 4 / 5 feuilles et 3 boss.

## 1. Installation (une seule fois)

1. Installe **Java 17** (JDK) et **IntelliJ IDEA Community**.
2. Télécharge le **MDK Forge 1.20.1** (version 47.x) sur https://files.minecraftforge.net et dézippe-le.
3. Dans le dossier du MDK, **supprime** le dossier `src` puis **copie** à la place :
   `src/`, `build.gradle`, `settings.gradle`, `gradle.properties` de ce projet.
   (Garde les fichiers `gradlew`, `gradlew.bat` et le dossier `gradle/` du MDK.)
4. Ouvre le dossier dans IntelliJ (`File > Open`), attends la fin du chargement Gradle (quelques minutes la 1re fois).
5. Lance le jeu : onglet Gradle > `forgegradle runs` > **runClient**, ou dans un terminal : `gradlew runClient`.

Pour créer le fichier .jar à donner à tes amis : `gradlew build` → `build/libs/blackclover-1.0.0.jar`
(à mettre dans le dossier `mods` d'un Minecraft avec Forge 1.20.1).

## 2. Comment jouer

**Obtenir un grimoire.** Fabrique une *Convocation de la Tour des Grimoires* :
```
  . Or .
Lapis Livre Lapis
  . Perle de l'Ender .
```
Clic droit avec la convocation. Conditions : pas déjà de grimoire, **niveau d'XP 15 ou plus**,
dans l'**Overworld**, et **la nuit**. (En créatif, les conditions de niveau et de nuit sont ignorées.)

**Chances :** trèfle à 3 feuilles (normal), 4 feuilles 7 % (plus fort, pages débloquées plus tôt),
5 feuilles = Anti-magie : 1 %, ou garanti si tu portes un *Fragment de démon* (lâché par Morvath).

**Touches :** `R` lancer le sort · `G` page suivante · clic droit sur le grimoire = voir les pages.
Les touches se changent dans Options > Commandes > Black Clover.

**Progression :** tuer des monstres et lancer des sorts donne de l'XP de grimoire.
Pages débloquées aux niveaux 1, 3 et 6 (niveau max 10).
*Cristal de mana* = +20 mana max · *Page ancienne* = +250 XP.

## 3. Les boss (clic droit sur le sol avec l'objet)

| Objet | Recette | Boss |
|---|---|---|
| Clé du Donjon | œil de l'Ender + 2 or + 2 pierres taillées moussues | Gardien du Donjon (300 PV) |
| Larme elfique | larme de Ghast + 4 cristaux de mana + 4 glowstone | Roi Elfe Spectral (400 PV) |
| Cœur démoniaque | crâne de wither squelette + 4 pages anciennes + 4 obsidiennes pleureuses | Archidémon Morvath (700 PV) |

Chaque boss donne les ingrédients du suivant. Morvath résiste à la magie (−50 %) mais
l'Anti-magie lui inflige double dégâts.

## 4. Commandes de test (mode triche activé)

- `/grimoire ceremony` : refaire la cérémonie
- `/grimoire give fire 4` : grimoire direct (fire, water, wind, lightning, darkness, light, thorn, anti ; 3 à 5 feuilles)
- `/grimoire level 6` · `/grimoire mana` · `/grimoire reset`

## 5. Où modifier quoi

| Je veux... | Fichier |
|---|---|
| Ajouter / changer un sort | `magic/Spells.java` (+ son nom dans `lang/fr_fr.json`) |
| Changer les conditions du grimoire | `item/TowerSummonsItem.java` |
| Changer les chances 4/5 feuilles | `ceremony/CeremonyManager.java` → `decideMagic` |
| Modifier la cérémonie (timing, effets) | `ceremony/CeremonyManager.java` |
| Modifier l'architecture de la tour | `ceremony/GrimoireTower.java` |
| Rendre un boss plus fort | `entity/boss/*.java` → `createAttributes` et `performAttack` |
| Position du grimoire flottant | `client/FloatingGrimoireLayer.java` → `ps.translate(...)` |
| Textures | `assets/blackclover/textures/` (ouvre-les dans un éditeur de pixel art) |

Astuce : la tour est construite une seule fois par monde. Si tu modifies `GrimoireTower.java`,
teste dans un nouveau monde.

## 6. Pour aller plus loin

- Modèles 3D et animations des boss : **Blockbench** + la librairie **GeckoLib**.
- Sons personnalisés : fichiers `.ogg` + `sounds.json`.
- Ce mod est un projet de fan : si tu le publies (CurseForge, Modrinth), évite d'utiliser
  le nom, le logo ou les images officielles de l'animé, et donne-lui ton propre nom.
