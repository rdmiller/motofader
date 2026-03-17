package com.motofader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.motofader.ui.screens.DspScreen
import com.motofader.ui.screens.MixerScreen
import com.motofader.ui.screens.SpectrumScreen
import com.motofader.ui.theme.Amber
import com.motofader.ui.theme.DarkBackground
import com.motofader.ui.theme.DarkSurface
import com.motofader.ui.theme.MutedText
import com.motofader.ui.theme.MotoFaderTheme
import com.motofader.viewmodel.MixerViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val viewModel: MixerViewModel by viewModels()
    private val _permissionDenied = MutableStateFlow(false)
    private var hasRecordPermission = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            hasRecordPermission = true
            _permissionDenied.value = false
            viewModel.startCapture()
        } else {
            _permissionDenied.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            hasRecordPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            MotoFaderTheme {
                MotoFaderApp(viewModel, _permissionDenied)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startVolumeMonitor()
        viewModel.startDsp()
        if (hasRecordPermission) {
            viewModel.startCapture()
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopCapture()
        viewModel.stopVolumeMonitor()
        viewModel.stopDsp()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshVolumes()
    }
}

private data class TabItem(val title: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("Mix", Icons.Filled.Tune),
    TabItem("Spectrum", Icons.Filled.Equalizer),
    TabItem("DSP", Icons.Filled.GraphicEq),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MotoFaderApp(viewModel: MixerViewModel, permissionDenied: MutableStateFlow<Boolean>) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val denied by permissionDenied.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MotoFader",
                        style = MaterialTheme.typography.titleMedium,
                        color = Amber,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkBackground,
                contentColor = Color.White,
                tonalElevation = 0.dp,
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Amber,
                            selectedTextColor = Amber,
                            unselectedIconColor = MutedText,
                            unselectedTextColor = MutedText,
                            indicatorColor = DarkSurface,
                        ),
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBackground)
        ) {
            if (denied) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "RECORD_AUDIO permission required for VU meters and spectrum analyzer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedText,
                    )
                }
            }
            when (selectedTab) {
                0 -> MixerScreen(viewModel)
                1 -> SpectrumScreen(viewModel)
                2 -> DspScreen(viewModel)
            }
        }
    }
}
