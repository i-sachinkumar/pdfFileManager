package com.example.pdfapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;

public class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.ViewHolder> {
    private String pdfPath;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;

    OnItemClickListener listener;

    public PdfAdapter(String pdfPath, OnItemClickListener listener) {
        this.pdfPath = pdfPath;
        this.listener = listener;

        try {
            openPdfRenderer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.pdf_page_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        try {
            displayPage(holder.imageView, position); // Display the first page by default
        } catch (Exception e) {
            e.printStackTrace();
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onItemClick(position);
                }
            }
        });

        holder.textView.setText(String.valueOf(position+1 ));
    }

    @Override
    public int getItemCount() {
        // Get the number of pages
        return pdfRenderer.getPageCount();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView textView;
        public ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.image_view);
            textView = view.findViewById(R.id.page_num);

        }
    }

    private void openPdfRenderer() throws Exception {
        File pdfFile = new File(pdfPath);
        try {
            // Create a ParcelFileDescriptor from the FileInputStream
            ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closePdfRenderer() {
        if (currentPage != null) {
            currentPage.close();
        }
        if (pdfRenderer != null) {
            pdfRenderer.close();
        }
    }

    private void displayPage(ImageView pdfImageView, int pageIndex) {
        if (pdfRenderer != null) {
            if (currentPage != null) {
                currentPage.close();
            }
            currentPage = pdfRenderer.openPage(pageIndex);
            Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(),
                    Bitmap.Config.ARGB_8888);
            currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            pdfImageView.setImageBitmap(bitmap);
        }
    }
}
