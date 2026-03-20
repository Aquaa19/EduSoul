package com.aquaa.edusoul.activities.settings

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.utils.ThemeManager
import com.google.android.material.card.MaterialCardView
import android.util.TypedValue
import android.view.ContextThemeWrapper

class ThemeSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerViewThemes: RecyclerView
    private lateinit var themeAdapter: ThemeAdapter
    private lateinit var themeKeyToSave: String

    override fun onCreate(savedInstanceState: Bundle?) {
        // Get the theme key from the intent, default to admin key for safety if not specified
        themeKeyToSave = intent.getStringExtra("THEME_SAVE_KEY") ?: ThemeManager.KEY_ADMIN_THEME
        // Load and apply the theme based on the specific key before super.onCreate()
        val savedTheme = ThemeManager.loadTheme(this, themeKeyToSave)
        ThemeManager.applyTheme(this, savedTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_selection)

        val toolbar = findViewById<Toolbar>(R.id.toolbarThemeSelection)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Choose Theme"

        // Initialize recyclerViewThemes before using it
        recyclerViewThemes = findViewById(R.id.recyclerViewThemes) // ADDED THIS LINE
        recyclerViewThemes.layoutManager = GridLayoutManager(this, 2)

        themeAdapter = ThemeAdapter(ThemeManager.availableThemes.toMutableList()) { selectedTheme ->
            ThemeManager.saveTheme(this, selectedTheme, themeKeyToSave)
            // Restart the application to apply the theme change. This ensures the correct dashboard
            // applies its theme on the next launch.
            val intent = Intent(this, com.aquaa.edusoul.activities.MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }
        recyclerViewThemes.adapter = themeAdapter
    }

    override fun onResume() {
        super.onResume()
        // Ensure the selected theme is correctly highlighted when returning to this activity
        themeAdapter.setSelectedTheme(ThemeManager.loadTheme(this, themeKeyToSave))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    inner class ThemeAdapter(
        private val themes: MutableList<ThemeManager.AppTheme>,
        private val onItemClick: (ThemeManager.AppTheme) -> Unit
    ) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

        // Load the initial selected theme using the specific key from the outer class
        private var selectedTheme: ThemeManager.AppTheme = ThemeManager.loadTheme(this@ThemeSelectionActivity, themeKeyToSave)

        fun setSelectedTheme(theme: ThemeManager.AppTheme) {
            this.selectedTheme = theme
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_theme_option, parent, false)
            return ThemeViewHolder(view)
        }

        override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
            val themeOption = themes[position]
            holder.textViewThemeName.text = themeOption.themeName

            val typedValue = TypedValue()
            val primaryColor: Int
            val secondaryColor: Int

            if (themeOption.themeResId != 0) {
                // For specific themes (Blue, Purple, Green, etc.), apply their own resource ID
                val themeContext = ContextThemeWrapper(holder.itemView.context, themeOption.themeResId)
                themeContext.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
                primaryColor = typedValue.data
                themeContext.theme.resolveAttribute(com.google.android.material.R.attr.colorSecondary, typedValue, true)
                secondaryColor = typedValue.data
            } else {
                // For "System Default" (AppTheme.NEUTRAL), always use Base.Theme.EduSoul colors
                val baseThemeContext = ContextThemeWrapper(holder.itemView.context, R.style.Base_Theme_EduSoul)
                baseThemeContext.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
                primaryColor = typedValue.data
                baseThemeContext.theme.resolveAttribute(com.google.android.material.R.attr.colorSecondary, typedValue, true)
                secondaryColor = typedValue.data
            }

            val gradientDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(primaryColor)
                setStroke(2, secondaryColor)
            }
            holder.viewThemePreview.background = gradientDrawable

            holder.imageViewSelectedIndicator.visibility =
                if (themeOption == selectedTheme) View.VISIBLE else View.GONE

            holder.itemView.setOnClickListener {
                onItemClick(themeOption)
            }
        }

        override fun getItemCount(): Int = themes.size

        inner class ThemeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cardThemeOption: MaterialCardView = itemView.findViewById(R.id.cardThemeOption)
            val viewThemePreview: View = itemView.findViewById(R.id.viewThemePreview)
            val textViewThemeName: TextView = itemView.findViewById(R.id.textViewThemeName)
            val imageViewSelectedIndicator: ImageView = itemView.findViewById(R.id.imageViewSelectedIndicator)
        }
    }
}