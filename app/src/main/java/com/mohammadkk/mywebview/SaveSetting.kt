package com.mohammadkk.mywebview

import android.annotation.SuppressLint
import android.content.Context

class SaveSetting(private var context: Context) {
    @SuppressLint("CommitPrefEdits")
    fun loadPossession(): Int {
        val preferences = context.getSharedPreferences("mainSetting", Context.MODE_PRIVATE)
        return preferences.getInt("possession", 0)
    }
    fun savePossession(possession: Int) {
        val preferences = context.getSharedPreferences("mainSetting", Context.MODE_PRIVATE)
        val editor = preferences.edit().putInt("possession", possession)
        editor.apply()
    }
    fun desktopModeLoad(): Boolean {
        val preferences = context.getSharedPreferences("mainSetting", Context.MODE_PRIVATE)
        return preferences.getBoolean("desktop", false)
    }
    fun desktopModeSave(desktop: Boolean) {
        val preferences = context.getSharedPreferences("mainSetting", Context.MODE_PRIVATE)
        val editor = preferences.edit().putBoolean("desktop", desktop)
        editor.apply()
    }
    fun inversionColorLoad(): Boolean {
        val preferences = context.getSharedPreferences("mainSetting", Context.MODE_PRIVATE)
        return preferences.getBoolean("inversion", false)
    }
    fun inversionColorSave(desktop: Boolean) {
        val preferences = context.getSharedPreferences("mainSetting", Context.MODE_PRIVATE)
        val editor = preferences.edit().putBoolean("inversion", desktop)
        editor.apply()
    }
}