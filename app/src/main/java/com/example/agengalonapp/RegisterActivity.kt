package com.example.agengalonapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var editName: EditText
    private lateinit var editAddress: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var editConfirmPassword: EditText
    private lateinit var buttonRegister: Button
    private lateinit var textBackToLogin: TextView
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        editName = findViewById(R.id.editName)
        editAddress = findViewById(R.id.editAddress)
        editEmail = findViewById(R.id.editEmailRegister)
        editPassword = findViewById(R.id.editPasswordRegister)
        editConfirmPassword = findViewById(R.id.editConfirmPassword)
        buttonRegister = findViewById(R.id.buttonRegister)
        textBackToLogin = findViewById(R.id.textBackToLogin)

        buttonRegister.setOnClickListener {
            val name = editName.text.toString().trim()
            val address = editAddress.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()
            val confirmPassword = editConfirmPassword.text.toString().trim()

            if (name.isEmpty() || address.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Isi semua field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Password tidak cocok", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        val user = hashMapOf(
                            "name" to name,
                            "address" to address,
                            "email" to email
                        )
                        userId?.let {
                            db.collection("users").document(it).set(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Register berhasil!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Gagal simpan data: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Gagal daftar: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        textBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
