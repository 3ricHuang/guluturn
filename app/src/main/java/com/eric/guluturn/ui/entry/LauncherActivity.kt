package com.eric.guluturn.ui.entry

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.eric.guluturn.common.storage.ApiKeyStorage
import com.eric.guluturn.ui.login.LoginActivity
import com.eric.guluturn.ui.profile.ProfileSelectorActivity
import com.eric.guluturn.ui.roulette.RouletteActivity

/**
 * LauncherActivity – App 的進入點，用來判斷登入狀態與導頁。
 *
 * 行為邏輯：
 * - 若尚未輸入 API key，跳轉至 LoginActivity
 * - 若尚未選擇 Profile，跳轉至 ProfileSelectorActivity
 * - 否則直接進入 RouletteActivity（主推薦流程）
 */
class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = applicationContext
        val apiKey = ApiKeyStorage.getSavedApiKey(context)
        val profileUuid = ApiKeyStorage.getSelectedProfileUuid(context)

        val nextIntent = when {
            apiKey.isNullOrBlank() -> Intent(this, LoginActivity::class.java)
            profileUuid.isNullOrBlank() -> Intent(this, ProfileSelectorActivity::class.java)
            else -> Intent(this, RouletteActivity::class.java)
        }

        startActivity(nextIntent)
        finish()
    }
}
