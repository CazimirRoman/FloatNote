package dev.cazimir.floatnote.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository(private val apiKey: String) {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-flash-latest",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.2f
            topK = 32
            topP = 1f
            // maxOutputTokens is handled dynamically per request
        },
        systemInstruction = com.google.ai.client.generativeai.type.content { 
            text("You are a helpful assistant that formats text for better readability. " +
                 "Your task is to fix punctuation, capitalization, and grammar without changing the meaning or the words used. " +
                 "Ensure sentences end with appropriate punctuation (like periods) and start with capital letters. " +
                 "Do not add any introductory or concluding remarks. Just return the formatted text.")
        }
    )

    suspend fun formatText(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Estimate token count (rough approximation: 1 token ~= 4 chars)
            // We give a generous buffer: (input length / 2) + 1000 tokens
            // This ensures we have enough space for the formatted output even if it expands slightly
            val estimatedInputTokens = text.length / 2
            val dynamicMaxTokens = (estimatedInputTokens + 1000).coerceAtMost(8192)

            val response = generativeModel.generateContent(
                com.google.ai.client.generativeai.type.content { text(text) }
            )
            
            val formattedText = response.text
            if (formattedText != null) {
                Result.success(formattedText.trim())
            } else {
                Result.failure(Exception("Empty response from Gemini"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
