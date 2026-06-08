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
- Don't explain *what* the code does; well-named identifiers do that.
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

## Design principles

- Keep the implementation as small as the current requirement allows. Prefer
  direct, boring code over abstractions that only serve possible future work.
- Introduce an abstraction only when it removes real duplication, isolates a
  volatile boundary, or matches an existing local pattern.
- Keep public APIs narrow and explicit. Avoid "just in case" methods, flags, or
  extension points until there is a concrete caller.
- Prefer deterministic, data-driven code for large vanilla surfaces. Hand-written
  code is fine for policy and lifecycle rules; it is not fine for hundreds of
  repeated vanilla mappings.
- When a change touches a shared subsystem, fix adjacent correctness issues in
  the same area if they affect the new behaviour. Do not hide unrelated
  refactors inside feature commits.

## API design

- The `fand-api` module is the public surface. Treat its package layout and
  signatures as semver-bound once a release ships.
- Use `@NullMarked` packages plus `@Nullable` for nullable references; don't
  rely on `Optional` for fields or parameters.
- Prefer `record` for data carriers, `sealed` hierarchies for closed type sets,
  and immutable collections for return values.
- Don't expose Bukkit, NMS, or vanilla types from `fand-api`.
- Every public lifecycle-bound registration must have a plugin-scoped entry
  point that is automatically cleaned up on plugin disable.

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

## Data generation

- Generated sources are build artifacts. Keep the generator source-controlled;
  do not commit generated output unless that output is intentionally part of the
  public source tree.
- Generated output must be deterministic: stable ordering, stable formatting,
  UTF-8 encoding, and no dependency on local filesystem ordering.
- Keep the generator split by responsibility: source loading, source parsing,
  metadata/spec definitions, and artifact writing belong in separate types.
- Prefer structured parsing when the source shape matters. Regex extraction is
  acceptable for simple registry constants; packet structures and constructors
  require a stronger model or explicit metadata.
- Represent repeated vanilla surfaces as declarations/specs plus small writers.
  Special cases belong in explicit override data or narrowly named helper
  methods, not scattered across the writer.
- A generator change must prove it still produces the current API shape. At
  minimum run `:fand-api:generateFandData` and the affected API tests.

## Packet and network API

- The packet API is version-controlled against the Minecraft version Fand ships.
  It may hard-code the current vanilla packet set, but the hard-coded surface
  must be generated or validated from the current sources.
- `PacketType`, typed packet views, and server-side vanilla mappings must come
  from the same packet metadata source. A view that is not reachable through
  `PacketType#viewType()` is dead API and should not be added.
- Packet views must expose API-safe values only. No NMS, Bukkit, or vanilla
  classes may leak through `fand-api`; opaque fields may be exposed as
  `Object` only when they are preserved unchanged.
- Packet replacement is allowed only when the implementation can reconstruct the
  vanilla packet exactly. Record packets may use canonical constructors; class
  packets are read-only by default unless they have an explicit dedicated codec.
- Do not use generic reflection to make unknown class packets replaceable.
  Field order, constructor order, and derived state are not API contracts.
- Interceptors and custom packet handlers run on the connection I/O thread.
  They must not block or touch main-thread world state directly.
- Custom packet channel direction is a contract: inbound definitions require a
  handler, outbound definitions must not register an inbound handler, and
  bidirectional definitions must satisfy both sides.
- Registration handles must remove exactly the resource they installed. Closing
  an old registration must never remove a newer registration for the same key.

## Tests

- JUnit 5 + AssertJ. Mockito only when a real implementation isn't feasible.
- A test class lives at the same package path as the type under test.
- Integration tests that boot the patched server live under
  `fand-server/src/integrationTest/`; pure unit tests under `src/test/`.
- Tests for generated APIs should verify both coverage and binding: every
  generated public type is reachable through the intended public entry point,
  and every public entry point has the generated backing metadata it needs.

## Patches

- One logical change per patch. Smaller patches rebase cleaner across vanilla
  updates.
- Patch commit messages follow the same style as a regular commit: imperative
  subject, optional body explaining the *why*.
- Never edit `.fand-work/decompiled/` directly. Always work in
  `.fand-work/sources/` so the change is captured by `rebuildPatches`.
