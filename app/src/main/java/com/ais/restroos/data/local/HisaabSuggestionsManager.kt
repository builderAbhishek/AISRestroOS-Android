package com.ais.restroos.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object HisaabSuggestionsManager {

    private const val PREFS_NAME = "hisaab_suggestions_prefs"
    private const val KEY_RAW_MATERIALS = "learned_raw_materials_"
    private const val KEY_EXPENSES = "learned_expenses_"
    private const val KEY_UNITS = "learned_units_"
    private const val KEY_VENDORS = "learned_vendors_"

    // Default Raw Materials (Common restaurant culinary ingredients)
    val DEFAULT_RAW_MATERIALS = listOf(
        "Potato (Aloo)",
        "Onion (Pyaz)",
        "Tomato (Tamatar)",
        "Milk",
        "Paneer",
        "Butter",
        "Cheese",
        "Cooking Oil",
        "Mustard Oil",
        "Wheat Flour (Atta)",
        "Maida (Refined Flour)",
        "Basmati Rice",
        "Chicken",
        "Mutton",
        "Fish",
        "Eggs",
        "Ginger & Garlic",
        "Green Chilli",
        "Fresh Coriander",
        "Garam Masala",
        "Red Chilli Powder",
        "Turmeric Powder",
        "Salt",
        "Sugar",
        "Tea Leaves",
        "Coffee Powder",
        "Bread & Buns",
        "Capsicum",
        "Cabbage",
        "Cauliflower",
        "Green Peas",
        "Food Containers / Packaging",
        "Tissue / Paper Napkins",
        "Drinking Water Bottles"
    )

    // Default General Expenses (Common restaurant operational overheads)
    val DEFAULT_EXPENSES = listOf(
        "Commercial Gas Cylinder (LPG)",
        "Electricity Bill",
        "Staff Daily Wages",
        "Staff Salary Advance",
        "Shop / Property Rent",
        "Dishwashing & Cleaning Supplies",
        "Plumbing & Tap Repair",
        "Kitchen Equipment Maintenance",
        "RO Drinking Water Refill",
        "Internet / Wi-Fi Bill",
        "Transportation / Auto Fare",
        "Generator Diesel",
        "Garbage Collection Fee",
        "POS Thermal Paper Rolls"
    )

    // Combined restaurant daily expenses & inwards (Vegetables, Groceries, Dairy, Gas, Petrol, Water, Staff, etc.)
    val DEFAULT_COMBINED_EXPENSES = listOf(
        "Sabji Mandi (Daily Vegetables)",
        "Kirana Store (Groceries Bill)",
        "Dudh / Dairy (Milk, Paneer)",
        "Commercial Gas Cylinder (LPG)",
        "Petrol / Vehicle Fuel",
        "Paani (Drinking Water Jar)",
        "Staff Daily Wages / Kharcha",
        "Chicken / Meat Supplier",
        "Cooking Oil & Ghee",
        "Potato (Aloo)",
        "Onion (Pyaz)",
        "Tomato (Tamatar)",
        "Paneer",
        "Milk",
        "Basmati Rice",
        "Wheat Flour (Atta)",
        "Maida (Refined Flour)",
        "Spices & Masala",
        "Food Containers & Packaging",
        "Tissue / Paper Napkins",
        "Electricity Bill",
        "Shop Rent",
        "Dishwashing & Cleaning Supplies"
    )

    // Default Units of measurement
    val DEFAULT_UNITS = listOf(
        "kg",
        "gm",
        "liter",
        "ml",
        "packet",
        "piece",
        "box",
        "cylinder",
        "jar",
        "bill",
        "item",
        "day",
        "month",
        "tin",
        "dozen"
    )

    // Default Vendors / Sources
    val DEFAULT_VENDORS = listOf(
        "Local Mandi (Vegetable Market)",
        "Kirana / Wholesale Grocery Store",
        "Dairy Farm / Milk Supplier",
        "Commercial Gas Agency",
        "Water Jar Supplier (Paani)",
        "Petrol Pump / Fuel",
        "Meat / Poultry Vendor",
        "Staff / Karigar",
        "Landlord / Rent",
        "Petty Cash / Counter"
    )

    private val gson = Gson()
    private val stringSetType = object : TypeToken<Set<String>>() {}.type

    /**
     * Retrieve suggestions for a given field type.
     * Combines learned entries + local DB history + default items.
     * Zero network requests: 100% offline & lightning fast.
     */
    fun getSuggestions(
        context: Context,
        restaurantId: String,
        type: String // "ALL_EXPENSES", "RAW_MATERIAL", "EXPENSE", "UNIT", "VENDOR"
    ): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val result = LinkedHashSet<String>()

        if (type == "ALL_EXPENSES") {
            // Combine both learned raw materials and learned expenses
            val rawLearned = getStoredSet(prefs, KEY_RAW_MATERIALS + restaurantId)
            val expLearned = getStoredSet(prefs, KEY_EXPENSES + restaurantId)
            result.addAll(expLearned)
            result.addAll(rawLearned)

            // DB distinct item names across both types
            try {
                val dbHelper = LocalDatabaseHelper.getInstance(context)
                val distinctFromDb = dbHelper.getDistinctPurchaseField(restaurantId, "item_name", null)
                result.addAll(distinctFromDb)
            } catch (_: Exception) {}

            result.addAll(DEFAULT_COMBINED_EXPENSES)
            result.addAll(DEFAULT_RAW_MATERIALS)
            result.addAll(DEFAULT_EXPENSES)
            return result.toList()
        }

        val key = when (type) {
            "RAW_MATERIAL" -> KEY_RAW_MATERIALS + restaurantId
            "EXPENSE" -> KEY_EXPENSES + restaurantId
            "UNIT" -> KEY_UNITS + restaurantId
            "VENDOR" -> KEY_VENDORS + restaurantId
            else -> return emptyList()
        }

        val learnedSet = getStoredSet(prefs, key)

        val baseList = when (type) {
            "RAW_MATERIAL" -> DEFAULT_RAW_MATERIALS
            "EXPENSE" -> DEFAULT_EXPENSES
            "UNIT" -> DEFAULT_UNITS
            "VENDOR" -> DEFAULT_VENDORS
            else -> emptyList()
        }

        // Learned entries have highest priority
        result.addAll(learnedSet)

        // Query distinct values from local SQLite
        try {
            val dbHelper = LocalDatabaseHelper.getInstance(context)
            val distinctFromDb = when (type) {
                "RAW_MATERIAL" -> dbHelper.getDistinctPurchaseField(restaurantId, "item_name", "PURCHASE")
                "EXPENSE" -> dbHelper.getDistinctPurchaseField(restaurantId, "item_name", "EXPENSE")
                "UNIT" -> dbHelper.getDistinctPurchaseField(restaurantId, "unit", null)
                "VENDOR" -> dbHelper.getDistinctPurchaseField(restaurantId, "vendor_name", null)
                else -> emptyList()
            }
            result.addAll(distinctFromDb)
        } catch (_: Exception) {
            // Ignore DB read errors
        }

        // Base defaults
        result.addAll(baseList)

        return result.toList()
    }

    /**
     * Record a newly entered item, unit, and vendor to local storage so they are
     * instantly available for autocomplete in future entries.
     */
    fun recordEntry(
        context: Context,
        restaurantId: String,
        expenseType: String,
        itemName: String,
        unit: String,
        vendorName: String
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // 1. Record item name
        val itemKey = if (expenseType == "PURCHASE") KEY_RAW_MATERIALS + restaurantId else KEY_EXPENSES + restaurantId
        val existingItems = getStoredSet(prefs, itemKey).toMutableSet()
        if (itemName.isNotBlank()) {
            existingItems.add(itemName.trim())
            editor.putString(itemKey, gson.toJson(existingItems))
        }

        // 2. Record unit
        if (unit.isNotBlank()) {
            val unitKey = KEY_UNITS + restaurantId
            val existingUnits = getStoredSet(prefs, unitKey).toMutableSet()
            existingUnits.add(unit.trim().lowercase())
            editor.putString(unitKey, gson.toJson(existingUnits))
        }

        // 3. Record vendor
        if (vendorName.isNotBlank() && !vendorName.equals("Local Market", ignoreCase = true)) {
            val vendorKey = KEY_VENDORS + restaurantId
            val existingVendors = getStoredSet(prefs, vendorKey).toMutableSet()
            existingVendors.add(vendorName.trim())
            editor.putString(vendorKey, gson.toJson(existingVendors))
        }

        editor.apply()
    }

    private fun getStoredSet(prefs: android.content.SharedPreferences, key: String): Set<String> {
        val json = prefs.getString(key, null) ?: return emptySet()
        return try {
            gson.fromJson(json, stringSetType)
        } catch (_: Exception) {
            emptySet()
        }
    }
}
