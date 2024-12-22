package tachiyomi.domain.custombuttons.exception

import java.io.IOException

/**
 * Exception to abstract over SQLiteException and SQLiteConstraintException for multiplatform.
 *
 * @param throwable the source throwable to include for tracing.
 */
class SaveCustomButtonException(throwable: Throwable) : IOException("Error Saving Repository to Database", throwable)
