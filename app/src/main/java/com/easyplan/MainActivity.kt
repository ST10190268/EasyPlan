package com.easyplan

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.easyplan.data.Task
import com.easyplan.util.TaskManager
import com.easyplan.util.TaskStatistics
import com.easyplan.util.ThemeUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * MainActivity - Main application activity with navigation and task management
 *
 * Features:
 * - Bottom navigation between Tasks, Statistics, Calendar, Settings, Profile
 * - Task creation and management
 * - Statistics dashboard
 * - Search and filter functionality
 * - Theme management
 *
 * @author EasyPlan Team
 * @version 3.0 - Added statistics and search/filter features
 */
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private var selectedDate = Date()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Search and filter state
    private var searchQuery: String = ""
    private var selectedPriority: String? = null
    private var selectedCategory: String? = null
    private var selectedStatus: String = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.applySavedTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize TaskManager with context for REST API integration
        TaskManager.initialize(applicationContext)

        // Load tasks from Firestore when authenticated; fallback to samples
        TaskManager.loadTasksForUser {
            updateTasksForSelectedDate()
            updateTasksTabUI()
            // Also sync to REST API after loading from Firestore
            TaskManager.syncToRestApi()
        }
        TaskManager.initializeSampleTasks()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        val viewTasks = findViewById<View>(R.id.view_tasks)
        val viewStatistics = findViewById<View>(R.id.view_statistics)
        val viewCalendar = findViewById<View>(R.id.view_calendar)
        val viewSettings = findViewById<View>(R.id.view_settings)
        val viewProfile = findViewById<View>(R.id.view_profile)

        fun show(viewToShow: View) {
            Log.d(TAG, "Switching to view: ${viewToShow.id}")
            viewTasks.visibility = if (viewToShow === viewTasks) View.VISIBLE else View.GONE
            viewStatistics.visibility = if (viewToShow === viewStatistics) View.VISIBLE else View.GONE
            viewCalendar.visibility = if (viewToShow === viewCalendar) View.VISIBLE else View.GONE
            viewSettings.visibility = if (viewToShow === viewSettings) View.VISIBLE else View.GONE
            viewProfile.visibility = if (viewToShow === viewProfile) View.VISIBLE else View.GONE
        }

        // Default
        show(viewTasks)
        setupTasksListeners()

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> {
                    show(viewTasks)
                    setupTasksListeners()
                }
                R.id.nav_statistics -> {
                    show(viewStatistics)
                    setupStatistics()
                }
                R.id.nav_calendar -> {
                    show(viewCalendar)
                    setupCalendarListeners()
                }
                R.id.nav_settings -> {
                    show(viewSettings)
                    setupSettingsListeners()
                }
                R.id.nav_profile -> {
                    show(viewProfile)
                    setupProfile()
                }
            }
            true
        }
    }

    private fun setupTasksListeners() {
        // Quick Action Cards with proper IDs
        findViewById<View>(R.id.cardAddTask)?.setOnClickListener {
            showAddTaskBottomSheet()
        }

        findViewById<View>(R.id.cardCreateReminder)?.setOnClickListener {
            showCreateReminderBottomSheet()
        }

        findViewById<View>(R.id.cardPlanWeek)?.setOnClickListener {
            // Switch to calendar tab
            findViewById<BottomNavigationView>(R.id.bottom_nav)?.selectedItemId = R.id.nav_calendar
            Toast.makeText(this, "Opening calendar to plan your week", Toast.LENGTH_SHORT).show()
        }

        // Refresh Tasks tab sections
        updateTasksTabUI()
    }

    private fun setupCalendarListeners() {
        // Calendar date selection
        findViewById<CalendarView>(R.id.calendarView)?.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
            updateTasksForSelectedDate()
        }

        // Add task button
        findViewById<MaterialButton>(R.id.btnAddTask)?.setOnClickListener {
            showAddTaskBottomSheet()
        }

        // Load today's tasks initially
        updateTasksForSelectedDate()
    }

    private fun updateTasksForSelectedDate() {
        val tasksContainer = findViewById<LinearLayout>(R.id.todayTasksContainer)
        val emptyMessage = findViewById<TextView>(R.id.emptyTasksMessage)

        tasksContainer?.removeAllViews()

        val tasksForDate = TaskManager.getTasksForDate(selectedDate)

        if (tasksForDate.isEmpty()) {
            emptyMessage?.visibility = View.VISIBLE
        } else {
            emptyMessage?.visibility = View.GONE
            tasksForDate.forEach { task ->
                addTaskViewToContainer(task, tasksContainer)
            }
        }
    }

    private fun addTaskViewToContainer(task: Task, container: LinearLayout?) {
        container?.let {
            val taskView = layoutInflater.inflate(R.layout.item_task, container, false)

            // Bind core fields
            taskView.findViewById<TextView>(R.id.tvTaskTitle)?.text = task.title
            taskView.findViewById<TextView>(R.id.tvTaskDescription)?.text = task.description
            taskView.findViewById<TextView>(R.id.taskTime)?.text = task.dueTime?.let { "Due: $it" } ?: "No time set"

            // Category icon
            val categoryIcon = taskView.findViewById<TextView>(R.id.tvCategoryIcon)
            categoryIcon?.text = com.easyplan.data.Task.Category.fromString(task.category).icon

            // Priority indicator color
            val priorityIndicator = taskView.findViewById<View>(R.id.priorityIndicator)
            val priorityColor = when (task.priority) {
                "high" -> Color.parseColor("#F44336")   // Red
                "low" -> Color.parseColor("#4CAF50")    // Green
                else -> Color.parseColor("#FF9800")      // Orange (medium)
            }
            priorityIndicator?.setBackgroundColor(priorityColor)

            // Completion checkbox
            val checkbox = taskView.findViewById<MaterialCheckBox>(R.id.taskCheckbox)
            checkbox?.isChecked = task.isCompleted
            checkbox?.setOnCheckedChangeListener { _, _ ->
                TaskManager.toggleTaskCompletion(task.id)
                updateTasksForSelectedDate()
                updateTasksTabUI()
            }

            container.addView(taskView)
        }
    }
    private fun updateTasksTabUI() {
        val todayContainer = findViewById<LinearLayout>(R.id.tasksTabTodayContainer)
        val emptyToday = findViewById<TextView>(R.id.tasksTabEmptyToday)
        val priorityContainer = findViewById<LinearLayout>(R.id.tasksTabPriorityContainer)
        val emptyPriority = findViewById<TextView>(R.id.tasksTabEmptyPriority)

        todayContainer?.removeAllViews()
        priorityContainer?.removeAllViews()

        // Today's tasks
        val todayTasks = TaskManager.getTodayTasks()
        if (todayTasks.isEmpty()) {
            emptyToday?.visibility = View.VISIBLE
        } else {
            emptyToday?.visibility = View.GONE
            todayTasks.forEach { task -> addTaskViewToContainer(task, todayContainer) }
        }

        // High priority, not completed
        val highPriorityTasks = TaskManager.getAllTasks().filter { it.priority == "high" && !it.isCompleted }
        if (highPriorityTasks.isEmpty()) {
            emptyPriority?.visibility = View.VISIBLE
        } else {
            emptyPriority?.visibility = View.GONE
            highPriorityTasks.forEach { task -> addTaskViewToContainer(task, priorityContainer) }
        }
    }


    private fun showAddTaskBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_add_task, null)
        bottomSheetDialog.setContentView(view)

        val titleInput = view.findViewById<TextInputEditText>(R.id.taskTitle)
        val descriptionInput = view.findViewById<TextInputEditText>(R.id.taskDescription)
        val dueDateInput = view.findViewById<TextInputEditText>(R.id.taskDueDate)
        val dueTimeInput = view.findViewById<TextInputEditText>(R.id.taskDueTime)
        val saveButton = view.findViewById<MaterialButton>(R.id.btnSaveTask)
        val cancelButton = view.findViewById<MaterialButton>(R.id.btnCancel)

        // Priority selection chips
        val chipHigh = view.findViewById<Chip>(R.id.chipPriorityHigh)
        val chipMedium = view.findViewById<Chip>(R.id.chipPriorityMedium)
        val chipLow = view.findViewById<Chip>(R.id.chipPriorityLow)

        // Category selection chips
        val chipCatWork = view.findViewById<Chip>(R.id.chipCatWork)
        val chipCatPersonal = view.findViewById<Chip>(R.id.chipCatPersonal)
        val chipCatStudy = view.findViewById<Chip>(R.id.chipCatStudy)
        val chipCatHealth = view.findViewById<Chip>(R.id.chipCatHealth)
        val chipCatShopping = view.findViewById<Chip>(R.id.chipCatShopping)
        val chipCatOther = view.findViewById<Chip>(R.id.chipCatOther)

        // Set default date to selected date
        dueDateInput?.setText(dateFormat.format(selectedDate))
        var taskDueDate = selectedDate

        // Date picker
        dueDateInput?.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = taskDueDate

            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, dayOfMonth)
                taskDueDate = newCalendar.time
                dueDateInput.setText(dateFormat.format(taskDueDate))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Time picker
        var taskDueTime: String? = null
        dueTimeInput?.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                taskDueTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                dueTimeInput.setText(taskDueTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        // Save task
        saveButton?.setOnClickListener {
            val title = titleInput?.text?.toString()?.trim()
            if (title.isNullOrEmpty()) {
                Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val description = descriptionInput?.text?.toString()?.trim() ?: ""

            // Determine selected priority (default to medium)
            val priority = when {
                chipHigh?.isChecked == true -> "high"
                chipLow?.isChecked == true -> "low"
                else -> "medium"
            }

            // Determine selected category (default to personal)
            val category = when {
                chipCatWork?.isChecked == true -> "work"
                chipCatStudy?.isChecked == true -> "study"
                chipCatHealth?.isChecked == true -> "health"
                chipCatShopping?.isChecked == true -> "shopping"
                chipCatOther?.isChecked == true -> "other"
                else -> "personal"
            }

            val newTask = Task(
                title = title,
                description = description,
                dueDate = taskDueDate,
                dueTime = taskDueTime,
                priority = priority,
                category = category
            )

            TaskManager.addTask(newTask)
            updateTasksForSelectedDate()
            updateTasksTabUI()
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Task added successfully!", Toast.LENGTH_SHORT).show()
        }

        // Cancel
        cancelButton?.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun setupSettingsListeners() {
        // Update chip selection states based on current theme
        val currentTheme = ThemeUtils.getSavedTheme(this)
        findViewById<Chip>(R.id.chipThemeSystem)?.isChecked = currentTheme == ThemeUtils.ThemeMode.SYSTEM
        findViewById<Chip>(R.id.chipThemeLight)?.isChecked = currentTheme == ThemeUtils.ThemeMode.LIGHT
        findViewById<Chip>(R.id.chipThemeDark)?.isChecked = currentTheme == ThemeUtils.ThemeMode.DARK

        // Language chips
        findViewById<Chip>(R.id.chipEnglish)?.setOnClickListener {
            Toast.makeText(this, "English selected", Toast.LENGTH_SHORT).show()
        }
        findViewById<Chip>(R.id.chipAfrikaans)?.setOnClickListener {
            Toast.makeText(this, "Afrikaans coming soon", Toast.LENGTH_SHORT).show()
        }
        findViewById<Chip>(R.id.chipZulu)?.setOnClickListener {
            Toast.makeText(this, "isiZulu coming soon", Toast.LENGTH_SHORT).show()
        }

        // Theme chips
        findViewById<Chip>(R.id.chipThemeSystem)?.setOnClickListener {
            ThemeUtils.saveTheme(this, ThemeUtils.ThemeMode.SYSTEM)
            Toast.makeText(this, "Using system theme", Toast.LENGTH_SHORT).show()
            recreate() // Restart activity to apply theme
        }
        findViewById<Chip>(R.id.chipThemeLight)?.setOnClickListener {
            ThemeUtils.saveTheme(this, ThemeUtils.ThemeMode.LIGHT)
            Toast.makeText(this, "Light theme applied", Toast.LENGTH_SHORT).show()
            recreate() // Restart activity to apply theme
        }
        findViewById<Chip>(R.id.chipThemeDark)?.setOnClickListener {
            ThemeUtils.saveTheme(this, ThemeUtils.ThemeMode.DARK)
            Toast.makeText(this, "Dark theme applied", Toast.LENGTH_SHORT).show()
            recreate() // Restart activity to apply theme
        }

        // JSONBin status text
        val tvJsonBinStatus = findViewById<TextView>(R.id.tvJsonBinStatus)
        val binId = TaskManager.getJsonBinId()
        tvJsonBinStatus?.text = if (binId.isNullOrEmpty()) "Bin: Not created" else "Bin ID: $binId"

        // Export to JSONBin
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExportJsonBin)?.setOnClickListener {
            Toast.makeText(this, "Exporting to JSONBin...", Toast.LENGTH_SHORT).show()
            TaskManager.syncToRestApi { success ->
                runOnUiThread {
                    val idNow = TaskManager.getJsonBinId()
                    tvJsonBinStatus?.text = if (idNow.isNullOrEmpty()) "Bin: Not created" else "Bin ID: $idNow"
                    Toast.makeText(this, if (success) "Export complete" else "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Import from JSONBin
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnImportJsonBin)?.setOnClickListener {
            Toast.makeText(this, "Importing from JSONBin...", Toast.LENGTH_SHORT).show()
            TaskManager.loadFromRestApi {
                runOnUiThread {
                    updateTasksForSelectedDate()
                    updateTasksTabUI()
                    setupStatistics()
                    Toast.makeText(this, "Import complete", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCreateReminderBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_create_reminder, null)
        bottomSheetDialog.setContentView(view)

        val titleInput = view.findViewById<TextInputEditText>(R.id.reminderTitle)
        val messageInput = view.findViewById<TextInputEditText>(R.id.reminderMessage)
        val dateInput = view.findViewById<TextInputEditText>(R.id.reminderDate)
        val timeInput = view.findViewById<TextInputEditText>(R.id.reminderTime)
        val saveButton = view.findViewById<MaterialButton>(R.id.btnSaveReminder)
        val cancelButton = view.findViewById<MaterialButton>(R.id.btnCancel)

        // Set default date to today
        var reminderDate = Date()
        dateInput?.setText(dateFormat.format(reminderDate))

        // Date picker
        dateInput?.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = reminderDate

            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, dayOfMonth)
                reminderDate = newCalendar.time
                dateInput.setText(dateFormat.format(reminderDate))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Time picker
        var reminderTime: String? = null
        timeInput?.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                reminderTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                timeInput.setText(reminderTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        // Save reminder
        saveButton?.setOnClickListener {
            val title = titleInput?.text?.toString()?.trim()
            if (title.isNullOrEmpty()) {
                Toast.makeText(this, "Please enter a reminder title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val message = messageInput?.text?.toString()?.trim() ?: ""

            // Create a task with reminder flag (we can extend Task class later for reminders)
            val reminderTask = Task(
                title = "🔔 $title",
                description = "Reminder: $message",
                dueDate = reminderDate,
                dueTime = reminderTime
            )

            TaskManager.addTask(reminderTask)
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Reminder set successfully!", Toast.LENGTH_SHORT).show()
        }

        // Cancel
        cancelButton?.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun showFAQBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_faq, null)
        bottomSheetDialog.setContentView(view)

        val closeButton = view.findViewById<MaterialButton>(R.id.btnCloseFAQ)
        closeButton?.setOnClickListener { bottomSheetDialog.dismiss() }

        bottomSheetDialog.show()
    }

    private fun setupProfile() {
        val user = com.google.firebase.ktx.Firebase.auth.currentUser
        findViewById<android.widget.TextView>(R.id.txtName)?.setText(user?.displayName ?: "")
        findViewById<android.widget.TextView>(R.id.txtEmail)?.setText(user?.email ?: "")
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSignOut)?.setOnClickListener {
            com.google.firebase.ktx.Firebase.auth.signOut()
            android.widget.Toast.makeText(this, "Signed out", android.widget.Toast.LENGTH_SHORT).show()
            startActivity(android.content.Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    /**
     * Setup statistics dashboard with comprehensive task analytics
     */
    private fun setupStatistics() {
        Log.d(TAG, "Setting up statistics dashboard")

        val allTasks = TaskManager.getAllTasks()
        val stats = TaskStatistics.calculateStatistics(allTasks)

        // Overall statistics
        findViewById<TextView>(R.id.tvTotalTasks)?.text = stats.totalTasks.toString()
        findViewById<TextView>(R.id.tvCompletedTasks)?.text = stats.completedTasks.toString()
        findViewById<TextView>(R.id.tvPendingTasks)?.text = stats.pendingTasks.toString()

        // Completion rate
        findViewById<TextView>(R.id.tvCompletionRate)?.text = "${stats.completionRate.toInt()}%"
        findViewById<LinearProgressIndicator>(R.id.progressCompletionRate)?.progress = stats.completionRate.toInt()

        // Productivity score
        findViewById<TextView>(R.id.tvProductivityScore)?.text = stats.productivityScore.toString()
        findViewById<CircularProgressIndicator>(R.id.progressProductivity)?.progress = stats.productivityScore
        findViewById<TextView>(R.id.tvProductivityLevel)?.text = when {
            stats.productivityScore >= 80 -> "🔥 Excellent!"
            stats.productivityScore >= 60 -> "💪 Great Job!"
            stats.productivityScore >= 40 -> "👍 Keep Going!"
            else -> "📈 Getting Started"
        }

        // Priority distribution
        val totalForPriority = stats.highPriorityTasks + stats.mediumPriorityTasks + stats.lowPriorityTasks
        if (totalForPriority > 0) {
            val highPercent = (stats.highPriorityTasks * 100f / totalForPriority).toInt()
            val mediumPercent = (stats.mediumPriorityTasks * 100f / totalForPriority).toInt()
            val lowPercent = (stats.lowPriorityTasks * 100f / totalForPriority).toInt()

            findViewById<TextView>(R.id.tvHighPriority)?.text = "${stats.highPriorityTasks} High Priority"
            findViewById<LinearProgressIndicator>(R.id.progressHighPriority)?.apply {
                progress = highPercent
                setIndicatorColor(Color.parseColor("#F44336"))
            }

            findViewById<TextView>(R.id.tvMediumPriority)?.text = "${stats.mediumPriorityTasks} Medium Priority"
            findViewById<LinearProgressIndicator>(R.id.progressMediumPriority)?.apply {
                progress = mediumPercent
                setIndicatorColor(Color.parseColor("#FF9800"))
            }

            findViewById<TextView>(R.id.tvLowPriority)?.text = "${stats.lowPriorityTasks} Low Priority"
            findViewById<LinearProgressIndicator>(R.id.progressLowPriority)?.apply {
                progress = lowPercent
                setIndicatorColor(Color.parseColor("#4CAF50"))
            }
        }

        // Time-based metrics
        findViewById<TextView>(R.id.tvTasksToday)?.text = stats.tasksCompletedToday.toString()
        findViewById<TextView>(R.id.tvTasksThisWeek)?.text = stats.tasksCompletedThisWeek.toString()
        findViewById<TextView>(R.id.tvTasksThisMonth)?.text = stats.tasksCompletedThisMonth.toString()

        // Completion streak
        val streak = TaskStatistics.getCompletionStreak(allTasks)
        findViewById<TextView>(R.id.tvCompletionStreak)?.text = "$streak"

        // Upcoming & Overdue
        val overdueTasks = TaskStatistics.getOverdueTasks(allTasks)
        val dueTodayTasks = TaskStatistics.getTasksDueToday(allTasks)
        val dueThisWeekTasks = TaskStatistics.getTasksDueThisWeek(allTasks)

        findViewById<TextView>(R.id.tvOverdueTasks)?.text = "${overdueTasks.size} overdue"
        findViewById<TextView>(R.id.tvDueToday)?.text = "${dueTodayTasks.size} due today"
        findViewById<TextView>(R.id.tvDueThisWeek)?.text = "${dueThisWeekTasks.size} due this week"

        // Category distribution
        val categoryContainer = findViewById<LinearLayout>(R.id.categoryContainer)
        categoryContainer?.removeAllViews()

        stats.tasksByCategory.forEach { (category, count) ->
            val categoryView = LayoutInflater.from(this).inflate(R.layout.item_category_stat, categoryContainer, false)

            val icon = when (category) {
                "work" -> "💼"
                "personal" -> "🏠"
                "study" -> "📚"
                "health" -> "💪"
                "shopping" -> "🛒"
                else -> "📌"
            }

            val name = when (category) {
                "work" -> "Work"
                "personal" -> "Personal"
                "study" -> "Study"
                "health" -> "Health"
                "shopping" -> "Shopping"
                else -> "Other"
            }

            categoryView.findViewById<TextView>(R.id.tvCategoryIcon)?.text = icon
            categoryView.findViewById<TextView>(R.id.tvCategoryName)?.text = name
            categoryView.findViewById<TextView>(R.id.tvCategoryCount)?.text = count.toString()

            categoryContainer.addView(categoryView)
        }

        Log.i(TAG, "Statistics loaded: ${stats.totalTasks} total tasks, ${stats.productivityScore} productivity score")
    }
}
