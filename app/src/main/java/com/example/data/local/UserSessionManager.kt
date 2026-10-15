package com.example.data.local

import android.content.Context
import com.example.BuildConfig
import com.example.data.model.UserTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserSessionManager(context: Context, private val now: () -> Long = System::currentTimeMillis,
    private val allowTierPreview: Boolean = BuildConfig.DEBUG) {
    private val prefs = context.getSharedPreferences("hvac_user_prefs", Context.MODE_PRIVATE)
    private val _currentTier = MutableStateFlow(loadTier())
    val currentTier = _currentTier.asStateFlow()
    private val _aiQueriesUsed = MutableStateFlow(prefs.getInt("ai_queries_used", 0).coerceAtLeast(0))
    val aiQueriesUsed = _aiQueriesUsed.asStateFlow()
    private val _weeklyVideoScansUsed = MutableStateFlow(prefs.getInt("weekly_video_scans_used", 0).coerceAtLeast(0))
    val weeklyVideoScansUsed = _weeklyVideoScansUsed.asStateFlow()
    private val _onboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_done", false))
    val onboardingCompleted = _onboardingCompleted.asStateFlow()
    private val _companyName = MutableStateFlow(prefs.getString("company_name", "HVAC TechPro Service") ?: "HVAC TechPro Service")
    val companyName = _companyName.asStateFlow()
    private val _companyContact = MutableStateFlow(prefs.getString("company_contact", "") ?: "")
    val companyContact = _companyContact.asStateFlow()
    private val _reportTerms = MutableStateFlow(prefs.getString("report_terms", "Work and recommendations documented above. Warranty is subject to company terms.") ?: "")
    val reportTerms = _reportTerms.asStateFlow()
    private val _companyLogoPath = MutableStateFlow(prefs.getString("company_logo_path", "") ?: "")
    val companyLogoPath = _companyLogoPath.asStateFlow()
    init { refreshUsageWindow() }
    private fun loadTier(): UserTier {
        val saved = runCatching { UserTier.valueOf(prefs.getString("user_tier", "NEW_USER") ?: "NEW_USER") }.getOrDefault(UserTier.NEW_USER)
        return if (!allowTierPreview && saved in listOf(UserTier.PLUS_USER, UserTier.PRO_USER)) UserTier.REGULAR_USER else saved
    }
    fun setTier(tier: UserTier) {
        if (!allowTierPreview && tier in listOf(UserTier.PLUS_USER, UserTier.PRO_USER)) return
        _currentTier.value = tier
        prefs.edit().putString("user_tier", tier.name).apply()
    }
    @Synchronized fun refreshUsageWindow() {
        val current = now(); val start = prefs.getLong("video_window_started_at", -1L)
        if (start < 0 || current - start >= WEEK_MS) {
            _weeklyVideoScansUsed.value = 0
            prefs.edit().putLong("video_window_started_at", current).putInt("weekly_video_scans_used", 0).apply()
        }
    }
    @Synchronized fun tryConsumeVideoScan(): Boolean {
        refreshUsageWindow()
        if (_currentTier.value == UserTier.PRO_USER) return true
        if (_weeklyVideoScansUsed.value >= _currentTier.value.weeklyVideoScansAllowed) return false
        _weeklyVideoScansUsed.value += 1
        prefs.edit().putInt("weekly_video_scans_used", _weeklyVideoScansUsed.value).apply()
        return true
    }
    @Synchronized fun incrementAiQuery() {
        _aiQueriesUsed.value = (_aiQueriesUsed.value.toLong() + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        prefs.edit().putInt("ai_queries_used", _aiQueriesUsed.value).apply()
    }
    fun setOnboardingCompleted(done: Boolean) {
        _onboardingCompleted.value = done
        prefs.edit().putBoolean("onboarding_done", done).apply()
        if (done && _currentTier.value == UserTier.NEW_USER) setTier(UserTier.REGULAR_USER)
    }
    fun setCompanyName(value: String) {
        val safe = value.trim().take(80).ifBlank { "HVAC TechPro Service" }
        _companyName.value = safe; prefs.edit().putString("company_name", safe).apply()
    }
    fun setBusinessIdentity(name: String, contact: String, terms: String, logoPath: String = _companyLogoPath.value) {
        val safeName = name.trim().take(80).ifBlank { "HVAC TechPro Service" }
        val safeContact = contact.trim().take(240)
        val safeTerms = terms.trim().take(2000)
        _companyName.value=safeName; _companyContact.value=safeContact; _reportTerms.value=safeTerms; _companyLogoPath.value=logoPath
        prefs.edit().putString("company_name",safeName).putString("company_contact",safeContact).putString("report_terms",safeTerms).putString("company_logo_path",logoPath).apply()
    }
    companion object { const val WEEK_MS = 7L * 24 * 60 * 60 * 1000 }
}
