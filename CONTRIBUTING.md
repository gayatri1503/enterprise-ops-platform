# Contributing

## Branching (keep it simple)

- `main` is always working and demo-able. Nobody pushes to it directly.
- Work on short-lived branches: `feature/<area>-<what>`, e.g. `feature/auth-login`, `fix/core-pagination`.
- Open a Pull Request into `main`; at least **one teammate reviews**; CI must pass; then **squash-merge**.
- Keep PRs small (a day or two of work). Merge often so nobody drifts.

## Commit messages

`type(scope): message`, e.g. `feat(auth): add login endpoint`, `fix(core): validate priority`, `docs(events): add version field`.
Types: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`.

## Golden rules for working in parallel

1. **Stay in your folder.** Ownership is in `.github/CODEOWNERS`. Need a change elsewhere? Ask the owner or open a PR they review.
2. **Contracts first.** Anything that crosses a service boundary (REST endpoint, event, JWT claim) is written in `docs/api-contract.md` or `docs/events.md` *before* it is coded. Change a contract = update the doc in the same PR and tag the affected teammates.
3. **Never touch another service's database.** Talk via REST or events only.
4. **No secrets in Git.** Use `.env` (ignored). Only `.env.example` is committed.
5. **Pull before you start, rebase before you PR:** `git pull --rebase origin main`.

## Setup for teammates

    git clone <repo-url>
    cd enterprise-ops-platform
    cp .env.example .env
    docker compose up -d

## GitHub settings (repo admin does this once)

Settings -> Branches -> add rule for `main`: require a pull request, require 1 approval, require status checks (CI) to pass, block force pushes.
