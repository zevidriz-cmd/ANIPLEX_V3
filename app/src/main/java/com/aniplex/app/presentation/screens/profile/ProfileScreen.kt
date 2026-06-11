package com.aniplex.app.presentation.screens.profile

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aniplex.app.theme.*

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onWatchlistClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSwitchProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val user = viewModel.currentUser
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()

    val defaultAudio by viewModel.defaultAudioCategory.collectAsStateWithLifecycle()
    val autoplay by viewModel.autoplayNextEpisode.collectAsStateWithLifecycle()
    val quality by viewModel.preferredQuality.collectAsStateWithLifecycle()
    val skipIntro by viewModel.skipIntro.collectAsStateWithLifecycle()
    val skipOutro by viewModel.skipOutro.collectAsStateWithLifecycle()
    val downloadOverCellular by viewModel.downloadOverCellular.collectAsStateWithLifecycle()

    var qualityExpanded by remember { mutableStateOf(false) }
    val qualityOptions = listOf("Auto", "1080p", "720p", "360p")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundVoid)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Profile Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CrunchyrollOrange.copy(alpha = 0.3f),
                            BackgroundVoid
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Avatar Box with Edit icon overlay
                val avatarColor = getAvatarColor(activeProfile?.avatarUrl ?: "avatar_orange")
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(avatarColor)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onSwitchProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (activeProfile?.name ?: user?.displayName ?: "Boss").take(1).uppercase(),
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Profile Name
                Text(
                    text = activeProfile?.name ?: user?.displayName?.takeIf { it.isNotBlank() } ?: "Boss",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // 2. Settings list matching Stitch
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(SurfaceDark, RoundedCornerShape(12.dp))
                .border(1.dp, SurfaceDarkVariant, RoundedCornerShape(12.dp))
        ) {
            // Switch Profile
            SettingsRow(
                title = "Switch Profile",
                onClick = onSwitchProfile
            )
            HorizontalDivider(color = SurfaceDarkVariant)

            // Profile PIN
            SettingsRow(
                title = "Manage Profiles & PIN Lock",
                onClick = onSwitchProfile
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Viewing Preferences
        Text(
            text = "${user?.displayName?.takeIf { it.isNotBlank() } ?: "Boss"}'s Viewing Preferences",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .background(SurfaceDark, RoundedCornerShape(12.dp))
                .border(1.dp, SurfaceDarkVariant, RoundedCornerShape(12.dp))
        ) {
            // Audio Language
            SettingsRow(
                title = "Default Audio Language",
                valueText = defaultAudio.uppercase(),
                onClick = {
                    val nextVal = if (defaultAudio == "sub") "dub" else "sub"
                    viewModel.setDefaultAudioCategory(nextVal)
                }
            )
            HorizontalDivider(color = SurfaceDarkVariant)

            // Quality Selection
            Box {
                SettingsRow(
                    title = "Streaming Quality",
                    valueText = quality,
                    onClick = { qualityExpanded = true }
                )
                DropdownMenu(
                    expanded = qualityExpanded,
                    onDismissRequest = { qualityExpanded = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    qualityOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = Color.White) },
                            onClick = {
                                viewModel.setPreferredQuality(option)
                                qualityExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Experience
        Text(
            text = "App Experience",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .background(SurfaceDark, RoundedCornerShape(12.dp))
                .border(1.dp, SurfaceDarkVariant, RoundedCornerShape(12.dp))
        ) {
            // Autoplay Next Episode
            SettingsSwitchRow(
                title = "Autoplay Next Episode",
                checked = autoplay,
                onCheckedChange = { viewModel.setAutoplayNextEpisode(it) }
            )
            HorizontalDivider(color = SurfaceDarkVariant)

            // Skip Intro
            SettingsSwitchRow(
                title = "Auto-Skip Intro",
                checked = skipIntro,
                onCheckedChange = { viewModel.setSkipIntro(it) }
            )
            HorizontalDivider(color = SurfaceDarkVariant)

            // Skip Outro
            SettingsSwitchRow(
                title = "Auto-Skip Outro",
                checked = skipOutro,
                onCheckedChange = { viewModel.setSkipOutro(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Downloads Setting Section
        Text(
            text = "Downloads Settings",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .background(SurfaceDark, RoundedCornerShape(12.dp))
                .border(1.dp, SurfaceDarkVariant, RoundedCornerShape(12.dp))
        ) {
            // Download Using Cellular Settings
            SettingsSwitchRow(
                title = "Download Using Cellular",
                checked = downloadOverCellular,
                onCheckedChange = { viewModel.setDownloadOverCellular(it) }
            )
            HorizontalDivider(color = SurfaceDarkVariant)

            // Clear Cache
            SettingsRow(
                title = "Clear Image & API Cache",
                onClick = {
                    viewModel.clearCache {
                        Toast.makeText(context, "Local app cache cleared successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Log Out Button
        Button(
            onClick = { viewModel.signOut(onSignOut) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(50.dp)
                .border(1.dp, NetflixRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Log Out",
                    tint = NetflixRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LOG OUT",
                    color = NetflixRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Footer version & policy info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Version 3.110.1 (1148)",
                fontSize = 11.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val context = LocalContext.current
                Text(
                    text = "Terms of Service",
                    fontSize = 11.sp,
                    color = CrunchyrollOrange,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { 
                        android.widget.Toast.makeText(context, "Opening Terms of Service...", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                Text(
                    text = "Privacy Policy",
                    fontSize = 11.sp,
                    color = CrunchyrollOrange,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { 
                        android.widget.Toast.makeText(context, "Opening Privacy Policy...", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    valueText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (valueText != null) {
                Text(
                    text = valueText,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CrunchyrollOrange,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceDarkVariant
            )
        )
    }
}
