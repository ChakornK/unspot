package com.chakornk.unspot.ui.auth

import android.content.Context
import android.content.SharedPreferences

object AuthStorage {
	private const val PREFS_NAME = "auth"
	private const val KEY_IS_LOGGED_IN = "isLoggedIn"

	private var prefs: SharedPreferences? = null

	fun init(context: Context) {
		prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
	}

	var isLoggedIn: Boolean
		get() = prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
		set(value) {
			prefs?.edit()?.putBoolean(KEY_IS_LOGGED_IN, value)?.apply()
		}
}
