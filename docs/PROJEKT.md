# GartenManager – Projektdatei

Stand: 28.09.2026
Zweck dieser Datei: gemeinsames Gedächtnis von Projektinhaber und Claude. Claude verliert zwischen Gesprächen Details. Diese Datei wird zu Beginn jeder Sitzung gelesen und am Ende aktualisiert.

Kennzeichnung:
- **Entschieden** = Der Projektinhaber hat es ausdrücklich bestätigt.
- **Vorschlag** = Claude hat es vorgeschlagen, der Projektinhaber hat noch nicht ausdrücklich zugestimmt.
- **Offen** = noch nicht besprochen oder nicht entschieden.
- **Nicht geprüft** = Aussage von Claude aus Wissen oder Einschätzung, nicht nachgeprüft.

---

## 1. Produkt

Eine kostenlose, lokale Android-App zur Gartenverwaltung für Privatpersonen.

- Läuft nach der Installation vollständig offline, ohne Internetverbindung.
- Verteilung über GitHub und F-Droid.
- Kein Abo, keine Werbung, keine Tracker (Grundgedanke des Projektinhabers).
- Backup: manueller Export und Import in den Einstellungen.
- Sprache: erstmal nur Deutsch. **Entschieden**

### Funktionsumfang (Wunschliste des Projektinhabers)

1. Garten frei zeichnen, auch geschwungene Formen.
2. Oberflächen unterscheiden: Stein, Holz, Gras, Erde usw.
3. Gegenstände frei platzieren: Häuser, Terrassen, Garagen, Schuppen, PKW, Deko usw.
4. Pflanzen frei platzieren. Nach dem Anlegen öffnet sich ein Modal zur Pflanzenauswahl mit Filter und Suche.
5. Platzierte Gegenstände und Pflanzen später verschieben und in der Größe ändern.
6. Pflanzeninfos aus einer mitgelieferten Datenbank: Bezeichnung (lateinisch und umgangssprachlich), Gattung, Herkunft, Standort, Wuchs, Blüte, Laub, Verwendung, Pflege.
7. Fehlen Infos, können Nutzer selbst welche ergänzen.
8. Pflegekalender über ein Menü: Pflegehinweise zu geeigneten Zeiträumen für alle angelegten Pflanzen, filterbar, Pflegearten farblich unterschieden (auch im Kalender).
9. Optional: Sync des Pflegekalenders mit dem Handy-Kalender.
10. Pflanzen per Handykamera scannen. Die App schlägt Pflanzen mit Wahrscheinlichkeit in Prozent vor.

---

## 2. Rollen und Arbeitsweise

- Der Projektinhaber hat keine Programmierkenntnisse. Er bringt Ideen und entscheidet. Claude setzt um.
- Alle Befehle führt der Projektinhaber aus. Claude liefert Dateien und sagt genau, was zu starten ist.
- Der Projektinhaber arbeitet von zwei Rechnern: Windows 11 PC und Linux-Mint-Laptop. Git ist auf beiden installiert, ein GitHub-Konto ist vorhanden. **Entschieden**
- Claude erklärt in verständlichem Deutsch, warum etwas so gemacht wird. Gründlich statt schnell.
- Claude benennt Unsicherheiten, sagt was nicht geprüft wurde, gibt Fehler zu und kennzeichnet Schätzungen als Schätzungen.

### Ablauf pro Feature (Vorschlag)

1. Kurz besprechen, was das Feature können soll. Der Projektinhaber entscheidet.
2. Claude liefert Dateien und sagt, welche Befehle der Projektinhaber ausführt.
3. Der Projektinhaber installiert die App auf seinem Handy und testet.
4. Der Projektinhaber meldet zurück: Screenshots, Beschreibung, bei Abstürzen den Fehlertext.
5. Claude korrigiert. Wiederholen, bis es passt.

Claude kann die App vermutlich nicht selbst auf einem Android-Gerät testen. **Nicht geprüft**, ob Claude sie in der eigenen Arbeitsumgebung überhaupt bauen kann.

---

## 3. Technik

### Testgerät
OnePlus Nord 4, LineageOS 23.2, Android 16. **Entschieden**

### Technikvorschläge (Claude, noch nicht ausdrücklich bestätigt)
- **Entschieden:** Kotlin mit Jetpack Compose (nativ Android), Oberfläche nach Material Design.
- **Entschieden:** lokale Datenbank SQLite über Room.
- **Entschieden:** Git und GitHub als Versionsverwaltung, GitHub Actions baut die APK automatisch, damit der Projektinhaber sie nur herunterladen und installieren muss.
- **Entschieden:** F-Droid-tauglich bauen, unbedingt (keine proprietären Bibliotheken, keine Google-Dienste).
- **Nicht geprüft:** aktuelle Versionen der Bibliotheken und ob GitHub Actions für dieses Projekt wie geplant funktioniert. Beides wird beim Projektstart geprüft.

### Zeichnen und Skizze
(Geändert am 02.10.2026: Es kommt ein Maßstab mit Metern und Linealen, siehe GEGENSTAENDE.md.) Ursprünglich: Kein maßstabsgetreuer Plan mit Meterangaben. Die Übersicht hat den Charakter einer Skizze. Es geht darum, dass der Nutzer sieht, wo welche Pflanze steht, und sie über die Größe des Icons schnell zuordnen kann. **Entschieden**

### Lizenz
Eine freie Lizenz für den App-Code ist in Ordnung. **Entschieden**
Code-Lizenz: GPL-3.0. **Entschieden** (Projektinhaber, 29.09.2026; liegt im Repository als LICENSE).

---

## 4. Pflanzendatenbank

### 4.1 Grundsätze
- Ein kleiner, sauberer Grundstock der gängigsten Pflanzen in Deutschland. **Entschieden**
- Informationen kommen aus frei nutzbaren Quellen. Kein Abgreifen von Webseiten.
- **Claude formuliert keine eigenen Pflegetexte** (Fehlergefahr). **Entschieden**
- Claude sagt ehrlich, wenn die Informationen nicht ausreichen.
- Datenbank von Anfang an in drei getrennten Bereichen (**Entschieden**). Mit jedem Update können Nutzerergänzungen in die Grunddaten übernommen werden, eigene Einträge werden nie überschrieben:
  1. **Grunddaten:** mitgeliefert, werden bei jedem Update ersetzt.
  2. **Meine Ergänzungen:** privat, werden nie überschrieben. Eigene Einträge bleiben als "Meine Notiz" sichtbar, auch wenn sie später offiziell übernommen werden.
  3. **Herkunft jeder Angabe:** z. B. Wikipedia, Wikidata, geprüfter Nutzerbeitrag.

### 4.2 Datenquellen und Lizenzstatus

| Quelle | Status | Anmerkung |
|---|---|---|
| Wikidata | nutzbar | CC0 (frei). Namen, Gattung/Familie, deutsche Volksnamen. Abdeckung deutscher Namen bei Gartenpflanzen **nicht geprüft**. |
| Deutsche Wikipedia | nutzbar mit Pflichten | CC BY-SA 4.0. Pflanzendaten-Datei folgt dieser Lizenz (**Entschieden**). Quellenangabe pro Pflanze Pflicht, Verlinkung ist in Ordnung (**Entschieden**). Kein Kopieren kompletter Artikel, sondern Informationen in die passenden Abschnitte (**Entschieden**). |
| POWO / Kew | eingeschränkt | Gut für Gattung und Heimat, englisch, Lizenz pro Datensatz. Lizenz der Herkunftsdaten **nicht geprüft**. |
| OpenFarm | offen | CC0, aber abgeschaltet (April 2025). Geretteter Teil (ca. 340 Datensätze) evtl. für Gemüse/Kräuter-Pflege. Inhalt **nicht geprüft**. |
| FloraWeb (BfN) | Anfrage nötig | Alle Rechte vorbehalten, außer ausdrücklich frei (Verbreitungs- und Arealkarten). Anfrage ist abgeschickt (29.09.2026). Erste Antwort des BfN: Interesse an dem Projekt, Angebot eines Telefonats, Hinweis auf eine neue Schnittstelle (API), die noch nicht produktiv läuft, aber für einmalige Downloads der "frei lizenzierten taxonomischen Referenz" geeignet sei. Die Lizenzfrage ist damit **noch nicht schriftlich beantwortet**. Ohne endgültige Antwort planen wir ohne FloraWeb-Inhalte über die Namensliste hinaus. |
| NaturaDB | gestrichen | Kommerzieller Betreiber (Maseto GmbH). Vom Projektinhaber verworfen, keine Anfrage. |
| Trefle | nicht nutzbar | Datenlizenz ungeklärt. |
| BiolFlor | nicht nutzbar | Nicht frei. |

**FloraWeb-Schnittstelle (geprüft am 29.09.2026, anhand der öffentlichen Beschreibung auf floraweb.de; die eigentliche Dokumentationsseite ist für Claudes Abrufwerkzeug gesperrt und wurde nicht gelesen):** Es gibt zwei Funktionen (namebyid, taxonbyid). Sie liefern wissenschaftliche Namen mit Autoren, akzeptierte Namen und Synonyme, Kennungen und untergeordnete Taxa, auf Grundlage der Florenliste 2018 (Gefäßpflanzen Deutschlands, etablierte Arten, dazu unbeständige). Verbreitungsdaten gibt es dort noch nicht. **Nicht enthalten:** Standort, Blütezeit, Wuchs, Pflege. Ob deutsche Volksnamen enthalten sind, ist nicht erkennbar. Auf den gelesenen Seiten steht keine Lizenzangabe; die Aussage "frei lizenziert" stammt aus der E-Mail. **Nutzen für uns:** Namensprüfung (akzeptierter Name, Synonyme) für Wildpflanzen und eingebürgerte Arten. Die meisten reinen Gartenpflanzen (z. B. Tomate, Tulpe) sind vermutlich nicht enthalten (**nicht geprüft**).

Wichtig bei FloraWeb: Falls das BfN nur "für Naturschutzzwecke" zustimmt, ist das für F-Droid vermutlich zu eng. **Nicht geprüft.**

### 4.3 Qualität der Quellen (Stichprobe)
Getestet an fünf Wikipedia-Artikeln (Lavendel, Forsythie, Gartenhortensie, Kirschlorbeer, Rosen). Die Stichprobe ist sehr klein.

- Gut: Name, Gattung, Herkunft, Wuchs, Laub.
- Mittel: Blüte (oft nur "Frühjahr"), Verwendung (eher botanisch als gärtnerisch).
- Lückenhaft: Standort.
- Schlecht: Pflege. Schätzung: bei etwa jeder fünften Gartenpflanze brauchbare Pflegehinweise.
- Die Angaben stehen als Fließtext. Das Sortieren in Abschnitte geschieht halbautomatisch mit Stichwortregeln, die im Zweifel lieber weglassen als falsch einsortieren. Fehlerquote wird im Probelauf gemessen.

### 4.4 Umgang mit Pflegehinweisen
Weg A und B kombiniert. **Entschieden**
- **A:** Pflege bleibt leer, wo keine belegte Angabe vorliegt. Nutzer tragen selbst ein. Der Pflegekalender stützt sich in der ersten Version auf eigene Einträge.
- **B:** gemeinschaftlich gepflegte Pflegedaten (siehe Abschnitt 6).
- Weg C (Erlaubnis bei Institutionen, z. B. Gartenakademien) und Weg D (Claude formuliert, Mensch prüft) sind aktuell zurückgestellt. Weg D nur mit menschlicher Prüfung.

### 4.5 Pflanzenliste
- Ziel: deutlich mehr als 150. Grobe Schätzung Claude: 800 bis 1.200 Pflanzen. Endgültige Zahl hängt von den vorhandenen Daten ab. **Offen**
- **13 Gruppen. Entschieden:**
  Stauden, Sträucher, Gräser, Kletterpflanzen, Heckenpflanzen, Obstgehölze, Beerensträucher, Kräuter, Gemüsepflanzen, Blumen, Wasserpflanzen, **Bäume**, **Zwiebel- und Knollenblumen**.
- Es werden Arten gelistet, keine Sorten (Sorten würden den Rahmen sprengen). **Entschieden**
- Die Gruppe darf aus den Quellen abgeleitet werden; alle Einträge von Hand zu prüfen ist zeitlich nicht machbar. **Entschieden** (Folge: einzelne Fehleinordnungen sind möglich und werden bei Hinweisen korrigiert).
- **Mehrere Gruppen pro Pflanze** erlauben (z. B. Himbeere: Beerenstrauch und Obstgehölz). **Entschieden** (Projektinhaber: ja).
- Als Maß für "gängig" könnten Abrufzahlen der Wikipedia-Artikel dienen. **Idee, nicht geprüft.**
- Die Liste enthält nur Namen, keine Fachangaben. Der Projektinhaber streicht und ergänzt.
- Erste Runde der Liste liegt vor (Pflanzenliste.md): 539 Einträge in 13 Gruppen, nur Arten. Lateinische Namen aus Claudes Wissen, **nicht geprüft** (Abgleich mit Wikidata im Probelauf). Nächster Schritt: Der Projektinhaber streicht und ergänzt.

---

### 4.6 Ergebnis des Probelaufs (29.09.2026, 20 Pflanzen)

Abfrage von Wikidata und deutscher Wikipedia durch den Rechner des Projektinhabers (Skript probelauf.py). Alle Zahlen sind Zählungen an 20 Pflanzen; Bewertungen der Inhalte sind Claudes Einschätzung, kein Beweis.

- **Lateinische Namen:** 20 von 20 in Wikidata mit genau diesem Namen gefunden (auch umbenannte Art, auch Unterart). Das zeigt nur, dass der Name dort existiert, nicht dass er der aktuell gültige ist.
- **Deutsche Wikipedia-Artikel:** 20 von 20 vorhanden, jeweils mit Versionsnummer (wichtig für die Quellenangabe nach CC BY-SA).
- **Deutscher Name:** Wikidata-Bezeichnung ist bei 17 von 20 brauchbar. Bei Rosmarin, Apfel und Möhre steht dort der lateinische Name. Der von uns gelistete Name kommt bei 19 von 20 in Bezeichnung, Aliasen oder Artikeltitel vor (Ausnahme: "Apfel", Wikipedia sagt "Kulturapfel"). Aliase enthalten viele umgangssprachliche Namen (Tomate: Paradeiser, Liebesapfel), aber bei 14 von 20 auch den lateinischen Namen, der herausgefiltert werden muss.
- **Gattung:** Der "Elternteil" in Wikidata ist nicht immer die Gattung (Möhre: eine Art; Hunds-Rose: vermutlich eine Untergruppe). Regel: Gattung = erstes Wort des lateinischen Namens.
- **Herkunft/Verbreitung:** Wikidata-Angabe bei 16 von 20 vorhanden (Inhalt nicht geprüft); Wikipedia-Abschnitt zu Vorkommen bei 20 von 20.
- **Abschnitte aus Wikipedia-Überschriften:** Wuchs, Herkunft und Verwendung 20/20; Blüte 17/20; Bezeichnung (Namensherkunft, Trivialnamen) 10/20; Laub nur 6/20 mit eigener Überschrift; "Standort" nur 5/20 (die Überschrift "Ökologie" meint meist Bestäubung und Tierwelt, nicht Standortansprüche).
- **Aufteilung von "Beschreibung":** 16 von 19 Artikeln haben Unterabschnitte, drei nicht (Gartenhortensie, Gewöhnliche Waldrebe, Chinaschilf). Wuchs und Laub stehen oft im selben Unterabschnitt ("Vegetative Merkmale").
- **Pflege:** "Anbau" in Wikipedia beschreibt oft den Erwerbsanbau (Schnittlauch, Lavendel, Tomate, Möhre, Rhabarber) und ist keine Gartenpflege. Brauchbare Gartenpflege fand Claude bei etwa 3 bis 5 von 20 (Gartenhortensie, Rosmarin, Efeu, teilweise Himbeere). Das passt zur früheren Stichprobe (etwa 1 von 5). Die Prüfung stützt sich auf die ersten Absätze der Abschnitte.
- **Entschieden (Projektinhaber, 29.09.2026):** (1) Wuchs und Laub werden ein gemeinsamer Abschnitt, weil sich beides in den Quellen nicht sicher trennen lässt. (2) Standort bleibt ein eigener Abschnitt. Übernommen werden nur Wikipedia-Abschnitte mit "Standort" im Titel; sonst bleibt er leer (bis Nutzer oder Prüfer ergänzen). Leer lassen ist besser als falsch einordnen.
- **Entschieden (29.09.2026):** Pflege wird nur aus Wikipedia-Abschnitten mit "Pflege" im Titel übernommen; "Anbau" bleibt draußen. Texte werden als **Auszug** übernommen (Originalwortlaut, gekürzt, gekennzeichnet); Details in DATENMODELL.md, Abschnitt 4.2. Gemüse mit gleichem lateinischen Namen (z. B. Kohlsorten) sind eigene Einträge, die sich dieselbe Quelle teilen; Anwender können sie anpassen.
- **Nicht gemessen:** Fehlerquote der automatischen Sortierung (dafür wäre ein Vergleich mit von Hand sortierten Texten nötig); Bildlizenzen; Herkunftsdaten anderer Quellen.

## 5. Pflanzeninfo-Modal (Oberfläche)

Wird geöffnet, wenn der Nutzer eine Pflanze im Platzierungsdialog auswählt oder auf eine bereits platzierte Pflanze tippt. **Entschieden**

- Alle verfügbaren Infos stehen in einem Modal, geordnet in Abschnitte (aufklappbare Liste, "Akkordeon"):
  Bezeichnung, Gattung, Herkunft, Standort, Wuchs und Laub, Blüte, Verwendung, Pflege (acht Abschnitte; Wuchs und Laub zusammengelegt, entschieden 29.09.2026).
- Ein Klick auf die Überschrift klappt den Abschnitt zu oder wieder auf.
- Beim Öffnen sind alle Abschnitte aufgeklappt.
- Zugeklappte Abschnitte werden **pro Pflanzenart** gemerkt (nicht pro einzelnem Exemplar und nicht artenübergreifend).
- Der Abschnitt **"Quellen und Lizenzen"** steht immer ganz unten und lässt sich nicht wegklappen (Lizenzpflicht von Wikipedia).
- Abschnitte ohne Daten zeigen "Noch keine Angaben" mit Schaltfläche "Selbst ergänzen".

---

## 6. Wissen der Nutzer zu allen bringen

Die App läuft offline. Wissen fließt deshalb nur über einen bewusst gebauten Umweg. **Entschieden**

1. Der Nutzer trägt etwas ein. Es bleibt zunächst privat auf seinem Handy und ist im Backup enthalten.
2. Er tippt auf "Als Vorschlag für alle senden". Die App öffnet Browser oder Mailprogramm mit einem vorausgefüllten Text. Die App braucht dafür **keine eigene Internetberechtigung** (**Nicht geprüft**).
3. Eingang: GitHub-Formular (Felder entsprechen den acht Abschnitten) und zusätzlich E-Mail für alle ohne GitHub-Konto. Vorschläge werden von Menschen geprüft und dann in die Datenbank aufgenommen.
4. Alle bekommen die Angaben mit dem nächsten Update über GitHub oder F-Droid. Bei F-Droid dauert das nach Schätzung von Claude Tage bis wenige Wochen.

Regeln:
- Der Einsender bestätigt, dass er den Text selbst geschrieben hat und freigibt (verhindert kopierte Ratgeber-Texte).
- **Prüfer werden gebraucht:** sachkundige Gartenfreunde (Gartenvereine, NABU-Gruppen, Foren). Das ist Gemeinschaftsarbeit des Projektinhabers, keine Technik. **Offen**
- Erfahrungsgemäß beteiligt sich nur ein kleiner Teil der Nutzer (Schätzung). Beitragen muss sehr einfach sein.

### Danke-Liste (Beitragende)
**Entschieden.** Zwei Orte:
1. Pro Pflanze im Abschnitt "Quellen", z. B. "Pflegehinweis ergänzt von [Name]".
2. Gesammelt unter Einstellungen → "Über GartenManager" (zusammen mit Lizenzen, Quellen, Projektlink).

Datenschutzregeln (**Entschieden**, Claude ist kein Anwalt):
- Vollkommen freiwillig: Feld "Wie soll ich genannt werden?". Name, Vorname oder Spitzname möglich, oder leer. Eindeutiger Hinweis auf Veröffentlichung und Freiwilligkeit im Formular.
- Nur übernehmen, was jemand selbst angibt. Keine E-Mail-Adressen oder Klarnamen aus Absenderzeilen.
- Im Formular ehrlich dazuschreiben: Ein einmal veröffentlichter Name bleibt in alten Installationen, entfernt wird er erst mit dem nächsten Update.

Nicht empfohlen: direkter Austausch zwischen Handys (Bluetooth/QR), eigener Server. Möglich später: freiwilliges Herunterladen neuer Daten (bräuchte Internetberechtigung).

---

## 7. Vorgeschlagene Reihenfolge der Umsetzung (grobe Schätzung)

1. ~~Projektgerüst, automatischer Build, "Hallo Garten" auf dem Handy des Projektinhabers~~ **erledigt am 29.09.2026** (Build auf GitHub Actions grün, APK auf dem Handy installiert und gestartet)
2. Gartenplan: Flächen zeichnen, Oberflächen, Zoom. Konzept: GARTENPLAN.md (Entschieden: Punkte setzen mit automatischer Rundung, Ecke oder rund pro Punkt, zuletzt gezeichnete Fläche oben mit änderbarer Reihenfolge, Flächen mit Namen, Rückgängig, mehrere Gärten, optionale Grundstücke; Teilschritte 2a bis 2e sind Vorschlag)
3. Objekte platzieren, verschieben, skalieren, drehen. Konzept: GEGENSTAENDE.md (Version 0.6: Maßstab, Ebenenliste, Farbauswahl auch für Flächen, freie Gebäude als Gegenstand; Teilschritte 3a bis 3d)
4. Pflanzen und Datenbank, Auswahl-Dialog mit Suche und Filter, Info-Modal
5. Backup Export/Import (früh, weil es die Datenstruktur absichert)
6. Pflegekalender mit Filter und Farben
7. Kalender-Sync
8. F-Droid-Vorbereitung
9. Pflanzenerkennung (nur wenn machbar)

Dauer: nicht seriös schätzbar. Bis zu einer nutzbaren Version (Schritte 1 bis 6) sind viele Sitzungen nötig.

---

## 8. Risiken und Unsicherheiten

- **Pflanzenerkennung per Kamera:** muss offline auf dem Handy laufen, braucht ein KI-Modell in der App (**Nicht geprüft**: Größe, Trefferquote bei Gartenpflanzen, ob ein frei lizenziertes Modell existiert). Bequeme Lösungen (Google ML Kit, Firebase, Online-Dienste) widersprechen "offline" oder den F-Droid-Regeln. Die Prozentangaben sind Sicherheitswerte des Modells, keine echten Wahrscheinlichkeiten und dürfen nicht überverkauft werden. Empfehlung: zuletzt bauen.
- **F-Droid-Regeln:** Alles muss freie Software sein (Bibliotheken, Icons, Schriften, Daten). Ob F-Droid unsere CC-BY-SA-Daten und die Danke-Liste akzeptiert: **Nicht geprüft.**
- **Signierschlüssel** für die GitHub-Version: geht er verloren, können bestehende Nutzer nicht mehr updaten. Muss sicher aufbewahrt werden. **Offen**, wie.
- **Grafiken** (Häuser, Autos, Deko, Bodenmuster): frei lizenziert oder von Claude als einfache Vektorzeichnungen. Werden schlicht aussehen. Pflanzenbilder: jedes Wikipedia-Bild hat eine eigene Lizenz, **Nicht geprüft**, aufwendig. Schlichte Icons wären einfacher.
- **Zeichnen mit vielen Objekten:** Darstellung muss flüssig bleiben. Kann Feinarbeit brauchen.
- **Kalender-Sync:** über Android machbar, braucht Berechtigung. Wiederkehrende Termine sind fummeliger als sie klingen.
- **Backup:** alte Backups müssen nach Updates lesbar bleiben. Versionsnummer im Backup von Anfang an einplanen.
- **Zugriff auf Wikipedia-Daten:** Unklar, ob Claudes Arbeitsumgebung sie abrufen kann. Wahrscheinlich läuft der Datenabruf später auf dem PC des Projektinhabers.

---

## 9. Offene Entscheidungen und nächste Schritte

Offen für den Projektinhaber:
- [x] BfN-Anfrage abgeschickt (29.09.2026, an floraweb@bfn.de). [ ] Antwort abwarten. Planung läuft ohne Antwort weiter.
- [x] Mehrere Gruppen pro Pflanze: ja (entschieden 28.09.2026).
- [ ] Prüfer für Nutzerbeiträge finden (Gartenvereine, NABU, Foren). Der Projektinhaber kümmert sich später darum, voraussichtlich über Reddit r/garten.
- [x] Lizenz für den Code: GPL-3.0 bestätigt.
- [x] Mindest-Android-Version: Android 8.0 (API 26), entschieden 29.09.2026. Gründe: Benachrichtigungskanäle für Pflege-Erinnerungen, moderne App-Symbole, weniger Sonderfälle. Der Geräteanteil von Android 8+ wurde nicht geprüft (Schätzung: deutlich über 90 %).

Als Nächstes:
1. ~~Pflanzenliste durchsehen~~ erledigt: Der Projektinhaber arbeitet mit dem Umfang von 539 Einträgen weiter.
2. ~~Probelauf~~ erledigt (Abschnitt 4.6).
3. Datenmodell (DATENMODELL.md, Version 0.2): Fragen a bis c entschieden, Auszug-Regel als Vorschlag. Danach technisches Grundgerüst.
4. ~~Repository anlegen und diese Datei einchecken~~ erledigt am 29.09.2026 (Ordner docs/).

---

## 10. Änderungsprotokoll

- 28.09.2026: Projektdatei angelegt. Diskussion zu Sprache, Technik, Datenquellen, Pflege-Hinweisen, Modal-Oberfläche, Nutzerbeiträgen, Danke-Liste besprochen und festgehalten.
- 28.09.2026: Mehrere Gruppen pro Pflanze entschieden (ja). Pflanzenliste, erste Runde, angelegt (539 Einträge, Latein ungeprüft).
- 29.09.2026: Erdbeeren (Garten-, Wald-Erdbeere) von Beerensträucher zu Stauden verschoben; Rhabarber bleibt Gemüsepflanzen + Stauden (keine eigene Gruppe für Botanik-Familien). Repository steht: https://github.com/alpenglowsea/GartenManager (öffentlich, enthält README, LICENSE GPL-3.0, .gitignore). BfN-Anfrage wird nach dem ersten Push gesendet.
- 29.09.2026: Der Projektinhaber entscheidet: Kotlin + Jetpack Compose (Material Design), Room/SQLite, Git/GitHub, F-Droid-tauglich, drei Datenbereiche, Arten statt Sorten, Gruppe aus Quellen ableitbar, Danke-Liste freiwillig mit Hinweis. Entwicklungsrechner: Windows 11; Repository noch nicht geklont.
- 29.09.2026: Erster Push nach GitHub erfolgt (docs/PROJEKT.md, docs/Pflanzenliste.md). GPL-3.0 bestätigt, Mindest-Android 8.0 (API 26) entschieden, BfN-Anfrage abgeschickt. Hinweis: GitHub verlangt die No-Reply-Mailadresse in Commits (Schutz der privaten Adresse).
- 29.09.2026: Probelauf mit 20 Pflanzen ausgewertet (Abschnitt 4.6).
- 29.09.2026: Entschieden: Wuchs und Laub ein gemeinsamer Abschnitt; Standort nur aus Quellen mit Standort-Überschrift, sonst leer. Pflanzenliste (539 Einträge) gilt als Arbeitsstand. Probelauf-Version der PROJEKT.md ist gepusht.
- 29.09.2026: DATENMODELL.md (Version 0.1, Entwurf) angelegt: zwei getrennte Datenbanken (Grunddaten / Meine Daten), Tabellen, Update-Regeln, Sortierregeln. Status: Vorschlag.
- 29.09.2026: Entschieden: Pflege nur aus "Pflege"-Überschriften; Texte als Auszug; Kohlsorten als eigene Einträge mit geteilter Quelle. Erste Antwort des BfN (Telefonangebot, API nur für Taxonomie). Persönlicher Name in den Projektdateien durch "Projektinhaber" ersetzt, da das Repository öffentlich ist.
- 29.09.2026: Repository neu angelegt (alter Verlauf enthielt persönlichen Namen), Dokumente dort abgelegt. Antwort ans BfN geschickt: bitte schriftlich weiterkommunizieren. Technisches Grundgerüst (gartenmanager-geruest.zip) als **Vorschlag** erstellt, noch **nicht gebaut**: Anwendungskennung io.github.alpenglowsea.gartenmanager (später nicht mehr änderbar, sobald veröffentlicht), Android 8 (API 26), Kotlin 2.0.21, Compose BOM 2024.12.01, AGP 8.7.3, Gradle 8.9, keine Berechtigungen, Sicherung durch Android abgeschaltet (eigenes Backup geplant), Test-Signatur öffentlich im Repo (nur für Entwicklungs-Builds).
- 29.09.2026: Schritt 1 der Umsetzung erledigt: Grundgerüst baut auf GitHub Actions ohne Änderungen, App läuft auf dem Handy des Projektinhabers. Hinweis: gradle-wrapper.jar und debug.keystore mussten in der .gitignore per Ausnahme freigegeben werden. Nächster Schritt: Gartenplan (Umsetzungsschritt 2), zuerst als Konzeptgespräch.
- 29.09.2026: Gartenplan besprochen. Entschieden: Punkte setzen, App rundet ab; zuletzt gezeichnete Fläche liegt oben, Reihenfolge änderbar; mehrere Gärten von Anfang an (auch getrennte Gärten auf einem Grundstück). GARTENPLAN.md (Version 0.1) mit Bedienung, Datenmodell und Teilschritten 2a bis 2e als Vorschlag angelegt.
- 29.09.2026: GARTENPLAN.md auf Version 0.2: Ecken pro Punkt, Flächennamen (kein eigener Typ Beet), Rückgängig, Oberflächenliste bestätigt; optionale Ebene Grundstück (gruppiert mehrere Gärten, nie aufgedrängt) entschieden.
- 29.09.2026: GARTENPLAN.md Version 0.3: Oberflächenliste, Rückgängig (letzte Schritte zurücknehmbar) und Kurvenverlauf durch die gesetzten Punkte als entschieden markiert.
- 29.09.2026: GARTENPLAN.md Version 0.4: Entschieden: Ein Garten öffnet standardmäßig in der Ansicht (nur Betrachten, Flächen und später Pflanzen antippen, Infos lesen). Bearbeiten ist ein eigener Modus, erreichbar aus der Ansicht oder über das Menü in der Gartenliste. Kennzeichnung des Modus, Sofort-Speichern und Verlauf-Ende beim Verlassen sind Vorschlag.
- 29.09.2026: GARTENPLAN.md Version 0.5: Entschieden: Bearbeitungsmodus wird deutlich gekennzeichnet; Änderungen werden erst mit "Speichern" übernommen (Verwerfen setzt alles seit dem Betreten zurück), damit Nutzer Änderungen durchspielen können; Rückgängig-Verlauf endet beim Verlassen des Modus. Vorschlag: Zwischenspeicher "Entwurf" gegen Verlust bei App-Abbruch.
- 29.09.2026: GARTENPLAN.md Version 0.6: Entschieden (ersetzt Version 0.5): Änderungen im Bearbeitungsmodus werden sofort gespeichert, kein Speichern-/Verwerfen-Knopf, kein Entwurf. Wer ausprobieren will, dupliziert den Garten über das Menü der Gartenliste. Der Knopf zum Schließen einer gezeichneten Fläche heißt "Fläche abschließen".
- 29.09.2026: Teilschritt 2a (Gartenliste mit optionalen Grundstücken, Garten anlegen/umbenennen/verschieben/duplizieren/löschen, erste Room-Datenbank "Meine Daten", Platzhalter für den geöffneten Garten mit Ansicht/Bearbeiten-Umschalter) als **Vorschlag geliefert, noch nicht gebaut und nicht getestet**. Neue Bibliotheken: Room 2.6.1, KSP 2.0.21-1.0.25, Lifecycle 2.8.7 (Versionen aus meinem Sandkasten nicht prüfbar). Die Datenbank löscht sich im Entwicklungsmodus bei Schemaänderungen selbst (fallbackToDestructiveMigration); **vor der ersten öffentlichen Version durch echte Migrationen ersetzen**. Dialoge und aufgeklappte Grundstücke überstehen keine Bildschirmdrehung.
- 29.09.2026: Teilschritt 2a gebaut (erster Build grün) und am Handy getestet: Gartenliste, Grundstücke, Umbenennen, Verschieben, Duplizieren, Löschen und Speichern funktionieren.
- 29.09.2026: Teilschritt 2b als **Vorschlag geliefert, noch nicht gebaut**: geöffneter Garten zeigt eine Zeichenfläche mit Hilfsgitter und Nullpunkt-Kreuz; Zoomen und Verschieben per Touch (Zoom 0,1 bis 10), Knopf "Ansicht zurücksetzen", letzte Ansicht wird pro Garten gespeichert (ohne "zuletzt geändert" zu ändern). Bearbeitungsmodus zusätzlich mit farbigem Rahmen. Keine Änderung am Datenbankaufbau, vorhandene Gärten bleiben erhalten. Offen: In 2b verschiebt schon ein Finger die Ansicht; sobald im Bearbeitungsmodus gezeichnet wird (2c), soll dort nur noch mit zwei Fingern verschoben werden.
- 29.09.2026: Teilschritt 2b gebaut (grün) und am Handy getestet: Zoomen und Verschieben fühlen sich organisch und flüssig an, das Hilfsgitter ist erwünscht, die Ansicht wird pro Garten gemerkt.
- 29.09.2026: Teilschritt 2c als **Vorschlag geliefert, noch nicht gebaut**: Im Bearbeitungsmodus Flächen zeichnen (Werkzeugleiste unten: Fläche zeichnen, Rückgängig; beim Zeichnen: Abbrechen, Rückgängig für den letzten Punkt, "Alle Punkte eckig/rund", Fläche abschließen ab drei Punkten). Kurve läuft durch die Punkte (Catmull-Rom, Führung auf 40 % der Teilstücklänge begrenzt gegen Überschießen). Im Bearbeitungsmodus verschiebt und zoomt man nur noch mit zwei Fingern, ein Finger setzt Punkte. Neue Tabellen Fläche und Punkt (Datenbankversion 2), Duplizieren kopiert Flächen und Punkte mit. Rückgängig außerhalb des Zeichnens nimmt die zuletzt angelegte Fläche zurück (voller Verlauf mit 2e). Alle Flächen sind vorerst gleich grün gefüllt (Oberflächen mit 2d). Einmaliger Datenverlust der Testgärten wegen Datenbankumbau (Entwicklungsmodus).
- 30.09.2026: Teilschritt 2c gebaut (grün) und am Handy getestet: Flächen zeichnen (rund und eckig), Abschließen ab drei Punkten, Abbrechen, Rückgängig, Reihenfolge, Speichern, Duplizieren, Zoomen beim Bearbeiten: alles in Ordnung. Kurven sehen gut aus (keine unschönen Beulen), Knöpfe gut erreichbar. Wünsche nach dem Test: Hilfe für gerade Linien und rechte Winkel, Schließen nur per Tipp auf den ersten Punkt, vorgegebene geometrische Formen (zum Einzeichnen der Grundstücksgrenzen), deren Punkte sich später ziehen lassen.
- 30.09.2026: GARTENPLAN.md Version 0.7. Nachbesserung 2c als **Vorschlag geliefert, noch nicht gebaut**: Punkt wird beim Loslassen gesetzt (mit Vorschau), Fläche schließt per Tipp auf den ersten Punkt (Knopf "Fläche abschließen" entfällt), Ausrichtungshilfe mit gestrichelten Hilfslinien (waagerecht, senkrecht, Verlängerung, rechter Winkel, Schnittpunkt; abschaltbar), Formen Rechteck, Quadrat, Kreis, Ellipse, Dreieck, Fünfeck, Sechseck, Achteck (aufziehen von Ecke zu Ecke). Keine Änderung am Datenbankaufbau. Nächster Schritt: 2e (Punkte ziehen, hinzufügen, löschen), danach 2d (Oberflächen). Reihenfolge so gewählt, weil sich Formen erst mit 2e verändern lassen.
- 30.09.2026: Nachbesserung 2c gebaut (grün) und am Handy getestet: Flächen schließen sich nicht von selbst, Tipp auf den Ring schließt (erst ab drei Punkten), Einrasten mit Hilfslinien ergibt saubere rechte Winkel, bei "Einrasten aus" bleibt der Punkt wo losgelassen, alle 8 Formen mit Vorschau, zweiter Finger bricht das Aufziehen ab und zoomt, Rückgängig entfernt die letzte Form. Urteil: Einrasten, Ring und Setzen beim Loslassen fühlen sich gut an; die Werkzeugleiste darf nicht höher werden (sonst zu wenig Platz zum Bearbeiten). Wunsch: Hilfetext hinter ein Fragezeichen in der Kopfleiste.
- 30.09.2026: GARTENPLAN.md Version 0.8. Teilschritt 2e als **Vorschlag geliefert, noch nicht gebaut**: Fläche auswählen (Tipp), Griffe (Kreis = rund, Quadrat = Ecke), Punkt ziehen (mit Einrasten zu den Nachbarpunkten), Punkt auf dem Rand hinzufügen, Punkt löschen (mind. 3), Rund/Ecke pro Punkt, Fläche verschieben, Menü "Fläche" (Name ändern, Nach vorn/hinten, Alle rund/eckig, Einrasten, Löschen mit Rückfrage), voller Rückgängig-Verlauf (bis 50 Abbilder), Hilfetext hinter "?" in der Kopfleiste (Werkzeugleiste ist dadurch niedriger). Keine Änderung am Datenbankaufbau. Nächster Schritt: 2d (Oberflächen mit Farben und Mustern, Tipp in der Ansicht zeigt Name und Oberfläche).
- 30.09.2026: Teilschritt 2e gebaut (grün) und am Handy getestet: Auswählen, Griffe (Kreis/Quadrat), Punkte ziehen, hinzufügen, löschen (nicht unter drei), Rund/Ecke, Fläche verschieben, Flächenmenü, Rückgängig über viele Schritte (auch gelöschte Fläche), leerer Verlauf nach erneutem Betreten, kontextabhängiges Fragezeichen: alles in Ordnung. Urteil: Griffe groß genug; Punkt auf dem Rand nur bei ausgewählter Fläche passt; Werkzeugleiste hoch genug; ein neuer Punkt auf einem Kreis dellt ihn leicht aus (akzeptiert). Wünsche: Beschriftung "Punkt löschen" war abgeschnitten; Rückmeldung beim Verschieben einer Fläche; Flächenmenü per langem Tipp statt Knopf; Gartenname in der Kopfleiste wird abgeschnitten (Symbole statt Texte); Hinweis: Flächen sollen deckend sein.
- 30.09.2026: GARTENPLAN.md Version 0.9. Überarbeitung 2e als **Vorschlag geliefert, noch nicht gebaut**: Knopfbeschriftungen mit weniger Innenabstand und kleinerer Schrift (damit "Punkt löschen" ganz lesbar ist), Flächenmenü per langem Tipp an der Fingerstelle (Knopf "Fläche" entfällt, stattdessen "Abwählen" und "Einrasten"), beim Verschieben einer Fläche Vibrieren, Schatten und dickerer Rahmen, Flächen deckend gefüllt, Kopfleiste mit Symbolen (Zurück-Pfeil, Spaten für Bearbeiten, "?" bleibt; Stift als Alternative gezeichnet). Keine Änderung am Datenbankaufbau. Nächster Schritt: 2d (Oberflächen mit Farben und Mustern).
- 30.09.2026: Teilschritt 2d als **Vorschlag geliefert, noch nicht gebaut** (gemeinsam mit der noch nicht gepushten Überarbeitung von 2e in einer Lieferung, auf Wunsch des Projektinhabers: kleine Änderungen nicht einzeln pushen): acht Oberflächen (Gras, Erde, Kies, Pflaster/Stein, Holz, Sand, Wasser, Beton/Asphalt) mit deckender Farbe und eigenem Muster, gespeichert nur als Schlüssel; nach dem Anlegen einer Fläche öffnet sich die Oberflächenwahl (Schließen ohne Wahl behält Gras; die Wahl zählt nicht als eigener Rückgängig-Schritt), später im Flächenmenü ("Oberfläche ändern", als eigener Rückgängig-Schritt); in der Ansicht zeigt ein Tipp auf eine Fläche eine Karte mit Name ("Ohne Namen" falls leer) und Oberfläche, die Fläche wird hervorgehoben. Keine Änderung am Datenbankaufbau. Dazu gilt weiter die Überarbeitung von 2e (Knopfbeschriftung, Fläche "an der Hand", Flächenmenü per langem Tipp, deckende Flächen, Symbole in der Kopfleiste). Nächste Schritte: Teilschritt 2 abschließen (Test), danach Umsetzungsschritt 3 (Gegenstände und Pflanzen platzieren) und 4 (Pflanzendatenbank).
- 02.10.2026: Teilschritte 2e-Überarbeitung und 2d gebaut (grün) und am Handy getestet. Alles in Ordnung: Kopfleiste (Zurück-Pfeil, voller Name lesbar), Knopfbeschriftungen, kurze Vibration beim Verschieben ("kurz und knackig"), Flächenmenü per langem Tipp (leerer Platz tut nichts, Finger bewegen verschiebt ohne Menü, Zeitpunkt gut), deckende Flächen, Oberflächenwahl (ohne Wahl bleibt Gras), Rückgängig direkt nach dem Anlegen entfernt die ganze Fläche, Oberflächenwechsel ist eigener Schritt, Tipp in der Ansicht zeigt Karte, Muster ruckeln nicht und bleiben beim Verschieben an der Fläche. Befunde: Schatten beim Verschieben nur über dunklem Untergrund sichtbar; Beton, Pflaster und Kies zu ähnlich; Spaten-Symbol sieht etwas seltsam aus (eigenes Piktogramm folgt vom Projektinhaber). Wünsche: "Zu rund"/"Zu Ecke" heißen "Abrunden"/"Entrunden".
- 02.10.2026: Kleine Änderungen als **Vorschlag geliefert, noch nicht gebaut** (nicht einzeln pushen, mit dem Piktogramm bündeln): Schatten entfällt, stattdessen wird die ganze angefasste Fläche heller und greller gezeichnet (dicker Rahmen bleibt); Punktknöpfe heißen "Abrunden" und "Entrunden"; neue Farben Kies (sehr hell), Pflaster (gedämpftes Rosagrau), Beton (dunkles Asphaltgrau). Notiert für später: **Ebenenanzeige**, weil man nach mehrmaligem Nach-vorn/Nach-hinten nicht mehr sieht, auf welcher Ebene eine Fläche liegt (entweder Ebenenliste am Fensterrand wie bei Photoshop oder eine Anzeige an der ausgewählten Fläche). Das Stockbild des Projektinhabers (Vecteezy, Lizenz ungeprüft) wird bewusst **nicht** verwendet, um Lizenzfragen zu vermeiden; stattdessen ein selbst gezeichnetes Spaten-Symbol (Spaten im Erdhaufen, Vektor, gleiche Strichart wie die anderen Symbole).
- 02.10.2026: GEGENSTAENDE.md Version 0.1 (Konzept Umsetzungsschritt 3) als **Vorschlag geliefert**, noch nichts entschieden: Gegenstände aus Katalog (etwa 25, selbst gezeichnete Draufsicht-Symbole, keine Bilddateien), Platzieren per Tipp auf den Ort, dann Katalog; Verschieben, Größe ändern (Eck- und Kantengriffe), Drehen (Griff, Einrasten 15 Grad), Menü per langem Tipp inklusive Duplizieren; drei feste Ebenen (Flächen, Gegenstände, später Pflanzen); neue Tabelle Gegenstand (Datenbankversion 3, wieder einmaliger Datenverlust der Testgärten); Teilschritte 3a bis 3d; zwölf offene Fragen (u. a. Zäune und Hecken, unregelmäßige Gebäude, Maßstab, Ebenenanzeige).
- 02.10.2026: Rückmeldung zu GEGENSTAENDE.md Version 0.1 eingearbeitet (**Version 0.2, Vorschlag**). **Entschieden:** erst Ort antippen, dann Katalog; zusätzliche Katalogeinträge (Motorrad, Fahrrad, Spielhaus, Wärmepumpe, Ladesäule, Trittstein, Rasenmähroboter) und mehr Details in den Symbolen; Drehgriff mit Einrasten alle 15 Grad; Größe an Ecken und Kanten; **Ebenen frei mischbar** (neuer Gegenstand Bewässerung darf unter Flächen liegen); unregelmäßige Gebäude als Fläche mit neuer Oberfläche "Gebäude"; Zäune, Hecken, Mauern und Bewässerung als schmale Gegenstände im Katalog (Muster wiederholt sich, Ecken durch zweites Stück plus Drehen); Farbauswahl für Gegenstände von Anfang an; Namen nur per Tipp; **Maßstab wird eingebaut** (Lineale oben und links im Bearbeitungsmodus, Maße für Flächen und Gegenstände, später Pflanzen; frühere Entscheidung "nur Skizze" damit geändert); **Ebenenliste am rechten Rand**; Größe und Drehen schon in 3b. Teilschritte neu: 3a Maßstab, 3b Gegenstände Kern, 3c Farbe/schmale Gegenstände/Gebäude, 3d Ebenenliste, 3e Feinschliff. Offene Fragen A bis J in Abschnitt 9.
- 02.10.2026: GEGENSTAENDE.md **Version 0.3** (Vorschlag, drei Rückfragen offen). **Entschieden (zweite Runde):** Lineal mit festem Nullpunkt; Gitter in Metern; jeder Gegenstand hat eine feste echte Ausgangsgröße, beim Ziehen an den Griffen erscheint das Maß live; 12 feste Farbfelder, **auch Flächen bekommen eine wählbare Farbe, das Muster bleibt**; Auge zum Ausblenden in der Ebenenliste; Schalter "Verdecktes zeigen"; **Gebäude bleibt Gegenstand** (Korrektur eines Missverständnisses: kein Flächen-Typ "Gebäude"), als "Gebäude (freie Form)" mit Punkten wie eine Fläche; Karte zeigt ungefähre Fläche in m²; Detailgrad der Symbole gut, Hochbeet korrigiert; **Ebenenliste schon in 3a**. Teilschritte jetzt: 3a Maßstab und Ebenenliste (Datenbank-Neuanlage mit allen neuen Tabellen), 3b Gegenstände Kern, 3c Farbe/schmale Gegenstände/Verdecktes zeigen, 3d Feinschliff. Rückfragen: freies Gebäude als Gegenstand (Punkte statt Größen- und Drehgriffe), "Maßstab festlegen" als Zusatzfunktion, Bedeutung des Auges.
- 02.10.2026: GEGENSTAENDE.md **Version 0.4**, keine offenen Fragen. **Entschieden (dritte Runde):** freies Gebäude ist drehbar; "Maßstab festlegen" entfällt (Größen werden an den Griffen angepasst, Maßstab fest 10 Einheiten pro Meter, als Wert pro Garten gespeichert); **Ebenenleiste** am rechten Rand mit einem Punkt je Ebene, Pfeil nach oben/unten zum Wechseln der Ebene und Auge, das die Liste aller Einträge nach Ebene öffnet (kein Ausblenden einzelner Elemente). Teilschritt **3a als Vorschlag geliefert, noch nicht gebaut**: Datenbankversion 3 (Garten.Maßstab, Fläche.Farbe, neue Tabellen Gegenstand und Gegenstandspunkt; einmaliger Datenverlust der Testgärten), Lineale oben und links im Bearbeitungsmodus (fester Nullpunkt, Meter, Teilung nach Zoom), Gitter in Metern, Info-Karte in der Ansicht zeigt Maße und ungefähre Fläche in m², Ebenenleiste. Eigenes Spaten-Symbol aus Version vom selben Tag ist enthalten.
- 02.10.2026: Teilschritt 3a gebaut (grün) und am Handy getestet: Lineale, Meter-Gitter, Ebenenleiste (Pfeile, Punkte, Auge), Fläche in m², Spaten-Symbol: alles in Ordnung. Wünsche: Liste der Ebenen soll sich ausklappen statt ein eigenes Fenster zu öffnen; mehrere Flächen sollen in einer Ebene liegen können (bisher je Fläche eine Ebene). GEGENSTAENDE.md **Version 0.5** (Vorschlag, fünf Rückfragen): Ebene als Behälter mit aktiver Ebene, ausklappbarer Liste, neue Tabelle Ebene (Datenbankversion 4, Testgärten noch einmal weg), Überarbeitung 3a2 vor 3b.
- 02.10.2026: Antworten zu den Ebenen: Ebene ist Behälter, neue Elemente in die aktive Ebene; "Nach vorn/hinten" verschiebt in die Nachbarebene (Reihenfolge innerhalb der Ebene egal); beim Löschen einer Ebene wird gefragt (Elemente mitlöschen oder in Nachbarebene verschieben); Auge pro Ebene (anderes Symbol als an der Leiste); Punkte der Leiste neutral. GEGENSTAENDE.md **Version 0.6**. Teilschritt **3a2 als Vorschlag geliefert, noch nicht gebaut**: Datenbankversion 4 (Tabelle Ebene; Testgärten gehen noch einmal verloren), Leiste mit einem Punkt je Ebene, ausklappbare Liste neben der Leiste (Ebenen mit Flächen, Neue Ebene, Umbenennen, Löschen, Verschieben, Ausblenden). Nächster Schritt danach: 3b (Gegenstände).


### Nachtrag zu 3a2 (Testergebnis und Verfeinerungen)
- Tests 1–7 von 3a2 alle bestanden.
- Neu: Auge pro Ebene mit drei Stufen (sichtbar → halbtransparent → ausgeblendet), Ebenennamen zweizeilig, Ebenenliste schließt per Tipp daneben. Datenbank-Version 5 (Testgärten werden einmal gelöscht). Noch nicht gebaut/getestet.


### Teilschritt 3b (Gegenstände, Kern) als Vorschlag geliefert
- Katalog mit 35 Gegenständen, Platzieren per Tipp, Auswählen, Verschieben, Größe ändern mit Live-Maß, Drehen mit Einrasten, Menü, Karte, Rückgängig, Ebenenliste, Garten duplizieren. Das freie Gebäude wurde auf 3b2 verschoben.
- Keine neue Datenbankversion gegenüber 3a3 (Version 5).
- Fehler in 3a3 gefunden und behoben: Die Ebenenliste kannte die neue Parameterbezeichnung des Auges noch nicht, der Bau von 3a3 wäre fehlgeschlagen.
- Noch nicht gebaut, nicht getestet.


### Tests und Teilschritt 3c
- Test von 3a3 (dreistufiges Auge, Namen, Liste schließen): alle 4 Tests in Ordnung.
- Test von 3b (Gegenstände): alle 10 Tests in Ordnung, der Bau hat geklappt.
- Teilschritt 3c als Vorschlag geliefert: Farbwahl (12 Felder) für Gegenstände und Flächen, Zaun/Hecke/Mauer/Bewässerung mit sich wiederholendem Muster, Schalter "Verdecktes zeigen". Keine neue Datenbankversion. Noch nicht gebaut, nicht getestet.
