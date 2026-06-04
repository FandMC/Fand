# Fand Patches

This directory holds the canonical, version-controlled diffs that turn the
patched vanilla server into Fand. `rebuildPatches` exports one git commit per
feature patch under `features/`, alongside any file patches under `sources/`
and `resources/`.

## Layout

- `features/` — ordered git-format patches (`0001-...patch`, `0002-...patch`)
- `sources/` — per-file source patches when paperweight rebuilds file patches
- `resources/` — per-file resource patches

## Workflow

1. `./gradlew :fand-server:applyPatches` — reset `fand-server/src/minecraft/`
   and replay the configured patch set.
2. Edit files under `fand-server/src/minecraft/`. Each logical change becomes a
   git commit inside `fand-server/src/minecraft/java/.git`.
3. `./gradlew :fand-server:rebuildPatches` — export the current commit history
   back into this directory.

## Notes

- The canonical patch location is `fand-server/patches/`, not the repository
  root.
- Source changes without a corresponding `rebuildPatches` run are not the
  source of truth.
