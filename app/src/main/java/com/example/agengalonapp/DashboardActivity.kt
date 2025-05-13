package com.example.agengalonapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.util.Calendar


class DashboardActivity : AppCompatActivity() {
    private lateinit var textEmptyGalon: TextView
    private lateinit var textWaterStock: TextView
    private lateinit var textFilledGalon: TextView
    private lateinit var textSales: TextView
    private lateinit var textTransactions: TextView


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        textSales = findViewById(R.id.textSales)
        textTransactions = findViewById(R.id.textTransactions)
        textEmptyGalon = findViewById(R.id.textEmptyGalon)
        textWaterStock = findViewById(R.id.textWaterStock)
        textFilledGalon = findViewById(R.id.textFilledGalon)
        val db = FirebaseFirestore.getInstance()
        val stockRef = db.collection("stocks").document("currentStock")

        val buttonNewTransaction = findViewById<LinearLayout>(R.id.buttonNewTransaction)
        val buttonAddStock = findViewById<LinearLayout>(R.id.buttonAddStock)
        stockRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("Firestore", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val emptyGalons = snapshot.getLong("emptyGalons") ?: 0
                val waterStock = snapshot.getLong("waterStock") ?: 0
                val filledGalons = snapshot.getLong("filledGalons") ?: 0

                textEmptyGalon.text = "Empty Galons: $emptyGalons units"
                textWaterStock.text = "Water Stock: $waterStock L"
                textFilledGalon.text = "Filled Galons: $filledGalons units"
            } else {
                Log.d("Firestore", "Current data: null")
            }
        }
//        val db = FirebaseFirestore.getInstance()
        val transaksiRef = db.collection("transactions")
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = Timestamp(calendar.time)

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = Timestamp(calendar.time)

        transaksiRef
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThanOrEqualTo("date", endOfDay)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Firestore", "Listen failed: ", e)
                    return@addSnapshotListener
                }

                var totalSales = 0
                val totalTransactions = snapshot?.size() ?: 0

                snapshot?.forEach { doc ->
                    val subtotal = doc.getLong("subtotal") ?: 0
                    totalSales += subtotal.toInt()
                }

                textSales.text = "Sales: Rp $totalSales"
                textTransactions.text = "Transactions: $totalTransactions"
            }
        fun addStock(emptyGalonsToAdd: Int, waterStockToAdd: Int, filledGalonsToAdd: Int) {
            val db = FirebaseFirestore.getInstance()
            val stockRef = db.collection("stocks").document("currentStock")

            db.runTransaction { transaction ->
                val snapshot = transaction.get(stockRef)

                val currentEmptyGalons = snapshot.getLong("emptyGalons") ?: 0
                val currentWaterStock = snapshot.getLong("waterStock") ?: 0
                val currentFilledGalons = snapshot.getLong("filledGalons") ?: 0

                transaction.update(stockRef, mapOf(
                    "emptyGalons" to (currentEmptyGalons + emptyGalonsToAdd),
                    "waterStock" to (currentWaterStock + waterStockToAdd),
                    "filledGalons" to (currentFilledGalons + filledGalonsToAdd)
                ))
            }.addOnSuccessListener {
                Toast.makeText(this, "Stok berhasil ditambahkan", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Gagal update stok: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        fun recordTransaction(type: String, qty: Int, totalPrice: Int) {
            val db = FirebaseFirestore.getInstance()
            val transactionData = hashMapOf(
                "date" to Timestamp.now(),
                "type" to type,
                "quantity" to qty,
                "totalPrice" to totalPrice
            )

            db.collection("transactions")
                .add(transactionData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Transaksi dicatat!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal catat transaksi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }



        // Dummy data sementara
//        textEmptyGalon.text = "Empty Galons: 45 units"
//        textWaterStock.text = "Water Stock: 855 L"
//        textFilledGalon.text = "Filled Galons: 78 units"

        buttonNewTransaction.setOnClickListener {
            val intent = Intent(this, TransactionActivity::class.java)
            startActivity(intent)
        }

        buttonAddStock.setOnClickListener {
            Toast.makeText(this, "Go to Add Stock", Toast.LENGTH_SHORT).show()
            // TODO: Intent ke AddStockActivity
            val intent = Intent(this, AddStockActivity::class.java)
            overridePendingTransition(0, 0)
            startActivity(intent)
        }

        // Bottom Navigation
        findViewById<LinearLayout>(R.id.navDashboard).setOnClickListener {
            Toast.makeText(this, "Already on Dashboard", Toast.LENGTH_SHORT).show()
            overridePendingTransition(0, 0)
            finish()
        }

        findViewById<LinearLayout>(R.id.navTransaction).setOnClickListener {
            Toast.makeText(this, "Go to Transaction", Toast.LENGTH_SHORT).show()
             startActivity(Intent(this, HistoryActivity::class.java))
            overridePendingTransition(0, 0)
            finish()




        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            Toast.makeText(this, "Go to Profile", Toast.LENGTH_SHORT).show()
            overridePendingTransition(0, 0)
            // startActivity(Intent(this, ProfileActivity::class.java))
        }

    }


}
