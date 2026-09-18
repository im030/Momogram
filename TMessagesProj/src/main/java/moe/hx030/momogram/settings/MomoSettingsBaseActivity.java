package moe.hx030.momogram.settings;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.RecyclerListView;

import java.lang.reflect.Field;
import java.util.function.Consumer;

import moe.hx030.momogram.config.ConfigItem;
import moe.hx030.momogram.util.ReflectUtil;
import moe.hx030.momogram.config.CellGroup;
import moe.hx030.momogram.config.cell.AbstractConfigCell;
import moe.hx030.momogram.config.cell.ConfigCellCustom;
import moe.hx030.momogram.utils.TelegramUtil;

public class MomoSettingsBaseActivity extends BaseFragment {

    protected BaseListAdapter listAdapter;
    protected RecyclerListView listView;
    protected final CellGroup cellGroup = new CellGroup(this);
    protected ValueAnimator highlightAnimator = null;
    protected View highlightView = null;


    private int previousIndex = -1;
    protected int scrollToIndex = -1;
    protected String scrollToKey = null;
    protected String scrollToString = null;
    protected int scrollToStringId = 0;
    protected void setScrollToIndex(int index, boolean schedule) {
        previousIndex = scrollToIndex;
        scrollToIndex = index;
        Log.d("030-?", String.format("setScrollTo %d, prev=%d", index, previousIndex));
        if (schedule) scheduleScrollToIndex();
    }

    protected int findScrollToIndex() {
        if (scrollToString == null && scrollToKey == null) {
            return -1;
        }
        for (int i = 0; i < cellGroup.rows.size(); ++i) {
            AbstractConfigCell c = cellGroup.rows.get(i);
            if (scrollToString != null && c instanceof ConfigCellCustom cell && scrollToStringId != 0 && cell.stringId == scrollToStringId) {
                return i;
            }

            if (scrollToKey != null) {
                if (ReflectUtil.hasField(c.getClass(), "bindConfig")) {
                    ConfigItem cfg = (ConfigItem) ReflectUtil.getFieldValue(c, "bindConfig");
                    if (cfg != null && scrollToKey.equals(cfg.key)) {
                        return i;
                    }
                }
                continue;
            }

            if (!ReflectUtil.hasField(c.getClass(), "title")) {
                continue;
            }
            String cmp = (String) ReflectUtil.getFieldValue(c, "title");
            if (scrollToString.equals(cmp)) {
                return i;
            }
        }
        return -1;
    }

    protected void resolveScrollToIndex() {
        int index = findScrollToIndex();
        if (index != -1 && index != scrollToIndex) {
            setScrollToIndex(index, false);
        }
    }

    protected void scheduleScrollToIndex() {
        resolveScrollToIndex();
        if (scrollToIndex == -1) return;
        if (listView == null || listAdapter == null) return;
        listView.post(() -> {
            if (listView == null || listAdapter == null) {
                return;
            }
            RecyclerView.LayoutManager layoutManager = listView.getLayoutManager();
            if (layoutManager == null) return;

            int itemCount = listAdapter != null ? listAdapter.getItemCount() : layoutManager.getItemCount();
            if (scrollToIndex < 0 || scrollToIndex >= itemCount) return;
            if (listView.getHeight() <= 0) {
                scheduleScrollToIndex();
                return;
            }

            if (layoutManager instanceof LinearLayoutManager lm) {
                lm.scrollToPositionWithOffset(scrollToIndex, 0);
            } else {
                listView.scrollToPosition(scrollToIndex);
            }
            listView.post(() -> setHighlightView(scrollToIndex));
        });
    }

    public MomoSettingsBaseActivity setScrollTo(String str, int stringId, boolean key) {
        if (str == null) return this;
        Log.d("030-?", String.format("find option for '%s', strId = %d, isKey = %s", str, stringId, key));
        if (key) {
            scrollToKey = str;
            scrollToString = null;
            scrollToStringId = 0;
        } else {
            scrollToKey = null;
            scrollToString = str;
            scrollToStringId = stringId;
        }
        resolveScrollToIndex();
        return this;
    }

    protected void updateRows() {
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void setHighlightView(View textView) {
        if (textView == null) return;
        if (highlightAnimator != null) highlightAnimator.end();
        highlightView = textView;
        final int start;
        if (textView instanceof TextView t) start = t.getCurrentTextColor();
        else start = ((SimpleTextView) textView).getTextColor();
        final int end = Color.CYAN;

        final Consumer<Integer> setColor = (textView instanceof TextView t) ?
                t::setTextColor : ((SimpleTextView) textView)::setTextColor;

        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), start, end);
        animator.setDuration(2000);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(3);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> setColor.accept((Integer) a.getAnimatedValue()));
        highlightAnimator = animator;
        animator.start();
    }

    private int lastHighlightIndex = -1;
    private void setHighlightView(int index) {
        if (index == -1) return;
        if (highlightAnimator != null) highlightAnimator.end();

        RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(index);
        if (holder == null) {
            // Try again after layout/scroll brings it into view
            listView.post(() -> setHighlightView(index));
            return;
        }

        View tv = null;
        View item = holder.itemView;
        if (item instanceof TextCheckCell) {
            tv = ((TextCheckCell) item).getTextView();
        } else if (item instanceof TextSettingsCell) {
            tv = ((TextSettingsCell) item).getTextView();
        } else if (item instanceof TextDetailSettingsCell) {
            tv = ((TextDetailSettingsCell) item).getTextView();
        } else if (item instanceof TextInfoPrivacyCell) {
            tv = ((TextInfoPrivacyCell) item).getTextView();
        } else {
            Field f = ReflectUtil.getField(item.getClass(), "textView");
            if (f != null) {
                try {
                    f.setAccessible(true);
                    tv = (TextView) f.get(item);
                } catch (Throwable ignore) {}
            }
        }

        if (tv != null) {
            setHighlightView(tv);
        } else {
            final int start = Theme.getColor(Theme.key_windowBackgroundWhite);
            final int end = Color.CYAN;
            highlightView = item;
            ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), start, end);
            animator.setDuration(2000);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(3);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(a -> item.setBackgroundColor((Integer) a.getAnimatedValue()));
            highlightAnimator = animator;
            animator.start();
        }
        lastHighlightIndex = index;
    }


    //impl ListAdapter
    protected abstract class BaseListAdapter extends RecyclerListView.SelectionAdapter {

        protected Context mContext;

        public BaseListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return cellGroup.rows.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            AbstractConfigCell a = cellGroup.rows.get(position);
            if (a != null) {
                return a.isEnabled();
            }
            return true;
        }

        @Override
        public int getItemViewType(int position) {
            AbstractConfigCell a = cellGroup.rows.get(position);
            if (a != null) {
                return a.getType();
            }
            return CellGroup.ITEM_TYPE_TEXT_DETAIL;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {}

        @Override
        public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
            super.onViewRecycled(holder);
            if (highlightAnimator != null && highlightView != null) {
                View item = holder.itemView;
                boolean same = (item == highlightView) || (highlightView.getParent() == item);
                if (same) {
                    highlightAnimator.end();
                    highlightView = null;
                }
            }
        }

        protected void checkScrollTo(int position, RecyclerView.ViewHolder holder, View textView, String currentText) {
            boolean highlight = (position == scrollToIndex);
            if (highlight) {
                if (scrollToKey != null) {
                    if (textView != null) {
                        setHighlightView(textView);
                    } else {
                        setHighlightView(position);
                    }
                    return;
                }
                if (textView == null && (holder.itemView instanceof TextSettingsCell c)) {
                    textView = c.getTextView();
                }
                if (textView == null) {
                    Field textViewField = ReflectUtil.getField(holder.itemView.getClass(), "textView");
                    if (textViewField != null) {
                        textViewField.setAccessible(true);
                        try {
                            Object textViewObject = textViewField.get(holder.itemView);
                            if (textViewObject instanceof View view) {
                                textView = view;
                                highlightView = view;
                            }
                        } catch (Throwable e) {
                            Log.e("030-?", "", e);
                        }
                    }
                }
                CharSequence cs;
                if (textView instanceof TextView t) cs = t.getText();
                else if (textView instanceof SimpleTextView st) cs = st.getText();
                else cs = ""; // unreachable
                String cmp = textView == null ? currentText : cs.toString();
                boolean ne = textView != null && !cmp.equals(scrollToString);
                if (textView == null || ne) {
                    if (scrollToString != null) {
                        for (int pos : new int[]{position - 1, position + 1}) {
                            if (pos < 0 || pos >= cellGroup.rows.size()) {
                                continue;
                            }
                            if (ReflectUtil.hasField(cellGroup.rows.get(pos).getClass(), "title")) {
                                String s = (String) ReflectUtil.getFieldValue(cellGroup.rows.get(pos), "title");
                                if (scrollToString.equals(s)) {
                                    setScrollToIndex(pos, true);
                                    return;
                                }
                            }
                        }
                    }
                }
            } else if (position == (scrollToIndex - 1) || position == (scrollToIndex + 1)) {
                CharSequence cs;
                if (textView instanceof TextView t) cs = t.getText();
                else if (textView instanceof SimpleTextView st) cs = st.getText();
                else cs = ""; // unreachable
                String cmp = textView == null ? currentText : cs.toString();
                boolean eq = textView != null && cmp.equals(scrollToString);
                highlight = (textView != null && eq);
                if (highlight) {
                    if (highlightAnimator != null && highlightAnimator.isStarted()) highlightAnimator.end();
                    setScrollToIndex(position, true);
                }
            }

            if (highlight) {
                if (textView != null) {
                    setHighlightView(textView);
                } else {
                    setHighlightView(position);
                }
            }
        }
    }
}
