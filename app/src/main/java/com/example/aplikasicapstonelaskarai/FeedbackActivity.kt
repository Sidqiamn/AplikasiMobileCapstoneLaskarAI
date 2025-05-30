package com.example.aplikasicapstonelaskarai

import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class FeedbackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        val radioGroupFeeling = findViewById<RadioGroup>(R.id.radioGroupFeeling)
        val editTextFeedback = findViewById<TextInputEditText>(R.id.editTextFeedback)
        val buttonSubmit = findViewById<Button>(R.id.buttonSubmit)
        val textViewStatus = findViewById<TextView>(R.id.textViewStatus)

        buttonSubmit.setOnClickListener {
            val selectedId = radioGroupFeeling.checkedRadioButtonId
            val feedbackText = editTextFeedback.text.toString().trim()

            if (selectedId == -1) {
                textViewStatus.text = "Pilih salah satu opsi perasaan!"
                return@setOnClickListener
            }

            val selectedFeeling = findViewById<RadioButton>(selectedId).text.toString()

            textViewStatus.text = "Feedback: $selectedFeeling\nKomentar: $feedbackText"
            // Kembali ke MainActivity
            finish()
        }
    }
}