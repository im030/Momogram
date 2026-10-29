package moe.hx030.momogram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.whispertflite.utils.WhisperModelDownloader;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
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
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.Components.UndoView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import kotlin.Unit;
import moe.hx030.momogram.MomoConfig;
import moe.hx030.momogram.NekoXConfig;
import moe.hx030.momogram.config.ConfigItem;
import moe.hx030.momogram.ui.PopupBuilder;
import moe.hx030.momogram.config.CellGroup;
import moe.hx030.momogram.config.cell.AbstractConfigCell;
import moe.hx030.momogram.config.cell.ConfigCellCustom;
import moe.hx030.momogram.config.cell.ConfigCellDivider;
import moe.hx030.momogram.config.cell.ConfigCellHeader;
import moe.hx030.momogram.config.cell.ConfigCellSelectBox;
import moe.hx030.momogram.config.cell.ConfigCellTextCheck;
import moe.hx030.momogram.config.cell.ConfigCellTextDetail;
import moe.hx030.momogram.config.cell.ConfigCellTextInput;
import moe.hx030.momogram.helpers.WhisperHelper;
import moe.hx030.momogram.util.ReflectUtil;

@SuppressLint("RtlHardcoded")
public class MomoChatSettingsActivity extends MomoSettingsBaseActivity implements NotificationCenter.NotificationCenterDelegate {
    public final static String MSG_MENU_KEY = "msgMenu";
    public final static String PROFILE_MENU_KEY = "profileMenu";

    // Sticker Size
    private final AbstractConfigCell header0 = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.StickerSize)));
    private final AbstractConfigCell stickerSizeRow = cellGroup.appendCell(new ConfigCellCustom(ConfigCellCustom.CUSTOM_ITEM_StickerSize, true));
    private final AbstractConfigCell divider0 = cellGroup.appendCell(new ConfigCellDivider());

    // Chats
    private final AbstractConfigCell header1 = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.Chat)));

    private final AbstractConfigCell autoTranslateRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.autoTranslate));
    private final AbstractConfigCell autoTranslateProviderRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.useCustomProviderForAutoTranslate));
    private final AbstractConfigCell transcribeProviderRow = cellGroup.appendCell(new ConfigCellSelectBox(LocaleController.getString(R.string.TranscribeProvider),
            MomoConfig.transcribeProvider, MomoConfig.transcribeOptions, null));
    private final AbstractConfigCell cfCredentialsRow = cellGroup.appendCell(new ConfigCellCustom(CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true));
    private final AbstractConfigCell useSlowWhisperModelRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.useSlowWhisperModel, LocaleController.getString(R.string.UseSlowWhisperModelDesc)));
    private final AbstractConfigCell deleteUnusedModelRow = cellGroup.appendCell(new ConfigCellCustom(CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true));
    private final AbstractConfigCell sendCommentAfterForwardRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.sendCommentAfterForward));
    private final AbstractConfigCell useChatAttachMediaMenuRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.useChatAttachMediaMenu, LocaleController.getString(R.string.UseChatAttachEnterMenuNotice)));
    private final AbstractConfigCell disableLinkPreviewByDefaultRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableLinkPreviewByDefault, LocaleController.getString(R.string.DisableLinkPreviewByDefaultNotice)));
    private final AbstractConfigCell takeGIFasVideoRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.takeGIFasVideo));
    private final AbstractConfigCell chooseBestVideoQualityByDefaultRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.chooseBestVideoQualityByDefault));
    private final AbstractConfigCell showBottomActionsWhenSelectingRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.showBottomActionsWhenSelecting));
    private final AbstractConfigCell showSpoilersDirectlyRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.showSpoilersDirectly));
    private final AbstractConfigCell messageMenuRow = cellGroup.appendCell(new ConfigCellSelectBox(LocaleController.getString(R.string.MessageMenu), null, null, this::showMessageMenuAlert));
    private final AbstractConfigCell profileMenuRow = cellGroup.appendCell(new ConfigCellSelectBox(LocaleController.getString(R.string.ProfileMenu), null, null, this::showProfileMenuAlert));
    private final AbstractConfigCell alwaysShowBotCommandButtonRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.alwaysShowBotCommandButton));
    private final AbstractConfigCell alwaysHideBotCommandButtonRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.alwaysHideBotCommandButton));
    private final AbstractConfigCell alwaysUseSpoilerForMediaRow = cellGroup.appendCell(new ConfigCellTextInput(null, MomoConfig.alwaysUseSpoilerForMedia, LocaleController.getString(R.string.AlwaysUseSpoilerForMediaDesc), null, MomoConfig::updateUseSpoilerMediaChatList));
    private final AbstractConfigCell keepBlockedBotChatHistoryRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.keepBlockedBotChatHistory));
    private final AbstractConfigCell dontSendStartCmdOnUnblockBotRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.dontSendStartCmdOnUnblockBot));
    private final AbstractConfigCell alwaysLoadStickerSetFromServerRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.alwaysLoadStickerSetFromServer));
    private final AbstractConfigCell keepSamePositionOnNewMsgRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.keepSamePositionOnNewMsg));
    private final AbstractConfigCell disableSaveDraftToCloudRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableSaveDraftToCloud));
    private final AbstractConfigCell showVoteCountBeforeVoteRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.keepVoteCountAfterRetractVote));
    private final AbstractConfigCell randomizeFilenameOnSendRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.randomizeFilenameOnSend));
    private final AbstractConfigCell secretChatReqConfirmRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.secretChatReqConfirm));
    private final AbstractConfigCell hideAIEditButtonRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.hideAIEditButton));
    private final AbstractConfigCell hideMessageRegexRow = cellGroup.appendCell(new ConfigCellTextInput(null, MomoConfig.hideMessageRegex, null));
    private final AbstractConfigCell dividerChat = cellGroup.appendCell(new ConfigCellDivider());

    // Interactions
    private final AbstractConfigCell headerInteractions = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.InteractionSettings)));
    private final AbstractConfigCell hideKeyboardOnChatScrollRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.hideKeyboardOnChatScroll));
    private final AbstractConfigCell rearVideoMessagesRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.rearVideoMessages));
    private final AbstractConfigCell disableInstantCameraRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableInstantCamera));
    private final AbstractConfigCell hideCameraInAttachMenuRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.hideCameraInAttachMenu));
    private final AbstractConfigCell disableVibrationRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableVibration));
    private final AbstractConfigCell disableProximityEventsRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableProximityEvents));
    private final AbstractConfigCell disableTrendingRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableTrending));
    private final AbstractConfigCell disableSwipeToNextRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableSwipeToNext));
    private final AbstractConfigCell disablePhotoSideActionRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disablePhotoSideAction));
    private final AbstractConfigCell disableRemoteEmojiInteractionsRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableRemoteEmojiInteractions));
    private final AbstractConfigCell rememberAllBackMessagesRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.rememberAllBackMessages));
    private final AbstractConfigCell confirmToSendCommandByClickRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.confirmToSendCommandByClick));
    private final AbstractConfigCell dontSendRightAfterTranslatedRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.dontSendRightAfterTranslated));
    private final AbstractConfigCell hideOriginalTextAfterTranslateRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.hideOriginalTextAfterTranslate));
    private final AbstractConfigCell showTranslatedForwardSourceRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.showTranslatedForwardSource));
    private final AbstractConfigCell showTranslatedForwardSourceDetailsRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.showTranslatedForwardSourceDetails));
    private final AbstractConfigCell autoSendMessageIfBlockedBySlowModeRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.autoSendMessageIfBlockedBySlowMode, LocaleController.getString(R.string.AutoSendMessageIfBlockedBySlowModeDesc)));
    private final AbstractConfigCell replyAsQuoteByDefaultRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.replyAsQuoteByDefault));
    private final AbstractConfigCell hideMessageSideButtonsRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.hideMessageSideButtons));
    private final AbstractConfigCell reactionsRow = cellGroup.appendCell(new ConfigCellSelectBox(LocaleController.getString(R.string.doubleTapAndReactions),
            MomoConfig.reactions, MomoConfig.reactionsOptions, null));
    private final AbstractConfigCell disableReactionsWhenSelectingRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableReactionsWhenSelecting));

    private final AbstractConfigCell increasedMaxPhotoResolutionRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.increasedMaxPhotoResolution));
    private final AbstractConfigCell enhancedVideoBitrateRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.enhancedVideoBitrate, LocaleController.getString(R.string.EnhancedVideoBitrateInfo)));
    private final AbstractConfigCell noStarReactionPlaceholderRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.noStarReactionPlaceholder));
    private final AbstractConfigCell autoTestProxyRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.autoTestProxy));
    private final AbstractConfigCell dividerInteractions = cellGroup.appendCell(new ConfigCellDivider());

    // Sticker
    private final AbstractConfigCell headerSticker = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.StickerSettings)));
    private final AbstractConfigCell dontSendGreetingStickerRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.dontSendGreetingSticker));
    private final AbstractConfigCell hideTimeForStickerRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.hideTimeForSticker));
    private final AbstractConfigCell hideGroupStickerRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.hideGroupSticker));
    private final AbstractConfigCell disablePremiumStickerAnimationRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disablePremiumStickerAnimation));
    private final AbstractConfigCell maxRecentStickerCountRow = cellGroup.appendCell(new ConfigCellCustom(CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true, R.string.maxRecentStickerCount));
    private final AbstractConfigCell maxRecentEmojiCountRow = cellGroup.appendCell(new ConfigCellCustom(CellGroup.ITEM_TYPE_TEXT_SETTINGS_CELL, true, R.string.maxRecentEmojiCount));
    private final AbstractConfigCell dividerSticker = cellGroup.appendCell(new ConfigCellDivider());

    // Operation Confirmatation
    private final AbstractConfigCell headerConfirms = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.ConfirmSettings)));
    private final AbstractConfigCell askBeforeCallRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.askBeforeCall));
    private final AbstractConfigCell skipOpenLinkConfirmRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.skipOpenLinkConfirm));
    private final AbstractConfigCell confirmAVRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.confirmAVMessage));
    private final AbstractConfigCell repeatConfirmRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.repeatConfirm));
    private final AbstractConfigCell dividerConfirms = cellGroup.appendCell(new ConfigCellDivider());

    // Instant View
    private final AbstractConfigCell headerInstantView = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.OpenInstantView)));
    private final AbstractConfigCell autoAttemptInstantViewRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.autoAttemptInstantView));
    private final AbstractConfigCell useExtBrowserOnIVAttemptFailRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.useExtBrowserOnIVAttemptFail));
    private final AbstractConfigCell saveIVFailDomainsRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.saveIVFailDomains));
    private final AbstractConfigCell resetIVFailDomainsRow = cellGroup.appendCell(new ConfigCellSelectBox(LocaleController.getString(R.string.ResetIVFailDomains), null, null, this::resetIVFailDomains));
    private final AbstractConfigCell disableEmbeddedPlayerRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableEmbeddedPlayer));
    private final AbstractConfigCell dividerInstantView = cellGroup.appendCell(new ConfigCellDivider());

    // Story
    private final AbstractConfigCell headerStory = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.Story)));
    private final AbstractConfigCell disableStoriesRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableStories));
    private final AbstractConfigCell onlyShowStoriesFromUsersRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.onlyShowStoriesFromUsers));
    private final AbstractConfigCell disableSendReadStoriesRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableSendReadStories));
    private final AbstractConfigCell dividerStory = cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell headerLinks = cellGroup.appendCell(new ConfigCellHeader(LocaleController.getString(R.string.Links)));
    private final AbstractConfigCell forceAllowChooseBrowserRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.forceAllowChooseBrowser, LocaleController.getString(R.string.ForceAllowChooseBrowserDesc)));
    private final AbstractConfigCell patchAndCleanupLinksRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.patchAndCleanupLinks, LocaleController.getString(R.string.PatchAndCleanupLinksDesc)));
    private final AbstractConfigCell customGetQueryBlacklistRow = cellGroup.appendCell(new ConfigCellTextInput(null, MomoConfig.customGetQueryBlacklist, null, null, MomoConfig::applyCustomGetQueryBlacklist));
    private final AbstractConfigCell dividerLinks = cellGroup.appendCell(new ConfigCellDivider());

    private final AbstractConfigCell ignoreBlockedRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.ignoreBlocked, LocaleController.getString(R.string.IgnoreBlockedAbout)));
    private final AbstractConfigCell muteBlockedFromGroupRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.muteBlockedFromGroup));
    private final AbstractConfigCell muteBotsFromGroupRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.muteBotsFromGroup, LocaleController.getString(R.string.MuteBotsFromGroupDesc)));
    private final AbstractConfigCell disableChatActionRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableChatAction));
    private final AbstractConfigCell disableChoosingStickerRow = cellGroup.appendCell(new ConfigCellTextCheck(MomoConfig.disableChoosingSticker));
    private final AbstractConfigCell dividerEnd = cellGroup.appendCell(new ConfigCellDivider());


    private ActionBarMenuItem menuItem;
    private StickerSizeCell stickerSizeCell;
    private UndoView tooltip;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

//        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.emojiDidLoad);
        updateRows();

        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.Chat));

        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }

        ActionBarMenu menu = actionBar.createMenu();
        menuItem = menu.addItem(0, R.drawable.ic_ab_other);
        menuItem.setContentDescription(LocaleController.getString(R.string.AccDescrMoreOptions));
        menuItem.addSubItem(1, R.drawable.msg_reset, LocaleController.getString(R.string.ResetStickerSize));
        menuItem.setVisibility(MomoConfig.stickerSize.Float() != 14.0f ? View.VISIBLE : View.GONE);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    MomoConfig.stickerSize.setConfigFloat(14.0f);
                    menuItem.setVisibility(View.GONE);
                    stickerSizeCell.invalidate();
                }
            }
        });

        // Before listAdapter
        if (!NekoXConfig.isDeveloper()) {
            cellGroup.rows.remove(disableChatActionRow);
            cellGroup.rows.remove(disableChoosingStickerRow);
            // cellGroup.rows.remove(ignoreBlockedRow);
            // cellGroup.rows.remove(dividerEnd);
            MomoConfig.disableChatAction.setConfigBool(false);
            MomoConfig.disableChoosingSticker.setConfigBool(false);
            // MomoConfig.ignoreBlocked.setConfigBool(false);
        }

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
                if (position == cellGroup.rows.indexOf(maxRecentStickerCountRow)) {
                    final int[] counts = {0, 20, 30, 40, 50, 80, 100, 120, 150, 180, 200};
                    List<String> types = Arrays.stream(counts)
                            .filter(i -> i <= getMessagesController().maxRecentStickersCount)
                            .mapToObj(String::valueOf)
                            .collect(Collectors.toList());
                    PopupBuilder builder = new PopupBuilder(view);
                    builder.setItems(types, (i, str) -> {
                        MomoConfig.maxRecentStickerCount.setConfigInt(Integer.parseInt(str.toString()));
                        listAdapter.notifyItemChanged(position);
                        return Unit.INSTANCE;
                    });
                    builder.show();
                } else if (position == cellGroup.rows.indexOf(maxRecentEmojiCountRow)) {
                    final int[] counts = {24, 36, 48, 60, 72, 84, 96, 108, 120};
                    List<String> types = Arrays.stream(counts)
                            .mapToObj(String::valueOf)
                            .collect(Collectors.toList());
                    PopupBuilder builder = new PopupBuilder(view);
                    builder.setItems(types, (i, str) -> {
                        int n = Integer.parseInt(str.toString());
                        MomoConfig.maxRecentEmojiCount.setConfigInt(n);
                        Emoji.MAX_RECENT_EMOJI_COUNT = n;
                        listAdapter.notifyItemChanged(position);
                        return Unit.INSTANCE;
                    });
                    builder.show();
                } else if (position == cellGroup.rows.indexOf(cfCredentialsRow)) {
                    WhisperHelper.showCfCredentialsDialog(this);
                } else if (position == cellGroup.rows.indexOf(deleteUnusedModelRow)) {
                    boolean modelInUse = false;
                    for (int acc : SharedConfig.activeAccounts) {
                        if (WhisperHelper.useLocalModel(acc)) {
                            modelInUse = true;
                            break;
                        }
                    }
                    if (modelInUse && !WhisperModelDownloader.deleteModels()) {
                        BulletinFactory.of(MomoChatSettingsActivity.this)
                                .createSimpleBulletin(R.raw.error, LocaleController.getString(R.string.WhisperModelInUse))
                                .show(true);
                        return;
                    }
                    BulletinFactory.of(MomoChatSettingsActivity.this)
                            .createSimpleBulletin(R.raw.info, LocaleController.getString(R.string.WhisperModelRemoved))
                            .show(true);
                }
            }
        });

        listView.setOnItemLongClickListener((v, i) -> {
            AbstractConfigCell a = cellGroup.rows.get(i);
            String key = null;
            if (ReflectUtil.hasField(a.getClass(), "bindConfig")) {
                Field cfgField = ReflectUtil.getField(a.getClass(), "bindConfig");
                try {
                    if (cfgField != null) {
                        ConfigItem cfg = (ConfigItem) cfgField.get(a);
                        key = (cfg == null) ? null : cfg.key;
                    }
                } catch (IllegalAccessException e) {
                    Log.e("030-cfg", "failed to get config field", e);
                }
            }
            if (key == null) {
                if (i == cellGroup.rows.indexOf(maxRecentEmojiCountRow)) {
                    key = MomoConfig.maxRecentEmojiCount.key;
                } else if (i == cellGroup.rows.indexOf(maxRecentStickerCountRow)) {
                    key = MomoConfig.maxRecentStickerCount.key;
                } else if (i == cellGroup.rows.indexOf(stickerSizeRow)) {
                    key = MomoConfig.stickerSize.key;
                } else if (i == cellGroup.rows.indexOf(messageMenuRow)) {
                    key = MSG_MENU_KEY;
                } else if (i == cellGroup.rows.indexOf(profileMenuRow)) {
                    key = PROFILE_MENU_KEY;
                }
            }
            Log.d("030-cfg", String.format("key = %s", String.valueOf(key)));
            if (key == null) return false;
            if (AndroidUtilities.addToClipboard(String.format("https://t.me/momosettings/?k=%s", key))) {
                BulletinFactory.of(this)
                        .createCopyBulletin(getString(R.string.LinkCopied))
                        .show();
            }
            return false;
        });

        // Cells: Set OnSettingChanged Callbacks
        cellGroup.callBackSettingsChanged = (key, newValue) -> {
            if (key.equals(MomoConfig.disableProximityEvents.getKey())) {
                MediaController.getInstance().recreateProximityWakeLock();
            } else if (key.equals(MomoConfig.disableStories.getKey())) {
                tooltip.showWithAction(0, UndoView.ACTION_NEED_RESTART, null, null);
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
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected int findScrollToIndex() {
        if (scrollToString == null && scrollToKey == null) {
            return -1;
        }
        if (scrollToKey != null) {
            if (scrollToKey.equals(MomoConfig.maxRecentEmojiCount.key)) {
                return cellGroup.rows.indexOf(maxRecentEmojiCountRow);
            } else if (scrollToKey.equals(MomoConfig.maxRecentStickerCount.key)) {
                return cellGroup.rows.indexOf(maxRecentStickerCountRow);
            } else if (scrollToKey.equals(MomoConfig.stickerSize.key)) {
                return cellGroup.rows.indexOf(stickerSizeRow);
            } else if (scrollToKey.equals(MSG_MENU_KEY)) {
                return cellGroup.rows.indexOf(messageMenuRow);
            } else if (scrollToKey.equals(PROFILE_MENU_KEY)) {
                return cellGroup.rows.indexOf(profileMenuRow);
            }
        }
        return super.findScrollToIndex();
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

    private void showMessageMenuAlert() {
        if (getParentActivity() == null) {
            return;
        }
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString(R.string.MessageMenu));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout linearLayoutInviteContainer = new LinearLayout(context);
        linearLayoutInviteContainer.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(linearLayoutInviteContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        int count = 13;
        for (int a = 0; a < count; a++) {
            TextCheckCell textCell = new TextCheckCell(context);
            switch (a) {
                case 0: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.DeleteDownloadedFile), MomoConfig.showDeleteDownloadedFile.Bool(), false);
                    break;
                }
                case 1: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.AddToSavedMessages), MomoConfig.showAddToSavedMessages.Bool(), false);
                    break;
                }
                case 2: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.Repeat), MomoConfig.showRepeat.Bool(), false);
                    break;
                }
                case 3: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.ViewHistory), MomoConfig.showViewHistory.Bool(), false);
                    break;
                }
                case 4: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.Translate), MomoConfig.showTranslate.Bool(), false);
                    break;
                }
                case 5: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.ReportChat), MomoConfig.showReport.Bool(), false);
                    break;
                }
                case 6: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.EditAdminRights), MomoConfig.showAdminActions.Bool(), false);
                    break;
                }
                case 7: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.ChangePermissions), MomoConfig.showChangePermissions.Bool(), false);
                    break;
                }
                case 8: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.Hide), MomoConfig.showMessageHide.Bool(), false);
                    break;
                }
                case 9: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.ShareMessages), MomoConfig.showShareMessages.Bool(), false);
                    break;
                }
                case 10: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.MessageDetails), MomoConfig.showMessageDetails.Bool(), false);
                    break;
                }
                case 11: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.CopyPhotoSticker), MomoConfig.showCopyPhoto.Bool(), false);
                    break;
                }
                case 12: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.CopyFileRef), MomoConfig.showCopyFileRef.Bool(), false);
                }
            }
            textCell.setTag(a);
            textCell.setBackground(Theme.getSelectorDrawable(false));
            linearLayoutInviteContainer.addView(textCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            if (a == (count - 1)) textCell.setDivider(true);
            textCell.setOnClickListener(v2 -> {
                Integer tag = (Integer) v2.getTag();
                switch (tag) {
                    case 0: {
                        textCell.setChecked(MomoConfig.showDeleteDownloadedFile.toggleConfigBool());
                        break;
                    }
                    case 1: {
                        textCell.setChecked(MomoConfig.showAddToSavedMessages.toggleConfigBool());
                        break;
                    }
                    case 2: {
                        textCell.setChecked(MomoConfig.showRepeat.toggleConfigBool());
                        break;
                    }
                    case 3: {
                        textCell.setChecked(MomoConfig.showViewHistory.toggleConfigBool());
                        break;
                    }
                    case 4: {
                        textCell.setChecked(MomoConfig.showTranslate.toggleConfigBool());
                        break;
                    }
                    case 5: {
                        textCell.setChecked(MomoConfig.showReport.toggleConfigBool());
                        break;
                    }
                    case 6: {
                        textCell.setChecked(MomoConfig.showAdminActions.toggleConfigBool());
                        break;
                    }
                    case 7: {
                        textCell.setChecked(MomoConfig.showChangePermissions.toggleConfigBool());
                        break;
                    }
                    case 8: {
                        textCell.setChecked(MomoConfig.showMessageHide.toggleConfigBool());
                        break;
                    }
                    case 9: {
                        textCell.setChecked(MomoConfig.showShareMessages.toggleConfigBool());
                        break;
                    }
                    case 10: {
                        textCell.setChecked(MomoConfig.showMessageDetails.toggleConfigBool());
                        break;
                    }
                    case 11: {
                        textCell.setChecked(MomoConfig.showCopyPhoto.toggleConfigBool());
                        break;
                    }
                    case 12: {
                        textCell.setChecked(MomoConfig.showCopyFileRef.toggleConfigBool());
                        break;
                    }
                }
            });
        }

        TextCheckCell menuHeightCell = new TextCheckCell(context);
        menuHeightCell.setBackground(Theme.getSelectorDrawable(false));
        menuHeightCell.setTextAndCheck(LocaleController.getString(R.string.HigherMinimumMenuHeight), MomoConfig.higherMinimumMenuHeight.Bool(), false);
        menuHeightCell.setOnClickListener(v -> menuHeightCell.setChecked(MomoConfig.higherMinimumMenuHeight.toggleConfigBool()));
        linearLayoutInviteContainer.addView(menuHeightCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        builder.setView(linearLayout);
        showDialog(builder.create());
    }

    private void showProfileMenuAlert() {
        if (getParentActivity() == null) {
            return;
        }
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString(R.string.ProfileMenu));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout linearLayoutInviteContainer = new LinearLayout(context);
        linearLayoutInviteContainer.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(linearLayoutInviteContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        int count = 7;
        for (int a = 0; a < count; a++) {
            TextCheckCell textCell = new TextCheckCell(context);
            switch (a) {
                case 0: {
                    textCell.setTextAndCheck(String.format("%s/%s",
                                    LocaleController.getString(R.string.LinkedChannel),
                                    LocaleController.getString(R.string.LinkedGroupChat)),
                            MomoConfig.profileShowLinkedChat.Bool(), false);
                    break;
                }
                case 1: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.FilterAddTo), MomoConfig.profileShowAddToFolder.Bool(), false);
                    break;
                }
                case 2: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.EventLog), MomoConfig.profileShowRecentActions.Bool(), false);
                    break;
                }
                case 3: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.ClearCache), MomoConfig.profileShowClearCache.Bool(), false);
                    break;
                }
                case 4: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.SearchBlacklistShort), MomoConfig.profileShowBlockSearch.Bool(), false);
                    break;
                }
                case 5: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.SpoilerOnAllMedia), MomoConfig.profileShowSpoilerOnAllMedia.Bool(), false);
                    break;
                }
                case 6: {
                    textCell.setTextAndCheck(LocaleController.getString(R.string.FBan), MomoConfig.showFBan.Bool(), false);
                }
            }
            textCell.setTag(a);
            textCell.setBackground(Theme.getSelectorDrawable(false));
            linearLayoutInviteContainer.addView(textCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            textCell.setOnClickListener(v2 -> {
                Integer tag = (Integer) v2.getTag();
                switch (tag) {
                    case 0: {
                        textCell.setChecked(MomoConfig.profileShowLinkedChat.toggleConfigBool());
                        break;
                    }
                    case 1: {
                        textCell.setChecked(MomoConfig.profileShowAddToFolder.toggleConfigBool());
                        break;
                    }
                    case 2: {
                        textCell.setChecked(MomoConfig.profileShowRecentActions.toggleConfigBool());
                        break;
                    }
                    case 3: {
                        textCell.setChecked(MomoConfig.profileShowClearCache.toggleConfigBool());
                        break;
                    }
                    case 4: {
                        textCell.setChecked(MomoConfig.profileShowBlockSearch.toggleConfigBool());
                        break;
                    }
                    case 5: {
                        textCell.setChecked(MomoConfig.profileShowSpoilerOnAllMedia.toggleConfigBool());
                        break;
                    }
                    case 6: {
                        textCell.setChecked(MomoConfig.showFBan.toggleConfigBool());
                        break;
                    }
                }
            });
        }
        builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        builder.setView(linearLayout);
        showDialog(builder.create());
    }

    private void resetIVFailDomains() {
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString(R.string.OpenInstantView));
        builder.setMessage(LocaleController.getString(R.string.ResetIVFailDomainsConfirm));
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (__, ___) -> {
            NekoXConfig.resetInstantViewFailedDomains();
            BulletinFactory.of(this)
                    .createSimpleBulletin(R.raw.info, LocaleController.getString(R.string.DataCleared))
                    .show();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
       /* if (id == NotificationCenter.emojiDidLoad) {
            if (listView != null) {
                listView.invalidateViews();
            }
        }*/
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
//        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.emojiDidLoad);
    }

    private class StickerSizeCell extends FrameLayout {

        private final StickerSizePreviewMessagesCell messagesCell;
        private final SeekBarView sizeBar;
        private final int startStickerSize = 2;
        private final int endStickerSize = 20;

        private final TextPaint textPaint;

        public StickerSizeCell(Context context) {
            super(context);

            setWillNotDraw(false);

            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(AndroidUtilities.dp(16));

            sizeBar = new SeekBarView(context);
            sizeBar.setReportChanges(true);
            sizeBar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
                @Override
                public void onSeekBarDrag(boolean stop, float progress) {
                    MomoConfig.stickerSize.setConfigFloat(startStickerSize + (endStickerSize - startStickerSize) * progress);
                    StickerSizeCell.this.invalidate();
                    menuItem.setVisibility(View.VISIBLE);
                }

                @Override
                public void onSeekBarPressed(boolean pressed) {

                }
            });
            addView(sizeBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.LEFT | Gravity.TOP, 9, 5, 43, 11));

            messagesCell = new StickerSizePreviewMessagesCell(context, parentLayout);
            addView(messagesCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, 53, 0, 0));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            canvas.drawText("" + Math.round(MomoConfig.stickerSize.Float()), getMeasuredWidth() - AndroidUtilities.dp(39), AndroidUtilities.dp(28), textPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            sizeBar.setProgress((MomoConfig.stickerSize.Float() - startStickerSize) / (float) (endStickerSize - startStickerSize));
        }

        @Override
        public void invalidate() {
            super.invalidate();
            messagesCell.invalidate();
            sizeBar.invalidate();
        }
    }

    //impl ListAdapter
    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
            Log.d("030-?", "maxRecentStickerCountRow index=" + cellGroup.rows.indexOf(maxRecentStickerCountRow));
            Log.d("030-?", "maxRecentEmojiCountRow index=" + cellGroup.rows.indexOf(maxRecentEmojiCountRow));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            AbstractConfigCell a = cellGroup.rows.get(position);
            SimpleTextView textView = null;
            String currentText = null;
            if (a != null) {
                if (a instanceof ConfigCellCustom) {
                    // Custom binds
                    if (holder.itemView instanceof TextSettingsCell) {
                        TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                        if (position == cellGroup.rows.indexOf(maxRecentStickerCountRow)) {
                            textCell.setTextAndValue(LocaleController.getString(R.string.maxRecentStickerCount), String.valueOf(MomoConfig.maxRecentStickerCount.Int()), true);
                        } else if (position == cellGroup.rows.indexOf(maxRecentEmojiCountRow)) {
                            textCell.setTextAndValue(LocaleController.getString(R.string.maxRecentEmojiCount), String.valueOf(MomoConfig.maxRecentEmojiCount.Int()), true);
                        } else if (position == cellGroup.rows.indexOf(cfCredentialsRow)) {
                            textCell.setTextAndValue(LocaleController.getString(R.string.CloudflareCredentials), "", true);
                        } else if (position == cellGroup.rows.indexOf(deleteUnusedModelRow)) {
                            textCell.setTextAndValue(LocaleController.getString(R.string.DeleteUnusedWhisperModel), "", true);
                        }
                        textView = textCell.getTextView();
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
                checkScrollTo(position, holder, textView, currentText);
            }
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
                case ConfigCellCustom.CUSTOM_ITEM_StickerSize:
                    view = stickerSizeCell = new StickerSizeCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }
    }
}
