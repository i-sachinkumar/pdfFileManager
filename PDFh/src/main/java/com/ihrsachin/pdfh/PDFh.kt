package com.ihrsachin.pdfh

import android.content.Context
import com.ihrsachin.pdfh.model.PdfDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font


class PDFh(private val context: Context) {
    private var document: PdfDocument = PdfDocument()
    private var spaceSize = 6f
    private var lineSpace = 8f
    private var fontSize = 12f
    private var text = ""

    var height: Float? = null
    var width: Float? = null

    var leftMargin: Float? = null
    var rightMargin: Float? = null
    var topMargin: Float? = null
    var bottomMargin: Float? = null
    var fontAssetPath: String? = null

    private var page: PDPage? = null

    public fun build(): PDFh {
        page = if (height == null || width == null) {
            PDPage(PDRectangle.A4)
        } else {
            PDPage(PDRectangle(width!!, height!!))
        }

        if (height == null || width == null) {
            height = page!!.bBox.height
            width = page!!.bBox.width
        }

        document.file!!.addPage(page)

        val contentStream = PDPageContentStream(document.file, page)

        var font: PDFont? = null
        // Set the font to the system default font (Helvetica)
        font = if (fontAssetPath == null) {
            PDType1Font.HELVETICA
        } else {
            PDType0Font.load(document.file, context.assets.open(fontAssetPath!!))
        }

        contentStream.setFont(font, fontSize)


        // Set the starting position and available width on the page
        val startX = if (leftMargin == null) 50f else leftMargin;
        val startY = if (topMargin == null) (height!! - 50f) else (height!! - topMargin!!);

        leftMargin = startX
        rightMargin = leftMargin
        topMargin = startY
        bottomMargin = if(bottomMargin == null) 40f else bottomMargin;

        val availableWidth = width!! - leftMargin!! - rightMargin!!
        val availableHeight = height!! - topMargin!! - bottomMargin!!

        contentStream.beginText()
        contentStream.newLineAtOffset(startX!!, startY)

        //split text into lines
        val sentences = text.split("\n")

        for (sentence in sentences) {
            // Split the sentence into words
            val words = sentence.split(" ")

            // Shift of writing pointer in x direction
            var shiftX = 0f

            for (word in words) {
                // Calculate the width of the word in the current font and size
                val wordWidth = spaceSize + font!!.getStringWidth(word) * fontSize / 1000

                if (shiftX + wordWidth <= availableWidth) {
                    println("........... same line")
                    contentStream.showText(word)
                    shiftX += wordWidth
                    contentStream.newLineAtOffset(wordWidth, 0f)
                } else {
                    println("........... new line")
                    contentStream.newLineAtOffset(-shiftX, -fontSize - lineSpace)
                    if (wordWidth <= availableWidth) {
                        contentStream.showText(word)
                        shiftX = wordWidth
                        contentStream.newLineAtOffset(wordWidth, 0f)
                    }
                    else{
                        // show By Letter
                        val letters = word.split("")
                        for(letter in letters){
                            val letterWidth = font.getStringWidth(letter) * fontSize / 1000
                            if(shiftX + letterWidth <= availableWidth){
                                contentStream.showText(letter)
                                shiftX += letterWidth
                                contentStream.newLineAtOffset(letterWidth, 0f)
                            }
                            else{
                                contentStream.newLineAtOffset(-shiftX, -fontSize - lineSpace)
                                contentStream.showText(letter)
                                shiftX = letterWidth
                                contentStream.newLineAtOffset(letterWidth, 0f)
                            }
                        }
                        contentStream.newLineAtOffset(spaceSize, 0f)
                        shiftX += spaceSize
                    }
                }

            }

            println("........... new line")
            contentStream.newLineAtOffset(-shiftX, -fontSize - lineSpace)
        }

        contentStream.endText()
        contentStream.close()

        return this
    }


    // Setter for spaceSize with method chaining
    fun setSpaceSize(spaceSize: Float): PDFh {
        this.spaceSize = spaceSize
        return this
    }

    // Setter for lineSpace with method chaining
    fun setLineSpace(lineSpace: Float): PDFh {
        this.lineSpace = lineSpace
        return this
    }

    // Setter for fontSize with method chaining
    fun setFontSize(fontSize: Float): PDFh {
        this.fontSize = fontSize
        return this
    }

    // Setter for text with method chaining
    fun setText(text: String): PDFh {
        this.text = text
        return this
    }

    // Setter for height with method chaining
    fun setHeight(height: Float?): PDFh {
        this.height = height
        return this
    }

    // Setter for width with method chaining
    fun setWidth(width: Float?): PDFh {
        this.width = width
        return this
    }

    // Setter for leftMargin with method chaining
    fun setLeftMargin(leftMargin: Float?): PDFh {
        this.leftMargin = leftMargin
        return this
    }

    // Setter for rightMargin with method chaining
    fun setRightMargin(rightMargin: Float?): PDFh {
        this.rightMargin = rightMargin
        return this
    }

    // Setter for topMargin with method chaining
    fun setTopMargin(topMargin: Float?): PDFh {
        this.topMargin = topMargin
        return this
    }

    // Setter for bottomMargin with method chaining
    fun setBottomMargin(bottomMargin: Float?): PDFh {
        this.bottomMargin = bottomMargin
        return this
    }

    // Setter for fontAssetPath with method chaining
    fun setFontAssetPath(fontAssetPath: String?): PDFh {
        this.fontAssetPath = fontAssetPath
        return this
    }
}