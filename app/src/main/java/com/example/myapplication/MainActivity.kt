package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private var player1Points = 0
    private var player2Points = 0
    private var player1Games = 0
    private var player2Games = 0
    private var player1Sets = 0
    private var player2Sets = 0
    private var isDeuce = false
    private var advantage = 0
    private var matchEnded = false
    private lateinit var originalBgP1: Drawable
    private lateinit var originalBgP2: Drawable
    private var player1Active = false
    private var player2Active = false
    private var player1Name = "Joueur 1"
    private var player2Name = "Joueur 2"

    private lateinit var tvPlayer1Name: TextView
    private lateinit var tvPlayer2Name: TextView
    private lateinit var tvPlayer1Score: TextView
    private lateinit var tvPlayer2Score: TextView
    private lateinit var tvPlayer1Games: TextView
    private lateinit var tvPlayer2Games: TextView
    private lateinit var tvPlayer1Sets: TextView
    private lateinit var tvPlayer2Sets: TextView
    private lateinit var btnPlayer1Point: Button
    private lateinit var btnPlayer2Point: Button
    private lateinit var btnReset: Button
    private lateinit var btnHistory: Button
    private lateinit var tvWinnerMessage: TextView
    private lateinit var topPlayerContainer: View
    private lateinit var bottomPlayerContainer: View

    private val PREFS_NAME = "TennisAppPrefs"
    private val CURRENT_MATCH = "current_match"
    private val MATCH_HISTORY = "match_history"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeViews()
        loadCurrentMatch()
        setupClickListeners()
        updateScoreDisplay()
    }

    private fun initializeViews() {
        tvPlayer1Name = findViewById(R.id.tvPlayer1Name)
        tvPlayer2Name = findViewById(R.id.tvPlayer2Name)
        tvPlayer1Score = findViewById(R.id.tvPlayer1Score)
        tvPlayer2Score = findViewById(R.id.tvPlayer2Score)
        tvPlayer1Games = findViewById(R.id.tvPlayer1Games)
        tvPlayer2Games = findViewById(R.id.tvPlayer2Games)
        tvPlayer1Sets = findViewById(R.id.tvPlayer1Sets)
        tvPlayer2Sets = findViewById(R.id.tvPlayer2Sets)
        btnPlayer1Point = findViewById(R.id.btnPlayer1Point)
        btnPlayer2Point = findViewById(R.id.btnPlayer2Point)
        btnReset = findViewById(R.id.btnReset)
        btnHistory = findViewById(R.id.btnHistory)
        tvWinnerMessage = findViewById(R.id.tvWinnerMessage)
        topPlayerContainer = findViewById(R.id.topPlayerContainer)
        bottomPlayerContainer = findViewById(R.id.bottomPlayerContainer)
        originalBgP1 = btnPlayer1Point.background
        originalBgP2 = btnPlayer2Point.background
    }

    private fun setupClickListeners() {
        btnPlayer1Point.setOnClickListener {
            player1Active = true
            player2Active = false
            updateButtonColors()
            addPointToPlayer1()
        }

        btnPlayer2Point.setOnClickListener {
            player2Active = true
            player1Active = false
            updateButtonColors()
            addPointToPlayer2()
        }

        btnReset.setOnClickListener { showResetDialog() }
        btnHistory.setOnClickListener { openHistory() }
        tvPlayer1Name.setOnClickListener { editPlayerName(1) }
        tvPlayer2Name.setOnClickListener { editPlayerName(2) }
    }

    private fun updateButtonColors() {
        // Player 1 button: bright green glow when active
        if (player1Active) {
            btnPlayer1Point.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00FF88"))
            btnPlayer1Point.alpha = 1.0f
        } else {
            btnPlayer1Point.backgroundTintList = null
            btnPlayer1Point.alpha = 0.85f
        }

        // Player 2 button
        if (player2Active) {
            btnPlayer2Point.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00FF88"))
            btnPlayer2Point.alpha = 1.0f
        } else {
            btnPlayer2Point.backgroundTintList = null
            btnPlayer2Point.alpha = 0.85f
        }
    }

    private fun editPlayerName(playerNumber: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (playerNumber == 1) "Nom du Joueur 1" else "Nom du Joueur 2")

        val input = EditText(this)
        input.hint = "Lettres uniquement "
        input.setText(if (playerNumber == 1) player1Name else player2Name)
        builder.setView(input)

        builder.setPositiveButton("Confirmer") { _, _ ->
            val name = input.text.toString().trim()
            if (isValidName(name)) {
                if (playerNumber == 1) {
                    player1Name = name
                    tvPlayer1Name.text = name
                    updateButtonText()
                } else {
                    player2Name = name
                    tvPlayer2Name.text = name
                    updateButtonText()
                }
                saveCurrentMatch()
            } else {
                Toast.makeText(this, "Nom invalide! Utilisez uniquement des lettres", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Annuler", null)
        builder.show()
    }

    private fun isValidName(name: String): Boolean {
        if (name.isEmpty()) return false
        return name.all { it.isLetter() || it.isWhitespace() }
    }

    private fun updateButtonText() {
        btnPlayer1Point.text = "${player1Name.uppercase()}\nMARQUE"
        btnPlayer2Point.text = "${player2Name.uppercase()}\nMARQUE"
    }

    private fun addPointToPlayer1() {
        if (matchEnded) return

        if (isDeuce) {
            when {
                advantage == 2 -> {
                    advantage = 0
                    isDeuce = true
                }
                advantage == 1 -> {
                    player1Games++
                    resetPoints()
                    checkGameWin()
                }
                else -> advantage = 1
            }
        } else {
            player1Points++
            checkPointWin()
        }
        updateScoreDisplay()
        saveCurrentMatch()
    }

    private fun addPointToPlayer2() {
        if (matchEnded) return

        if (isDeuce) {
            when {
                advantage == 1 -> {
                    advantage = 0
                    isDeuce = true
                }
                advantage == 2 -> {
                    player2Games++
                    resetPoints()
                    checkGameWin()
                }
                else -> advantage = 2
            }
        } else {
            player2Points++
            checkPointWin()
        }
        updateScoreDisplay()
        saveCurrentMatch()
    }

    private fun checkPointWin() {
        if (player1Points >= 3 && player2Points >= 3) {
            isDeuce = true
            advantage = when {
                player1Points == player2Points -> 0
                player1Points > player2Points -> 1
                else -> 2
            }
            return
        }

        when {
            player1Points >= 4 -> {
                player1Games++
                resetPoints()
                checkGameWin()
            }
            player2Points >= 4 -> {
                player2Games++
                resetPoints()
                checkGameWin()
            }
        }
    }

    private fun checkGameWin() {
        when {
            player1Games >= 6 && player1Games - player2Games >= 2 -> {
                player1Sets++
                resetGames()
                checkSetWin()
            }
            player2Games >= 6 && player2Games - player1Games >= 2 -> {
                player2Sets++
                resetGames()
                checkSetWin()
            }
            player1Games == 7 && player2Games == 6 -> {
                player1Sets++
                resetGames()
                checkSetWin()
            }
            player2Games == 7 && player1Games == 6 -> {
                player2Sets++
                resetGames()
                checkSetWin()
            }
        }
    }

    private fun checkSetWin() {
        when {
            player1Sets >= 2 -> {
                showWinner(player1Name)
                saveMatchToHistory(player1Name)
            }
            player2Sets >= 2 -> {
                showWinner(player2Name)
                saveMatchToHistory(player2Name)
            }
        }
    }

    private fun showWinner(winnerName: String) {
        matchEnded = true
        // Animate cards out
        val fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        topPlayerContainer.startAnimation(fadeOut)
        bottomPlayerContainer.startAnimation(fadeOut)
        topPlayerContainer.visibility = View.GONE
        bottomPlayerContainer.visibility = View.GONE
        btnPlayer1Point.visibility = View.GONE
        btnPlayer2Point.visibility = View.GONE
        tvWinnerMessage.text = "$winnerName Gagne!"
        val winnerContainer = findViewById<View>(R.id.winnerContainer)
        winnerContainer.visibility = View.VISIBLE
        // Pop-in animation for winner card
        val popIn = AnimationUtils.loadAnimation(this, R.anim.pop_in)
        winnerContainer.startAnimation(popIn)
        saveCurrentMatch()
    }

    private fun showResetDialog() {
        AlertDialog.Builder(this)
            .setTitle("Réinitialiser")
            .setMessage("Voulez-vous réinitialiser le match?")
            .setPositiveButton("Oui") { _, _ -> resetMatch() }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun resetMatch() {
        player1Points = 0
        player2Points = 0
        player1Games = 0
        player2Games = 0
        player1Sets = 0
        player2Sets = 0
        isDeuce = false
        advantage = 0
        matchEnded = false

        topPlayerContainer.visibility = View.VISIBLE
        bottomPlayerContainer.visibility = View.VISIBLE
        btnPlayer1Point.visibility = View.VISIBLE
        btnPlayer2Point.visibility = View.VISIBLE
        findViewById<View>(R.id.winnerContainer).visibility = View.GONE

        updateScoreDisplay()
        saveCurrentMatch()
    }

    private fun resetPoints() {
        player1Points = 0
        player2Points = 0
        isDeuce = false
        advantage = 0
    }

    private fun resetGames() {
        player1Games = 0
        player2Games = 0
    }

    private fun updateScoreDisplay() {
        tvPlayer1Name.text = player1Name
        tvPlayer2Name.text = player2Name

        val newP1Score = getPointsDisplay(player1Points, 1)
        val newP2Score = getPointsDisplay(player2Points, 2)

        // Pulse animation when score changes
        if (tvPlayer1Score.text != newP1Score) {
            val pulse = AnimationUtils.loadAnimation(this, R.anim.score_pulse)
            tvPlayer1Score.startAnimation(pulse)
        }
        if (tvPlayer2Score.text != newP2Score) {
            val pulse = AnimationUtils.loadAnimation(this, R.anim.score_pulse)
            tvPlayer2Score.startAnimation(pulse)
        }

        tvPlayer1Score.text = newP1Score
        tvPlayer2Score.text = newP2Score
        tvPlayer1Games.text = player1Games.toString()
        tvPlayer2Games.text = player2Games.toString()
        tvPlayer1Sets.text = player1Sets.toString()
        tvPlayer2Sets.text = player2Sets.toString()
        updateButtonText()
    }

    private fun getPointsDisplay(points: Int, player: Int): String {
        if (isDeuce) {
            return when {
                advantage == 0 -> "40"
                advantage == player -> "AV"
                else -> "40"
            }
        }

        return when (points) {
            0 -> "0"
            1 -> "15"
            2 -> "30"
            3 -> "40"
            else -> "G"
        }
    }

    private fun saveCurrentMatch() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val matchData = MatchData(
            player1Name, player2Name,
            player1Points, player2Points,
            player1Games, player2Games,
            player1Sets, player2Sets,
            isDeuce, advantage, matchEnded
        )
        val json = Gson().toJson(matchData)
        prefs.edit().putString(CURRENT_MATCH, json).apply()
    }

    private fun loadCurrentMatch() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(CURRENT_MATCH, null) ?: return

        try {
            val matchData = Gson().fromJson(json, MatchData::class.java)
            player1Name = matchData.player1Name
            player2Name = matchData.player2Name
            player1Points = matchData.player1Points
            player2Points = matchData.player2Points
            player1Games = matchData.player1Games
            player2Games = matchData.player2Games
            player1Sets = matchData.player1Sets
            player2Sets = matchData.player2Sets
            isDeuce = matchData.isDeuce
            advantage = matchData.advantage
            matchEnded = matchData.matchEnded

            if (matchEnded) {
                val winner = if (player1Sets >= 2) player1Name else player2Name
                showWinner(winner)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveMatchToHistory(winner: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyJson = prefs.getString(MATCH_HISTORY, "[]")
        val type = object : TypeToken<MutableList<MatchHistory>>() {}.type
        val history: MutableList<MatchHistory> = Gson().fromJson(historyJson, type)

        val matchHistory = MatchHistory(
            date = System.currentTimeMillis(),
            player1Name = player1Name,
            player2Name = player2Name,
            player1Sets = player1Sets,
            player2Sets = player2Sets,
            winner = winner
        )

        history.add(0, matchHistory)
        if (history.size > 50) history.removeAt(history.size - 1)

        val newJson = Gson().toJson(history)
        prefs.edit().putString(MATCH_HISTORY, newJson).apply()
    }

    private fun openHistory() {
        val intent = Intent(this, HistoryActivity::class.java)
        startActivity(intent)
    }

    override fun onPause() {
        super.onPause()
        saveCurrentMatch()
    }

    data class MatchData(
        val player1Name: String,
        val player2Name: String,
        val player1Points: Int,
        val player2Points: Int,
        val player1Games: Int,
        val player2Games: Int,
        val player1Sets: Int,
        val player2Sets: Int,
        val isDeuce: Boolean,
        val advantage: Int,
        val matchEnded: Boolean
    )

    data class MatchHistory(
        val date: Long,
        val player1Name: String,
        val player2Name: String,
        val player1Sets: Int,
        val player2Sets: Int,
        val winner: String
    )
}