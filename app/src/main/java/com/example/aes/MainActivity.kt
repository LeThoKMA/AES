package com.example.aes

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Random
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {
    lateinit var pickFile: TextView
    lateinit var nameFile: TextView
    lateinit var encrypt: TextView
    lateinit var decrypt: TextView
    lateinit var encryptContent: TextView
    lateinit var decryptContent: TextView
    lateinit var loadingDialog: AlertDialog
    lateinit var encryptByteArray: ByteArray
    lateinit var spinnerBit: Spinner
    lateinit var spinnerType: Spinner
    lateinit var aes: AES
    var content = ""
    var afterEncrypt = ""

    @SuppressLint("MissingInflatedId", "ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadingDialog = setupProgressDialog()!!
        permission()
        aes = AES(generateRandomString(16)!!.toByteArray(), "1234567812345678".toByteArray())
        pickFile = findViewById(R.id.pick_file)
        nameFile = findViewById(R.id.file_name)
        encrypt = findViewById(R.id.encrypt)
        decrypt = findViewById(R.id.decrypt)
        decrypt.isEnabled = false
        encryptContent = findViewById(R.id.encrypt_content)
        decryptContent = findViewById(R.id.decrypt_content)
        spinnerBit = findViewById(R.id.sp_bit)
        spinnerBit.adapter = SpinnerAdapter(arrayListOf("128 bit", "192 bit", "256 bit"), this)
        spinnerType = findViewById(R.id.sp_type)
        spinnerType.adapter = SpinnerAdapter(arrayListOf("ECB", "CBC"), this)

        spinnerBit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                encryptByteArray = "".toByteArray()
                encryptContent.text = ""
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
        encrypt.setOnClickListener {
            if (content.isBlank()) {
                Toast.makeText(this, "Hãy chọn file", Toast.LENGTH_LONG).show()
            } else {
                var time = measureTimeMillis {
                    if (spinnerBit.selectedItemPosition == 0) {
                        aes = AES(
                            generateRandomString(16)!!.toByteArray(),
                            "1234567812345678".toByteArray(),
                        )
                    } else if (spinnerBit.selectedItemPosition == 1) {
                        aes = AES(
                            generateRandomString(24)!!.toByteArray(),
                            "1234567812345678".toByteArray(),
                        )
                    } else {
                        aes = AES(
                            generateRandomString(32)!!.toByteArray(),
                            "1234567812345678".toByteArray(),
                        )
                    }
                    if (spinnerType.selectedItem == "ECB") {
                        encryptByteArray = aes.ECB_encrypt(content.toByteArray())
                    } else {
                        encryptByteArray = aes.CBC_encrypt(content.toByteArray())
                    }

                    decrypt.isEnabled = true
                    decryptContent.text = ""
                }
                encryptContent.text = String(encryptByteArray) + "\n\n" + "Thời gian mã hóa :" + time + "ms"
            }
        }
        decrypt.setOnClickListener {
            var time = measureTimeMillis {
                if (spinnerType.selectedItem == "ECB") {
                    decryptContent.text = String(aes.ECB_decrypt(encryptByteArray))
                } else {
                    decryptContent.text = String(aes.CBC_decrypt(encryptByteArray))
                }
            }
            decryptContent.text =
                decryptContent.text.toString() + "\n\n" + "Thời gian giải mã :" + time + "ms"
        }

        pickFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "text/plain"
            startActivityForResult(intent, 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            var fileName = ""
            var contentResolver = contentResolver
            var documentFile = data?.data?.let { DocumentFile.fromSingleUri(this, it) }
            if (documentFile != null) {
                fileName = documentFile.getName().toString()
                Log.e("TAG", fileName)
            }

            if (fileName.isBlank()) {
                val cursor = contentResolver.query(data?.data!!, null, null, null, null)
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        fileName = cursor.getString(nameIndex)
                    }
                } finally {
                    cursor?.close()
                }
            }
            nameFile.text = fileName
            content = data?.data?.let { readFileFromUri(it) }.toString()
        }
    }

    private fun permission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                )
            ) {
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    1,
                )
            }
        }
    }

    fun readFileFromUri(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            stringBuilder.append(line).append("\n")
        }

        inputStream?.close()
        reader.close()

        return stringBuilder.toString()
    }

    private fun setupProgressDialog(): AlertDialog? {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this, R.style.CustomDialog)
        builder.setCancelable(false)

        val myLayout = LayoutInflater.from(this)
        val dialogView: View = myLayout.inflate(R.layout.fragment_progress_dialog, null)

        builder.setView(dialogView)

        val dialog: AlertDialog = builder.create()
        val window: Window? = dialog.window
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window?.attributes)
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            dialog.window?.attributes = layoutParams
        }
        return dialog
    }

    fun generateRandomString(size: Int): String? {
        val random = Random()
        val sb = java.lang.StringBuilder()
        for (i in 0..size.minus(1)) {
            val randomNumber: Int = random.nextInt(size)
            val randomChar =
                (if (randomNumber < 10) '0'.code + randomNumber else 'a'.code + randomNumber - 10).toChar()
            sb.append(randomChar)
        }
        return sb.toString()
    }
}
