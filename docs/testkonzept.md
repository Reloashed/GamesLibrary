# Testkonzept – GamesLibrary

## 1. Teststrategie und Testobjekt

### 1.1 Testobjekt
Das Testobjekt ist die Fullstack-Webapplikation **GamesLibrary** zur Verwaltung einer persönlichen Spielesammlung.
Im Repository befindet sich folgendes Maven-Modul, das in der CI/CD-Pipeline automatisch gebaut und getestet wird:

| Modul / Ordner | Technologie | Beschreibung |
|---|---|---|
| `backend` | Spring Boot 4.1.0, Java 26, Maven, H2 | REST-API für CRUD-Operationen auf Spielen (`/api/games`), Titelsuche (`/api/games/search`), Hilfsklasse `TitleNormalizer` sowie Proxy zur Steam Store API (`/api/steam`) |

### 1.2 Abzudeckende Funktionen und Risiken
| ID | Funktion / Komponente | Risiko bei Fehlfunktion | Schaden | Eintritt | Priorität | Zugeordnete Teststufen |
|---|---|---|---|---|---|---|
| R1 | **Spieleverwaltung (CRUD & Validierung)** (`GameService`, `GameController`, `Game`) | Ungültige Daten (z. B. leerer Titel, Erscheinungsdatum in der Zukunft) werden gespeichert oder bestehende Einträge beim Update überschrieben/beschädigt. | Hoch | Mittel | Hoch | Unit-Test (`GameServiceTest`), Integrationstest (`@SpringBootTest`), Controller-Test (`@WebMvcTest`), System-/Schnittstellentest (Postman) |
| R2 | **Normalisierung von Spieltiteln** (`TitleNormalizer`) | Uneinheitliche Titel durch führende/nachgestellte oder doppelte Leerzeichen, Kleinschreibung am Wortanfang oder Akzeptanz leerer Eingaben (`null`, `""`, `"   "`). | Mittel | Mittel | Mittel | Unit-Test mit TDD (`TitleNormalizerTest`) |
| R3 | **Suche nach Titel** (`GameRepository`, `GameService`) | Spiele werden trotz passendem Teilbegriff (Gross-/Kleinschreibung) nicht gefunden. | Mittel | Gering | Mittel | Unit-Test (`GameServiceTest`), Integrationstest (`@SpringBootTest` mit H2) |
| R4 | **Externe Abhängigkeit Steam-API** (`SteamController`) | Ausfall oder veränderte Antworten der externen Steam Store API führen zu Fehlern oder instabilen Tests. | Gering | Mittel | Gering | Isolierte Tests ohne laufende Fremddienste |

### 1.3 Teststufen
1. **Unit-Tests:** Isolierte Prüfung einzelner Klassen (`TitleNormalizer`, `GameService`) mit JUnit 5 und Mockito 5 (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, `when/thenReturn`, `verify`). Es wird keine Datenbank gestartet.
2. **Komponenten-/Controller-Tests:** Prüfung der HTTP-Schicht (`GameController`, Validierung `@Valid`, `GlobalExceptionHandler`) mit `@WebMvcTest`, `MockMvc` und `@MockitoBean` (folgt in Meilenstein 6.2).
3. **Integrationstests:** Prüfung des Zusammenspiels von Controller, Service, Repository und H2-In-Memory-Datenbank mit `@SpringBootTest` (folgt in Meilenstein 5.3).
4. **Schnittstellentests:** Automatisierte API-Tests über HTTP mit einer Postman-Collection in `postman/` (folgt in Meilenstein 6.1).

---

## 2. Rollen und Verantwortlichkeiten im Team

*(Wird bis zur Schlussabgabe ergänzt – Aufteilung von Driver/Navigator beim TDD, Erstellung und Pflege von Testkonzept, Unit-/Integrationstests, Pipeline und Code-Reviews.)*

---

## 3. Infrastruktur (Umgebungen, Testdaten, Werkzeuge)

- **Lokale Entwicklungsumgebung:** IntelliJ IDEA, JDK 26 (gemäss `backend/pom.xml`), Maven-Wrapper (`./mvnw`), H2-In-Memory-Datenbank (`jdbc:h2:mem:gamesdb`).
- **CI/CD-Umgebung:** GitHub Actions Runner (`ubuntu-latest`) mit Temurin JDK 26 und Maven-Dependency-Cache.
- **Testdaten:** Für Unit-Tests werden Testobjekte direkt im Arrange-Schritt per `Game.builder()` erzeugt; für Integrationstests wird die H2-In-Memory-Datenbank verwendet.
- **Werkzeuge:** JUnit 5, Mockito 5, Spring Boot Test / MockMvc, JaCoCo (ab Auftrag 5.3), Postman (ab Auftrag 6.1), GitHub Actions.

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
- **Matrix-Build (`test`):** Alle Maven-Ordner des Repositories (`service: [backend]`) werden mit `./mvnw -B test` auf einem frischen Runner gebaut und getestet (`fail-fast: false`).
- **Deployment-Steuerung (`deploy`):** Der Job `deploy` besitzt die Abhängigkeit `needs: test`. Er wird **nur dann ausgeführt, wenn alle Test-Jobs grün sind**. Schlägt auch nur ein einziger Test fehl, bricht der Job `test` rot ab und `deploy` wird automatisch übersprungen.

---

## 5. Testfallerstellung

- **Methodik:** Bildung von Äquivalenzklassen (gültige Eingaben vs. ungültige Eingaben) und Grenzwertanalysen (z. B. `null`, leerer String `""`, nur Leerzeichen `"   "`, ein einzelnes Wort vs. mehrere Wörter mit mehrfachen Leerzeichen, existierende vs. nicht existierende IDs).
- **Aufbau der Tests:** Alle Unit-Tests folgen dem **Arrange-Act-Assert**-Muster (AAA) und nutzen sprechende Methodennamen nach dem Schema `methode_szenario_erwartetesVerhalten`.

---

## 6. Testfalldurchführung und Abnahmekriterien

- **Lokal ausführen:** `cd backend && ./mvnw test`
- **In der Pipeline:** Automatisch bei jedem Push und Pull Request über GitHub Actions.
- **Abnahmekriterien:**
  - Alle Unit- und Integrationstests laufen in der Pipeline zu 100 % grün.
  - Mindestens 10 grüne Unit-Tests insgesamt, davon mindestens 3 mit `@Mock` und `verify(...)`.
  - Der `deploy`-Job läuft ausschliesslich nach erfolgreichem `test`-Job.

---

## 7. Fehlerbehandlung

*(Wird in Auftrag 5.3 / bis zur Schlussabgabe mit Issue-Vorlage und Schweregrad-Klassifikation ergänzt.)*
