package com.lukasoft.lukarest.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.lukasoft.lukarest.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LukaViewModel : ViewModel() {

    // ── Auth State ──
    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    // ── Shifts State (React Native'deki gibi başlangıçta boş liste) ──
    private val _definitions = MutableStateFlow<List<ShiftDefinition>>(emptyList())
    val definitions: StateFlow<List<ShiftDefinition>> = _definitions.asStateFlow()

    private val _assignments = MutableStateFlow<List<CalendarAssignment>>(emptyList())
    val assignments: StateFlow<List<CalendarAssignment>> = _assignments.asStateFlow()

    private val _selectedPaletteShiftId = MutableStateFlow<String?>(null)
    val selectedPaletteShiftId: StateFlow<String?> = _selectedPaletteShiftId.asStateFlow()

    // ── Tasks State ──
    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    // ── Requests State ──
    private val _requests = MutableStateFlow<List<BaseRequest>>(emptyList())
    val requests: StateFlow<List<BaseRequest>> = _requests.asStateFlow()

    // ── Staff State ──
    private val _staffList = MutableStateFlow<List<StaffMember>>(emptyList())
    val staffList: StateFlow<List<StaffMember>> = _staffList.asStateFlow()

    // ── Break State (Personel bugün ekranı için) ──
    private val _isOnBreak = MutableStateFlow(false)
    val isOnBreak: StateFlow<Boolean> = _isOnBreak.asStateFlow()

    fun toggleBreak() {
        _isOnBreak.update { !it }
    }

    // ── Auth Actions ──
    fun setUser(user: AuthUser?) {
        _currentUser.value = user
    }

    // ── Shift Actions ──
    fun setDefinitions(definitions: List<ShiftDefinition>) {
        _definitions.value = definitions
        if (_selectedPaletteShiftId.value == null && definitions.isNotEmpty()) {
            _selectedPaletteShiftId.value = definitions.first().id
        }
    }

    fun setAssignments(assignments: List<CalendarAssignment>) {
        _assignments.value = assignments
    }

    fun selectPaletteShift(shiftId: String?) {
        _selectedPaletteShiftId.value = shiftId
    }

    fun assignShift(staffId: String, dayIndex: Int, shiftId: String) {
        _assignments.update { list ->
            val filtered = list.filterNot { it.staffId == staffId && it.dayIndex == dayIndex }
            filtered + CalendarAssignment(staffId, dayIndex, shiftId)
        }
    }

    fun removeAssignment(staffId: String, dayIndex: Int) {
        _assignments.update { list ->
            list.filterNot { it.staffId == staffId && it.dayIndex == dayIndex }
        }
    }

    fun addShiftDefinition(definition: ShiftDefinition) {
        _definitions.update { it + definition }
        if (_selectedPaletteShiftId.value == null) {
            _selectedPaletteShiftId.value = definition.id
        }
    }

    fun clearAllAssignments() {
        _assignments.value = emptyList()
    }

    // ── Task Actions ──
    fun setTasks(tasks: List<TaskItem>) {
        _tasks.value = tasks
    }

    fun addTask(task: TaskItem) {
        _tasks.update { listOf(task) + it }
    }

    fun updateTask(updated: TaskItem) {
        _tasks.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }
    }

    fun deleteTask(taskId: String) {
        _tasks.update { list -> list.filterNot { it.id == taskId } }
    }

    // ── Request Actions ──
    fun setRequests(requests: List<BaseRequest>) {
        _requests.value = requests
    }

    fun addRequest(request: BaseRequest) {
        _requests.update { listOf(request) + it }
    }

    fun decideRequest(requestId: String, status: RequestStatus, managerNote: String? = null) {
        _requests.update { list ->
            list.map { req ->
                if (req.id == requestId) {
                    when (req) {
                        is ShiftChangeRequest -> req.copy(status = status, managerNote = managerNote ?: req.managerNote)
                        is LeaveChangeRequest -> req.copy(status = status, managerNote = managerNote ?: req.managerNote)
                    }
                } else req
            }
        }
    }

    // ── Staff Actions ──
    fun setStaffList(staff: List<StaffMember>) {
        _staffList.value = staff
    }

    fun addStaff(staffMember: StaffMember) {
        _staffList.update { it + staffMember }
    }
}
