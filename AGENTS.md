# Repository Guidelines

## Project Structure & Module Organization
This repository is a Maven multi-module backend with a separate Vite frontend. The root `pom.xml` aggregates `wimoor-common`, `wimoor-admin`, `wimoor-gateway`, `wimoor-erp`, `wimoor-amazon`, `wimoor-amazon-adv`, `wimoor-api`, and `wimoor-modules`. Java code lives in `*/src/main/java`, resources in `*/src/main/resources`, and backend tests in `*/src/test/java`. Frontend code is under `wimoorui/src`, static files under `wimoorui/public`, and bootstrap/config templates under `init-config/`.

## Build, Test, and Development Commands
Use Maven from the repository root for backend work:

- `mvn clean package -DskipTests` builds all backend modules.
- `mvn test` runs available backend tests across modules.
- `mvn -pl wimoor-erp/wimoor-erp-proxy test` runs tests for one module.

Use npm inside `wimoorui` for frontend work:

- `npm install` installs frontend dependencies.
- `npm run dev` starts the Vite dev server.
- `npm run build` creates a production bundle.

MySQL, Redis, Nacos, and Seata are expected for full local integration; see `README.md` and `init-config/`.

## Coding Style & Naming Conventions
Follow the style already present in the touched module. Java packages use `com.wimoor`; classes are `PascalCase`; methods and fields are `camelCase`; indentation is 4 spaces in backend code. Vue components use `PascalCase` directories or filenames such as `Pagination/index.vue`. Router modules commonly end in `Router.js`; utility files stay lowercase or kebab-case to match neighbors. No repository-wide lint or format script is defined in the current snapshot, so avoid unrelated reformatting.

## Testing Guidelines
Backend tests use Spring Boot Test and JUnit 5, with files such as `ProxyApplicationTests.java` under `src/test/java`. Name new tests `*Test.java` or `*Tests.java` and keep them close to the module you changed. The frontend currently has no automated test script; include manual verification steps for UI changes in your PR.

## Commit & Pull Request Guidelines
Git commit messages must use Chinese, and should stay concise, imperative, and scoped by module when helpful, for example `erp: 修复采购计划筛选`. Keep unrelated changes in separate commits. PRs should list affected modules, config changes, verification commands, and screenshots for `wimoorui` UI changes.

## Security & Configuration Tips
Do not commit secrets or environment-specific credentials. Reuse the templates in `init-config/mysql`, `init-config/nacos`, and `init-config/seata` when documenting setup changes, and note any new ports, queues, or configuration keys in the PR description.
