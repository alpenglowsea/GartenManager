# GartenManager: Entwurf des Datenmodells

Stand: 03.10.2026, Version 0.3 (Lizenznachtrag nach BfN-Antwort; Datenmodell sonst unverändert; Vorschlag: leere Tabelle "Merkmal" für spätere Filter, siehe PFLANZEN.md)

Vorher: Stand 29.09.2026, Version 0.2 (Fragen a bis c entschieden; Auszug-Regel als Vorschlag)

Dieses Dokument beschreibt, **was die App speichert und wie die Teile zusammenhängen**. Es enthält keinen Code. Es ist die Bauzeichnung der Datenbank: Wenn hier etwas falsch ist, ist es später teuer zu ändern. Deshalb prüfen wir es gemeinsam, bevor programmiert wird.

Kennzeichnung wie in PROJEKT.md: **Entschieden** (der Projektinhaber hat zugestimmt), **Vorschlag** (Claude schlägt vor), **Offen** (braucht Entscheidung), **Nicht geprüft** (Claude hat es nicht ausprobiert).

Begriffe: Eine **Tabelle** ist eine Liste mit festen Spalten, wie eine Excel-Tabelle. Eine **Datenbank** ist eine Datei, die mehrere Tabellen enthält. Eine **Nummer (ID)** ist eine feste, eindeutige Kennung für einen Eintrag, damit man ihn auch dann wiederfindet, wenn sich sein Name ändert.

---

## 1. Grundidee: zwei getrennte Datenbanken

**Vorschlag.** Vorgabe des Projektinhabers war: Grunddaten werden bei Updates ersetzt, eigene Einträge werden niemals überschrieben. Am sichersten ist es, das nicht nur per Regel, sondern **baulich** zu erzwingen: zwei getrennte Dateien.

| | Datei 1: Grunddaten | Datei 2: Meine Daten |
|---|---|---|
| Inhalt | Pflanzen, Namen, Gruppen, Infotexte, Quellen | Mein Garten, meine Notizen, meine eigenen Pflanzen, meine Pflege, Einstellungen |
| Woher | wird mit der App ausgeliefert | entsteht auf dem Handy des Nutzers |
| Bei App-Update | wird komplett durch die neue Fassung ersetzt | bleibt erhalten, wird nur bei Bedarf umgebaut |
| Backup (Export) | nicht enthalten, kommt mit der App | **das ist der Inhalt des Backups** |
| Ändert der Nutzer etwas? | nein, nur lesen | ja |

Vorteil: Ein Update kann eigene Einträge gar nicht überschreiben, weil sie in einer anderen Datei liegen. Ein Programmierfehler beim Import neuer Grunddaten kann den Garten des Nutzers nicht zerstören.

Nachteil: Die beiden Dateien können sich nicht mit festen Regeln gegenseitig absichern. "Meine Daten" verweist nur per Nummer auf die Grunddaten. Was passiert, wenn eine Pflanze im nächsten Update aus den Grunddaten verschwindet, regelt Abschnitt 6.

**Nicht geprüft:** Ob sich die Grunddaten-Datei mit der geplanten Programmbibliothek (Room) sauber bei jedem App-Update austauschen lässt. Ich weiß, dass Room fertige Datenbanken mitliefern kann. Dass sie bei einem Update zuverlässig durch die neue Fassung ersetzt wird, muss ich beim Bauen des ersten Prototyps testen. Falls das hakt, gibt es einen Ausweg (Grunddaten beim ersten Start jedes neuen Versionsstands neu einlesen). Am Modell ändert das nichts.

---

## 2. Datei 1: Grunddaten

### 2.1 Pflanze

Eine Zeile pro Pflanze, die der Nutzer in der Auswahl sieht (Beispiel: "Wald-Erdbeere").

| Spalte | Bedeutung | Beispiel |
|---|---|---|
| Nummer | feste Kennung, wird nie neu vergeben | 214 |
| Hauptname | deutscher Name, der angezeigt wird | Wald-Erdbeere |
| Lateinischer Name | wissenschaftlicher Name, **nicht eindeutig** | Fragaria vesca |
| Gattung | erstes Wort des lateinischen Namens (immer richtig, siehe Probelauf) | Fragaria |
| Wikidata-Nummer | Verweis auf Wikidata, kann fehlen | Q146684 |
| Wikipedia-Artikel | Titel in der deutschen Wikipedia, kann fehlen | Wald-Erdbeere |
| Ersetzt durch | nur bei veralteten Einträgen, siehe 6 | leer |

**Warum der lateinische Name nicht eindeutig ist:** Auf unserer Liste stehen mehrere Gemüse mit demselben lateinischen Namen (Kohl-Sorten wie Rotkohl und Kohlrabi teilen sich *Brassica oleracea*; ebenso Rote Bete und Mangold bei *Beta vulgaris*). Wenn der lateinische Name eindeutig sein müsste, könnten wir sie nicht getrennt führen. Eindeutig ist deshalb nur die **Nummer**.

Das war im Probelauf sichtbar: Bei Rosa und Cichorium intybus tauchen dieselben Namen zweimal auf, weil unsere Liste dort unterschiedliche Gartenformen führt. Das bleibt so erlaubt. Sie teilen sich dann dieselbe Wikipedia-Quelle.

### 2.2 Name

Alle Namen einer Pflanze. Die Suche durchsucht alle, angezeigt wird der Hauptname.

| Spalte | Bedeutung | Beispiel |
|---|---|---|
| Pflanze | Nummer der Pflanze | 214 |
| Name | der Text | Monatserdbeere |
| Art | Hauptname, umgangssprachlich, oder lateinisches Synonym | umgangssprachlich |
| Quelle | woher der Name stammt | Wikidata |

Regeln aus dem Probelauf:
- Namen kommen aus Wikidata-Bezeichnung, Wikidata-Aliasen und dem Wikipedia-Titel.
- Steht in der Wikidata-Bezeichnung der lateinische Name (bei Rosmarin, Apfel, Möhre), wird sie **nicht** als deutscher Name genommen. Dann gilt der Wikipedia-Titel, sonst unser Listenname.
- Lateinische Namen in der Alias-Liste (14 von 20) werden herausgefiltert und als "lateinisches Synonym" eingeordnet, nicht als deutscher Name.
- Unser Listenname ("Apfel") kommt immer als Name dazu, auch wenn er in den Quellen fehlt. Er ist vom Projektinhaber freigegeben.

### 2.3 Gruppe und Zuordnung

**Entschieden:** Eine Pflanze kann mehrere Gruppen haben.

- **Gruppe:** 13 feste Zeilen (Stauden, Sträucher, Gräser, Kletterpflanzen, Heckenpflanzen, Obstgehölze, Beerensträucher, Kräuter, Gemüsepflanzen, Blumen, Wasserpflanzen, Bäume, Zwiebel- und Knollenblumen).
- **Zuordnung Pflanze–Gruppe:** je eine Zeile pro Paar. Eine Spalte kennzeichnet die **Hauptgruppe** (in unserer Liste die Gruppe, in der die Pflanze steht; die anderen stehen unter "Auch in").

Die Filter im Auswahldialog benutzen diese Zuordnung. Wer nach "Kräuter" filtert, sieht auch Lavendel, obwohl seine Hauptgruppe Sträucher ist.

### 2.4 Angabe (Infotext pro Abschnitt)

Ein Text zu einem Abschnitt einer Pflanze, mit seiner Quelle.

| Spalte | Bedeutung | Beispiel |
|---|---|---|
| Pflanze | Nummer | 214 |
| Abschnitt | einer von: Herkunft, Standort, Wuchs und Laub, Blüte, Verwendung, Pflege | Blüte |
| Text | der eigentliche Text | (Auszug aus Wikipedia) |
| Quelle | Nummer aus der Tabelle Quelle | 31 |

**Entschieden:** Das Modal hat acht Abschnitte. Zwei davon sind keine Texte, sondern kommen aus anderen Tabellen:
- **Bezeichnung** = aus der Tabelle Name.
- **Gattung** = aus der Tabelle Pflanze.
- Die übrigen sechs sind Zeilen in der Tabelle Angabe.

**Fehlende Abschnitte** haben schlicht keine Zeile. Das Modal zeigt dann "Noch keine Angaben" und "Selbst ergänzen". So ist "leer lassen" der Normalfall, kein Sonderfall.

Ein Abschnitt darf **mehrere** Zeilen haben (verschiedene Quellen). Dann zeigt das Modal sie nacheinander mit ihrer jeweiligen Quelle.

### 2.5 Quelle

Für jede Angabe wird festgehalten, woher sie stammt. Das brauchen wir für die Lizenz (Wikipedia verlangt Namensnennung und Hinweis auf Änderungen) und für den Abschnitt "Quellen und Lizenzen" im Modal.

| Spalte | Bedeutung | Beispiel |
|---|---|---|
| Nummer | Kennung | 31 |
| Art | Wikipedia, Wikidata, geprüfter Nutzerbeitrag | Wikipedia |
| Titel | Name des Artikels | Wald-Erdbeere |
| Adresse | Link | https://de.wikipedia.org/wiki/Wald-Erdbeere |
| Version | Versionsnummer des Artikels zum Abrufzeitpunkt | 260660122 |
| Lizenz | Lizenztext | CC BY-SA 4.0 |
| Abrufdatum | wann abgerufen | 2026-09-29 |
| Änderungshinweis | was wir verändert haben | "Text gekürzt und einem Abschnitt zugeordnet" |

Die Versionsnummer war im Probelauf bei allen 20 Artikeln vorhanden. Damit lässt sich später nachvollziehen, welchen Stand wir übernommen haben.

### 2.6 Pflegehinweis (für den Kalender)

Der Pflegekalender braucht **strukturierte** Angaben. Ein Fließtext wie "Im Spätwinter wird zurückgeschnitten" lässt sich nicht automatisch in den Kalender eintragen. Deshalb eine eigene Tabelle:

| Spalte | Bedeutung | Beispiel |
|---|---|---|
| Pflanze | Nummer | 214 |
| Pflegeart | Schneiden, Düngen, Gießen, Aussäen, Pflanzen, Ernten, Überwintern, Sonstiges | Schneiden |
| Von Monat | Beginn des Zeitraums | 2 |
| Bis Monat | Ende des Zeitraums | 3 |
| Text | Kurzhinweis | (Text mit Quelle) |
| Quelle | Nummer | 45 |

Die Pflegearten stehen in einer eigenen kleinen Tabelle mit **Farbe** (Vorgabe aus dem Projekt: Pflegearten sind farblich unterscheidbar, auch im Kalender). Die genauen Farben legen wir später bei der Oberfläche fest.

**Wichtig und ehrlich:** Der Probelauf hat gezeigt, dass Wikipedia kaum verwertbare Pflegeangaben liefert. Diese Tabelle bleibt in der ersten Version daher **weitgehend leer**. Das war schon vorher klar (Ansatz A+B in PROJEKT.md): Sie füllt sich über Nutzerbeiträge und geprüfte Vorschläge. Sie muss trotzdem von Anfang an im Modell sein, weil sie später nur mit Aufwand nachzurüsten wäre.

---

## 3. Datei 2: Meine Daten

### 3.1 Eigene Angabe (Notiz zu einer Pflanze)

Was der Nutzer selbst zu einem Abschnitt einer Pflanze schreibt.

| Spalte | Bedeutung |
|---|---|
| Pflanze | Verweis auf eine Grundpflanze **oder** eine eigene Pflanze (3.2) |
| Abschnitt | wie in 2.4 |
| Text | Text des Nutzers |
| Status | nur privat, als Vorschlag eingereicht, übernommen |
| Erstellt am | Datum |

Wird ein Vorschlag später in die Grunddaten übernommen, **bleibt** die eigene Notiz bestehen und wird als "Meine Notiz" gezeigt (Entschieden). Der Status "übernommen" ist nur eine Information für den Nutzer.

### 3.2 Eigene Pflanze

Wenn eine Pflanze in den Grunddaten fehlt, legt der Nutzer sie selbst an (Vorgabe aus dem Projekt). Sie hat dieselben Spalten wie eine Grundpflanze (Name, lateinischer Name, Gruppen), liegt aber in Datei 2.

**Wichtig:** Ihre Nummern kommen aus einem **eigenen Nummernkreis**, damit sie nie mit Nummern aus den Grunddaten kollidieren, auch wenn spätere Updates viele neue Pflanzen bringen. Ein Verweis in "Meine Daten" besteht deshalb immer aus zwei Angaben: **Herkunft** (Grundpflanze oder eigene Pflanze) und **Nummer**.

Für eigene Pflanzen gibt es in Datei 2 auch eigene Namen, Gruppen-Zuordnungen und Pflegehinweise, mit derselben Struktur wie in Datei 1.

### 3.3 Eigener Pflegehinweis

Wie in 2.6, aber vom Nutzer. Zeigt sich im Kalender genauso, mit Kennzeichnung "eigen". Beim Anlegen kann er gleich mit der Pflanze verknüpft werden.

### 3.4 Garten (nur als Skizze)

Hier landen später die gezeichneten Flächen, Oberflächen, Gegenstände und die **platzierten Pflanzen**. Das Datenmodell dafür beschreiben wir in einem eigenen Dokument, wenn wir die Zeichenfunktion planen. Für dieses Dokument zählt nur eines: Eine platzierte Pflanze speichert ihren **Verweis** (Herkunft + Nummer), ihre Position und Größe, ihr Pflanzdatum und eine Notiz. Sie speichert **nicht** den Text der Pflanze und für die Anzeige auch nicht ihren Namen (nur eine Notfall-Kopie des Namens, siehe Abschnitt 6). Sonst würde ein Update der Grunddaten (Namenskorrektur) bei platzierten Pflanzen nicht ankommen.

### 3.5 Einstellungen und Merker

- Zugeklappte Abschnitte **pro Pflanzenart** (Entschieden): Pflanze + Abschnitt.
- Kalender-Einstellungen (welche Pflegearten sichtbar, ob mit dem Handy-Kalender synchronisiert wird).
- Danke-Liste: Angaben zum eigenen Namen für Einsendungen (freiwillig, siehe 5).

---

## 4. Wie die Grunddaten entstehen

**Vorschlag.** Die Grunddaten-Datei (SQLite) wird **nicht von Hand gebaut**, sondern automatisch erzeugt. Die Wahrheit liegt in gewöhnlichen **Textdateien im GitHub-Repository** (eine Datei pro Pflanze). Die App-Datei wird daraus gebaut.

Vorteile:
- Prüfer (z. B. aus Reddit r/garten) können Änderungen als Textvorschlag machen. Das braucht kein Wissen über Datenbanken.
- Jede Änderung ist im Verlauf sichtbar: wer, wann, was.
- Ein Fehler in einem Text lässt sich korrigieren, ohne die ganze Datenbank neu zu bauen.

Ablauf pro Pflanze:
1. Ausgang: unsere Pflanzenliste (Name, lateinischer Name, Gruppen).
2. Wikidata abfragen: Namen, Aliase, Wikipedia-Titel (wie im Probelauf). Optional zusätzlich die FloraWeb-Schnittstelle als reine Namensprüfung auf deinem Rechner für Wildpflanzen und eingebürgerte Arten (akzeptierter Name, Synonyme). Nach der BfN-Antwort vom 03.10.2026 übernehmen wir **keine** FloraWeb-Inhalte und speichern keine FloraWeb-Kennungen (sie können sich noch ändern).
3. Wikipedia-Artikel abrufen, nach Überschriften in Abschnitte zerlegen (Regeln in Abschnitt 4.1).
4. Ergebnis als Textdatei speichern, mit allen Quellenangaben.
5. Stichprobenweise Prüfung durch Menschen.
6. Aus allen Textdateien wird die Grunddaten-Datei gebaut und der App beigelegt.

**Nicht geprüft:** Ob GitHub Actions (der automatische Baudienst) die Abfragen bei Wikipedia ausführen darf. Aus der Sandbox, in der ich arbeite, komme ich nicht an die Server. Dein Rechner kam dran. Bis das geklärt ist, läuft Schritt 2 und 3 auf deinem Rechner, das Skript liefere ich.

### 4.1 Regeln zum Sortieren der Wikipedia-Texte

Grundsatz aus deinen Entscheidungen: **Lieber weglassen als falsch einordnen.** Es wird ein Wikipedia-Abschnitt nur dann übernommen, wenn seine **Überschrift** eindeutig zu einem unserer Abschnitte passt. Es werden keine einzelnen Sätze umsortiert.

| Unser Abschnitt | Übernommen wird ein Wikipedia-Abschnitt mit Überschrift, die enthält … |
|---|---|
| Wuchs und Laub | Beschreibung, Vegetative Merkmale, Erscheinungsbild, Habitus, Wuchs, Blatt, Laub, Nadeln |
| Blüte | Blüte, Blütenstand, Generative Merkmale |
| Herkunft | Vorkommen, Verbreitung, Herkunft, Ursprung |
| Standort | **nur** Überschriften mit "Standort" (Entschieden); "Ökologie" nicht |
| Verwendung | Nutzung, Verwendung, Küche |
| Pflege | **nur** Überschriften mit "Pflege" (Entschieden); "Anbau" nicht |

Bekannte Schwächen, aus dem Probelauf:
- In "Beschreibung" stehen bei manchen Artikeln Wuchs, Blüte und Blatt zusammen. Diesen Text übernehmen wir als Ganzes unter "Wuchs und Laub", er kann dort auch Blütenangaben enthalten. Das nehmen wir hin. Bei 16 von 19 Artikeln ist "Beschreibung" in Unterabschnitte gegliedert, dann sind die Blütenabschnitte getrennt.
- Die Regeln sind noch nicht auf Fehler gemessen. Ich weiß nur, dass sie bei Wuchs, Herkunft und Verwendung überall greifen. Wie oft sie falsch einordnen, wissen wir erst nach einer Stichprobe von Hand.
- Überschriften wie "Verwendung als Zierstrauch" oder "Nutzung des Holzes" können zu viel oder das Falsche einsammeln. Beides muss beim Bau der ersten Daten geprüft werden.

### 4.2 Auszüge statt ganzer Abschnitte

**Wunsch des Projektinhabers (Entschieden):** Lange Abschnitte werden als Auszug übernommen, weil das meiste für Anwender nicht relevant ist.

**Vorschlag für die Regel:**
- Der Auszug besteht aus **Originalsätzen** von Wikipedia, **nicht** aus von Claude umformulierten Texten. Das entspricht der Vorgabe, keine selbst formulierten Texte zu verwenden.
- Es werden ganze Absätze vom Anfang des Abschnitts übernommen, bis etwa **1.000 Zeichen** erreicht sind. Es wird an einem Absatzende geschnitten. Ist schon der erste Absatz länger, wird am Satzende geschnitten.
- Der Auszug ist als "Auszug" gekennzeichnet und verweist auf den vollständigen Wikipedia-Artikel (steht ohnehin unter Quellen). Der Änderungshinweis in der Quelle lautet dann "gekürzt".

**Warum diese Zahl (Schätzung):** In den 20 Probeartikeln war der Text pro Abschnitt im Mittel etwa 1.000 bis 1.600 Zeichen lang. Er lag bei Wuchs und Laub in 11 von 20 Fällen über 1.000 Zeichen, bei Herkunft in 16 von 20, bei Verwendung in 9 von 19. Der längste Verwendungstext hatte knapp 9.600 Zeichen. Die 1.000 Zeichen sind eine Schätzung, kein geprüfter Wert. Sie lassen sich später ändern, ohne die Datenstruktur anzufassen.

**Bekannte Schwäche:** Der Anfang eines Abschnitts ist nicht immer der nützlichste Teil. Ein Verwendungsabschnitt kann mit Geschichte beginnen statt mit Gartenverwendung. Eine Auswahl nach Relevanz (ebenfalls mit Originalsätzen) wäre besser, verlangt aber Urteilsvermögen und ist bei über 500 Pflanzen nur stichprobenhaft prüfbar. Das könnte eine spätere Verbesserung sein. **Offen:** ob wir mit der einfachen Regel starten. Ich empfehle das.

---

## 5. Herkunft, Lizenz und Danke-Liste

**Entschieden:** Das Datenpaket steht unter CC BY-SA 4.0, weil Wikipedia-Text darin steckt.

**Nachtrag 03.10.2026 (Hinweis des BfN, kein Rechtsrat, nicht juristisch geprüft):** Eine Lizenz wie CC BY-SA kann man nur für ein **eigenständiges neues Werk** vergeben. Teile, die wir unverändert weitergeben, behalten ihre **ursprüngliche** Lizenz. Für uns heißt das: Jede Zeile hat ihre Quelle samt Lizenz (Tabelle Quelle, 2.5). Wikipedia-Auszüge: CC BY-SA 4.0. Wikidata-Angaben: CC0. Unsere eigene Zusammenstellung und Sortierung: CC BY-SA 4.0. Käme jemals eine Quelle mit anderer Lizenz hinzu (zum Beispiel CC BY), behält sie diese, und der Abschnitt "Quellen und Lizenzen" muss es ausweisen. Der Text für "Über GartenManager" muss das so formulieren, nicht pauschal "alles CC BY-SA".

- Jede Zeile in Angabe und Name hat eine Quelle (2.5).
- Im Modal zeigt der nicht wegklappbare Abschnitt "Quellen und Lizenzen" für die jeweilige Pflanze: Artikelname, Link, Versionsnummer, Lizenz, Änderungshinweis.
- Unter Einstellungen → "Über GartenManager" steht die Gesamtübersicht (Lizenzen, Quellen, Projektlink).
- **Danke-Liste (Entschieden):** Sie hat eine eigene Tabelle in den Grunddaten: Pflanze, Abschnitt, freiwillig angegebener Name. Vollständig freiwillig; Name, Vorname oder Spitzname möglich; das Formular weist eindeutig auf Veröffentlichung und Freiwilligkeit hin.

---

## 6. Was passiert bei einem Update

Beim Update ersetzt die App Datei 1 komplett. Datei 2 bleibt. Damit "Meine Daten" nicht ins Leere zeigt, gelten diese Regeln:

1. **Nummern werden nie wiederverwendet und nie gelöscht.** Fällt eine Pflanze aus den Grunddaten weg, bleibt ihre Nummer mit der Markierung "veraltet" bestehen.
2. Eine veraltete Pflanze kann eine **"Ersetzt durch"-Nummer** haben (Beispiel: zwei Einträge werden zusammengelegt, oder eine Art wird umbenannt). Platzierte Pflanzen und Notizen wandern dann automatisch zum Nachfolger.
3. Ohne Nachfolger zeigt die App die platzierte Pflanze weiter an, mit dem zuletzt bekannten Namen, und weist darauf hin, dass sie in den Grunddaten nicht mehr geführt wird. **Es geht nichts verloren.**
4. Damit der letzte bekannte Name erhalten bleibt, speichert eine platzierte Pflanze zusätzlich eine Kopie ihres Namens als Notbehelf.

**Backup (Export/Import):** Der Export enthält Datei 2 und die Versionsnummer der Grunddaten, mit denen sie entstand. Beim Import auf einem anderen Handy oder nach einer Neuinstallation gelten dieselben Regeln wie bei einem Update: Nummern werden über "Ersetzt durch" auf die aktuelle Fassung gebracht.

**Nicht geprüft:** Das Verhalten beim Import einer Sicherung, die von einer **neueren** App-Version stammt als die installierte. Vorschlag: In diesem Fall verweigert die App den Import mit einem verständlichen Hinweis, statt Daten zu verstümmeln. Muss vor der Umsetzung noch bedacht werden.

---

## 7. Beispiel: die Wald-Erdbeere im Modell

- **Pflanze** 214: Hauptname "Wald-Erdbeere", *Fragaria vesca*, Gattung Fragaria, Wikidata Q146684.
- **Name:** "Wald-Erdbeere" (Hauptname), "Monatserdbeere" und "Walderdbeere" (umgangssprachlich, aus den Wikidata-Aliasen). "Fragaria vesca" steht ebenfalls als Alias, wird aber als lateinisch erkannt und aussortiert.
- **Gruppen:** Hauptgruppe Stauden. (Beerensträucher entfällt nach deiner Entscheidung.)
- **Angaben:**
  - Wuchs und Laub: aus Wikipedia-Abschnitt "Vegetative Merkmale".
  - Blüte: aus "Generative Merkmale".
  - Herkunft: aus "Vorkommen".
  - Verwendung: aus "Verwendung als Nahrungsmittel" und "Pflanzenheilkundliche Verwendung".
  - Standort: **leer**, weil keine Überschrift mit "Standort" existiert.
  - Pflege: **leer**, weil Wikipedia keinen Pflegeabschnitt hat.
- **Quellen:** Eine Quelle (Wikipedia, Wald-Erdbeere, Version 260660122, CC BY-SA 4.0), Änderungshinweis "gekürzt und Abschnitten zugeordnet".

Im Modal sähe der Nutzer sechs gefüllte Abschnitte (Bezeichnung, Gattung, Herkunft, Wuchs und Laub, Blüte, Verwendung), zwei leere (Standort, Pflege) mit "Selbst ergänzen" und unten die Quellen.

Ich habe die Zuordnung der Überschriften bei der Wald-Erdbeere anhand der Überschriftenliste aus dem Probelauf nachvollzogen. Den Text habe ich dabei nicht Zeile für Zeile geprüft.

---

## 8. Entschiedene Fragen (29.09.2026)

**a) Pflege: Entschieden.** Nur Wikipedia-Abschnitte mit "Pflege" im Titel werden übernommen. "Anbau" bleibt draußen, weil dort oft der Erwerbsanbau steht. Folge: Pflege ist bei den meisten Pflanzen leer, bis Anwender oder Prüfer ergänzen.

**b) Textlänge: Entschieden ist der Wunsch nach Auszügen; die genaue Regel ist Vorschlag.** Siehe 4.2.

**c) Gleicher lateinischer Name bei mehreren Gemüsen: Entschieden.** Jede Pflanze bleibt ein eigener Eintrag mit eigener Nummer und eigenem deutschen Namen. Sie teilen sich dieselbe Quelle (Beispiel: Rotkohl, Weißkohl, Kohlrabi teilen sich den Wikipedia-Artikel "Kohl"). Anwender können den Text bei ihrer Pflanze über "Meine Ergänzungen" anpassen. Bekannte Schwäche: Bei Rotkohl steht zunächst ein Text über Kohl allgemein.

**d) Nummernkreis für eigene Pflanzen:** eigener Nummernkreis, damit es nie zu Kollisionen kommt (braucht keine Entscheidung, nur festgehalten).

---

## 9. Was ich nicht geprüft habe (Übersicht)

- Ob Room die mitgelieferte Grunddaten-Datei bei jedem App-Update sauber austauscht (Abschnitt 1).
- Ob GitHub Actions bei Wikipedia und Wikidata abfragen darf (Abschnitt 4).
- Fehlerquote der Überschriften-Regeln (Abschnitt 4.1); der Probelauf hat nur gezählt, wo sie greifen.
- Import einer Sicherung aus einer neueren App-Version (Abschnitt 6).
- Größe der fertigen Grunddaten-Datei. **Schätzung:** 539 Pflanzen mit je einigen Kilobyte Text ergeben wenige Megabyte. Ich habe es nicht nachgerechnet, ein Problem für die App-Größe sehe ich aber nicht.
- Die Datenstruktur für den gezeichneten Garten (kommt in einem eigenen Dokument).

## Nachtrag 09.10.2026: Tabelle `bild` (Grunddaten, Datei 1)
`bild(pflanze_id PRIMARY KEY, datei, urheber, lizenz, lizenz_url, seite, titel, herkunft, abruf)`. Ein Bild je Pflanze; die Bilddatei liegt als WebP in `assets/bilder/<lateinischer-slug>.webp`, Pflanzen mit gleichem lateinischen Namen teilen sich eine Datei. Die Metadaten stehen in `daten/bilder.json` (erzeugt von `tools/baue_bilder.py`). Der persönliche Bildausschnitt (Mittelpunkt, Zoom) kommt in Stufe 5 in die eigene Datenbank (Datei 2).
