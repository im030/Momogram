package moe.hx030.momogram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
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
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import moe.hx030.momogram.MomoConfig;
import moe.hx030.momogram.NekoXConfig;
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
import moe.hx030.momogram.database.NitritesKt;
import moe.hx030.momogram.transtale.Translator;
import moe.hx030.momogram.transtale.source.FirefoxLocalTranslator;
import moe.hx030.momogram.ui.PopupBuilder;
import moe.hx030.momogram.util.ModUtil;
import moe.hx030.momogram.util.ReflectUtil;
import moe.hx030.momogram.util.SessionsUtil;
import moe.hx030.momogram.utils.FileUtil;
import moe.hx030.momogram.utils.ShareUtil;
import moe.hx030.momogram.utils.StrUtil;
import moe.hx030.momogram.utils.TelegramUtil;
import moe.hx030.momogram.utils.ZipUtil;

@SuppressLint("RtlHardcoded")
public class MomoAntiSpamSettingsActivity extends MomoSettingsBaseActivity {

    private final AbstractConfigCell headerGeneral = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.General)));
    private final AbstractConfigCell autoArchiveAndMuteRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.autoArchiveAndMute, LocaleController.getString(R.string.AutoArchiveAndMuteAbout)));
    private final AbstractConfigCell autoArchiveAndMuteNoCommonGroupOnlyRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.autoArchiveAndMuteNoCommonGroupOnly, LocaleController.getString(R.string.AutoArchiveAndMuteNoCommonGroupOnlyAbout)));
    private final AbstractConfigCell ignoreAllReactionsRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.ignoreAllReactions));
    private final AbstractConfigCell ignoreBlockedRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.ignoreBlocked, LocaleController.getString(R.string.IgnoreBlockedAbout)));
    private final AbstractConfigCell muteBlockedFromGroupRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.muteBlockedFromGroup));
    private final AbstractConfigCell muteBotsFromGroupRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.muteBotsFromGroup, LocaleController.getString(R.string.MuteBotsFromGroupDesc)));
    private final AbstractConfigCell dividerGeneral = cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell headerAdmins = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.ChannelAdministrators)));
    private final AbstractConfigCell autoBanDelAccFromReqRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.autoDismissJoinReq, LocaleController.getString(R.string.AutoDismissJoinReqDesc)));
    private final AbstractConfigCell autoBanAccNameRegexRow = cellGroup.appendCell(new ConfigCellTextInput(null, MomoConfig.autoDismissRegex, null));
    private final AbstractConfigCell autoBanDelAccBioFromReqRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.autoDismissJoinReqBio));
    private final AbstractConfigCell autoBanNameUseOpenCCRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.autoDismissNameUseOpenCC));
    private final AbstractConfigCell autoDismissDummyRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.autoDismissDummy));
    private final AbstractConfigCell autoDismissSuggestedChatsRow = cellGroup.appendCell(new ConfigCellSelectBox(LocaleController.getString(R.string.AutoDismissSuggestedChats),
            MomoConfig.autoDismissSuggestedChats, MomoConfig.autoDismissSuggestedChatsOptions, null));
    private final AbstractConfigCell triggerFilterRow = cellGroup.appendCell(new ConfigCellSelectBox(LocaleController.getString(R.string.TriggerFilterForAllStoredChats), null, null,
            () -> ModUtil.filterJoinRequestsJob(false)));
    private final AbstractConfigCell dividerAdmins = cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell headerDebug = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.DebugMenu)));
    private final AbstractConfigCell debugAntiSpamRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.debugAntiSpam));
    private final AbstractConfigCell dividerDebug = cellGroup.appendCell(new ConfigCellDivider());

    private UndoView tooltip;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        updateRows();

        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.AntiSpamSettings));

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

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);

        // Fragment: Set OnClick Callbacks
        listView.setOnItemClickListener((view, position, x, y) -> {
            AbstractConfigCell a = cellGroup.rows.get(position);
            if (a instanceof ConfigCellTextCheck) {
                if (position == cellGroup.rows.indexOf(debugAntiSpamRow) && !MomoConfig.debugAntiSpam.Bool()) {
                    AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                            .setTitle(StrUtil.getAppName())
                            .setMessage(LocaleController.getString(R.string.DebugWarning))
                            .setPositiveButton(LocaleController.getString(R.string.OK), (__, ___) -> {
                                ((ConfigCellTextCheck) a).onClick((TextCheckCell) view);
                            })
                            .setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                    if (!NekoXConfig.isDeveloper() || MomoConfig.tempDebug.Bool()) {
                        dlg.setTimeout(5, DialogInterface.BUTTON_POSITIVE);
                    }
                    dlg.show();
                } else if (position == cellGroup.rows.indexOf(autoBanDelAccFromReqRow)) {
                    ((ConfigCellSelectBox) triggerFilterRow).setEnabled(!MomoConfig.autoDismissJoinReq.Bool());
                    ((ConfigCellTextCheck) a).onClick((TextCheckCell) view);
                } else {
                    ((ConfigCellTextCheck) a).onClick((TextCheckCell) view);
                }
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
            } else if (a instanceof ConfigCellCustom) { // Custom onclick
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
            if (key.equals(MomoConfig.mediaPreview.getKey())) {
                if ((boolean) newValue) {
                    tooltip.setInfoText(AndroidUtilities.replaceTags(LocaleController.formatString(R.string.BetaWarning)));
                    tooltip.showWithAction(0, UndoView.ACTION_CACHE_WAS_CLEARED, null, null);
                }
            } else if (key.equals(MomoConfig.enableStickerPin.getKey())) {
                if ((boolean) newValue) {
                    tooltip.setInfoText(AndroidUtilities.replaceTags(LocaleController.formatString(R.string.EnableStickerPinTip)));
                    tooltip.showWithAction(0, UndoView.ACTION_CACHE_WAS_CLEARED, null, null);
                }
            } else if (key.equals(MomoConfig.useCustomEmoji.getKey())) {
                // Check
                if (!(boolean) newValue) {
                    tooltip.showWithAction(0, UndoView.ACTION_NEED_RESTART, null, null);
                    return;
                }
                MomoConfig.useCustomEmoji.setConfigBool(false);

                // Open picker
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                Activity act = getParentActivity();
                act.startActivityFromChild(act, intent, 114);
            } else if (key.equals(MomoConfig.allowDupLogin.getKey())) {
                if ((boolean) newValue) {
                    new AlertDialog.Builder(context)
                            .setTitle(StrUtil.getAppName())
                            .setMessage(LocaleController.getString(R.string.AllowDupLoginInfo))
                            .setPositiveButton(LocaleController.getString(R.string.OK), null)
                            .show();
                }
            }
        };

        //Cells: Set ListAdapter
        cellGroup.setListAdapter(listView, listAdapter);

        tooltip = new UndoView(context);
        frameLayout.addView(tooltip, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));

        scheduleScrollToIndex();

        return fragmentView;
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == 114 && resultCode == Activity.RESULT_OK) {
            try {
                // copy emoji zip
                Uri uri = data.getData();
                String zipPath = MediaController.copyFileToCache(uri, "file");

                if (zipPath == null || zipPath.isEmpty()) {
                    throw new Exception("zip copy failed");
                }

                //dirs
                File dir = new File(ApplicationLoader.applicationContext.getFilesDir(), "custom_emoji");
                if (dir.exists()) {
                    FileUtil.deleteDirectory(dir);
                }
                dir.mkdir();

                //process zip
                File zipFile = new File(zipPath);
                ZipUtil.unzip(new FileInputStream(zipFile), dir);
                zipFile.delete();
                if (!new File(ApplicationLoader.applicationContext.getFilesDir(), "custom_emoji/emoji/0_0.png").exists()) {
                    throw new Exception(LocaleController.getString(R.string.useCustomEmojiInvalid));
                }

                MomoConfig.useCustomEmoji.setConfigBool(true);
            } catch (Exception e) {
                FileLog.e(e);
                MomoConfig.useCustomEmoji.setConfigBool(false);
                Toast.makeText(ApplicationLoader.applicationContext, "Failed: " + e.toString(), Toast.LENGTH_LONG).show();
            }
            tooltip.showWithAction(0, UndoView.ACTION_NEED_RESTART, null, null);
//            listAdapter.notifyItemChanged(cellGroup.rows.indexOf(useCustomEmojiRow));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
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

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            AbstractConfigCell a = cellGroup.rows.get(position);
            SimpleTextView textView = null;
            String currentText = null;
            if (a != null) {
                if (a instanceof ConfigCellCustom) {
                } else {
                    // Default binds
                    a.onBindViewHolder(holder);
                }
                checkScrollTo(position, holder, textView, currentText);
            }
        }

    }
}