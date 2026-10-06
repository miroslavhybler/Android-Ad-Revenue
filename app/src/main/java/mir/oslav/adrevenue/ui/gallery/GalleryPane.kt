@file:OptIn(com.jet.admob.annotations.JetAdMobAlpha::class)

package mir.oslav.adrevenue.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdSize
import com.jet.admob.AdMobAdsUtil
import com.jet.admob.AdMobBanner
import com.jet.admob.AdMobBannerState
import com.jet.admob.AdMobNative
import com.jet.admob.AdMobNativeAdState
import com.jet.admob.NativeAdColors
import com.jet.admob.NativeAdFormat
import com.jet.admob.rememberAdMobBannerState
import com.jet.admob.rememberAdMobNativeAdState

/** Real SDK test inventory, rendered through jet-admob-ads. No publisher account is needed. */
@Composable
fun GalleryPane(
    onBackClick: () -> Unit,
    showBackButton: Boolean,
) {
    var showImplementation by remember { mutableStateOf(value = false) }
    // These owners stay composed while LazyColumn disposes and recreates off-screen items.
    val bannerState = rememberAdMobBannerState(
        adUnitId = AdMobAdsUtil.TestIds.FIXED_SIZE_BANNER,
        adSize = AdSize.BANNER,
    )
    val feedNativeState = rememberAdMobNativeAdState(AdMobAdsUtil.TestIds.NATIVE)
    val detailNativeState = rememberAdMobNativeAdState(AdMobAdsUtil.TestIds.NATIVE)

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp)
                    .statusBarsPadding()
            ) {
                Text(
                    "Compose ad gallery",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp) + paddingValues,
            verticalArrangement = Arrangement.spacedBy(space = 20.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(space = 10.dp)) {
                    if (showBackButton) {
                        OutlinedButton(onClick = onBackClick) { Text("Back") }
                    }

                    Text(
                        "Real test ads, placed with jet-admob-ads. See the result, then see the Compose code behind it.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "Demo inventory only · No revenue is generated",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                        FilterChip(
                            selected = !showImplementation,
                            onClick = { showImplementation = false },
                            label = { Text("Showcase") })
                        FilterChip(
                            selected = showImplementation,
                            onClick = { showImplementation = true },
                            label = { Text("Implementation") })
                    }
                }
            }
            if (showImplementation) {
                item { ImplementationContent() }
            } else {
                item(key = "banner-example") { BannerExample(bannerState) }
                item(key = "feed-intro") {
                    Text("A scrolling article feed", style = MaterialTheme.typography.titleLarge)
                }
                items(count = 6, key = { "article-before-$it" }) { index ->
                    ArticleRow(index + 1)
                }
                item(key = "feed-native-ad") { FeedExample(feedNativeState) }
                items(count = 6, key = { "article-after-$it" }) { index ->
                    ArticleRow(index + 7)
                }
                item(key = "detail-example") { DetailExample(detailNativeState) }
            }
        }
    }

}

@Composable
private fun BannerExample(bannerState: AdMobBannerState) {
    ExampleCard(
        title = "A banner at a natural pause",
        description = "The ad follows the content section instead of interrupting a task or covering controls.",
    ) {
        Text("Your weekly overview", style = MaterialTheme.typography.titleMedium)
        Text("A short summary belongs here. The banner appears after the summary ends.")
        AdMobBanner(state = bannerState, preOccupySpace = true)
    }
}

@Composable
private fun FeedExample(nativeState: AdMobNativeAdState) {
    ExampleCard(
        title = "A native card in a feed",
        description = "A small native format can follow a group of items while retaining its ad label and AdChoices.",
    ) {
        AdMobNative(
            state = nativeState,
            adFormat = NativeAdFormat.Small,
            colors = galleryAdColors(),
        )
    }
}

@Composable
private fun DetailExample(nativeState: AdMobNativeAdState) {
    ExampleCard(
        title = "A native ad after a story",
        description = "Give the story room to finish, then use the medium format as a distinct recommendation.",
    ) {
        Text("The art of useful pauses", style = MaterialTheme.typography.titleLarge)
        Text("A reader reaches the end of an article before this placement appears. The ad remains clearly identified.")
        AdMobNative(
            state = nativeState,
            adFormat = NativeAdFormat.Medium,
            colors = galleryAdColors(),
        )
    }
}

@Composable
private fun ArticleRow(number: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Article $number · Compose design", style = MaterialTheme.typography.titleMedium)
            Text(
                "A short editorial preview gives the feed a real scroll distance and keeps the ad between content groups.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun galleryAdColors() = NativeAdColors(
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
    buttonColor = MaterialTheme.colorScheme.primary,
    buttonTextColor = MaterialTheme.colorScheme.onPrimary,
)

@Composable
private fun ExampleCard(title: String, description: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
private fun ImplementationContent() {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("The Compose path", style = MaterialTheme.typography.titleLarge)
        Text("The Google Mobile Ads SDK still supplies Android ad views. jet-admob-ads wraps loading, state, and the view bridge in Compose APIs, so your screen can use composables directly.")
        Text("Remember each ad state in the screen composable, above LazyColumn. Lazy items leave composition when scrolled away, but their ad states should stay alive in the parent.")
        CodeExample(
            "Banner after content",
            """// In the screen composable, before LazyColumn:
val state = rememberAdMobBannerState(
    adUnitId = AdMobAdsUtil.TestIds.FIXED_SIZE_BANNER,
    adSize = AdSize.BANNER,
)
LazyColumn {
    item(key = "summary") { Summary() }
    item(key = "banner-ad") {
        AdMobBanner(state = state, preOccupySpace = true)
    }
}""",
        )
        CodeExample(
            "Native card in a feed",
            """// Remember state outside LazyColumn items.
val state = rememberAdMobNativeAdState(
    adUnitId = AdMobAdsUtil.TestIds.NATIVE,
)
LazyColumn {
    items(articles.take(6), key = { it.id }) { article ->
        ArticleRow(article)
    }
    item(key = "native-ad") {
        AdMobNative(state = state, adFormat = NativeAdFormat.Small)
    }
    items(articles.drop(6), key = { it.id }) { article ->
        ArticleRow(article)
    }
}""",
        )
        CodeExample(
            "Medium native after an article",
            """// Separate placement, separate state; keep it above the list.
val state = rememberAdMobNativeAdState(
    adUnitId = AdMobAdsUtil.TestIds.NATIVE,
)
LazyColumn {
    item(key = "story") { Story() }
    item(key = "native-after-story") {
        AdMobNative(state = state, adFormat = NativeAdFormat.Medium)
    }
}""",
        )
        Text(
            "Replace demo IDs with your own units for your app. Keep ad attribution and AdChoices visible, and test loading, failure, scrolling, and screen rotation.",
            style = MaterialTheme.typography.bodyMedium
        )
        TextButton(onClick = {
            uriHandler.openUri("https://github.com/miroslavhybler/jet-admob-ads/tree/main/example-app")
        }) {
            Text("Open complete example app on GitHub")
        }
    }
}

@Composable
private fun CodeExample(title: String, code: String) {
    val clipboard = LocalClipboardManager.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                code,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Text("Copy code")
            }
        }
    }
}
