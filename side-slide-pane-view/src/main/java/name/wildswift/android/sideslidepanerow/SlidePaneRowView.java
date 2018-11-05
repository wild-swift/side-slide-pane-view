package name.wildswift.android.sideslidepanerow;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
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
            }
        }
    };


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
        detector = new GestureDetector(getContext(), this);
        scroller = new Scroller(getContext());
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
        centerViewOffset = 0;
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
}
