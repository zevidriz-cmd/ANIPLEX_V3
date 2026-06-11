package com.aniplex.app.presentation.screens.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aniplex.app.domain.model.UserProfile
import com.aniplex.app.theme.*

@Composable
fun ProfileSelectionScreen(
    onProfileSelected: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isMigrating by viewModel.isMigrating.collectAsState()
    val context = LocalContext.current

    // Screen State
    var isManageMode by remember { mutableStateOf(false) }
    var activeProfileForPin by remember { mutableStateOf<UserProfile?>(null) }
    var showCreateEditDialog by remember { mutableStateOf<UserProfile?>(null) } // if not null, show edit or create dialog (if ID is empty, it's create)

    // Handle back button when dialog or PIN entry is open
    BackHandler(enabled = activeProfileForPin != null || showCreateEditDialog != null || isManageMode) {
        if (activeProfileForPin != null) {
            activeProfileForPin = null
        } else if (showCreateEditDialog != null) {
            showCreateEditDialog = null
        } else if (isManageMode) {
            isManageMode = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BackgroundVoid, Color(0xFF0C0D12), BackgroundVoid)
                )
            )
    ) {
        when (val state = uiState) {
            is ProfileSelectionState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CrunchyrollOrange)
                        if (isMigrating) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Preparing your profiles...",
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            is ProfileSelectionState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = Color.Red, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadProfilesAndMigrate() },
                            colors = ButtonDefaults.buttonColors(containerColor = CrunchyrollOrange)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            is ProfileSelectionState.Success -> {
                ProfileSelectionContent(
                    profiles = state.profiles,
                    isManageMode = isManageMode,
                    onProfileClick = { profile ->
                        if (isManageMode) {
                            showCreateEditDialog = profile
                        } else {
                            if (profile.pin != null) {
                                activeProfileForPin = profile
                            } else {
                                viewModel.selectProfile(profile, onProfileSelected)
                            }
                        }
                    },
                    onAddProfileClick = {
                        showCreateEditDialog = UserProfile() // Empty profile indicates create
                    },
                    onToggleManageMode = {
                        isManageMode = !isManageMode
                    },
                    onLogOut = {
                        viewModel.signOut(onSignOut)
                    }
                )
            }
        }

        // Dialog for PIN entry
        activeProfileForPin?.let { profile ->
            PinEntryOverlay(
                profile = profile,
                onPinVerified = {
                    activeProfileForPin = null
                    viewModel.selectProfile(profile, onProfileSelected)
                },
                onCancel = {
                    activeProfileForPin = null
                },
                verifyPin = { pin -> viewModel.verifyPin(profile, pin) }
            )
        }

        // Dialog for Profile Creation / Edit
        showCreateEditDialog?.let { profile ->
            CreateEditProfileDialog(
                profile = profile,
                onDismiss = { showCreateEditDialog = null },
                onSave = { name, avatarUrl, pin ->
                    if (profile.id.isEmpty()) {
                        viewModel.createProfile(name, avatarUrl, pin) { result ->
                            result.onSuccess {
                                showCreateEditDialog = null
                                Toast.makeText(context, "Profile created successfully!", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, it.message ?: "Failed to create profile", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        viewModel.updateProfile(profile.id, name, avatarUrl, pin) { result ->
                            result.onSuccess {
                                showCreateEditDialog = null
                                isManageMode = false
                                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, it.message ?: "Failed to update profile", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onDelete = if (profile.id.isNotEmpty()) {
                    {
                        viewModel.deleteProfile(profile.id) { result ->
                            result.onSuccess {
                                showCreateEditDialog = null
                                isManageMode = false
                                Toast.makeText(context, "Profile deleted successfully!", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, it.message ?: "Failed to delete profile", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else null
            )
        }
    }
}

@Composable
fun ProfileSelectionContent(
    profiles: List<UserProfile>,
    isManageMode: Boolean,
    onProfileClick: (UserProfile) -> Unit,
    onAddProfileClick: () -> Unit,
    onToggleManageMode: () -> Unit,
    onLogOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isManageMode) "Manage Profiles" else "Who's going on an adventure?",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            profiles.forEach { profile ->
                ProfileItemCard(
                    profile = profile,
                    isManageMode = isManageMode,
                    onClick = { onProfileClick(profile) }
                )
            }

            if (profiles.size < 4) {
                AddProfileItemCard(onClick = onAddProfileClick)
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = onToggleManageMode,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isManageMode) CrunchyrollOrange else Color(0xFF1F1F1F)
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (isManageMode) Color.Transparent else Color.DarkGray)
        ) {
            Text(
                text = if (isManageMode) "Done" else "Manage Profiles",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onLogOut) {
            Text(
                text = "Log Out",
                color = CrunchyrollOrange,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ProfileItemCard(
    profile: UserProfile,
    isManageMode: Boolean,
    onClick: () -> Unit
) {
    val avatarColor = remember(profile.avatarUrl) { getAvatarColor(profile.avatarUrl) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profile.name.take(1).uppercase(),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            if (profile.pin != null && !isManageMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "PIN Protected",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (isManageMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = CrunchyrollOrange,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = profile.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AddProfileItemCard(
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color(0xFF191919))
                .border(2.dp, Color.DarkGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Profile",
                tint = Color.Gray,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Add Profile",
            color = Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PinEntryOverlay(
    profile: UserProfile,
    onPinVerified: () -> Unit,
    onCancel: () -> Unit,
    verifyPin: (String) -> Boolean
) {
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = Color.White
                )
            }

            Text(
                text = "Enter PIN to access Profile",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = profile.name,
                color = CrunchyrollOrange,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(40.dp))

            // PIN Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val filled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (pinError) ErrorColor
                                else if (filled) CrunchyrollOrange
                                else Color.DarkGray
                            )
                    )
                }
            }

            if (pinError) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Incorrect PIN. Try again.",
                    color = ErrorColor,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Keypad
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                val digits = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("Clear", "0", "Delete")
                )

                digits.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { char ->
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (char == "Clear" || char == "Delete") Color.Transparent
                                        else Color(0xFF1C1C1C)
                                    )
                                    .clickable {
                                        if (pinError) {
                                            pinError = false
                                            enteredPin = ""
                                        }

                                        when (char) {
                                            "Clear" -> enteredPin = ""
                                            "Delete" -> {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                }
                                            }
                                            else -> {
                                                if (enteredPin.length < 4) {
                                                    enteredPin += char
                                                    if (enteredPin.length == 4) {
                                                        if (verifyPin(enteredPin)) {
                                                            onPinVerified()
                                                        } else {
                                                            pinError = true
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    color = if (char == "Clear" || char == "Delete") CrunchyrollOrange else Color.White,
                                    fontSize = if (char == "Clear" || char == "Delete") 14.sp else 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateEditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (name: String, avatarUrl: String, pin: String?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(profile.name) }
    var selectedAvatar by remember { mutableStateOf(profile.avatarUrl.ifBlank { "avatar_orange" }) }
    
    var isPinEnabled by remember { mutableStateOf(profile.pin != null) }
    var pinText by remember { mutableStateOf("") }
    
    val avatarOptions = listOf("avatar_orange", "avatar_blue", "avatar_purple", "avatar_green")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (profile.id.isEmpty()) "Create Profile" else "Edit Profile",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 12) name = it },
                    label = { Text("Profile Name", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrunchyrollOrange,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedLabelColor = CrunchyrollOrange,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Avatar color selection
                Text(
                    text = "Select Avatar Theme Color:",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    avatarOptions.forEach { opt ->
                        val color = getAvatarColor(opt)
                        val isSelected = selectedAvatar == opt

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    2.dp,
                                    if (isSelected) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedAvatar = opt }
                        )
                    }
                }

                // PIN protection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPinEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isPinEnabled) CrunchyrollOrange else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Profile PIN Lock",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    Switch(
                        checked = isPinEnabled,
                        onCheckedChange = { isPinEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CrunchyrollOrange,
                            checkedTrackColor = CrunchyrollOrange.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }

                if (isPinEnabled) {
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinText = it
                            }
                        },
                        label = {
                            Text(
                                text = if (profile.pin != null && pinText.isEmpty()) "Leave empty to keep existing PIN" else "4-Digit PIN",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrunchyrollOrange,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val finalPin = if (isPinEnabled) {
                        if (pinText.isNotEmpty()) pinText else null // if editing and empty, keep current; if creating and empty, it will fail validation or we force pinText to be 4 digits
                    } else {
                        "REMOVE" // Sentinel value to clear PIN if disabled
                    }

                    if (isPinEnabled && profile.pin == null && pinText.length != 4) {
                        // Force a 4-digit PIN for new profiles
                        return@Button
                    }
                    onSave(name, selectedAvatar, finalPin)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrunchyrollOrange)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = ErrorColor)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        },
        containerColor = SurfaceDark
    )
}

fun getAvatarColor(avatarUrl: String): Color {
    return when (avatarUrl) {
        "avatar_orange" -> CrunchyrollOrange
        "avatar_blue" -> Color(0xFF2196F3)
        "avatar_purple" -> Color(0xFF9C27B0)
        "avatar_green" -> Color(0xFF4CAF50)
        else -> Color(0xFF757575)
    }
}
