package com.example.agengalonapp

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {
    private lateinit var editNama: EditText
    private lateinit var editEmail: EditText
    private lateinit var editAlamat: EditText
    private lateinit var buttonUpdate: Button
    private lateinit var buttonEod: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        calculateTotalAssets()


        editNama = findViewById(R.id.editName)
        editEmail = findViewById(R.id.editEmail)
        editAlamat = findViewById(R.id.editAddress)
        buttonUpdate = findViewById(R.id.buttonUpdate)
        buttonEod = findViewById(R.id.buttonEOD)
        val textTotalAsset = findViewById<TextView>(R.id.textTotalAssetValue)
        val textAvgTransaction = findViewById<TextView>(R.id.textAvgTransactionValue)

        val uid = auth.currentUser?.uid ?: return

        val transaksiRef = db.collection("transactions")
        transaksiRef.get().addOnSuccessListener { result ->
            var total = 0
            for (doc in result) {
                val subtotal = doc.getLong("subtotal") ?: 0
                total += subtotal.toInt()
            }

            val avg = if (result.size() > 0) total / result.size() else 0

//            textTotalAsset.text = "Total Asset Toko: Rp $total"
            textAvgTransaction.text = "${formatRupiah(avg.toLong())} "
        }.addOnFailureListener {
            textTotalAsset.text = "--"
            textAvgTransaction.text = "--"
        }


        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    editNama.setText(doc.getString("name") ?: "")
                    editEmail.setText(doc.getString("email") ?: "")
                    editAlamat.setText(doc.getString("address") ?: "")
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
            }

        buttonUpdate.setOnClickListener {
            val name = editNama.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val address = editAlamat.text.toString().trim()

            if (name.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Nama dan Alamat tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updateMap = mapOf(
                "name" to name,
                "email" to email,
                "address" to address
            )

            db.collection("users").document(uid).update(updateMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Data berhasil diupdate", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        findViewById<Button>(R.id.buttonLogout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        buttonEod.setOnClickListener {
            exportEODReport()
        }

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
            overridePendingTransition(0, 0)
        }
    }

    private fun exportEODReport() {
        val db = FirebaseFirestore.getInstance()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.time

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.time

        db.collection("transactions")
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThanOrEqualTo("date", endOfDay)
            .get()
            .addOnSuccessListener { result ->
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("EOD Report")
                val header = sheet.createRow(0)
                header.createCell(0).setCellValue("Date")
                header.createCell(1).setCellValue("Water Only Qty")
                header.createCell(2).setCellValue("Galon Only Qty")
                header.createCell(3).setCellValue("Water + Galon Qty")
                header.createCell(4).setCellValue("Subtotal")

                var total = 0
                result.forEachIndexed { index, doc ->
                    val row = sheet.createRow(index + 1)
                    val date = doc.getDate("date") ?: Date()
                    val waterOnly = doc.getLong("waterOnlyQty") ?: 0
                    val galonOnly = doc.getLong("galonOnlyQty") ?: 0
                    val waterGalon = doc.getLong("waterGalonQty") ?: 0
                    val subtotal = doc.getLong("subtotal") ?: 0

                    row.createCell(0).setCellValue(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date))
                    row.createCell(1).setCellValue(waterOnly.toDouble())
                    row.createCell(2).setCellValue(galonOnly.toDouble())
                    row.createCell(3).setCellValue(waterGalon.toDouble())
                    row.createCell(4).setCellValue(subtotal.toDouble())

                    total += subtotal.toInt()
                }

                val totalRow = sheet.createRow(result.size() + 2)
                totalRow.createCell(3).setCellValue("Total")
                totalRow.createCell(4).setCellValue(total.toDouble())

                val fileName = "eod_report_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.xlsx"
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloads, fileName)

                try {
                    FileOutputStream(file).use {
                        workbook.write(it)
                        workbook.close()
                    }
                    Toast.makeText(this, "Saved to Downloads/$fileName", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal load transaksi", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateTotalAssets() {
        val db = FirebaseFirestore.getInstance()
        val stockRef = db.collection("stocks").document("currentStock")

        stockRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val waterStock = doc.getLong("waterStock") ?: 0
                val emptyGalons = doc.getLong("emptyGalons") ?: 0
                val filledGalons = doc.getLong("filledGalons") ?: 0

                val priceWaterOnly = doc.getLong("priceWaterOnly") ?: 0
                val priceGalonOnly = doc.getLong("priceGalonOnly") ?: 0
                val priceWaterGalon = doc.getLong("priceWaterGalon") ?: 0

                val nilaiAir = waterStock * priceWaterOnly
                val nilaiGalonKosong = emptyGalons * priceGalonOnly
                val nilaiGalonIsi = filledGalons * priceWaterGalon

                val totalAsset = nilaiAir + nilaiGalonKosong + nilaiGalonIsi
                val textTotalAsset = findViewById<TextView>(R.id.textTotalAssetValue)
                textTotalAsset.text = "${formatRupiah(totalAsset)}"
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal ambil data stok", Toast.LENGTH_SHORT).show()
        }
    }
    private fun formatRupiah(number: Long): String {
        return String.format("%,d", number).replace(',', '.')
    }

}
