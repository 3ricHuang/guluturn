package com.eric.guluturn.semantic.templates

import com.eric.guluturn.common.utils.TagConfigLoader

object PromptTemplates {

    /**
     * Build a structured prompt that forces GPT to output a JSON array of tags.
     *
     * @param reason Original user statement in any language.
     * @return Prompt string ready for the LLM.
     */
    fun generatePrompt(reason: String): String {
        // Load tags and expose ONLY their names to the model to reduce token cost.
        val tagList = TagConfigLoader.loadAllTags()
            .joinToString(", ") { "\"${it.tag}\"" }

        return """
            You are a classification API that must respond with a JSON array only.
        
            ## Allowed tags
            [$tagList]
        
            ## Instructions
            1. Identify all tags that match the semantics of the input.
               • Multiple tags are allowed—sort them by relevance (most relevant first).  
               • If no tag applies, return an empty array [].
            2. Output ONLY the JSON array. Do not include explanations, markdown, or extra words.
        
            ## Format
            Output format: ["tag_1", "tag_2"]
        
            ## Examples
            Input: 我吃素
            Output: ["need_vegetarian_options", "avoid_meat", "avoid_seafood"]
        
            Input: 太辣又太油
            Output: ["quality_too_spicy", "quality_too_greasy"]
            
            Input: 我不吃豬肉
            Output: ["avoid_pork", "no_halal_options"]
        
            ## User input
            "$reason"
        """.trimIndent()

    }
}
