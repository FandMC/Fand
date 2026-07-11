# Third Party Notices

## Lithium

Fand contains performance optimizations adapted from or directly inspired by
Lithium by CaffeineMC.

Lithium is licensed under the GNU Lesser General Public License version 3
(GNU LGPLv3). The upstream project and license text are available at:

- https://github.com/CaffeineMC/lithium
- https://github.com/CaffeineMC/lithium/blob/develop/LICENSE.md

The Fand source files that adapt Lithium optimization work include explicit
inline notes near the relevant implementation points. Current adapted areas
include explosion raycasting/caching, primitive explosion resistance paths,
entity and block collision fast paths, chunk tick scheduling, random ticking
section metadata, fluid spreading, NBT map allocation, and selected hot
collection utilities.

## Moonrise

Fand contains performance optimizations adapted from or inspired by Moonrise
by Tuinity.

Moonrise is licensed under the GNU General Public License version 3
(GNU GPLv3). The upstream project and license text are available at:

- https://github.com/Tuinity/Moonrise
- https://github.com/Tuinity/Moonrise/blob/master/LICENSE.md

The Fand source files that adapt Moonrise optimization work include explicit
inline notes near the relevant implementation points. Current adapted areas
include thread-unsafe private random sources, mob spawning biome-cost lookup
avoidance, and chunk block/biome read hot paths.
