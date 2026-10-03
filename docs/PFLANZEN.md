# GartenManager: Konzept Pflanzen (Umsetzungsschritt 4)

Stand: 03.10.2026, Version 0.3 (alle Rückfragen beantwortet; Piktogramme und Initialen freigegeben)

Dieses Dokument beschreibt, **wie die Pflanzen in die App kommen**: die mitgelieferte Pflanzendatenbank, die Auswahl mit Suche und Filter, das Info-Modal, das Platzieren im Garten und das Ergänzen eigener Angaben. Es baut auf zwei Dokumenten auf, die ich hier nicht wiederhole:

- **PROJEKT.md**, Abschnitte 4 bis 6: Quellen, Lizenzen, Grundsätze, Ergebnis des Probelaufs, Aufbau des Info-Modals, Nutzerbeiträge.
- **DATENMODELL.md** (Version 0.2): zwei getrennte Datenbankdateien, Tabellen, Update-Regeln.

Kennzeichnung wie in PROJEKT.md: **Entschieden** (der Projektinhaber hat zugestimmt), **Vorschlag** (Claude schlägt vor), **Offen** (braucht Entscheidung), **Nicht geprüft** (Claude hat es nicht ausprobiert).

---

## 1. Was in diesem Schritt entsteht, und was nicht

**Entsteht:**
- Die **Grunddaten-Datei** (Datei 1 aus DATENMODELL.md) mit Pflanzen, Namen, Gruppen, Infotexten, Quellen und der Danke-Liste, mitgeliefert in der App.
- Ein **Werkzeug auf deinem Rechner**, das die Grunddaten aus Wikidata und Wikipedia holt, nach den bereits entschiedenen Regeln sortiert und die Datei baut. Du führst es aus, ich liefere die Skripte.
- Der **Auswahl-Dialog** mit Suche und Gruppenfilter.
- Das **Info-Modal** mit den acht Abschnitten, dem festen Abschnitt "Quellen und Lizenzen" und "Selbst ergänzen".
- **Pflanzen im Garten platzieren**, verschieben, in der Größe ändern, einfärben, benennen, duplizieren, löschen. Sie sind Teil der Ebenenliste, des Rückgängig-Verlaufs und des Garten-Duplizierens.
- **Eigene Pflanzen** und **eigene Notizen** zu Abschnitten (Datei 2).

**Entsteht noch nicht:** Pflegekalender und Kalender-Sync (Schritt 6 und 7), Backup (Schritt 5), Kamera-Erkennung (Schritt 9), Pflanzenfotos, Einreichen von Vorschlägen an die Prüfer (siehe Abschnitt 7).

---

## 2. Ehrliche Vorbemerkung zu den Daten

Dein Funktionswunsch nennt Filteroptionen. Mit dem, was wir entschieden haben, gibt es **nur den Gruppenfilter** (die 13 Gruppen) und die Suche. Filter wie "Sonne oder Schatten", "Blütezeit", "Blütenfarbe" oder "winterhart" brauchen **strukturierte** Angaben. Wikipedia liefert Fließtext, und aus Fließtext sortiere ich nur nach Überschriften, nicht nach Inhalt (Entscheidung vom 29.09.2026). Solche Filter wären erst möglich, wenn Angaben von Hand oder aus einer weiteren frei lizenzierten Quelle strukturiert vorliegen. **Offen** für später, nicht für diesen Schritt.

**Entschieden (03.10.2026):** Wir starten mit **Suche und Gruppenfilter**. Weitere Filter später einzubauen ist **als Oberfläche wenig Aufwand**, der Aufwand liegt in den **Daten**: Jemand muss die Angaben (zum Beispiel "sonnig", "halbschattig") je Pflanze strukturiert erfassen oder aus einer frei lizenzierten Quelle holen. Damit später nichts umgebaut werden muss, sieht die Grunddaten-Datei eine zunächst **leere Tabelle "Merkmal"** vor (Pflanze, Merkmalsart, Wert, Quelle). **Vorschlag**, ist nicht aufwendig und ändert nichts am Rest.

Ebenso: Pflege und Standort sind bei den meisten Pflanzen **leer** (Probelauf: brauchbare Pflege bei etwa 3 bis 5 von 20). Das Info-Modal wird daher bei vielen Pflanzen Lücken zeigen. Das ist gewollt ("lieber leer als falsch"), sieht aber am Anfang dünn aus.

---

## 3. Vorgeschlagene Technik

**Vorschlag, nicht geprüft** (ich kann nicht kompilieren und habe es nicht auf dem Gerät ausprobiert):

1. **Grunddaten als einfache SQLite-Datei, Datei 2 weiter mit Room.** Zur Frage "ist Room nötig?": Room ist eine Programmbibliothek, die Datenbanken für uns verwaltet; die Gärten und Gegenstände nutzen sie schon und das bleibt so. Für die **mitgelieferte** Pflanzendatei verlangt Room, dass Tabellen und eine interne Prüfsumme **genau** zu seinem Code passen. Das kann ich ohne Compiler und Gerät nicht absichern. Eine schlichte, nur lesbare Datei, die ich mit einem Python-Skript baue, hat dieses Problem nicht. **Deine Anforderung (Entschieden):** Updates sollen den Pflanzenbestand erweitern können, und die Einträge der Nutzer bleiben dabei erhalten. Beides erfüllt diese Technik: Bei einem Update wird nur die Pflanzendatei ersetzt (sobald ihre eingebaute Versionsnummer neuer ist), die Nutzereinträge liegen in der anderen Datei und werden nie berührt. Wie Nutzerbeiträge später in die Pflanzendatei kommen, ist der Weg über die Prüfer (PROJEKT.md, Abschnitt 6): Sie landen als geprüfte Texte in den Textdateien und damit im nächsten Update. **Die Technikwahl liegt bei mir; falls sich beim ersten Test zeigt, dass sie nicht trägt, wechsle ich und sage es dir.** Nicht geprüft.
2. **Die Wahrheit liegt in Textdateien im Repository**, eine pro Pflanze (so schon in DATENMODELL.md, Abschnitt 4 vorgesehen). Format: **JSON** (kommt ohne Zusatzpaket aus; Prüfer können es mit etwas Hilfe bearbeiten). Die Grunddaten-Datei wird daraus gebaut.
3. **Feste Pflanzennummern** in einer Datei `nummern.csv` im Repository: Das Skript vergibt neue Nummern nur für neue Pflanzen und **nie** eine alte neu (Regel aus DATENMODELL.md, Abschnitt 6).
4. **Wer baut die Datei?** Zuerst baust du sie mit dem Skript auf deinem Rechner und checkst sie mit ein. Später kann GitHub Actions sie bauen. **Nicht geprüft:** ob F-Droid so einen Bauschritt (Python vor dem App-Bau) akzeptiert. Das klären wir vor der F-Droid-Vorbereitung.
5. **Suche** durchsucht alle Namen einer Pflanze (Hauptname, umgangssprachliche Namen, lateinische Namen) ohne Rücksicht auf Groß- und Kleinschreibung, mit Umlauten gleichgestellt zu "ae/oe/ue" (Schätzung: für etwa 1.000 Namen läuft das ohne spürbare Wartezeit; nicht gemessen).

---

## 4. Vorgeschlagene Bedienung

### 4.1 Pflanze platzieren (**Entschieden** am 03.10.2026)
- Knopf **"Pflanze"** in der Werkzeugleiste.
- Erst den **Ort im Garten antippen**, dann öffnet sich der **Auswahl-Dialog**. (Der Ablauf entspricht dem bei Gegenständen und bei deinem Wunsch aus dem Projektauftrag: "nach dem Anlegen einer Pflanze öffnet sich ein Modal mit Auswahl der Pflanze".)
- Im Auswahl-Dialog: oben **Suchfeld**, darunter die **13 Gruppen als Filter** (mehrere wählbar), darunter die Liste (Hauptname, darunter lateinischer Name klein, rechts die Gruppe).
- Tipp auf einen Eintrag öffnet das **Info-Modal** mit den Angaben und dem Knopf **"Hier platzieren"**. Zurück führt in die Liste.
- Die Pflanze erscheint in der **aktiven Ebene**.

### 4.2 Darstellung im Plan (**Entschieden** am 03.10.2026, Ausführung Vorschlag)
Für 539 Pflanzen eigene Zeichnungen wären nicht machbar. Entscheidung des Projektinhabers: **ein Piktogramm je Gruppe** (13 Stück), dazu die **Initialen des umgangssprachlichen (nicht lateinischen) Namens**. Ich zeichne die 13 Piktogramme selbst (keine Lizenzfrage), in der App werden sie vom Handy gezeichnet, wie die Gegenstände.
- Eine Pflanze ist ein **Kreis** (Krone oder Wuchsbreite) in einer **Farbe je Gruppe** mit dem **Piktogramm ihrer Hauptgruppe** und den **Initialen**.
- Die **Farbe lässt sich wie bei Gegenständen aus den 12 Farbfeldern ändern** (Wunsch des Projektinhabers). Ändert sich die Farbe, ändert sich der Kreis samt Piktogramm.
- Je nach Größe auf dem Bildschirm: sehr klein nur Piktogramm, größer Piktogramm und Initialen (Schwellen sind Schätzwerte, die wir am Gerät einstellen).
- Namen stehen nicht dauerhaft im Plan, sondern in der Karte nach dem Antippen (wie bei Gegenständen, entschieden).
- Eine Skizze der 13 Piktogramme und ein Beispiel im Plan: `symbole-pflanzen.png`. **Entwurf, Geschmack und Wiedererkennbarkeit sind noch nicht bestätigt.**
- **Initialen-Regel (Vorschlag):** je ein Buchstabe der ersten beiden Wortteile des deutschen Namens (Leerzeichen oder Bindestrich trennen); bei einem einzigen Wort die ersten beiden Buchstaben. Beispiele: Wald-Erdbeere → WE, Gartenhortensie → Ga, Tomate → To. Bei Namen wie "Echter Lavendel" ergibt das "EL", was wenig sagt; die Alternative (das Hauptwort statt des Beiworts nehmen) erfordert Urteil und lässt sich später ändern. **Offen**, wir probieren es am Gerät.

### 4.3 Größe (**Entschieden** am 03.10.2026)
Wikipedia nennt Wuchsmaße im Fließtext, nicht strukturiert. Ich lese sie **nicht automatisch aus**. Jede Pflanze startet mit einer **Standardgröße je Gruppe** (zum Beispiel Stauden 0,5 m, Sträucher 1,5 m, Bäume 4 m, Heckenpflanzen 0,6 m; **Schätzwerte von mir, nicht belegt**). Der Nutzer ändert sie an den Griffen, mit Maß live daneben, wie bei Gegenständen. Der Projektinhaber probiert die Werte in der App aus und gibt Rückmeldung. Eine Pflanze kann später einen eigenen Standardwert tragen.

### 4.4 Bearbeiten im Garten
- Auswählen, verschieben, **Größe ändern** (Kreis, Seitenverhältnis bleibt), Menü per langem Tipp: **Name**, **Farbe**, **Duplizieren**, **Eine Ebene nach vorn/hinten**, **Pflanzdatum**, **Notiz**, **Info anzeigen**, **Löschen**. Kein Drehen (ein Kreis hat keine Richtung).
- **Einrasten** wie in 3d (an Kanten und Ecken anderer Formen; bei Kreisen am Mittelpunkt und am Rand).
- Tippen in der Ansicht zeigt die **Karte** (Name, lateinischer Name, Größe, Pflanzdatum, Notiz) mit Knopf "Info".
- Ebenenliste, Rückgängig, Garten duplizieren: wie bei Gegenständen.

### 4.5 Info-Modal (aus PROJEKT.md, Abschnitt 5, schon entschieden)
- Acht Abschnitte als aufklappbare Liste: Bezeichnung, Gattung, Herkunft, Standort, Wuchs und Laub, Blüte, Verwendung, Pflege. Beim Öffnen alle aufgeklappt, zugeklappte Abschnitte werden **pro Pflanzenart** gemerkt.
- Unten **"Quellen und Lizenzen"**, nicht wegklappbar.
- Leere Abschnitte: "Noch keine Angaben" mit **"Selbst ergänzen"**.
- Texte aus Wikipedia sind als **Auszug** gekennzeichnet (Entschieden, DATENMODELL.md 4.2).
- Eigene Notizen erscheinen als **"Meine Notiz"** unter dem Grundtext oder an seiner Stelle (Entschieden).

### 4.6 Eigene Pflanze anlegen (Vorschlag)
- Im Auswahl-Dialog **"Eigene Pflanze"**: Name, lateinischer Name (freiwillig), Gruppen. Dann **"Selbst ergänzen"** in den Abschnitten. Liegt in Datei 2 mit eigenem Nummernkreis (Entschieden, DATENMODELL.md 3.2).

---

## 5. Was gespeichert wird (Vorschlag)

**Datei 1 "Grunddaten"** (mitgeliefert, nur lesbar): wie in DATENMODELL.md, Abschnitte 2.1 bis 2.6, plus zwei Kleinigkeiten:
- **Metadaten**: Versionsnummer der Grunddaten, Datum, Anzahl Pflanzen.
- **Danke-Liste** (Entschieden): Pflanze, Abschnitt, freiwilliger Name.

**Datei 2 "Meine Daten"** (Room, Teil der Datenbank, die schon Gärten und Gegenstände enthält): neue Tabellen
- **Pflanzung** (die platzierte Pflanze): Nummer, Garten, Ebene, Reihenfolge (gemeinsamer Zähler mit Flächen und Gegenständen derselben Ebene), Verweis auf die Pflanze (**Herkunft** Grundpflanze oder eigene Pflanze, **Nummer**), **Notfall-Kopie des Namens** (Entschieden, DATENMODELL.md Abschnitt 6), Mitte X und Y, Durchmesser, Farbe, Pflanzdatum (leer erlaubt), Notiz.
- **Eigene Pflanze**, **Eigener Name**, **Eigene Gruppenzuordnung**, **Eigene Angabe** (Notiz zu einem Abschnitt, Status "nur privat", Datum): wie in DATENMODELL.md, Abschnitte 3.1 und 3.2.
- **Merker**: zugeklappte Abschnitte pro Pflanzenart.

Datenbankversion 5 wird dafür auf **6** erhöht. Pflanzen haben **Farbe** als eigene Spalte (wie Gegenstände). **Wie bei 3a gehen die Testgärten dabei einmalig verloren** (Entwicklungsmodus). Alle neuen Tabellen kommen **schon im ersten Teilschritt mit Datei-2-Änderung (4d)**, damit die Datenbank nur einmal neu entsteht. Echte Migrationen kommen weiterhin vor der ersten veröffentlichten Version.

---

## 6. Umsetzung in Teilschritten (Vorschlag)

Wie bei den Schritten 2 und 3: Jeder Teilschritt ist eine eigene Lieferung. Du pushst, GitHub baut, du testest.

| Schritt | Ergebnis |
|---|---|
| 4a | **Gerüst der Grunddaten:** Datei 1 als SQLite-Datei mit der Struktur aus DATENMODELL.md, Lesezugriff in der App, Bauskript (Textdateien → Datei), `nummern.csv`. Dazu etwa **12 Testpflanzen mit ausdrücklich gekennzeichneten Testtexten** (keine echten Inhalte, damit nichts Erfundenes als Wissen erscheint). In der App ein schlichter Bildschirm "Pflanzen (Test)" über die Gartenliste: Liste je Gruppe mit Anzahl, Test der Suche. Prüft, ob die mitgelieferte Datei auf dem Handy ankommt und gelesen wird. **Stand 03.10.2026: geliefert und getestet (10/10 OK).** Umgesetzt mit 14 Testpflanzen, `tools/baue_grunddaten.py`, `daten/` (Pflanzen als JSON, `quellen.json`, `gruppen.json`, `version.txt`, `nummern.csv`) und der Bildschirm "Pflanzen (Test)" (Schaltfläche in der Kopfzeile der Gartenliste). |
| 4b | **Datenbeschaffung auf deinem Rechner (kein App-Code):** Skripte holen Wikidata und Wikipedia für alle 539 Pflanzen, sortieren nach den Regeln (Überschriften, Auszug, etwa 1.000 Zeichen), schreiben die Textdateien und bauen die Datei. Dazu ein **Prüfbericht**: Anzahl gefüllter und leerer Abschnitte je Gruppe, Namensabweichungen, fehlende Artikel und eine **Stichprobenliste** (zum Beispiel 30 Pflanzen), die wir zusammen von Hand gegenlesen, um die Fehlerquote der Sortierung zu schätzen. Dauer des Abrufs: **Schätzung** 1 bis 2 Stunden bei einer Sekunde Pause zwischen den Abfragen. **Stand 03.10.2026: Skripte geliefert (`tools/hole_daten.py`, `tools/erzeuge_pflanzen.py`, `tools/pflanzenliste.py`), von Claude nur offline mit erfundenen Beispieldaten getestet; der echte Lauf steht aus.** Umsetzungsentscheidungen (Vorschlag): Überschrift muss zu genau einem Abschnitt passen, sonst weglassen; nur der eigene Text von Abschnitten mit passender Überschrift zählt (Unterabschnitte ohne Treffer werden nicht angehängt), Absätze über Chromosomenzahlen entfallen; Auszug nur aus ganzen Absätzen (Satzschnitt nur beim ersten Absatz); vorhandene echte Pflanzendateien werden nicht überschrieben (Handkorrekturen bleiben); Rohdaten (ganze Wikipedia-Artikel) bleiben lokal in `rohdaten/` und kommen nicht ins Repository. |
| 4c | **Piktogramme, Auswahl-Dialog und Info-Modal** (noch ohne Garten): die **13 Piktogramme** (aus `symbole-pflanzen.png`, nach deiner Freigabe) werden in der App gezeichnet und in der Liste gezeigt (Piktogramm und Initialen in der Gruppenfarbe), so siehst du sie früh.  "Pflanzenkatalog" in der Gartenliste. Suche, Gruppenfilter, Liste, Info-Modal mit acht Abschnitten, Quellen und Lizenzen, Merker für zugeklappte Abschnitte. Zugleich **"Über GartenManager"** mit Lizenzübersicht (Wikipedia, Wikidata, CC BY-SA 4.0). Die Daten sind die aus 4b. |
| 4d | **Pflanzen im Garten:** Datenbankversion 6 (alle Datei-2-Tabellen), Knopf "Pflanze", Platzieren, Kreis-Darstellung, Verschieben, Größe, Menü, Karte, Ebenenliste, Rückgängig, Garten duplizieren, Info-Modal aus Karte und Menü. **Größter Teilschritt**, ich schlage vor, ihn bei Bedarf in 4d1 (Anzeige, Platzieren, Bewegen) und 4d2 (Ebenenliste, Rückgängig, Duplizieren) zu teilen. |
| 4e | **Selbst ergänzen:** eigene Notizen zu Abschnitten ("Meine Notiz"), **Eigene Pflanze** anlegen, Ersetzt-durch-Logik beim Laden (veraltete Pflanzen). |
| 4f | **Feinschliff** nach deinen Tests. |

**Reihenfolge entschieden am 03.10.2026.** Begründung: 4a und 4b zuerst, weil alles andere auf der fertigen Datei aufbaut und die Datenqualität das größte Risiko ist. 4c vor 4d, weil du Auswahl und Info-Modal so **ohne** den komplizierten Gesten-Code testen kannst.

---

## 7. Was ich bewusst nicht in diesen Schritt packe

- **Vorschläge an die Prüfer einreichen** (Status "als Vorschlag eingereicht", Danke-Liste mit Formular): Das braucht einen Weg aus der App hinaus (Mail oder GitHub) und eine Entscheidung zu Datenschutz und Prüfern. PROJEKT.md, Abschnitt 6, beschreibt es. Der Status "nur privat" ist schon da, damit später nichts umgebaut werden muss. **Offen**, eigener Schritt nach Schritt 4.
- **Fotos** von Pflanzen (Lizenz je Bild, nicht geprüft).
- **Strukturierte Pflege für den Kalender**: Die Tabelle "Pflegehinweis" ist in Datei 1 und 2 angelegt, aber weitgehend leer (siehe Abschnitt 2). Befüllt und genutzt wird sie in Schritt 6.

---

## 8. Risiken und Unsicherheiten

- **Piktogramme:** Geschmackssache und Wiedererkennbarkeit. Strauch, Obstgehölz und Baum sehen ähnlich aus (Unterschied: Früchte beziehungsweise Größe). Bei sehr kleinen Kreisen sieht man kaum mehr als einen Fleck. Die Skizze ist ein Entwurf; die Umsetzung im Handy-Code sieht im Einzelnen etwas anders aus.
- **Datenqualität:** Die Sortierung nach Überschriften ist nur an 20 Pflanzen gezählt, nicht auf Fehler gemessen. Die Fehlerquote bei 539 Pflanzen kennen wir erst nach der Stichprobe in 4b. **Nicht geprüft.**
- **Viele leere Abschnitte:** Der erste Eindruck der Pflanzeninfos kann dünn wirken (Abschnitt 2). Ich sage das lieber jetzt als nach der Lieferung.
- **Namensabdeckung:** Wikidata hat bei Gartenpflanzen nicht immer alle umgangssprachlichen Namen. Die Suche findet nur, was in den Daten steht. Dein Listenname kommt immer dazu.
- **539 Pflanzen mit Wikipedia-Abruf:** Einzelne Artikel können umbenannt, geteilt oder weitergeleitet sein. Das Skript muss das melden, nicht verschweigen. Wie viele Artikel betroffen sind, wissen wir erst nach dem Lauf.
- **Mitgelieferte Datei ohne Room:** eigener, kleiner Lesecode, den ich nicht kompilieren kann. Das Risiko ist überschaubar (einfache Abfragen), aber es ist ein Bauteil mehr, das ich beim ersten Test erwarten muss, anzupassen.
- **Größe:** 539 Pflanzen mit Auszügen ergeben **Schätzung** wenige Megabyte. Nicht nachgerechnet.
- **F-Droid:** Ob CC BY-SA und ein Bauschritt mit Python akzeptiert werden, ist **nicht geprüft** (steht auch in PROJEKT.md, Abschnitt 8).
- **Gesten:** Pflanzen als dritter Elementtyp in Hit-Test, Reihenfolge, Ebenenliste und Rückgängig berühren viel bestehenden Code. Ich rechne mit Fehlern beim ersten Test von 4d.
- **Leistung:** Kreise sind billig zu zeichnen, aber Einrasten und Suche laufen über viele Einträge. Bei einigen hundert Pflanzen im Garten erwarte ich keine Probleme (Schätzung, ungetestet).
- **Lizenzen im Datenpaket:** Wikipedia-Auszüge CC BY-SA, Wikidata CC0, jede Zeile mit eigener Quelllizenz. Nicht juristisch geprüft; vor der Veröffentlichung sollten wir den Lizenztext für "Über GartenManager" noch einmal gemeinsam prüfen.

---

## 9. Rückfragen und Antworten (03.10.2026)

1. **Darstellung:** Piktogramm je Gruppe mit Initialen des umgangssprachlichen Namens, Farbe änderbar. **Entschieden.** Ausführung siehe 4.2.
2. **Standardgröße je Gruppe,** vom Nutzer änderbar. **Entschieden**, Schätzwerte reichen zum Anfang, Rückmeldung nach dem Probieren.
3. **Filter:** vorerst Suche und Gruppen, weitere später. **Entschieden.** Aufwand liegt in den Daten (Abschnitt 2).
4. **Technik:** Anforderung entschieden (Bestand erweiterbar, Nutzereinträge bleiben). Die Technikwahl liegt bei Claude (Abschnitt 3).
5. **Teilschritte und Reihenfolge:** **Entschieden.**
6. **4.1 Platzieren** wie beschrieben: **Entschieden.**

**Zusätzlich entschieden am 03.10.2026:**
- **Piktogramme** wie in `symbole-pflanzen.png`: "simpel, aber ausreichend". **Entschieden.**
- **Initialen-Regel** wie vorgeschlagen (4.2). **Entschieden**, Feinschliff am Gerät.
- **BfN-Antwort** eingearbeitet (PROJEKT.md, Abschnitt 4.2): keine FloraWeb-Inhalte in den Grunddaten. Für das Datenpaket gilt: jede Zeile behält ihre Quelllizenz (DATENMODELL.md, Abschnitt 5, Nachtrag).

**Offen:** nichts, was 4a blockiert.
