package com.eric.guluturn.semantic.templates

import com.eric.guluturn.common.utils.TagConfigLoader

object PromptTemplates {

    /**
     * Build a prompt that forces an LLM to return a single JSON object with keys:
     *
     * {
     *   "user_input": "<original sentence>",
     *   "general_tags": ["tag_a", "tag_b"],
     *   "specific_tags": [
     *     { "tag": "<phrase>", "polarity": "positive|negative" }
     *   ]
     * }
     *
     * Rules:
     *   • general_tags must come only from the allowed list.
     *   • specific_tags are short verbatim phrases from the input and carry polarity.
     *   • Output must be pure JSON (no markdown, no comments, no extra text).
     */
    fun generatePrompt(reason: String): String {
        val allowedTags = TagConfigLoader.loadAllTags()
            .joinToString(", ") { "\"${it.tag}\"" }

        return """
        You are a tagging API. Respond ONLY with a single JSON object.
        No preamble, no explanation, no markdown. If the format is invalid or fields are missing, the response will be rejected.

        Format your response using this exact schema:
        {
          "user_input": "<original sentence>",
          "general_tags": ["tag1", "tag2"],
          "specific_tags": [
            { "tag": "<phrase_in_english>", "polarity": "positive|negative" }
          ]
        }

        ===== GENERAL TAGS RULES =====
        - Select ONLY from the allowed_tags list shown below.
        - DO NOT invent, rephrase, or hallucinate any tag names. Use only those provided.
        - DO NOT transform into "quality_*" tags unless explicitly listed.
        - Tag names are case-sensitive and must match exactly.
        - You MUST assign at least one general_tag. If no good match exists, fallback to ["not_in_the_mood"].

        ===== SEMANTIC MAPPING HINTS =====
        Use your understanding of intent and tone to infer general_tags:
        - Input expresses health or diet control:
            e.g., "我想控制體重", "最近減脂", "不想太負擔"
            → ["avoid_greasy_dishes", "avoid_sweet_dishes", "prefer_fresh_dishes"]

        - Input expresses indulgence or emotional reward:
            e.g., "想吃爽一點", "想犒賞自己", "今天壓力大"
            → ["prefer_greasy_dishes", "avoid_fresh_dishes"]

        - Input expresses apathy, resistance, or fatigue:
            e.g., "不想動", "隨便", "都可以", "沒胃口"
            → ["not_in_the_mood"]

        - Input expresses time constraint or urgency:
            e.g., "快遲到了", "趕時間", "能快點吃完的最好"
            → ["prefer_quick_meal"]

        - Input expresses food allergies:
            e.g., "最近過敏", "不能吃堅果", "對花生過敏"
            → ["allergy_concern"]

        ===== ALLOWED GENERAL_TAGS =====
        [$allowedTags]

        ===== SPECIFIC TAGS RULES =====
        - Extract short verbatim phrases (1–6 characters) from the input.
        - Translate each to English.
        - Assign polarity:
            "positive" → shows preference, desire, or acceptance.
            "negative" → shows rejection, avoidance, or dislike.

        ===== EXAMPLE =====
        Input: 我今天想吃點清爽的，太油太鹹都不行

        Output:
        {
          "user_input": "我今天想吃點清爽的，太油太鹹都不行",
          "general_tags": ["avoid_greasy_dishes", "avoid_salty_dishes"],
          "specific_tags": [
            {"tag": "refreshing", "polarity": "positive"},
            {"tag": "too greasy", "polarity": "negative"},
            {"tag": "too salty", "polarity": "negative"}
          ]
        }

        User input:
        "$reason"
    """.trimIndent()
    }
}
