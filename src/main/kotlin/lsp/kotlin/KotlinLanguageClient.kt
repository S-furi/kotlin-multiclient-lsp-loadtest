package lsp.kotlin

import org.eclipse.lsp4j.LogTraceParams
import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.services.LanguageClient
import java.util.concurrent.CompletableFuture

class KotlinLanguageClient : LanguageClient {
    override fun telemetryEvent(o: Any) {
        println(o.toString())
    }

    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams) {
//        diagnostics.diagnostics.forEach { d -> println("Diagnostic: ${d.message} at ${d.range}") }
    }

    override fun showMessage(messageParams: MessageParams) {
//        println("[${messageParams.type}]: ${messageParams.message}")
    }

    override fun showMessageRequest(params: ShowMessageRequestParams): CompletableFuture<MessageActionItem?>? {
//        println("Message request: ${params.message}")
        params.actions?.forEach { action ->
//            println("Action: ${action.title}")
        }
        return CompletableFuture.completedFuture(params.actions?.firstOrNull())
    }

    override fun logMessage(message: MessageParams) {
//        println("[${message.type}]: ${message.message}")
    }

    override fun logTrace(params: LogTraceParams?) {
//        println("Log trace: ${params?.message}")
    }
}
