package com.example.eventhou.ui.filter

import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.eventhou.R


class CategoryPrefsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val preferenceManager: PreferenceManager = preferenceManager
        preferenceManager.sharedPreferencesName = getString(R.string.preference_filter_categories)
        setPreferencesFromResource(R.xml.filter_preferences, rootKey)

        val preferenceScreen = this.preferenceScreen

        // get categories and genres from resources
        val categoryLabels = resources.getStringArray(R.array.category_labels)
        val categorySlugs = resources.getStringArray(R.array.category_slugs)
        val subCategoryValues = resources.obtainTypedArray(R.array.subcategory_slugs)
        val subCategoryLabels = resources.obtainTypedArray(R.array.subcategory_labels)
        for (i in 0 until subCategoryValues.length()) {
            val subSlugsId: Int = subCategoryValues.getResourceId(i, 0)
            val subLabelsId: Int = subCategoryLabels.getResourceId(i, 0)
            if (subSlugsId > 0) {
                val subcategorySlugs = resources.getStringArray(subSlugsId)
                val subcategoryLabels = resources.getStringArray(subLabelsId)

                val preferenceCategory = PreferenceCategory(preferenceScreen.context)
                preferenceCategory.title = categoryLabels[i]
                preferenceCategory.isIconSpaceReserved = false // Removes icon indent
                preferenceScreen.addPreference(preferenceCategory)

                // generate checkboxes for genres
                for (subCounter in subcategorySlugs.indices) {
                    val checkBoxPref = CheckBoxPreference(this.context)
                    checkBoxPref.key = categorySlugs[i] + "." + subcategorySlugs[subCounter]
                    checkBoxPref.title = subcategoryLabels[subCounter]
                    checkBoxPref.isIconSpaceReserved = false // Removes icon indent
                    preferenceCategory.addPreference(checkBoxPref)
                }
            }
        }
        subCategoryValues.recycle()
        subCategoryLabels.recycle()
    }


    companion object {
        fun newInstance() =
            CategoryPrefsFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}
