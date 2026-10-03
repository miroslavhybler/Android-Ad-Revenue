package mir.oslav.adrevenue.data.search

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Inject
import javax.inject.Singleton
import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import mir.oslav.adrevenue.BuildConfig
import mir.oslav.adrevenue.core.AppJson
import androidx.core.net.toUri

@Serializable
private data class Response constructor(
    val results: List<Chunk> = emptyList()
)

@Serializable
private data class Chunk constructor(
    val content: String = "",
    val relevanceScore: Double? = null,
    val document: Document? = null
)

@Serializable
private data class Document constructor(
    val title: String = "",
    val uri: String = "",
    val description: String? = null
)

@Keep
@Serializable
data class DocumentationSearchResult constructor(
    val title: String,
    val snippet: String,
    val url: String
)

@Singleton
class DeveloperKnowledgeClient @Inject constructor() {
    private val client = HttpClient(engineFactory = Android) {
        install(plugin = ContentNegotiation) {
            json(json = AppJson.Json)
        }
    }

    suspend fun search(query: String): List<DocumentationSearchResult> {
        if (BuildConfig.DEVELOPER_KNOWLEDGE_API_KEY.isBlank()) return emptyList()
        val response =
            client.get(urlString = "https://developerknowledge.googleapis.com/v1/documents:searchDocumentChunks") {
                parameter(key = "query", value = "AdMob ${query.trim()}")
                parameter(key = "filter", value = "data_source = \"developers.google.com\"")
                parameter(key = "pageSize", value = 30)
                header("x-goog-api-key", BuildConfig.DEVELOPER_KNOWLEDGE_API_KEY)
                header("X-Android-Package", BuildConfig.APPLICATION_ID)
                header("X-Android-Cert", BuildConfig.DEVELOPER_KNOWLEDGE_ANDROID_CERT)
            }.body<Response>()
        return response.results.asSequence()
            .filter(predicate = { it.document?.uri?.isAdMobUrl() == true })
            .sortedByDescending(selector = { it.relevanceScore ?: 0.0 })
            .distinctBy(selector = { it.document!!.uri })
            .map(transform = { chunk ->
                DocumentationSearchResult(
                    chunk.document!!.title,
                    snippet = chunk.content.compact(),
                    url = chunk.document.uri
                )
            })
            .take(n = 12).toList()
    }
}


private fun String.isAdMobUrl(): Boolean {
    val uri = this.toUri()
    val isValidScheme = uri.scheme == "https"
    val isValidHost = uri.host == "developers.google.com"
    val isValidPath = uri.path == "/admob" || uri.path?.startsWith("/admob/") == true
    return isValidScheme && isValidHost && isValidPath
}


private fun String.compact(): String =
    replace(regex = Regex(pattern = "\\s+"), replacement = " ").trim()
