package org.kastoriadev.unicalendar.testutil

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Builds small WordprocessingML documents for parser and sync tests. */
object TestDocuments {

    private const val NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"

    fun paragraph(text: String): String = "<w:p><w:r><w:t xml:space=\"preserve\">${escape(text)}</w:t></w:r></w:p>"

    fun cell(vararg lines: String, merge: String? = null, span: Int = 1): String {
        val properties = buildString {
            if (span > 1) append("<w:gridSpan w:val=\"$span\"/>")
            when (merge) {
                "restart" -> append("<w:vMerge w:val=\"restart\"/>")
                "continue" -> append("<w:vMerge/>")
            }
        }
        val content = if (lines.isEmpty()) "<w:p/>" else lines.joinToString("") { paragraph(it) }
        return "<w:tc>${if (properties.isEmpty()) "" else "<w:tcPr>$properties</w:tcPr>"}$content</w:tc>"
    }

    fun row(vararg cells: String): String = "<w:tr>${cells.joinToString("")}</w:tr>"

    fun table(vararg rows: String): String = "<w:tbl>${rows.joinToString("")}</w:tbl>"

    fun document(vararg blocks: String): String =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<w:document xmlns:w=\"$NS\"><w:body>${blocks.joinToString("")}</w:body></w:document>"

    /** Zips [documentXml] into a minimal .docx archive. */
    fun docx(documentXml: String): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"/>".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(documentXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    /** A one-semester winter timetable for 2026-2027 with a Monday lecture and a Tuesday lab. */
    fun winterTimetableXml(): String = document(
        paragraph("Ωρολόγιο Πρόγραμμα μαθημάτων Χειμερινού Εξαμήνου Aκαδ. Έτους 2026-2027"),
        paragraph("Α΄ εξάμηνο"),
        table(
            row(cell("ΩΡΑ"), cell("ΔΕΥΤΕΡΑ"), cell("ΤΡΙΤΗ")),
            row(cell("09:00 – 10:00"), cell("Φυσική", "Θ. Παπαβασιλείου", "Μεγ. Αμφιθέατρο", merge = "restart"), cell()),
            row(cell("10:00 – 11:00"), cell(merge = "continue"), cell("Ηλεκτρονική (ΑΠ)", "Δ. Φωτιάδης", "Εργ. Ε6"))
        )
    )

    /** A September exam schedule with two days. */
    fun examScheduleXml(): String = document(
        paragraph("Πρόγραμμα Εξετάσεων Σεπτεμβρίου 2025-2026"),
        paragraph("Δευτέρα 31-08-2026"),
        paragraph("09:00-11:00 Εκπαιδευτική Ψυχολογία (Δ’) (ΠΔΕ), (5+0)  Κ. Μόσχου  Αμφ.2 Φ.Π."),
        paragraph("11:00-13:00 Αλγόριθμοι και Πολυπλοκότητα (Ε’) / Υπολογισιμότητα και Πολυπλοκότητα (Ζ’), (63+18+9)  Ν. Δημόκας  Μ.Αμφ."),
        paragraph("Τρίτη 01-09-2026"),
        paragraph("15:00-17:00 Μαθηματική Ανάλυση Ι (Α’), (332+14)  Ι. Τουλόπουλος  Μ.Αμφ., Αμφ.1")
    )

    private fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
