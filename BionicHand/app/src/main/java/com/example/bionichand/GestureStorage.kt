package com.example.bionichand

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private const val FINGER_COUNT = 4

data class CustomGesture(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val angles: List<Int>,
    val isStandard: Boolean = false
)

class GestureStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("BionicPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val defaultGestures = listOf(
        CustomGesture(id = "std_fist", name = "Кулак", angles = listOf(180, 180, 180, 180), isStandard = true),
        CustomGesture(id = "std_index", name = "Вказівний", angles = listOf(180, 0, 180, 180), isStandard = true)
    )

    fun getLibrary(): List<CustomGesture> {
        val json = prefs.getString("LIBRARY_LIST", null) ?: return defaultGestures
        val type = object : TypeToken<List<CustomGesture>>() {}.type
        return runCatching { gson.fromJson<List<CustomGesture>>(json, type) }
            .getOrDefault(defaultGestures)
            .map { it.copy(angles = it.angles.toFourFingerAngles()) }
            .ifEmpty { defaultGestures }
    }

    fun saveToLibrary(gesture: CustomGesture) {
        val currentList = getLibrary().toMutableList()
        currentList.add(gesture.copy(angles = gesture.angles.toFourFingerAngles()))
        prefs.edit().putString("LIBRARY_LIST", gson.toJson(currentList)).apply()
    }

    fun deleteFromLibrary(gestureId: String) {
        val currentList = getLibrary().filter { it.id != gestureId || it.isStandard }
        prefs.edit().putString("LIBRARY_LIST", gson.toJson(currentList)).apply()
    }

    fun getActiveSlots(): List<CustomGesture?> {
        val json = prefs.getString("ACTIVE_SLOTS", null) ?: return listOf(defaultGestures[0], null, null)
        val type = object : TypeToken<List<CustomGesture?>>() {}.type
        val slots = runCatching { gson.fromJson<List<CustomGesture?>>(json, type) }
            .getOrDefault(listOf(defaultGestures[0], null, null))
            .map { it?.copy(angles = it.angles.toFourFingerAngles()) }
            .toMutableList()

        while (slots.size < 3) slots.add(null)
        return slots.take(3)
    }

    fun saveToActiveSlot(slotIndex: Int, gesture: CustomGesture) {
        val currentSlots = getActiveSlots().toMutableList()
        if (slotIndex in 0..2) {
            currentSlots[slotIndex] = gesture.copy(angles = gesture.angles.toFourFingerAngles())
            prefs.edit().putString("ACTIVE_SLOTS", gson.toJson(currentSlots)).apply()
        }
    }

    fun clearActiveSlot(slotIndex: Int) {
        val currentSlots = getActiveSlots().toMutableList()
        if (slotIndex in 0..2) {
            currentSlots[slotIndex] = null
            prefs.edit().putString("ACTIVE_SLOTS", gson.toJson(currentSlots)).apply()
        }
    }

    fun updateInLibrary(updatedGesture: CustomGesture) {
        val currentList = getLibrary().toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedGesture.id }
        if (index != -1 && !currentList[index].isStandard) {
            currentList[index] = updatedGesture.copy(angles = updatedGesture.angles.toFourFingerAngles())
            prefs.edit().putString("LIBRARY_LIST", gson.toJson(currentList)).apply()
        }
    }
}

private fun List<Int>.toFourFingerAngles(): List<Int> {
    return when {
        size == FINGER_COUNT -> this.map { it.coerceIn(0, 180) }
        size >= 5 -> listOf(this[0], this[1], this[2], maxOf(this[3], this[4])).map { it.coerceIn(0, 180) }
        else -> (this + List(FINGER_COUNT - size) { 0 }).map { it.coerceIn(0, 180) }
    }
}
