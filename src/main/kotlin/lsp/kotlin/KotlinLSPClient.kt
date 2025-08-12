package lsp.kotlin

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageServer
import java.net.Socket
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class KotlinLSPClient(host: String = "127.0.0.1", port: Int = 9999) {
    private val socket by lazy { Socket(host, port) }
    private val languageClient = KotlinLanguageClient()
    private val languageServer: LanguageServer by lazy { getRemoteLanguageServer() }
    private lateinit var stopFuture: Future<Void>

    typealias Completions = Either<List<CompletionItem>, CompletionList>

    fun init(kotlinProjectRoot: String, projectName: String = "None"): Future<Void> {
        val capabilities = getCompletionCapabilities()
        val workspaceFolders = null // listOf(WorkspaceFolder("file://$kotlinProjectRoot", projectName))

        val params = InitializeParams().apply {
            this.capabilities = capabilities
            this.workspaceFolders = workspaceFolders
        }

        return languageServer.initialize(params)
            .thenCompose { _ ->
                languageServer.initialized(InitializedParams())
                CompletableFuture.completedFuture(null)
            }
    }

    fun getCompletion(
        uri: URI,
        position: Position,
        triggerKind: CompletionTriggerKind = CompletionTriggerKind.Invoked
    ): Future<Completions> {
        val context = CompletionContext(triggerKind)
        val params = CompletionParams(TextDocumentIdentifier(uri.toString()), position, context)
        return languageServer.textDocumentService.completion(params)
            ?: CompletableFuture.completedFuture(Either.forLeft(emptyList()))
    }

    fun hover(uri: URI, position: Position): CompletableFuture<Hover?> {
        val params = HoverParams(TextDocumentIdentifier(uri.toString()), position)
        return languageServer.textDocumentService.hover(params)
    }

    fun findReferences(uri: URI, position: Position): CompletableFuture<List<Location>> {
        val params = ReferenceParams(TextDocumentIdentifier(uri.toString()), position, ReferenceContext(true))
        return languageServer.textDocumentService.references(params)
    }

    fun workspaceSymbol(query: String = ""): CompletableFuture<Either<List<SymbolInformation?>?, List<WorkspaceSymbol?>?>?>? =
        languageServer.workspaceService.symbol(WorkspaceSymbolParams(query))

    fun openDocument(uri: URI) {
        val content = Files.readString(Paths.get(uri))
        val params = DidOpenTextDocumentParams(
            TextDocumentItem(uri.toString(), "kotlin", 1, content)
        )
        languageServer.textDocumentService.didOpen(params)
    }

    /**
     * Changes are not considered incremental, the whole document is replaced.
     */
    fun changeDocument(uri: URI, newContent: String) {
        val params = DidChangeTextDocumentParams(
            VersionedTextDocumentIdentifier(uri.toString(), 1),
            listOf(TextDocumentContentChangeEvent(newContent)),
        )
        languageServer.textDocumentService.didChange(params)
    }

    fun closeDocument(uri: URI) {
        val params = DidCloseTextDocumentParams(TextDocumentIdentifier(uri.toString()))
        languageServer.textDocumentService.didClose(params)
    }


    fun shutdown(): CompletableFuture<Any> = languageServer.shutdown()

    fun exit() {
        languageServer.exit()
        stopFuture.cancel(true)
        socket.close()
    }

    private fun getCompletionCapabilities() = ClientCapabilities().apply {
        textDocument = TextDocumentClientCapabilities().apply {
            completion =
                CompletionCapabilities(
                    CompletionItemCapabilities(true)
                ).apply { contextSupport = true }
        }
        workspace = WorkspaceClientCapabilities().apply { workspaceFolders = true }
    }

    private fun getRemoteLanguageServer(): LanguageServer {
        val input = socket.getInputStream()
        val output = socket.getOutputStream()
        val launcher = LSPLauncher.createClientLauncher(languageClient, input, output)
        stopFuture = launcher.startListening()
        return launcher?.remoteProxy ?: throw RuntimeException("Cannot connect to server")
    }
}
