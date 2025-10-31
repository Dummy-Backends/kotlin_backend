package com.example.chacego.ui.auth

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

const val RC_SIGN_IN_GOOGLE = 9001

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

    // --- États de l'UI et de l'Authentification ---
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var infoMessage by mutableStateOf<String?>(null)

    // État de l'utilisateur
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // États de navigation
    var isSigningUp by mutableStateOf(true)
    var isAuthenticated by mutableStateOf(auth.currentUser != null)

    init {
        auth.addAuthStateListener { firebaseAuth ->
            isAuthenticated = firebaseAuth.currentUser != null && (firebaseAuth.currentUser?.isEmailVerified != false)
        }
    }

    private fun handleAuthResult(task: Task<AuthResult>, onSuccessMessage: String) {
        isLoading = false
        if (task.isSuccessful) {
            errorMessage = null
            Log.d("AuthViewModel", "$onSuccessMessage: ${auth.currentUser?.uid}")
            isAuthenticated = true
        } else {
            errorMessage = task.exception?.localizedMessage ?: "Erreur d'authentification inconnue."
            Log.e("AuthViewModel", "Authentication failed", task.exception)
        }
    }

    // --- Inscription classique email/mot de passe avec envoi d'email de confirmation ---
    fun signUpWithEmail(activity: Activity) {
        if (email.isBlank()) {
            errorMessage = "Veuillez entrer une adresse email valide."
            return
        }
        if (password.isBlank() || password.length < 6) {
            errorMessage = "Le mot de passe doit contenir au moins 6 caractères."
            return
        }

        isLoading = true
        errorMessage = null
        infoMessage = null

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { sendTask ->
                            isLoading = false
                            if (sendTask.isSuccessful) {
                                infoMessage = "Un email de confirmation a été envoyé. Veuillez vérifier votre boîte mail."
                                // Option: déconnecter jusqu'à vérification
                                auth.signOut()
                                isAuthenticated = false
                            } else {
                                errorMessage = sendTask.exception?.localizedMessage
                                    ?: "Échec de l'envoi de l'email de confirmation."
                                auth.signOut()
                                isAuthenticated = false
                            }
                        }
                } else {
                    isLoading = false
                    errorMessage = task.exception?.localizedMessage
                        ?: "Échec de l'inscription."
                }
            }
    }

    // --- Connexion email/mot de passe (refus si email non vérifié) ---
    fun signInWithEmail(activity: Activity) {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Veuillez entrer l'email et le mot de passe."
            return
        }

        isLoading = true
        errorMessage = null
        infoMessage = null

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        handleAuthResult(task, "Connexion Email réussie")
                    } else {
                        isLoading = false
                        auth.signOut()
                        isAuthenticated = false
                        infoMessage = "Votre email n'est pas vérifié. Nous venons de renvoyer un email de confirmation."
                        user?.sendEmailVerification()
                    }
                } else {
                    isLoading = false
                    errorMessage = task.exception?.localizedMessage
                        ?: "Échec de la connexion."
                }
            }
    }

    // Réinitialiser le formulaire
    fun resetAuthForm() {
        email = ""
        password = ""
        errorMessage = null
        infoMessage = null
    }

    // Google Sign-In
    fun firebaseAuthWithGoogle(idToken: String, activity: Activity) {
        isLoading = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                handleAuthResult(task, "Connexion Google réussie")
            }
    }

    // Déconnexion
    fun signOut(activity: Activity) {
        auth.signOut()
        GoogleSignIn.getClient(activity, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        isAuthenticated = false
        resetAuthForm()
    }

    // Changer entre inscription et connexion
    fun toggleSignUpMode() {
        isSigningUp = !isSigningUp
        resetAuthForm()
    }
}