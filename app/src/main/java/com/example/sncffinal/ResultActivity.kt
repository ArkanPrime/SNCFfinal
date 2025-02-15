package com.example.sncffinal


import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

data class TrainItem(
    val type: String,
    val departureStation: String,
    val arrivalStation: String,
    val departureTime: String,
    val arrivalTime: String,
    val duration: String,
    val isDirect: Boolean
)

class ResultActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val trainList = mutableListOf<TrainItem>()
    private val apiKey = "d3373c77-15ba-4af5-907a-76910079f1c6" // 🔥 Remplace par ta clé API SNCF

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        recyclerView = findViewById(R.id.recyclerViewTrains)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val departUIC = intent.getStringExtra("departUIC") ?: ""
        val arriveeUIC = intent.getStringExtra("arriveeUIC") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val heure = intent.getStringExtra("heure") ?: ""

        if (departUIC.isEmpty() || arriveeUIC.isEmpty() || date.isEmpty() || heure.isEmpty()) {
            Toast.makeText(this, "Données manquantes", Toast.LENGTH_SHORT).show()
            return
        }

        fetchTrains(departUIC, arriveeUIC, date, heure)
    }

    private fun fetchTrains(departUIC: String, arriveeUIC: String, date: String, heure: String) {
        val queue = Volley.newRequestQueue(this)
        val heureComplete = heure.padStart(4, '0') + "00" // Format HHMMSS
        val datetime = "${date}T$heureComplete"
        Log.d("API_SNCF", "🔍 Vérification datetime avant requête : $datetime")

        val baseUrl = "https://api.sncf.com/v1/coverage/sncf/journeys?from=$departUIC&to=$arriveeUIC&datetime=$datetime&count=10"

        val request = object : JsonObjectRequest(Request.Method.GET, baseUrl, null,
            { response ->
                Log.d("API_SNCF", "✅ Réponse reçue")
                parseTrainData(response)
            },
            { error ->
                Log.e("API_SNCF", "❌ Erreur API : ${error.networkResponse?.statusCode} - ${error.message}")
                Toast.makeText(this, "Erreur de récupération des trains", Toast.LENGTH_SHORT).show()
            }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Authorization" to "Basic " + Base64.encodeToString("$apiKey:".toByteArray(), Base64.NO_WRAP)
                )
            }
        }

        queue.add(request)
    }

    private fun parseTrainData(response: org.json.JSONObject) {
        trainList.clear()
        val journeys = response.optJSONArray("journeys") ?: JSONArray()

        for (i in 0 until journeys.length()) {
            val journey = journeys.getJSONObject(i)
            val departureTime = formatDateTime(journey.optString("departure_date_time", "Inconnu"))
            val arrivalTime = formatDateTime(journey.optString("arrival_date_time", "Inconnu"))
            val duration = formatDuration(journey.optInt("duration", 0))

            val sections = journey.optJSONArray("sections") ?: JSONArray()
            var departureStation = "Inconnue"
            var arrivalStation = "Inconnue"
            var transportMode = "Train"
            var isDirect = true

            for (j in 0 until sections.length()) {
                val section = sections.getJSONObject(j)

                if (section.has("from") && section.has("to")) {
                    val from = section.getJSONObject("from")
                    val to = section.getJSONObject("to")

                    // 🔥 Essayer d'obtenir le nom de la gare de départ (stop_point, stop_area, administrative_region)
                    if (j == 0) {
                        departureStation = getStationName(from)
                    }

                    // 🔥 Essayer d'obtenir le nom de la gare d'arrivée (stop_point, stop_area, administrative_region)
                    if (j == sections.length() - 1) {
                        arrivalStation = getStationName(to)
                    }
                }

                if (section.has("display_informations")) {
                    val transportInfo = section.getJSONObject("display_informations")
                    transportMode = transportInfo.optString("commercial_mode", transportMode) + " " + transportInfo.optString("label", "")
                }

                if (section.optString("type") == "transfer") {
                    isDirect = false
                }
            }

            trainList.add(TrainItem(transportMode, departureStation, arrivalStation, departureTime, arrivalTime, duration, isDirect))
        }

        recyclerView.adapter = TrainAdapter(trainList)
    }

    private fun getStationName(jsonObject: org.json.JSONObject): String {
        return when {
            jsonObject.has("stop_point") -> jsonObject.getJSONObject("stop_point").optString("name", "Inconnue")
            jsonObject.has("stop_area") -> jsonObject.getJSONObject("stop_area").optString("name", "Inconnue")
            jsonObject.has("administrative_region") -> jsonObject.getJSONObject("administrative_region").optString("name", "Inconnue")
            else -> "Inconnue"
        }
    }


    private fun formatDateTime(rawDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = inputFormat.parse(rawDate)
            outputFormat.format(date ?: rawDate)
        } catch (e: Exception) {
            "Inconnu"
        }
    }

    private fun formatDuration(durationInSeconds: Int): String {
        val hours = durationInSeconds / 3600
        val minutes = (durationInSeconds % 3600) / 60
        return "${hours}h${minutes.toString().padStart(2, '0')}"
    }
}
