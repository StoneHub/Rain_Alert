package com.stoneCode.rain_alert.feedback

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.StoneCode.rain_alert.R

/**
 * Activity for collecting user feedback about alert accuracy
 */
class FeedbackActivity : AppCompatActivity() {
    private lateinit var alertFeedbackManager: AlertFeedbackManager
    private var alertId: String? = null
    private var alertType: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)
        
        alertFeedbackManager = AlertFeedbackManager.getInstance(this)
        
        // Get alert details from intent
        alertId = intent.getStringExtra(AlertFeedbackManager.EXTRA_ALERT_ID)
        alertType = intent.getStringExtra(AlertFeedbackManager.EXTRA_ALERT_TYPE)
        
        if (alertId == null || alertType == null) {
            Toast.makeText(this, "Error: Missing alert information", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupUI()
    }
    
    private fun setupUI() {
        // Set the title based on alert type
        val titleTextView = findViewById<TextView>(R.id.feedbackTitleTextView)
        titleTextView.text = when (alertType) {
            "rain" -> "How accurate was our rain alert?"
            "freeze" -> "How accurate was our freeze warning?"
            else -> "How accurate was our weather alert?"
        }
        
        // Set up the submit button
        val submitButton = findViewById<Button>(R.id.submitFeedbackButton)
        submitButton.setOnClickListener {
            submitFeedback()
        }
    }
    
    private fun submitFeedback() {
        // Get the accuracy rating (1-5)
        val accuracyRatingGroup = findViewById<RadioGroup>(R.id.accuracyRatingGroup)
        val selectedRatingId = accuracyRatingGroup.checkedRadioButtonId
        
        if (selectedRatingId == -1) {
            Toast.makeText(this, "Please select an accuracy rating", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Map the selected radio button to a score from 1-5
        val accuracyScore = when (selectedRatingId) {
            R.id.ratingButton1 -> 1
            R.id.ratingButton2 -> 2
            R.id.ratingButton3 -> 3
            R.id.ratingButton4 -> 4
            R.id.ratingButton5 -> 5
            else -> 3 // Default to middle value
        }
        
        // Get any additional feedback text
        val feedbackEditText = findViewById<EditText>(R.id.feedbackEditText)
        val feedbackText = feedbackEditText.text.toString().trim()
        
        // Get the actual conditions description
        val actualConditionsEditText = findViewById<EditText>(R.id.actualConditionsEditText)
        val actualConditions = actualConditionsEditText.text.toString().trim()
        
        // Get algorithm data if available
        val algorithmData = intent.getSerializableExtra(AlertFeedbackManager.EXTRA_ALGORITHM_DATA) as? HashMap<String, Any?>
        
        // Record the feedback
        alertFeedbackManager.recordFeedback(
            alertId = alertId!!,
            alertType = alertType!!,
            accuracyScore = accuracyScore,
            feedback = if (feedbackText.isNotEmpty()) feedbackText else null,
            actualConditionsDescription = if (actualConditions.isNotEmpty()) actualConditions else null,
            algorithmData = algorithmData
        )
        
        // Thank the user and close the activity
        Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
        finish()
    }
}