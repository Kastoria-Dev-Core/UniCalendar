package org.kastoriadev.unicalendar.domain.model

sealed interface SyncResult {
    /**
     * At least one source was imported. When the other source of the same sync could not be
     * updated, its error is reported in [timetableError] / [examsError] and its old events are kept.
     */
    data class Success(
        /** Distinct courses imported from the timetable. */
        val lessonCount: Int = 0,
        val examCount: Int = 0,
        /** Events imported from a generic calendar (.ics) or web page. */
        val otherCount: Int = 0,
        val timetableError: SyncError? = null,
        val examsError: SyncError? = null,
        /** Names of the published documents that were imported, e.g. "Timetable_ΧE_2026-2027_V1.2". */
        val timetableLabel: String? = null,
        val examsLabel: String? = null
    ) : SyncResult {
        val totalCount: Int get() = lessonCount + examCount + otherCount
    }

    data class Failure(val error: SyncError) : SyncResult
}

/** Reasons a sync or import can fail, mapped to localized messages by the UI. */
sealed interface SyncError {
    /** No connection, DNS failure or timeout. */
    data object Network : SyncError

    /** A server answered with an unexpected HTTP status. */
    data class Http(val code: Int) : SyncError

    /** The department page lists no schedule document. */
    data object NoDocumentFound : SyncError

    /** The document is not a .docx / .ics / web page (e.g. PDF or legacy .doc). */
    data object UnsupportedFormat : SyncError

    /** The document was read, but no lessons or exams could be recognised in it. */
    data object NothingRecognized : SyncError

    /** The picked file could not be opened or read. */
    data object FileUnreadable : SyncError

    /**
     * Anything else, such as a bug in a parser. [details] holds the stack trace, so the problem
     * is shown and can be reported instead of crashing the app.
     */
    data class Unexpected(val details: String) : SyncError
}
