package com.example.verhuur_app

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.verhuur_app.ui.theme.VerhuurappTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setContent {
            VerhuurappTheme {
                // A surface container using the 'background' color from the theme
                AuthenticationUI(auth = auth, context = this)
            }
        }
    }
}
private fun registerUser(auth: FirebaseAuth, email: String, password: String, context: Context) {
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Registration successful", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
}

// Functie om gebruiker in te loggen
private fun loginUser(auth: FirebaseAuth, email: String, password: String, context: Context) {
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationUI(auth: FirebaseAuth, context: Context) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLoginMode) "Login" else "Register",
            style = MaterialTheme.typography.titleLarge  // Gebruik een stabiele TextStyle
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isLoginMode) {
                    loginUser(auth, email, password, context)  // Context meegeven
                } else {
                    registerUser(auth, email, password, context)  // Context meegeven
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (isLoginMode) "Login" else "Register")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { isLoginMode = !isLoginMode },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = if (isLoginMode) "Don't have an account? Register" else "Already have an account? Login"
            )
        }
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
            text = "Hello $name!",
            modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VerhuurappTheme {
        Greeting("Android")
    }
}