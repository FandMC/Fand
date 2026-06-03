# Fand Patches

This directory holds the canonical, version-controlled diffs that turn the
decompiled vanilla server into Fand. Every file is a single git commit exported
in unified-diff form by `./gradlew rebuildPatches`.

## Naming convention

```
NNNN-Short-subject.patch
```

- `NNNN` is the application order (`0001`, `0002`, ...). Lower numbers are
  applied first.
- `Short-subject` mirrors the commit subject with non-alphanumerics replaced by
  hyphens.

## Workflow

1. `./gradlew applyPatches` — reset `.fand-work/sources` to the `vanilla`
   commit and replay every patch in order.
2. Edit files under `.fand-work/sources`. Each logical change becomes one git
   commit on top of the existing patch chain.
3. `./gradlew rebuildPatches` — export the new commit history back into this
   directory, overwriting previous `.patch` files.

## Conflicts

`applyPatches` aborts on the first failed patch and leaves `.fand-work/sources`
in the partially-applied state for inspection. Resolve manually in that working
tree (the conflicting patch's stem ends up in the working set), commit, and run
`rebuildPatches` to refresh the on-disk patches.
