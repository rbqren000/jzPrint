package com.org.jzprinter.ui.activity;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.org.jzprinter.R;
import com.google.zxing.Result;
import com.king.camera.scan.AnalyzeResult;
import com.king.camera.scan.CameraScan;
import com.king.camera.scan.analyze.Analyzer;
import com.king.zxing.BarcodeCameraScanActivity;
import com.king.zxing.DecodeConfig;
import com.king.zxing.DecodeFormatManager;
import com.king.zxing.analyze.QRCodeAnalyzer;

/**
 * 扫码页面，继承自 ZXingLite 3.x 的 BarcodeCameraScanActivity。
 * 只识别二维码，识别到后立即返回结果并关闭。
 */
public class ScanActivity extends BarcodeCameraScanActivity {

    public static final String EXTRA_RESULT = CameraScan.SCAN_RESULT;

    @Override
    public int getLayoutId() {
        return R.layout.activity_scan;
    }

    @Override
    public void initCameraScan(@NonNull CameraScan<Result> cameraScan) {
        super.initCameraScan(cameraScan);
        cameraScan.setPlayBeep(true);
    }

    @Nullable
    @Override
    public Analyzer<Result> createAnalyzer() {
        DecodeConfig config = new DecodeConfig();
        config.setHints(DecodeFormatManager.QR_CODE_HINTS);
        config.setMultiDecode(false);
        return new QRCodeAnalyzer(config);
    }

    @Override
    public void onScanResultCallback(@NonNull AnalyzeResult<Result> result) {
        getCameraScan().setAnalyzeImage(false);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT, result.getResult().getText());
        setResult(RESULT_OK, intent);
        finish();
    }
}
