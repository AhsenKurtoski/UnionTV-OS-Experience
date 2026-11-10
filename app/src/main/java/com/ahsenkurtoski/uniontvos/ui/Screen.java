package com.ahsenkurtoski.uniontvos.ui;

import android.view.View;
import android.widget.FrameLayout;

import com.ahsenkurtoski.uniontvos.MainActivity;

/** Base class for every full-screen surface. */
public abstract class Screen extends FrameLayout {

    protected final MainActivity host;
    protected View initialFocus;

    private View lastFocus;

    public Screen(MainActivity host) {
        super(host);
        this.host = host;
        setClipChildren(false);
        setClipToPadding(false);
        setVisibility(GONE);
    }

    /** Called once the screen is visible: restores or places focus. */
    public void onShown() {
        View target = lastFocus;
        if (target == null || !target.isShown() || target.getParent() == null) {
            target = initialFocus;
        }
        if (target == null) {
            return;
        }
        final View focusTarget = target;

        // First attempt immediately, then once more after the first layout pass.
        // Custom-built TV layouts have no XML ids, so relying on the platform's
        // default focus hand-off is not reliable enough.
        post(new Runnable() {
            @Override
            public void run() {
                if (isShown()) {
                    focusTarget.requestFocus();
                }
            }
        });

        getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (isShown()) {
                    // The platform may have focused another control while the
                    // screen was being laid out; the intended target wins once.
                    focusTarget.requestFocus();
                }
            }
        });
    }

    /** Called before the screen is hidden: remembers focus for the return trip. */
    public void onHidden() {
        View focused = findFocus();
        if (focused != null && focused != this) {
            lastFocus = focused;
        }
    }

    public void onHostResume() {
    }

    public void onHostPause() {
    }

    /** Called whenever the application list or favourites change. */
    public void onAppsChanged() {
    }

    /** @return true when the screen consumed the back press. */
    public boolean handleBack() {
        return false;
    }

    protected void requestInitialFocus() {
        if (initialFocus != null) {
            initialFocus.requestFocus();
        }
    }

    // Views are not Contexts; these keep screen code readable.
    protected String getString(int resId) {
        return getContext().getString(resId);
    }

    protected String getString(int resId, Object... args) {
        return getContext().getString(resId, args);
    }
}
