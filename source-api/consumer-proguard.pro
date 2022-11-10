-keep class eu.kanade.tachiyomi.source.model.** { public protected *; }
-keep class eu.kanade.tachiyomi.source.online.** { public protected *; }
-keep class eu.kanade.tachiyomi.source.** extends eu.kanade.tachiyomi.source.Source { public protected *; }

-keep class eu.kanade.tachiyomi.animesource.model.** { public protected *; }
-keep class eu.kanade.tachiyomi.animesource.online.** { public protected *; }
-keep class eu.kanade.tachiyomi.animesource.** extends eu.kanade.tachiyomi.animesource.AnimeSource { public protected *; }

-keep,allowoptimization class eu.kanade.tachiyomi.util.JsoupExtensionsKt { public protected *; }
