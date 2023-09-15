package tachiyomi.data.category

import tachiyomi.domain.category.model.Category

val categoryMapper: (Long, String, Long, Long, Long) -> Category =
    { id, name, order, flags, hidden ->
        Category(
            id = id,
            name = name,
            order = order,
            flags = flags,
            hidden = hidden == 1L,
        )
    }
