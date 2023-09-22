package com.example.pdfapp


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.IOException


open class MainActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var edittext: EditText
    private lateinit var saveBtn: Button
    private lateinit var discardBtn: Button

    private val STORAGE_CONSTANT = 200

    private val fileName = "my_doc.pdf"

    private lateinit var pdfAdapter: PdfAdapter
    private lateinit var recyclerView: RecyclerView

    private lateinit var directory: File

    private var editPageNum = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PDFBoxResourceLoader.init(applicationContext)

        edittext = findViewById(R.id.edit_text)
        saveBtn = findViewById(R.id.save_btn)
        discardBtn = findViewById(R.id.discard_btn)
        recyclerView = findViewById(R.id.recycler_view)


        directory = File(Environment.getExternalStorageDirectory(), "documents")

        showPdf()

        saveBtn.setOnClickListener {
            if (!checkPermission()) {
                // Permission is not granted, ask for permission
                showAlertDialogue(
                    "Permission needed!",
                    "Storage permission is needed to save PDF files",
                    false,
                    "Ok",
                    "Don't save",
                    {
                        // Logic for positive button click (Grant)
                        requestPermission()
                    },
                    {
                        // (Don't save)
                        println("permission not granted, failed to save")
                    }
                )
            } else {
                println("..........Permission already granted")
                if (saveBtn.text == "Save") {
                    //add new page
                    val pdfFile = File(directory, fileName)
                    addTextToPdf(pdfFile.path, edittext.text.toString())
                    closeKeyBoard()
                    showPdf()
                    if(pdfAdapter.itemCount > 1) recyclerView.smoothScrollToPosition(pdfAdapter.itemCount - 1)
                } else if (saveBtn.text == getString(R.string.save_changes)) {
                    // save changes
                    saveChanges()
                    saveBtn.text = getString(R.string.save)
                    discardBtn.visibility = GONE
                    closeKeyBoard()
                    recyclerView.scrollToPosition(editPageNum-1)
                    Toast.makeText(this, "changes saved", Toast.LENGTH_SHORT).show()
                }
                edittext.text.clear()
            }
        }

        discardBtn.setOnClickListener {
            edittext.text.clear()
            // Close the keyboard
            closeKeyBoard()
            discardBtn.visibility = GONE
        }
    }

    private fun closeKeyBoard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(edittext.windowToken, 0)
    }

    private fun saveChanges() {
        try {
            val spaceSize = 5f
            val lineSpace = 10f
            val fontSize = 16f

            val document = PDDocument()

            val file = File(directory.path + "/" + fileName)
            if (file.exists()) {
                val existingPdf = PDDocument.load(file)
                for (i in 0..<existingPdf.numberOfPages) {
                    if (i == editPageNum - 1) {
                        //need to edit

                        val page = PDPage(PDRectangle.A4)
                        document.addPage(page)

                        val contentStream = PDPageContentStream(document, page)
                        val font = PDType0Font.load(
                            document,
                            assets.open("Roboto/Roboto-Regular.ttf")
                        )
                        contentStream.setFont(font, fontSize)

                        // Set the starting position and available width on the page
                        val startX = 50f
                        val startY = 750f
                        val availableWidth = PDRectangle.A4.width - 2 * startX

                        contentStream.beginText()
                        contentStream.newLineAtOffset(startX, startY)

                        //split text into lines
                        val sentences = edittext.text.toString().split("\n")

                        for (sentence in sentences) {
                            // Split the sentence into words
                            val words = sentence.split(" ")

                            // Shift of writing pointer in x direction
                            var shiftX = 0f

                            for (word in words) {
                                // Calculate the width of the word in the current font and size
                                val wordWidth =
                                    spaceSize + font.getStringWidth(word) * fontSize / 1000

                                if (shiftX + wordWidth <= availableWidth) {
                                    println("........... same line")
                                    contentStream.showText(word)
                                    shiftX += wordWidth
                                    contentStream.newLineAtOffset(wordWidth, 0f)
                                } else {
                                    println("........... new line")
                                    contentStream.newLineAtOffset(-shiftX, -fontSize - lineSpace)
                                    contentStream.showText(word)
                                    shiftX = wordWidth
                                    contentStream.newLineAtOffset(wordWidth, 0f)
                                }

                            }

                            println("........... new line")
                            contentStream.newLineAtOffset(-shiftX, -fontSize - lineSpace)
                        }

                        contentStream.endText()
                        contentStream.close()
                    } else document.addPage(existingPdf.getPage(i))
                }
            }

            // Save the document to a file
            document.save(File(directory.path + "/" + fileName))
            document.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }
        showPdf()
    }


    private fun showPdf() {
        if (checkPermission()) {
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val existingFile = File(directory.path + "/" + fileName)
            if (existingFile.exists()) {
                pdfAdapter = PdfAdapter( existingFile.path, this)
                val pdfLayoutManager: RecyclerView.LayoutManager =
                    LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
                recyclerView.layoutManager = pdfLayoutManager
                recyclerView.adapter = pdfAdapter
                recyclerView.invalidate()
            }
        } else {
            // Permission is not granted, ask for permission
            showAlertDialogue(
                "Permission needed!",
                "Storage permission is needed to save PDF files",
                false,
                "Ok",
                "Don't save",
                {
                    // Logic for positive button click (Grant)
                    requestPermission()
                },
                {
                    // (Don't save)
                    println("permission not granted, failed to save")
                }
            )
        }
    }


    private fun showAlertDialogue(
        title: String,
        msg: String,
        isCancellable: Boolean,
        positiveBtn: String,
        negativeBtn: String,
        positiveClickListener: () -> Unit,
        negativeClickListener: () -> Unit
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setMessage(msg)
        builder.setTitle(title)
        builder.setCancelable(isCancellable)

        builder.setPositiveButton(positiveBtn) { _, _ ->
            positiveClickListener.invoke() // Invoke the lambda when positive button is clicked
        }

        builder.setNegativeButton(negativeBtn) { _, _ ->
            negativeClickListener.invoke() // Invoke the lambda when negative button is clicked
        }

        // Create the Alert dialog
        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
    }


    private fun requestPermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            // android below 10
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                STORAGE_CONSTANT
            )
        } else {
            // android 10+, request permission through MediaStore API
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data =
                    Uri.parse(String.format("package:%s", applicationContext.packageName))
                startActivityForResult(intent, STORAGE_CONSTANT)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, STORAGE_CONSTANT)
            }
        }
    }


    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            // android below 10
            val write =
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val read =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

            write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        } else {
            // android 10+, request permission through MediaStore API
            Environment.isExternalStorageManager()
        }
    }


    // Handle the result of the permission request
    @Deprecated("Deprecated in Java")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == STORAGE_CONSTANT) {
            // Check the permission status and call the appropriate callback
            if (Environment.isExternalStorageManager()) {
                showPdf()
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_CONSTANT) {
            // Check if the permission was granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showPdf()
            } else {
                showAlertDialogue(
                    "Alert!",
                    "Permission is necessary to use app's features",
                    false,
                    "Ok",
                    "Don't save",
                    {
                        // Logic for positive button click (Grant)
                        requestPermission()
                    },
                    {
                        // (Don't save)
                        println("permission not granted, failed to save")
                    }
                )
            }
        }
    }


    private fun addTextToPdf(outputFilePath: String, text: String) {
        try {
            val spaceSize = 5f
            val lineSpace = 10f
            val fontSize = 16f

            val document = PDDocument()

            val file = File(outputFilePath)
            if (file.exists()) {
                val existingPdf = PDDocument.load(file)
                for (i in 0..<existingPdf.numberOfPages) {
                    document.addPage(existingPdf.getPage(i))
                }
            }

            val page = PDPage(PDRectangle.A4)
            document.addPage(page)

            val contentStream = PDPageContentStream(document, page)
            val font = PDType0Font.load(
                document,
                assets.open("Roboto/Roboto-Regular.ttf")
            )
            contentStream.setFont(font, fontSize)

            // Set the starting position and available width on the page
            val startX = 50f
            val startY = 750f
            val availableWidth = PDRectangle.A4.width - 2 * startX

            contentStream.beginText()
            contentStream.newLineAtOffset(startX, startY)

            //split text into lines
            val sentences = text.split("\n")

            for (sentence in sentences) {
                // Split the sentence into words
                val words = sentence.split(" ")

                // Shift of writing pointer in x direction
                var shiftX = 0f

                for (word in words) {
                    // Calculate the width of the word in the current font and size
                    val wordWidth = spaceSize + font.getStringWidth(word) * fontSize / 1000

                    if (shiftX + wordWidth <= availableWidth) {
                        println("........... same line")
                        contentStream.showText(word)
                        shiftX += wordWidth
                        contentStream.newLineAtOffset(wordWidth, 0f)
                    } else {
                        println("........... new line")
                        contentStream.newLineAtOffset(-shiftX, -fontSize - lineSpace)
                        contentStream.showText(word)
                        shiftX = wordWidth
                        contentStream.newLineAtOffset(wordWidth, 0f)
                    }

                }

                println("........... new line")
                contentStream.newLineAtOffset(-shiftX, -fontSize - lineSpace)
            }

            contentStream.endText()
            contentStream.close()

            // Save the document to a file
            document.save(File(outputFilePath))
            document.close()


        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    override fun onItemClick(position: Int) {
        showAlertDialogue(
            "Alert!",
            "What do you want to do with this page",
            true,
            "Delete",
            "Edit",
            {
                // Logic for Delete
                editPageNum = position + 1
                deletePage()
            },
            {
                // Logic for Edit
                editPageNum = position + 1
                val prevText = readTextFromFile(directory.path + "/" + fileName, position + 1)
                edittext.text.clear()
                edittext.setText(prevText.trim())
                saveBtn.text = getString(R.string.save_changes)
                discardBtn.visibility = VISIBLE
            }
        )
    }

    private fun deletePage() {
        try {
            val document = PDDocument()

            val file = File(directory.path + "/" + fileName)
            if (file.exists()) {
                val existingPdf = PDDocument.load(file)
                for (i in 0..<existingPdf.numberOfPages) {
                    if (i == editPageNum - 1) continue
                    document.addPage(existingPdf.getPage(i))
                }
            }
            // Save the document to a file
            document.save(File(directory.path + "/" + fileName))
            document.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }
        showPdf()
        recyclerView.scrollToPosition(editPageNum-1)
    }


    private fun readTextFromFile(filePath: String, pageNumber: Int): String {
        val document = PDDocument.load(File(filePath))
        val pdfStripper = PDFTextStripper()
        // Set the page range to extract text from the specific page
        pdfStripper.startPage = pageNumber
        pdfStripper.endPage = pageNumber

        val text = pdfStripper.getText(document)
        document.close()
        return text
    }

}