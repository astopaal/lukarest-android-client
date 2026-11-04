package com.lukasoft.lukarest.data.store

import android.content.Context
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.LogoutCommand
import com.lukasoft.lukarest.data.api.UnregisterDeviceCommand
import java.io.IOException
import android.content.SharedPreferences
import com.lukasoft.lukarest.data.api.MeDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppRole {
    STAFF,
    BOSS,
    NONE
}

object AuthStore {

    private const val PREFS_NAME = "lukarest_auth_prefs"
    private const val KEY_ROLE = "key_role"
    private const val KEY_ACCESS_TOKEN = "key_access_token"
    private const val KEY_REFRESH_TOKEN = "key_refresh_token"
    private const val KEY_STAFF_ID = "key_staff_id"
    private const val KEY_STAFF_NAME = "key_staff_name"
    private const val KEY_IS_PAIRED = "key_is_paired"
    private const val KEY_USER_ID = "key_user_id"
    private const val KEY_COMPANY_ID = "key_company_id"
    private const val KEY_COMPANY_NAME = "key_company_name"
    private const val KEY_ACTIVE_BRANCH_ID = "key_active_branch_id"
    private const val KEY_ACTIVE_BRANCH_NAME = "key_active_branch_name"
    private const val KEY_GLOBAL_ROLE = "key_global_role"
    private const val KEY_ROLE_IN_BRANCH = "key_role_in_branch"
    private const val KEY_FCM_TOKEN = "key_fcm_token"
    private const val KEY_REQUIRES_SETUP = "key_requires_setup"

    private var sharedPreferences: SharedPreferences? = null

    private val _role = MutableStateFlow(AppRole.NONE)
    val role: StateFlow<AppRole> = _role.asStateFlow()

    private val _requiresSetup = MutableStateFlow(false)
    val requiresSetup: StateFlow<Boolean> = _requiresSetup.asStateFlow()

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    private val _refreshToken = MutableStateFlow<String?>(null)
    val refreshToken: StateFlow<String?> = _refreshToken.asStateFlow()

    private val _staffId = MutableStateFlow<String?>(null)
    val staffId: StateFlow<String?> = _staffId.asStateFlow()

    private val _staffName = MutableStateFlow<String?>(null)
    val staffName: StateFlow<String?> = _staffName.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _companyId = MutableStateFlow<String?>(null)
    val companyId: StateFlow<String?> = _companyId.asStateFlow()

    private val _companyName = MutableStateFlow<String?>(null)
    val companyName: StateFlow<String?> = _companyName.asStateFlow()

    private val _activeBranchId = MutableStateFlow<String?>(null)
    val activeBranchId: StateFlow<String?> = _activeBranchId.asStateFlow()

    private val _activeBranchName = MutableStateFlow<String?>(null)
    val activeBranchName: StateFlow<String?> = _activeBranchName.asStateFlow()

    private val _globalRole = MutableStateFlow<String?>(null)
    val globalRole: StateFlow<String?> = _globalRole.asStateFlow()

    private val _roleInBranch = MutableStateFlow<String?>(null)
    val roleInBranch: StateFlow<String?> = _roleInBranch.asStateFlow()

    private val _me = MutableStateFlow<MeDto?>(null)
    val me: StateFlow<MeDto?> = _me.asStateFlow()

    private val _isPaired = MutableStateFlow(false)
    val isPaired: StateFlow<Boolean> = _isPaired.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences = prefs

        val savedRoleStr = prefs.getString(KEY_ROLE, AppRole.NONE.name) ?: AppRole.NONE.name
        val savedRole = try { AppRole.valueOf(savedRoleStr) } catch (_: Exception) { AppRole.NONE }

        _role.value = savedRole
        _accessToken.value = prefs.getString(KEY_ACCESS_TOKEN, null)
        _refreshToken.value = prefs.getString(KEY_REFRESH_TOKEN, null)
        _staffId.value = prefs.getString(KEY_STAFF_ID, null)
        _staffName.value = prefs.getString(KEY_STAFF_NAME, null)
        _userId.value = prefs.getString(KEY_USER_ID, null)
        _companyId.value = prefs.getString(KEY_COMPANY_ID, null)
        _companyName.value = prefs.getString(KEY_COMPANY_NAME, null)
        _activeBranchId.value = prefs.getString(KEY_ACTIVE_BRANCH_ID, null)
        _activeBranchName.value = prefs.getString(KEY_ACTIVE_BRANCH_NAME, null)
        _globalRole.value = prefs.getString(KEY_GLOBAL_ROLE, null)
        _roleInBranch.value = prefs.getString(KEY_ROLE_IN_BRANCH, null)
        _fcmToken.value = prefs.getString(KEY_FCM_TOKEN, null)
        _isPaired.value = prefs.getBoolean(KEY_IS_PAIRED, false)
        _requiresSetup.value = prefs.getBoolean(KEY_REQUIRES_SETUP, false)
    }

    fun setRole(role: AppRole) {
        _role.value = role
        sharedPreferences?.edit()?.putString(KEY_ROLE, role.name)?.apply()
    }

    fun setRequiresSetup(value: Boolean) {
        _requiresSetup.value = value
        sharedPreferences?.edit()?.putBoolean(KEY_REQUIRES_SETUP, value)?.apply()
    }

    fun clearTokens() {
        _accessToken.value = null
        _refreshToken.value = null
        sharedPreferences?.edit()
            ?.remove(KEY_ACCESS_TOKEN)
            ?.remove(KEY_REFRESH_TOKEN)
            ?.apply()
    }

    fun setTokens(access: String?, refresh: String?, requiresSetup: Boolean? = null) {
        _accessToken.value = access
        _refreshToken.value = refresh
        if (requiresSetup != null) {
            _requiresSetup.value = requiresSetup
        }
        val editor = sharedPreferences?.edit()
            ?.putString(KEY_ACCESS_TOKEN, access)
            ?.putString(KEY_REFRESH_TOKEN, refresh)
        if (requiresSetup != null) {
            editor?.putBoolean(KEY_REQUIRES_SETUP, requiresSetup)
        }
        editor?.apply()
    }

    fun setStaff(id: String, name: String) {
        _staffId.value = id
        _staffName.value = name
        _isPaired.value = true
        _role.value = AppRole.STAFF

        sharedPreferences?.edit()
            ?.putString(KEY_STAFF_ID, id)
            ?.putString(KEY_STAFF_NAME, name)
            ?.putBoolean(KEY_IS_PAIRED, true)
            ?.putString(KEY_ROLE, AppRole.STAFF.name)
            ?.apply()
    }

    fun updateUser(me: MeDto) {
        _me.value = me
        _userId.value = me.userId
        _staffName.value = me.fullName
        _companyId.value = me.companyId
        _companyName.value = me.companyName
        _activeBranchId.value = me.activeBranchId
        _activeBranchName.value = me.activeBranchName
        _globalRole.value = me.globalRole
        _roleInBranch.value = me.roleInBranch
        _requiresSetup.value = me.requiresSetup

        val determinedRole = if (me.globalRole == "CompanyOwner" || me.roleInBranch in listOf("Owner", "GeneralManager", "ShiftSupervisor")) {
            AppRole.BOSS
        } else {
            AppRole.STAFF
        }
        _role.value = determinedRole

        sharedPreferences?.edit()
            ?.putString(KEY_USER_ID, me.userId)
            ?.putString(KEY_STAFF_NAME, me.fullName)
            ?.putString(KEY_COMPANY_ID, me.companyId)
            ?.putString(KEY_COMPANY_NAME, me.companyName)
            ?.putString(KEY_ACTIVE_BRANCH_ID, me.activeBranchId)
            ?.putString(KEY_ACTIVE_BRANCH_NAME, me.activeBranchName)
            ?.putString(KEY_GLOBAL_ROLE, me.globalRole)
            ?.putString(KEY_ROLE_IN_BRANCH, me.roleInBranch)
            ?.putBoolean(KEY_REQUIRES_SETUP, me.requiresSetup)
            ?.putString(KEY_ROLE, determinedRole.name)
            ?.apply()
    }

    fun updateActiveBranch(branchId: String, branchName: String) {
        _activeBranchId.value = branchId
        _activeBranchName.value = branchName
        val current = _me.value
        if (current != null) {
            _me.value = current.copy(activeBranchId = branchId, activeBranchName = branchName)
        }
        sharedPreferences?.edit()
            ?.putString(KEY_ACTIVE_BRANCH_ID, branchId)
            ?.putString(KEY_ACTIVE_BRANCH_NAME, branchName)
            ?.apply()
    }

    fun setFcmToken(token: String?) {
        _fcmToken.value = token
        sharedPreferences?.edit()?.putString(KEY_FCM_TOKEN, token)?.apply()
    }

    suspend fun registerCurrentDevice(token: String, pushEnabled: Boolean) {
        setFcmToken(token)
        try {
            com.lukasoft.lukarest.data.api.ApiClient.devicesService.registerDevice(
                com.lukasoft.lukarest.data.api.RegisterDeviceCommand(
                    fcmPushToken = token,
                    deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                    operatingSystem = "Android ${android.os.Build.VERSION.RELEASE}",
                    isPushEnabled = pushEnabled
                )
            )
        } catch (_: Exception) {}
    }

    suspend fun endSession() {
        try {
            fcmToken.value?.let { fid ->
                try {
                    ApiClient.devicesService.unregisterDevice(UnregisterDeviceCommand(fid))
                } catch (_: IOException) {
                    // Continue with server logout even if device cleanup fails.
                }
            }
            refreshToken.value?.let { ApiClient.authService.logout(LogoutCommand(it)) }
        } catch (_: IOException) {
            // Local logout must also work offline.
        } finally {
            logout()
        }
    }

    fun logout() {
        _fcmToken.value = null
        _role.value = AppRole.NONE
        _accessToken.value = null
        _refreshToken.value = null
        _staffId.value = null
        _staffName.value = null
        _userId.value = null
        _companyId.value = null
        _companyName.value = null
        _activeBranchId.value = null
        _activeBranchName.value = null
        _globalRole.value = null
        _roleInBranch.value = null
        _me.value = null
        _isPaired.value = false
        _requiresSetup.value = false

        sharedPreferences?.edit()?.clear()?.apply()
    }
}


