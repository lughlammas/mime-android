package com.lughlammas.mime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lughlammas.mime.ui.LibraryScreen
import com.lughlammas.mime.ui.MimeTheme
import com.lughlammas.mime.ui.MimeViewModel
import com.lughlammas.mime.ui.PlayScreen
import com.lughlammas.mime.ui.Screen

class MainActivity : ComponentActivity() {
    private val viewModel: MimeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MimeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    when (val screen = state.screen) {
                        is Screen.Library -> LibraryScreen(
                            maps = state.maps,
                            error = state.mapsError,
                            loading = state.loadingMap,
                            onStart = viewModel::startLine,
                        )
                        is Screen.Play -> PlayScreen(
                            line = screen.line,
                            snapshot = state.snapshot,
                            onLibrary = viewModel::goLibrary,
                            onRetry = viewModel::retry,
                            onUserMove = viewModel::onUserMove,
                        )
                    }
                }
            }
        }
    }
}
