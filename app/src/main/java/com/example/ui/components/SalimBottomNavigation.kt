package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen

enum class SalimTab(val label: String, val icon: ImageVector) {
    FAVORITES("Favorites", Icons.Default.Star),
    RECENTS("Recents", Icons.Default.AccessTime),
    CONTACTS("Contacts", Icons.Default.Person),
    KEYPAD("Keypad", Icons.Default.Dialpad),
    VOICEMAIL("Voicemail", Icons.Default.Voicemail)
}

@Composable
fun SalimBottomNavigation(
    selectedTab: SalimTab,
    onTabSelected: (SalimTab) -> Unit,
    missedCallsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SalimTab.values().forEach { tab ->
                    val isSelected = tab == selectedTab
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "tab_scale"
                    )

                    val interactionSource = remember { MutableInteractionSource() }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .scale(scale)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Tab,
                                onClick = { onTabSelected(tab) }
                            )
                            .testTag("tab_${tab.name.lowercase()}")
                            .semantics {
                                contentDescription = if (tab == SalimTab.RECENTS && missedCallsCount > 0) {
                                    "${tab.label}, $missedCallsCount missed calls"
                                } else {
                                    tab.label
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )

                            if (tab == SalimTab.RECENTS && missedCallsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(AccentGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (missedCallsCount > 9) "9+" else missedCallsCount.toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
