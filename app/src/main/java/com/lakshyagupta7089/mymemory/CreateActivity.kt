package com.lakshyagupta7089.mymemory

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.storage.ktx.storage
import com.lakshyagupta7089.mymemory.adapter.ImagePickerAdapter
import com.lakshyagupta7089.mymemory.databinding.ActivityCreateBinding
import com.lakshyagupta7089.mymemory.models.BoardSize
import com.lakshyagupta7089.mymemory.utils.*
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {
    companion object {
        private const val READ_EXTERNAL_PHOTOS_CODE = 248
        private const val READ_PHOTOS_PERMISSIONS = Manifest.permission.READ_EXTERNAL_STORAGE
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14
    }

    private lateinit var adapter: ImagePickerAdapter
    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1
    private val chosenImageUris = mutableListOf<Uri>()

    private val storage = Firebase.storage
    private val db = Firebase.firestore

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK && it.data != null) {
            val data = it.data

            val selectedUri = data?.data
            val clipData = data?.clipData

            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val clipItem = clipData.getItemAt(i)

                    if (chosenImageUris.size < numImagesRequired) {
                        chosenImageUris.add(clipItem.uri)
                    }
                }
            } else if (selectedUri != null) {
                chosenImageUris.add(selectedUri)
            }

            adapter.notifyDataSetChanged()
            supportActionBar?.title = "Choose pics (${chosenImageUris.size} / $numImagesRequired)"
            binding.btnSave.isEnabled = shouldEnableSaveButton()
        }
    }

    private lateinit var binding: ActivityCreateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Firebase.initialize(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired = boardSize.getNumPairs()

        supportActionBar?.title = "Choose pics (0 / $numImagesRequired)"

        binding.btnSave.setOnClickListener {
            saveDataFirebase()
        }

        binding.etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))

        binding.etGameName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                binding.btnSave.isEnabled = shouldEnableSaveButton()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {}

        })

        adapter = ImagePickerAdapter(
            this,
            chosenImageUris,
            boardSize,
            object : ImagePickerAdapter.ImageClickListener {
                override fun onPlaceholderClicked() {
                    if (isPermissionGranted(
                            this@CreateActivity,
                            READ_PHOTOS_PERMISSIONS
                        )
                    ) {
                        launchIntentForPhotos()
                    } else {
                        requestPermission(
                            this@CreateActivity,
                            READ_PHOTOS_PERMISSIONS,
                            READ_EXTERNAL_PHOTOS_CODE
                        )
                    }
                }
            }
        )

        binding.rvImagePicker.adapter = adapter
        binding.rvImagePicker.setHasFixedSize(true)
        binding.rvImagePicker.layoutManager = GridLayoutManager(
            this,
            boardSize.getWidth(),
        )
    }

    private fun shouldEnableSaveButton(): Boolean {
        // Check if we should enable the save button or not
        if (chosenImageUris.size != numImagesRequired) {
            return false
        }

        if (binding.etGameName.text.isBlank() || binding.etGameName.text.length < MIN_GAME_NAME_LENGTH) {
            return false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == READ_EXTERNAL_PHOTOS_CODE) {
            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                launchIntentForPhotos()
            } else {
                Toast.makeText(
                    this,
                    "In order to create custom game, you need to provide access to your photos",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        startForResult.launch(Intent.createChooser(intent, "Choose pics"))
    }

    private fun saveDataFirebase() {
        binding.btnSave.isEnabled = false
        val customGameName: String = binding.etGameName.text.toString()

        // Check that we're not over writing someone else's data
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            if (document != null && document.data != null) {
                AlertDialog.Builder(this)
                    .setTitle("Name taken")
                    .setMessage("A game already exist with name '$customGameName'")
                    .setPositiveButton("OK", null)
                    .show()
                binding.btnSave.isEnabled = true
            } else {
                handleImageUploading(customGameName)
                binding.btnSave.isEnabled = true
            }
        }.addOnFailureListener {
            Toast.makeText(
                this,
                " Encountered error while saving memory game",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleImageUploading(customGameName: String) {
        binding.pbUploading.visibility = View.VISIBLE
        var didEncounterError = false
        val uploadedImageUrl = mutableListOf<String>()

        for ((index, photoUri) in chosenImageUris.withIndex()) {
            val imageByteArray = getImageByteArray(photoUri)
            val filePath = "images/$customGameName/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference = storage.reference.child(filePath)

//            Toast.makeText(
//                applicationContext,
//                "I come in this loop",
//                Toast.LENGTH_SHORT
//            ).show()

            photoReference.putBytes(imageByteArray)
                .continueWithTask { photoUploadTask ->
                    photoReference.downloadUrl
                }.addOnCompleteListener { downloadUrlCheck ->
                    if (!downloadUrlCheck.isSuccessful) {
                        Toast.makeText(
                            applicationContext,
                            "Failed to upload image",
                            Toast.LENGTH_SHORT
                        ).show()
                        didEncounterError = true
                        return@addOnCompleteListener
                    }

                    if (didEncounterError) {
                        binding.pbUploading.visibility = View.GONE
                        return@addOnCompleteListener
                    }

                    val downloadUrl = downloadUrlCheck.result.toString()
                    uploadedImageUrl.add(downloadUrl)

                    binding.pbUploading.progress = uploadedImageUrl.size * 100 / chosenImageUris.size

                    if (uploadedImageUrl.size == chosenImageUris.size) {
                        handleAllImagesUploaded(customGameName, uploadedImageUrl)
                    }
                }
        }
    }

    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
        db.collection("games").document(gameName)
            .set(mapOf("images" to imageUrls))
            .addOnCompleteListener { gameCreationTask ->
                binding.pbUploading.visibility = View.GONE

                if (!gameCreationTask.isSuccessful) {
                    Log.d("TAG", "gameCreationTask is not word")
                    Toast.makeText(
                        applicationContext,
                        "Failed game creation",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnCompleteListener
                }


                Log.d("TAG", "handleAllImagesUploaded: yay")
                AlertDialog.Builder(this)
                    .setTitle("Upload complete! Let's play game $gameName")
                    .setPositiveButton("Ok") {_, _ ->
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME, gameName)
                        setResult(Activity.RESULT_OK, resultData)
                        finish()
                    }.show()
            }
    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }

        // Log.d("getImageByteArray", "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
//        Toast.makeText(
//            this,
//            "Original width ${originalBitmap.width} and height ${originalBitmap.height}",
//            Toast.LENGTH_SHORT
//        ).show()

        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)

        // Log.d("getImageByteArray", )
//        Toast.makeText(
//            this,
//            "ScaledBitmap width ${scaledBitmap.width} and height ${scaledBitmap.height}",
//            Toast.LENGTH_SHORT
//        ).show()

        val byteArrayOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream)

        return byteArrayOutputStream.toByteArray()
    }
}
