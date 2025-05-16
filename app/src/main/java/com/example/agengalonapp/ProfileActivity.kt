package com.example.agengalonapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private lateinit var editNama: EditText
    private lateinit var editEmail: EditText
    private lateinit var editAlamat: EditText
    private lateinit var buttonUpdate: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Bind views
        editNama = findViewById(R.id.editName)
        editEmail = findViewById(R.id.editEmail)
        editAlamat = findViewById(R.id.editAddress)
        buttonUpdate = findViewById(R.id.buttonUpdate)

        val uid = auth.currentUser?.uid ?: return

        // Load user data
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    editNama.setText(doc.getString("nama") ?: "")
                    editEmail.setText(doc.getString("email") ?: "")
                    editAlamat.setText(doc.getString("alamat") ?: "")
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
            }

        // Update user data
        buttonUpdate.setOnClickListener {
            val nama = editNama.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val alamat = editAlamat.text.toString().trim()

            if (nama.isEmpty() || alamat.isEmpty()) {
                Toast.makeText(this, "Nama dan Alamat tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updateMap = mapOf(
                "nama" to nama,
                "email" to email,  // email tidak bisa diedit dari sini, tapi tetap ditampilkan
                "alamat" to alamat
            )

            db.collection("users").document(uid).update(updateMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Data berhasil diupdate", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Bottom Navigation
        findViewById<LinearLayout>(R.id.navDashboard).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        findViewById<LinearLayout>(R.id.navTransaction).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            Toast.makeText(this, "Already on Profile", Toast.LENGTH_SHORT).show()
        }
    }
}
