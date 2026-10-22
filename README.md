# Black Clover : Grimoires — mod Forge 1.20.1 (v2.0)

Mod de magie inspiré de l'animé : cérémonie de la Tour des Grimoires, 8 attributs magiques
(24 sorts), trèfles à 3 / 4 / 5 feuilles et 3 boss.


## Nouveautés de la version 2.0

**Objets en 3D.** Le grimoire est un vrai livre en volume (couverture, pages, fermoir, coins en métal, trèfle en relief).
Son modèle change selon le trèfle : 3 feuilles doré, 4 feuilles vert, 5 feuilles noir et rouge. Le cristal, la clé,
la larme, le cœur, le fragment, la page et la convocation sont aussi en 3D. Quand tu tiens ton grimoire, ton personnage
le tient ouvert devant lui, et il tend le bras quand tu lances un sort.

**Ordres de chevaliers-mages** (capuche + robe, avec une vraie cape 3D qui flotte au vent) :

| Ordre | Laine | Bonus de l'ensemble complet |
|---|---|---|
| Corbeaux d'Onyx | noire | +25 % de puissance des sorts |
| Cerfs d'Azur | bleue | +50 % de régénération de mana |
| Phénix Écarlates | rouge | immunité au feu, -20 % de recharge |
| Serpents de Jade | verte | Vitesse I, -20 % de coût en mana |
| Hiboux d'Argent | blanche | vision nocturne, +40 mana max |

Capuche : `laine laine laine / laine cristal laine`. Robe : `laine _ laine / laine cristal laine / laine or laine`.
Cristal de mana : `améthyste lapis améthyste / lapis diamant lapis / améthyste lapis améthyste` (donne 2 cristaux).

**Boss en 3D, avec des designs originaux :** colosse de pierre aux cristaux runiques, roi elfe à couronne, halo et
quatre ailes de lumière, archidémon à cornes, ailes de chauve-souris articulées et queue. Chacun a ses animations
(marche, respiration, frappe, incantation, griffe, rugissement de transformation).

**Cinématiques :** la caméra prend le contrôle pendant la cérémonie (plongée dans la tour, poursuite du grimoire,
gros plan au moment où il s'ouvre), à l'apparition, à la transformation et à la mort des boss, et quand tu lances
un sort ultime. Touche **V** pour passer.

**Cercles magiques et rayons en 3D :** un cercle tourne sous tes pieds à chaque sort, et les attaques des boss
ont leurs propres cercles et colonnes de lumière.

**Page ultime (niveau 9, ou 7 avec un trèfle à 4/5 feuilles) :**

| Attribut | Sort ultime |
|---|---|
| Feu | Couronne de météores : un cercle dans le ciel fait pleuvoir des météores |
| Eau | Prison abyssale : une sphère d'eau emprisonne et noie les ennemis, puis explose |
| Vent | Déchirure céleste : 12 lames de vent jaillissent d'un cercle dans ton dos |
| Foudre | Lance du ciel : un éclair géant tombe d'un cercle céleste |
| Ténèbres | Éclipse : le ciel s'assombrit, les ennemis sont aspirés puis écrasés |
| Lumière | Cathédrale de lumière : 8 colonnes forment une zone qui soigne et repousse |
| Épines | Arbre-monde : des racines immobilisent jusqu'à 10 ennemis, tu es soigné |
| Anti-magie | Lame du néant : une tranche géante qui efface toute magie autour de toi |

**16 succès** dans un onglet « Black Clover : Grimoires » (grimoire, trèfles rares, pages, ordres, boss...).

## 1. Installation (une seule fois)

1. Installe **Java 17** (JDK) et **IntelliJ IDEA Community**.
2. Télécharge le **MDK Forge 1.20.1** (version 47.x) sur https://files.minecraftforge.net et dézippe-le.
3. Dans le dossier du MDK, **supprime** le dossier `src` puis **copie** à la place :
   `src/`, `build.gradle`, `settings.gradle`, `gradle.properties` de ce projet.
   (Garde les fichiers `gradlew`, `gradlew.bat` et le dossier `gradle/` du MDK.)
4. Ouvre le dossier dans IntelliJ (`File > Open`), attends la fin du chargement Gradle (quelques minutes la 1re fois).
5. Lance le jeu : onglet Gradle > `forgegradle runs` > **runClient**, ou dans un terminal : `gradlew runClient`.

Pour créer le fichier .jar à donner à tes amis : `gradlew build` → `build/libs/blackclover-2.0.0.jar`
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

**Touches :** `R` lancer le sort · `G` page suivante · `V` passer une cinématique · clic droit sur le grimoire = voir les pages.
Les touches se changent dans Options > Commandes > Black Clover.

**Progression :** tuer des monstres et lancer des sorts donne de l'XP de grimoire.
Pages débloquées aux niveaux 1, 3, 6 et 9 (niveau max 10).
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
| Trajectoires des cinématiques | `client/CinematicManager.java` → `place()` |
| Forme des boss | `gen/bossmodels.py` (puis relance-le) ou directement `client/model/*.java` |
| Bonus des ordres | `magic/KnightOrder.java`, `event/CommonEvents.java` |
| Textures | `assets/blackclover/textures/` (ouvre-les dans un éditeur de pixel art) |

Astuce : la tour est construite une seule fois par monde. Si tu modifies `GrimoireTower.java`,
teste dans un nouveau monde.

## 6. Pour aller plus loin

- Modèles 3D et animations des boss : **Blockbench** + la librairie **GeckoLib**.
- Sons personnalisés : fichiers `.ogg` + `sounds.json`.
- Ce mod est un projet de fan : si tu le publies (CurseForge, Modrinth), évite d'utiliser
  le nom, le logo ou les images officielles de l'animé, et donne-lui ton propre nom.
