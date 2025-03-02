package com.stoneCode.rain_alert.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.sharp.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ApiStatus(
    val isConnected: Boolean,
    val lastUpdated: Long,
    val errorMessage: String? = null,
    val responseTime: Long? = null,
    val rainProbability: Int? = null,
    val temperature: Double? = null
)

@Composable
fun ApiStatusWidget(
    apiStatus: ApiStatus
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weather API Status", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Status icon
                when {
                    !apiStatus.isConnected -> {
                        Icon(
                            imageVector = Icons.Sharp.Refresh,
                            contentDescription = "API Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    apiStatus.errorMessage != null -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "API Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "API OK",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Divider()
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status details
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Status: ${if (apiStatus.isConnected) "Connected" else "Disconnected"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "Last Updated: ${dateFormat.format(Date(apiStatus.lastUpdated))}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                apiStatus.responseTime?.let {
                    Text(
                        text = "Response Time: ${it}ms",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                apiStatus.rainProbability?.let {
                    Text(
                        text = "Rain Probability: $it%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                apiStatus.temperature?.let {
                    Text(
                        text = "Temperature: ${it}°F",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                apiStatus.errorMessage?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}