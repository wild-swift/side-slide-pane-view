/*
 * Copyright (C) 2018 Wild Swift
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package name.wildswift.android.sideslidepanerow;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Scroller;

/**
 * Created by swift on 25.03.15.
 */
public class SlidePaneRowView extends FrameLayout implements GestureDetector.OnGestureListener{
    private View leftView;
    private View rightView;
    private View centerView;

    private boolean slideLeftEnabled = true;
    private boolean slideRightEnabled = true;

    private boolean leftViewHide;
    private boolean rightViewHide;
    private int centerViewOffset;

    private GestureDetector detector;
    private Scroller scroller;

    private Mode currentMode;
    private Runnable scrollerAction = new Runnable() {
        @Override
        public void run() {
            scroller.computeScrollOffset();
            moveCenterViewTo(scroller.getCurrX());
            if (!scroller.isFinished()) {
                post(this);
            } else if (onSideOpenListener != null){
                if (centerViewOffset == 0) {
                    onSideOpenListener.onClose(SlidePaneRowView.this);
                } else if (centerViewOffset > 0) {
                    onSideOpenListener.onLeftOpen(SlidePaneRowView.this, centerViewOffset);
                } else {
                    onSideOpenListener.onRightOpen(SlidePaneRowView.this, -centerViewOffset);
                }
            }
        }
    };

    private OnSideOpenListener onSideOpenListener = null;

    public SlidePaneRowView(Context context) {
        super(context);
        init();
    }

    public SlidePaneRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SlidePaneRowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SlidePaneRowView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        detector = new GestureDetector(getContext(), this, new Handler(Looper.getMainLooper()));
        scroller = new Scroller(getContext());
        setChildrenDrawingOrderEnabled(true);
    }

    public boolean isSlideLeftEnabled() {
        return slideLeftEnabled;
    }

    public void setSlideLeftEnabled(boolean slideLeftEnabled) {
        this.slideLeftEnabled = slideLeftEnabled;
    }

    public boolean isSlideRightEnabled() {
        return slideRightEnabled;
    }

    public void setSlideRightEnabled(boolean slideRightEnabled) {
        this.slideRightEnabled = slideRightEnabled;
    }

    public OnSideOpenListener getListener() {
        return onSideOpenListener;
    }

    public void setOnSideOpenListener(OnSideOpenListener onSideOpenListener) {
        this.onSideOpenListener = onSideOpenListener;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getChildCount() > 3) throw new IllegalArgumentException("SlidePaneRow can not contains more than 3 rows");
        leftView = rightView = centerView = null;
        for(int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            FrameLayout.LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            switch (layoutParams.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                case Gravity.CENTER_HORIZONTAL:
                    if (centerView != null) throw new IllegalArgumentException("Duplicate center view");
                    centerView = child;
                    break;
                case Gravity.LEFT:
                    if (leftView != null) throw new IllegalArgumentException("Duplicate left view");
                    leftView = child;
                    break;
                case Gravity.RIGHT:
                    if (rightView != null) throw new IllegalArgumentException("Duplicate right view");
                    rightView = child;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown view gravity");

            }
        }
        super.onLayout(changed, l, t, r, b);

        if (leftView != null)
            leftView.offsetLeftAndRight(-getWidth());
        if (rightView != null) {
            rightView.offsetLeftAndRight(getWidth());
        }

        leftViewHide = true;
        rightViewHide = true;
        moveViewToAnchorPosition();
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (i == childCount - 1) return indexOfChild(centerView);
        if (i == 0) return leftView != null?indexOfChild(leftView):indexOfChild(rightView);
        if (i == 1) return indexOfChild(rightView);
        return -1;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean b = detector.onTouchEvent(ev);
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            return false;
        }
        return b;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean b = detector.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
            if(!scroller.isFinished()) return true;
            if (moveViewToAnchorPosition()) return true;

        }
        return b;
    }

    private boolean moveViewToAnchorPosition() {
        if (centerViewOffset == 0) return true;
        if (centerViewOffset < 0) {
            if (rightView == null || centerViewOffset > - rightView.getWidth() / 2) {
                scroller.startScroll(centerViewOffset, 0, -centerViewOffset, 0, 100);
                post(scrollerAction);
                return true;
            }
            if (- rightView.getWidth() - centerViewOffset != 0) {
                scroller.startScroll(centerViewOffset, 0, - rightView.getWidth() - centerViewOffset, 0, 100);
                post(scrollerAction);
            }
        }
        if (centerViewOffset > 0) {
            if (leftView == null || centerViewOffset < leftView.getWidth() / 2) {
                scroller.startScroll(centerViewOffset, 0, -centerViewOffset, 0, 100);
                post(scrollerAction);
                return true;
            }
            if (leftView.getWidth() - centerViewOffset != 0) {
                scroller.startScroll(centerViewOffset, 0, leftView.getWidth() - centerViewOffset, 0, 100);
                post(scrollerAction);
            }
        }
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        scroller.abortAnimation();
        currentMode = null;
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        requestDisallowInterceptTouchEvent(false);
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (currentMode == null) {
            if (Math.abs(distanceY) > Math.abs(distanceX)) {
                currentMode = Mode.none;
            } else if (centerViewOffset < 0 || centerViewOffset == 0 && distanceX > 0) {
                if (rightView != null && slideLeftEnabled) {
                    currentMode = Mode.rightOpen;
                    requestDisallowInterceptTouchEvent(true);
                    if (rightViewHide) {
                        rightView.offsetLeftAndRight(-getWidth());
                        rightViewHide = false;
                    }
                    if (!leftViewHide) {
                        leftView.offsetLeftAndRight(-getWidth());
                        leftViewHide = true;
                    }
                } else {
                    currentMode = Mode.none;
                }
            } else {
                if (leftView != null && slideRightEnabled) {
                    currentMode = Mode.leftOpen;
                    requestDisallowInterceptTouchEvent(true);
                    if (leftViewHide) {
                        leftView.offsetLeftAndRight(getWidth());
                        leftViewHide = false;
                    }
                    if (!rightViewHide) {
                        rightView.offsetLeftAndRight(getWidth());
                        rightViewHide = true;
                    }
                } else {
                    currentMode = Mode.none;
                }
            }
        }
        if (currentMode == Mode.none) return false;

        int tmpCenterViewOffset = centerViewOffset - (int) distanceX;

        moveCenterViewTo(tmpCenterViewOffset);
        return true;
    }

    private void moveCenterViewTo(int tmpCenterViewOffset) {
        if (currentMode == Mode.none) return;

        if (currentMode == Mode.rightOpen) {
            tmpCenterViewOffset = Math.max(-rightView.getWidth(), Math.min(tmpCenterViewOffset, 0));
        }
        if (currentMode == Mode.leftOpen) {
            tmpCenterViewOffset = Math.min(leftView.getWidth(), Math.max(tmpCenterViewOffset, 0));
        }

        centerView.offsetLeftAndRight(tmpCenterViewOffset - centerViewOffset);
        centerViewOffset = tmpCenterViewOffset;

        invalidate();
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (currentMode == null) {
            if (Math.abs(velocityY) > Math.abs(velocityX)) {
                currentMode = Mode.none;
            } else if (centerViewOffset < 0 || centerViewOffset == 0 && velocityX > 0) {
                if (rightView != null) {
                    currentMode = Mode.rightOpen;
                    requestDisallowInterceptTouchEvent(true);
                    if (rightViewHide) {
                        rightView.offsetLeftAndRight(-getWidth());
                        rightViewHide = false;
                    }
                    if (!leftViewHide) {
                        leftView.offsetLeftAndRight(-getWidth());
                        leftViewHide = true;
                    }
                } else {
                    currentMode = Mode.none;
                }
            } else {
                if (leftView != null) {
                    requestDisallowInterceptTouchEvent(true);
                    currentMode = Mode.leftOpen;
                    if (leftViewHide) {
                        leftView.offsetLeftAndRight(getWidth());
                        leftViewHide = false;
                    }
                    if (!rightViewHide) {
                        rightView.offsetLeftAndRight(getWidth());
                        rightViewHide = true;
                    }
                } else {
                    currentMode = Mode.none;
                }
            }
        }
        if (currentMode == Mode.none) return false;

        if (currentMode == Mode.rightOpen) {
            if (velocityX < 0) {
                scroller.startScroll(centerViewOffset, 0, -rightView.getWidth() - centerViewOffset, 0, 200);
                post(scrollerAction);
            } else {
                scroller.startScroll(centerViewOffset, 0, -centerViewOffset, 0, 200);
                post(scrollerAction);
            }
        } else {
            if (velocityX < 0) {
                scroller.startScroll(centerViewOffset, 0, -centerViewOffset, 0, 200);
                post(scrollerAction);
            } else {
                scroller.startScroll(centerViewOffset, 0, leftView.getWidth()-centerViewOffset, 0, 200);
                post(scrollerAction);
            }
        }

        return true;
    }

    private enum Mode {
        none, leftOpen, rightOpen
    }

    public interface OnSideOpenListener {
        void onLeftOpen(SlidePaneRowView owner, int size);
        void onRightOpen(SlidePaneRowView owner, int size);
        void onClose(SlidePaneRowView owner);
    }
}
