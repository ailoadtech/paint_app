package com.example.paintapp

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.paintapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isRainbowActive = false
    private var colorDialog: AlertDialog? = null
    private val sizeValues = floatArrayOf(1f, 10f, 30f, 100f, 300f)
    private var sizeIndex = 1
    private var stampMode = 0
    private val stampResources = listOf(
        R.drawable.black, R.drawable.stamp1, R.drawable.stamp2, R.drawable.stamp3,
        R.drawable.stamp4, R.drawable.stamp5, R.drawable.stamp6, R.drawable.stamp7
    )

    private val saveLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        uri?.let { saveBitmapToUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        binding.drawingView.setBrushSize(sizeValues[sizeIndex])
    }

    private fun setupToolbar() {
        binding.btnClear.setOnClickListener {
            binding.drawingView.clearCanvas()
        }

        binding.btnColor.setOnClickListener {
            showColorWheelDialog()
        }

        binding.btnSize.setOnClickListener {
            sizeIndex = (sizeIndex + 1) % sizeValues.size
            binding.drawingView.setBrushSize(sizeValues[sizeIndex])
        }

        binding.btnRainbow.setOnClickListener {
            isRainbowActive = !isRainbowActive
            binding.drawingView.setRainbowEnabled(isRainbowActive)
        }

        binding.btnStamp.setOnClickListener {
            stampMode = (stampMode + 1) % 8
            binding.btnStamp.setImageResource(stampResources[stampMode])
            binding.drawingView.setStampMode(stampMode)
            if (stampMode > 0 && isRainbowActive) {
                isRainbowActive = false
                binding.drawingView.setRainbowEnabled(false)
            }
        }

        binding.btnSave.setOnClickListener {
            val bitmap = binding.drawingView.getBitmap()
            if (bitmap == null) {
                Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveLauncher.launch("my_painting.jpg")
        }
    }

    private fun showColorWheelDialog() {
        val colorWheelView = ColorWheelView(this)
        colorWheelView.setOnColorSelectedListener { color ->
            binding.drawingView.setColor(color)
            if (isRainbowActive) {
                isRainbowActive = false
                binding.drawingView.setRainbowEnabled(false)
            }
            colorDialog?.dismiss()
        }

        colorDialog = AlertDialog.Builder(this)
            .setView(colorWheelView)
            .show()
    }

    private fun saveBitmapToUri(uri: android.net.Uri) {
        val bitmap = binding.drawingView.getBitmap() ?: return
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
            }
            Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
