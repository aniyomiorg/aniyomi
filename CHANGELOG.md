# Changelog

All notable changes to this project will be documented in this file.

The format is a modified version of [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
- `Added` - for new features.
- `Changed ` - for changes in existing functionality.
- `Improved` - for enhancement or optimization in existing functionality.
- `Removed` - for now removed features.
- `Fixed` - for any bug fixes.
- `Other` - for technical stuff.

## [v0.18.1.1] - 2025-10-26
### Fixed

- Fix source Seasons/Hosters feature detection ([@hollowshiroyuki](https://github.com/hollowshiroyuki)) ([#2195](https://github.com/aniyomiorg/aniyomi/pull/2195))
- Fix shared download cache messing up downloaded episodes detection ([@choppeh](https://github.com/choppeh)) ([#2184](https://github.com/aniyomiorg/aniyomi/pull/2184))
- Fix Shikimori anime tracking ([@danya140](https://github.com/danya140)) ([#2205](https://github.com/aniyomiorg/aniyomi/pull/2205))

### Improved

- Make volume gesture the same sensitivity as brightness ([@jmir1](https://github.com/jmir1))

## [v0.18.1.0] - 2025-10-02
### Fixed

- Fix list view resetting scroll upon exiting child ([@quickdesh](https://github.com/quickdesh)) ([#1982](https://github.com/aniyomiorg/aniyomi/pull/1982))
- Fix episode number parsing ([@Secozzi](https://github.com/Secozzi)) ([#2096](https://github.com/aniyomiorg/aniyomi/pull/2096))
- Fix tracking menu not opening on add to library ([@Secozzi](https://github.com/Secozzi)) ([#2098](https://github.com/aniyomiorg/aniyomi/pull/2098))
- Fix stop/continue anime download button ([@Secozzi](https://github.com/Secozzi)) ([#2099](https://github.com/aniyomiorg/aniyomi/pull/2099))
- Fix creating/restoring backups between mihon and aniyomi ([@Secozzi](https://github.com/Secozzi)) ([#2117](https://github.com/aniyomiorg/aniyomi/pull/2117))

### Added

- Add support for new parameters from ext lib 16 ([@quickdesh](https://github.com/quickdesh)) ([#1982](https://github.com/aniyomiorg/aniyomi/pull/1982))
- Add player settings to the main settings screen ([@jmir1](https://github.com/jmir1)) ([#2081](https://github.com/aniyomiorg/aniyomi/pull/2081))
- Add seasons support ([@Secozzi](https://github.com/Secozzi)) ([#2095](https://github.com/aniyomiorg/aniyomi/pull/2095))

## [v0.18.0.1] - 2025-07-06
### Fixed

- Fix crash on migration ([@Secozzi](https://github.com/Secozzi)) ([#2079](https://github.com/aniyomiorg/aniyomi/pull/2079))

## [v0.18.0.0] - 2025-07-05
### Added

- Set mpv's media-title property ([@Secozzi](https://github.com/Secozzi)) ([#1672](https://github.com/aniyomiorg/aniyomi/pull/1672))
- Add mpvKt to external players ([@Secozzi](https://github.com/Secozzi)) ([#1674](https://github.com/aniyomiorg/aniyomi/pull/1674))
- Add video filters ([@abdallahmehiz](https://github.com/abdallahmehiz)) ([#1698](https://github.com/aniyomiorg/aniyomi/pull/1698))
- Show hours and minutes in relative time strings ([@jmir1](https://github.com/jmir1)) ([`1f3be7b`](https://github.com/aniyomiorg/aniyomi/commit/1f3be7b523136039b3b60213f2cee7959a9367d7))
  - Fix some issues with relative date calculations ([@jmir1](https://github.com/jmir1)) ([`03e1ecd`](https://github.com/aniyomiorg/aniyomi/commit/03e1ecd75edd2ea15dc8732ffeab32c6af26b202))
- Add better auto sub select ([@Secozzi](https://github.com/Secozzi)) ([#1706](https://github.com/aniyomiorg/aniyomi/pull/1706))
- Copy the file location when using ext downloader ([@quickdesh](https://github.com/quickdesh)) ([#1758](https://github.com/aniyomiorg/aniyomi/pull/1758))
- Replace player with mpvKt ([@Secozzi](https://github.com/Secozzi)) ([#1834](https://github.com/aniyomiorg/aniyomi/pull/1834), [#1855](https://github.com/aniyomiorg/aniyomi/pull/1855), [#1859](https://github.com/aniyomiorg/aniyomi/pull/1859), [#1860](https://github.com/aniyomiorg/aniyomi/pull/1860))
  - Move player preferences to separate section ([@Secozzi](https://github.com/Secozzi)) ([#1819](https://github.com/aniyomiorg/aniyomi/pull/1819))
- Implement video hosters ([@Secozzi](https://github.com/Secozzi)) ([#1892](https://github.com/aniyomiorg/aniyomi/pull/1892))
- Add size slider for the "List Display" Mode ([@MavikBow](https://github.com/MavikBow)) ([#1906](https://github.com/aniyomiorg/aniyomi/pull/1906))
  - Make the default list a set size and make browse list scale ([@MavikBow](https://github.com/MavikBow)) ([#1914](https://github.com/aniyomiorg/aniyomi/pull/1914))
- Allow negative brightness values (dimming) ([@jmir1](https://github.com/jmir1)) ([#1915](https://github.com/aniyomiorg/aniyomi/pull/1915))
- Add new lua functions for custom buttons ([@Secozzi](https://github.com/Secozzi)) ([#1980](https://github.com/aniyomiorg/aniyomi/pull/1980))
- Use timestamps provided by extensions ([@Secozzi](https://github.com/Secozzi)) ([#1983](https://github.com/aniyomiorg/aniyomi/pull/1983))
- Add titles to player sheets + consistency with More sheet ([@quickdesh](https://github.com/quickdesh)) ([#2015](https://github.com/aniyomiorg/aniyomi/pull/2015))
- Add script & script-opts editor to player settings ([@Secozzi](https://github.com/Secozzi)) ([#2019](https://github.com/aniyomiorg/aniyomi/pull/2019))

### Improved

- Show "Now" instead of "0 minutes ago" ([@Secozzi](https://github.com/Secozzi)) ([#1715](https://github.com/aniyomiorg/aniyomi/pull/1715))
- Add headers when using 1dm as external player ([@Secozzi](https://github.com/Secozzi)) ([#2032](https://github.com/aniyomiorg/aniyomi/pull/2032))

### Fixed

- Fix enhanced tracking for jellyfin ([@Secozzi](https://github.com/Secozzi)) ([#1656](https://github.com/aniyomiorg/aniyomi/pull/1656), [#1658](https://github.com/aniyomiorg/aniyomi/pull/1658))
- Use different status strings for anime trackers ([@jmir1](https://github.com/jmir1)) ([`74b32a3`](https://github.com/aniyomiorg/aniyomi/commit/74b32a3a0b323ed2f6f7929e131dcb4901e7bf9b))
- Fix Shikimori tracking for anime ([@jmir1](https://github.com/jmir1)) ([`58817c7`](https://github.com/aniyomiorg/aniyomi/commit/58817c724e2808072ff273329cee261d12084927))
- Group updates by date and not time ([@jmir1](https://github.com/jmir1)) ([`c83ebf3`](https://github.com/aniyomiorg/aniyomi/commit/c83ebf322f48d41ca1ad0105262160ecb7cde991))
- Fix airing time not showing ([@Secozzi](https://github.com/Secozzi)) ([#1720](https://github.com/aniyomiorg/aniyomi/pull/1720))
- Don't invalidate anime downloads on startup ([@Secozzi](https://github.com/Secozzi)) ([#1753](https://github.com/aniyomiorg/aniyomi/pull/1753))
- Fix hidden categories getting reset after delete/reorder ([@cuong-tran](https://github.com/cuong-tran)) ([#1780](https://github.com/aniyomiorg/aniyomi/pull/1780))
- Fix episode progress not being saved and duplicate tracks ([@perokhe](https://github.com/perokhe)) ([#1784](https://github.com/aniyomiorg/aniyomi/pull/1784), [#1785](https://github.com/aniyomiorg/aniyomi/pull/1785))
- Fix subtitle select not matching two letter language codes ([@Secozzi](https://github.com/Secozzi)) ([#1805](https://github.com/aniyomiorg/aniyomi/pull/1805))
- Fix potential intent extra npe ([@quickdesh](https://github.com/quickdesh)) ([#1816](https://github.com/aniyomiorg/aniyomi/pull/1816))
- Fix history date header duplication ([@quickdesh](https://github.com/quickdesh)) ([#1817](https://github.com/aniyomiorg/aniyomi/pull/1817))
- Fix migrations not getting context correctly ([@Secozzi](https://github.com/Secozzi)) ([#1820](https://github.com/aniyomiorg/aniyomi/pull/1820))
- Fix various issues due to replacing the player with mpvKt
  - Fix gesture seeking not seeking to start and end ([@perokhe](https://github.com/perokhe)) ([#1865](https://github.com/aniyomiorg/aniyomi/pull/1865))
  - Fix crash when opening player settings in tablet ui ([@Secozzi](https://github.com/Secozzi)) ([#1868](https://github.com/aniyomiorg/aniyomi/pull/1868))
  - Fix episode list in player not respecting filters & crash when exiting while stuff is loading ([@Secozzi](https://github.com/Secozzi)) ([#1869](https://github.com/aniyomiorg/aniyomi/pull/1869))
  - Fix episode being marked as seen at start ([@perokhe](https://github.com/perokhe)) ([#1871](https://github.com/aniyomiorg/aniyomi/pull/1871))
  - Fix player not being paused when loading tracks after changing quality ([@Secozzi](https://github.com/Secozzi)) ([#1878](https://github.com/aniyomiorg/aniyomi/pull/1878))
  - Fix lag when toggling player ui ([@Secozzi](https://github.com/Secozzi)) ([#1887](https://github.com/aniyomiorg/aniyomi/pull/1887))
  - Fix audio selection not working on external audio tracks ([@Secozzi](https://github.com/Secozzi)) ([#1901](https://github.com/aniyomiorg/aniyomi/pull/1901))
  - Reset "hide player controls time" when pressing custom button ([@Secozzi](https://github.com/Secozzi)) ([#1902](https://github.com/aniyomiorg/aniyomi/pull/1902))
  - Don't unpause on share and save ([@Secozzi](https://github.com/Secozzi)) ([#1905](https://github.com/aniyomiorg/aniyomi/pull/1905))
  - Fix player pausing with gesture seek ([@perokhe](https://github.com/perokhe)) ([#1916](https://github.com/aniyomiorg/aniyomi/pull/1916))
  - Fix potential npe issues with mpv-lib ([@Secozzi](https://github.com/Secozzi)) ([#1921](https://github.com/aniyomiorg/aniyomi/pull/1921))
  - Dismiss chapter sheet on chapter select ([@Secozzi](https://github.com/Secozzi)) ([#1976](https://github.com/aniyomiorg/aniyomi/pull/1976))
  - Fix some issues caused by [`10e28cc`](https://github.com/aniyomiorg/aniyomi/commit/10e28cc4092758cf38d27cc14aadf539698738f2) ([@Secozzi](https://github.com/Secozzi)) ([#1981](https://github.com/aniyomiorg/aniyomi/pull/1981))
  - Fix npe issue caused in player controls ([@Secozzi](https://github.com/Secozzi)) ([#1986](https://github.com/aniyomiorg/aniyomi/pull/1986))
- Replace some manga strings with respective anime strings ([@perokhe](https://github.com/perokhe)) ([#1864](https://github.com/aniyomiorg/aniyomi/pull/1864))
- Open correct tab from extension update notifications ([@jmir1](https://github.com/jmir1)) ([`161471d`](https://github.com/aniyomiorg/aniyomi/commit/161471d94a2350c0c983eeeccd3b7ac0dc66d429))
- Fix sub-auto not loading all external subtitle files ([@perokhe](https://github.com/perokhe)) ([#1866](https://github.com/aniyomiorg/aniyomi/pull/1866))
- Fix `ALSearchItem.format` nullability ([@Secozzi](https://github.com/Secozzi)) ([#1910](https://github.com/aniyomiorg/aniyomi/pull/1910))
- Don't format mpv preferences ([@Secozzi](https://github.com/Secozzi)) ([#1939](https://github.com/aniyomiorg/aniyomi/pull/1939))
- Prevent crash on app death when watching in external player ([@Secozzi](https://github.com/Secozzi)) ([#1945](https://github.com/aniyomiorg/aniyomi/pull/1945))
- Don't run unnecessary stuff when exiting the player ([@Secozzi](https://github.com/Secozzi)) ([#1961](https://github.com/aniyomiorg/aniyomi/pull/1961))
- Fix some downloader issues ([@Secozzi](https://github.com/Secozzi)) ([#1964](https://github.com/aniyomiorg/aniyomi/pull/1964))
  - Fix downloader not working for certain types of tracks & duration sometimes not being logged ([@Secozzi](https://github.com/Secozzi)) ([#2001](https://github.com/aniyomiorg/aniyomi/pull/2001))
- Fix some issues with intro skip length ([@jmir1](https://github.com/jmir1)) ([`72cac57`](https://github.com/aniyomiorg/aniyomi/commit/72cac57d8e66366cbc0f3106eb351c82250c460b), [`25dd3ea`](https://github.com/aniyomiorg/aniyomi/commit/25dd3ea69fb217de7b0485c29e4a9b970737fd45))
- Force clipboard to use UI thread when copying path for external players ([@quickdesh](https://github.com/quickdesh)) ([#1994](https://github.com/aniyomiorg/aniyomi/pull/1994))
- Use application directory for storing files used by mpv ([@Secozzi](https://github.com/Secozzi)) ([#1995](https://github.com/aniyomiorg/aniyomi/pull/1995))
- Update backup warning string (follow Mihon) ([@cuong-tran](https://github.com/cuong-tran)) ([#2012](https://github.com/aniyomiorg/aniyomi/pull/2012))
- Fix issues with episode deletion & more ([@quickdesh](https://github.com/quickdesh)) ([#2017](https://github.com/aniyomiorg/aniyomi/pull/2017))
- Fix vertical slider width issues and shift boost volume value to slider ([@quickdesh](https://github.com/quickdesh)) ([#2018](https://github.com/aniyomiorg/aniyomi/pull/2018))
- Fix MyAnimeList login ([@choppeh](https://github.com/choppeh)) ([#2035](https://github.com/aniyomiorg/aniyomi/pull/2035))
- Call sort methods for videos and hosters ([@cuong-tran](https://github.com/cuong-tran)) ([#2058](https://github.com/aniyomiorg/aniyomi/pull/2058))
- Invalidate preferred languages in settings ([@Secozzi](https://github.com/Secozzi)) ([#2075](https://github.com/aniyomiorg/aniyomi/pull/2075))
- Fix crash when using sort by airing time ([@quickdesh](https://github.com/quickdesh)) ([#2076](https://github.com/aniyomiorg/aniyomi/pull/2076))

### Other

- Merge from mihon until 0.16.5 ([@Secozzi](https://github.com/Secozzi)) ([#1663](https://github.com/aniyomiorg/aniyomi/pull/1663))
  - Merge until latest mihon commits ([@Secozzi](https://github.com/Secozzi)) ([#1693](https://github.com/aniyomiorg/aniyomi/pull/1693))
  - Merge until latest mihon commits (v0.17.0) ([@Secozzi](https://github.com/Secozzi)) ([#1804](https://github.com/aniyomiorg/aniyomi/pull/1804))
  - Merge until latest mihon commits (v0.18.0) ([@Secozzi](https://github.com/Secozzi)) ([#1863](https://github.com/aniyomiorg/aniyomi/pull/1863))
- Remove ACRA crash report analytics ([@jmir1](https://github.com/jmir1)) ([`d3c6a15`](https://github.com/aniyomiorg/aniyomi/commit/d3c6a159d82ca239c10e8f5822c3b2046c5545f2), [`5ae35c8`](https://github.com/aniyomiorg/aniyomi/commit/5ae35c891b90ae927200185641240280effaf667))

## [v0.16.4.3] - 2024-07-01
### Fixed

- Fix extensions disappearing due to errors with the ClassLoader ([@jmir1](https://github.com/jmir1)) ([`959f84a`](https://github.com/aniyomiorg/aniyomi/commit/959f84ab41859f90c458c076d83d363ae086e47f))

## [v0.16.4.2] - 2024-07-01
### Fixed

- Hotfix to eliminate all proguard issues causing errors and crashes ([@jmir1](https://github.com/jmir1)) ([`a8cd723`](https://github.com/aniyomiorg/aniyomi/commit/a8cd7233dfdf26c98ff86b1871a7ac5774379b5e), [`a7644c2`](https://github.com/aniyomiorg/aniyomi/commit/a7644c268153fc0b9f10c27202591f960c6f6384), [`5045fa1`](https://github.com/aniyomiorg/aniyomi/commit/5045fa18ce5a1faa2130f1a33609e43d8453f078))

## [v0.16.4.1] - 2024-07-01
### Fixed

- Hotfix release to address errors with extensions ([@jmir1](https://github.com/jmir1)) ([`98d2528`](https://github.com/aniyomiorg/aniyomi/commit/98d252866e17beba7d9a4d094797e23c05ead6c1))

## [v0.16.4.0] - 2024-07-01
### Fixed

- Fix pip not broadcasting intent in A14+ ([@quickdesh](https://github.com/quickdesh)) ([#1603](https://github.com/aniyomiorg/aniyomi/pull/1603))
- Fix advanced player settings crash in android ≤ 10 ([@perokhe](https://github.com/perokhe)) ([#1627](https://github.com/aniyomiorg/aniyomi/pull/1627))

### Improved

- Hide the skip intro button if the skipped amount == 0 ([@abdallahmehiz](https://github.com/abdallahmehiz)) ([#1598](https://github.com/aniyomiorg/aniyomi/pull/1598))

### Other

- Merge from mihon until mihon 0.16.2 ([@Secozzi](https://github.com/Secozzi)) ([#1578](https://github.com/aniyomiorg/aniyomi/pull/1578))
  - Merge from mihon until 0.16.4 ([@Secozzi](https://github.com/Secozzi)) ([#1601](https://github.com/aniyomiorg/aniyomi/pull/1601))
