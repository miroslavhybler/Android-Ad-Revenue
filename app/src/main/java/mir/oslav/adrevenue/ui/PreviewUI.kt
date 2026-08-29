@file:Suppress("RedundantVisibilityModifier")

package mir.oslav.adrevenue.ui

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark


/**
 * @author Miroslav Hýbler <br>
 * created on 07.03.2025
 */
@PreviewLightDark
@Preview(
    device = "spec:parent=pixel_tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    name = "Pixel tablet"
)
@Preview(
    device = "spec:parent=pixel_6,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    name = "Pixel 6 landscape",
)
@Preview(
    device = "spec:parent=pixel_6,orientation=portrait",
    fontScale = 2f,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    name = "Pixel 6 font x2",
)
@Preview(
    device = "spec:parent=pixel_fold,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    name = "Fold portrait",
)
@Preview(
    device = "spec:parent=pixel_fold,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    name = "Fold landscape",
)
//@PreviewDynamicColors
public annotation class PreviewUI public constructor() {

}