package com.org.jzprinter.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;

import com.org.jzprinter.R;
import com.org.jzprinter.widget.PhotoView.PhotoView;

public class PreviewDialog extends Dialog {

    private final Bitmap bitmap;

    public PreviewDialog(Context context, Bitmap bitmap) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.bitmap = bitmap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_preview);

        PhotoView ivPreview = findViewById(R.id.ivPreview);
        ivPreview.setImageBitmap(bitmap);

        Button btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> dismiss());
    }
}
