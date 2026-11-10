package com.ahsenkurtoski.uniontvos.ui;

import android.content.Context;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * A titled horizontal rail. The scroll position follows the focused card, which
 * keeps navigation predictable without touching the remote's direction keys.
 */
public class RailView extends LinearLayout {

    public static final int HEIGHT_DP = 138;

    private final TextView titleView;
    private final TextView emptyView;
    private final HorizontalScrollView scroller;
    private final LinearLayout items;

    public RailView(Context context, CharSequence title) {
        super(context);
        setOrientation(VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);

        titleView = Ui.medium(context, title, 16f, Theme.TEXT);
        titleView.setLetterSpacing(0.01f);
        addView(titleView, Ui.lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        Ui.setMargins(titleView, Theme.dp(4), 0, 0, Theme.dp(10));

        scroller = new HorizontalScrollView(context);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setClipChildren(false);
        scroller.setClipToPadding(false);
        scroller.setOverScrollMode(OVER_SCROLL_NEVER);

        items = new LinearLayout(context);
        items.setOrientation(HORIZONTAL);
        items.setClipChildren(false);
        items.setClipToPadding(false);
        items.setPadding(Theme.dp(4), Theme.dp(2), Theme.dp(24), Theme.dp(6));
        scroller.addView(items, new HorizontalScrollView.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        addView(scroller, Ui.lp(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        emptyView = Ui.text(context, "", 12.5f, Theme.TEXT_FAINT);
        emptyView.setVisibility(GONE);
        addView(emptyView, Ui.lp(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        Ui.setMargins(emptyView, Theme.dp(4), Theme.dp(6), 0, Theme.dp(6));
    }

    public void setEmptyText(CharSequence text) {
        emptyView.setText(text);
    }

    public void setCards(List<? extends View> cards) {
        items.removeAllViews();
        if (cards == null || cards.isEmpty()) {
            scroller.setVisibility(GONE);
            emptyView.setVisibility(emptyView.getText().length() > 0 ? VISIBLE : GONE);
            return;
        }
        scroller.setVisibility(VISIBLE);
        emptyView.setVisibility(GONE);

        for (int i = 0; i < cards.size(); i++) {
            View card = cards.get(i);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    Theme.dp(AppCard.WIDTH_DP), Theme.dp(AppCard.HEIGHT_DP));
            if (i < cards.size() - 1) {
                params.rightMargin = Theme.dp(12);
            }
            items.addView(card, params);
            if (card instanceof FocusCard) {
                ((FocusCard) card).addFocusListener(new FocusCard.FocusListener() {
                    @Override
                    public void onCardFocus(FocusCard focusedCard, boolean hasFocus) {
                        if (hasFocus) {
                            Ui.scrollIntoView(scroller, focusedCard, Theme.dp(6), Theme.dp(28));
                        }
                    }
                });
            }
        }
    }

    public void setTitle(CharSequence title) {
        titleView.setText(title);
        titleView.setVisibility(title == null || title.length() == 0 ? GONE : VISIBLE);
    }

    public void setTitleVisible(boolean visible) {
        titleView.setVisibility(visible ? VISIBLE : GONE);
    }

    public HorizontalScrollView scroller() {
        return scroller;
    }

    /** Ensures the rail is scrolled back to its start after a rebuild. */
    public void scrollToStart() {
        scroller.scrollTo(0, 0);
    }

    public static LinearLayout.LayoutParams layoutParams(Context context) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, Theme.dp(HEIGHT_DP));
        params.topMargin = Theme.dp(18);
        return params;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // The rail owns its height so that hidden/empty states do not collapse
        // the layout rhythm of the home screen.
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height;
        if (heightMode == MeasureSpec.EXACTLY) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        } else {
            height = Theme.dp(HEIGHT_DP);
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }
}
