# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

`pefima-backend` is the (future) Spring Boot service for **PeFiMa**, a personal-finance manager whose data currently lives in Notion and is archived in the sibling repository `..\pefima-data`. Right now it is an untouched Spring Initializr skeleton: one `@SpringBootApplication` class, one `contextLoads()` test, and `spring.application.name` in `application.properties`. There are no controllers, entities, persistence, security, or profiles yet, so any architecture you add is the first of its kind. Confirm the approach with the user before introducing a new layer or dependency.

- Remote: `https://github.com/PhucPhan11/pefima-backend.git`, default branch `main`.
- Commit style so far: Conventional Commits (`feat: ...`).

## Stack

| Item | Value |
|---|---|
| Spring Boot | `4.1.1` (parent `spring-boot-starter-parent`; upgraded from 4.0.6 on 2026-10-19) |
| Java | `21` (`<java.version>` in `pom.xml`) |
| Build | Maven via wrapper (`mvnw` / `mvnw.cmd`, wrapper 3.3.4, Maven 3.9.14, `only-script` distribution) |
| Dependencies | `spring-boot-starter`, `spring-boot-starter-test` (JUnit 5, AssertJ, Mockito) |
| Coordinates | `com.ceu.pefima:backend:0.0.1-SNAPSHOT`, base package `com.ceu.pefima.backend` |

Let the Boot parent manage dependency versions; don't pin versions that the BOM already covers.

## Commands

Run from the repository root in PowerShell:

```powershell
.\mvnw.cmd clean verify                                   # compile + all tests + package jar
.\mvnw.cmd test -Dtest=PefimaBackendApplicationTests      # single test class
.\mvnw.cmd test -Dtest=PefimaBackendApplicationTests#contextLoads   # single test method
.\mvnw.cmd spring-boot:run                                # run the app (exits immediately: no web starter yet)
```

**JDK gotcha:** on this machine `JAVA_HOME` points to JDK 17 (`C:\Program Files\Java\jdk-17`) even though `java` on PATH is Temurin 21. The wrapper uses `JAVA_HOME`, so builds fail with `release version 21 not supported`. Unless the user has fixed it, override it for the command:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot'; .\mvnw.cmd clean verify
```

Don't change system or user environment variables yourself. Tell the user instead.

The test run prints a Mockito "self-attaching inline-mock-maker" warning. It's harmless for now.

## Layout

```text
src/main/java/com/ceu/pefima/backend/PefimaBackendApplication.java   entry point
src/main/resources/application.properties                            config (only the app name)
src/test/java/com/ceu/pefima/backend/PefimaBackendApplicationTests.java  @SpringBootTest smoke test
```

Put new code in sub-packages of `com.ceu.pefima.backend` so component scanning picks it up.

## Spring Boot 4 notes

Most online examples target Boot 3. Watch for these differences:

- Starters are modular: use `spring-boot-starter-webmvc` (not the deprecated `spring-boot-starter-web`), plus the matching test starter (for example `spring-boot-starter-webmvc-test`) for slice tests.
- Jackson 3 is the default. Its packages are `tools.jackson.*`, while annotations stay in `com.fasterxml.jackson.annotation`.
- `@MockBean`/`@SpyBean` are gone. Use `@MockitoBean`/`@MockitoSpyBean` from `org.springframework.test.context.bean.override.mockito`.
- The baseline is Jakarta EE 11 (`jakarta.*`, never `javax.*`). JSpecify nullness annotations (`org.jspecify.annotations`) are the Spring-standard choice.

## Domain source: `..\pefima-data`

The backend has no domain model yet. The analysis behind one lives in the data repo. Read these before designing entities or APIs:

- `..\pefima-data\data-structure.md`: section 6 covers row grain and relationships, section 7 has the **proposed normalized schema** (`ExportSnapshot`, `Page`, `WalletAccount`, `BudgetEnvelope`, `Transaction`, `Loan`, `Debt`, `MonthlyBalance`, `Relation`, `ComputedProperty`), and section 9 lists **open questions** that are still unanswered: authoritative 2026 root, currency, timezone, formula materialization, and whether Budget rows are envelopes or accounts. Don't settle those questions in code. Ask the user.
- `..\pefima-data\notion_cleanup\schema.sql`: the versioned SQLite contract (v1.0) produced by the Notion cleanup importer. This is the most likely data source for the backend.
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
