package com.lakshyagupta7089.mymemory

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lakshyagupta7089.mymemory.adapter.MemoryBoardAdapter
import com.lakshyagupta7089.mymemory.databinding.ActivityMainBinding
import com.lakshyagupta7089.mymemory.databinding.DialogBoardSizeBinding
import com.lakshyagupta7089.mymemory.models.BoardSize
import com.lakshyagupta7089.mymemory.models.MemoryGame
import com.lakshyagupta7089.mymemory.models.UserImageList
import com.lakshyagupta7089.mymemory.utils.EXTRA_BOARD_SIZE
import com.lakshyagupta7089.mymemory.utils.EXTRA_GAME_NAME
import com.squareup.picasso.Picasso


class MainActivity : AppCompatActivity() {
    private lateinit var adapter: MemoryBoardAdapter
    private lateinit var memoryGame: MemoryGame
    private lateinit var binding: ActivityMainBinding
    private var boardSize: BoardSize = BoardSize.EASY

    private lateinit var soundPool: SoundPool

    private var correct: Int? = null
    private var cardFlip: Int? = null

    private val db = Firebase.firestore
    private var gameName: String? = null
    private var customGameImages: List<String>? = null

    private val startForResult = registerForActivityResult(
        StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val intent = it.data
            val customGameName = intent?.getStringExtra(EXTRA_GAME_NAME)

            if (customGameName == null) {
                Log.d("TAG", "Some error occur so this is null")
                return@registerForActivityResult
            }

            downloadGame(customGameName)
        }
    }

    private fun downloadGame(customGameName: String) {
        db.collection("games")
            .document(customGameName)
            .get()
            .addOnSuccessListener { document ->
                val userImageList = document.toObject(UserImageList::class.java)

                if (userImageList?.images == null) {
                    Log.d("TAG", "downloadGame: Error")
                    Snackbar.make(
                        binding.root,
                        "Sorry, we couldn't find any such game, $gameName",
                        Snackbar.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                val numCards = userImageList.images.size * 2
                customGameImages = userImageList.images
                boardSize = BoardSize.getByValue(numCards)

                for (imageUrl in userImageList.images) {
                    Picasso.get()
                        .load(imageUrl).fetch()
                }

                Snackbar.make(
                    binding.root,
                    "You're now playing '$customGameName'!",
                    Snackbar.LENGTH_LONG
                ).show()

                gameName = customGameName

                setupBoard()
            }.addOnFailureListener {
                Log.d("TAG", "downloadGame: something wrong happens with me")
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val attributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attributes)
            .build()

        correct = soundPool.load(this, R.raw.correct, 1)
        cardFlip = soundPool.load(this, R.raw.card_flip, 1)

        setupBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_refresh -> {
                if (memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()) {
                    showAlertDialog("Quit your current game!", null) {
                        setupBoard()
                    }
                } else {
                    setupBoard()
                }

                return true
            }

            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }

            R.id.mi_custom -> {
                showCreationDialog()
                return true
            }

            R.id.mi_download -> {
                showDownloadDialog()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showDownloadDialog() {
        val boardDownloadView =
            LayoutInflater.from(this).inflate(R.layout.dialog_download_board, null)

        showAlertDialog("Fetch memory game", boardDownloadView) {
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString()

            downloadGame(gameToDownload)
        }
    }

    private fun showCreationDialog() {
        val dialogBoardSizeBinding = DialogBoardSizeBinding.inflate(layoutInflater)

        showAlertDialog(
            "Create you own memory board",
            dialogBoardSizeBinding.root
        ) {
            val desiredBoardSize = when (dialogBoardSizeBinding.radioGroup.checkedRadioButtonId) {
                dialogBoardSizeBinding.rbEasy.id -> BoardSize.EASY
                dialogBoardSizeBinding.rbMedium.id -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }

            // Navigate to a new activity
            val intent = Intent(this, CreateActivity::class.java)

            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)

            startForResult.launch(intent)
        }
    }

    private fun showNewSizeDialog() {
        val dialogBoardSizeBinding = DialogBoardSizeBinding.inflate(layoutInflater)

        when (boardSize) {
            BoardSize.EASY -> dialogBoardSizeBinding.radioGroup.check(dialogBoardSizeBinding.rbEasy.id)
            BoardSize.MEDIUM -> dialogBoardSizeBinding.radioGroup.check(dialogBoardSizeBinding.rbMedium.id)
            BoardSize.HARD -> dialogBoardSizeBinding.radioGroup.check(dialogBoardSizeBinding.rbHard.id)
        }

        showAlertDialog(
            "Choose new size",
            dialogBoardSizeBinding.root
        ) {
            boardSize = when (dialogBoardSizeBinding.radioGroup.checkedRadioButtonId) {
                dialogBoardSizeBinding.rbEasy.id -> BoardSize.EASY
                dialogBoardSizeBinding.rbMedium.id -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }

            gameName = null
            customGameImages = null

            setupBoard()
        }
    }

    private fun showAlertDialog(
        title: String,
        view: View?,
        positiveClickListener: View.OnClickListener
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok") { _, _ ->
                positiveClickListener.onClick(null)
            }.show()
    }

    private fun setupBoard() {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)

        when (boardSize) {
            BoardSize.EASY -> {
                binding.tvNumMoves.text = "Easy: 4 x 2"
                binding.tvNumPairs.text = "Pairs: 0 / 4"
            }
            BoardSize.MEDIUM -> {
                binding.tvNumMoves.text = "Medium: 6 x 3"
                binding.tvNumPairs.text = "Pairs: 0 / 9"
            }
            BoardSize.HARD -> {
                binding.tvNumMoves.text = "Hard: 6 x 4"
                binding.tvNumPairs.text = "Pairs: 0 / 12"
            }
        }

        binding.tvNumPairs.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.color_progress_none
            )
        )

        memoryGame = MemoryGame(boardSize, customGameImages)

        adapter = MemoryBoardAdapter(
            this,
            boardSize,
            memoryGame.cards,
            object : MemoryBoardAdapter.CardClickListener {
                override fun onCardClick(position: Int) {
                    updateGameWithFlip(position)
                }
            }
        )

        binding.rvBoard.adapter = adapter

        binding.rvBoard.setHasFixedSize(true)
        binding.rvBoard.layoutManager = GridLayoutManager(
            this, boardSize.getWidth()
        )
    }

    private fun updateGameWithFlip(position: Int) {
        if (memoryGame.haveWonGame()) {
            Snackbar.make(
                binding.root,
                "You already won",
                Snackbar.LENGTH_SHORT
            ).show()

            return
        }

        if (memoryGame.isCardIsFaceUp(position)) {
            Snackbar.make(
                binding.root,
                "Invalid move!",
                Snackbar.LENGTH_SHORT
            ).show()

            return
        }

        if (memoryGame.flipCard(position)) {
            soundPool.play(
                correct!!,
                1F,
                1F,
                0,
                0,
                1F
            )

            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairFound.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full),
            ) as Int

            binding.tvNumPairs.setTextColor(color)
            binding.tvNumPairs.text =
                "Pairs: ${memoryGame.numPairFound} / ${boardSize.getNumPairs()}"

            if (memoryGame.haveWonGame()) {
                Snackbar.make(
                    binding.root,
                    "You won! Congratulations",
                    Snackbar.LENGTH_LONG
                ).show()

                CommonConfetti.rainingConfetti(binding.root, intArrayOf(
                    Color.YELLOW,
                    Color.GREEN,
                    Color.MAGENTA,
                )).oneShot()
            }
        } else {
            soundPool.play(
                cardFlip!!,
                1F,
                1F,
                0,
                0,
                1F
            )
        }

        binding.tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }
}