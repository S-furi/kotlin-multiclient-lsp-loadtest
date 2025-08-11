package loadtest

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import utils.LSPServer
import kotlin.system.exitProcess

object Launcher {
    fun launchTest(nClients: Int) = runBlocking {

        repeat(nClients) {
            LoadTest.spawnRandomWorkspace("client-$it").also { project ->
                LoadTest.spawnClient(project, "client-$it")
                println("Created client $it for $project")
            }
        }

        try {
            LoadTest.initClients(this)
            LoadTest.runClients(this)
            println("All clients launched")
            delay(10_000)
        } finally {
            LoadTest.killAllClients()
            LoadTest.cleanupProjects()
        }
    }
}

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Usage: <number of clients>")
        exitProcess(1)
    }

    // Cannot find a way to attach to the server and gracefully shutdown when pressing stop
    // or interrupt it from CLI in gradle... Run it by hand :)
    // ```bash
    // cd kotlin-lsp && ./kotlin-lsp.sh --multi-client
    // ```
    // LSPServer.startServer()

    val nClients = args[0].toInt()
    try {
        Launcher.launchTest(nClients)
    } finally {
        LoadTest.cleanupProjects()
        LSPServer.stopServer()
    }
    exitProcess(0)
}