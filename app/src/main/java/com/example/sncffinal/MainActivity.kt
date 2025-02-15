package com.example.sncffinal

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var autoCompleteDepart: AutoCompleteTextView
    private lateinit var autoCompleteArrivee: AutoCompleteTextView
    private lateinit var btnRecherche: Button
    private lateinit var txtDate: TextView
    private lateinit var txtHeure: TextView
    private var isItemSelected = false
    private var isSwapping = false
    private var dateFormatForApp: String = ""  // Format yyyyMMdd
    private var timeFormatForApp: String = ""  // Format HHmm



    private val calendar = Calendar.getInstance()
    private val apiKey = "d3373c77-15ba-4af5-907a-76910079f1c6"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val videoView: VideoView = findViewById(R.id.videoBackground)
        val videoPath = "android.resource://" + packageName + "/" + R.raw.traintrain
        videoView.setVideoPath(videoPath)

        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true // 🔄 Met la vidéo en boucle
            mp.setVolume(0f, 0f) // 🔇 Coupe le son
            videoView.start()
        }

        // 🔥 Initialisation des vues
        autoCompleteDepart = findViewById(R.id.autoCompleteDepart)
        autoCompleteArrivee = findViewById(R.id.autoCompleteArrivee)
        btnRecherche = findViewById(R.id.btnRecherche)

        // Gestion des événements
        val btnSelectDate: MaterialButton = findViewById(R.id.btnSelectDate)
        val btnSelectTime: MaterialButton = findViewById(R.id.btnSelectTime)

        btnSelectDate.setOnClickListener { showDatePicker(btnSelectDate) }
        btnSelectTime.setOnClickListener { showTimePicker(btnSelectTime) }




        btnRecherche.setOnClickListener { rechercherTrains() }

        // Auto-complétion des villes
        setupAutoComplete(autoCompleteDepart)
        setupAutoComplete(autoCompleteArrivee)

        val btnSwap: ImageButton = findViewById(R.id.btnSwap)
        btnSwap.setOnClickListener {
            swapDepartArrivee()
        }

    }

    private fun setupAutoComplete(autoCompleteTextView: AutoCompleteTextView) {
        autoCompleteTextView.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                if (!isSwapping) { // Vérifie si on est en train de swapper
                    val searchText = s.toString().trim()
                    if (searchText.length >= 2 && !isItemSelected) {
                        fetchCities(autoCompleteTextView, searchText)
                    }
                }
                isItemSelected = false // Reset après modification du texte
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        autoCompleteTextView.setOnItemClickListener { _, _, _, _ ->
            isItemSelected = true
            autoCompleteTextView.dismissDropDown()
            //on ferme le clavier
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(autoCompleteTextView.windowToken, 0)
        }
    }


    private fun swapDepartArrivee() {
        isSwapping = true // Empêche l'ouverture de la liste déroulante

        val temp = autoCompleteDepart.text.toString()
        autoCompleteDepart.setText(autoCompleteArrivee.text.toString(), false) // false pour ne pas afficher la liste
        autoCompleteArrivee.setText(temp, false)

        isSwapping = false // Réactive l'auto-complétion après le swap
    }



    private fun fetchCities(autoCompleteTextView: AutoCompleteTextView, searchText: String) {
        val queue = Volley.newRequestQueue(this)
        val url = "https://api.sncf.com/v1/coverage/sncf/places?q=$searchText"

        val request = object : JsonObjectRequest(Request.Method.GET, url, null,
            JsonObjectRequest@{ response ->
                val places = response.optJSONArray("places") ?: return@JsonObjectRequest
                val cityList = mutableListOf<String>()

                for (i in 0 until places.length()) {
                    val place = places.getJSONObject(i)
                    if (place.optString("embedded_type") == "administrative_region") {
                        val cityName = place.optString("name", "Ville inconnue")
                        cityList.add(cityName)
                    }
                }

                autoCompleteTextView.setAdapter(
                    ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, cityList.distinct())
                )
                if (!isItemSelected) {
                    autoCompleteTextView.showDropDown()
                }            },
            { error -> Log.e("API_SNCF", "Erreur API : ${error.networkResponse?.statusCode} - ${error.message}") }) {
            override fun getHeaders() = hashMapOf("Authorization" to "Basic " + Base64.encodeToString("$apiKey:".toByteArray(), Base64.NO_WRAP))
        }
        queue.add(request)
    }

    private fun showDatePicker(button: MaterialButton) {
        val datePicker = DatePickerDialog(
            this, { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)

                // Stocke la date au format API (yyyyMMdd)
                dateFormatForApp = SimpleDateFormat("yyyyMMdd", Locale.FRANCE).format(calendar.time)

                // Affiche la date au format jj/MM/aa (ex: 17/02/24)
                button.text = SimpleDateFormat("dd/MM/yy", Locale.FRANCE).format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.minDate = System.currentTimeMillis()
        datePicker.show()
    }




    private fun showTimePicker(button: MaterialButton) {
        val timePicker = TimePickerDialog(
            this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)

                // Stocke l'heure au format API
                timeFormatForApp = SimpleDateFormat("HHmm", Locale.FRANCE).format(calendar.time)

                // Affiche une heure lisible sur le bouton
                button.text = SimpleDateFormat("HH:mm", Locale.FRANCE).format(calendar.time)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePicker.show()
    }



    private fun rechercherTrains() {
        val depart = autoCompleteDepart.text.toString().trim()
        val arrivee = autoCompleteArrivee.text.toString().trim()

        if (depart.isEmpty() || arrivee.isEmpty() || dateFormatForApp.isEmpty() || timeFormatForApp.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            return
        }

        fetchUICCode(depart) { departId ->
            fetchUICCode(arrivee) { arriveeId ->
                val intent = Intent(this, ResultActivity::class.java)
                intent.putExtra("departUIC", departId)
                intent.putExtra("arriveeUIC", arriveeId)

                // ✅ Passe les formats pour l'application à ResultActivity
                intent.putExtra("date", dateFormatForApp)   // yyyyMMdd
                intent.putExtra("heure", timeFormatForApp)  // HHmm

                startActivity(intent)
            }
        }
    }



    private fun fetchUICCode(ville: String, callback: (String?) -> Unit) {
        val queue = Volley.newRequestQueue(this)
        val url = "https://api.sncf.com/v1/coverage/sncf/places?q=$ville"

        val request = object : JsonObjectRequest(Request.Method.GET, url, null,
            JsonObjectRequest@{ response ->
                val places = response.optJSONArray("places") ?: return@JsonObjectRequest callback(null)
                for (i in 0 until places.length()) {
                    val place = places.getJSONObject(i)
                    val id = place.optString("id", "")
                    if (id.isNotEmpty()) return@JsonObjectRequest callback(id)
                }
                callback(null)
            },
            { error -> callback(null) }) {
            override fun getHeaders() = hashMapOf("Authorization" to "Basic " + Base64.encodeToString("$apiKey:".toByteArray(), Base64.NO_WRAP))
        }
        queue.add(request)
    }
}
