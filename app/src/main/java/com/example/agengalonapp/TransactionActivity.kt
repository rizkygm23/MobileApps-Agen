package com.example.agengalonapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class TransactionActivity : AppCompatActivity() {
    private var priceWater = 0
    private var priceGalon = 0
    private var priceWaterGalon = 0


    private lateinit var editQtyWater: EditText
    private lateinit var editQtyGalon: EditText
    private lateinit var editQtyWaterGalon: EditText
    private lateinit var textSubtotal: TextView
    private lateinit var editCash: EditText
    private lateinit var textChange: TextView
    private lateinit var buttonComplete: Button
    private lateinit var textWaterLabel: TextView
    private lateinit var textGalonLabel: TextView
    private lateinit var textWaterGalonLabel: TextView

    private var subtotal = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)
        textWaterLabel = findViewById(R.id.textWaterLabel)
        textGalonLabel = findViewById(R.id.textGalonLabel)
        textWaterGalonLabel = findViewById(R.id.textWaterGalonLabel)
        editQtyWater = findViewById(R.id.editQtyWater)
        editQtyGalon = findViewById(R.id.editQtyGalon)
        editQtyWaterGalon = findViewById(R.id.editQtyWaterGalon)
        textSubtotal = findViewById(R.id.textSubtotal)
        editCash = findViewById(R.id.editCash)
        textChange = findViewById(R.id.textChange)
        buttonComplete = findViewById(R.id.buttonCompleteTransaction)

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = calculateSubtotal()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        val db = FirebaseFirestore.getInstance()
        val stockRef = db.collection("stocks").document("currentStock")

        stockRef.get().addOnSuccessListener { snapshot ->
            if (snapshot != null && snapshot.exists()) {
                priceWater = (snapshot.getLong("priceWaterOnly") ?: 0).toInt()
                priceGalon = (snapshot.getLong("priceGalonOnly") ?: 0).toInt()
                priceWaterGalon = (snapshot.getLong("priceWaterGalon") ?: 0).toInt()

                // ✅ Tambahkan kode ini untuk UPDATE TextView-nya:
                textWaterLabel.text = "Water Refill Only\nRp $priceWater / unit"
                textGalonLabel.text = "Galon Only\nRp $priceGalon / unit"
                textWaterGalonLabel.text = "Galon + Water\nRp $priceWaterGalon / unit"

                Log.d("Firestore", "Harga diambil: Water=$priceWater, Galon=$priceGalon, Water+Galon=$priceWaterGalon")
            }
        }

        editQtyWater.addTextChangedListener(watcher)
        editQtyGalon.addTextChangedListener(watcher)
        editQtyWaterGalon.addTextChangedListener(watcher)
        editCash.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val cash = editCash.text.toString().toIntOrNull() ?: 0
                val change = cash - subtotal
                textChange.text = "Change: Rp ${if (change < 0) 0 else change}"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        buttonComplete.setOnClickListener {
            val qtyWater = editQtyWater.text.toString().toIntOrNull() ?: 0
            val qtyGalon = editQtyGalon.text.toString().toIntOrNull() ?: 0
            val qtyWaterGalon = editQtyWaterGalon.text.toString().toIntOrNull() ?: 0

            updateStockAndRecordTransaction(qtyWater, qtyGalon, qtyWaterGalon)
        }
    }

    private fun calculateSubtotal() {
        val qtyWater = editQtyWater.text.toString().toIntOrNull() ?: 0
        val qtyGalon = editQtyGalon.text.toString().toIntOrNull() ?: 0
        val qtyWaterGalon = editQtyWaterGalon.text.toString().toIntOrNull() ?: 0

        subtotal = (qtyWater * priceWater) + (qtyGalon * priceGalon) + (qtyWaterGalon * priceWaterGalon)

        textSubtotal.text = "Subtotal: Rp $subtotal"
    }

    private fun updateStockAndRecordTransaction(qtyWater: Int, qtyGalon: Int, qtyWaterGalon: Int) {
        val db = FirebaseFirestore.getInstance()
        val stockRef = db.collection("stocks").document("currentStock")

        db.runTransaction { transaction ->
            val snapshot = transaction.get(stockRef)

            val currentEmptyGalons = snapshot.getLong("emptyGalons") ?: 0
            val currentWaterStock = snapshot.getLong("waterStock") ?: 0

            val totalGalonsToReduce = qtyGalon + qtyWaterGalon
            val totalWaterToReduce = (qtyWater * 19) + (qtyWaterGalon * 19)

            transaction.update(stockRef, mapOf(
                "emptyGalons" to (currentEmptyGalons - totalGalonsToReduce),
                "waterStock" to (currentWaterStock - totalWaterToReduce)
            ))
        }.addOnSuccessListener {
            Toast.makeText(this, "Stok terupdate!", Toast.LENGTH_SHORT).show()
            recordTransaction(qtyWater, qtyGalon, qtyWaterGalon)
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Gagal update stok: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun recordTransaction(qtyWater: Int, qtyGalon: Int, qtyWaterGalon: Int) {
        val db = FirebaseFirestore.getInstance()
        val transaksi = hashMapOf(
            "date" to Timestamp.now(),
            "waterOnlyQty" to qtyWater,
            "galonOnlyQty" to qtyGalon,
            "waterGalonQty" to qtyWaterGalon,
            "subtotal" to subtotal
        )

        db.collection("transactions")
            .add(transaksi)
            .addOnSuccessListener {
                Toast.makeText(this, "Transaksi dicatat!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal catat transaksi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
