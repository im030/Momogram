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
import android.widget.TextView;
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
import moe.hx030.momogram.config.ConfigItem;
import moe.hx030.momogram.util.ReflectUtil;
import moe.hx030.momogram.util.SessionsUtil;
import moe.hx030.momogram.MomoConfig;
import moe.hx030.momogram.NekoXConfig;
import moe.hx030.momogram.transtale.TranslateDb;
import moe.hx030.momogram.transtale.Translator;
import moe.hx030.momogram.transtale.source.FirefoxLocalTranslator;
import moe.hx030.momogram.ui.PopupBuilder;
import moe.hx030.momogram.utils.FileUtil;
import moe.hx030.momogram.utils.ShareUtil;
import moe.hx030.momogram.utils.StrUtil;
import moe.hx030.momogram.utils.TelegramUtil;
import moe.hx030.momogram.utils.ZipUtil;
import moe.hx030.momogram.config.CellGroup;
import moe.hx030.momogram.config.cell.AbstractConfigCell;
import moe.hx030.momogram.config.cell.*;

@SuppressLint("RtlHardcoded")
public class MomoExperimentalSettingsActivity extends MomoSettingsBaseActivity {
    private AnimatorSet animatorSet;

    private boolean sensitiveCanChange = false;
    private boolean sensitiveEnabled = false;

    private final AbstractConfigCell header1 = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.Experiment)));
    private final AbstractConfigCell bufferCleanerRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.bufferCleaner, LocaleController.getString(R.string.BufferCleanerDesc)));
    private final AbstractConfigCell useSystemEmojiRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.useSystemEmoji));
//    private final AbstractConfigCell useCustomEmojiRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.useCustomEmoji));
    private final AbstractConfigCell channelAliasRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.channelAlias));
    private final AbstractConfigCell chatNameOverrideRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.chatNameOverride, LocaleController.getString(R.string.ChatNameOverrideDesc)));

//    private final AbstractConfigCell smoothKeyboardRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.smoothKeyboard));
    private final AbstractConfigCell enhancedFileLoaderRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.enhancedFileLoader));
    private final AbstractConfigCell disableFilteringRow = cellGroup.appendCell(new ConfigCellCustom(CellGroup.ITEM_TYPE_TEXT_CHECK, true));
    //    private final NekomuraTGCell ignoreContentRestrictionsRow = addNekomuraTGCell(nkmrCells.new NekomuraTGTextCheck(MomoConfig.ignoreContentRestrictions, LocaleController.getString("IgnoreContentRestrictionsNotice")));
    private final AbstractConfigCell unlimitedFavedStickersRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.unlimitedFavedStickers, LocaleController.getString(R.string.UnlimitedFavoredStickersAbout)));
    private final AbstractConfigCell unlimitedPinnedDialogsRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.unlimitedPinnedDialogs, LocaleController.getString(R.string.UnlimitedPinnedDialogsAbout)));
    private final AbstractConfigCell enableStickerPinRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.enableStickerPin, LocaleController.getString(R.string.EnableStickerPinAbout)));
    private final AbstractConfigCell useMediaStreamInVoipRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.useMediaStreamInVoip));
    private final AbstractConfigCell customAudioBitrateRow = cellGroup.appendCell(new ConfigCellCustom(CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true));
    private final AbstractConfigCell fasterReconnectHackRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.fasterReconnectHack, LocaleController.getString(R.string.FasterReconnectHackAbout)));
    private final AbstractConfigCell showQuickReconnectRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.showQuickReconnect, LocaleController.getString(R.string.ShowQuickReconnectDesc)));
    private final AbstractConfigCell removePremiumAnnoyanceRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.removePremiumAnnoyance, LocaleController.getString(R.string.RemovePremiumAnnoyanceDesc)));
    private final AbstractConfigCell chatListFontSizeFollowChatRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.chatListFontSizeFollowChat));
    private final AbstractConfigCell alwaysDestroyPhotoViewerRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.alwaysDestroyPhotoViewer));
    private final AbstractConfigCell autoRestartOnLeakRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.autoRestartOnLeak, LocaleController.getString(R.string.AutoRestartOnLeakInfo)));
    private final AbstractConfigCell resumeAudioPlaybackOnLaunchRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.resumeAudioPlaybackOnLaunch));
    private final AbstractConfigCell aidlOnLaunchRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.aidlOnLaunch));
    private final AbstractConfigCell autoDecryptPGPMessagesRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.autoDecryptPGPMessages));
    private final AbstractConfigCell tempDebugRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.tempDebug));
    private final AbstractConfigCell divider0 = cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell header2 = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.OverrideSettings)));
    private final AbstractConfigCell overrideSettingBooleanRow = cellGroup.appendCell(new ConfigCellTextInput(null, MomoConfig.overrideSettingBoolean, LocaleController.getString(R.string.OverrideSettingHint), null, MomoConfig::applyOverriddenValue));
    private final AbstractConfigCell overrideSettingIntegerRow = cellGroup.appendCell(new ConfigCellTextInput(null, MomoConfig.overrideSettingInteger, LocaleController.getString(R.string.OverrideSettingHint), null, MomoConfig::applyOverriddenValue));
    private final AbstractConfigCell overrideSettingStringRow = cellGroup.appendCell(new ConfigCellTextInput(null, MomoConfig.overrideSettingString, LocaleController.getString(R.string.OverrideSettingHint), null, MomoConfig::applyOverriddenValue));
    private final AbstractConfigCell overrideSettingLongRow = cellGroup.appendCell(new ConfigCellTextInput(null, MomoConfig.overrideSettingLong, LocaleController.getString(R.string.OverrideSettingHint), null, MomoConfig::applyOverriddenValue));
    private final AbstractConfigCell overrideSettingFloatRow = cellGroup.appendCell(new ConfigCellTextInput(null, MomoConfig.overrideSettingFloat, LocaleController.getString(R.string.OverrideSettingHint), null, MomoConfig::applyOverriddenValue));
    private final AbstractConfigCell divider1 = cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell header3 = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.DebugMenu)));
    private final AbstractConfigCell allowDupLoginRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.allowDupLogin));
    private final AbstractConfigCell ignoreTranslatorCacheRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.ignoreTranslatorCache));
    private final AbstractConfigCell clearTranslatorCacheRow = cellGroup.appendCell(new ConfigCellSelectBox(LocaleController.getString(R.string.ClearTranslatorCache), null, null,
            () -> AndroidUtilities.runOnUIThread(() -> {
                TranslateDb.clearCache();
                TelegramUtil.restartApp(false);
            })));
    private final AbstractConfigCell shareApiIdsRow = cellGroup.appendCell(new ConfigCellSelectBox(LocaleController.getString(R.string.ShareCurrentKnownApiIds), null, null,
            () -> AndroidUtilities.runOnUIThread(() -> {
                Intent i = new Intent(Intent.ACTION_SEND);
                if (Build.VERSION.SDK_INT >= 24) {
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL, "");
                i.putExtra(Intent.EXTRA_SUBJECT, SessionsUtil.getSessionsString());
                i.setClass(getParentActivity(), LaunchActivity.class);
                getParentActivity().startActivity(i);
            })));
    private final AbstractConfigCell resetClientWarningsRow = cellGroup.appendCell(new ConfigCellSelectBox(LocaleController.getString(R.string.ResetDismissedClientWarnings), null, null,
            () -> AndroidUtilities.runOnUIThread(() -> {
                MomoConfig.warnedClients.setConfigString("");
                MomoConfig.prevSessionCheck.setConfigLong(0L);
            })));
    private final AbstractConfigCell dumpThreadsRow = cellGroup.appendCell(new ConfigCellSelectBox(LocaleController.getString(R.string.DumpThreads), null, null,
            () -> AndroidUtilities.runOnUIThread(() -> {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
                File cacheFile = new File(ApplicationLoader.applicationContext.getCacheDir(), timestamp + ".m0m0-threads.txt");
                FileUtil.writeUtf8String(FileLog.dumpThreads(false), cacheFile);
                ShareUtil.shareFile(getParentActivity(), cacheFile);
            })));
    private final AbstractConfigCell triggerCrashRow = cellGroup.appendCell(new ConfigCellSelectBox(LocaleController.getString(R.string.TriggerCrash), null, null,
            () -> AndroidUtilities.runOnUIThread(() -> { int[] arr = new int[0]; arr[1] = 0;})));
    private final AbstractConfigCell divider2 = cellGroup.appendCell(new ConfigCellDivider());

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
        actionBar.setTitle(LocaleController.getString(R.string.Experiment));

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

        if (!NekoXConfig.isDeveloper()) {
            cellGroup.rows.remove(tempDebugRow);
        }

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);

        // Fragment: Set OnClick Callbacks
        listView.setOnItemClickListener((view, position, x, y) -> {
            AbstractConfigCell a = cellGroup.rows.get(position);
            if (a instanceof ConfigCellTextCheck) {
                if (position == cellGroup.rows.indexOf(enhancedFileLoaderRow)) {
                    ConfigCellTextCheck check = ((ConfigCellTextCheck) a);
                    if (!check.cell.isChecked()) {
                        new AlertDialog.Builder(context)
                                .setTitle(LocaleController.getString(R.string.enhancedFileLoader))
                                .setMessage(LocaleController.getString(R.string.enhancedFileLoaderWarning))
                                .setPositiveButton(LocaleController.getString(R.string.OK), (__, ___) -> {
                                    ((ConfigCellTextCheck) a).onClick((TextCheckCell) view);
                                })
                                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                                .setTimeout(3, DialogInterface.BUTTON_POSITIVE)
                                .show();
                        return;
                    }
                } else if (position == cellGroup.rows.indexOf(aidlOnLaunchRow)) {
                    ConfigCellTextCheck check = ((ConfigCellTextCheck) a);
                    if (check.cell.isChecked() && MomoConfig.translationProvider.Int() == Translator.providerFirefox)
                        FirefoxLocalTranslator.INSTANCE.bind(false, new Continuation<>() {
                            @NonNull
                            @Override
                            public CoroutineContext getContext() {
                                return EmptyCoroutineContext.INSTANCE;
                            }

                            @Override
                            public void resumeWith(@NonNull Object o) {}
                        });
                }
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
            } else if (a instanceof ConfigCellCustom) { // Custom onclick
                if (position == cellGroup.rows.indexOf(disableFilteringRow)) {
                    sensitiveEnabled = !sensitiveEnabled;
                    TL_account.setContentSettings req = new TL_account.setContentSettings();
                    req.sensitive_enabled = sensitiveEnabled;
                    AlertDialog progressDialog = new AlertDialog(getParentActivity(), 3);
                    progressDialog.show();
                    getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                        progressDialog.dismiss();
                        if (error == null) {
                            if (response instanceof TLRPC.TL_boolTrue && view instanceof TextCheckCell) {
                                ((TextCheckCell) view).setChecked(sensitiveEnabled);
                            }
                        } else {
                            AndroidUtilities.runOnUIThread(() -> AlertsCreator.processError(currentAccount, error, this, req));
                        }
                    }));
                } else if (position == cellGroup.rows.indexOf(customAudioBitrateRow)) {
                    PopupBuilder builder = new PopupBuilder(view);
                    builder.setItems(new String[]{
                            "32 (" + LocaleController.getString(R.string.Default) + ")",
                            "64",
                            "128",
                            "192",
                            "256",
                            "320"
                    }, (i, __) -> {
                        switch (i) {
                            case 0:
                                MomoConfig.customAudioBitrate.setConfigInt(32);
                                break;
                            case 1:
                                MomoConfig.customAudioBitrate.setConfigInt(64);
                                break;
                            case 2:
                                MomoConfig.customAudioBitrate.setConfigInt(128);
                                break;
                            case 3:
                                MomoConfig.customAudioBitrate.setConfigInt(192);
                                break;
                            case 4:
                                MomoConfig.customAudioBitrate.setConfigInt(256);
                                break;
                            case 5:
                                MomoConfig.customAudioBitrate.setConfigInt(320);
                                break;
                        }
                        listAdapter.notifyItemChanged(position);
                        return Unit.INSTANCE;
                    });
                    builder.show();
                }
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
            checkSensitive();
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

    private void checkSensitive() {
        TL_account.getContentSettings req = new TL_account.getContentSettings();
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                TL_account.contentSettings settings = (TL_account.contentSettings) response;
                sensitiveEnabled = settings.sensitive_enabled;
                sensitiveCanChange = settings.sensitive_can_change;
                int count = listView.getChildCount();
                ArrayList<Animator> animators = new ArrayList<>();
                for (int a = 0; a < count; a++) {
                    View child = listView.getChildAt(a);
                    RecyclerListView.Holder holder = (RecyclerListView.Holder) listView.getChildViewHolder(child);
                    int position = holder.getAdapterPosition();
                    if (position == cellGroup.rows.indexOf(disableFilteringRow)) {
                        TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                        checkCell.setChecked(sensitiveEnabled);
                        checkCell.setEnabled(sensitiveCanChange, animators);
                        if (sensitiveCanChange) {
                            if (!animators.isEmpty()) {
                                if (animatorSet != null) {
                                    animatorSet.cancel();
                                }
                                animatorSet = new AnimatorSet();
                                animatorSet.playTogether(animators);
                                animatorSet.addListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animator) {
                                        if (animator.equals(animatorSet)) {
                                            animatorSet = null;
                                        }
                                    }
                                });
                                animatorSet.setDuration(150);
                                animatorSet.start();
                            }
                        }
                    }
                }
            } else {
                AndroidUtilities.runOnUIThread(() -> AlertsCreator.processError(currentAccount, error, this, req));
            }
        }));
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
                    // Custom binds
                    if (holder.itemView instanceof TextCheckCell) {
                        TextCheckCell textCell = (TextCheckCell) holder.itemView;
                        textCell.setEnabled(true, null);
                        if (position == cellGroup.rows.indexOf(disableFilteringRow)) {
                            textCell.setTextAndValueAndCheck(currentText = LocaleController.getString(R.string.SensitiveDisableFiltering), LocaleController.getString(R.string.SensitiveAbout), sensitiveEnabled, true, true);
                            textCell.setEnabled(sensitiveCanChange, null);
                        }
                        textView = textCell.getTextView();
                    } else if (holder.itemView instanceof TextSettingsCell) {
                        TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                        if (position == cellGroup.rows.indexOf(customAudioBitrateRow)) {
                            String value = String.valueOf(MomoConfig.customAudioBitrate.Int()) + "kbps";
                            if (MomoConfig.customAudioBitrate.Int() == 32)
                                value += " (" + LocaleController.getString(R.string.Default) + ")";
                            textCell.setTextAndValue(currentText = LocaleController.getString(R.string.customGroupVoipAudioBitrate), value, false);
                        }
                        textView = textCell.getTextView();
                    }
                    if (textView != null && currentText == null) currentText = textView.getText().toString();
                    if (currentText != null && currentText.equals(scrollToString)) {
                        int index = holder.getAdapterPosition();
                        if (index != scrollToIndex) {
                            setScrollToIndex(index, true);
                        }
                    }
                } else {
                    // Default binds
                    a.onBindViewHolder(holder);
//                    if (position == cellGroup.rows.indexOf(smoothKeyboardRow) && AndroidUtilities.isTablet()) {
//                        holder.itemView.setVisibility(View.GONE);
//                    }
                }
                checkScrollTo(position, holder, textView, currentText);
            }
        }

    }
}