package com.org.jzprinter.widget.CustomLinearLayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import com.org.jzprinter.R;

public class UniformSizeSpacingLinearLayout extends LinearLayoutCompat {

    private int spacing; // 间距属性

    public UniformSizeSpacingLinearLayout(Context context) {
        super(context);
    }

    public UniformSizeSpacingLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public UniformSizeSpacingLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // 从 XML 属性中读取 spacing 值
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.UniformSizeSpacingLinearLayout);
        spacing = a.getDimensionPixelSize(R.styleable.UniformSizeSpacingLinearLayout_uniform_spacing, 0);
        a.recycle();
    }

    public void setSpacing(int spacing) {
        this.spacing = spacing;
        requestLayout();
    }

    public int getSpacing() {
        return spacing;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int totalWidth = 0;
        int totalHeight = 0;
        int childState = 0;
        int childCount = getChildCount();
        int maxWidth = 0;
        int maxHeight = 0;

        // 测量子视图，计算最大宽度和最大高度
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;

            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            LinearLayoutCompat.LayoutParams lp = (LinearLayoutCompat.LayoutParams) child.getLayoutParams();

            if (getOrientation() == HORIZONTAL) {
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
            } else {
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            }

            childState = combineMeasuredStates(childState, child.getMeasuredState());
        }

        // 设置所有子视图的宽度或高度以确保一致
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;

            LinearLayoutCompat.LayoutParams lp = (LinearLayoutCompat.LayoutParams) child.getLayoutParams();

            if (getOrientation() == HORIZONTAL) {
                lp.height = maxHeight - lp.topMargin - lp.bottomMargin;
            } else {
                lp.width = maxWidth - lp.leftMargin - lp.rightMargin;
            }
        }

        if (getOrientation() == HORIZONTAL) {
            totalHeight = maxHeight + getPaddingTop() + getPaddingBottom();
            totalWidth = getPaddingLeft() + getPaddingRight();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) continue;
                LinearLayoutCompat.LayoutParams lp = (LinearLayoutCompat.LayoutParams) child.getLayoutParams();
                totalWidth += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
                if (i < childCount - 1) {
                    totalWidth += spacing;
                }
            }
        } else {
            totalWidth = maxWidth + getPaddingLeft() + getPaddingRight();
            totalHeight = getPaddingTop() + getPaddingBottom();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) continue;
                LinearLayoutCompat.LayoutParams lp = (LinearLayoutCompat.LayoutParams) child.getLayoutParams();
                totalHeight += child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
                if (i < childCount - 1) {
                    totalHeight += spacing;
                }
            }
        }

        // 设置测量的宽度和高度
        setMeasuredDimension(
                resolveSizeAndState(totalWidth, widthMeasureSpec, childState),
                resolveSizeAndState(totalHeight, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT)
        );
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getOrientation() == HORIZONTAL) {
            adjustHorizontalLayout();
        } else {
            adjustVerticalLayout();
        }
    }

    private void adjustHorizontalLayout() {
        int childCount = getChildCount();
        int currentX = getPaddingLeft();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;

            LinearLayoutCompat.LayoutParams lp = (LinearLayoutCompat.LayoutParams) child.getLayoutParams();
            currentX += lp.leftMargin;
            child.layout(currentX, getPaddingTop() + lp.topMargin, currentX + child.getMeasuredWidth(), getPaddingTop() + lp.topMargin + child.getMeasuredHeight());
            currentX += child.getMeasuredWidth() + lp.rightMargin + spacing;
        }
    }

    private void adjustVerticalLayout() {
        int childCount = getChildCount();
        int currentY = getPaddingTop();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;

            LinearLayoutCompat.LayoutParams lp = (LinearLayoutCompat.LayoutParams) child.getLayoutParams();
            currentY += lp.topMargin;
            child.layout(getPaddingLeft() + lp.leftMargin, currentY, getPaddingLeft() + lp.leftMargin + child.getMeasuredWidth(), currentY + child.getMeasuredHeight());
            currentY += child.getMeasuredHeight() + lp.bottomMargin + spacing;
        }
    }

    @Override
    protected LinearLayoutCompat.LayoutParams generateDefaultLayoutParams() {
        return new LinearLayoutCompat.LayoutParams(
                LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
                LinearLayoutCompat.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LinearLayoutCompat.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LinearLayoutCompat.LayoutParams(getContext(), attrs);
    }

    @Override
    protected LinearLayoutCompat.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return new LinearLayoutCompat.LayoutParams(lp);
    }
}
