package org.kastoriadev.unicalendar.data.parser

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Minimal, dependency-free reader for WordprocessingML (.docx) files.
 *
 * Only what the schedule parsers need is exposed: the body blocks (paragraphs and tables),
 * the visible text of a paragraph split at manual line breaks, and table rows laid out on the
 * logical grid (so horizontally merged cells keep their columns aligned with the header).
 */
internal object DocxReader {

    private const val WORDPROCESSING_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    private const val DOCUMENT_ENTRY = "word/document.xml"

    /** Upper bound for the uncompressed document.xml, so a malicious archive cannot exhaust memory. */
    private const val MAX_DOCUMENT_XML_BYTES = 32L * 1024 * 1024

    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)

    fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= ZIP_MAGIC.size && ZIP_MAGIC.indices.all { bytes[it] == ZIP_MAGIC[it] }

    /** Reads and parses `word/document.xml`, or returns null when [stream] is not a .docx archive. */
    fun readDocument(stream: InputStream): Document? {
        val xml = readDocumentXml(stream) ?: return null
        return parseXml(xml)
    }

    private fun readDocumentXml(stream: InputStream): ByteArray? {
        ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.equals(DOCUMENT_ENTRY, ignoreCase = true)) {
                    return readBounded(zip, MAX_DOCUMENT_XML_BYTES)
                }
                entry = zip.nextEntry
            }
        }
        return null
    }

    fun parseXml(xml: ByteArray): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            // Harden against XXE / entity expansion. Not every platform parser supports every
            // feature (Android's does not), so unsupported ones are skipped.
            trySetFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            trySetFeature("http://xml.org/sax/features/external-general-entities", false)
            trySetFeature("http://xml.org/sax/features/external-parameter-entities", false)
            trySetFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        }
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(xml))
    }

    private fun DocumentBuilderFactory.trySetFeature(name: String, value: Boolean) {
        try {
            setFeature(name, value)
        } catch (_: Exception) {
            // Feature not supported by this parser implementation.
        }
    }

    /** Top-level paragraphs and tables of the document body, with content controls unwrapped. */
    fun bodyBlocks(document: Document): List<Element> {
        val body = document.getElementsByTagNameNS("*", "body").item(0) as? Element
            ?: document.documentElement
            ?: return emptyList()
        val blocks = mutableListOf<Element>()
        fun collect(parent: Element) {
            for (child in parent.childElements()) {
                when (child.localNameCompat()) {
                    "p", "tbl" -> blocks += child
                    "sdt" -> child.childElements().firstOrNull { it.localNameCompat() == "sdtContent" }?.let(::collect)
                    "customXml" -> collect(child)
                }
            }
        }
        collect(body)
        return blocks
    }

    fun isParagraph(element: Element) = element.localNameCompat() == "p"
    fun isTable(element: Element) = element.localNameCompat() == "tbl"

    /**
     * Visible text of a paragraph, split at manual line breaks. Text inside hyperlinks, tracked
     * insertions, smart tags and content controls is kept; deleted text and field codes are not.
     */
    fun paragraphLines(paragraph: Element): List<String> {
        val lines = mutableListOf<String>()
        val current = StringBuilder()

        fun flush() {
            val line = current.toString().collapseWhitespace()
            if (line.isNotEmpty()) lines += line
            current.setLength(0)
        }

        fun walk(node: Element) {
            for (child in node.childElements()) {
                when (child.localNameCompat()) {
                    "t" -> current.append(child.textContent)
                    "tab", "ptab" -> current.append(' ')
                    "br", "cr" -> flush()
                    "noBreakHyphen" -> current.append('-')
                    "softHyphen", "del", "delText", "instrText", "fldChar",
                    "pPr", "rPr", "drawing", "pict", "object", "AlternateContent" -> Unit
                    else -> walk(child)
                }
            }
        }

        walk(paragraph)
        flush()
        return lines
    }

    fun paragraphText(paragraph: Element): String = paragraphLines(paragraph).joinToString(" ")

    /** Rows of a table, including rows wrapped in content controls. */
    fun tableRows(table: Element): List<Element> {
        val rows = mutableListOf<Element>()
        fun collect(parent: Element) {
            for (child in parent.childElements()) {
                when (child.localNameCompat()) {
                    "tr" -> rows += child
                    "sdt" -> child.childElements().firstOrNull { it.localNameCompat() == "sdtContent" }?.let(::collect)
                    "customXml" -> collect(child)
                }
            }
        }
        collect(table)
        return rows
    }

    enum class VerticalMerge { NONE, RESTART, CONTINUE }

    /** A table cell positioned on the table's logical grid. */
    data class GridCell(
        val startColumn: Int,
        val span: Int,
        val verticalMerge: VerticalMerge,
        val lines: List<String>
    ) {
        val text: String get() = lines.joinToString(" ")
        val columns: IntRange get() = startColumn until startColumn + span
    }

    /** Cells of [row] with their logical grid column, honouring `gridBefore` and `gridSpan`. */
    fun rowCells(row: Element): List<GridCell> {
        var column = row.childElements()
            .firstOrNull { it.localNameCompat() == "trPr" }
            ?.childElements()
            ?.firstOrNull { it.localNameCompat() == "gridBefore" }
            ?.wordAttribute("val")
            ?.toIntOrNull()
            ?: 0

        val cells = mutableListOf<GridCell>()
        fun collect(parent: Element) {
            for (child in parent.childElements()) {
                when (child.localNameCompat()) {
                    "tc" -> {
                        val properties = child.childElements().firstOrNull { it.localNameCompat() == "tcPr" }
                        val span = properties?.childElements()
                            ?.firstOrNull { it.localNameCompat() == "gridSpan" }
                            ?.wordAttribute("val")
                            ?.toIntOrNull()
                            ?.coerceAtLeast(1)
                            ?: 1
                        val mergeElement = properties?.childElements()?.firstOrNull { it.localNameCompat() == "vMerge" }
                        val merge = when {
                            mergeElement == null -> VerticalMerge.NONE
                            mergeElement.wordAttribute("val").equals("restart", ignoreCase = true) -> VerticalMerge.RESTART
                            else -> VerticalMerge.CONTINUE
                        }
                        cells += GridCell(column, span, merge, cellLines(child))
                        column += span
                    }
                    "sdt" -> child.childElements().firstOrNull { it.localNameCompat() == "sdtContent" }?.let(::collect)
                    "customXml" -> collect(child)
                }
            }
        }
        collect(row)
        return cells
    }

    private fun cellLines(cell: Element): List<String> {
        val lines = mutableListOf<String>()
        fun collect(parent: Element) {
            for (child in parent.childElements()) {
                when (child.localNameCompat()) {
                    "p" -> lines += paragraphLines(child)
                    "sdt" -> child.childElements().firstOrNull { it.localNameCompat() == "sdtContent" }?.let(::collect)
                    "customXml" -> collect(child)
                }
            }
        }
        collect(cell)
        return lines
    }

    private fun Element.wordAttribute(name: String): String {
        val namespaced = getAttributeNS(WORDPROCESSING_NS, name)
        if (namespaced.isNotEmpty()) return namespaced
        val prefixed = getAttribute("w:$name")
        if (prefixed.isNotEmpty()) return prefixed
        return getAttribute(name)
    }

    private fun Element.localNameCompat(): String = localName ?: nodeName.substringAfter(':')

    private fun Element.childElements(): List<Element> {
        val nodes = childNodes
        val result = ArrayList<Element>(nodes.length)
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) result += node as Element
        }
        return result
    }

    private fun readBounded(stream: InputStream, maxBytes: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) throw IOException("Document is too large to import")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}

/** Collapses runs of whitespace (including non-breaking spaces) into single spaces and trims. */
internal fun String.collapseWhitespace(): String =
    replace('\u00A0', ' ').replace(WHITESPACE_RUN, " ").trim()

private val WHITESPACE_RUN = Regex("\\s+")
