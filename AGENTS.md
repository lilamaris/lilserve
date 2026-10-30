# lilserve

Java 21 + Spring Boot 4, Gradle Kotlin DSL.

## Commands

- Gate: `./gradlew spotlessApply` then `./gradlew clean build` must pass before declaring a task complete.

## Index

- `gradle/libs.versions.toml` — source of truth for dependencies and versions; reuse existing entries before adding one.
- `build-logic/convention` — convention plugins; apply them in the root build, never configure java/jacoco/spotless directly.
- `settings.gradle.kts` — module layout source of truth; new modules apply `java.module` (plain Java) or `spring.base`/`spring.platform`, package root `com.lilamaris.lilserve.<context>`.
- `.github/` — issue and PR templates; every change starts from an issue.

## Code

- English in source files; Korean only in `@DisplayName`, `@ArchTest` descriptions, and commit messages.
- Extract long conditions and long functions into small, well-named helpers.
- JavaDoc on every public method in `src/main`.
- Constructor injection, never field `@Autowired`. DTOs and value objects are `records`; JPA entities stay mutable.

## Migrations

- Flyway scripts live in the owning module: `src/main/resources/db/migration/V<n>__<snake_case>.sql`; never edit an applied migration, add the next one.
- Uniqueness, checks, and FKs are enforced in SQL, not only in Java validation (`V1__create_room_seat.sql` as the pattern).

## Tests

- Write tests only when strictly necessary; add a regression test for every bug fix.
- `check` enforces 80% line coverage (`jacocoTestCoverageVerification` via `java.coverage`); add real tests, not filler, to meet it.
- `coverage.minimum` and `coverage.excludes` are per-module gradle properties; an exclusion needs a stated reason in the PR, and never lower the minimum to pass `check`.
- JUnit 5 + AssertJ. Mockito only for external dependencies; real objects for value objects.
- Unit: domain logic without a Spring context. Integration: only the DB/framework scope needed.
- One scenario per test; test public behavior and results, not internal methods or call order.
- English camelCase method names. Every Jupiter test method gets a Korean `@DisplayName`, and every test class gets a Korean class-level `@DisplayName`; `@ArchTest` rules use `.as("korean rule description")` without `@DisplayName`.
- Apply to tests you touch; bulk cleanup is a separate task.

## Issues and PRs

- Every change starts from an issue: bugs use `.github/ISSUE_TEMPLATE/bug_report.md` (repro steps required), features use `feature_request.md`; its `완료 기준` checklist is the scope — do not exceed it.
- One branch per issue: `<type>/<kebab-scope>` reusing the commit types. Example: `feat/reservation-domain`, `fix/room-delete`.
- PR body follows `.github/PULL_REQUEST_TEMPLATE.md` (Korean): 변경 내용, 검증, 관련 이슈. Link issues with `Closes #N` in the PR body, not in the commit message.
- `검증` lists only what actually ran: `./gradlew spotlessApply`, `./gradlew clean build`, targeted `:module:test`. A skipped check needs its reason stated; never claim verification you did not run.
- A bug-fix PR is not opened without a regression test (see Tests).

## Commits

- Commit only when the user explicitly asks; a finished task is not permission, and a prior approval does not carry over.
- One logical change per commit: `<icon> <type>: <Korean summary>` — `✨ feat`, `🐛 fix`, `♻️ refactor`, `✅ test`, `📝 docs`, `🔧 chore`. Example: `✨ feat: 게시판 생성 기능 추가`.
