package com.example.callsilencer

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.ContactsContract
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private val contacts = mutableListOf<ContactItem>()
    private lateinit var adapter: ContactAdapter
    private var minutes = 30L
    private var countDownTimer: CountDownTimer? = null

    private lateinit var statusText: TextView
    private lateinit var countdownText: TextView
    private lateinit var customMinutes: EditText
    private lateinit var startButton: Button
    private lateinit var addContactButton: Button
    private lateinit var settingsButton: Button
    private lateinit var contactList: RecyclerView

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("CallSilencerPrefs", Context.MODE_PRIVATE)

        // Initialize views
        statusText = findViewById(R.id.statusText)
        countdownText = findViewById(R.id.countdownText)
        customMinutes = findViewById(R.id.customMinutes)
        startButton = findViewById(R.id.startButton)
        addContactButton = findViewById(R.id.addContactButton)
        settingsButton = findViewById(R.id.settingsButton)
        contactList = findViewById(R.id.contactList)

        // Load saved contacts
        loadContacts()

        // Setup RecyclerView with delete support
        adapter = ContactAdapter(contacts) { position ->
            contacts.removeAt(position)
            saveContacts()
            adapter.notifyItemRemoved(position)
            Toast.makeText(this, "Contact removed", Toast.LENGTH_SHORT).show()
        }

        contactList.layoutManager = LinearLayoutManager(this)
        contactList.adapter = adapter

        // Timer buttons
        findViewById<Button>(R.id.btn15).setOnClickListener { minutes = 15 }
        findViewById<Button>(R.id.btn30).setOnClickListener { minutes = 30 }
        findViewById<Button>(R.id.btn60).setOnClickListener { minutes = 60 }

        // Start Silent Mode
        startButton.setOnClickListener {
            val custom = customMinutes.text.toString().trim()
            if (custom.isNotEmpty()) {
                minutes = custom.toLongOrNull() ?: 30L
            }

            if (!isDndAccessGranted()) {
                requestDndPermission()
                return@setOnClickListener
            }

            val intent = Intent(this, SilentService::class.java).apply {
                putExtra("minutes", minutes)
            }
            startService(intent)

            startCountdown(minutes * 60 * 1000)
            updateStatus(true)
        }

        // Add Contact
        addContactButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            startActivityForResult(intent, 1)
        }

        // Settings
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        updateStatus(false)
    }

    private fun isDndAccessGranted(): Boolean {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    private fun requestDndPermission() {
        Toast.makeText(this, "Please grant Do Not Disturb access", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun updateStatus(isActive: Boolean) {
        if (isActive) {
            statusText.text = "✅ Silent Mode: ACTIVE"
            statusText.setTextColor(0xFF4CAF50.toInt())
            countdownText.visibility = View.VISIBLE
        } else {
            statusText.text = "Silent Mode: OFF"
            statusText.setTextColor(0xFF757575.toInt())
            countdownText.visibility = View.GONE
            countdownText.text = "Time remaining: --"
        }
    }

    private fun startCountdown(totalMillis: Long) {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minLeft = millisUntilFinished / 1000 / 60
                val secLeft = (millisUntilFinished / 1000) % 60
                countdownText.text = "Time remaining: ${minLeft}m ${secLeft}s"
            }

            override fun onFinish() {
                countdownText.text = "Time remaining: --"
                updateStatus(false)
                Toast.makeText(this@MainActivity, "Silent mode ended", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    // ==================== Persistent Storage ====================
    private fun loadContacts() {
        val json = sharedPreferences.getString("contacts", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<ContactItem>>() {}.type
            val savedContacts: MutableList<ContactItem> = gson.fromJson(json, type)
            contacts.clear()
            contacts.addAll(savedContacts)
        }
    }

    private fun saveContacts() {
        val json = gson.toJson(contacts)
        sharedPreferences.edit().putString("contacts", json).apply()
    }

    // ==================== Add Contact ====================
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            val cursor = contentResolver.query(data.data!!, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val phone = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))

                    val newContact = ContactItem(
                        name = name ?: "Unknown Contact",
                        phone = phone ?: "No number available"
                    )

                    contacts.add(newContact)
                    saveContacts()
                    adapter.notifyDataSetChanged()

                    Toast.makeText(this, "Added: $name", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}