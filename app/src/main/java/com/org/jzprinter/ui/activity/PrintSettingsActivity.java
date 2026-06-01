package com.org.jzprinter.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.org.jzprinter.R;
import com.org.jzprinter.databinding.ActivityPrintSettingsBinding;
import com.org.jzprinter.print.PrintEngine;
import com.org.jzprinter.print.PrintImagePreparer;
import com.org.jzprinter.utils.Storage.PreferencesUtils;

public class PrintSettingsActivity extends BaseActivity {

    private static final String KEY_ROTATION = "rotation_direction";
    private static final String KEY_ALIGNMENT = "vertical_alignment";

    private ActivityPrintSettingsBinding binding;

    public static Intent newIntent(Context context) {
        return new Intent(context, PrintSettingsActivity.class);
    }

    public static PrintImagePreparer.RotationDirection getRotationDirection(Context context) {
        int value = PreferencesUtils.getInt(context, KEY_ROTATION,
            PrintImagePreparer.RotationDirection.CW_90.getValue());
        return PrintImagePreparer.RotationDirection.fromValue(value);
    }

    public static PrintImagePreparer.VerticalAlignment getVerticalAlignment(Context context) {
        int value = PreferencesUtils.getInt(context, KEY_ALIGNMENT,
            PrintImagePreparer.VerticalAlignment.TOP.getValue());
        return PrintImagePreparer.VerticalAlignment.fromValue(value);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrintSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupStatusBarWithCustomColorResId(R.color.primary_blue);

        binding.commonAppBar.titleTextView.setText("打印设置");
        binding.commonAppBar.leftMenuLayout.setOnClickListener(v -> finish());

        binding.rgRotation.check(getRotationDirection(this) == PrintImagePreparer.RotationDirection.CW_90
            ? R.id.rbCw90 : R.id.rbCcw90);
        binding.rgAlignment.check(getVerticalAlignment(this) == PrintImagePreparer.VerticalAlignment.TOP
            ? R.id.rbAlignTop : R.id.rbAlignBottom);

        binding.rgRotation.setOnCheckedChangeListener((group, checkedId) -> {
            PrintImagePreparer.RotationDirection rotation = checkedId == R.id.rbCcw90
                ? PrintImagePreparer.RotationDirection.CCW_90
                : PrintImagePreparer.RotationDirection.CW_90;
            PreferencesUtils.putInt(this, KEY_ROTATION, rotation.getValue());
            applyToEngine();
        });

        binding.rgAlignment.setOnCheckedChangeListener((group, checkedId) -> {
            PrintImagePreparer.VerticalAlignment alignment = checkedId == R.id.rbAlignBottom
                ? PrintImagePreparer.VerticalAlignment.BOTTOM
                : PrintImagePreparer.VerticalAlignment.TOP;
            PreferencesUtils.putInt(this, KEY_ALIGNMENT, alignment.getValue());
            applyToEngine();
        });
    }

    private void applyToEngine() {
        PrintEngine engine = PrintEngine.getInstance();
        engine.setRotationDirection(getRotationDirection(this));
        engine.setVerticalAlignment(getVerticalAlignment(this));
    }
}
