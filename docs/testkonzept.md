# Testkonzept – GamesLibrary

## 1. Teststrategie und Testobjekt

### 1.1 Testobjekt
Das Testobjekt ist die Fullstack-Webapplikation **GamesLibrary** zur Verwaltung einer persönlichen Spielesammlung.
Im Repository befindet sich folgendes Maven-Modul, das in der CI/CD-Pipeline automatisch gebaut und getestet wird:

| Modul / Ordner | Technologie | Beschreibung |
|---|---|---|
| `backend` | Spring Boot 4.1.0, Java 26, Maven, H2 | REST-API für CRUD-Operationen auf Spielen (`/api/games`), Titelsuche (`/api/games/search`), Hilfsklassen `TitleNormalizer` und `GameBadges` sowie Proxy zur Steam Store API (`/api/steam`) |

### 1.2 Abzudeckende Funktionen und Risiken
| ID | Funktion / Komponente | Risiko bei Fehlfunktion | Schaden | Eintritt | Priorität | Zugeordnete Teststufen |
|---|---|---|---|---|---|---|
| R1 | **Spieleverwaltung (CRUD & Validierung)** (`GameService`, `GameController`, `Game`) | Ungültige Daten (z. B. leerer Titel, Erscheinungsdatum in der Zukunft) werden gespeichert oder bestehende Einträge beim Update überschrieben/beschädigt; nicht existierende IDs liefern falschen HTTP-Statuscode. | Hoch | Mittel | Hoch | Unit-Test (`GameServiceTest`), Integrationstest (`GameApiIntegrationTest`), Controller-Test (`@WebMvcTest`), Schnittstellentest (Postman) |
| R2 | **Normalisierung & Badges von Spielen** (`TitleNormalizer`, `GameBadges`) | Uneinheitliche Titel durch Leerzeichen/Kleinschreibung oder zeitabhängige (Flaky) Berechnungen für «neue» Spiele (`LocalDate.now()` ohne feste `Clock`). | Mittel | Mittel | Mittel | Unit-Test mit TDD (`TitleNormalizerTest`), zeitstabiler Unit-Test mit `Clock` (`GameBadgesTest`) |
| R3 | **Suche nach Titel** (`GameRepository`, `GameService`, `GameController`) | Spiele werden trotz passendem Teilbegriff (Gross-/Kleinschreibung) über `GET /api/games/search?title=...` nicht gefunden oder die Datenbankabfrage schlägt fehl. | Mittel | Gering | Mittel | Unit-Test (`GameServiceTest`), Integrationstest (`GameApiIntegrationTest` mit `@SpringBootTest` und H2) |
| R4 | **Externe Abhängigkeit Steam-API** (`SteamController`) | Ausfall oder veränderte Antworten der externen Steam Store API führen zu Fehlern oder instabilen Tests. | Gering | Mittel | Gering | Isolierte Tests ohne laufende Fremddienste |

### 1.3 Teststufen
1. **Unit-Tests:** Isolierte Prüfung einzelner Klassen (`TitleNormalizer`, `GameService`, `GameBadges`) mit JUnit 5 und Mockito 5 (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, `when/thenReturn`, `verify`) sowie `java.time.Clock` für zeitabhängige Logik. Es wird keine Datenbank gestartet.
2. **Komponenten-/Controller-Tests:** Prüfung der HTTP-Schicht (`GameController`, Validierung `@Valid`, `GlobalExceptionHandler`) mit `@WebMvcTest`, `MockMvc` und `@MockitoBean` (folgt in Meilenstein 6.2).
3. **Integrationstests (`GameApiIntegrationTest`):** Prüfung des Zusammenspiels von `GameController`, `GameService`, `GameRepository`, `GlobalExceptionHandler` und der H2-In-Memory-Datenbank mit `@SpringBootTest`, `@AutoConfigureMockMvc` und `@ActiveProfiles("h2")`. Geprüft werden `GET /api/games` (Liste nicht leer), `GET /api/games/search?title=...` (Suche mit Treffer / ohne Treffer) und `GET /api/games/{id}` (404 bei unbekannter ID).
4. **Schnittstellentests:** Automatisierte API-Tests über HTTP mit einer Postman-Collection in `postman/` (folgt in Meilenstein 6.1).

---

## 2. Rollen und Verantwortlichkeiten im Team

*(Wird bis zur Schlussabgabe ergänzt – Aufteilung von Driver/Navigator beim TDD, Erstellung und Pflege von Testkonzept, Unit-/Integrationstests, Pipeline und Code-Reviews.)*

---

## 3. Infrastruktur (Umgebungen, Testdaten, Werkzeuge)

- **Lokale Entwicklungsumgebung:** IntelliJ IDEA, JDK 26 (gemäss `backend/pom.xml`), Maven-Wrapper (`./mvnw`), H2-In-Memory-Datenbank (`jdbc:h2:mem:gamesdb`).
- **CI/CD-Umgebung:** GitHub Actions Runner (`ubuntu-latest`) mit Temurin JDK 26 und Maven-Dependency-Cache.
- **Testdaten:** Für Unit-Tests werden Testobjekte direkt im Arrange-Schritt per `Game.builder()` und fester `Clock.fixed(...)` erzeugt; für Integrationstests wird die H2-In-Memory-Datenbank mit den initialen Testdaten aus `backend/src/main/resources/data.sql` verwendet.
- **Werkzeuge:** JUnit 5, Mockito 5, Spring Boot Test / MockMvc, JaCoCo (`jacoco-maven-plugin` 0.8.16), Postman (ab Auftrag 6.1), GitHub Actions.

---

## 4. Entwicklungsvorgehen und Pipeline (TDD & CI/CD)

### 4.1 Test-Driven Development (TDD)
Neue Fachlogik wie der `TitleNormalizer` wird nach dem TDD-Zyklus **Rot → Grün → Refactor** entwickelt:
1. **Phase Rot:** Zuerst wird ein fehlschlagender Unit-Test für genau ein neues Verhalten geschrieben und committet (`rot: ...`).
2. **Phase Grün:** Danach wird nur so viel Produktionscode geschrieben, bis alle Tests grün sind, und separat committet (`grün: ...`).
3. **Phase Refactor:** Bei Bedarf wird der Code ohne Verhaltensänderung aufgeräumt (`refactor: ...`).

Dadurch ist in der Git-Historie (`git log --oneline`) jederzeit nachvollziehbar, dass der Test vor dem Produktionscode entstanden ist.

### 4.2 CI/CD-Pipeline (`.github/workflows/ci.yml`)
- **Trigger:** Jeder `push` und `pull_request` startet automatisch den Workflow `CI`.
- **Matrix-Build (`test`):** Alle Maven-Ordner des Repositories (`service: [backend]`) werden mit `./mvnw -B verify` auf einem frischen Runner gebaut, getestet und mit JaCoCo ausgewertet (`fail-fast: false`).
- **JaCoCo-Artefakt:** Nach dem Testlauf lädt `actions/upload-artifact@v4` (mit `if: always()` und `if-no-files-found: ignore`) den HTML-Coverage-Bericht aus `backend/target/site/jacoco` als Artefakt `jacoco-backend` hoch.
- **Deployment-Steuerung (`deploy`):** Der Job `deploy` besitzt die Abhängigkeit `needs: test`. Er wird **nur dann ausgeführt, wenn alle Test-Jobs grün sind**.

---

## 5. Testfallerstellung

- **Methodik:** Bildung von Äquivalenzklassen (gültige Eingaben vs. ungültige Eingaben) und Grenzwertanalysen (z. B. `null`, leerer String `""`, nur Leerzeichen `"   "`, 30-Tage-Grenze bei `GameBadges`, existierende vs. unbekannte ID `999999`, Suche mit 1 Treffer vs. 0 Treffern).
- **Vermeidung von Flaky Tests:** Zeitabhängige Logik (`GameBadges.isNew`) verwendet kein direktes `LocalDate.now()`, sondern erhält eine `java.time.Clock` über den Konstruktor (`Clock.fixed(...)` im Test).
- **Aufbau der Tests:** Alle Tests folgen dem **Arrange-Act-Assert**-Muster (AAA) und nutzen sprechende Methodennamen nach dem Schema `methode_szenario_erwartetesVerhalten`.

---

## 6. Testfalldurchführung und Abnahmekriterien

- **Lokal ausführen:** `cd backend && ./mvnw verify` (führt Unit- und Integrationstests aus und erzeugt den JaCoCo-Bericht unter `backend/target/site/jacoco/index.html`).
- **In der Pipeline:** Automatisch bei jedem Push und Pull Request über GitHub Actions.
- **Abnahmekriterien:**
  - Alle Unit- und Integrationstests laufen lokal und in der Pipeline zu 100 % grün und zeitunabhängig (keine Flaky Tests).
  - Mindestens 10 grüne Unit-Tests insgesamt, davon mindestens 3 mit `@Mock` und `verify(...)`.
  - Mindestens 4 grüne Integrationstests in `GameApiIntegrationTest` mit `@SpringBootTest` und H2.
  - JaCoCo-Coverage-Bericht liegt nach jedem Pipeline-Lauf als Artefakt vor.
  - Der `deploy`-Job läuft ausschliesslich nach erfolgreichem `test`-Job.

---

## 7. Fehlerbehandlung

Gefundene Defekte werden als nachstellbare GitHub-Issues mit der Vorlage `.github/ISSUE_TEMPLATE/bug.md` erfasst (Felder: **Titel**, **Schritte**, **Erwartet**, **Tatsächlich**, **Umgebung**, **Schweregrad** sowie die Beschreibung des Tests, der den Fehler beweist).

**Klassifikation der Schweregrade:**
- **Hoch:** Kernfunktion (CRUD/Start) fällt aus, Datenverlust oder falscher `500 Internal Server Error` bei normalen Client-/Validierungsfehlern.
- **Mittel:** Funktion liefert falsches Ergebnis oder falschen HTTP-Statuscode (z. B. `500` statt `400 Bad Request` bei fehlendem Query-Parameter oder ungültiger Pfad-ID), Workaround möglich.
- **Niedrig:** Kosmetischer Fehler oder kleinere Abweichung ohne Einfluss auf die Kernfunktionalität.
