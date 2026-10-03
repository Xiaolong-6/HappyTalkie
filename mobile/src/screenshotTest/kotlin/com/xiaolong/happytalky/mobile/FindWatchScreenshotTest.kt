package com.xiaolong.happytalky.mobile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.xiaolong.happytalky.core.ProximityBand
import com.xiaolong.happytalky.core.ProximityReading
import com.xiaolong.happytalky.core.ProximityTrend

@PreviewTest
@Preview(
    name = "Find Watch nearby",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
fun FindWatchNearbyScreenshot() {
    HappyTalkyPhoneTheme(
        darkTheme = false
    ) {
        FindWatchScreen(
            state =
                FindWatchUiState(
                    watchReady = true,
                    searching = false,
                    reading =
                        ProximityReading(
                            rawRssi = -63,
                            filteredRssi =
                                -64.2,
                            signalScore = 65,
                            band =
                                ProximityBand
                                    .CLOSE,
                            trend =
                                ProximityTrend
                                    .GETTING_CLOSER,
                            timestampMs = 1L
                        )
                ),
            onClose = {},
            onRetry = {}
        )
    }
}

@PreviewTest
@Preview(
    name = "Find Watch searching dark",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
fun FindWatchSearchingDarkScreenshot() {
    HappyTalkyPhoneTheme(
        darkTheme = true
    ) {
        FindWatchScreen(
            state =
                FindWatchUiState(
                    watchReady = true,
                    searching = true
                ),
            onClose = {},
            onRetry = {}
        )
    }
}
