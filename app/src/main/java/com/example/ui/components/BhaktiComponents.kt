package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberPaid
import com.example.ui.theme.AmberPaidContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianOutlineGold
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.SacredGold

@Composable
fun SacredCard(
    modifier: Modifier = Modifier,
    goldBorder: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (goldBorder) ObsidianOutlineGold else ObsidianOutline
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun ServiceTierBadge(
    isFree: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isFree) EmeraldContainer else AmberPaidContainer
    val fgColor = if (isFree) EmeraldFree else AmberPaid
    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, fgColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isFree) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fgColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun GoldAccentDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        SacredGold.copy(alpha = 0.6f),
                        RadiantGold,
                        SacredGold.copy(alpha = 0.6f),
                        Color.Transparent
                    )
                )
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingTransparencySheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceElevated,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = SacredGold
                )
                Text(
                    text = "Free vs Paid Services",
                    style = MaterialTheme.typography.headlineSmall,
                    color = RadiantGold
                )
            }

            Text(
                text = "BhaktiMotion AI never activates paid billing automatically. Paid video rendering adapters remain disconnected unless you explicitly wire a provider later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = ObsidianSurfaceHigh,
                border = BorderStroke(1.dp, EmeraldFree.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ServiceTierBadge(isFree = true, label = "FREE • ACTIVE NOW")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "• AI Movie Director (Gemini 3.8 Flash via Firebase AI Logic)\n" +
                            "• Character Vision Analyzer & Continuity Library (Firestore)\n" +
                            "• Sanskrit & Hindi Voice Coach + On-Device Speech Preview\n" +
                            "• Storyboard Preview Player, SRT Subtitles & Script Export\n" +
                            "• Production Shot Packet Compiler",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = ObsidianSurfaceHigh,
                border = BorderStroke(1.dp, AmberPaid.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ServiceTierBadge(isFree = false, label = "PAID • NOT CONNECTED")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "• Cloud Video Generation (Veo / Runway / Kling Adapters)\n" +
                            "• Neural Studio Voice Dubbing API\n" +
                            "• Cloud MP4 Video Rendering & Compositing\n\n" +
                            "All video jobs created for unconnected providers are stored transparently as 'Awaiting Provider' specifications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(color = ObsidianOutline)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = SacredGold,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "All Gemini calls run securely through Firebase AI Logic & App Check with zero frontend API keys.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
