# GartenManager: Konzept Gegenstände, Maßstab und Ebenen (Umsetzungsschritt 3)

Stand: 02.10.2026, Version 0.4 (dritte Runde vom 02.10.2026 eingearbeitet, keine offenen Fragen; Teilschritt 3a gebaut und als Vorschlag geliefert)

Dieses Dokument beschreibt, **wie Gegenstände (Häuser, Garagen, Autos, Gartenmöbel, Zäune, Bewässerung, Deko), ein Maßstab und eine Ebenenliste in den Garten kommen**, bevor Code entsteht. Pflanzen kommen in Schritt 4, nutzen aber dieselben Bedienweisen.

Kennzeichnung wie in PROJEKT.md: **Entschieden**, **Vorschlag**, **Offen**, **Nicht geprüft**.

Die Bilder in `symbole-gegenstaende.png` sind **Skizzen von mir** (Detailgrad von dir bestätigt, Hochbeet korrigiert). Sie sind komplett selbst gezeichnet (keine Lizenzfragen). In der App werden sie vom Handy gezeichnet und sehen im Einzelnen etwas anders aus.

---

## 1. Was in diesem Schritt entsteht, und was nicht

**Entsteht:**
- **Maßstab:** Lineale am Rand, Größen in Metern, Gitter in Metern.
- Gegenstände aus einem Katalog platzieren, verschieben, in der Größe ändern, drehen, einfärben, benennen, duplizieren, löschen.
- Zäune, Hecken, Mauern und Bewässerung als schmale Gegenstände, in Länge und Breite veränderbar.
- **Gebäude in freier Form** als Gegenstand: Punkte setzen und ziehen wie bei Flächen.
- **Frei mischbare Ebenen** für Flächen und Gegenstände, mit **Ebenenliste** am Fensterrand (schon in 3a).
- **Wählbare Farbe auch für Flächen**, ohne dass das Muster verschwindet.
- Schalter **"Verdecktes zeigen"**.
- Rückgängig für alles davon.

**Entsteht noch nicht:** Pflanzen (Schritt 4), eigene Gegenstände aus Fotos, freier Farbwähler (12 feste Farbfelder reichen, entschieden).

---

## 2. Entschieden (Rückmeldung vom 02.10.2026)

1. **Platzieren:** erst den Ort im Garten antippen, dann den Katalog. **Entschieden**
2. **Katalog:** Liste wie vorgeschlagen, dazu **Motorrad, Fahrrad, Spielhaus, Wärmepumpe, Ladesäule, Trittstein, Rasenmähroboter**. Schaukel ist dabei. Draufsicht und schlichter Stil bleiben, **etwas mehr Details** (siehe Bild). **Entschieden**
3. **Drehen:** Drehgriff, Einrasten alle 15 Grad. **Entschieden**
4. **Größe ändern:** an Ecken und Kanten ziehen. Ob es zu viele Griffe werden, probieren wir am Gerät. **Entschieden**
5. **Ebenen frei mischbar**, zum Beispiel liegt eine Bewässerung **unter** einer Fläche und ist nicht dauerhaft sichtbar. **Entschieden**
6. **Unregelmäßige Gebäude** werden wie Flächen bearbeitet (Punkte setzen, ziehen). **Entschieden**
7. **Zäune, Hecken, Mauern, Bewässerung** sind normale Einträge im Katalog, als schmale Grafik, in Länge und Breite veränderbar. Ecken entstehen, indem man ein weiteres Stück platziert und dreht. **Entschieden**
8. **Farbauswahl** für Gegenstände, gleich von Anfang an. **Entschieden**
9. **Namen** nur nach Antippen (Karte), nicht dauerhaft im Plan. **Entschieden**
10. **Maßstab wird eingebaut** (Änderung gegenüber der früheren Entscheidung "nur Skizze", begründet mit den Pflanzengrößen). Dauerhaft im Bearbeitungsmodus an einer waagerechten und einer senkrechten Seite angezeigt. Flächen und Gegenstände (später Pflanzen) bekommen echte Maße. **Entschieden**
11. **Dezente Ebenenliste** am Fensterrand, auf der Seite, auf der nicht das senkrechte Lineal ist. **Entschieden**
12. **Teilschritte:** Größe ändern und Drehen schon in 3b. **Entschieden**

---

**Zweite Runde (02.10.2026):**

- **A.** Lineal mit festem Nullpunkt. **Entschieden**
- **B.** Gitter an Meter gekoppelt. **Entschieden**
- **C.** Jeder Gegenstand hat eine **feste Ausgangsgröße in echten Maßen**. Sie lässt sich an den Griffen ändern, **beim Ziehen steht das Maß live daneben** ("4,5 × 1,8 m"), so sieht man sofort, wie groß der Gegenstand wirklich ist. **Entschieden**
- **D.** 12 feste Farbfelder. **Auch Flächen bekommen eine wählbare Farbe, das Muster darf dabei nicht verschwinden.** **Entschieden**
- **E.** Auge zum Ausblenden. Neuordnen per Ziehen zunächst später (siehe 8.2). Rückfrage zum Auge in Abschnitt 9. **Teilweise entschieden**
- **F.** Schalter **"Verdecktes zeigen"**. **Entschieden**
- **G.** **Gebäude bleibt ein Gegenstand, keine Fläche** (meine Lesart: ein Gegenstand im Katalog mit freier Form aus Punkten; siehe Abschnitt 7 und Rückfrage 1). **Entschieden**
- **H.** Karte zeigt ungefähre Fläche in m². **Entschieden**
- **I.** Detailgrad gut, **Hochbeet korrigiert** (Pflanzen liegen jetzt im Beet). **Entschieden**
- **J.** **Ebenenliste kommt schon in 3a.** Rest der Reihenfolge bleibt. **Entschieden**

**Dritte Runde (02.10.2026):**

- **Freies Gebäude:** Unterschied zwischen "Haus (Rechteck)" und "Gebäude (freie Form)" bleibt. Das freie Gebäude muss **drehbar** sein (Drehgriff wie bei den anderen Gegenständen). **Entschieden**
- **"Maßstab festlegen" entfällt.** Größen werden an den Griffen angepasst, nicht durch Zahleneingabe. Der Maßstab bleibt als Wert pro Garten gespeichert (Standard 10 Einheiten pro Meter), ist aber nicht einstellbar. **Entschieden**
- **Ebenenleiste (neu beschrieben):** dezente Leiste am rechten Rand; jede Ebene ist ein Punkt, der Punkt der aktuellen Ebene ist leicht hervorgehoben; oben ein Pfeil nach oben, unten ein Pfeil nach unten, damit geht man durch die Ebenen; unter dem unteren Pfeil ein dezentes Auge, ein Tipp darauf öffnet die Liste aller Einträge, nach Ebene sortiert. Das Auge blendet **nicht** einzelne Elemente aus (die Spalte "Sichtbar" entfällt). **Entschieden**

---

## 3. Maßstab und Lineale (Vorschlag für die Ausführung)

**Grundidee:** Jeder Garten bekommt einen **Maßstab** ("so viele Skizzeneinheiten sind ein Meter"). Alle Lagen und Größen bleiben intern Skizzeneinheiten. Der Maßstab macht daraus Meter. Ändert man den Maßstab, ändern sich alle echten Maße gleichzeitig (der Plan bleibt, wie er gezeichnet ist). Das ist wie "Ich habe diese Strecke gezeichnet, sie ist in Wirklichkeit 12 Meter lang".

- **Anfangswert:** Neue Gärten bekommen **10 Einheiten pro Meter** (**Annahme**), damit sofort alles Metermaße hat. Ein Handybildschirm bei Zoomstufe 1 zeigt dann etwa 35 Meter Breite (Schätzung, nicht geprüft).
- **Die Größen kommen aus den Gegenständen:** Jeder Gegenstand hat eine echte Ausgangsgröße, die man an den Griffen anpasst. Eine Zahleneingabe oder "Maßstab festlegen" gibt es nicht (entschieden). Der Maßstab pro Garten ist fest 10 Einheiten pro Meter.
- **Lineale:** Im Bearbeitungsmodus ein **waagerechtes Lineal oben** (unter der Kopfleiste) und ein **senkrechtes Lineal links**, schmal und halbtransparent. Beschriftung in Metern, Teilung wechselt mit dem Zoom (1 m, 2 m, 5 m, 10 m, …). Die Lineale bewegen sich beim Verschieben und Zoomen mit. **Vorschlag.** Der Nullpunkt ist fest (entschieden).
- **Gitter:** Die Gitterlinien richten sich nach den Metern (zum Beispiel alle 1 m oder 5 m, je nach Zoom). **Vorschlag**
- **Größenanzeige:** **Beim Ziehen an den Griffen steht das Maß live am Gegenstand** ("4,5 × 1,8 m"), ebenso in der Karte. Bei Flächen zusätzlich die **ungefähre Fläche in m²** (Näherung, weil die Kurve nicht ganz exakt gerechnet wird; sie ist nützlich zum Beispiel für den Rasen). **Entschieden**
- **Standardgrößen** neuer Gegenstände stehen als echte Maße im Katalog (Haus etwa 10 × 8 m, Garage 3 × 6 m, Auto 1,8 × 4,5 m, Tisch rund 1,2 m, Zaun 0,1 × 5 m und so weiter). Das ersetzt die Annahme "30 Meter Bildschirmbreite" aus Version 0.1.

---

## 4. Gegenstände: Katalog (Vorschlag, mit deinen Ergänzungen)

| Gruppe | Gegenstände |
|---|---|
| Gebäude | Haus (Rechteck), **Gebäude (freie Form)**, Garage, Carport, Schuppen, Gewächshaus, Wärmepumpe, Ladesäule |
| Verkehr | Auto, **Motorrad, Fahrrad** |
| Sitzen und Feiern | Tisch rund, Tisch eckig, Tisch mit Stühlen, Stuhl, Bank, Liege, Sonnenschirm, Grill, Feuerstelle |
| Spiel und Wasser | Trampolin, Sandkasten, Schaukel, **Spielhaus**, Pool, Brunnen |
| Garten | Hochbeet, Komposter, Regentonne, Mülltonnen, Wäschespinne, **Trittstein, Rasenmähroboter** |
| Zaun, Hecke, Technik | **Zaun, Hecke, Mauer, Bewässerung** |
| Deko | Findling, Statue, Vogelbad, Laterne |

Das sind etwa 39 Einträge. Eine Zeichnung pro Eintrag ist Arbeit. Ich liefere in 3b zuerst alle Symbole in einfacher Form und verfeinere sie nach deinen Tests.

**Schmale Gegenstände (Zaun, Hecke, Mauer, Bewässerung):** Das Muster **wiederholt sich** entlang der Länge (Pfosten und Latten, Büsche, Steine, gestrichelte Leitung mit Regnern), es wird nicht gestreckt. Länge und Breite ändert man mit den Kantengriffen. Ein Zaun mit Ecke sind zwei Zäune, einer gedreht (**Entschieden**, Punkt 7). Eine Besonderheit: Bei sehr schmalen Gegenständen liegen die Griffe dicht beieinander. **Vorschlag:** Es gilt immer der Griff, der dem Finger am nächsten ist. Ob das zuverlässig klappt, sehen wir live.

---

## 5. Bedienung (Vorschlag)

### 5.1 Platzieren
1. Im Bearbeitungsmodus Knopf **"Gegenstand"** (neben "Zeichnen" und "Form").
2. Tipp auf die Stelle im Garten.
3. Katalog öffnet sich (Raster mit Symbolen, Namen, Gruppen oben, Suchfeld wäre denkbar).
4. Tipp auf einen Eintrag: Gegenstand erscheint an der Stelle, in Standardgröße, ist ausgewählt. Die Werkzeugleiste darf nicht höher werden (vier Knöpfe: Zeichnen, Form, Gegenstand, Rückgängig).

### 5.2 Auswählen, verschieben, vergrößern, drehen
- **Tipp** auf einen Gegenstand wählt ihn aus (es zählt der oberste Gegenstand oder die oberste Fläche an der Stelle).
- **Ziehen** am Gegenstand verschiebt ihn, mit kurzer Vibration, hellerer und greller Farbe und dickem Rahmen (wie bei Flächen).
- **Eckgriffe** (Quadrate): Größe ändern, Seitenverhältnis bleibt. **Das Maß in Metern erscheint live neben dem Gegenstand.** **Kantengriffe** (kleinere Quadrate in der Mitte jeder Seite): nur eine Richtung.
- **Drehgriff** (Kreis an kurzem Stiel): drehen, Einrasten alle 15 Grad, bei 0, 90, 180, 270 Grad kurze Vibration. Mit "Einrasten" aus: stufenlos.
- Berührflächen mindestens etwa 48 Punkte, auch wenn das gezeichnete Quadrat kleiner ist.
- **Zwei Finger** zoomen und verschieben weiter, auch bei ausgewähltem Gegenstand.
- Der **Rahmen und die Griffe des ausgewählten Gegenstands** werden immer ganz oben gezeichnet, auch wenn er unter einer Fläche liegt. So findet man ihn.

### 5.3 Menü per langem Tipp
Name ändern, **Farbe ändern**, Duplizieren (leicht versetzt), 90 Grad drehen, Nach vorn, Nach hinten, **Ganz nach hinten** (zum Beispiel für Bewässerung unter allen Flächen), Löschen (mit Rückfrage).

### 5.4 Ansicht (nur Betrachten)
Tipp auf einen Gegenstand oder eine Fläche zeigt die Karte mit Name, Art und Maßen ("Ohne Namen", wenn leer). Was unter einer Fläche liegt, ist in der Ansicht nicht antippbar (es liegt ja darunter), man erreicht es über die Ebenenliste im Bearbeitungsmodus.

### 5.5 Rückgängig
Gleicher Mechanismus: Vor jeder Änderung wird der Garten gesichert (jetzt mit Gegenständen), bis zu 50 Schritte, geleert beim Verlassen des Bearbeitungsmodus. Jede abgeschlossene Aktion ist ein Schritt: Platzieren, Verschieben, Größe ändern, Drehen, Farbe, Umbenennen, Löschen, Ebene ändern.

---

## 6. Farben für Gegenstände (Vorschlag)

- Jedes Symbol hat eine **Hauptfarbe** (Dach beim Haus, Karosserie beim Auto, Tischplatte, Zaunlatten, Laub der Hecke). Die Hauptfarbe lässt sich wählen. **Dunklere Umrandung und hellere Details leitet die App automatisch ab**, so bleibt das Symbol lesbar.
- Auswahl im Menü "Farbe ändern": Dialog mit **12 festen Farbfeldern** und "Standard".
- Gespeichert wird die Farbe als Zahl (leer = Standard).
- Beim Platzieren hat jeder Gegenstand seine Standardfarbe (Haus rot, Auto blau, wie im Bild).
- Ein freier Farbwähler (Farbkreis) wäre später möglich (Frage D).
- **Auch Flächen bekommen eine wählbare Farbe** (Menü "Farbe ändern", dieselben 12 Felder plus "Standard"). Die **Füllung nimmt die gewählte Farbe an, das Muster bleibt**: Rand und Muster werden aus der gewählten Farbe abgeleitet (etwas dunkler, bei Wasser etwas heller), so bleibt Gras erkennbar als Gras, nur eben zum Beispiel gelbgrün oder bläulich. Gespeichert wird die Farbe als Zahl in einer neuen Spalte der Fläche (leer = Standardfarbe der Oberfläche).

---

## 7. Gebäude in freier Form (Vorschlag, nach deiner Korrektur)

Gebäude bleibt ein **Gegenstand** im Katalog (Gruppe Gebäude). Neben dem rechteckigen "Haus" gibt es **"Gebäude (freie Form)"**:

- Platzieren wie jeder Gegenstand (Tipp auf den Ort, Katalog, Eintrag wählen). Es erscheint zunächst als Rechteck mit **vier Punkten**.
- Ausgewählt zeigt es **Punktgriffe wie eine Fläche** (Kreis = rund, Quadrat = Ecke). Punkte ziehen, auf dem Rand neue Punkte hinzufügen, Punkte löschen (mindestens drei), jeder Punkt rund oder Ecke. So entsteht ein L-Haus oder ein Haus mit Erker.
- **Verschieben** durch Ziehen am Gebäude. Es hat **keine** Größengriffe (die Punkte übernehmen das), aber einen **Drehgriff** wie die anderen Gegenstände (entschieden 02.10.2026). Dazu sind die Punkte relativ zur Mitte gespeichert, die Drehung wird darübergelegt.
- Darstellung: Dachziegel-Muster in der gewählten **Dachfarbe** (12 Farbfelder, wie bei allen Gegenständen), gleiche Menüs wie bei Gegenständen (Name, Farbe, Duplizieren, Nach vorn/hinten, Löschen).
- Technisch ist es ein Gegenstand mit eigener Punktliste (neue Tabelle "Gegenstandspunkt", gleicher Aufbau wie die Punkte der Flächen). Das Zeichnen der Kurve und die Punktbearbeitung kann ich vom vorhandenen Flächencode übernehmen.
- Für die Karte und die Fläche in m² gilt dasselbe wie bei Flächen.

Damit gibt es keine neue "Gebäude"-Oberfläche mehr (das war ein Missverständnis von mir in Version 0.2).

---

## 8. Ebenen und Ebenenliste

### 8.1 Gemeinsame Reihenfolge
Flächen und Gegenstände (später Pflanzen) haben **eine gemeinsame Reihenfolge**. Was oben in der Reihenfolge steht, wird zuletzt gezeichnet. Nach vorn und nach hinten tauscht mit dem Nachbarn in dieser gemeinsamen Liste, egal ob Fläche oder Gegenstand. Neues kommt zunächst ganz nach oben. Eine Bewässerung legt man mit "Ganz nach hinten" unter alles.

Technisch bekommen Flächen und Gegenstände ihre Reihenfolgenummern aus **einem gemeinsamen Zähler pro Garten**, damit es keine Doppelten gibt.

### 8.2 Ebenenleiste (Entschieden, Teilschritt 3a gebaut)

- Nur im Bearbeitungsmodus, **am rechten Fensterrand** (die Lineale sind oben und links). Während man eine Fläche oder Form zeichnet, ist sie ausgeblendet.
- **Dezent:** schmale, halbtransparente Leiste. **Jede Ebene ist ein Punkt** (in der Farbe der Oberfläche), ganz oben die vorderste. Der Punkt der ausgewählten Ebene ist größer und hat einen Ring.
- **Pfeil nach oben** (am oberen Rand) wählt die nächste Ebene weiter vorn, **Pfeil nach unten** (am unteren Rand) die nächste weiter hinten. Ist nichts ausgewählt, wählt der Pfeil nach oben die vorderste, der nach unten die hinterste. Ein Tipp auf einen Punkt springt direkt zu dieser Ebene.
- **Auge** (unter dem Pfeil nach unten): öffnet die **Liste aller Einträge, vorderste zuerst**, mit Farbfeld, Name, Oberfläche und Ebenennummer. Ein Tipp wählt den Eintrag aus (auch wenn er unter einer Fläche liegt).
- Bei sehr vielen Ebenen zeigt die Leiste nur so viele Punkte, wie hineinpassen, der Ausschnitt wandert mit der Auswahl mit.
- Die Ebenen werden **nicht** durch Ziehen oder in der Leiste umsortiert, sondern wie bisher im Flächenmenü (Nach vorn, Nach hinten). Umsortieren per Ziehen in der Liste wäre später möglich.
- **Verdecktes zeigen:** Ein Schalter zeichnet alles, was unter einer Fläche liegt, zusätzlich halbtransparent obenauf (zum Beispiel die Bewässerung unter dem Rasen). Nur eine Ansichtsfunktion, ändert keine Reihenfolge. **Entschieden**, Umsetzung in 3c, wenn die Bewässerung kommt.

---

## 9. Rückfragen

Keine offenen Fragen mehr. Alles Entschiedene steht in Abschnitt 2.

---

## 10. Umsetzung in Teilschritten (Vorschlag, mit deiner Änderung)

Wie bei Schritt 2: Jeder Teilschritt ist eine eigene Lieferung. Du pushst, GitHub baut, du testest.

| Schritt | Ergebnis auf dem Handy |
|---|---|
| 3a | **Maßstab und Ebenenliste:** Datenbankversion 3 (Garten bekommt den Maßstab, Fläche bekommt eine Farbspalte, gemeinsamer Reihenfolgenzähler, Tabellen Gegenstand und Gegenstandspunkt schon angelegt). Lineale oben und links im Bearbeitungsmodus (fester Nullpunkt), Gitter in Metern, Karte zeigt Maße und ungefähre Fläche in m². **Ebenenleiste am rechten Rand** (Punkte, Pfeile, Auge mit Liste), zunächst nur mit Flächen. Noch keine Gegenstände. |
| 3b | **Gegenstände, Kern:** Knopf "Gegenstand", Katalog (alle Einträge außer den schmalen), Platzieren, Anzeige, Auswählen, Verschieben, **Größe ändern mit Maßanzeige live, Drehen**, Gebäude in freier Form, Menü (Name, Duplizieren, 90 Grad, vor/zurück, ganz nach hinten, Löschen), Karte, Rückgängig, Gegenstände erscheinen in der Ebenenliste, Duplizieren des Gartens kopiert sie mit. |
| 3c | **Farbe, schmale Gegenstände, Verdecktes zeigen:** Farbauswahl (12 Felder) für Gegenstände **und Flächen** (Muster bleibt), Zaun, Hecke, Mauer und Bewässerung mit sich wiederholendem Muster, Schalter "Verdecktes zeigen". |
| 3d | **Feinschliff** nach deinen Tests: Symbole verbessern, kleine Wünsche. |

Wie lange das dauert, kann ich nicht seriös schätzen.

**Hinweis zu 3a:** Weil die Datenbank sich ändert, gehen deine Testgärten einmalig verloren (Entwicklungsmodus, wie bei 2c). Alle neuen Tabellen und Spalten kommen schon in 3a, damit die Datenbank nur **einmal** neu entsteht.

---

## 11. Was gespeichert wird (Datei 2 "Meine Daten")

**Garten**: neue Spalte **Maßstab** (Skizzeneinheiten pro Meter, Standard 10).

**Fläche**: neue Spalte **Farbe** (Zahl, leer = Standardfarbe der Oberfläche). Die Reihenfolge kommt künftig aus dem gemeinsamen Zähler.

Neue Tabelle **Gegenstand** (ab 3a angelegt, ab 3b benutzt):

| Spalte | Bedeutung |
|---|---|
| Nummer | feste Kennung |
| Garten | zu welchem Garten (Löschen des Gartens löscht ihn mit) |
| Art | Schlüssel aus dem Katalog, z. B. "haus", "zaun", "gebaeude" |
| Name | optional |
| Mitte X, Mitte Y | Lage der Mitte in Skizzeneinheiten (bei freien Gebäuden die Lage der Punkte relativ dazu) |
| Breite, Höhe | Größe in Skizzeneinheiten (vor der Drehung); bei freien Gebäuden ungenutzt |
| Drehung | Grad |
| Farbe | Hauptfarbe als Zahl, leer = Standard |
| Reihenfolge | gemeinsame Reihenfolge mit den Flächen |

Neue Tabelle **Gegenstandspunkt** (nur für freie Gebäude, Lage relativ zur Mitte, vor der Drehung): Gegenstand, Nummer, X, Y, Art (Ecke oder rund), gleicher Aufbau wie die Punkte der Flächen.

Entwicklungsmodus: Die Datenbank wird bei Änderung neu angelegt (einmaliger Datenverlust der Testgärten). **Echte Migrationen kommen vor der ersten veröffentlichten Version.** Rückgängig-Verlauf wird nicht gespeichert.

---

## 12. Risiken und Unsicherheiten

- **Bedienung am Finger:** Acht Größen-Griffe plus Drehgriff bei kleinen Gegenständen, besonders in Verbindung mit der Ebenenliste am Rand und den Linealen (Platz!). Auf einem kleinen Bildschirm bleibt dann weniger Zeichenfläche. Die Lineale sind schmal und halbtransparent, die Liste ist schmal und lässt sich einklappen. Ob das reicht, zeigt der Test. **Nicht geprüft.**
- **Berührungen:** Gegenstände, Flächen, Punkte, Griffe, Lineale und Liste wollen alle Berührungen. Ich lege eine feste Reihenfolge fest (Liste vor Griffen vor Gegenständen vor Flächen). Der Gesten-Code wird deutlich umfangreicher. Ich kann ihn nicht kompilieren und rechne beim ersten Test mit Fehlverhalten oder Fehlbau.
- **Gedrehte Gegenstände:** Tippen und Ziehen an gedrehten Rechtecken braucht Umrechnung ins eigene Koordinatensystem. Klassische Fehlerquelle, nicht am Gerät getestet.
- **Mischbare Ebenen:** Weil alles dazwischen liegen kann, muss das Zeichnen alle Elemente in einer gemeinsamen Reihenfolge sortieren und die Tippabfrage von oben nach unten laufen. Mehr Fehlerquellen als bei festen Gruppen.
- **Maßstab und Altdaten:** Flächen, die vor dem Maßstab gezeichnet wurden, bekommen durch den Anfangswert willkürliche Metermaße. Weil die Testgärten ohnehin verschwinden, ist das jetzt kein Problem. Später (nach Veröffentlichung) wäre es eines, deshalb jetzt einführen.
- **Fläche in m² ist eine Näherung** (Kurve, nicht exakt), wird in der Karte als "ca." angezeigt.
- **Symbole sind Geschmackssache** und viel Zeichenarbeit (etwa 39). Detailgrad lässt sich später steigern, ohne die Datenbank zu ändern.
- **Leistung:** Bei einigen Dutzend Elementen erwarte ich keine Probleme. Muster in Hecke/Zaun wiederholen sich entlang langer Gegenstände, bei sehr langen vielen Stücken könnte es ruckeln (Schätzung, ungetestet).
- **Farbe bei Flächen:** Eine gewählte Farbe verändert die Wirkung des Musters (kontrastarme Farben, zum Beispiel sehr dunkle). Die Ableitung von Rand und Muster aus der Farbe ist eine Näherung und braucht deinen Blick am Gerät.
- **Zwei Geometrien:** Rechteckige Gegenstände und freie Gebäude werden verschieden bearbeitet (Griffe gegen Punkte). Das erhöht die Gesten-Komplexität, nutzt aber den bestehenden Flächencode.
