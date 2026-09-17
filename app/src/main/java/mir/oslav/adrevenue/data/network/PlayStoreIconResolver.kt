package mir.oslav.adrevenue.data.network

import com.jet.article.core.ArticleContentTransformer
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayStoreIconResolver @Inject constructor() {
    private val cache = ConcurrentHashMap<String, String>()
    private val client = HttpClient(engineFactory = Android)

    private val transformer = ArticleContentTransformer()

    suspend fun resolve(packageName: String): String? {
        cache[packageName]?.let { return it }
        return runCatching {
            val html =
                client.get(urlString = "${GOOGLE_PLAY_BASE_URL}/store/apps/details?id=$packageName&hl=en")
                    .bodyAsText()
            val candidates = mutableListOf<String>()
            transformer.transform(
                html = html,
                url = GOOGLE_PLAY_BASE_URL
            ) { tag, attrs ->
                if (tag.equals(other = "img", ignoreCase = true) && attrs.isAppIconImage()) {
                    attrs["srcset"]
                        ?.split(',')
                        ?.mapNotNull(transform= { candidate -> candidate.trim().split(' ').firstOrNull() })
                        ?.let(block = candidates::addAll)
                    attrs["src"]?.let(block = candidates::add)
                }
                true
            }
            val url =
                (candidates + CLASS_SCOPED_IMAGE_REGEX.findAll(input = html)
                    .map { it.groupValues[1] })
                    .map {
                        it.replace(oldValue = "\\u003d", newValue = "=")
                            .replace(oldValue = "\\u0026", newValue = "&")
                    }
                    .firstOrNull { it.startsWith(prefix = "https://play-lh.googleusercontent.com/") }
            url?.also { cache[packageName] = it }
        }.getOrNull()
    }

    private fun Map<String, String>.isAppIconImage(): Boolean {
        val classes = this["class"]?.split(Regex("\\s+"))?.toSet().orEmpty()
        return APP_ICON_CLASSES.all(predicate = classes::contains) && this["itemprop"] == "image"
    }

    private companion object {
        val APP_ICON_CLASSES = setOf("T75of", "nm4vBd", "arM4bb")
        val CLASS_SCOPED_IMAGE_REGEX = Regex(
            pattern = "<img[^>]+class=\\\"[^\\\"]*(?:T75of\\s+nm4vBd\\s+arM4bb|nm4vBd\\s+T75of\\s+arM4bb|arM4bb\\s+T75of\\s+nm4vBd)[^\\\"]*\\\"[^>]+src=\\\"(https://play-lh\\.googleusercontent\\.com/[^\\\"]+)\\\"",
            option = RegexOption.IGNORE_CASE,
        )

        const val GOOGLE_PLAY_BASE_URL = "https://play.google.com"
    }
}
