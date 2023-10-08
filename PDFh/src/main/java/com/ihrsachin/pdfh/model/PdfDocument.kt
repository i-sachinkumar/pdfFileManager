package com.ihrsachin.pdfh.model

import com.tom_roush.pdfbox.pdmodel.PDDocument

class PdfDocument() {
    var file: PDDocument? = null
        private set

    init {
        file = PDDocument()
    }
}