package edu.sungshin.moneyhero

import android.content.Context
import android.content.SharedPreferences

object DataManager {
    private const val PREFS_NAME = "ToDoListPrefs"
    private const val KEY_SELECTED_DATE = "selectedDate"
    private const val KEY_ITEMS = "items"

    private lateinit var preferences: SharedPreferences

    var selectedDate: String? = null
        set(value) {
            field = value
            saveData() // 데이터 변경 시마다 저장
        }

    var itemList: MutableList<String> = mutableListOf()
        set(value) {
            field = value
            saveData() // 데이터 변경 시마다 저장
        }

    // 초기화
    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadData()
    }

    // 저장
    private fun saveData() {
        val editor = preferences.edit()
        editor.putString(KEY_SELECTED_DATE, selectedDate)
        editor.putStringSet(KEY_ITEMS, itemList.toSet())  // Set으로 저장해서 중복을 방지
        editor.apply()
    }

    // 로드
    private fun loadData() {
        selectedDate = preferences.getString(KEY_SELECTED_DATE, null)
        val storedItems = preferences.getStringSet(KEY_ITEMS, mutableSetOf())
        itemList = storedItems?.toMutableList() ?: mutableListOf()
    }

    // 항목 추가
    fun addItem(item: String) {
        itemList.add(item)
        saveData() // 저장
    }

    // 항목 목록 반환
    fun getItems(): List<String> {
        return itemList
    }
}
