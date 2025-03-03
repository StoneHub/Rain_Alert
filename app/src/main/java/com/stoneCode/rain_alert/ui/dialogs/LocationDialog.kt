package com.stoneCode.rain_alert.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun LocationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onUseDeviceLocation: () -> Unit
) {
    var zipCode by remember { mutableStateOf("") }
    var zipCodeError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Weather Location") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("Enter a ZIP code or use your device location.")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = zipCode,
                    onValueChange = { 
                        zipCode = it.take(5)
                        zipCodeError = validateZipCode(it)
                    },
                    label = { Text("ZIP Code") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null
                        )
                    },
                    isError = zipCodeError != null,
                    supportingText = {
                        zipCodeError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onUseDeviceLocation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text("Use My Current Location")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val error = validateZipCode(zipCode)
                    if (error == null) {
                        onConfirm(zipCode)
                    } else {
                        zipCodeError = error
                    }
                }
            ) {
                Text("Set Location")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun validateZipCode(zipCode: String): String? {
    if (zipCode.isEmpty()) {
        return "ZIP code is required"
    }
    
    if (zipCode.length != 5) {
        return "ZIP code must be 5 digits"
    }
    
    if (!zipCode.all { it.isDigit() }) {
        return "ZIP code must contain only digits"
    }
    
    return null
}
