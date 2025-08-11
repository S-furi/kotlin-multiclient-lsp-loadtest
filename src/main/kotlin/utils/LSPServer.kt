package utils

import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

object LSPServer {
    private const val link = "https://download-cdn.jetbrains.com/kotlin-lsp/0.253.10629/kotlin-0.253.10629.zip"
    private const val zipDir = "kotlin-lsp.zip"
    private const val serverDir = "kotlin-lsp"
    private var process: Process? = null

    fun startServer() {
        if (!Paths.get(serverDir).toFile().exists()) downloadServer()
        println("Starting LSP server...")

        val pb = ProcessBuilder("./kotlin-lsp.sh", "--multi-client")
            .redirectOutput(File("/dev/null"))
            .redirectError(ProcessBuilder.Redirect.INHERIT)

        pb.directory(File(serverDir))
        pb.start().waitFor(5, TimeUnit.SECONDS)
    }

    fun stopServer() {
        println("Stopping LSP server...")
        process?.destroy()
    }

    private fun downloadServer() {
        println("Downloading LSP server...")
        URI(link).toURL().openStream().use {
            Files.copy(it, Paths.get(zipDir))
        }.also { unzip(zipDir, serverDir) }
    }

    private fun unzip(zipFile: String, destDir: String) {
        println("Unzipping server...")
        ProcessBuilder("unzip", "-o", zipFile, "-d", destDir)
            .start()
            .waitFor()

        ProcessBuilder("chmod", "+x", "$serverDir/kotlin-lsp.sh")
            .start()
            .waitFor()
    }
}
