package com.ultratv.tv.nativeapp.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit = {},
    vm: SettingsViewModel = hiltViewModel(),
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val S = com.ultratv.tv.nativeapp.i18n.LocalStrings.current

    val T = com.ultratv.tv.nativeapp.ui.theme.UltraTokens
    val F = com.ultratv.tv.nativeapp.ui.theme.UltraFonts
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = T.EdgeGutter, end = T.EdgeGutter, top = 40.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "RÉGLAGES",
            color = T.Fg3,
            fontSize = 11.sp,
            letterSpacing = 2.3.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            S.settingsTitle,
            fontFamily = F.Serif,
            fontSize = 56.sp,
            lineHeight = 56.sp,
            letterSpacing = (-1.5).sp,
            color = T.Fg,
        )
        Spacer(Modifier.height(8.dp))

        // Manual update check — useful when the launch-time auto-check
        // missed (no network at start, dialog dismissed too early, etc.).
        val updateInfo by com.ultratv.tv.nativeapp.update.UpdateChecker.state.collectAsState()
        var checking by remember { mutableStateOf(false) }
        var checkMsg by remember { mutableStateOf<String?>(null) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (checking) return@Button
                    checking = true
                    checkMsg = null
                    scope.launch {
                        val info = com.ultratv.tv.nativeapp.update.UpdateChecker.checkForUpdate()
                        checking = false
                        checkMsg = if (info != null) S.settingsUpdateAvailableTemplate.format(info.versionName)
                        else S.settingsUpToDateTemplate.format(com.ultratv.tv.nativeapp.BuildConfig.VERSION_NAME)
                    }
                },
            ) {
                Text(if (checking) S.settingsCheckingForUpdates else S.settingsCheckForUpdates, fontSize = 14.sp)
            }
            checkMsg?.let { Text(it, color = T.Fg3, fontSize = 13.sp) }
            if (updateInfo != null) {
                Text(
                    "v${updateInfo!!.versionName} prête à installer",
                    color = T.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(16.dp))




        // ---- 4. Display & playback ----
        SectionCard {
            Text(S.settingsDisplay, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            PreferencesSection()
        }


        // ---- 5. Parental ----
        SectionCard {
            Text(S.settingsParental, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            com.ultratv.tv.nativeapp.ui.parental.ParentalSection(
                onManageLockedChannels = { onNavigate("locked-channels") },
            )
            Text(
                S.settingsParentalHint,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
    }


private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(com.ultratv.tv.nativeapp.ui.theme.UltraTokens.Surface1)
            .androidx_border()
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun Modifier.androidx_border(): Modifier =
    this.border(
        1.dp,
        com.ultratv.tv.nativeapp.ui.theme.UltraTokens.Line,
        RoundedCornerShape(16.dp),
    )

private typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope
