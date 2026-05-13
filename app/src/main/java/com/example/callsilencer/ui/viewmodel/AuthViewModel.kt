package com.example.callsilencer.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callsilencer.data.repository.CallSilencerRepository
import com.example.callsilencer.data.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object LoggedOut : AuthState()
    data class LoggedIn(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firebaseRepo = FirebaseRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    init {
        val current = auth.currentUser
        _authState.value = if (current != null) {
            AuthState.LoggedIn(current)
        } else {
            AuthState.LoggedOut
        }
    }

    fun signInWithGoogle(idToken: String, context: Context) {
        _authState.value = AuthState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                _authState.value = AuthState.LoggedIn(user)
                // Sync from cloud after login
                syncFromCloud(context)
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
    }

    fun signOut(context: Context) {
        val user = auth.currentUser
        if (user != null) {
            val localRepo = CallSilencerRepository(context)
            viewModelScope.launch {
                try {
                    // Push latest local data to cloud before signing out
                    firebaseRepo.saveAllowedContacts(localRepo.getAllowedContacts())
                    firebaseRepo.saveSilencedCalls(localRepo.getRecentSilencedCalls())
                    firebaseRepo.saveSettings(
                        localRepo.isSilencerActive(),
                        localRepo.isScheduleEnabled()
                    )
                } catch (_: Exception) {}
                auth.signOut()
                _authState.value = AuthState.LoggedOut
            }
        } else {
            auth.signOut()
            _authState.value = AuthState.LoggedOut
        }
    }

    fun syncToFirebase(context: Context) {
        val localRepo = CallSilencerRepository(context)
        viewModelScope.launch {
            try {
                firebaseRepo.saveAllowedContacts(localRepo.getAllowedContacts())
                firebaseRepo.saveSilencedCalls(localRepo.getRecentSilencedCalls())
                firebaseRepo.saveSettings(
                    localRepo.isSilencerActive(),
                    localRepo.isScheduleEnabled()
                )
            } catch (_: Exception) {}
        }
    }

    private fun syncFromCloud(context: Context) {
        val localRepo = CallSilencerRepository(context)
        viewModelScope.launch {
            localRepo.syncFromCloud()
        }
    }
}