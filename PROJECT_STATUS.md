# Aniyomi Plus - Nested Categories & Entry Reordering

## Project Status: Partially Complete

The backend is complete. UI integration is partially done. Build automation is working via GitHub Actions.

---

## What Was Built

### Backend (Done ✅)

**1. Database Layer**
- `mangas_categories.sq` / `animes_categories.sq`
  - `insert` now auto-calculates `sort_order` for new entries
  - Added `getEntriesInCategory` query
- `libraryView.sq` / `animelibView.sq`
  - Added `sortOrder` column

**2. Domain Layer**
- `LibraryManga.kt` / `LibraryAnime.kt` - Added `sortOrder: Long = 0` field
- `MangaLibrarySortMode.kt` / `AnimeLibrarySortMode.kt`
  - Added `Custom` sort type (flag: `0b00100000`)
- `MangaCategoryRepository.kt` / `AnimeCategoryRepository.kt`
  - Added `getEntriesInCategory()` and `updateSortOrder()` methods
- `ReorderMangaEntry.kt` / `ReorderAnimeEntry.kt`
  - `moveUp(mangaId, categoryId)` - Swap with previous entry
  - `moveDown(mangaId, categoryId)` - Swap with next entry
  - `moveTo(mangaId, categoryId, position)` - Move to specific position

**3. Data Layer**
- `MangaMapper.kt` / `AnimeMapper.kt` - Updated to include `sortOrder`

**4. UI Layer (Partial)**
- `MangaLibrarySettingsDialog.kt` / `AnimeLibrarySettingsDialog.kt` - Added Custom sort option
- `MangaLibraryScreenModel.kt` / `AnimeLibraryScreenModel.kt`
  - `applySort()` uses `sortOrder` when sort type is Custom
  - `moveSelectionUp()` / `moveSelectionDown()` call `reorderMangaEntry`

---

## What Still Needs Work

### 1. Entry Reordering UI

**Current State:** Works in selection mode only (long-press → select → move)

**To Enable Drag-and-Drop:**
1. Add drag handles to library entry items in:
   - `MangaLibraryContent.kt` / `AnimeLibraryContent.kt`
   - `CommonEntryItem.kt`

2. Wire up callbacks in:
   - `MangaLibraryTab.kt` - Pass `onMoveMangaUp`/`onMoveMangaDown` to content
   - `AnimeLibraryTab.kt` - Same for anime

3. Use existing reorderable library:
   ```kotlin
   import sh.calvin.reorderable.ReorderableItem
   ```

### 2. Nested Categories Navigation

**Current State:** `currentCategoryId`, `onEnterCategory()`, `goBackToParent()` exist

**To Fix:**
- Ensure `applySort()` reads from `currentCategoryId` when nested
- Add back button UI in category headers
- Test category drilling (root → subcategory → entry)

### 3. Category Thumbnails

**Current State:** `thumbnailUrl` field exists in `Category` model

**To Enable:**
- Add `AsyncImage` display in `CategoryListItem.kt` (may already exist)
- Ensure thumbnail URL persists via `CategoryUpdate`
- Add thumbnail URL input in category edit dialog

---

## Key Files to Modify

| Feature | Files |
|---------|-------|
| Sort Settings UI | `MangaLibrarySettingsDialog.kt`, `AnimeLibrarySettingsDialog.kt` |
| Library Content | `MangaLibraryContent.kt`, `AnimeLibraryContent.kt` |
| Entry Items | `CommonEntryItem.kt` |
| Library Tab | `MangaLibraryTab.kt`, `AnimeLibraryTab.kt` |
| Category UI | `CategoryListItem.kt`, `MangaCategoryScreen.kt`, `AnimeCategoryScreen.kt` |
| Sort Logic | `MangaLibraryScreenModel.kt`, `AnimeLibraryScreenModel.kt` (applySort) |

---

## How to Test

1. **Build APK:** GitHub Actions builds on every push to `main`
2. **Custom Sort:** Library Settings → Sort → Select "Custom"
3. **Reorder Entries:** Long-press entries → Select → Use move up/down in bottom menu
4. **Nested Categories:** Create parent category → Create child category → Add entries

---

## Build & Deploy

```bash
# Push to main triggers GitHub Actions build automatically
git push origin main

# APK artifacts available at:
# GitHub → Actions → Latest run → Artifacts
```

---

## Architecture Notes

- **Entry-Category Relationship:** Many-to-many via junction table (`mangas_categories`)
- **Sort Storage:** Per-category flags in `Category.flags`
- **Custom Sort:** When selected, entries sorted by `sort_order` column in junction table
- **Nested Categories:** Uses `parent_id` in Category table, `currentCategoryId` in ScreenModel

---

## String Resources Added

- `action_sort_custom` in `base/strings.xml`

---

## Next Engineer Checklist

- [ ] Test entry reordering in selection mode
- [ ] Add drag-and-drop UI if needed
- [ ] Verify nested category navigation works
- [ ] Test thumbnail display and persistence
- [ ] Build and test on device
