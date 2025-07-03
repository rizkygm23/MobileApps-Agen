
package com.example.agengalonapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class TransactionActivity : AppCompatActivity() {
    private var priceWater = 0
    private var priceGalon = 0
    private var priceWaterGalon = 0
    private var subtotal = 0

    private lateinit var editQtyWater: EditText
    private lateinit var editQtyGalon: EditText
    private lateinit var editQtyWaterGalon: EditText
    private lateinit var textSubtotal: TextView
    private lateinit var editCash: EditText
    private lateinit var textChange: TextView
    private lateinit var buttonComplete: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)

        editQtyWater = findViewById(R.id.editQtyWater)
        editQtyGalon = findViewById(R.id.editQtyGalon)
        editQtyWaterGalon = findViewById(R.id.editQtyWaterGalon)
        textSubtotal = findViewById(R.id.textSubtotal)
        editCash = findViewById(R.id.editCash)
        textChange = findViewById(R.id.textChange)
        buttonComplete = findViewById(R.id.buttonCompleteTransaction)

        val btnMap = mapOf(
            R.id.btn2000 to 2000,
            R.id.btn5000 to 5000,
            R.id.btn10000 to 10000,
            R.id.btn20000 to 20000,
            R.id.btn50000 to 50000,
            R.id.btn100000 to 100000
        )

        for ((id, value) in btnMap) {
            findViewById<Button>(id).setOnClickListener {
                val current = editCash.text.toString().toIntOrNull() ?: 0
                editCash.setText((current + value).toString())
            }
        }

        setupQtyButtons(R.id.btnMinusWater, R.id.btnPlusWater, editQtyWater)
        setupQtyButtons(R.id.btnMinusGalon, R.id.btnPlusGalon, editQtyGalon)
        setupQtyButtons(R.id.btnMinusWaterGalon, R.id.btnPlusWaterGalon, editQtyWaterGalon)

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = calculateSubtotal()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        editQtyWater.addTextChangedListener(watcher)
        editQtyGalon.addTextChangedListener(watcher)
        editQtyWaterGalon.addTextChangedListener(watcher)
        editCash.addTextChangedListener(watcher)

        FirebaseFirestore.getInstance().collection("stocks")
            .document("currentStock")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    priceWater = (snapshot.getLong("priceWaterOnly") ?: 0).toInt()
                    priceGalon = (snapshot.getLong("priceGalonOnly") ?: 0).toInt()
                    priceWaterGalon = (snapshot.getLong("priceWaterGalon") ?: 0).toInt()
                    calculateSubtotal()
                }
            }

        buttonComplete.setOnClickListener {
            val intent = Intent(this, SuccessActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
            val qtyWater = editQtyWater.text.toString().toIntOrNull() ?: 0
            val qtyGalon = editQtyGalon.text.toString().toIntOrNull() ?: 0
            val qtyWaterGalon = editQtyWaterGalon.text.toString().toIntOrNull() ?: 0
            updateStockAndRecordTransaction(qtyWater, qtyGalon, qtyWaterGalon)
        }
    }

    private fun setupQtyButtons(minusId: Int, plusId: Int, editText: EditText) {
        findViewById<Button>(minusId).setOnClickListener {
            val qty = (editText.text.toString().toIntOrNull() ?: 0).coerceAtLeast(1) - 1
            editText.setText(qty.toString())
        }
        findViewById<Button>(plusId).setOnClickListener {
            val qty = (editText.text.toString().toIntOrNull() ?: 0) + 1
            editText.setText(qty.toString())
        }
    }

    private fun calculateSubtotal() {
        val qtyWater = editQtyWater.text.toString().toIntOrNull() ?: 0
        val qtyGalon = editQtyGalon.text.toString().toIntOrNull() ?: 0
        val qtyWaterGalon = editQtyWaterGalon.text.toString().toIntOrNull() ?: 0
        subtotal = (qtyWater * priceWater) + (qtyGalon * priceGalon) + (qtyWaterGalon * priceWaterGalon)
        textSubtotal.text = "Subtotal: Rp $subtotal"
        val cash = editCash.text.toString().toIntOrNull() ?: 0
        val change = (cash - subtotal).coerceAtLeast(0)
        textChange.text = "Change: Rp $change"
    }

    private fun updateStockAndRecordTransaction(qtyWater: Int, qtyGalon: Int, qtyWaterGalon: Int) {
        val db = FirebaseFirestore.getInstance()
        val stockRef = db.collection("stocks").document("currentStock")
        db.runTransaction { transaction ->
            val snapshot = transaction.get(stockRef)
            val currentEmptyGalons = snapshot.getLong("emptyGalons") ?: 0
            val currentWaterStock = snapshot.getLong("waterStock") ?: 0
            val totalGalonsToReduce = qtyGalon + qtyWaterGalon
            val totalWaterToReduce = (qtyWater + qtyWaterGalon) * 19
            transaction.update(stockRef, mapOf(
                "emptyGalons" to (currentEmptyGalons - totalGalonsToReduce),
                "waterStock" to (currentWaterStock - totalWaterToReduce)
            ))
        }.addOnSuccessListener {
            recordTransaction(qtyWater, qtyGalon, qtyWaterGalon)
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
        db.collection("transactions").add(transaksi)
    }
}
