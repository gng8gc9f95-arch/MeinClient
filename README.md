# Chest ESP (Fabric, Minecraft 1.21.11)

Funktionen
- Umrandung + Tracer zu Kisten, Trapped Chests, Enderkisten, Faessern, Shulkerboxen
  und Spawnern - durch Waende sichtbar
- Spawner-Alarm: Chat-Nachricht, Actionbar und Ton, sobald ein Spawner
  innerhalb von 48 Bloecken ist (Radius: SpawnerAlert.RADIUS)
- Sus Chunks: Chunks mit auffaellig vielen Spieler-Bloecken (Kisten, Enderkisten,
  Shulker, Hopper, Oefen, Leuchtfeuer ...) werden pink umrandet + Chat-Meldung mit
  Koordinaten. Schwelle: SusChunks.THRESHOLD (Standard 6)
- Menue: Klickbares Menue fuer alle Funktionen
- Freecam: Kamera loest sich von der Figur und fliegt frei umher

Tasten (unter Optionen -> Steuerung -> "Chest ESP" aenderbar)
- Rechte Shift  Menue oeffnen (alles per Klick an/aus)
- G  Sus Chunks an/aus
- K  ESP an/aus
- J  Tracer an/aus
- V  Freecam an/aus (WASD, Leertaste = hoch, Shift = runter, Sprint = schneller)
- U  Spawner-Alarm an/aus

Farben: Orange Kiste | Rot Trapped Chest | Lila Enderkiste | Braun Fass |
        Tuerkis Shulker | Gruen Spawner

## Bauen
Voraussetzung: JDK 21 und Gradle 8.14 oder neuer (oder IntelliJ IDEA).

    gradle wrapper --gradle-version 8.14   # einmalig
    ./gradlew build                        # Windows: gradlew.bat build

Fertige Mod: build/libs/chestesp-1.1.0.jar  (NICHT die -sources.jar)
In den mods-Ordner legen. Benoetigt Fabric Loader 0.18+ und Fabric API fuer 1.21.11.

## Hinweise
- Mappings: Offizielle Mojang-Mappings (Yarn wird nach 1.21.11 nicht mehr gepflegt).
- Freecam: Die echte Figur bleibt stehen (faellt aber, wenn sie in der Luft ist).
  Mausbewegung dreht auch die Figur; beim Beenden wird die alte Blickrichtung
  wiederhergestellt. Angriff/Benutzen sind waehrend Freecam gesperrt.
- Auf vielen Multiplayer-Servern ist so etwas verboten.
