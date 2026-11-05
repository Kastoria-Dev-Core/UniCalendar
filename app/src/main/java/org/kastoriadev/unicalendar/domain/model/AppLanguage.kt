package org.kastoriadev.unicalendar.domain.model

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    SYSTEM("", "System Default", "Προεπιλογή Συστήματος"),
    ENGLISH("en", "English", "English"),
    GREEK("el", "Greek", "Ελληνικά")
}
