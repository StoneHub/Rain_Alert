package com.stoneCode.rain_alert.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoneCode.rain_alert.feedback.AlertFeedback

/**
 * A dialog that allows users to provide feedback on the accuracy of weather alerts
 * This feedback is used to improve the algorithm over time
 */
@Composable
fun AlertFeedbackDialog(
    alertType: String,
    alertId: String,
    onDismiss: () -> Unit,
    onSubmitFeedback: (AlertFeedback) -> Unit,
    algorithmData: Map<String, Any?>? = null
) {
    var wasAccurate by remember { mutableStateOf<Boolean?>(null) }
    var comments by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Was this alert accurate?") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Please let us know if this ${alertType.lowercase()} alert was accurate. Your feedback helps us improve our predictions.")
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { wasAccurate = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (wasAccurate == true) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Yes")
                    }
                    
                    Button(
                        onClick = { wasAccurate = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (wasAccurate == false) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("No")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Additional comments (optional):")
                OutlinedTextField(
                    value = comments,
                    onValueChange = { comments = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (wasAccurate != null) {
                        val feedback = AlertFeedback(
                            alertId = alertId,
                            alertType = alertType,
                            wasAccurate = wasAccurate!!,
                            userComments = comments.takeIf { it.isNotBlank() },
                            // Extract algorithm details if available
                            stationCount = algorithmData?.get("stationCount") as? Int,
                            weightedPercentage = algorithmData?.get("weightedPercentage") as? Double,
                            maxDistance = algorithmData?.get("maxDistance") as? Double,
                            usedMultiStationApproach = algorithmData?.get("usedMultiStationApproach") as? Boolean ?: false,
                            thresholdUsed = algorithmData?.get("thresholdUsed") as? Double,
                            confidenceScore = algorithmData?.get("confidenceScore") as? Float
                        )
                        onSubmitFeedback(feedback)
                        onDismiss()
                    }
                },
                enabled = wasAccurate != null
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
