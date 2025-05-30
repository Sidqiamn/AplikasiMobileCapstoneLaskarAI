package com.example.aplikasicapstonelaskarai

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.util.Random

class ActScriptManager(context: Context) {
    private val scripts = mutableMapOf<String, List<String>>(
        "happy" to emptyList(),
        "sadness" to emptyList(),
        "fear" to emptyList(),
        "stress" to emptyList()
    )
    private val githubUrls = mapOf(
        "happy" to "https://raw.githubusercontent.com/Sidqiamn/Dataset_Capstone_LaskarAI/refs/heads/master/act_happy.txt",
        "sadness" to "https://raw.githubusercontent.com/Sidqiamn/Dataset_Capstone_LaskarAI/refs/heads/master/act_sadness.txt",
        "fear" to "https://raw.githubusercontent.com/Sidqiamn/Dataset_Capstone_LaskarAI/refs/heads/master/act_fear.txt",
        "stress" to "https://raw.githubusercontent.com/Sidqiamn/Dataset_Capstone_LaskarAI/refs/heads/master/act_stress.txt"
    )
    private val queue = Volley.newRequestQueue(context)
    private val TAG = "ActScriptManager"

    init {
        loadScripts()
    }

    private fun loadScripts() {
        githubUrls.forEach { (emotion, url) ->
            val request = StringRequest(Request.Method.GET, url,
                { response ->
                    val parsedScripts = parseScripts(response)
                    scripts[emotion] = parsedScripts
                    Log.d(TAG, "Loaded ${parsedScripts.size} scripts for $emotion")
                },
                { error ->
                    Log.e(TAG, "Failed to load scripts for $emotion: ${error.message}")
                    scripts[emotion] = listOf("Skrip default untuk $emotion...")
                })
            queue.add(request)
        }
    }

    private fun parseScripts(content: String): List<String> {
        // Asumsi skrip dipisahkan oleh baris kosong atau "## Skrip"
        val scripts = mutableListOf<String>()
        var currentScript = mutableListOf<String>()
        content.split("\n").forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("## Skrip") && currentScript.isNotEmpty()) {
                scripts.add(currentScript.joinToString("\n").trim())
                currentScript = mutableListOf()
            } else if (trimmedLine.isNotEmpty()) {
                currentScript.add(trimmedLine)
            }
        }
        if (currentScript.isNotEmpty()) {
            scripts.add(currentScript.joinToString("\n").trim())
        }
        return scripts
    }

    fun getRandomScript(emotion: String): String {
        val emotionScripts = scripts[emotion] ?: listOf("Maaf, skrip tidak tersedia untuk $emotion.")
        return if (emotionScripts.isNotEmpty()) {
            emotionScripts[Random().nextInt(emotionScripts.size)]
        } else {
            "Maaf, skrip tidak tersedia untuk $emotion."
        }
    }
}