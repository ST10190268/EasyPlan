package com.easyplan.util

import android.util.Log
import com.easyplan.data.Task
import java.util.Calendar
import java.util.Date

/**
 * TaskStatistics - Utility class for calculating task statistics and productivity metrics
 * 
 * This class provides various statistical calculations for tasks including:
 * - Completion rates
 * - Productivity trends
 * - Category distribution
 * - Priority analysis
 * - Time-based metrics
 * 
 * @author EasyPlan Team
 * @version 1.0
 * 
 * References:
 * - Kotlin Collections: https://kotlinlang.org/docs/collections-overview.html
 */
object TaskStatistics {
    
    private const val TAG = "TaskStatistics"
    
    /**
     * Data class for holding statistics results
     */
    data class Statistics(
        val totalTasks: Int,
        val completedTasks: Int,
        val pendingTasks: Int,
        val completionRate: Float,
        val highPriorityTasks: Int,
        val mediumPriorityTasks: Int,
        val lowPriorityTasks: Int,
        val tasksByCategory: Map<String, Int>,
        val tasksCompletedToday: Int,
        val tasksCompletedThisWeek: Int,
        val tasksCompletedThisMonth: Int,
        val averageCompletionTime: Long, // in milliseconds
        val productivityScore: Int // 0-100
    )
    
    /**
     * Calculate comprehensive statistics for a list of tasks
     * 
     * @param tasks List of tasks to analyze
     * @return Statistics object with all calculated metrics
     */
    fun calculateStatistics(tasks: List<Task>): Statistics {
        Log.d(TAG, "calculateStatistics: Analyzing ${tasks.size} tasks")
        
        val totalTasks = tasks.size
        val completedTasks = tasks.count { it.isCompleted }
        val pendingTasks = totalTasks - completedTasks
        val completionRate = if (totalTasks > 0) (completedTasks.toFloat() / totalTasks) * 100 else 0f
        
        // Priority distribution
        val highPriority = tasks.count { it.priority == "high" }
        val mediumPriority = tasks.count { it.priority == "medium" }
        val lowPriority = tasks.count { it.priority == "low" }
        
        // Category distribution
        val tasksByCategory = tasks.groupBy { it.category }
            .mapValues { it.value.size }
        
        // Time-based completion metrics
        val today = getStartOfDay(Date())
        val weekAgo = getDateDaysAgo(7)
        val monthAgo = getDateDaysAgo(30)
        
        val tasksCompletedToday = tasks.count { task ->
            task.isCompleted && task.completedAt != null && 
            task.completedAt!!.after(today)
        }
        
        val tasksCompletedThisWeek = tasks.count { task ->
            task.isCompleted && task.completedAt != null && 
            task.completedAt!!.after(weekAgo)
        }
        
        val tasksCompletedThisMonth = tasks.count { task ->
            task.isCompleted && task.completedAt != null && 
            task.completedAt!!.after(monthAgo)
        }
        
        // Average completion time
        val completionTimes = tasks.filter { it.isCompleted && it.completedAt != null }
            .map { it.completedAt!!.time - it.createdAt.time }
        val averageCompletionTime = if (completionTimes.isNotEmpty()) {
            completionTimes.average().toLong()
        } else 0L
        
        // Productivity score (0-100)
        val productivityScore = calculateProductivityScore(
            completionRate,
            tasksCompletedToday,
            tasksCompletedThisWeek,
            highPriority,
            completedTasks
        )
        
        Log.i(TAG, "Statistics calculated: $completionRate% completion rate, $productivityScore productivity score")
        
        return Statistics(
            totalTasks = totalTasks,
            completedTasks = completedTasks,
            pendingTasks = pendingTasks,
            completionRate = completionRate,
            highPriorityTasks = highPriority,
            mediumPriorityTasks = mediumPriority,
            lowPriorityTasks = lowPriority,
            tasksByCategory = tasksByCategory,
            tasksCompletedToday = tasksCompletedToday,
            tasksCompletedThisWeek = tasksCompletedThisWeek,
            tasksCompletedThisMonth = tasksCompletedThisMonth,
            averageCompletionTime = averageCompletionTime,
            productivityScore = productivityScore
        )
    }
    
    /**
     * Calculate productivity score based on various metrics
     * Score ranges from 0-100
     */
    private fun calculateProductivityScore(
        completionRate: Float,
        tasksCompletedToday: Int,
        tasksCompletedThisWeek: Int,
        highPriorityTasks: Int,
        totalCompleted: Int
    ): Int {
        var score = 0
        
        // Completion rate contributes 40 points
        score += (completionRate * 0.4).toInt()
        
        // Daily activity contributes 20 points
        score += minOf(tasksCompletedToday * 5, 20)
        
        // Weekly activity contributes 20 points
        score += minOf(tasksCompletedThisWeek * 2, 20)
        
        // High priority completion contributes 10 points
        score += minOf(highPriorityTasks * 2, 10)
        
        // Total completed tasks contributes 10 points
        score += minOf(totalCompleted, 10)
        
        return minOf(score, 100)
    }
    
    /**
     * Get tasks filtered by priority
     */
    fun getTasksByPriority(tasks: List<Task>, priority: String): List<Task> {
        return tasks.filter { it.priority == priority }
    }
    
    /**
     * Get tasks filtered by category
     */
    fun getTasksByCategory(tasks: List<Task>, category: String): List<Task> {
        return tasks.filter { it.category == category }
    }
    
    /**
     * Get overdue tasks
     */
    fun getOverdueTasks(tasks: List<Task>): List<Task> {
        val now = Date()
        return tasks.filter { task ->
            !task.isCompleted && task.dueDate != null && task.dueDate!!.before(now)
        }
    }
    
    /**
     * Get tasks due today
     */
    fun getTasksDueToday(tasks: List<Task>): List<Task> {
        val today = getStartOfDay(Date())
        val tomorrow = getDateDaysAhead(1)
        
        return tasks.filter { task ->
            task.dueDate != null && 
            task.dueDate!!.after(today) && 
            task.dueDate!!.before(tomorrow)
        }
    }
    
    /**
     * Get tasks due this week
     */
    fun getTasksDueThisWeek(tasks: List<Task>): List<Task> {
        val today = getStartOfDay(Date())
        val weekAhead = getDateDaysAhead(7)
        
        return tasks.filter { task ->
            task.dueDate != null && 
            task.dueDate!!.after(today) && 
            task.dueDate!!.before(weekAhead)
        }
    }
    
    /**
     * Get completion streak (consecutive days with completed tasks)
     */
    fun getCompletionStreak(tasks: List<Task>): Int {
        val completedTasks = tasks.filter { it.isCompleted && it.completedAt != null }
            .sortedByDescending { it.completedAt }
        
        if (completedTasks.isEmpty()) return 0
        
        var streak = 0
        var currentDate = getStartOfDay(Date())
        
        for (i in 0 until 365) { // Check up to a year
            val dayStart = getDateDaysAgo(i)
            val dayEnd = getDateDaysAgo(i - 1)
            
            val hasTasksThisDay = completedTasks.any { task ->
                task.completedAt!!.after(dayStart) && task.completedAt!!.before(dayEnd)
            }
            
            if (hasTasksThisDay) {
                streak++
            } else if (i > 0) {
                break // Streak broken
            }
        }
        
        return streak
    }
    
    /**
     * Helper: Get start of day (00:00:00)
     */
    private fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    /**
     * Helper: Get date N days ago
     */
    private fun getDateDaysAgo(days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        return getStartOfDay(calendar.time)
    }
    
    /**
     * Helper: Get date N days ahead
     */
    private fun getDateDaysAhead(days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return getStartOfDay(calendar.time)
    }
    
    /**
     * Format completion rate as percentage string
     */
    fun formatCompletionRate(rate: Float): String {
        return String.format("%.1f%%", rate)
    }
    
    /**
     * Get productivity level description
     */
    fun getProductivityLevel(score: Int): String {
        return when {
            score >= 80 -> "🔥 Excellent"
            score >= 60 -> "⭐ Great"
            score >= 40 -> "👍 Good"
            score >= 20 -> "📈 Fair"
            else -> "💪 Keep Going"
        }
    }
}

