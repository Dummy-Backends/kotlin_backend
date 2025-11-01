package com.example.chacego.ui.theme.Auth



import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.chacego.data.PlayerProfile


@Composable

fun AuthScreen(

    viewModel: AuthViewModel = viewModel(),

    onGoogleSignInClicked: () -> Unit

) {

    if (viewModel.isAuthenticated) {

// --- AUTHENTICATED USER NAVIGATION ---

        when {

// 1. If profile is NOT loaded yet, show a loading screen

            viewModel.profile == null -> LoadingScreen()



// 2. If profile is loaded but needs customization (new user check) OR user is editing

            viewModel.profile!!.nickname == "NewPlayer" || viewModel.isEditingProfile -> CustomizationScreen(viewModel)



// 3. Profile is complete, show completion screen

            else -> ProfileCompleteScreen(viewModel)

        }

    } else {

// --- UN-AUTHENTICATED USER NAVIGATION ---

        AuthenticationForm(viewModel, onGoogleSignInClicked)

    }

}



@Composable

fun LoadingScreen() {

    Column(

        modifier = Modifier.fillMaxSize(),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center

    ) {

        CircularProgressIndicator()

        Spacer(modifier = Modifier.height(16.dp))

        Text("Chargement du profil...")

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



// --- Message d'information (Email verification status) ---

        if (viewModel.infoMessage != null) {

            Text(

                text = viewModel.infoMessage!!,

                color = MaterialTheme.colorScheme.primary,

                modifier = Modifier.padding(bottom = 16.dp)

            )

        }



// --- Formulaire classique: Sign Up ---

        if (viewModel.isSigningUp) {

            OutlinedTextField(

                value = viewModel.email,

                onValueChange = { viewModel.email = it },

                label = { Text("Email") },

                enabled = !viewModel.isLoading,

                modifier = Modifier.fillMaxWidth(),

                singleLine = true,

                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)

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

        }

// --- Formulaire classique: Sign In ---

        else {

            OutlinedTextField(

                value = viewModel.email,

                onValueChange = { viewModel.email = it },

                label = { Text("Email") },

                enabled = !viewModel.isLoading,

                modifier = Modifier.fillMaxWidth(),

                singleLine = true,

                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)

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



// Google Button

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



@Composable

fun CustomizationScreen(viewModel: AuthViewModel) {

    val context = LocalContext.current

    // Initialize inputs with current profile values when editing, or empty for new profile
    var nicknameInput by remember { 
        mutableStateOf(viewModel.profile?.nickname?.takeIf { it != "NewPlayer" } ?: "") 
    }
    
    var pictureUrlInput by remember { mutableStateOf(viewModel.profile?.profilePictureUrl ?: "") }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }



    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            viewModel.selectedImageUri = it
            viewModel.uploadImageToServer(it, context)
        }
    }



    Column(

        modifier = Modifier

            .fillMaxSize()

            .verticalScroll(rememberScrollState())

            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally

    ) {

        Text(

            if (viewModel.isEditingProfile) "Modifier votre profil" else "Finalisez votre profil",

            style = MaterialTheme.typography.headlineLarge,

            fontWeight = FontWeight.Bold

        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(if (viewModel.isEditingProfile) "Mettez à jour vos informations de profil" else "Ceci n'est demandé qu'une seule fois.")

        Spacer(modifier = Modifier.height(32.dp))



        // --- Profile Picture Selection ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Profile picture preview - prioritize selected image, then uploaded URL, then existing profile
            val imageModel = selectedImageUri 
                ?: viewModel.pictureUrlInput.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                ?: viewModel.profile?.profilePictureUrl?.takeIf { it.isNotBlank() && !it.contains("placehold") }
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable {
                        imagePickerLauncher.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AddPhotoAlternate,
                        contentDescription = "Select Profile Picture",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                },
                enabled = !viewModel.isLoading && !viewModel.isUploadingImage,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.isUploadingImage) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Téléchargement...")
                } else {
                    Icon(
                        imageVector = Icons.Filled.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sélectionner une photo")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Sélectionnez une photo de profil depuis votre galerie",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))



// --- Nickname Input ---

        OutlinedTextField(

            value = nicknameInput,

            onValueChange = { nicknameInput = it },

            label = { Text("Nom d'utilisateur (Nickname)") },

            enabled = !viewModel.isLoading,

            modifier = Modifier.fillMaxWidth(),

            singleLine = true

        )

        Spacer(modifier = Modifier.height(32.dp))



// --- Error Message ---

        if (viewModel.errorMessage != null) {

            Text(

                text = "Erreur: ${viewModel.errorMessage}",

                color = MaterialTheme.colorScheme.error,

                modifier = Modifier.padding(bottom = 16.dp)

            )

        }



// --- Save Button ---

        Button(

            onClick = {

                if (nicknameInput.isBlank()) {

                    Toast.makeText(context, "Le Nickname est requis.", Toast.LENGTH_SHORT).show()

                } else {

                    // Use the uploaded URL if available, otherwise use the input field or existing profile picture
                    val finalPictureUrl = viewModel.pictureUrlInput.takeIf { it.isNotBlank() } 
                        ?: pictureUrlInput.takeIf { it.isNotBlank() }
                        ?: viewModel.profile?.profilePictureUrl?.takeIf { it.isNotBlank() }
                        ?: ""
                    
                    viewModel.createOrUpdateProfile(context as Activity, nicknameInput, finalPictureUrl)
                }

            },

            enabled = !viewModel.isLoading && !viewModel.isUploadingImage && nicknameInput.isNotBlank(),

            modifier = Modifier.fillMaxWidth().height(50.dp)

        ) {

            if (viewModel.isLoading) {

                CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)

            } else {

                Text(if (viewModel.isEditingProfile) "Enregistrer les modifications" else "Enregistrer et Continuer")

            }

        }

    }

}



@Composable

fun ProfileCompleteScreen(viewModel: AuthViewModel) {

    val profile = viewModel.profile ?: return

    val context = LocalContext.current



    Column(

        modifier = Modifier

            .fillMaxSize()

            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center

    ) {

        Text(

            "✅ Profil configuré!",

            style = MaterialTheme.typography.headlineLarge,

            fontWeight = FontWeight.Bold,

            color = MaterialTheme.colorScheme.primary

        )

        Spacer(modifier = Modifier.height(16.dp))


        Text(

            "Bienvenue, ${profile.nickname}!",

            style = MaterialTheme.typography.headlineMedium

        )

        Spacer(modifier = Modifier.height(8.dp))


        Text(

            "Votre profil est prêt. Vous pouvez maintenant jouer!",

            style = MaterialTheme.typography.bodyLarge,

            color = MaterialTheme.colorScheme.onSurfaceVariant

        )

        Spacer(modifier = Modifier.height(48.dp))



// --- Edit Profile Button ---

        Button(

            onClick = {

                viewModel.isEditingProfile = true

            },

            modifier = Modifier.fillMaxWidth()

        ) {

            Text("Modifier le profil")

        }

        Spacer(modifier = Modifier.height(16.dp))



// --- Log Out Button ---

        OutlinedButton(

            onClick = {

                viewModel.signOut(context as Activity)

                Toast.makeText(context, "Déconnexion réussie", Toast.LENGTH_SHORT).show()

            },

            modifier = Modifier.fillMaxWidth()

        ) {

            Text("Déconnexion")

        }

    }

}