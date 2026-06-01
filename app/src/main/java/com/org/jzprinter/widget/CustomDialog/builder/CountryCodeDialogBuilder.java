package com.org.jzprinter.widget.CustomDialog.builder;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 国家代码选择对话框 Builder
 * 提供基础的国家/地区代码选择功能
 */
public class CountryCodeDialogBuilder {

    private final Context context;
    private OnCountryCodeListener listener;

    public interface OnCountryCodeListener {
        void onItemClick(Dialog dialog, CountryItem item);
    }

    public static class CountryItem {
        private final String countryName;
        private final String region;
        private final String phoneCode;

        public CountryItem(String countryName, String region, String phoneCode) {
            this.countryName = countryName;
            this.region = region;
            this.phoneCode = phoneCode;
        }

        public String getCountryName() { return countryName; }
        public String getRegion() { return region; }
        public String getPhoneCode() { return phoneCode; }
    }

    public CountryCodeDialogBuilder(Context context) {
        this.context = context;
    }

    public CountryCodeDialogBuilder onItemClick(OnCountryCodeListener listener) {
        this.listener = listener;
        return this;
    }

    public Dialog show() {
        Dialog dialog = new Dialog(context);

        List<CountryItem> countries = createDefaultCountries();
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(24, 24, 24, 24);

        TextView titleView = new TextView(context);
        titleView.setText("选择国家/地区");
        titleView.setTextSize(18);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 16);
        container.addView(titleView);

        EditText searchView = new EditText(context);
        searchView.setHint("搜索...");
        container.addView(searchView);

        LinearLayout listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        for (CountryItem item : countries) {
            TextView itemView = new TextView(context);
            itemView.setText(item.getCountryName() + " (" + item.getPhoneCode() + ")");
            itemView.setTextSize(15);
            itemView.setPadding(0, 12, 0, 12);
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(dialog, item);
                dialog.dismiss();
            });
            listContainer.addView(itemView);
        }
        container.addView(listContainer);

        dialog.setContentView(container);
        dialog.setCancelable(true);

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.gravity = Gravity.CENTER;
            lp.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85);
            window.setAttributes(lp);
        }

        dialog.show();
        return dialog;
    }

    private List<CountryItem> createDefaultCountries() {
        List<CountryItem> list = new ArrayList<>();
        list.add(new CountryItem("中国", "CN", "+86"));
        list.add(new CountryItem("美国", "US", "+1"));
        list.add(new CountryItem("英国", "GB", "+44"));
        list.add(new CountryItem("日本", "JP", "+81"));
        list.add(new CountryItem("韩国", "KR", "+82"));
        list.add(new CountryItem("德国", "DE", "+49"));
        list.add(new CountryItem("法国", "FR", "+33"));
        list.add(new CountryItem("澳大利亚", "AU", "+61"));
        list.add(new CountryItem("加拿大", "CA", "+1"));
        list.add(new CountryItem("新加坡", "SG", "+65"));
        return list;
    }
}
