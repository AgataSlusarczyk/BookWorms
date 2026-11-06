# Contributing to BookWorms

## The one rule

Never commit directly to `main`. Every change goes through its own branch and a Pull Request.

```
branch → commits → push → Pull Request → review → merge
```

## One-time setup

```
git clone https://github.com/AgataSlusarczyk/BookWorms.git
cd BookWorms
git config user.name  "Your Name"
git config user.email "your-github-email@example.com"
```

- **Server** (`server/`): requires JDK 21 and Docker Desktop running. Start with `./gradlew bootRun` – Postgres starts automatically.
- **Database**: Postgres runs in Docker on `localhost:5434` (database, user `bookworms`, password `secret`), so you can connect to it from DataGrip or `psql`. The schema comes from the Flyway migrations in `server/src/main/resources/db/migration` – never edit a merged migration, add a new `V<n>__...sql` instead.
- **Android** (`android/`): open the `android` folder in Android Studio and run on an emulator. The emulator reaches the local server at `http://10.0.2.2:8080`. On Android 16+ the debug app asks for **local network** access on its first start – tap Allow, or the requests to the server time out (`scripts/run-android.sh` grants it for you).
- **VS Code**: `Cmd+Shift+B` runs **Start everything** – the database with the server and, at the same time, the app on the emulator (it starts the first emulator from Android Studio if nothing is connected). The same from the terminal: `./scripts/start.sh` (Ctrl+C stops the server), or only the app: `./scripts/run-android.sh`. The single steps (and **Database: reset**, which deletes all data) are under `Tasks: Run Task`.

## Daily workflow

**1. Create a branch from the latest `main`.**

```
git switch main
git pull
git switch -c feat/android-login
```

**2. Commit small and often.** Messages in English, format `type(area): what changed`, where area is `server`, `android` or `docs`:

```
feat(server): add loan request endpoint
fix(android): crash when offline
docs: describe loan statuses
```

**3. Push and open a Pull Request.**

```
git push -u origin feat/android-login
```

On GitHub click **Compare & pull request**. Use the commit format for the title, and describe what changed and how you tested it. Not ready yet? Open it as a **Draft**.

**4. Review and merge.** At least one approval is required. Push fixes to the same branch – the PR updates itself. Once approved, use **Squash and merge** and delete the branch.
