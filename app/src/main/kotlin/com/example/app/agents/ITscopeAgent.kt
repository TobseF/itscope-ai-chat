package com.example.app.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.context.RollbackStrategy
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.extension.ModeratedMessage
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.agents.features.tracing.writer.TraceFeatureMessageLogWriter
import ai.koog.agents.snapshot.feature.Persistence
import ai.koog.agents.snapshot.providers.PersistenceStorageProvider
import ai.koog.prompt.dsl.AttachmentBuilder
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.Attachment
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.rag.base.RankedDocumentStorage
import ai.koog.rag.base.mostRelevantDocuments
import com.example.app.ChatSessionId
import com.example.app.koog.propmts.PromptTemplateProvider
import de.itscope.ai.mcp.config.ApiAccess
import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.sdk.trace.export.SpanExporter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

@Service
class ITscopeAgent(
    private val promptExecutor: SingleLLMPromptExecutor,
    private val spanExporters: List<SpanExporter>,
    private val buildProps: BuildProperties,
    private val rankedDocumentStorage: RankedDocumentStorage<Path>,
    private val persistenceStorageProvider: PersistenceStorageProvider,
    private val promptTemplateProvider: PromptTemplateProvider,
    private val itscopeAPI: ApiAccess,
) {
    private val logger = LoggerFactory.getLogger(ITscopeAgent::class.java)
    private val kotlinLogger = KotlinLogging.logger(name = "ITscopeAgent")

    private val systemErrorResponse =
        javaClass.getResource("/agents/itscope-assistant/system-error.md")!!.readText()

    private val moderationErrorResponse =
        javaClass.getResource("/agents/itscope-assistant/moderation-error.md")!!.readText()

    private val greetings =
        arrayOf(
            "Hallo, wie kann ich ihnen behilflich sein?",
            "Hallo, wie kann ich Ihnen heute weiterhelfen?",
            "Wie kann ich Ihnen bei der Nutzung von ITscope unterstützen?",
        )

    @Value("\${ai.koog.agents.tracing}")
    private var enableTracing: Boolean = false

    private val tools =
        ToolRegistry {
            tools(AssistantTools(itscopeAPI))
        }

    fun giveAdvice(
        input: String,
        chatSessionId: ChatSessionId,
    ): Flow<String> {
        if (input == "[START]") {
            return flowOf(greetings.random())
        } else if (input == "[CONTINUE]") {
            return flowOf("") // do nothing
        }

        return callbackFlow {
            var agent: AIAgent<String, String>? = null
            var flowClosed = false

            try {
                val relevantDocuments =
                    rankedDocumentStorage
                        .mostRelevantDocuments(input, count = 3)
                        .toList()

                val systemPrompt =
                    promptTemplateProvider.getPromptTemplate(
                        group = "itscope-assistant",
                        id = "system",
                        version = "latest",
                    )

                agent =
                    AIAgent(
                        id = chatSessionId,
                        promptExecutor = promptExecutor,
                        agentConfig =
                            AIAgentConfig(
                                prompt =
                                    createPrompt(systemPrompt, input, relevantDocuments),
                                model = OpenAIModels.CostOptimized.GPT4_1Mini,
                                // model = OpenAIModels.Chat.GPT5,
                                maxAgentIterations = 100,
                            ),
                        toolRegistry = tools, // TODO: Get tools working
                    ) {
                        install(Persistence) {
                            // storage = persistenceStorageProvider

                            // Enable automatic checkpoint creation
                            this.enableAutomaticPersistence = true

                            // We preserve message history on restore
                            this.rollbackStrategy = RollbackStrategy.MessageHistoryOnly
                        }

                        /*
                        install(OpenTelemetry) {
                            setServiceInfo(serviceName = buildProps.name, serviceVersion = buildProps.version)
                            // Configuration options here
                            setVerbose(true)
                            spanExporters.forEach {
                                addSpanExporter(it)
                            }
                        }*/

                        if (enableTracing) {
                            install(Tracing) {
                                // Configure message processors to handle trace events
                                addMessageProcessor(TraceFeatureMessageLogWriter(kotlinLogger))
                            }
                        }

                        handleEvents {
                            onToolCallStarting { context ->
                                logger.info("\n🔧 Using ${context.tool.name} with ${context.toolArgs}... ")
                            }

                            onToolValidationFailed {
                                logger.warn("❌ Tool validation failed. tool=${it.tool} error=${it.error}")
                                if (!flowClosed) {
                                    flowClosed = true
                                    trySend(systemErrorResponse)
                                    close()
                                }
                            }

                            onNodeExecutionCompleted { context ->
                                logger.trace("Node execution completed: {}", context)

                                val moderatedMessage = context.output as? ModeratedMessage
                                moderatedMessage?.let {
                                    if (it.moderationResult.isHarmful) {
                                        if (!flowClosed) {
                                            flowClosed = true
                                            trySend(moderationErrorResponse)
                                            close()
                                        }
                                    }
                                }
                            }

                            onNodeExecutionFailed {
                                logger.warn("❌ Node execution failed: ${it.node}", it.throwable)
                                if (!flowClosed) {
                                    flowClosed = true
                                    trySend(systemErrorResponse)
                                    close()
                                }
                            }

                            onLLMStreamingFrameReceived { context ->
                                (context.streamFrame as? StreamFrame.Append)?.let { frame ->
                                    logger.debug("➡️ Received: \"${frame.text}\"")
                                    if (!flowClosed) {
                                        trySend(frame.text)
                                    }
                                }
                            }

                            onLLMStreamingFailed {
                                logger.warn("❌ Error: ${it.error}")
                                if (!flowClosed) {
                                    flowClosed = true
                                    trySend(systemErrorResponse)
                                    close()
                                }
                            }

                            onLLMStreamingCompleted {
                                logger.debug("✅ Streaming complete")
                                if (!flowClosed) {
                                    flowClosed = true
                                    close()
                                }
                            }
                        }
                    }

                logger.trace("Running command: {}", input)

                agent.run(input)
            } catch (e: Exception) {
                logger.error("❌ Error processing request", e)
                if (!flowClosed) {
                    flowClosed = true
                    trySend(systemErrorResponse)
                    close()
                }
            }
            awaitClose {
                logger.debug("Flow cancelled for session: $chatSessionId, cleaning up resources")
                runBlocking {
                    try {
                        agent?.close()
                        logger.debug("Agent closed successfully for session: $chatSessionId")
                    } catch (e: Exception) {
                        logger.error("Error closing agent during cleanup for session: $chatSessionId", e)
                    }
                }
            }
        }
    }

    private fun createPrompt(
        systemPrompt: String,
        input: String,
        relevantDocuments: List<Path>,
    ): Prompt =
        prompt(
            "with-context",
            params = LLMParams(temperature = 0.2),
        ) {
            system(systemPrompt)
            user {
                +"Nutzer Input: ```$input```."
                if (relevantDocuments.isNotEmpty()) {
                    +"Use attachment as additional relevant context, but first use the tools"
                    attachments {
                        relevantDocuments.forEach {
                            createAttachmentFromFile(path = it)
                        }
                    }
                }
            }
        }
}

private fun AttachmentBuilder.createAttachmentFromFile(path: Path) {
    val file = File("./${path.pathString}")
    val text = file.readText()
    attachment(
        Attachment.File(
            content = AttachmentContent.PlainText(text),
            format = "md",
            mimeType = "text/plain",
            fileName = file.name,
        ),
    )
}