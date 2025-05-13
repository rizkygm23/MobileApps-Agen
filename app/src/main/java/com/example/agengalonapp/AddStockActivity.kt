package com.example.agengalonapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AddStockActivity : AppCompatActivity() {

    private lateinit var textEmptyGalonsStock: TextView
    private lateinit var textWaterStock: TextView
    private lateinit var textFilledGalonsStock: TextView
    private lateinit var editAddEmptyGalons: EditText
    private lateinit var editAddWaterStock: EditText
    private lateinit var editAddFilledGalons: EditText
    private lateinit var editPriceWater: EditText
    private lateinit var editPriceGalon: EditText
    private lateinit var editPriceWaterGalon: EditText
    private lateinit var editNotes: EditText
    private lateinit var buttonSaveChanges: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_stock)

        textEmptyGalonsStock = findViewById(R.id.textEmptyGalonsStock)
        textWaterStock = findViewById(R.id.textWaterStock)
        textFilledGalonsStock = findViewById(R.id.textFilledGalonsStock)
        editAddEmptyGalons = findViewById(R.id.editAddEmptyGalons)
        editAddWaterStock = findViewById(R.id.editAddWaterStock)
        editAddFilledGalons = findViewById(R.id.editAddFilledGalons)
        editPriceWater = findViewById(R.id.editPriceWater)
        editPriceGalon = findViewById(R.id.editPriceGalon)
        editPriceWaterGalon = findViewById(R.id.editPriceWaterGalon)
        editNotes = findViewById(R.id.editNotes)
        buttonSaveChanges = findViewById(R.id.buttonSaveChanges)

        val db = FirebaseFirestore.getInstance()
        val stockRef = db.collection("stocks").document("currentStock")

        // Load current stock & prices
        stockRef.get().addOnSuccessListener { snapshot ->
            if (snapshot != null && snapshot.exists()) {
                val emptyGalons = snapshot.getLong("emptyGalons") ?: 0
                val waterStock = snapshot.getLong("waterStock") ?: 0
                val filledGalons = snapshot.getLong("filledGalons") ?: 0
                val priceWater = snapshot.getLong("priceWaterOnly") ?: 0
                val priceGalon = snapshot.getLong("priceGalonOnly") ?: 0
                val priceWaterGalon = snapshot.getLong("priceWaterGalon") ?: 0

                textEmptyGalonsStock.text = "Current Stock: $emptyGalons units"
                textWaterStock.text = "Current Stock: $waterStock L"
                textFilledGalonsStock.text = "Current Stock: $filledGalons units"

                editPriceWater.setText(priceWater.toString())
                editPriceGalon.setText(priceGalon.toString())
                editPriceWaterGalon.setText(priceWaterGalon.toString())
            }
        }

        buttonSaveChanges.setOnClickListener {
            val addEmptyGalons = editAddEmptyGalons.text.toString().toIntOrNull() ?: 0
            val addWaterStock = editAddWaterStock.text.toString().toIntOrNull() ?: 0
            val addFilledGalons = editAddFilledGalons.text.toString().toIntOrNull() ?: 0
            val newPriceWater = editPriceWater.text.toString().toLongOrNull()
            val newPriceGalon = editPriceGalon.text.toString().toLongOrNull()
            val newPriceWaterGalon = editPriceWaterGalon.text.toString().toLongOrNull()

            db.runTransaction { transaction ->
                val snapshot = transaction.get(stockRef)

                val currentEmptyGalons = snapshot.getLong("emptyGalons") ?: 0
                val currentWaterStock = snapshot.getLong("waterStock") ?: 0
                val currentFilledGalons = snapshot.getLong("filledGalons") ?: 0

                val newEmptyGalons = (currentEmptyGalons + addEmptyGalons) - (addFilledGalons)
                val newWaterStock = (currentWaterStock + addWaterStock) - (addFilledGalons * 19)
                val newFilledGalons = currentFilledGalons + addFilledGalons

                val updatedData = mutableMapOf<String, Any>(
                    "emptyGalons" to newEmptyGalons.coerceAtLeast(0), // biar ga minus
                    "waterStock" to newWaterStock.coerceAtLeast(0),
                    "filledGalons" to newFilledGalons
                )

                // Update prices if not null
                if (newPriceWater != null) updatedData["priceWaterOnly"] = newPriceWater
                if (newPriceGalon != null) updatedData["priceGalonOnly"] = newPriceGalon
                if (newPriceWaterGalon != null) updatedData["priceWaterGalon"] = newPriceWaterGalon

                transaction.update(stockRef, updatedData)

            }.addOnSuccessListener {
                Toast.makeText(this, "Stock & prices updated!", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
