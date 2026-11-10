package com.ahsenkurtoski.uniontvos.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Minimal grid layout: fixed cell height, equal columns, one gap value.
 *
 * Written by hand instead of pulling in a grid library; measurement is a single
 * pass and layout is arithmetic, which matters on a 32-bit TV SoC.
 */
public class SimpleGrid extends ViewGroup {

    private int columns = 4;
    private int gap;
    private int cellHeight;

    public SimpleGrid(Context context) {
        super(context);
        gap = Theme.dp(12);
        cellHeight = Theme.dp(AppCard.HEIGHT_DP);
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setColumns(int columnCount) {
        columns = Math.max(1, columnCount);
        requestLayout();
    }

    public void setGap(int gapPx) {
        gap = Math.max(0, gapPx);
        requestLayout();
    }

    public void setCellHeight(int heightPx) {
        cellHeight = Math.max(1, heightPx);
        requestLayout();
    }

    public int columns() {
        return columns;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int count = getChildCount();
        int rows = count == 0 ? 0 : (count + columns - 1) / columns;
        int cellWidth = Math.max(1, (width - gap * (columns - 1)) / columns);
        int height = rows * cellHeight + Math.max(0, rows - 1) * gap;

        int childWidthSpec = MeasureSpec.makeMeasureSpec(cellWidth, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(cellHeight, MeasureSpec.EXACTLY);
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(childWidthSpec, childHeightSpec);
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = getWidth();
        int cellWidth = Math.max(1, (width - gap * (columns - 1)) / columns);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            int row = i / columns;
            int column = i % columns;
            int left = column * (cellWidth + gap);
            int top = row * (cellHeight + gap);
            View child = getChildAt(i);
            child.layout(left, top, left + cellWidth, top + cellHeight);
        }
    }
}
