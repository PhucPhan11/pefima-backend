# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

`pefima-backend` is the (future) Spring Boot service for **PeFiMa**, a personal-finance manager whose data currently lives in Notion and is archived in the sibling repository `..\pefima-data`. So far it has a read-only data-source layer (see "Data source") and a read-only REST API with Swagger UI on top of it (see "REST API"). There are no services, persistence of its own, security, or profiles yet, so any other architecture you add is the first of its kind. Confirm the approach with the user before introducing a new layer or dependency.

- Remote: `https://github.com/PhucPhan11/pefima-backend.git`, default branch `main`.
- Commit style so far: Conventional Commits (`feat: ...`).

## Stack

| Item | Value |
|---|---|
| Spring Boot | `4.1.1` (parent `spring-boot-starter-parent`; upgraded from 4.0.6 on 2026-10-19) |
| Java | `21` (`<java.version>` in `pom.xml`) |
| Build | Maven via wrapper (`mvnw` / `mvnw.cmd`, wrapper 3.3.4, Maven 3.9.14, `only-script` distribution) |
| Dependencies | `spring-boot-starter`, `spring-boot-starter-webmvc`, `springdoc-openapi-starter-webmvc-ui` (pinned via `<springdoc.version>`; the 3.x line is the one for Boot 4), `spring-jdbc` (plain, not the starter), `org.xerial:sqlite-jdbc` (pinned via `<sqlite-jdbc.version>`, not in the Boot BOM), `spring-boot-starter-test` (JUnit 5, AssertJ, Mockito) |
| Coordinates | `com.ceu.pefima:backend:0.0.1-SNAPSHOT`, base package `com.ceu.pefima.backend` |

Let the Boot parent manage dependency versions; don't pin versions that the BOM already covers.

## Commands

Run from the repository root in PowerShell:

```powershell
.\mvnw.cmd clean verify                                   # compile + all tests + package jar
.\mvnw.cmd test -Dtest=SqliteSourcesTest                  # single test class
.\mvnw.cmd test -Dtest=PefimaBackendApplicationTests#contextLoads   # single test method
.\mvnw.cmd spring-boot:run                                # run on http://localhost:8080 (API answers 503: no source)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--pefima.source.sqlite.path=../pefima-data/exports/clean/notion-clean.sqlite3"   # run with the SQLite source
```

**JDK gotcha:** the wrapper uses `JAVA_HOME`, not the `java` on PATH. As of 2026-10-21, `JAVA_HOME` points to Temurin 21 (`C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot`), so `.\mvnw.cmd` works as is. Earlier it pointed to JDK 17, and builds failed with `release version 21 not supported`. If that error comes back, override it for the command only:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot'; .\mvnw.cmd clean verify
```

Don't change system or user environment variables yourself. Tell the user instead.

The test run prints a Mockito "self-attaching inline-mock-maker" warning. It's harmless for now.

## Layout

```text
src/main/java/com/ceu/pefima/backend/
  PefimaBackendApplication.java    entry point
  api/                             REST controller, problem-detail error mapping, OpenAPI info
  source/                          read-only ports (*Source interfaces) + exceptions
  source/model/                    records returned by the ports (BigDecimal, LocalDate, Provenance)
  source/sqlite/                   adapter over the notion_cleanup SQLite DB + SqliteSourceConfiguration
src/main/resources/application.properties
src/test/java/com/ceu/pefima/backend/api/             @WebMvcTest controller tests + OpenAPI/Swagger UI test
src/test/java/com/ceu/pefima/backend/source/sqlite/   adapter tests + SyntheticCleanupDatabase fixture builder
src/test/resources/pefima-data/schema-v1.2.sql        verbatim copy of pefima-data/notion_cleanup/schema.sql
```

Put new code in sub-packages of `com.ceu.pefima.backend` so component scanning picks it up. Packages are `@NullMarked` (JSpecify), so mark nullable record components and returns with `@Nullable`.

## Data source

The backend collects data from `pefima-data` through source-agnostic ports in `source/`: `SnapshotSource`, `BudgetEnvelopeSource`, `WalletAccountSource`, `TransactionSource`, `MonthlyBalanceSource`, and `ReviewIssueSource`. The only adapter so far is `source/sqlite`, which reads the database written by `pefima-data`'s `notion_cleanup` importer through its app-facing views (`budget_envelope_observations`, `wallet_account_observations`, `transaction_observations`, `monthly_balance_observations`) and its `export_snapshots`, `schema_meta`, `conflicts`, and `unknown_fields` tables.

- It's enabled only when `pefima.source.sqlite.path` is set (example commented in `application.properties`). Without it, no source beans exist and the app still starts.
- It opens the file **read-only** (`SQLiteConfig.setReadOnly`). It fails at startup if the file is missing or `schema_meta.schema_version` isn't `1.2` (pefima-data's importer upgrades 1.0 and 1.1 databases in place when it ingests). Never write to the pefima-data database.
- The SQLite `DataSource` is deliberately not a bean, so it can't become the app's primary database through auto-configuration. Keep it that way if you add a real database.
- Every data query takes an explicit `snapshotId`, and an unknown ID throws `UnknownSnapshotException`. Don't add "latest snapshot" defaults; snapshot choice belongs to the user or caller.
- `ReviewIssue` deliberately omits `conflicts.value_a`/`value_b` (raw financial values). Don't add them.
- If `schema.sql` in pefima-data changes, refresh `src/test/resources/pefima-data/schema-v1.2.sql` (rename it for a new version) and `SqliteCleanupDatabase.SUPPORTED_SCHEMA_VERSION`.
- `WalletAccount.etfCompany` comes from the view's `etf_company`: the title of a Fund wallet's `ETF Company` relation, a page outside the export. It is a name only; the importer keeps the link in `relations.external_locator`, and the API must never return that link. Titles are written at ingest time (contract 1.2), so a database ingested earlier has `null` here until it is re-ingested.
- `WalletAccount.emergencyFundAllocation` is true when the wallet has a `relations` row with `canonical_field = 'emergency_fund_allocation_relation'` (Notion's `🏹 EmerFund Allocation`, from the 2025 exports on). The view has no column for it, so `SqliteWalletAccountSource` checks with `EXISTS` on the `relations` table. That is the adapter's only read of `relations`, and it never selects the relation's target or link. The relation exists in contract 1.2, so it needs no re-ingest.
- Test with `SyntheticCleanupDatabase` (synthetic IDs and amounts only), never with the real database. Since 2026-10-21 the real local DB is built with `notion_cleanup`'s `"mode": "all"` scope and `tools\ingest_weekly_exports.py` in pefima-data: one snapshot per 2026 weekly export plus one per archived year 2022–2025 (nominal export date `<year>-12-31`, so `year` is the data year), each with all five categories (2022 has no Budget). Since the follow-up importer fixes, these snapshots carry no open review items.
- A future CSV-archive adapter should implement the same ports. It must take an explicit 2026 weekly root rather than picking one.

## REST API

`api/PefimaDataController` exposes the ports read-only under `/api`: `GET /api/source/contract`, `GET /api/snapshots`, `GET /api/snapshots/{snapshotId}`, plus `/budgets`, `/wallets`, `/transactions`, `/monthly-balances`, and `/review-issues` under a snapshot. `/monthly-balances` returns the Notion Balance rows (January to December, then `Year`); its `net` is income minus expenses, not an account balance. `/wallets` items carry `etfCompany` (fund company name or `null`) and `emergencyFundAllocation` (boolean). Swagger UI is at `http://localhost:8080/swagger-ui.html` and the OpenAPI JSON at `/v3/api-docs`.

- `server.address=127.0.0.1`: the API serves personal finance data, so it only listens on this machine. Don't widen it without the user's say-so.
- The controller takes the sources through `ObjectProvider`, so the app (and Swagger UI) starts without `pefima.source.sqlite.path`. Endpoints then answer 503.
- `ApiExceptionHandler` maps `UnknownSnapshotException` to 404, the missing source to 503, and other `PefimaSourceException`s to 500. All of these use `ProblemDetail` responses. Source exception messages are value-free, so it's safe to return them; keep them that way.
- Controller tests use `@WebMvcTest` + `@MockitoBean` + `MockMvcTester` with synthetic records. In Boot 4, `WebMvcTest`/`AutoConfigureMockMvc` live in `org.springframework.boot.webmvc.test.autoconfigure`.
- In this app, `.claude/launch.json` (in the parent `pefima` folder) runs the built jar on port 8080 with the SQLite source. Rebuild the jar (`.\mvnw.cmd package`) before relaunching.
- In IntelliJ, the shared run configs in `.run/` cover the same setup: `Pefima Backend` (Spring Boot with the SQLite source), `Pefima Frontend` (`npm run dev` in `../pefima-frontend`), and the compound `Pefima All`, which starts both.

## Spring Boot 4 notes

Most online examples target Boot 3. Watch for these differences:

- Starters are modular: use `spring-boot-starter-webmvc` (not the deprecated `spring-boot-starter-web`), plus the matching test starter (for example `spring-boot-starter-webmvc-test`) for slice tests.
- Jackson 3 is the default. Its packages are `tools.jackson.*`, while annotations stay in `com.fasterxml.jackson.annotation`.
- `@MockBean`/`@SpyBean` are gone. Use `@MockitoBean`/`@MockitoSpyBean` from `org.springframework.test.context.bean.override.mockito`.
- The baseline is Jakarta EE 11 (`jakarta.*`, never `javax.*`). JSpecify nullness annotations (`org.jspecify.annotations`) are the Spring-standard choice.

## Domain source: `..\pefima-data`

The backend has no domain model yet. The analysis behind one lives in the data repo. Read these before designing entities or APIs:

- `..\pefima-data\data-structure.md`: section 6 covers row grain and relationships, section 7 has the **proposed normalized schema** (`ExportSnapshot`, `Page`, `WalletAccount`, `BudgetEnvelope`, `Transaction`, `Loan`, `Debt`, `MonthlyBalance`, `Relation`, `ComputedProperty`), and section 9 lists **open questions** that are still unanswered: authoritative 2026 root, currency, timezone, formula materialization, and whether Budget rows are envelopes or accounts. Don't settle those questions in code. Ask the user.
- `..\pefima-data\notion_cleanup\schema.sql`: the versioned SQLite contract (v1.2) produced by the Notion cleanup importer. This is the most likely data source for the backend.
- `..\pefima-data\.github\instructions\notion-field-mapping.instructions.md`: the authoritative field-alias mapping. Don't re-create mappings in Java.
- `..\pefima-data\CLAUDE.md`: commands and conventions for the Python tools in that repo.

Categories: `Balance`, `Budget`, `Budgets Transactions`, `Correction`, `Debt`, `Expenses`, `Income`, `Internal Transactions`, `Onloan`, `Saving Investing`, `Wallet Balance`. Some of the source data is in Vietnamese, so keep everything UTF-8.

## Data-safety rules (carried over from pefima-data)

- Never put real financial amounts, account names, Notion page IDs, or external URLs into code, tests, fixtures, logs, commit messages, or docs. Use synthetic values.
- Regular and `_all` CSV views are separate views with different row sets. Never merge or deduplicate them implicitly.
- The 2026 archive has several weekly export roots. Never pick one as authoritative (including "the latest") without the user's decision.
- Keep provenance (snapshot, view, source page ID) on anything imported, and keep formula/rollup values distinct from raw values.
- Treat `pefima-data` as read-only from this repo. Don't write to the archive.

## Repo gotchas

- `README.md` is **UTF-16 LE with BOM** (it contains only a title). Rewrite it as UTF-8 if you edit it, and tell the user you did.
- `.gitattributes` forces `mvnw` to LF and `*.cmd` to CRLF. `core.autocrlf=true` in this checkout.
- `.idea/` and `target/` are git-ignored.
