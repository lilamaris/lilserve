# lilserve

Java 21 + Spring Boot 4, Gradle Kotlin DSL.

## Commands

- Gate: `./gradlew spotlessApply` then `./gradlew clean build` must pass before declaring a task complete.

## Index

- `gradle/libs.versions.toml` — source of truth for dependencies and versions; reuse existing entries before adding one.

## Code

- English in source files; Korean only in `@DisplayName`, `@ArchTest` descriptions, and commit messages.
- Extract long conditions and long functions into small, well-named helpers.
- JavaDoc on every public method in `src/main`.
- Constructor injection, never field `@Autowired`. DTOs and value objects are `records`; JPA entities stay mutable.

## Tests

- Write tests only when strictly necessary; add a regression test for every bug fix.
- JUnit 5 + AssertJ. Mockito only for external dependencies; real objects for value objects.
- Unit: domain logic without a Spring context. Integration: only the DB/framework scope needed.
- One scenario per test; test public behavior and results, not internal methods or call order.
- English camelCase method names. Every Jupiter test method gets a Korean `@DisplayName`, and every test class gets a Korean class-level `@DisplayName`; `@ArchTest` rules use `.as("korean rule description")` without `@DisplayName`.
- Apply to tests you touch; bulk cleanup is a separate task.

## Commits

- Commit only when the user explicitly asks; a finished task is not permission, and a prior approval does not carry over.
- One logical change per commit: `<icon> <type>: <Korean summary>` — `✨ feat`, `🐛 fix`, `♻️ refactor`, `✅ test`, `📝 docs`, `🔧 chore`. Example: `✨ feat: 게시판 생성 기능 추가`.
