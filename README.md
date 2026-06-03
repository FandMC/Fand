# Fand

A patched-vanilla Minecraft server for Java Edition 26.1.2 with a fresh,
type-safe plugin API. Spiritually a sibling of Paper: same mechanism (work on
the decompiled vanilla server, ship as a chain of patches), different surface
(no Bukkit compatibility, modernised core APIs).

**Current status**: Phase 0 complete, Phase 1 in progress. Build pipeline is
functional; runtime integration with patched vanilla code is underway.

## Modules

| Module        | Role                                                                 |
|---------------|----------------------------------------------------------------------|
| `fand-api`    | Public plugin API. Stable surface, no implementation details.        |
| `fand-server` | Server runtime. Hosts the patched vanilla code via paperweight.      |
| `fandclip`    | End-user launcher. Downloads vanilla bundler on first run.           |
| `patches/`    | Ordered set of unified-diff patches applied on top of vanilla.       |

## Build pipeline

26.1+ ships unobfuscated, so the pipeline is shorter than Paper's legacy flow:
no remap stage needed. We use **paperweight-core** to manage the workflow.

```
piston-meta → paperweight → vanilla-bundler.jar
            → unbundleServer → vanilla-server.jar
            → decompileServer → decompiled/ (via Mache patches)
            → applyPatches → fand-server/src/minecraft/ (git-applied patches/)
edit sources → rebuildPatches → patches/ (updated)
```

Paperweight tasks are automatically available in the `:fand-server` subproject.
The patched Minecraft code lives in `fand-server/src/minecraft/` after setup.

## Status

**Phase 0 (Build Infrastructure)**: ✅ Complete
- Paperweight integration functional
- Patch system operational
- Module structure established
- First sample patch applied

**Phase 1 (Core API Design)**: 🚧 In Progress
- API interfaces defined (events, plugins, scheduler, commands)
- Runtime implementation pending
- Integration with vanilla server underway

See `PROJECT_PROPOSAL.md` for the full roadmap and `CODING_STANDARDS.md` for
development guidelines.
