# Aniyomi Plus

An enhanced fork of [Aniyomi](https://github.com/aniyomiorg/aniyomi) with a redesigned library UI for better organization.

## What's Different

- **Nested Categories** - Create subcategories within categories to organize your library hierarchically
- **Grid Navigation** - Library shows categories as folders and entries in a single grid. Tap folders to drill in, back button to go up
- **Entry Reordering** - Long-press entries to select them, then use the up/down arrows in the bottom menu to reorder within a category
- **Create Category from Entry** - Select existing entries in your library and create a new category from them. Entries get moved into the new category automatically
- **Category Thumbnails** - Folders show the first entry's cover as a thumbnail

## How It Works

The library replaces the original tabbed-pager layout with a nested grid. Each category level shows its subcategories as folder tiles and its entries as manga/anime tiles. The custom sort order is stored in the database and respected when the sort mode is set to "Custom" in settings.

## Credits

Built on [Aniyomi](https://github.com/aniyomiorg/aniyomi). See the original project for full credits and dependencies.

## License

Apache-2.0. See [LICENSE](LICENSE) for details.
