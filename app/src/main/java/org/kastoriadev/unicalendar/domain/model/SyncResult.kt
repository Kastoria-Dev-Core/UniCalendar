package org.kastoriadev.unicalendar.domain.model

sealed interface SyncResult {
    data class Success(val importedCount: Int) : SyncResult
    data class Failure(val message: String) : SyncResult
}
