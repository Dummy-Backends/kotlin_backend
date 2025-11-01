package com.example.chacego.ui.theme.Auth

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chacego.data.CustomizationRequest
import com.example.chacego.data.PlayerProfile
import com.example.chacego.data.ProfileApiService
import com.example.chacego.data.RetrofitClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException

// Constants
const val RC_SIGN_IN_GOOGLE = 9001

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val profileApiService: ProfileApiService = RetrofitClient.api

    // --- State Management ---
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var nicknameInput by mutableStateOf("")
    var pictureUrlInput by mutableStateOf("")
    var selectedImageUri by mutableStateOf<Uri?>(null)

    var isAuthenticated by mutableStateOf(auth.currentUser?.isEmailVerified == true)
    var isLoading by mutableStateOf(false)
    var isUploadingImage by mutableStateOf(false)
    var isSigningUp by mutableStateOf(true)
    var isEditingProfile by mutableStateOf(false)

    var errorMessage by mutableStateOf<String?>(null)
    var infoMessage by mutableStateOf<String?>(null)

    var profile by mutableStateOf<PlayerProfile?>(null)

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    init {
        // Observe auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            isAuthenticated = user != null && user.isEmailVerified
            
            if (isAuthenticated && profile == null) {
                fetchOrCreateProfile(null)
            } else if (!isAuthenticated) {
                profile = null
            }
        }
    }

    private fun handleAuthResult(task: Task<AuthResult>, successMessage: String, activity: Activity) {
        isLoading = false
        if (task.isSuccessful) {
            val user = auth.currentUser
            if (user != null && user.isEmailVerified) {
                Log.d("AuthViewModel", successMessage)
                isAuthenticated = true
                fetchOrCreateProfile(activity)
            } else if (user != null) {
                user.sendEmailVerification()
                auth.signOut()
                infoMessage = "Un email de vérification a été envoyé. Veuillez vérifier votre boîte mail."
            }
        } else {
            errorMessage = task.exception?.localizedMessage ?: "Erreur d'authentification inconnue."
            Log.e("AuthViewModel", "Authentication failed", task.exception)
            isAuthenticated = false
        }
    }

    private fun resetAuthForm() {
        email = ""
        password = ""
        errorMessage = null
        infoMessage = null
    }

    // --- Auth Methods ---
    fun signUpWithEmail(activity: Activity) {
        if (email.isBlank() || password.length < 6) return

        isLoading = true
        errorMessage = null
        infoMessage = null

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                isLoading = false
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()?.addOnCompleteListener { sendTask ->
                        if (sendTask.isSuccessful) {
                            infoMessage = "Un email de vérification a été envoyé. Veuillez vérifier votre boîte mail et vous reconnecter."
                            auth.signOut()
                            isAuthenticated = false
                        } else {
                            errorMessage = sendTask.exception?.localizedMessage ?: "Échec de l'envoi de l'email de vérification."
                            auth.signOut()
                        }
                    }
                } else {
                    errorMessage = task.exception?.localizedMessage ?: "Échec de l'inscription."
                }
            }
    }

    fun signInWithEmail(activity: Activity) {
        if (email.isBlank() || password.isBlank()) return

        isLoading = true
        errorMessage = null
        infoMessage = null

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        handleAuthResult(task, "Connexion Email réussie", activity)
                    } else {
                        isLoading = false
                        auth.signOut()
                        isAuthenticated = false
                        infoMessage = "Votre email n'est pas vérifié. Veuillez vérifier votre boîte mail."
                        user?.sendEmailVerification()
                    }
                } else {
                    isLoading = false
                    errorMessage = task.exception?.localizedMessage ?: "Échec de la connexion."
                }
            }
    }

    fun firebaseAuthWithGoogle(idToken: String, activity: Activity) {
        isLoading = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                handleAuthResult(task, "Connexion Google réussie", activity)
            }
    }

    fun signOut(activity: Activity) {
        auth.signOut()
        GoogleSignIn.getClient(activity, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        isAuthenticated = false
        resetAuthForm()
        profile = null
    }

    fun toggleSignUpMode() {
        isSigningUp = !isSigningUp
        resetAuthForm()
    }

    // --- Profile API Integration ---
    fun fetchOrCreateProfile(activity: Activity?) {
        viewModelScope.launch {
            val user = currentUser ?: return@launch
            if (!user.isEmailVerified) return@launch

            isLoading = true
            errorMessage = null

            try {
                val idToken: String = user.getIdToken(true).await().token
                    ?: throw IOException("ID Token is empty.")

                Log.d("AuthViewModel", "Fetching profile from backend...")
                profile = profileApiService.getProfile("Bearer $idToken")
                Log.d("AuthViewModel", "Profile fetched: nickname=${profile?.nickname}")

                if (profile?.nickname == "NewPlayer") {
                    Log.d("AuthViewModel", "Profile needs customization - showing CustomizationScreen")
                }

            } catch (e: HttpException) {
                Log.e("AuthViewModel", "HTTP error: ${e.code()}, message: ${e.message()}")
                if (e.code() == 404) {
                    Log.i("AuthViewModel", "Profile not found (404). Creating default profile.")
                } else {
                    Log.w("AuthViewModel", "Server error (${e.code()}). Creating default profile to allow user to continue.")
                    errorMessage = "Erreur serveur (${e.code()}). Vous pouvez continuer avec un profil par défaut."
                }
                // Create a default profile on 404 or other server errors to unblock the user
                profile = PlayerProfile(
                    userId = user.uid,
                    nickname = "NewPlayer",
                    profilePictureUrl = "",
                    score = 100, wins = 0, losses = 0, winrate = 0.0, historyOfGames = emptyList()
                )
            } catch (e: IOException) {
                Log.e("AuthViewModel", "Network error", e)
                errorMessage = "Erreur réseau. Vérifiez que le serveur backend est démarré."
                profile = PlayerProfile(
                    userId = user.uid,
                    nickname = "NewPlayer",
                    profilePictureUrl = "",
                    score = 100, wins = 0, losses = 0, winrate = 0.0, historyOfGames = emptyList()
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Unknown error", e)
                errorMessage = "Erreur inconnue: ${e.localizedMessage ?: e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun uploadImageToServer(uri: Uri, context: Context) {
        viewModelScope.launch {
            val user = currentUser ?: return@launch
            if (!user.isEmailVerified) return@launch

            isUploadingImage = true
            errorMessage = null

            try {
                val idToken: String = user.getIdToken(true).await().token
                    ?: throw IOException("Failed to retrieve ID Token.")

                val contentResolver: ContentResolver = context.contentResolver

                // Get file display name
                var displayName = "image.jpg"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIndex) ?: "image.jpg"
                    }
                }

                // Get MIME type and read file bytes
                val mimeType = contentResolver.getType(uri)
                val fileBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IOException("Unable to read image file.")

                // Create the request body and multipart part
                val mediaType = mimeType?.toMediaTypeOrNull() ?: "image/*".toMediaTypeOrNull()
                val requestFile = fileBytes.toRequestBody(mediaType)

                val imagePart = MultipartBody.Part.createFormData(
                    "image", // Ensure this name matches the backend's expected field name
                    displayName,
                    requestFile
                )

                // Upload to backend
                val response = profileApiService.uploadImage("Bearer $idToken", imagePart)

                if (response.success) {
                    pictureUrlInput = response.imageUrl
                    Log.d("AuthViewModel", "Image uploaded successfully: ${response.imageUrl}")
                    Toast.makeText(context, "Image téléchargée avec succès!", Toast.LENGTH_SHORT).show()
                } else {
                    throw IOException("Upload failed: Server returned success=false")
                }

            } catch (e: HttpException) {
                Log.e("AuthViewModel", "HTTP error uploading image", e)
                errorMessage = "Erreur serveur (${e.code()}): ${e.message()}"
                Toast.makeText(context, "Erreur serveur lors du téléchargement de l'image", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Log.e("AuthViewModel", "Error uploading image", e)
                errorMessage = "Erreur lors du téléchargement de l'image: ${e.localizedMessage ?: e.message}"
                Toast.makeText(context, "Erreur lors du téléchargement de l'image", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Unknown error uploading image", e)
                errorMessage = "Erreur inconnue: ${e.localizedMessage ?: e.message}"
                Toast.makeText(context, "Erreur lors du téléchargement de l'image", Toast.LENGTH_SHORT).show()
            } finally {
                isUploadingImage = false
            }
        }
    }

    fun createOrUpdateProfile(activity: Activity, nickname: String, pictureUrl: String) {
        viewModelScope.launch {
            val user = currentUser ?: return@launch
            if (!user.isEmailVerified) return@launch

            isLoading = true
            errorMessage = null

            try {
                val idToken: String = user.getIdToken(true).await().token
                    ?: throw IOException("Failed to retrieve ID Token.")

                val customizationRequest = CustomizationRequest(
                    nickname = nickname.trim(),
                    profilePictureUrl = pictureUrl.trim().takeIf { it.isNotBlank() }
                )

                Log.d("AuthViewModel", "Updating profile with nickname: ${nickname.trim()}")
                profile = profileApiService.updateProfile("Bearer $idToken", customizationRequest)

                Log.d("AuthViewModel", "Profile updated successfully: ${profile?.nickname}")
                Toast.makeText(activity, "Profil mis à jour avec succès!", Toast.LENGTH_SHORT).show()
                
                // If editing, reset the editing state after successful update
                if (isEditingProfile) {
                    isEditingProfile = false
                }

            } catch (e: HttpException) {
                Log.e("AuthViewModel", "HTTP error updating profile", e)
                errorMessage = "Erreur serveur (${e.code()}). Impossible de mettre à jour le profil."
            } catch (e: IOException) {
                Log.e("AuthViewModel", "Network error updating profile", e)
                errorMessage = "Erreur réseau. Vérifiez votre connexion et que le serveur est démarré."
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Unknown error updating profile", e)
                errorMessage = "Erreur inconnue: ${e.localizedMessage ?: e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}
