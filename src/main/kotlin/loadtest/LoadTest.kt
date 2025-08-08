package loadtest

import lsp.client.KotlinLSPClient
import java.io.File
import java.net.URI
import kotlin.io.path.createTempDirectory

/**
 * TODO:
 * - create random files in workspaces
 * - create an LSPClient that accepts a list of operations (while true, listOfOp.foreach(execute))
 *   - actions could be defined as (LspClient) -> Unit, just ignore the result
 * - create logic in order to draw operations to assign for each client
 * - create a metrics class in order to asses performance and server stress under load
 */
object LoadTest {
    private val clients = mutableListOf<KotlinLSPClient>()
    private val projects = mutableListOf<URI>()


    fun spawnNClients(n: Int) = repeat(n) { clients.add(KotlinLSPClient()) }

    fun killAllClients() = clients.forEach { it.exit() }

    fun cleanupProjects() = projects.forEach { File(it).deleteRecursively() }

    fun spawnRandomWorkspace(projectName: String): URI {
        val projectRoot = createTempDirectory(projectName).toFile()
        val sourcesPath = "src/main/kotlin"
        projectRoot.resolve(sourcesPath).mkdirs()

        LoadTest::class.java.getResource("/gradle_templates")?.file?.let {
            File(it).listFiles()?.forEach { templateFile ->
                val fileName = templateFile.name.replace(".template", "")
                if (!fileName.endsWith(".kt")) {
                    templateFile.copyTo(
                        projectRoot.resolve(fileName)
                    )
                } else {
                    templateFile.copyTo(projectRoot.resolve(sourcesPath).resolve(fileName))
                }
                templateFile.writeText(templateFile.readText().replace("{{ projectName }}", projectName))

            }
        }
        return projectRoot.toURI()
    }
}

fun main() {
    LoadTest.spawnRandomWorkspace("culo")
//    Thread.sleep(10000)
//    LoadTest.cleanupProjects()
}