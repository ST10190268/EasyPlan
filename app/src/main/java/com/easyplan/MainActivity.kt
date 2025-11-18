package com.easyplan

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.easyplan.data.Task
import com.easyplan.notifications.EasyPlanFirebaseMessagingService
import com.easyplan.notifications.NotificationUtils
import com.easyplan.security.BiometricHelper
import com.easyplan.util.LanguageManager
import com.easyplan.util.NetworkUtils
import com.easyplan.util.SettingsManager
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
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
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

    private fun switchLanguage(language: LanguageManager.SupportedLanguage) {
        if (LanguageManager.getSavedLanguage(this) == language) return
        LanguageManager.applyLanguage(this, language)
        val label = LanguageManager.getDisplayName(this, language)
        Toast.makeText(this, getString(R.string.language_updated, label), Toast.LENGTH_SHORT).show()
        recreate()
    }

    private fun handleBiometricToggle(isChecked: Boolean) {
        val biometricSwitch = findViewById<MaterialSwitch>(R.id.switchBiometric)
        if (isChecked) {
            if (!BiometricHelper.isBiometricAvailable(this)) {
                Toast.makeText(this, getString(R.string.biometric_not_available), Toast.LENGTH_LONG).show()
                biometricSwitch?.isChecked = false
                return
            }
            val prompt = BiometricHelper.createPrompt(this, {
                BiometricHelper.setEnabled(this, true)
                Toast.makeText(this, getString(R.string.biometric_enabled), Toast.LENGTH_SHORT).show()
            }) { error ->
                Toast.makeText(
                    this,
                    error ?: getString(R.string.biometric_enrollment_required),
                    Toast.LENGTH_LONG
                ).show()
                biometricSwitch?.isChecked = false
            }
            prompt.authenticate(BiometricHelper.buildPromptInfo(this))
        } else {
            BiometricHelper.setEnabled(this, false)
            Toast.makeText(this, getString(R.string.biometric_disabled), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleNotificationToggle(isChecked: Boolean) {
        val notificationSwitch = findViewById<MaterialSwitch>(R.id.switchNotifications)
        if (isChecked) {
            if (shouldRequestNotificationPermission()) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
            SettingsManager.setNotificationsEnabled(this, true)
            NotificationUtils.ensureChannels(this)
            FirebaseMessaging.getInstance()
                .subscribeToTopic(EasyPlanFirebaseMessagingService.DEFAULT_TOPIC)
            Toast.makeText(this, getString(R.string.success_notifications_enabled), Toast.LENGTH_SHORT).show()
        } else {
            SettingsManager.setNotificationsEnabled(this, false)
            FirebaseMessaging.getInstance()
                .unsubscribeFromTopic(EasyPlanFirebaseMessagingService.DEFAULT_TOPIC)
            Toast.makeText(this, getString(R.string.success_notifications_disabled), Toast.LENGTH_SHORT).show()
        }
        notificationSwitch?.isChecked = isChecked
    }

    private fun shouldRequestNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    }

    private fun updateOfflineStatusText(textView: TextView?) {
        val pending = TaskManager.getPendingSyncCount()
        textView?.text = if (pending == 0) {
            getString(R.string.offline_sync_complete)
        } else {
            getString(R.string.offline_sync_pending, pending)
        }
    }

    private var selectedDate = Date()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private var lockAppOnResume = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val notificationSwitch = findViewById<MaterialSwitch>(R.id.switchNotifications)
            if (granted) {
                SettingsManager.setNotificationsEnabled(this, true)
                NotificationUtils.ensureChannels(this)
                FirebaseMessaging.getInstance()
                    .subscribeToTopic(EasyPlanFirebaseMessagingService.DEFAULT_TOPIC)
                notificationSwitch?.isChecked = true
                Toast.makeText(
                    this,
                    getString(R.string.success_notifications_enabled),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                SettingsManager.setNotificationsEnabled(this, false)
                notificationSwitch?.isChecked = false
                Toast.makeText(
                    this,
                    getString(R.string.notifications_permission_required),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

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
            updateOfflineStatusText(findViewById(R.id.tvOfflineStatus))
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

    override fun onResume() {
        super.onResume()
        if (lockAppOnResume && BiometricHelper.shouldPromptForBiometrics(this)) {
            enforceBiometricLock()
        } else {
            lockAppOnResume = false
        }
        TaskManager.syncPendingTasks {
            runOnUiThread {
                updateOfflineStatusText(findViewById(R.id.tvOfflineStatus))
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lockAppOnResume = BiometricHelper.shouldPromptForBiometrics(this)
    }

    private fun enforceBiometricLock() {
        val prompt = BiometricHelper.createPrompt(this, {
            lockAppOnResume = false
        }) { error ->
            lockAppOnResume = false
            Toast.makeText(
                this,
                error ?: getString(R.string.biometric_unlock_failed),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
        prompt.authenticate(BiometricHelper.buildPromptInfo(this))
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
            Toast.makeText(this, getString(R.string.toast_plan_week), Toast.LENGTH_SHORT).show()
            findViewById<BottomNavigationView>(R.id.bottom_nav)?.selectedItemId = R.id.nav_calendar
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
            taskView.findViewById<TextView>(R.id.taskTime)?.text = task.dueTime?.let {
                getString(R.string.task_due_time, it)
            } ?: getString(R.string.task_no_time)

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
                updateOfflineStatusText(findViewById(R.id.tvOfflineStatus))
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
                Toast.makeText(this, getString(R.string.error_task_title_required), Toast.LENGTH_SHORT).show()
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

            val savedOffline = !NetworkUtils.isOnline(this)
            TaskManager.addTask(newTask)
            updateTasksForSelectedDate()
            updateTasksTabUI()
            updateOfflineStatusText(findViewById(R.id.tvOfflineStatus))
            bottomSheetDialog.dismiss()
            val messageRes = if (savedOffline) R.string.offline_task_saved else R.string.success_task_created
            Toast.makeText(this, getString(messageRes), Toast.LENGTH_SHORT).show()
        }

        // Cancel
        cancelButton?.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun setupSettingsListeners() {
        val currentTheme = ThemeUtils.getSavedTheme(this)
        findViewById<Chip>(R.id.chipThemeSystem)?.isChecked = currentTheme == ThemeUtils.ThemeMode.SYSTEM
        findViewById<Chip>(R.id.chipThemeLight)?.isChecked = currentTheme == ThemeUtils.ThemeMode.LIGHT
        findViewById<Chip>(R.id.chipThemeDark)?.isChecked = currentTheme == ThemeUtils.ThemeMode.DARK

        findViewById<Chip>(R.id.chipThemeSystem)?.setOnClickListener {
            ThemeUtils.saveTheme(this, ThemeUtils.ThemeMode.SYSTEM)
            Toast.makeText(this, getString(R.string.theme_system), Toast.LENGTH_SHORT).show()
            recreate()
        }
        findViewById<Chip>(R.id.chipThemeLight)?.setOnClickListener {
            ThemeUtils.saveTheme(this, ThemeUtils.ThemeMode.LIGHT)
            Toast.makeText(this, getString(R.string.theme_light), Toast.LENGTH_SHORT).show()
            recreate()
        }
        findViewById<Chip>(R.id.chipThemeDark)?.setOnClickListener {
            ThemeUtils.saveTheme(this, ThemeUtils.ThemeMode.DARK)
            Toast.makeText(this, getString(R.string.theme_dark), Toast.LENGTH_SHORT).show()
            recreate()
        }

        val savedLanguage = LanguageManager.getSavedLanguage(this)
        findViewById<Chip>(R.id.chipEnglish)?.apply {
            isChecked = savedLanguage == LanguageManager.SupportedLanguage.ENGLISH
            setOnClickListener { switchLanguage(LanguageManager.SupportedLanguage.ENGLISH) }
        }
        findViewById<Chip>(R.id.chipZulu)?.apply {
            isChecked = savedLanguage == LanguageManager.SupportedLanguage.ZULU
            setOnClickListener { switchLanguage(LanguageManager.SupportedLanguage.ZULU) }
        }

        val biometricSwitch = findViewById<MaterialSwitch>(R.id.switchBiometric)
        biometricSwitch?.setOnCheckedChangeListener(null)
        biometricSwitch?.isChecked = BiometricHelper.isEnabled(this)
        biometricSwitch?.setOnCheckedChangeListener { _, isChecked ->
            handleBiometricToggle(isChecked, biometricSwitch)
        }

        val notificationSwitch = findViewById<MaterialSwitch>(R.id.switchNotifications)
        notificationSwitch?.setOnCheckedChangeListener(null)
        val systemNotificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        var notificationsEnabled = SettingsManager.isNotificationsEnabled(this)
        if (!systemNotificationsEnabled && notificationsEnabled) {
            SettingsManager.setNotificationsEnabled(this, false)
            notificationsEnabled = false
        }
        notificationSwitch?.isChecked = notificationsEnabled && systemNotificationsEnabled
        if (notificationsEnabled && systemNotificationsEnabled) {
            FirebaseMessaging.getInstance()
                .subscribeToTopic(EasyPlanFirebaseMessagingService.DEFAULT_TOPIC)
        }
        notificationSwitch?.setOnCheckedChangeListener { _, isChecked ->
            handleNotificationToggle(isChecked)
        }

        val offlineStatus = findViewById<TextView>(R.id.tvOfflineStatus)
        updateOfflineStatusText(offlineStatus)
        findViewById<MaterialButton>(R.id.btnOfflineSync)?.setOnClickListener {
            TaskManager.syncPendingTasks { success ->
                runOnUiThread {
                    updateOfflineStatusText(offlineStatus)
                    val msg = if (success) R.string.offline_sync_complete else R.string.offline_sync_failed
                    Toast.makeText(this, getString(msg), Toast.LENGTH_SHORT).show()
                }
            }
        }

        val tvJsonBinStatus = findViewById<TextView>(R.id.tvJsonBinStatus)
        val binId = TaskManager.getJsonBinId()
        tvJsonBinStatus?.text = if (binId.isNullOrEmpty()) {
            getString(R.string.jsonbin_status_unknown)
        } else {
            getString(R.string.jsonbin_status_value, binId)
        }

        findViewById<MaterialButton>(R.id.btnExportJsonBin)?.setOnClickListener {
            Toast.makeText(this, getString(R.string.jsonbin_exporting), Toast.LENGTH_SHORT).show()
            TaskManager.syncToRestApi { success ->
                runOnUiThread {
                    val idNow = TaskManager.getJsonBinId()
                    tvJsonBinStatus?.text = if (idNow.isNullOrEmpty()) {
                        getString(R.string.jsonbin_status_unknown)
                    } else {
                        getString(R.string.jsonbin_status_value, idNow)
                    }
                    val msg = if (success) R.string.jsonbin_export_success else R.string.jsonbin_export_failed
                    Toast.makeText(this, getString(msg), Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<MaterialButton>(R.id.btnImportJsonBin)?.setOnClickListener {
            Toast.makeText(this, getString(R.string.jsonbin_importing), Toast.LENGTH_SHORT).show()
            TaskManager.loadFromRestApi {
                runOnUiThread {
                    updateTasksForSelectedDate()
                    updateTasksTabUI()
                    setupStatistics()
                    updateOfflineStatusText(findViewById(R.id.tvOfflineStatus))
                    Toast.makeText(this, getString(R.string.jsonbin_import_success), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleBiometricToggle(isChecked: Boolean, switch: MaterialSwitch?) {
        if (isChecked) {
            val currentUser = Firebase.auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, getString(R.string.biometric_requires_sign_in), Toast.LENGTH_LONG).show()
                switch?.isChecked = false
                return
            }
            if (!BiometricHelper.isBiometricAvailable(this)) {
                Toast.makeText(this, getString(R.string.biometric_not_available), Toast.LENGTH_LONG).show()
                switch?.isChecked = false
                return
            }
            val prompt = BiometricHelper.createPrompt(this, {
                BiometricHelper.setEnabled(this, true)
                lockAppOnResume = false
                Toast.makeText(this, getString(R.string.biometric_enabled), Toast.LENGTH_SHORT).show()
                switch?.isChecked = true
            }) { error ->
                switch?.isChecked = false
                Toast.makeText(
                    this,
                    error ?: getString(R.string.biometric_unlock_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
            prompt.authenticate(BiometricHelper.buildPromptInfo(this))
        } else {
            BiometricHelper.setEnabled(this, false)
            lockAppOnResume = false
            Toast.makeText(this, getString(R.string.biometric_disabled), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, getString(R.string.error_reminder_title_required), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, getString(R.string.success_reminder_created), Toast.LENGTH_SHORT).show()
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
            android.widget.Toast.makeText(
                this,
                getString(R.string.success_signed_out),
                android.widget.Toast.LENGTH_SHORT
            ).show()
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
