package com.example.chacego.ui.theme.History

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chacego.data.GameHistory
import com.example.chacego.data.PlayerProfile
import com.example.chacego.data.ProfileApiService
import com.example.chacego.data.RetrofitClient
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.io.IOException

class HistoryViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val profileApiService: ProfileApiService = RetrofitClient.api

    // State Management
    var historyOfGames by mutableStateOf<List<GameHistory>>(emptyList())
        private set

    var profile by mutableStateOf<PlayerProfile?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    init {
        // Load history when ViewModel is created
        loadHistory()
    }

    /**
     * Loads the user's profile and history from the backend
     */
    fun loadHistory() {
        viewModelScope.launch {
            val user = currentUser ?: return@launch
            if (!user.isEmailVerified) {
                errorMessage = "Email not verified"
                return@launch
            }

            isLoading = true
            errorMessage = null

            try {
                val idToken: String = user.getIdToken(true).await().token
                    ?: throw IOException("ID Token is empty.")

                Log.d("HistoryViewModel", "Loading history from backend...")
                profile = profileApiService.getProfile("Bearer $idToken")
                historyOfGames = profile?.historyOfGames ?: emptyList()
                Log.d("HistoryViewModel", "History loaded: ${historyOfGames.size} games")

            } catch (e: HttpException) {
                Log.e("HistoryViewModel", "HTTP error: ${e.code()}, message: ${e.message()}")
                errorMessage = when (e.code()) {
                    404 -> "Profile not found"
                    else -> "Server error (${e.code()})"
                }
            } catch (e: IOException) {
                Log.e("HistoryViewModel", "Network error", e)
                errorMessage = "Network error. Please check your connection."
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Unknown error", e)
                errorMessage = "Unknown error: ${e.localizedMessage ?: e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Refreshes the history data
     */
    fun refreshHistory() {
        loadHistory()
    }
}

