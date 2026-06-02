package com.org.jzprinter.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.org.jzprinter.R;
import com.org.jzprinter.databinding.ActivityPrintSettingsBinding;
import com.org.jzprinter.print.PrintConfig;

public class PrintSettingsActivity extends BaseActivity {

    private ActivityPrintSettingsBinding binding;

    public static Intent newIntent(Context context) {
        return new Intent(context, PrintSettingsActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrintSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupStatusBarWithCustomColorResId(R.color.primary_blue);

        binding.commonAppBar.titleTextView.setText("打印设置");
        binding.commonAppBar.leftMenuLayout.setOnClickListener(v -> finish());

        boolean oddPageRight = PrintConfig.isOddPageOnRight(this);
        binding.rgBookLayout.check(oddPageRight ? R.id.rbOddOnRight : R.id.rbOddOnLeft);

        binding.rgBookLayout.setOnCheckedChangeListener((group, checkedId) -> {
            boolean oddOnRight = checkedId == R.id.rbOddOnRight;
            PrintConfig.setOddPageOnRight(this, oddOnRight);
        });
    }
}
