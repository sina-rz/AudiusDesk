package com.audiusdesk

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.audiusdesk.ui.AppShell
import com.audiusdesk.ui.theme.AppTheme
import java.awt.Dimension


fun main() {

    application {
        val app = remember { AudiusDeskApp() }

        Window(
            onCloseRequest = {
                app.close()
                exitApplication()
            },
            title = "AudiusDesk",
            state = rememberWindowState(
                position = WindowPosition(Alignment.Center),
                size = DpSize(width = 1280.dp, height = 860.dp),
            ),
        ) {
            setSingletonImageLoaderFactory { context ->
                ImageLoader.Builder(context)
                    .components { add(KtorNetworkFetcherFactory()) }
                    .build()
            }

            LaunchedEffect(Unit) {
                window.minimumSize = Dimension(1020, 680)
            }

            AppTheme {
                AppShell(app)
            }
        }
    }
}
