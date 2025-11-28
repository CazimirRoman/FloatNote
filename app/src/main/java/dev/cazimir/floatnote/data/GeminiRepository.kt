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
            temperature = 0.1f
            topK = 32
            topP = 1f
        },
        systemInstruction = com.google.ai.client.generativeai.type.content {
            text(
                "You are a precise text polisher. Improve spelling, grammar, punctuation, and word order to make the text clear and well-structured. " +
                "Do not alter the meaning. Do not introduce new words or synonyms—keep the user's original wording whenever possible. " +
                "Fix misspellings, adjust sentence structure for readability, and ensure proper capitalization and punctuation. " +
                "Return only the corrected version of the user's text, without any commentary or extra explanations."
            )
        }
    )

    suspend fun formatText(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent(
                com.google.ai.client.generativeai.type.content {
                    // Provide a concise directive with the user text
                    text("Correct and polish the following text while preserving exact meaning and wording as much as possible:\n\n$text")
                }
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
