package com.example.app.koog.rag

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Responsible for splitting large text files into smaller chunk files suitable for embedding.
 */
class RagFileChunker {
    private val logger: Logger = LoggerFactory.getLogger(RagFileChunker::class.java)

    companion object {
        // Character-based chunk size to stay under ~8k token embedding limit (roughly 4 chars/token)
        private const val MAX_CHARS_PER_CHUNK = 12000
        private const val CHUNK_OVERLAP = 400
        private const val PART_FILE_SUFFIX = "_part"
    }

    fun isPartFile(file: File): Boolean = file.name.contains(PART_FILE_SUFFIX)

    fun createChunksIfNeeded(file: File): List<Path> =
        try {
            val content = file.readText(Charsets.UTF_8)
            if (content.length <= MAX_CHARS_PER_CHUNK) {
                listOf(file.toPath())
            } else {
                // Split on paragraph boundaries first, then assemble chunks with overlap
                val paragraphs = content.split("\r?\n\r?\n".toRegex())
                val chunks = mutableListOf<StringBuilder>()

                var current = StringBuilder()
                for (para in paragraphs) {
                    val paragraphWithSep = if (current.isEmpty()) para else "\n\n$para"
                    if (current.length + paragraphWithSep.length > MAX_CHARS_PER_CHUNK) {
                        if (current.isNotEmpty()) {
                            chunks.add(current)
                            current = StringBuilder()
                        }
                        if (para.length > MAX_CHARS_PER_CHUNK) {
                            // Hard split very long paragraphs
                            var i = 0
                            while (i < para.length) {
                                val end = (i + MAX_CHARS_PER_CHUNK).coerceAtMost(para.length)
                                chunks.add(StringBuilder(para.substring(i, end)))
                                i = end
                            }
                        } else {
                            current.append(para)
                        }
                    } else {
                        current.append(paragraphWithSep)
                    }
                }
                if (current.isNotEmpty()) chunks.add(current)

                // Add simple overlap between chunks for context continuity
                val overlapped = mutableListOf<String>()
                for (idx in chunks.indices) {
                    val chunkStr = chunks[idx].toString()
                    if (CHUNK_OVERLAP > 0 && idx > 0) {
                        val prev = chunks[idx - 1].toString()
                        val overlapStart = (prev.length - CHUNK_OVERLAP).coerceAtLeast(0)
                        overlapped.add(prev.substring(overlapStart) + chunkStr)
                    } else {
                        overlapped.add(chunkStr)
                    }
                }

                // Write to temp files next to original file to keep path locality
                overlapped.mapIndexed { index, chunk ->
                    val tmpFile =
                        Files
                            .createTempFile(
                                file.parentFile.toPath(),
                                file.nameWithoutExtension + PART_FILE_SUFFIX + "${index + 1}_",
                                ".${file.extension.ifEmpty { "txt" }}",
                            ).toFile()
                    tmpFile.writeText(chunk, StandardCharsets.UTF_8)
                    tmpFile.toPath()
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to chunk file ${file.absolutePath}, falling back to original: ${e.message}")
            listOf(file.toPath())
        }
}