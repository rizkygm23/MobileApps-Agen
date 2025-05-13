package com.example.agengalonapp

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var textAir: TextView
    private lateinit var textGalonKosong: TextView
    private lateinit var textGalonIsi: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // 🔥 Pake XML

        // Inisialisasi TextView
        textAir = findViewById(R.id.textAir)
        textGalonKosong = findViewById(R.id.textGalonKosong)
        textGalonIsi = findViewById(R.id.textGalonIsi)

        getStockData()
    }

    private fun getStockData() {
        val db = FirebaseFirestore.getInstance()
        val stockRef = db.collection("stocks").document("currentStock")

        stockRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val airLiter = document.getLong("airLiter") ?: 0
                    val galonKosong = document.getLong("galonKosong") ?: 0
                    val galonIsi = document.getLong("galonIsi") ?: 0

                    // Update UI
                    textAir.text = "Stok Air: $airLiter Liter"
                    textGalonKosong.text = "Galon Kosong: $galonKosong"
                    textGalonIsi.text = "Galon Isi: $galonIsi"
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Gagal ambil data: ${e.message}")
            }
    }
}
