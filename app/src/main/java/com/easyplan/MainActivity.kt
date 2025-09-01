package com.easyplan

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.easyplan.data.Task
import com.easyplan.util.TaskManager
import com.easyplan.util.ThemeUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private var selectedDate = Date()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.applySavedTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Load tasks from Firestore when authenticated; fallback to samples
        TaskManager.loadTasksForUser {
            updateTasksForSelectedDate()
        }
        TaskManager.initializeSampleTasks()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        val viewTasks = findViewById<View>(R.id.view_tasks)
        val viewCalendar = findViewById<View>(R.id.view_calendar)
        val viewSettings = findViewById<View>(R.id.view_settings)

        fun show(viewToShow: View) {
            viewTasks.visibility = if (viewToShow === viewTasks) View.VISIBLE else View.GONE
            viewCalendar.visibility = if (viewToShow === viewCalendar) View.VISIBLE else View.GONE
            viewSettings.visibility = if (viewToShow === viewSettings) View.VISIBLE else View.GONE
        }

        // Default
        show(viewTasks)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> {
                    show(viewTasks)
                    setupTasksListeners()
                }
                R.id.nav_calendar -> {
                    show(viewCalendar)
                    setupCalendarListeners()
                }
                R.id.nav_settings -> {
                    show(viewSettings)
                    setupSettingsListeners()
                }
                R.id.nav_faq -> {
                    showFAQBottomSheet()
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

            taskView.findViewById<TextView>(R.id.taskTitle)?.text = task.title
            taskView.findViewById<TextView>(R.id.taskTime)?.text = task.dueTime ?: "No time set"

            val checkbox = taskView.findViewById<MaterialCheckBox>(R.id.taskCheckbox)
            checkbox?.isChecked = task.isCompleted
            checkbox?.setOnCheckedChangeListener { _, isChecked ->
                TaskManager.toggleTaskCompletion(task.id)
                updateTasksForSelectedDate()
            }

            container.addView(taskView)
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

            val newTask = Task(
                title = title,
                description = description,
                dueDate = taskDueDate,
                dueTime = taskDueTime
            )

            TaskManager.addTask(newTask)
            updateTasksForSelectedDate()
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
        closeButton?.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }
}