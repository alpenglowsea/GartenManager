# GartenManager: Konzept Gartenplan (Umsetzungsschritt 2)

Stand: 30.09.2026, Version 0.7 (Fläche schließt per Tipp auf den ersten Punkt; Ausrichtungshilfe; geometrische Formen)

Dieses Dokument beschreibt, **wie das Zeichnen des Gartens aussehen und funktionieren soll**, bevor Code entsteht. Es geht nur um Gärten und Flächen. Gegenstände (Häuser, Schuppen, Autos) und Pflanzen kommen in späteren Schritten dazu.

Kennzeichnung wie in PROJEKT.md: **Entschieden**, **Vorschlag**, **Offen**, **Nicht geprüft**.

---

## 1. Was in diesem Schritt entsteht, und was nicht

**Entsteht:**
- Mehrere Gärten anlegen, umbenennen, löschen. Gärten lassen sich **optional** zu einem Grundstück zusammenfassen.
- In einem Garten Flächen zeichnen: Punkte setzen, die App rundet die Kurve ab.
- Jeder Fläche eine Oberfläche geben (Gras, Erde, Stein, Holz, …).
- Flächen später bearbeiten: Punkte verschieben, hinzufügen, löschen; Reihenfolge ändern; Fläche löschen.
- Die Ansicht zoomen und verschieben.
- **Zwei Betriebsarten pro Garten: Ansicht (Standard, nur Betrachten) und Bearbeiten.**
- Alles bleibt auf dem Handy gespeichert.

**Entsteht noch nicht:** Gegenstände, Pflanzen, Pflegekalender, Backup, Maßstab mit Metern. Es bleibt bei einer Skizze ohne Meterangaben (**Entschieden**).

---

## 2. Bereits entschieden (29.09.2026)

- **Zeichnen:** Der Nutzer setzt nacheinander Punkte, die App verbindet sie zu einer weichen Kurve. Einzelne Punkte lassen sich später verschieben. **Entschieden**
- **Überlagerung:** Die zuletzt gezeichnete Fläche liegt oben. Die Reihenfolge lässt sich ändern. **Entschieden**
- **Mehrere Gärten:** Ja, von Anfang an. Auch mehrere getrennte Gärten auf einem Grundstück sind möglich (Beispiel Reihenhaus). **Entschieden**
- **Grundstück (optional):** Mehrere Gärten (Abschnitte) können zu einem Grundstück gehören. Das hilft bei sehr großen Gärten, damit man nicht ständig zoomen und verschieben muss. Es ist vollkommen freiwillig: Wer lieber mehrere einzelne Gärten anlegt, kann das tun. **Entschieden**
- **Ecken pro Punkt:** Jeder Punkt kann Ecke oder rund sein, für maximale Flexibilität. **Entschieden**
- **Flächen haben einen Namen** (optional). Ein Beet ist eine Fläche mit Oberfläche Erde und Namen, kein eigener Typ. **Entschieden**
- **Rückgängig-Knopf** von Anfang an. **Entschieden**
- **Oberflächenliste** wie in 3.5. Alles Weitere regeln später die Objekte oder ein Update. **Entschieden**
- **Kurve läuft durch die gesetzten Punkte** (Abschnitt 4). **Entschieden**
- **Sofort speichern:** Änderungen im Bearbeitungsmodus werden sofort gespeichert, es gibt keinen Speichern- oder Verwerfen-Knopf. Wer nur ausprobieren will, legt vorher eine Kopie des Gartens an. **Entschieden**
- **Garten duplizieren:** über das Menü (langes Drücken auf einen Garten in der Liste). **Entschieden**
- **Ansicht ist der Standard, Bearbeiten ist ein eigener Modus** (Abschnitt 3.2). Ein Tipp auf einen Garten in der Liste öffnet die Ansicht. Aus der Ansicht startet man das Bearbeiten; alternativ gibt es im Menü (langes Drücken auf den Garten) den Eintrag "Bearbeiten". **Entschieden**

---

## 3. So soll es sich bedienen (Vorschlag)

### 3.1 Startseite: Gartenliste

- Eine Liste mit **Grundstücken** und **Gärten**. Ein Grundstück ist ein aufklappbarer Eintrag, der seine Gärten enthält. Gärten ohne Grundstück stehen direkt in der Liste.
- Ein Knopf "+" bietet **Neuer Garten** und **Neues Grundstück**. Beides fragt nach einem Namen (z. B. Garten "Reihenhaus vorne", Grundstück "Schrebergarten mit Obstwiese").
- **Wer keine Grundstücke will, sieht keine:** Die Liste sieht dann aus wie eine einfache Gartenliste. Grundstücke werden nie aufgedrängt.
- Ein Grundstück enthält einen Eintrag "Garten hinzufügen". Ein Garten lässt sich später über sein Menü einem Grundstück zuordnen, in ein anderes verschieben oder aus dem Grundstück herauslösen.
- Ein Tipp auf einen Garten öffnet die **Ansicht** (nur Betrachten, siehe 3.2). Langes Drücken öffnet ein Menü: **Bearbeiten** (öffnet den Garten gleich im Bearbeitungsmodus), **Duplizieren** (legt eine Kopie an, siehe unten), Umbenennen, Zu Grundstück verschieben, Löschen.
- **Grundstück löschen:** Es gibt zwei Wege: "Nur Grundstück auflösen" (die Gärten bleiben als einzelne Gärten erhalten) und "Grundstück samt Gärten löschen". Beide fragen nach.
- **Garten löschen fragt immer nach** ("Garten samt allen Flächen wirklich löschen?"), weil es nicht rückgängig zu machen ist.
- **Duplizieren:** Legt eine Kopie des Gartens mit allen Flächen und Punkten an, Name "Originalname (Kopie)", im selben Grundstück wie das Original. Gedacht für Experimente und Umbauvarianten: Man probiert in der Kopie und behält das Original unverändert. Die Kopie ist danach ein ganz normaler, unabhängiger Garten.
- Ist noch nichts vorhanden, zeigt die Seite einen freundlichen Hinweis und den Knopf "Ersten Garten anlegen".

### 3.2 Ansicht und Bearbeiten: zwei Betriebsarten

Ein Garten wird nicht ständig umgebaut. Meist will man ihn nur ansehen, Pflanzen antippen und Informationen lesen. Deshalb hat jeder Garten zwei Betriebsarten (**Entschieden**):

**Ansicht (Standard).** Ein Tipp auf einen Garten in der Liste öffnet sie.
- Man sieht den Garten. Mit **zwei Fingern** zoomt man und verschiebt die Ansicht.
- Ein Tipp auf eine Fläche zeigt ihre Angaben (Name, Oberfläche). Ein Tipp auf eine Pflanze öffnet später das Info-Modal mit allen Angaben zur Pflanze. Das kommt mit den Pflanzen (Umsetzungsschritte 3 und 4), in diesem Schritt gibt es nur Flächen.
- **Nichts lässt sich versehentlich verändern.** Es gibt keine Griffe, kein Verschieben, kein Löschen.
- Ein Knopf "Bearbeiten" (Stift-Symbol) wechselt in den Bearbeitungsmodus.

**Bearbeiten.** Erreichbar über den Knopf "Bearbeiten" in der Ansicht oder direkt über das Menü in der Gartenliste (langes Drücken, "Bearbeiten").
- Ein deutlich gekennzeichneter Modus: Die Kopfleiste ändert Farbe und Titel ("Bearbeiten"), damit man nie im Zweifel ist, in welcher Betriebsart man gerade ist. **Entschieden**
- Unten eine Werkzeugleiste: **Fläche zeichnen**, **Rückgängig**. In der Kopfleiste steht **Fertig**, das zurück in die Ansicht führt. Auch die Zurück-Taste des Handys führt zurück in die Ansicht.
- **Alle Änderungen werden sofort gespeichert** (**Entschieden**). Es gibt keinen Speichern- und keinen Verwerfen-Knopf. Das vermeidet auch das Problem, dass Android die App im Hintergrund beenden und nicht gespeicherte Änderungen vernichten könnte.
- **Zum Ausprobieren:** Wer Änderungen nur durchspielen will, legt in der Gartenliste vorher eine **Kopie** an (Menü, "Duplizieren") und probiert in der Kopie. Das Original bleibt unberührt. **Entschieden**
- Der Rückgängig-Verlauf gilt, solange man im Bearbeitungsmodus ist. Beim Verlassen ist er weg; die gespeicherten Änderungen bleiben. **Entschieden**
- Rückgängig macht auch gespeicherte Schritte rückgängig, solange man im Modus ist (weil alles sofort gespeichert wird, wird auch das Zurücknehmen sofort gespeichert).

**Für beide Betriebsarten:** Die letzte Ansicht (Zoom und Ausschnitt) wird pro Garten gemerkt.

### 3.3 Eine Fläche zeichnen (im Bearbeitungsmodus)

1. Tipp auf "Fläche zeichnen".
2. Punkte nacheinander setzen: Finger auf die Stelle legen, bei Bedarf verschieben, **loslassen setzt den Punkt** (so verdeckt der Finger die Stelle nicht). Die Linie durch die Punkte bleibt **offen**, sie schließt sich nicht von selbst. **Entschieden** (30.09.2026)
3. Ab dem dritten Punkt erscheint ein Ring um den ersten Punkt. **Ein Tipp auf den ersten Punkt schließt die Fläche.** Einen eigenen Knopf "Fläche abschließen" gibt es nicht mehr. **Entschieden** (30.09.2026)
4. Danach öffnet sich die Auswahl der **Oberfläche** (ab 2d). Die Fläche ist sofort sichtbar gefüllt.
5. Mit "Abbrechen" verwirft man die angefangene Fläche. Den letzten Punkt nimmt "Rückgängig" zurück.

**Ausrichtungshilfe (Einrasten). Entschieden** (30.09.2026): Beim Setzen eines Punktes rastet dieser ein, wenn er in die Nähe (etwa 12 Punkte auf dem Bildschirm) einer Hilfslinie kommt: waagerecht oder senkrecht zu einem schon gesetzten Punkt, in Verlängerung der letzten Kante oder im rechten Winkel zur letzten Kante. Liegen zwei Hilfslinien in Reichweite, rastet der Punkt an ihrem Schnittpunkt ein (so entstehen saubere Rechtecke). Die Hilfslinien erscheinen gestrichelt, solange der Finger liegt. Standardmäßig an, in der Werkzeugleiste abschaltbar ("Einrasten: an/aus"). Die Reichweite (12) ist ein Schätzwert, ungetestet am Gerät.

### 3.3a Geometrische Formen (im Bearbeitungsmodus)

**Entschieden** (30.09.2026): Der Knopf "Form" bietet Rechteck, Quadrat, Kreis, Ellipse, Dreieck, Fünfeck, Sechseck und Achteck. Nach der Wahl zieht man die Form mit einem Finger auf, von einer Ecke zur gegenüberliegenden Ecke des umschließenden Rechtecks. Quadrat, Kreis und die Vielecke werden immer in ein Quadrat gezogen. Das Ergebnis ist eine ganz normale Fläche aus Punkten (Rechteck: 4 Eckpunkte, Kreis und Ellipse: 8 runde Punkte). Dadurch lassen sich an der Form später **Punkte ziehen, hinzufügen und löschen**, um sie zu verändern, zum Beispiel für ein nicht ganz rechteckiges Grundstück. Das Bearbeiten der Punkte kommt mit Teilschritt 2e. **Hinweis:** Eine Ellipse, die aus 8 Punkten besteht, ist eine Annäherung und keine mathematisch exakte Ellipse.

### 3.4 Eine Fläche bearbeiten (im Bearbeitungsmodus)

- Ein Tipp auf eine Fläche wählt sie aus. Ihre Punkte werden als Griffe sichtbar.
- **Punkt verschieben:** Griff mit dem Finger ziehen.
- **Punkt hinzufügen:** auf den Rand tippen, dort entsteht ein neuer Punkt.
- **Punkt löschen:** Griff antippen, dann "Punkt löschen". Eine Fläche behält mindestens drei Punkte.
- **Fläche verschieben:** die Fläche selbst ziehen.
- Ein Menü an der ausgewählten Fläche bietet: Oberfläche ändern, Name ändern, **Nach vorn / Nach hinten**, Löschen (mit Rückfrage).

### 3.5 Oberflächen

**Entschieden** (Liste für den Anfang; Ergänzungen kommen bei Bedarf mit einem Update, auch nach Wünschen von Anwendern). Jede Oberfläche bekommt eine eigene Farbe und ein dezentes Muster, die ich selbst zeichne; echte Fototexturen erst später:

| Oberfläche | Gedacht für |
|---|---|
| Gras | Rasen |
| Erde | Beet, unbewachsener Boden |
| Kies | Wege, Stellflächen |
| Pflaster/Stein | Terrasse, Wege |
| Holz | Terrasse, Deck |
| Sand | Spielplatz, Sandkasten |
| Wasser | Teich, Pool |
| Beton/Asphalt | Einfahrt |

Die Liste ist in der App fest eingebaut und leicht erweiterbar. Im Speicher steht nur der Schlüssel ("gras", "erde"), nicht die Farbe. So lässt sich das Aussehen später ändern, ohne gespeicherte Gärten anzufassen.

### 3.6 Rückgängig

**Entschieden:** Die letzten Schritte lassen sich zurücknehmen (Punkt setzen, Punkt verschieben, Fläche löschen). Auf dem Handy passieren Fehler mit dem Finger schnell. Ohne "Rückgängig" wäre das Bedienen frustrierend.

---

## 4. Wie die Kurve entsteht

**Entschieden:** Die Kurve läuft **durch** die gesetzten Punkte (Fachbegriff: Catmull-Rom-Spline). Der Nutzer sieht also genau dort einen Punkt, wo die Linie hindurchgeht.

Wichtig für Flächen mit geraden Kanten, etwa eine rechteckige Terrasse: Ein reiner Rundungsalgorithmus würde die Ecken abrunden. Deshalb kann **jeder Punkt eine Ecke oder ein runder Punkt** sein (**Entschieden**). Ein Tipp auf einen ausgewählten Griff schaltet um. Zusätzlich gibt es "Alle Ecken eckig" und "Alle Punkte rund" für die ganze Fläche.

**Bekannte Schwäche (Nicht geprüft am Gerät):** Setzt man zwei Punkte sehr nah beieinander und einen weit entfernt, kann die Kurve überschießen und eine Beule bilden. Es gibt Varianten des Verfahrens, die das abmildern. Ich wähle beim Bau die gutmütigste. Ob das im Alltag ausreicht, kannst nur du auf dem Handy beurteilen.

---

## 5. Was gespeichert wird (kommt in Datei 2 "Meine Daten")

Wie in DATENMODELL.md beschrieben, liegen alle Gartendaten in der Datei, die bei Updates nie überschrieben wird und die im Backup steckt.

**Grundstück** (optional, kann fehlen)
| Spalte | Bedeutung |
|---|---|
| Nummer | feste Kennung |
| Name | z. B. "Schrebergarten mit Obstwiese" |

**Garten**
| Spalte | Bedeutung |
|---|---|
| Nummer | feste Kennung |
| Grundstück | zu welchem Grundstück er gehört; **leer, wenn keins** |
| Name | z. B. "Obstwiese" |
| Angelegt / Geändert | Datum |
| Ansicht | Zoom und Ausschnitt (letzte Ansicht) |

**Fläche**
| Spalte | Bedeutung |
|---|---|
| Nummer | feste Kennung |
| Garten | zu welchem Garten sie gehört |
| Name | optional, z. B. "Gemüsebeet" |
| Oberfläche | Schlüssel, z. B. "gras" |
| Reihenfolge | wer oben liegt |

**Punkt**
| Spalte | Bedeutung |
|---|---|
| Fläche | zu welcher Fläche er gehört |
| Nummer in der Fläche | 1, 2, 3, … in Zeichenreihenfolge |
| X und Y | Lage auf der Zeichenfläche |
| Art | Ecke oder rund |

**Koordinaten ohne Meter:** X und Y sind Zahlen auf einer unendlichen Zeichenfläche in "Skizzeneinheiten". Sie haben keine feste Bedeutung wie Meter. Das passt zu deiner Vorgabe, dass es eine Skizze ist. Sollte später ein optionaler Maßstab gewünscht sein, ist er nachrüstbar, ohne bestehende Gärten kaputtzumachen.

**Löschen:** Wird ein Garten gelöscht, verschwinden automatisch seine Flächen und Punkte. Wird eine Fläche gelöscht, verschwinden ihre Punkte. Beim Löschen eines Grundstücks gilt, was der Nutzer gewählt hat (Gärten behalten oder mitlöschen).

**Nicht gespeichert:** Der Rückgängig-Verlauf. Er gilt nur, solange man im Bearbeitungsmodus ist.

---

## 6. Umsetzung in Teilschritten

Jeder Teilschritt ist eine eigene Lieferung. Du pushst, GitHub baut, du testest auf dem Handy. So merken wir Fehler früh.

| Schritt | Ergebnis auf dem Handy |
|---|---|
| 2a | Gartenliste: Gärten und Grundstücke anlegen, umbenennen, verschieben, duplizieren, löschen. (Das Duplizieren kopiert zunächst nur den leeren Garten; ab 2c kopiert es auch Flächen und Punkte.) Erste Datenbank in der App. Nach dem Schließen und Neustarten sind die Gärten noch da. |
| 2b | Leere Gartenfläche mit Zoomen und Verschieben. Ansicht und Bearbeitungsmodus mit Umschalter (im Bearbeitungsmodus noch ohne Werkzeuge). Über das Menü in der Liste lässt sich "Bearbeiten" direkt wählen. |
| 2c | Im Bearbeitungsmodus Flächen zeichnen: Punkte setzen, Kurve, Schließen per Tipp auf den ersten Punkt, Ausrichtungshilfe, geometrische Formen. Alles wird sofort gespeichert. Flächen sind danach in der Ansicht sichtbar, und Duplizieren kopiert sie mit. (Am Handy getestet: Zeichnen, Kurven, Rückgängig, Speichern, Duplizieren; Nachbesserungen nach dem Test: Schließen, Ausrichtung, Formen.) |
| 2d | Oberflächen mit Farben und Mustern. In der Ansicht zeigt ein Tipp auf eine Fläche Name und Oberfläche. |
| 2e | Bearbeiten: Punkte verschieben, hinzufügen, löschen, Reihenfolge, Löschen, Rückgängig. |

Wie lange das dauert, kann ich nicht seriös schätzen (wie im Rest des Projekts).

---

## 7. Risiken und Unsicherheiten

- **Bedienbarkeit am Finger:** Ob das Antippen der Griffe, das Zoomen und das Zeichnen sich gut anfühlen, lässt sich nicht am Schreibtisch klären. Ich baue Griffe bewusst groß (Fingerspitze, mindestens etwa 48 Punkte). Danach brauche ich dein Urteil am Gerät.
- **Erste Datenbank:** Mit Schritt 2a kommt die Bibliothek für die Datenbank (Room) in die App. Sie braucht ein weiteres Werkzeug beim Bauen (KSP). Beides ist F-Droid-tauglich, soweit ich weiß. **Nicht geprüft.** Die passenden Versionsnummern kann ich von hier aus nicht nachschlagen. Ein Fehlschlag beim ersten Bau ist möglich, wie schon beim Grundgerüst.
- **Sich überschneidende Ränder:** Zeichnet jemand einen Rand, der sich selbst kreuzt (Acht), sieht die Füllung seltsam aus. **Vorschlag:** erlauben und nicht eingreifen. Wer das nicht will, korrigiert die Punkte.
- **Leistung bei vielen Flächen:** Ich erwarte bei normalen Gärten (einige Dutzend Flächen) keine Probleme. Das ist eine Schätzung, ungetestet.
- **Zwei Datenbank-Dateien:** Garten und Flächen liegen in Datei 2. Die Grunddaten (Pflanzen) kommen erst in Schritt 4 dazu. In diesem Schritt gibt es nur Datei 2.

---

## 8. Entschiedene Fragen (29.09.2026)

a) Ecken pro Punkt: **Entschieden.**
b) Namen für Flächen, kein eigener Typ "Beet": **Entschieden.**
c) Rückgängig von Anfang an: **Entschieden.**
d) Oberflächenliste wie in 3.5: **Entschieden.** Weiteres über Objekte, später.
e) Grundstück als optionale Gruppe von Gärten: **Entschieden.**

**Noch offen (klein):** Soll ein Grundstück später eine **Übersicht** bekommen, die zeigt, wie die Abschnitte zueinander liegen? Dieses Konzept baut sie nicht. Ein Grundstück ist erstmal nur eine Gruppe in der Liste. Ich würde erst nach dem Testen entscheiden, ob die Übersicht gebraucht wird. Der Aufbau hier schließt sie nicht aus.
