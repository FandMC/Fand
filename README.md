# Fand

A patched-vanilla Minecraft server for Java Edition 26.1.2 with a fresh,
type-safe plugin API. Spiritually a sibling of Paper: same mechanism (work on
the decompiled vanilla server, ship as a chain of patches), different surface
(no Bukkit compatibility, modernised core APIs).

**Current status**: Phase 0 complete, Phase 1 late-stage. Build pipeline is
functional; runtime integration with patched vanilla code is active across
plugins, events, commands, scheduling, configuration, inventories, and proxy
forwarding. The remaining Phase 1 work is hardening, documentation, and
end-to-end validation rather than initial runtime scaffolding.

## Modules

| Module        | Role                                                                 |
|---------------|----------------------------------------------------------------------|
| `fand-api`    | Public plugin API. Stable surface, no implementation details.        |
| `fand-server` | Server runtime. Hosts the patched vanilla code via paperweight.      |
| `fandclip`    | End-user launcher. Downloads vanilla bundler on first run.           |
| `fand-server/patches/` | Canonical paperweight patch set applied on top of vanilla. |

## Build pipeline

26.1+ ships unobfuscated, so the pipeline is shorter than Paper's legacy flow:
no remap stage needed. We use **paperweight-core** to manage the workflow.

```
piston-meta → paperweight → vanilla-bundler.jar
            → unbundleServer → vanilla-server.jar
            → decompileServer → decompiled/ (via Mache patches)
            → applyPatches → fand-server/src/minecraft/ (git-applied from fand-server/patches/)
edit sources → rebuildPatches → fand-server/patches/ (updated)
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
- API interfaces defined (events, plugins, scheduler, commands, permissions,
  worlds/entities, inventories/items)
- Runtime implementation exists for plugins, events, scheduler, commands,
  permissions, config reload, inventories/items, and proxy forwarding
- Vanilla integration underway through ordered feature patches

**Immediate engineering focus**:
- Harden plugin failure paths and resource cleanup
- Reduce allocations and repeated runtime lookups in hot event patches
- Clarify scheduler thread/tick semantics in public API docs
- Stabilize local/CI build memory settings for paperweight workflows

See `PROJECT_PROPOSAL.md` for the full roadmap and `CODING_STANDARDS.md` for
development guidelines.

## License

Fand is licensed under the GNU General Public License v3.0. See `LICENSE` for
the full license text.
