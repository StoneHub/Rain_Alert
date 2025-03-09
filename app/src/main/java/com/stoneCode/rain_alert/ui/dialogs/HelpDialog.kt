package com.stoneCode.rain_alert.ui.dialogs

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HelpDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                imageVector = Icons.Outlined.Info, 
                contentDescription = "Information"
            ) 
        },
        title = { 
            Text(
                text = "About Rain Alert",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // What the app does section
                SectionTitle(text = "What Rain Alert Does", icon = Icons.Outlined.Cloud)
                Text(
                    text = "Rain Alert monitors local weather stations for precipitation and freezing conditions. When detected, the app notifies you before the weather affects your area."
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // How it works section
                SectionTitle(text = "How It Works", icon = Icons.Default.LocationOn)
                Text(
                    text = "The app aggregates data from multiple nearby weather stations, weighing their reliability based on distance and recency of data. This gives you a more accurate picture of local conditions than using a single source."
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Required permissions section
                SectionTitle(text = "Required Permissions", icon = Icons.Default.Security)
                PermissionItem(
                    title = "Location", 
                    description = "Needed to find the nearest weather stations to your location"
                )
                PermissionItem(
                    title = "Notifications", 
                    description = "Required to alert you when rain or freezing conditions are detected"
                )
                PermissionItem(
                    title = "Background Service", 
                    description = "Allows continuous monitoring even when the app is closed"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Troubleshooting section
                SectionTitle(text = "Troubleshooting", icon = Icons.Default.Notifications)
                Text(
                    text = "If you're not receiving alerts:"
                )
                BulletPoint(text = "Ensure location and notification permissions are granted")
                BulletPoint(text = "Disable battery optimization for this app")
                BulletPoint(text = "Verify that the monitoring service is running")
                BulletPoint(text = "Check that you've selected at least one weather station")
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Permission control buttons
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text("Check Permissions")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Battery0Bar,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text("Battery Optimization")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun SectionTitle(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun PermissionItem(title: String, description: String) {
    Column(modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) {
        Text(
            text = "• $title",
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun BulletPoint(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun Row(
    modifier: Modifier = Modifier,
    horizontalArrangement: androidx.compose.foundation.layout.Arrangement.Horizontal = androidx.compose.foundation.layout.Arrangement.Start,
    verticalAlignment: androidx.compose.ui.Alignment.Vertical = androidx.compose.ui.Alignment.Top,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        content = content
    )
}