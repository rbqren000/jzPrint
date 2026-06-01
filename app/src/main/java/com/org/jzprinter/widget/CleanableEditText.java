package com.org.jzprinter.widget;
 
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.text.Editable;
import android.text.TextWatcher; 
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import androidx.appcompat.widget.AppCompatEditText;
import com.org.jzprinter.R;
import com.mx.mxSdk.Utils.RBQLog;

import java.util.ArrayList;
import java.util.List;

public class CleanableEditText extends AppCompatEditText {
  private Drawable clearIcon;
  private Drawable unclearIcon;
  private boolean showIcon = true;
  private int iconSize;
  private int iconTapPadding;
  private final List<TextWatcher> customTextWatchers = new ArrayList<>();

  public CleanableEditText(Context context) {
    super(context);
    init(context, null);
  }

  public CleanableEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public CleanableEditText(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context, attrs);
  }

  private void init(Context context, AttributeSet attrs) {
    // 默认图标尺寸设为18dp（转换为像素）
    int defaultIconSize = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            18,
            getResources().getDisplayMetrics()
    );
    int defaultIconTapPadding = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            2,
            getResources().getDisplayMetrics()
    );

    if (attrs != null) {
      TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CleanableEditText);
      try {
        clearIcon = a.getDrawable(R.styleable.CleanableEditText_clearIcon);
        unclearIcon = a.getDrawable(R.styleable.CleanableEditText_unclearIcon);
        iconSize = a.getDimensionPixelSize(
                R.styleable.CleanableEditText_clearIconSize,
                defaultIconSize
        );
        showIcon = a.getBoolean(R.styleable.CleanableEditText_isShowClearIcon, true);
        iconTapPadding = a.getDimensionPixelSize(
                R.styleable.CleanableEditText_clearIconTapPadding,
                defaultIconTapPadding
        );
      } finally {
        a.recycle();
      }
    }

    initFocusListener();
    initTextWatcher();
    updateDrawable();
  }

  private void initFocusListener() {
    setOnFocusChangeListener(new OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        updateDrawable();
      }
    });
  }

  private void initTextWatcher() {
    addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        notifyBeforeTextChanged(s, start, count, after);
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        notifyTextChanged(s, start, before, count);
      }

      @Override
      public void afterTextChanged(Editable s) {
        updateDrawable();
        notifyAfterTextChanged(s);
      }
    });
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_UP) {
      RBQLog.i("【CleanableEditText】onTouchEvent isClearIconTapped:"+(isClearIconTapped(event)?"YES":"NO")+"; showIcon"+(showIcon?"YES":"NO")+"; clearIcon:"+(clearIcon != null?"YES":"NO"));
      if (isClearIconTapped(event) && showIcon && clearIcon != null) {
        setText("");
      }
    }
    return super.onTouchEvent(event);
  }

  private boolean isClearIconTapped(MotionEvent event) {
    int x = (int) event.getX();
    // 计算可点击区域，扩大图标点击有效范围
    int width = getWidth();
    int paddingEnd = getPaddingEnd();
    int iconStart = width - paddingEnd - iconSize - iconTapPadding;
    int iconRight = width - paddingEnd + iconTapPadding;
//    RBQLog.i("【CleanableEditText】width:"+width +"; paddingEnd:"+paddingEnd+"; x:"+x+"; iconStart:"+iconStart+"; iconRight:"+iconRight);
    return x >= iconStart && x <= iconRight;
  }

  private void updateDrawable() {
    if (!showIcon) {
      setCompoundDrawables(null, null, null, null);
      return;
    }

    Drawable rightDrawable = shouldShowClearIcon()
            ? getScaledDrawable(clearIcon)
            : getScaledDrawable(unclearIcon);

    setCompoundDrawables(null, null, rightDrawable, null);
  }

  private boolean shouldShowClearIcon() {
    return getText() != null && !getText().toString().isEmpty();
  }

  private Drawable getScaledDrawable(Drawable original) {
    if (original == null) return null;

    // 创建可缩放副本
    Drawable drawable = original.mutate();
    drawable.setBounds(0, 0, iconSize, iconSize);

    // 适配矢量图
    if (drawable instanceof VectorDrawable) {
      drawable.setTint(getCurrentHintTextColor());
    }

    return drawable;
  }

  // 文本变化事件代理
  private void notifyBeforeTextChanged(CharSequence s, int start, int count, int after) {
    for (TextWatcher watcher : customTextWatchers) {
      watcher.beforeTextChanged(s, start, count, after);
    }
  }

  private void notifyTextChanged(CharSequence s, int start, int before, int count) {
    for (TextWatcher watcher : customTextWatchers) {
      watcher.onTextChanged(s, start, before, count);
    }
  }

  private void notifyAfterTextChanged(Editable s) {
    for (TextWatcher watcher : customTextWatchers) {
      watcher.afterTextChanged(s);
    }
  }

  // 公有方法
  public void setShowIcon(boolean showIcon) {
    this.showIcon = showIcon;
    post(this::updateDrawable);
  }

  public void setClearIcon(Drawable drawable) {
    this.clearIcon = drawable;
    post(this::updateDrawable);
  }

  public void setUnclearIcon(Drawable drawable) {
    this.unclearIcon = drawable;
    post(this::updateDrawable);
  }

  public void setIconSize(int sizeInDp) {
    this.iconSize = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            sizeInDp,
            getResources().getDisplayMetrics()
    );
    post(this::updateDrawable);
  }

  public void setIconTapPadding(int paddingInDp) {
    this.iconTapPadding = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            paddingInDp,
            getResources().getDisplayMetrics()
    );
    post(this::updateDrawable);
  }

  public void addCustomTextWatcher(TextWatcher customTextWatcher) {
    if (customTextWatcher != null) {
      customTextWatchers.add(customTextWatcher);
    }
  }

  public void removeCustomTextWatcher(TextWatcher customTextWatcher) {
    customTextWatchers.remove(customTextWatcher);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    clearListeners();
  }

  private void clearListeners() {
    setOnFocusChangeListener(null);
    customTextWatchers.clear();
  }
}


