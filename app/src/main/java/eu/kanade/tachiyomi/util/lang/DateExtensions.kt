package eu.kanade.tachiyomi.util.lang

import android.content.Context
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.math.absoluteValue

fun LocalDateTime.toDateTimestampString(dateTimeFormatter: DateTimeFormatter): String {
    val date = dateTimeFormatter.format(this)
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(this)
    return "$date $time"
}

fun Date.toTimestampString(): String {
    return DateFormat.getTimeInstance(DateFormat.SHORT).format(this)
}

fun Long.convertEpochMillisZone(
    from: ZoneId,
    to: ZoneId,
): Long {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), from)
        .atZone(to)
        .toInstant()
        .toEpochMilli()
}

fun Long.toLocalDate(): LocalDate {
    return LocalDate.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
}

fun Instant.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
    return LocalDate.ofInstant(this, zoneId)
}

fun LocalDate.toRelativeString(
    context: Context,
    relative: Boolean = true,
    dateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT),
): String {
    if (!relative) {
        return dateFormat.format(this)
    }
    val now = LocalDate.now()
    val difference = ChronoUnit.DAYS.between(this, now)
    return when {
        difference < -7 -> dateFormat.format(this)
        difference < 0 -> context.pluralStringResource(
            MR.plurals.upcoming_relative_time,
            difference.toInt().absoluteValue,
            difference.toInt().absoluteValue,
        )
        difference < 1 -> context.stringResource(MR.strings.relative_time_today)
        difference < 7 -> context.pluralStringResource(
            MR.plurals.relative_time,
            difference.toInt(),
            difference.toInt(),
        )
        else -> dateFormat.format(this)
    }
}

// For use in chapter/episode release time
fun LocalDateTime.toRelativeString(
    context: Context,
    relative: Boolean = true,
    dateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT),
): String {
    if (!relative) {
        return dateFormat.format(this)
    }
    val now = LocalDateTime.now()
    val timeDifference = ChronoUnit.DAYS.between(this, now)
    val dateDifference = ChronoUnit.DAYS.between(this.toLocalDate(), now.toLocalDate())
    return when {
        timeDifference < -7 -> dateFormat.format(this)
        timeDifference < 0 -> context.pluralStringResource(
            MR.plurals.upcoming_relative_time,
            dateDifference.toInt().absoluteValue,
            dateDifference.toInt().absoluteValue,
        )
        timeDifference < 1 -> {
            val hourDifference = ChronoUnit.HOURS.between(this, now)
            when {
                hourDifference < 0 -> context.pluralStringResource(
                    AYMR.plurals.upcoming_relative_time_hours,
                    hourDifference.toInt().absoluteValue,
                    hourDifference.toInt().absoluteValue,
                )
                hourDifference < 1 -> {
                    val minuteDifference = ChronoUnit.MINUTES.between(this, now)
                    when {
                        minuteDifference < 0 -> context.pluralStringResource(
                            AYMR.plurals.upcoming_relative_time_minutes,
                            minuteDifference.toInt().absoluteValue,
                            minuteDifference.toInt().absoluteValue,
                        )
                        minuteDifference == 0L -> context.stringResource(AYMR.strings.relative_time_now)
                        else -> context.pluralStringResource(
                            AYMR.plurals.relative_time_minutes,
                            minuteDifference.toInt(),
                            minuteDifference.toInt(),
                        )
                    }
                }
                else -> context.pluralStringResource(
                    AYMR.plurals.relative_time_hours,
                    hourDifference.toInt(),
                    hourDifference.toInt(),
                )
            }
        }
        timeDifference < 7 -> context.pluralStringResource(
            MR.plurals.relative_time,
            dateDifference.toInt(),
            dateDifference.toInt(),
        )
        else -> dateFormat.format(this)
    }
}
