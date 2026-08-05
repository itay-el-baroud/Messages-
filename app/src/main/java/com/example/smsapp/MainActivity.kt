package com.example.smsapp

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ConversationAdapter
    private val messages = mutableListOf<SmsItem>()

    private val requiredPermissions = arrayOf(
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.READ_CONTACTS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ConversationAdapter(messages)
        recyclerView.adapter = adapter

        findViewById<android.widget.Button>(R.id.btnDefaultApp).setOnClickListener {
            requestDefaultSmsApp()
        }

        findViewById<FloatingActionButton>(R.id.fabCompose).setOnClickListener {
            showComposeDialog()
        }

        ActivityCompat.requestPermissions(this, requiredPermissions, 100)
        loadMessages()
    }

    private fun requestDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS))
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            startActivity(intent)
        }
    }

    private fun loadMessages() {
        messages.clear()
        val uri = Uri.parse("content://sms/")
        val cursor = contentResolver.query(uri, null, null, null, "date DESC")
        cursor?.use {
            val addressIndex = it.getColumnIndex("address")
            val bodyIndex = it.getColumnIndex("body")
            while (it.moveToNext()) {
                val address = if (addressIndex >= 0) it.getString(addressIndex) ?: "" else ""
                val body = if (bodyIndex >= 0) it.getString(bodyIndex) ?: "" else ""
                messages.add(SmsItem(address, body))
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun showComposeDialog() {
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 20)

        val phoneInput = EditText(this)
        phoneInput.hint = "رقم الهاتف"
        val messageInput = EditText(this)
        messageInput.hint = "الرسالة"

        layout.addView(phoneInput)
        layout.addView(messageInput)

        AlertDialog.Builder(this)
            .setTitle("رسالة جديدة")
            .setView(layout)
            .setPositiveButton("إرسال") { _, _ ->
                sendSms(phoneInput.text.toString(), messageInput.text.toString())
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun sendSms(phone: String, message: String) {
        if (phone.isBlank() || message.isBlank()) {
            Toast.makeText(this, "من فضلك أدخل الرقم والرسالة", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val smsManager = getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phone, null, message, null, null)
            Toast.makeText(this, "تم الإرسال", Toast.LENGTH_SHORT).show()
            loadMessages()
        } catch (e: Exception) {
            Toast.makeText(this, "فشل الإرسال: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadMessages()
    }
}

data class SmsItem(val address: String, val body: String)
