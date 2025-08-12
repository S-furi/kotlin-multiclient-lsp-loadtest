package loadtest

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import lsp.RandomOpLSPClient
import lsp.kotlin.KotlinLSPClient
import java.io.File
import java.net.URI
import java.nio.file.attribute.FileAttribute
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

object LoadTest {
    private val clients = mutableListOf<RandomOpLSPClient>()
    private val projects = mutableListOf<URI>()
    private val jobs = mutableListOf<Job>()

    fun spawnClient(project: URI, name: String): RandomOpLSPClient {
        val ktClient = KotlinLSPClient()
        val rndClient = RandomOpLSPClient.buildClientWithSupportedOperations(ktClient, project, name)
        clients.add(rndClient)
        projects.add(project)
        return rndClient
    }

    fun runClients(scope: CoroutineScope) {
        clients.forEach {
            jobs += scope.launch(Dispatchers.IO) {
                it.execute()
                println("Client ${it.name} started")
            }
        }
    }

    suspend fun initClients(scope: CoroutineScope) {
        clients.forEach { client ->
            scope.launchTimed("initialization") {
                client.initialize()
            }.let { client.log("Elapsed time: $it ms") }
        }
    }

    suspend fun killAllClients() = coroutineScope {
        jobs.forEach { it.cancelAndJoin() }
    }

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

    fun createBareGradleProject(projectName: String): URI {
        val projectRoot = createTempDirectory(projectName).toFile()

        LoadTest::class.java.getResource("/gradle_templates/Completion.kt.template")?.file?.let { template ->
            File(template).copyTo(projectRoot.resolve("Completion.kt"))
            projectRoot.resolve("build.gradle.kts").createNewFile()
        }
        return projectRoot.toURI()
    }

    private suspend fun CoroutineScope.launchTimed(name: String, block: suspend CoroutineScope.() -> Unit): Long =
        measureTimeMillis {
            launch { block() }.join()
        }
}