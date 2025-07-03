package com.example.agengalonapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private val transactions = mutableListOf<TransactionItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        recyclerView = findViewById(R.id.recyclerHistory)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(transactions)
        recyclerView.adapter = adapter

        fetchTransactions()

        // Bottom Navigation
        findViewById<LinearLayout>(R.id.navDashboard).setOnClickListener {
            Toast.makeText(this, "Go to Dashboard", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, DashboardActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navTransaction).setOnClickListener {
            Toast.makeText(this, "Go to Transaction", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, TransactionActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            Toast.makeText(this, "Already on History", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.navDashboard).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        findViewById<LinearLayout>(R.id.navTransaction).setOnClickListener {
//            startActivity(Intent(this, TransactionActivity::class.java))
//            overridePendingTransition(0, 0)
//            finish()
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            Toast.makeText(this, "Go to Profile", Toast.LENGTH_SHORT).show()

             startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private fun fetchTransactions() {
        val db = FirebaseFirestore.getInstance()
        db.collection("transactions")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                transactions.clear()
                for (doc in result) {
                    val id = doc.id
                    val date = doc.getTimestamp("date")?.toDate() ?: Date()
                    val waterOnlyQty = doc.getLong("waterOnlyQty") ?: 0
                    val galonOnlyQty = doc.getLong("galonOnlyQty") ?: 0
                    val waterGalonQty = doc.getLong("waterGalonQty") ?: 0
                    val subtotal = doc.getLong("subtotal")?.toInt() ?: 0

                    transactions.add(
                        TransactionItem(
                            id = id,
                            date = date,
                            waterOnlyQty = waterOnlyQty.toInt(),
                            galonOnlyQty = galonOnlyQty.toInt(),
                            waterGalonQty = waterGalonQty.toInt(),
                            subtotal = subtotal
                        )
                    )

                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error loading transactions", e)
            }
    }
}

// Data class
data class TransactionItem(
    val id: String,
    val date: Date,
    val waterOnlyQty: Int,
    val galonOnlyQty: Int,
    val waterGalonQty: Int,
    val subtotal: Int
)

// Adapter
class HistoryAdapter(private val items: List<TransactionItem>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transactions_history, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textID: TextView = view.findViewById(R.id.textTransactionId)
        private val textDate: TextView = view.findViewById(R.id.textTransactionDate)
        private val textDetails: TextView = view.findViewById(R.id.textTransactionDetails)
        private val textSubtotal: TextView = view.findViewById(R.id.textTransactionTotal)

        fun bind(item: TransactionItem) {
            val formatter = SimpleDateFormat("MMM d, yyyy - HH:mm", Locale.getDefault())
            textDate.text = formatter.format(item.date)
            textID.text = "#trx-${item.id.takeLast(6)}"

            val details = StringBuilder()
            if (item.waterOnlyQty > 0) details.append("Water Refill: ${item.waterOnlyQty}x\n")
            if (item.galonOnlyQty > 0) details.append("Empty Galon: ${item.galonOnlyQty}x\n")
            if (item.waterGalonQty > 0) details.append("Filled Galon: ${item.waterGalonQty}x")

            textDetails.text = details.toString().trim()
            textSubtotal.text = "Rp ${item.subtotal}" // format as needed
        }


    }
    // Bottom Navigation




}
