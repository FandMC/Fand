# Fand Coding Standards

Authoritative for everything under this repository. When other docs disagree
with this file, this file wins.

## Language

- Java 25, no preview features.
- Source and compiled bytecode target Java 25 (`options.release = 25`).
- UTF-8 source encoding, LF line endings.

## Comments, docs, logs

- All comments, JavaDoc, log messages, exception messages, and identifiers are
  written in **English**. No exceptions.
- Default to writing **no comments**. Add one only when the *why* is non-obvious:
  a hidden constraint, a subtle invariant, a workaround for a specific bug, or
  behaviour that would surprise a reader.
- Don't explain *what* the code does — well-named identifiers do that.
- Don't reference the current task, fix, ticket, or callers in comments. Those
  belong in the commit message.

## Style

- Four-space indent. No tabs.
- Wrap at 120 columns; soft limit, not strictly enforced.
- One top-level type per file; the file name matches the type.
- Imports: no wildcards. Group as `java.*`, third-party, then project. Static
  imports last.
- Prefer `var` for local variables when the right-hand side makes the type
  obvious. Don't use `var` for primitives or numeric literals where the
  inferred type would be ambiguous.

## API design

- The `fand-api` module is the public surface. Treat its package layout and
  signatures as semver-bound once a release ships.
- Use `@NullMarked` packages plus `@Nullable` for nullable references; don't
  rely on `Optional` for fields or parameters.
- Prefer `record` for data carriers, `sealed` hierarchies for closed type sets,
  and immutable collections for return values.
- Don't expose Bukkit, NMS, or vanilla types from `fand-api`.

## Concurrency

- Document the threading model of every public method that doesn't run on the
  main thread.
- Default to immutable shared state. Use `java.util.concurrent` primitives over
  hand-rolled locking.
- Adjacent concurrency / race-condition issues spotted while editing should be
  fixed in the same change, not deferred.

## Error handling

- Validate at system boundaries (network, disk, plugin loading). Trust internal
  callers; don't re-validate invariants the type system already enforces.
- Throw, don't return error sentinels. Wrap checked exceptions only when the
  caller cannot reasonably react to the original type.
- Never swallow exceptions silently. If catching is the right call, log at
  `WARN` or higher and explain why in a comment.

## Tests

- JUnit 5 + AssertJ. Mockito only when a real implementation isn't feasible.
- A test class lives at the same package path as the type under test.
- Integration tests that boot the patched server live under
  `fand-server/src/integrationTest/`; pure unit tests under `src/test/`.

## Patches

- One logical change per patch. Smaller patches rebase cleaner across vanilla
  updates.
- Patch commit messages follow the same style as a regular commit: imperative
  subject, optional body explaining the *why*.
- Never edit `.fand-work/decompiled/` directly. Always work in
  `.fand-work/sources/` so the change is captured by `rebuildPatches`.
