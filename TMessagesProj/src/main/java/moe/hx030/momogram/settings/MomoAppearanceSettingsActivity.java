
package moe.hx030.momogram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.text.TextPaint;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.Components.UndoView;

import java.lang.reflect.Field;
import java.util.ArrayList;

import moe.hx030.momogram.MomoConfig;
import moe.hx030.momogram.config.CellGroup;
import moe.hx030.momogram.config.ConfigItem;
import moe.hx030.momogram.config.cell.AbstractConfigCell;
import moe.hx030.momogram.config.cell.ConfigCellCustom;
import moe.hx030.momogram.config.cell.ConfigCellDivider;
import moe.hx030.momogram.config.cell.ConfigCellHeader;
import moe.hx030.momogram.config.cell.ConfigCellSelectBox;
import moe.hx030.momogram.config.cell.ConfigCellTextCheck;
import moe.hx030.momogram.config.cell.ConfigCellTextDetail;
import moe.hx030.momogram.config.cell.ConfigCellTextInput;
import moe.hx030.momogram.util.ReflectUtil;

@SuppressLint("RtlHardcoded")
public class MomoAppearanceSettingsActivity extends MomoSettingsBaseActivity {

    private ValueAnimator statusBarColorAnimator;

    // private final AbstractConfigCell largeAvatarInDrawerRow = cellGroup.appendCell(new ConfigCellSelectBox(null, MomoConfig.largeAvatarInDrawer, LocaleController.getString(R.string.valuesLargeAvatarInDrawer), null));
    // private final AbstractConfigCell avatarBackgroundDarkenRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.avatarBackgroundDarken));
    // private final AbstractConfigCell divider0 = cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell header1 = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.General)));
    private final AbstractConfigCell generateMonetThemeRow = cellGroup.appendCell(new ConfigCellSelectBox(LocaleController.getString(R.string.GenerateMonetTheme), null, null, () -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlertsCreator.createMonetThemeDialog(getParentActivity()).show();
        }
    }));
    private final AbstractConfigCell customTitleTextRow = cellGroup.appendCell(new ConfigCellTextInput(null, MomoConfig.customTitleText, "Momogram", null));
    private final AbstractConfigCell nameTitleTextRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.nameAsTitleText));
    private final AbstractConfigCell typefaceRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.typeface));
    private final AbstractConfigCell showSelfInsteadOfSavedMessagesRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.showSelfInsteadOfSavedMessages));
    private final AbstractConfigCell transparentStatusBarRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.transparentStatusBar));
    // private final AbstractConfigCell appBarShadowRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableAppBarShadow));
    // private final AbstractConfigCell avatarBackgroundBlurRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableAppBarShadow));
    private final AbstractConfigCell squareAvatarRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.squareAvatar));
    private final AbstractConfigCell hideBottomNavTabsRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.hideBottomNavTabs));
    private final AbstractConfigCell disableNumberRoundingRow = cellGroup.appendCell(new ConfigCellSelectBox(null, MomoConfig.numberRounding, MomoConfig.disableNumberRoundingOptions, null));
    private final AbstractConfigCell nameOrderRow = cellGroup.appendCell(new ConfigCellSelectBox(null, MomoConfig.nameOrder, new String[]{
            LocaleController.getString(R.string.LastFirst),
            LocaleController.getString(R.string.FirstLast)
    }, null));
    private final AbstractConfigCell usePersianCalendarRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.usePersianCalendar, LocaleController.getString(R.string.UsePersiancalendarInfo)));
    private final AbstractConfigCell displayPersianCalendarByLatinRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.displayPersianCalendarByLatin));
    private final AbstractConfigCell hideFABRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.hideFAB));
    private final AbstractConfigCell newYearRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.newYear));
    private final AbstractConfigCell actionBarDecorationRow = cellGroup.appendCell(new ConfigCellSelectBox(null, MomoConfig.actionBarDecoration, new String[]{
            LocaleController.getString(R.string.DependsOnDate),
            LocaleController.getString(R.string.Snowflakes),
            LocaleController.getString(R.string.Fireworks)
    }, null));
    private final AbstractConfigCell tabletModeRow = cellGroup.appendCell(new ConfigCellSelectBox(null, MomoConfig.tabletMode, new String[]{
            LocaleController.getString(R.string.TabletModeDefault),
            LocaleController.getString(R.string.Enable),
            LocaleController.getString(R.string.Disable)
    }, null));
    private final AbstractConfigCell divider1 = cellGroup.appendCell(new ConfigCellDivider());


    private final AbstractConfigCell header2 = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.MainTabsProfile)));
    private final AbstractConfigCell showIdAndDcRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.showIdAndDc));
    private final AbstractConfigCell hidePhoneRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.hidePhone));
    private final AbstractConfigCell boostedContactRatingInProfileRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.boostedContactRatingInProfile));
    private final AbstractConfigCell hideProfileRatingRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.hideProfileRating));
    private final AbstractConfigCell profileActionCircleBtnRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.profileActionCircleBtn));
    private final AbstractConfigCell showAddedToFoldersAtTitleTypeRow = cellGroup.appendCell(new ConfigCellSelectBox(LocaleController.getString(R.string.ShowAddedToFoldersAtTitle),
            MomoConfig.showAddedToFoldersAtTitleType, MomoConfig.titleFolderIconOptions, null));
    private final AbstractConfigCell enableAvatarBlurRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.enableAvatarBlur));
    private final AbstractConfigCell forceBlurInChatRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.forceBlurInChat));
    private final AbstractConfigCell header_chatblur = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.ChatBlurAlphaValue)));
    private final AbstractConfigCell chatBlurAlphaValueRow = cellGroup.appendCell(new ConfigCellCustom(ConfigCellCustom.CUSTOM_ITEM_CharBlurAlpha, MomoConfig.forceBlurInChat.Bool()));
    private final AbstractConfigCell divider2 = cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell header3 = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.Folder)));

    private final AbstractConfigCell showTabsOnForwardRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.showTabsOnForward));
    private final AbstractConfigCell ignoreMutedCountRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.ignoreMutedCount));
    private final AbstractConfigCell hideUnreadCounterOnFolderTabsRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.hideUnreadCounterOnFolderTabs));
    private final AbstractConfigCell pauseInactiveTabAnimationRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.pauseInactiveTabAnimation));
    private final AbstractConfigCell tabsTitleTypeRow = cellGroup.appendCell(new ConfigCellSelectBox(null, MomoConfig.tabsTitleType,
            new String[]{
                    LocaleController.getString(R.string.TabTitleTypeText),
                    LocaleController.getString(R.string.TabTitleTypeIcon),
                    LocaleController.getString(R.string.TabTitleTypeMix)
            }, null));
    private final AbstractConfigCell divider3 = cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell header4 = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.Chat)));
    private final AbstractConfigCell unreadBadgeOnBackButton = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.unreadBadgeOnBackButton));
    private final AbstractConfigCell smallerEmojiInChooserRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.smallerEmojiInChooser));
    private final AbstractConfigCell mediaPreviewRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.mediaPreview));
    private final AbstractConfigCell showSeconds = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.showSeconds));
    private final AbstractConfigCell labelChannelUserRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.labelChannelUser, LocaleController.getString(R.string.labelChannelUserDetails)));
    private final AbstractConfigCell alwaysLabelAnonAdminRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.alwaysLabelAnonAdmin));
    private final AbstractConfigCell hideSendAsChannelRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.hideSendAsChannel));
    private final AbstractConfigCell hideChannelBottomMuteUnmuteRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.hideChannelBottomMuteUnmute));
    private final AbstractConfigCell unroundedChatViewRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.unroundedChatView));
    private final AbstractConfigCell removeChatViewPaddingRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.removeChatViewPadding));
    private final AbstractConfigCell showEditTimeInPopupMenuRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.showEditTimeInPopupMenu));
    private final AbstractConfigCell showForwardTimeInPopupMenuRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.showForwardTimeInPopupMenu));
    private final AbstractConfigCell marqueeForLongChatTitlesRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.marqueeForLongChatTitles));
    private final AbstractConfigCell marqueeForLongMomoOptionsRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.marqueeForLongMomoOptions));
    private final AbstractConfigCell disableCustomWallpaperUserRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableCustomWallpaperUser));
    private final AbstractConfigCell disableCustomWallpaperChannelRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableCustomWallpaperChannel));
    private final AbstractConfigCell appendOriginalTimestampRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.appendOriginalTimestamp));
    private final AbstractConfigCell forceHideShowAsListRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.forceHideShowAsList));
    private final AbstractConfigCell largerImageMessageRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.imageMessageSizeTweak));
    private final AbstractConfigCell showChannelMsgFwdCountRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.showChannelMsgFwdCount));
    // private final AbstractConfigCell ignoreTopicTabViewRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.ignoreTopicTabView));
    private final AbstractConfigCell overrideForumStyleRow = cellGroup.appendCell(new ConfigCellSelectBox(null, MomoConfig.overrideForumStyle,
            MomoConfig.overrideForumStyleOptions, null));
    private final AbstractConfigCell useEmojiForEditedRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.useEmojiForEdited));
    private final AbstractConfigCell divider4 = cellGroup.appendCell(new ConfigCellDivider());

    private ChatBlurAlphaSeekBar chatBlurAlphaSeekbar;
    private UndoView restartTooltip;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        updateRows(true);

        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.AppearanceSettings));

        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        // Before listAdapter
        setCanNotChange();

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        if (listView.getItemAnimator() != null) {
            ((DefaultItemAnimator) listView.getItemAnimator()).setSupportsChangeAnimations(false);
        }
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);

        // Fragment: Set OnClick Callbacks
        listView.setOnItemClickListener((view, position, x, y) -> {
            AbstractConfigCell a = cellGroup.rows.get(position);
            if (a instanceof ConfigCellTextCheck) {
                ((ConfigCellTextCheck) a).onClick((TextCheckCell) view);
            } else if (a instanceof ConfigCellSelectBox) {
                ((ConfigCellSelectBox) a).onClick(view);
            } else if (a instanceof ConfigCellTextInput) {
                ((ConfigCellTextInput) a).onClick();
            } else if (a instanceof ConfigCellTextDetail) {
                RecyclerListView.OnItemClickListener o = ((ConfigCellTextDetail) a).onItemClickListener;
                if (o != null) {
                    try {
                        o.onItemClick(view, position);
                    } catch (Exception e) {
                    }
                }
            } else if (a instanceof ConfigCellCustom) { // Custom OnClick
            }
        });

        listView.setOnItemLongClickListener((v, i) -> {
            AbstractConfigCell a = cellGroup.rows.get(i);
            if (ReflectUtil.hasField(a.getClass(), "bindConfig")) {
                Field cfgField = ReflectUtil.getField(a.getClass(), "bindConfig");
                try {
                    if (cfgField != null) {
                        ConfigItem cfg = (ConfigItem) cfgField.get(a);
                        String key = (cfg == null) ? null : cfg.key;

                        if (key == null) return false;
                        if (AndroidUtilities.addToClipboard(String.format("https://t.me/momosettings/?k=%s", key))) {
                            BulletinFactory.of(this)
                                    .createCopyBulletin(getString(R.string.LinkCopied))
                                    .show();
                        }
                    }
                } catch (IllegalAccessException e) {
                    Log.e("030-cfg", "failed to get config field", e);
                }
            }
            return false;
        });

        // Cells: Set OnSettingChanged Callbacks
        cellGroup.callBackSettingsChanged = (key, newValue) -> {
            if (key.equals(MomoConfig.transparentStatusBar.getKey())) {
                restartTooltip.showWithAction(0, UndoView.ACTION_NEED_RESTART, null, null);
            } else if (key.equals(MomoConfig.actionBarDecoration.getKey())) {
                restartTooltip.showWithAction(0, UndoView.ACTION_NEED_RESTART, null, null);
            } else if (key.equals(MomoConfig.hideBottomNavTabs.getKey())) {
                restartTooltip.showWithAction(0, UndoView.ACTION_NEED_RESTART, null, null);
            } else if (key.equals(MomoConfig.tabletMode.getKey())) {
                // default or enable = set force disable to false, otherwise true
                if ((MomoConfig.tabletMode.Int() < 2 && SharedConfig.forceDisableTabletMode) ||
                        (MomoConfig.tabletMode.Int() == 2 && !SharedConfig.forceDisableTabletMode)) {
                    SharedConfig.toggleForceDisableTabletMode();
                }
                restartTooltip.showWithAction(0, UndoView.ACTION_NEED_RESTART, null, null);
            } else if (key.equals(MomoConfig.newYear.getKey())) {
                restartTooltip.showWithAction(0, UndoView.ACTION_NEED_RESTART, null, null);
            } else if (key.equals(MomoConfig.usePersianCalendar.getKey())) {
                restartTooltip.showWithAction(0, UndoView.ACTION_NEED_RESTART, null, null);
            } else if (key.equals(MomoConfig.displayPersianCalendarByLatin.getKey())) {
                restartTooltip.showWithAction(0, UndoView.ACTION_NEED_RESTART, null, null);
            } else if (key.equals(MomoConfig.nameOrder.getKey())) {
                LocaleController.getInstance().recreateFormatters();
            } else if (key.equals(MomoConfig.showSeconds.getKey())) {
                restartTooltip.showWithAction(0, UndoView.ACTION_NEED_RESTART, null, null);
            } else if (key.equals(MomoConfig.hideUnreadCounterOnFolderTabs.getKey())) {
                restartTooltip.showWithAction(0, UndoView.ACTION_NEED_RESTART, null, null);
            } else if (key.equals(MomoConfig.largeAvatarInDrawer.getKey())) {
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                setCanNotChange();
                listAdapter.notifyDataSetChanged();
            } else if (key.equals(MomoConfig.avatarBackgroundBlur.getKey())) {
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            } else if (key.equals(MomoConfig.avatarBackgroundDarken.getKey())) {
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            } else if (key.equals(MomoConfig.disableAppBarShadow.getKey())) {
                ActionBarLayout.headerShadowDrawable = (boolean) newValue ? null : parentLayout.getParentActivity().getResources().getDrawable(R.drawable.header_shadow).mutate();
                parentLayout.rebuildFragments(INavigationLayout.REBUILD_FLAG_REBUILD_LAST | INavigationLayout.REBUILD_FLAG_REBUILD_ONLY_LAST);
            } else if (MomoConfig.forceBlurInChat.getKey().equals(key)) {
                boolean enabled = (Boolean) newValue;
                if (chatBlurAlphaSeekbar != null)
                    chatBlurAlphaSeekbar.setEnabled(enabled);
                ((ConfigCellCustom) chatBlurAlphaValueRow).enabled = enabled;
            } else if (key.equals(MomoConfig.nameAsTitleText.getKey())) {
                restartTooltip.showWithAction(0, UndoView.ACTION_NEED_RESTART, null, null);
            } else if (key.equals(MomoConfig.hidePhone.getKey())) {
                parentLayout.rebuildAllFragmentViews(false, false);
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            } else if (key.equals(MomoConfig.tabsTitleType.getKey())) {
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            }
        };

        //Cells: Set ListAdapter
        cellGroup.setListAdapter(listView, listAdapter);

        restartTooltip = new UndoView(context);
        frameLayout.addView(restartTooltip, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));

        if (Build.VERSION.SDK_INT < 31) cellGroup.rows.remove(generateMonetThemeRow);

        scheduleScrollToIndex();

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void updateRows(boolean notify) {
        if (notify && listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{EmptyCell.class, TextSettingsCell.class, TextCheckCell.class, HeaderCell.class, TextDetailSettingsCell.class, NotificationsCheckCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_avatar_actionBarIconBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_avatar_actionBarSelectorBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUBACKGROUND, null, null, null, null, Theme.key_actionBarDefaultSubmenuBackground));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM, null, null, null, null, Theme.key_actionBarDefaultSubmenuItem));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));

        return themeDescriptions;
    }

    //impl ListAdapter
    private class ListAdapter extends BaseListAdapter {


        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            AbstractConfigCell a = cellGroup.rows.get(position);
            SimpleTextView textView = null;
            String currentText = null;
            if (a != null) {
                if (a instanceof ConfigCellCustom) {
                    // Custom binds
                    if (holder.itemView instanceof TextCheckCell) {
                        TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                        textView = checkCell.getTextView();
                        currentText = textView.getText().toString();
                    }
                    if (currentText != null && currentText.equals(scrollToString)) {
                        int index = holder.getAdapterPosition();
                        if (index != scrollToIndex) {
                            setScrollToIndex(index, true);
                        }
                    }
                } else {
                    // Default binds
                    a.onBindViewHolder(holder);
                }
                // Other things
                checkScrollTo(position, holder, textView, currentText);
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;
            switch (viewType) {
                case CellGroup.ITEM_TYPE_DIVIDER:
                    view = new ShadowSectionCell(mContext);
                    break;
                case CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case CellGroup.ITEM_TYPE_TEXT_CHECK:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case CellGroup.ITEM_TYPE_HEADER:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case CellGroup.ITEM_TYPE_TEXT_DETAIL:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case CellGroup.ITEM_TYPE_TEXT:
                    view = new TextInfoPrivacyCell(mContext);
                    // view.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case ConfigCellCustom.CUSTOM_ITEM_CharBlurAlpha:
                    view = chatBlurAlphaSeekbar = new ChatBlurAlphaSeekBar(mContext);
                    chatBlurAlphaSeekbar.setEnabled(MomoConfig.forceBlurInChat.Bool());
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }
    }

    private void setCanNotChange() {
        // boolean enabled = MomoConfig.largeAvatarInDrawer.Int() > 0;
        // ((ConfigCellTextCheck) avatarBackgroundBlurRow).setEnabled(enabled);
        // ((ConfigCellTextCheck) avatarBackgroundDarkenRow).setEnabled(enabled);
    }

    private class ChatBlurAlphaSeekBar extends FrameLayout {

        private final SeekBarView sizeBar;
        private final TextPaint textPaint;
        private boolean enabled = true;

        public ChatBlurAlphaSeekBar(Context context) {
            super(context);

            setWillNotDraw(false);

            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(AndroidUtilities.dp(16));

            sizeBar = new SeekBarView(context);
            sizeBar.setReportChanges(true);
            sizeBar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
                @Override
                public void onSeekBarDrag(boolean stop, float progress) {
                    MomoConfig.chatBlurAlphaValue.setConfigInt(Math.min(255, (int) (255 * progress)));
                    invalidate();
                }

                @Override
                public void onSeekBarPressed(boolean pressed) {

                }
            });
            sizeBar.setOnTouchListener((v, event) -> !enabled);
            sizeBar.setProgress(MomoConfig.chatBlurAlphaValue.Int());
            addView(sizeBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.LEFT | Gravity.TOP, 9, 5, 43, 11));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            canvas.drawText(String.valueOf(MomoConfig.chatBlurAlphaValue.Int()), getMeasuredWidth() - AndroidUtilities.dp(39), AndroidUtilities.dp(28), textPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            sizeBar.setProgress((MomoConfig.chatBlurAlphaValue.Int() / 255.0f));
        }

        @Override
        public void invalidate() {
            super.invalidate();
            sizeBar.invalidate();
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            this.enabled = enabled;
            sizeBar.setAlpha(enabled ? 1.0f : 0.5f);
            textPaint.setAlpha((int) ((enabled ? 1.0f : 0.3f) * 255));
            this.invalidate();
        }
    }
}