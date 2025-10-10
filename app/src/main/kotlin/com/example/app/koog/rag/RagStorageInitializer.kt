package com.example.app.koog.rag

import ai.koog.rag.base.RankedDocumentStorage
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class RagStorageInitializer(
    private val storage: RankedDocumentStorage<Path>,
    private val knowledgeBasePath: Path,
    private val embeddingStorePath: Path,
) {
    private val logger: Logger = LoggerFactory.getLogger(RagStorageInitializer::class.java)
    private val initScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val ingestIndexFile: File = embeddingStorePath.resolve("ingested-files.csv").toFile()

    private val createChunks = true
    private val chunker = RagFileChunker()

    companion object {
        /**
         * Number of parallel processing jobs for document reading.
         */
        private const val MAX_CONCURRENT_FILES = 4
    }

    @PostConstruct
    fun init() {
        initScope.launch {
            try {
                initDocumentStorage()
                logger.info("RAG storage initialization completed successfully")
            } catch (e: Exception) {
                logger.error("Failed to initialize RAG storage", e)
            }
        }
    }

    @EventListener(ContextClosedEvent::class)
    fun cleanup() {
        initScope.cancel()
    }

    private suspend fun initDocumentStorage() {
        logger.info("Knowledge base path: ${knowledgeBasePath.toAbsolutePath()}")

        val files = knowledgeBasePath.toFile().listFiles()
        if (files == null || files.isEmpty()) {
            logger.warn("No files found in knowledge base: ${knowledgeBasePath.toAbsolutePath()}")
            return
        }

        val ingested = loadIngestedRecords().toMutableMap()

        logger.info("Processing ${files.size} files with max concurrency: $MAX_CONCURRENT_FILES")

        val semaphore = Semaphore(MAX_CONCURRENT_FILES)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        files
            .asFlow()
            .map { file: File ->
                semaphore.withPermit {
                    try {
                        if (chunker.isPartFile(file)) {
                            logger.info("Skipping part file: ${file.absolutePath}")
                            return@withPermit Result.success(file)
                        }
                        val key = file.absolutePath
                        val currentSig = file.lastModified().toString() + ":" + file.length().toString()
                        val prevSig = ingested[key]
                        if (prevSig == currentSig) {
                            logger.info("Skipping unchanged file (already embedded): ${file.absolutePath}")
                            Result.success(file)
                        } else {
                            val chunks =
                                if (createChunks) {
                                    chunker.createChunksIfNeeded(file)
                                } else {
                                    listOf(file.toPath())
                                }
                            if (chunks.size > 1) {
                                logger.info("Chunked ${file.name} into ${chunks.size} parts due to size limits")
                            } else {
                                logger.info("Adding file: ${file.absolutePath}")
                            }
                            chunks.forEach { path ->
                                storage.store(path)
                            }
                            successCount.addAndGet(chunks.size)
                            ingested[key] = currentSig
                            Result.success(file)
                        }
                    } catch (e: Exception) {
                        logger.error("Error adding file: ${file.absolutePath}", e)
                        failureCount.incrementAndGet()
                        Result.failure<File>(e)
                    }
                }
            }.collect()

        // Persist ingested index once after processing completes
        try {
            saveIngestedRecords(ingested)
        } catch (e: Exception) {
            logger.warn("Failed to persist ingested index at ${ingestIndexFile.absolutePath}: ${e.message}")
        }

        logger.info(
            "File processing completed: ${successCount.get()} successful chunks/files, ${failureCount.get()} failed",
        )
    }

    private fun loadIngestedRecords(): Map<String, String> {
        return try {
            if (!ingestIndexFile.exists()) return emptyMap()
            ingestIndexFile
                .readLines(Charsets.UTF_8)
                .asSequence()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val parts = line.split('\t')
                    if (parts.size >= 3) {
                        val path = parts[0]
                        val lastModified = parts[1]
                        val size = parts[2]
                        path to ("$lastModified:$size")
                    } else {
                        null
                    }
                }.toMap()
        } catch (e: Exception) {
            logger.warn("Failed to read ingested index at ${ingestIndexFile.absolutePath}: ${e.message}")
            emptyMap()
        }
    }

    private fun saveIngestedRecords(ingested: Map<String, String>) {
        try {
            val parent = ingestIndexFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val header = "# path\tlastModified\tsize"
            val lines =
                buildList {
                    add(header)
                    ingested.toSortedMap().forEach { (path, sig) ->
                        val parts = sig.split(":")
                        val lastModified = parts.getOrNull(0) ?: ""
                        val size = parts.getOrNull(1) ?: ""
                        add(listOf(path, lastModified, size).joinToString("\t"))
                    }
                }
            ingestIndexFile.writeText(lines.joinToString(System.lineSeparator()), Charsets.UTF_8)
        } catch (e: Exception) {
            logger.warn("Failed to write ingested index at ${ingestIndexFile.absolutePath}: ${e.message}")
        }
    }
}