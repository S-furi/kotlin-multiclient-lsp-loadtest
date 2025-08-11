package lsp

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import lsp.kotlin.KotlinLSPClient
import org.eclipse.lsp4j.Position
import java.net.URI
import kotlin.random.Random

class RandomOpLSPClient(
    private val ktClient: KotlinLSPClient,
    private val operations: List<Pair<String, () -> Any?>>,
    private val projectURI: URI,
    val name: String
) {
    suspend fun initialize(): Boolean = coroutineScope {
        log("Starting")
        ktClient.init(projectURI.path).get()
        log("Initialized")
        true
    }

    suspend fun execute() = coroutineScope {
        try {
            while (isActive) {
                val (name, op) = operations.random()
                op()
                log("Finished $name")
                delay(Random.nextLong(500))
            }
        } finally {
            ktClient.exit()
        }
    }

    private fun log(msg: String) = println("[$name] - $msg")

    companion object {
        /**
         * Builds a client with the available, supported operations:
         * - hover
         * - completion
         * - references
         * - workspace symbols
         *
         * @param ktClient the underlying Kotlin LSP client
         * @return a client with the supported operations
         */
        fun buildClientWithSupportedOperations(ktClient: KotlinLSPClient, project: URI, name: String): RandomOpLSPClient {
            val mainRes = project.resolve("src/main/kotlin/Main.kt")
            val completionRes = project.resolve("src/main/kotlin/Completion.kt")
            val hover = {
                val position = Position(1, 7) // println
                ktClient.openDocument(mainRes)
                ktClient.hover(mainRes, position).get()
                ktClient.closeDocument(mainRes)
            }

            val completion = {
                val position = Position(2, 49)
                ktClient.openDocument(completionRes)
                ktClient.getCompletion(completionRes, position)
            }

            val references = {
                val position = Position(2, 16) // String
                ktClient.openDocument(completionRes)
                ktClient.getCompletion(completionRes, position).get()
                ktClient.closeDocument(completionRes)
            }

            val workspaceSymbols = {
                val query = "log"
                ktClient.openDocument(completionRes)
                ktClient.workspaceSymbol(query)?.get()
                ktClient.closeDocument(completionRes)
            }

            val operations = listOf(
                "hover" to hover,
                "completion" to completion,
                "references" to references,
                "workspace_symbols" to workspaceSymbols
            )

            return RandomOpLSPClient(ktClient, operations, project, name)
        }
    }
}