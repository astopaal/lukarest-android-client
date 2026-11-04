package com.lukasoft.lukarest.ui.navigation

sealed class Screen(val route: String) {
    // Auth
    data object Login : Screen("login")
    data object Verify : Screen("verify")

    // Staff
    data object StaffToday : Screen("staff/today")
    data object StaffShifts : Screen("staff/shifts")
    data object StaffTasks : Screen("staff/tasks")
    data object StaffTaskComplete : Screen("staff/tasks/{taskId}/complete") {
        fun createRoute(taskId: String) = "staff/tasks/$taskId/complete"
    }
    data object StaffRequests : Screen("staff/requests")
    data object StaffShiftChange : Screen("staff/requests/shift-change")
    data object StaffLeaveChange : Screen("staff/requests/leave-change")
    data object StaffPair : Screen("staff/pair")

    // Boss
    data object BossDashboard : Screen("boss/dashboard")
    data object BossCompanySetup : Screen("boss/company/setup")
    data object BossStaffScan : Screen("boss/staff/scan")
    data object BossShifts : Screen("boss/shifts")
    data object BossAddShift : Screen("boss/shifts/add")
    data object BossTasks : Screen("boss/tasks")
    data object BossCreateTask : Screen("boss/tasks/create")
    data object BossRequests : Screen("boss/requests")
    data object BossRequestDetail : Screen("boss/requests/{requestId}") {
        fun createRoute(requestId: String) = "boss/requests/$requestId"
    }
    data object OperationalNotes : Screen("boss/notes")
}

