package com.example.chacego.ui.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chacego.ui.auth.AuthViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(),
    onGoogleSignInClicked: () -> Unit
) {
    if (viewModel.isAuthenticated) {
        MainScreen(viewModel)
    } else {
        AuthenticationForm(viewModel, onGoogleSignInClicked)
    }
}


@SuppressLint("ContextCastToActivity")
@Composable
fun MainScreen(viewModel: AuthViewModel) {
    val context = LocalContext.current as Activity
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bienvenue !", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "UID: ${viewModel.currentUser?.uid ?: "N/A"}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Email: ${viewModel.currentUser?.email ?: "N/A"}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            viewModel.signOut(context)
            Toast.makeText(context, "Déconnexion réussie", Toast.LENGTH_SHORT).show()
        }) {
            Text("Déconnexion")
        }
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun AuthenticationForm(viewModel: AuthViewModel, onGoogleSignInClicked: () -> Unit) {
    val context = LocalContext.current as Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (viewModel.isSigningUp) "Inscription" else "Connexion",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        // --- Message d'erreur ---
        if (viewModel.errorMessage != null) {
            Text(
                text = "Erreur: ${viewModel.errorMessage}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // --- Message d'information ---
        if (viewModel.infoMessage != null) {
            Text(
                text = viewModel.infoMessage!!,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // --- Formulaire classique ---
        if (viewModel.isSigningUp) {
            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.email = it },
                label = { Text("Email") },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                label = { Text("Mot de passe") },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Le mot de passe doit contenir au moins 6 caractères",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.signUpWithEmail(context) },
                enabled = !viewModel.isLoading && viewModel.email.isNotBlank() && viewModel.password.length >= 6,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Créer le compte")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Déjà un compte? Connectez-vous",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { viewModel.toggleSignUpMode() }
            )
        } else {
            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.email = it },
                label = { Text("Email") },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                label = { Text("Mot de passe") },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.signInWithEmail(context) },
                enabled = !viewModel.isLoading && viewModel.email.isNotBlank() && viewModel.password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Se connecter")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pas encore inscrit? Créez un compte",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { viewModel.toggleSignUpMode() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Divider(Modifier.padding(horizontal = 32.dp))
        Text("OU", modifier = Modifier.padding(vertical = 16.dp))

        // Google
        Button(
            onClick = onGoogleSignInClicked,
            enabled = !viewModel.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("S'inscrire/Se connecter avec Google")
        }
    }
}
