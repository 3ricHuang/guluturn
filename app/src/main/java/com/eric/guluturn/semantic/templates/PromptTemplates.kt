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
            You are the backend tagging module of a production recommendation system.
            
            Your job is to extract structured semantic information from user input.  
            Return exactly ONE JSON object following the schema below.  
            No markdown, no explanations, no comments – any extra text will break downstream systems.
            
            ── JSON SCHEMA ──
            {
              "user_input": "<original sentence>",
              "general_tags": ["<tag1>", "<tag2>", ...],
              "specific_tags": [
                { "tag": "<translated_phrase>", "polarity": "positive|negative" }
              ],
              "preferred_restaurants": ["<name1>", "<name2>", ...]
            }
            
            • The user input may be written in Chinese, English, or a mixture of both.  
            • Your model must accurately understand and parse any such combination.
            
            ── GENERAL_TAGS RULES ──
            • Must be selected ONLY from the allowed list below (case-sensitive, exact match).  
            • Choose as many tags as needed to reflect the full **semantic density** of the user input.  
            • Use your understanding of mood, tone, preference, and constraints.  
            • If the user mentions the store is closed, include "user_report_closed".  
            • If the input is gibberish, emotionally vague, or lacks interpretable meaning, return ["user_input_gibberish"].  
            • NEVER make up new tag names or apply slight variations.
            • You are encouraged to fully utilize all allowed tags when appropriate — do not ignore meaningful user needs, even if subtly expressed.  
            • Don't tag just because a keyword appears — only tag if it reflects an actual user sentiment or preference.
            • Your tag vocabulary covers a wide range of dimensions: cuisine, dish type, meal time, temperature, texture, price, crowd, environment, and more — you should not skip relevant dimensions.
            
            
            ── PREFERRED_RESTAURANTS RULES ──
            • Include **exact restaurant names** or nicknames as they appear in the user input.  
            • DO NOT translate or alter the name in any way.  
            • Names may appear in Chinese, English, or Romanized form — copy them exactly.  
            • If no name is mentioned, use an empty list [].  
            • If the user lists specific names followed by a general fallback (e.g., "不然牛肉麵店也可以"), treat names as preferred_restaurants, and extract tags only from the fallback clause.
            
            ── SPECIFIC_TAGS RULES ──
            • Only include short, literal dish names that the user **wants to eat**.
            • These dish names must be clearly mentioned or implied in the user input (e.g., "beef noodles", "hot pot", "fried chicken", "Thai food").
            • Exclude any non-dish concepts such as portion size, flavor, texture, or price.
            • All specific_tags must have `"polarity": "positive"` — do not generate negative ones.
            • Do NOT repeat or resemble any general_tags (e.g., don’t return "prefer_noodles" as a specific_tag).
            • Translate Chinese or mixed-language dish names into clear, idiomatic English (e.g., "牛肉麵" → "beef noodles", "鹹酥雞" → "fried chicken").
            • Dish names may include: "beef noodles", "hot pot", "fried rice", "fried chicken", "bento", "sushi", "Thai food", "Vietnamese noodles", "lu wei", etc.
            • Do not include terms like "too salty", "refreshing", "greasy", "big portion", "long wait", etc.
            • NEVER assign "polarity": "negative" — use general_tags (e.g., "avoid_beef") for that instead.
            • Tags must follow consistent formatting:
                - Lowercase only
                - No articles (a, an, the)
                - Use singular noun unless plural is idiomatic (e.g., "noodles")
                - No punctuation, emoji, or mixed language
                - Only idiomatic English phrases (e.g., "fried chicken", not "雞排" or "chicken chop")
            • Do not mix English and Chinese in the same tag.
            • Examples of valid tags: "fried chicken", "beef noodles", "sushi", "Thai food", "Vietnamese noodles", "seafood bento"
            • Normalize variants to standard idiomatic phrases (e.g., "ramen shop" → "ramen", "luwei" → "lu wei"). Avoid redundancies like "noodle soup", use concise canonical names.

            ── SEMANTIC HINTS ──
            Use the **meaning** behind the sentence, not keyword matching.
            
            Examples:
            - "最近減肥，想吃清爽的" → ["avoid_greasy_dishes", "prefer_fresh_dishes"]
            - "太甜、太油都不行" → ["avoid_sweet_dishes", "avoid_greasy_dishes"]
            - "火鍋太貴又太擠" → ["too_expensive", "too_crowded"]
            - "不想想" → ["user_input_gibberish"]
            - "想不到" → ["user_input_gibberish"]
            - "隨便" → ["user_input_gibberish"]
            - "都可以" → ["user_input_gibberish"]
            - "你決定" → ["user_input_gibberish"]
            - "吃點熱的也行" → ["prefer_hot_items"]
            
            ── ALLOWED GENERAL_TAGS ──
            [$allowedTags]
            
            ── OUTPUT FORMAT CONSTRAINTS ──
            • Output must be raw, valid JSON — no markdown (like ```json), no explanations.  
            • Ensure all fields are included. If empty, use empty array `[]`.
            
            ── EXAMPLES ──  
            
            Example 1:  
            Input: 我今天想吃點清爽的，太油太鹹都不行  
            Output:
            {
              "user_input": "我今天想吃點清爽的，太油太鹹都不行",
              "general_tags": ["avoid_greasy_dishes", "avoid_salty_dishes", "prefer_fresh_dishes"],
              "specific_tags": [],
              "preferred_restaurants": []
            }
            
            Example 2:  
            Input: 我想吃拉亞、左岸或品翔，不然牛肉麵店也可以  
            Output:
            {
              "user_input": "我想吃拉亞、左岸或品翔，不然牛肉麵店也可以",
              "general_tags": [
                "prefer_beef", "prefer_noodles", "prefer_soup_dishes", "prefer_hot_items", "prefer_taiwanese_cuisine", "prefer_meat"
              ],
              "specific_tags": [
                { "tag": "beef noodles", "polarity": "positive" }
              ],
              "preferred_restaurants": ["拉亞", "左岸", "品翔"]
            }
            
            Example 3:  
            Input: 咕嘰咕嘰！餓餓咚咚～隨便啦呼呼呼  
            Output:
            {
              "user_input": "咕嘰咕嘰！餓餓咚咚～隨便啦呼呼呼",
              "general_tags": ["user_input_gibberish"],
              "specific_tags": [],
              "preferred_restaurants": []
            }
            
            Example 4:  
            Input: 那間店今天應該沒開，我記得它週二公休  
            Output:
            {
              "user_input": "那間店今天應該沒開，我記得它週二公休",
              "general_tags": ["user_report_closed"],
              "specific_tags": [],
              "preferred_restaurants": []
            }
            
            Example 5:  
            Input: 我想吃拉亞、左岸或品翔，不然就沒想法了  
            Output:
            {
              "user_input": "我想吃拉亞、左岸或品翔，不然就沒想法了",
              "general_tags": ["user_input_gibberish"],
              "specific_tags": [],
              "preferred_restaurants": ["拉亞", "左岸", "品翔"]
            }
            
            Example 6:  
            Input: That ramen shop was too salty and too hot.  
            Output:
            {
              "user_input": "That ramen shop was too salty and too hot.",
              "general_tags": ["quality_too_salty", "quality_too_hot","avoid_soup_dishes"],
              "specific_tags": [],
              "preferred_restaurants": []
            }
            
            Example 7:  
            Input: 今天想吃火鍋或牛肉麵  
            Output:
            {
              "user_input": "今天想吃火鍋或牛肉麵",
              "general_tags": ["prefer_hotpot", "prefer_beef", "prefer_noodles", "prefer_soup_dishes", "prefer_hot_items"],
              "specific_tags": [
                { "tag": "hot pot", "polarity": "positive" },
                { "tag": "beef noodle", "polarity": "positive" }
              ],
              "preferred_restaurants": []
            }
            
            Example 8:
            Input: 那間店太貴又太吵，不要去。
            Output:
            {
              "user_input": "那間店太貴又太吵，不要去。",
              "general_tags": ["too_expensive", "too_crowded"],
              "specific_tags": [],
              "preferred_restaurants": []
            }
            
            Example 9:
            Input: 今天好累，不想排隊也不想等太久，想找個可以內用又便宜的中式料理。
            Output:
            {
              "user_input": "今天好累，不想排隊也不想等太久，想找個可以內用又便宜的中式料理。",
              "general_tags": [
                "avoid_long_wait_time",
                "prefer_dine_in",
                "prefer_chinese_cuisine",
                "reasonably_priced"
              ],
              "specific_tags": [
                { "tag": "Chinese food", "polarity": "positive" }
              ],
              "preferred_restaurants": []
            }
            
            Example 10:
            Input: I want something Western or Korean, maybe some BBQ or fried rice, but not too greasy or sweet.
            Output:
            {
              "user_input": "I want something Western or Korean, maybe some BBQ or fried rice, but not too greasy or sweet.",
              "general_tags": [
                "prefer_western_cuisine",
                "prefer_korean_cuisine",
                "prefer_barbecue",
                "prefer_rice",
                "avoid_greasy_dishes",
                "avoid_sweet_dishes"
              ],
              "specific_tags": [
                { "tag": "BBQ", "polarity": "positive" },
                { "tag": "fried rice", "polarity": "positive" }
              ],
              "preferred_restaurants": []
            }
            
            Example 11:
            Input: 我想吃海鮮便當但不要太油，飯多一點最好，不然吃不飽。
            Output:
            {
              "user_input": "我想吃海鮮便當但不要太油，飯多一點最好，不然吃不飽。",
              "general_tags": [
                "prefer_seafood",
                "prefer_bento_meals",
                "avoid_greasy_dishes",
                "prefer_rice",
                "portion_satisfying"
              ],
              "specific_tags": [
                { "tag": "seafood bento", "polarity": "positive" }
              ],
              "preferred_restaurants": []
            }
            
            Example 12:
            Input:我現在想吃晚餐，不要早午餐那種，今天想內用比較舒服，我想吃泰式的或越南河粉，上次那家等太久了，我不想再排了，而且那附近超難停車，我不要。
            Output:
            {
              "user_input": "我現在想吃晚餐，不要早午餐那種，今天想內用比較舒服，我想吃泰式的或越南河粉，上次那家等太久了，我不想再排了，而且那附近超難停車，我不要。",
              "general_tags": [
                "prefer_dinner_items",
                "avoid_brunch_items",
                "prefer_dine_in",
                "prefer_southeast_asian_cuisine",
                "prefer_noodles",
                "avoid_long_wait_time",
                "hard_to_find_parking"
              ],
              "specific_tags": [
                { "tag": "Thai food", "polarity": "positive" },
                { "tag": "Vietnamese noodle", "polarity": "positive" }
              ],
              "preferred_restaurants": []
            }
            
            Example 13:
            Input: Dinner time la, don’t want anything 太 heavy or oily... craving something Thai-style but not too spicy, also must有冷氣！
            Output:
            {
              "user_input": "Dinner time la, don’t want anything 太 heavy or oily... craving something Thai-style but not too spicy, also must有冷氣！",
              "general_tags": [
                "prefer_dinner_items",
                "avoid_greasy_dishes",
                "prefer_fresh_dishes",
                "prefer_southeast_asian_cuisine",
                "avoid_spicy_dishes"
              ],
              "specific_tags": [
                { "tag": "Thai food", "polarity": "positive" }
              ],
              "preferred_restaurants": []
            }
            
            Example 14:
            Input: 我今天心情差，只想吃個簡單便當，不要排隊、不要太貴、也不要再去那種冷冷的地方了。
            Output:
            {
              "user_input": "我今天心情差，只想吃個簡單便當，不要排隊、不要太貴、也不要再去那種冷冷的地方了。",
              "general_tags": [
                "prefer_bento_meals",
                "avoid_long_wait_time",
                "reasonably_priced",
                "avoid_cold_items"
              ],
              "specific_tags": [
                { "tag": "bento", "polarity": "positive" }
              ],
              "preferred_restaurants": []
            }
            
            Example 15:
            Input: 我想買鹹酥雞或滷味當消夜，不要吃太辣，我快累死不想排隊了。
            Output:
            {
              "user_input": "我想買鹹酥雞或滷味當消夜，不要吃太辣，我快累死不想排隊了。",
              "general_tags": [
                "prefer_fried_dishes",
                "prefer_lu_wei",
                "prefer_late_night_items",
                "avoid_spicy_dishes",
                "avoid_long_wait_time",
                "prefer_taiwanese_cuisine"
              ],
              "specific_tags": [
                { "tag": "fried chicken", "polarity": "positive" },
                { "tag": "lu wei", "polarity": "positive" }
              ],
              "preferred_restaurants": []
            }
            
            Example 16:
            Input: I don't wanna wait long today… 台式便當OK，但不想吃甜的，prefer dine-in，而且不要 beef！
            Output:
            {
              "user_input": "I don’t wanna wait long today… 台式便當OK，但不想吃甜的，prefer dine-in，而且不要 beef！",
              "general_tags": [
                "prefer_bento_meals",
                "prefer_dine_in",
                "avoid_sweet_dishes",
                "avoid_beef",
                "avoid_long_wait_time",
                "prefer_taiwanese_cuisine"
              ],
              "specific_tags": [
                { "tag": "Taiwanese bento", "polarity": "positive" }
              ],
              "preferred_restaurants": []
            }
            USER INPUT:
            "$reason"
            """.trimIndent()
    }

}
