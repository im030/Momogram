package moe.hx030.momogram;

import static org.telegram.messenger.LocaleController.getString;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.apache.commons.lang3.StringUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.LauncherIconController;
import org.unifiedpush.android.connector.UnifiedPush;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import moe.hx030.momogram.config.ConfigItem;
import moe.hx030.momogram.helpers.EvilLeakerKiller;
import moe.hx030.momogram.settings.MomoAppearanceSettingsActivity;
import moe.hx030.momogram.settings.MomoChatSettingsActivity;
import moe.hx030.momogram.settings.MomoExperimentalSettingsActivity;
import moe.hx030.momogram.settings.MomoGeneralSettingsActivity;
import moe.hx030.momogram.transtale.Translator;
import moe.hx030.momogram.transtale.source.FirefoxLocalTranslator;
import moe.hx030.momogram.util.ModUtil;
import moe.hx030.momogram.utils.FileUtil;
import moe.hx030.momogram.utils.ShareUtil;
import moe.hx030.momogram.utils.StrUtil;

import static moe.hx030.momogram.config.ConfigItem.*;
import static moe.hx030.momogram.utils.StrUtil.getAppName;
import static moe.hx030.momogram.utils.StrUtil.isAppName;

import androidx.annotation.NonNull;

@SuppressLint("ApplySharedPref")
public class MomoConfig {

    public static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nkmrcfg", Context.MODE_PRIVATE);
    public static final Object sync = new Object();
    public static final String channelAliasPrefix = "channelAliasPrefix_";
    public static final String chatNameOverridePrefix = "chatNameOverride_";

    public static final int TRANSCRIBE_AUTO = 0;
    public static final int TRANSCRIBE_PREMIUM = 1;
    public static final int TRANSCRIBE_WORKERSAI = 2;
    public static final int TRANSCRIBE_LOCAL = 3;

    public static final int AUTO_DISMISS_DISABLED = 0;
    public static final int AUTO_DISMISS_ALL = 1;
    public static final int AUTO_DISMISS_BY_RULE = 2;

    private static boolean configLoaded = false;
    private static final ArrayList<ConfigItem> configs = new ArrayList<>();

    // Configs
    public static ConfigItem migrate = addConfig("NekoConfigMigrate", configTypeBool, false);
    public static ConfigItem migrate030 = addConfig("NekoConfigMigrate2", configTypeInt, 0);
    public static ConfigItem useOldName = addConfig(R.string.UseOldAppName, "UseOldAppName", configTypeBool, GENERAL, false);
    public static ConfigItem largeAvatarInDrawer = addConfig(R.string.AvatarAsBackground, "AvatarAsBackground", configTypeInt, GENERAL, 0); // 0:TG Default 1:NekoX Default 2:Large Avatar

    public static ConfigItem checkUpdate = addConfig(R.string.AutoCheckUpdate, "AutoCheckUpdate", configTypeBool, GENERAL, true);
    public static ConfigItem allowTestingUpdate = addConfig(R.string.AllowTestingUpdate, "AllowTestingUpdate", configTypeBool, GENERAL, false);
    public static ConfigItem useIPv6 = addConfig(R.string.IPv6, "IPv6", configTypeBool, GENERAL, false);
    public static ConfigItem inappCamera = addConfig(R.string.DebugMenuEnableCamera, "DebugMenuEnableCamera", configTypeBool, GENERAL, true); // fake
    public static ConfigItem mapPreviewProvider = addConfig(R.string.MapPreviewProvider, "MapPreviewProvider", configTypeInt, GENERAL, 0);
    public static ConfigItem openArchiveOnPull = addConfig(R.string.OpenArchiveOnPull, "OpenArchiveOnPull", configTypeBool, GENERAL, false);
    public static ConfigItem avatarBackgroundBlur = addConfig(R.string.BlurAvatarBackground, "BlurAvatarBackground", configTypeBool, false);
    public static ConfigItem avatarBackgroundDarken = addConfig(R.string.DarkenAvatarBackground, "DarkenAvatarBackground", configTypeBool, false);

    public static ConfigItem sortByUnread = addConfig("sort_by_unread", configTypeBool, GENERAL, false);
    public static ConfigItem sortByUnmuted = addConfig("sort_by_unmuted", configTypeBool, GENERAL, true);
    public static ConfigItem sortByUser = addConfig("sort_by_user", configTypeBool, GENERAL, true);
    public static ConfigItem sortByContacts = addConfig("sort_by_contacts", configTypeBool, GENERAL, true);

    public static ConfigItem disableUndo = addConfig(R.string.DisableUndo, "DisableUndo", configTypeBool, GENERAL, false);
    public static ConfigItem disableSystemAccount = addConfig(R.string.DisableSystemAccount, "DisableSystemAccount", configTypeBool, GENERAL, false);
    public static ConfigItem customSavePath = addConfig(R.string.customSavePath, "customSavePath", configTypeString, GENERAL, null);
    public static ConfigItem alwaysSaveToDownloads = addConfig(R.string.AlwaysSaveToDownloads, "AlwaysSaveToDownloads", configTypeBool, GENERAL, false);
    public static ConfigItem translationProvider = addConfig(R.string.TranslationProvider, "translationProvider", configTypeInt, GENERAL, 1);
    public static ConfigItem translateToLang = addConfig(R.string.TransToLang, "TransToLang", configTypeString, GENERAL, ""); // "" -> translate to current language (MessageTrans.kt & Translator.kt)
    public static ConfigItem translateInputLang = addConfig(R.string.TransInputToLang, "TransInputToLang", configTypeString, GENERAL, "en");
    public static ConfigItem useTelegramTranslateInChat = addConfig(R.string.useTelegramTranslateInChat, "useTelegramTranslateInChat", configTypeBool, GENERAL, false);
    public static ConfigItem trimCOTFromTranslateResult = addConfig(R.string.TrimCOTFromTranslateResult, "TrimCOTFromTranslateResult", configTypeBool, GENERAL, true);
    public static ConfigItem googleCloudTranslateKey = addConfig(R.string.GoogleCloudTransKey, "GoogleCloudTransKey", configTypeString, GENERAL, "");
    public static ConfigItem disableNotificationBubbles = addConfig(R.string.disableNotificationBubbles, "disableNotificationBubbles", configTypeBool, GENERAL, false);
    public static ConfigItem useSystemDNS = addConfig(R.string.useSystemDNS, "useSystemDNS", configTypeBool, GENERAL, false);
    public static ConfigItem useAdGuardDNS = addConfig(R.string.UseAdGuardDNS, "UseAdGuardDNS", configTypeBool, GENERAL, false);
    public static ConfigItem customDoH = addConfig(R.string.customDoH, "customDoH", configTypeString, GENERAL, "");
    public static ConfigItem hideProxyByDefault = addConfig(R.string.HideProxyByDefault, "HideProxyByDefault", configTypeBool, GENERAL, false);
    public static ConfigItem disableAppBarShadow = addConfig(R.string.DisableAppBarShadow, "DisableAppBarShadow", configTypeBool, GENERAL, false);

    public static ConfigItem openPGPApp = addConfig(R.string.OpenPGPApp, "OpenPGPApp", configTypeString, GENERAL, "");
    public static ConfigItem openPGPKeyId = addConfig(R.string.OpenPGPKey, "OpenPGPKey", configTypeLong, GENERAL, 0L);

    public static ConfigItem autoPauseVideo = addConfig(R.string.AutoPauseVideo, "AutoPauseVideo", configTypeBool, GENERAL, false);
    public static ConfigItem disableAutoDownloadingWin32Executable = addConfig(R.string.Win32ExecutableFiles, "Win32ExecutableFiles", configTypeBool, GENERAL, true);
    public static ConfigItem disableAutoDownloadingArchive = addConfig(R.string.ArchiveFiles, "ArchiveFiles", configTypeBool, GENERAL, true);
    public static ConfigItem useOSMDroidMap = addConfig(R.string.useOSMDroidMap, "useOSMDroidMap", configTypeBool, GENERAL, !BuildVars.isGServicesCompiled);
    public static ConfigItem mapDriftingFixForGoogleMaps = addConfig(R.string.mapDriftingFixForGoogleMaps, "mapDriftingFixForGoogleMaps", configTypeBool, GENERAL, true);
    public static ConfigItem closeWebViewWithoutConfirmation = addConfig(R.string.CloseWebViewWithoutConfirmation, "CloseWebViewWithoutConfirmation", ConfigItem.configTypeBool, false);
    public static ConfigItem openWebViewTabWithoutBot = addConfig(R.string.OpenWebViewTabWithoutBot, "OpenWebViewTabWithoutBot", ConfigItem.configTypeBool, GENERAL, false);
    public static ConfigItem showBotWebViewSettings = addConfig(R.string.ShowBotWebViewSettings, "ShowBotWebViewSettings", ConfigItem.configTypeBool, GENERAL, false);
    public static ConfigItem disableWebViewGeolocation = addConfig(R.string.DisableWebViewGeolocation, "DisableWebViewGeolocation", ConfigItem.configTypeBool, GENERAL, false);
    public static ConfigItem hideWebViewTabOverlayWhenSharing = addConfig(R.string.HideWebViewTabOverlayWhenSharing, "HideWebViewTabOverlayWhenSharing", ConfigItem.configTypeBool, GENERAL, true);
    public static ConfigItem hideWebViewTabOverlayInChat = addConfig(R.string.HideWebViewTabOverlayInChat, "HideWebViewTabOverlayInChat", ConfigItem.configTypeBool, GENERAL, true);
    public static ConfigItem preventPullDownWebview = addConfig(R.string.PreventPullDownWebview, "PreventPullDownWebview", ConfigItem.configTypeBool, GENERAL, false);
    public static ConfigItem useBotWebviewForGames = addConfig(R.string.UseBotWebviewForGames, "UseBotWebviewForGames", ConfigItem.configTypeBool, GENERAL, false);
    public static ConfigItem confirmOpenLinkInWebView = addConfig(R.string.ConfirmOpenLinkInWebView, "ConfirmOpenLinkInWebView", ConfigItem.configTypeBool, GENERAL, false);
    public static ConfigItem forceExternalBrowserForBots = addConfig(R.string.ForceExternalBrowserForBots, "ForceExternalBrowserForBots", ConfigItem.configTypeBool, GENERAL, false);
    public static ConfigItem openChatOnWebView = addConfig(R.string.PreventPauseWebViewByOpenChat, "PreventPauseWebViewByOpenChat", ConfigItem.configTypeBool, GENERAL, false);
    public static ConfigItem alwaysShowDownloads = addConfig(R.string.AlwaysShowDownloads, "AlwaysShowDownloads", ConfigItem.configTypeBool, GENERAL, false);
    public static ConfigItem openAvatarInsteadOfExpand = addConfig(R.string.OpenAvatarInsteadOfExpand, "OpenAvatarInsteadOfExpand", ConfigItem.configTypeBool, GENERAL, false);
    public static ConfigItem showSharedMediaOnOpeningProfile = addConfig(R.string.ShowSharedMediaOnOpeningProfile, "ShowSharedMediaOnOpeningProfile", configTypeBool, GENERAL, false);
    public static ConfigItem disableAddBirthdayReminder = addConfig(R.string.DisableAddBirthdayReminder, "DisableAddBirthdayReminder", configTypeBool, GENERAL, false);
    public static ConfigItem disableBirthdayReminder = addConfig(R.string.DisableBirthdayReminder, "DisableBirthdayReminder", configTypeBool, GENERAL, false);
    public static ConfigItem alwaysDisableSafeBrowsingInWebView = addConfig(R.string.AlwaysDisableSafeBrowsingInWebView, "AlwaysDisableSafeBrowsingInWebView", configTypeBool, GENERAL, false);
    public static ConfigItem scanQrCodeFromChatList = addConfig(R.string.ScanQrCodeFromChatList, "ScanQrCodeFromChatList", configTypeBool, GENERAL, false);
    public static ConfigItem customSearchEngine = addConfig(R.string.CustomSearchEngine, "CustomSearchEngine", configTypeString, GENERAL, "https://www.startpage.com/sp/search?query=");
    public static ConfigItem dontAutoPlayNextMessage = addConfig(R.string.DontAutoPlayNextMessage, "DontAutoPlayNextMessage", configTypeBool, GENERAL, false);
    public static ConfigItem dontShareNumberWhenAddContactByDefault = addConfig(R.string.DontShareNumberWhenAddContactByDefault, "DontShareNumberWhenAddContactByDefault", configTypeBool, GENERAL, true);
    public static ConfigItem customDeepLXInstance = addConfig(R.string.CustomDeepLXInstance, "CustomDeepLXInstance", configTypeString, GENERAL, "");
    public static ConfigItem customLingvaInstance = addConfig(R.string.CustomLingvaInstance, "CustomLingvaInstance", configTypeString, GENERAL, "");
    public static ConfigItem disableAutoWebLogin = addConfig(R.string.DisableAutoWebLogin, "DisableAutoWebLogin", configTypeBool, GENERAL, false);
    public static ConfigItem ignoreFilterEmoticonUpdate = addConfig(R.string.IgnoreFilterEmoticonUpdate, "IgnoreFilterEmoticonUpdate", configTypeBool, GENERAL, false);
    public static ConfigItem articleViewerBottomActionBar = addConfig(R.string.ArticleViewerBottomActionBar, "ArticleViewerBottomActionBar", configTypeBool, GENERAL, false);
    public static ConfigItem hideCocoonAISummary = addConfig(R.string.HideCocoonAISummary, "HideCocoonAISummary", configTypeBool, GENERAL, false);
    public static ConfigItem enableUnifiedPush = addConfig(R.string.EnableUnifiedPush, "EnableUnifiedPush", configTypeBool, GENERAL, false);
    public static ConfigItem customApiId = addConfig(R.string.CustomApiId, "CustomApiId", configTypeString, GENERAL, "");
    public static ConfigItem customApiHash = addConfig(R.string.CustomApiHash, "CustomApiHash", configTypeString, GENERAL, "");
    public static ConfigItem unarchiveOnSwipe = addConfig(R.string.UnarchiveOnSwipe, "UnarchiveOnSwipe", configTypeBool, GENERAL, true);
    public static ConfigItem swipeActionInTopicList = addConfig(R.string.SwipeActionInTopicList, "SwipeActionInTopicList", configTypeBool, GENERAL, true);
    public static ConfigItem allowBotInDirectShare = addConfig(R.string.AllowBotInDirectShare, "AllowBotInDirectShare", configTypeBool, GENERAL, false);
    public static ConfigItem recentChatFolderSize = addConfig(R.string.RecentChatFolderSize, "RecentChatFolderSize", configTypeInt, GENERAL, 0); // 0: disabled
    public static ConfigItem checkMemLeak = addConfig(R.string.CheckMemLeak, "CheckMemLeak", configTypeBool, GENERAL, true);
    public static ConfigItem memLeakThreshold = addConfig(R.string.MemLeakThreshold, "MemLeakThreshold", configTypeInt, GENERAL, EvilLeakerKiller.setThreshold(1.2F));
    public static ConfigItem noPreloadTrackIfRepeatOne = addConfig(R.string.NoPreloadTrackIfRepeatOne, "NoPreloadTrackIfRepeatOne", configTypeBool, GENERAL, false);
    public static ConfigItem noForwardToStories = addConfig(R.string.HideForwardToStories, "HideForwardToStories", configTypeBool, GENERAL, false);
    public static ConfigItem disableSessionChecker = addConfig(R.string.DisableSessionChecker, "DisableSessionChecker", configTypeBool, GENERAL, false);
    public static ConfigItem disablePullDownSearch = addConfig(R.string.DisablePullDownSearch, "DisablePullDownSearch", configTypeBool, GENERAL, false);
    public static ConfigItem switchAccountsFromChatListMenu = addConfig(R.string.SwitchAccountsFromChatListMenu, "SwitchAccountsFromChatListMenu", configTypeBool, GENERAL, false);
    public static ConfigItem alwaysTryHevcCodec = addConfig(R.string.AlwaysTryHevcCodec, "AlwaysTryHevcCodec", configTypeBool, GENERAL, false);
    public static ConfigItem swipeLoopChatFilter = addConfig(R.string.SwipeLoopChatFilter, "SwipeLoopChatFilter", configTypeBool, GENERAL, false);

    public static ConfigItem preferredTranslateTargetLang = addConfig(R.string.PreferredTranslateTargetLangTitle, "PreferredTranslateTargetLangTitle", configTypeString, GENERAL, "");
    public static ArrayList<String> preferredTranslateTargetLangList = new ArrayList<>();

    public static ConfigItem perfClassOverride = addConfig(R.string.OverridePerformanceClass, "perfClassOverride", configTypeInt, GENERAL, 0);
    public static String[] perfClassOverrideOptions = null;

    public static ConfigItem searchBlacklist = addConfig(R.string.SearchBlacklist, "searchBlackList", configTypeString, GENERAL, "");
    public static ArrayList<Long> searchBlacklistData = new ArrayList<>();


    public static ConfigItem repeatConfirm = addConfig(R.string.repeatConfirm, "repeatConfirm", configTypeBool, CHAT, false);
    public static ConfigItem disableInstantCamera = addConfig(R.string.DisableInstantCamera, "DisableInstantCamera", configTypeBool, CHAT, false);
    public static ConfigItem hideCameraInAttachMenu = addConfig(R.string.HideCameraInAttachMenu, "HideCameraInAttachMenu", configTypeBool, CHAT, false);

    public static ConfigItem hidePhone = addConfig(R.string.HidePhone, "HidePhone", configTypeBool, APPEARANCE, true);
    public static ConfigItem tabletMode = addConfig(R.string.TabletMode, "TabletMode", configTypeInt, APPEARANCE, 0);
    public static ConfigItem typeface = addConfig(R.string.TypefaceUseDefault, "TypefaceUseDefault", configTypeBool, APPEARANCE, false);
    public static ConfigItem nameOrder = addConfig(R.string.NameOrder, "NameOrder", configTypeInt, APPEARANCE, 1);
    public static ConfigItem transparentStatusBar = addConfig(R.string.TransparentStatusBar, "TransparentStatusBar", configTypeBool, APPEARANCE, true);
    public static ConfigItem enableAvatarBlur = addConfig(R.string.EnableAvatarBlur, "EnableAvatarBlur", configTypeBool, APPEARANCE, true);
    public static ConfigItem forceBlurInChat = addConfig(R.string.forceBlurInChat, "forceBlurInChat", configTypeBool, APPEARANCE, false);
    public static ConfigItem chatBlurAlphaValue = addConfig("forceBlurInChatAlphaValue", configTypeInt, APPEARANCE, 127);
    public static ConfigItem actionBarDecoration = addConfig(R.string.ActionBarDecoration, "ActionBarDecoration", configTypeInt, APPEARANCE, 0);
    public static ConfigItem newYear = addConfig(R.string.ChristmasHat, "ChristmasHat", configTypeBool, APPEARANCE, false);
    public static ConfigItem showTabsOnForward = addConfig(R.string.ShowTabsOnForward, "ShowTabsOnForward", configTypeBool, APPEARANCE, false);
    public static ConfigItem ignoreMutedCount = addConfig(R.string.IgnoreMutedCount, "IgnoreMutedCount", configTypeBool, APPEARANCE, true);
    public static ConfigItem showIdAndDc = addConfig(R.string.ShowIdAndDc, "ShowIdAndDc", configTypeBool, APPEARANCE, false);
    public static ConfigItem tabsTitleType = addConfig(R.string.TabTitleType, "TabTitleType", configTypeInt, APPEARANCE, NekoXConfig.TITLE_TYPE_TEXT);
    public static ConfigItem disableNumberRounding = addConfig(R.string.DisableNumberRounding, "DisableNumberRounding", configTypeBool, APPEARANCE, false);
    public static ConfigItem numberRounding = addConfig(R.string.NumRounding, "NumberRounding", configTypeInt, APPEARANCE, 0);
    public static String[] disableNumberRoundingOptions = null;
    public static ConfigItem usePersianCalendar = addConfig(R.string.UsePersiancalendar, "UsePersiancalendar", configTypeBool, APPEARANCE, false);
    public static ConfigItem displayPersianCalendarByLatin = addConfig(R.string.DisplayPersianCalendarByLatin, "DisplayPersianCalendarByLatin", configTypeBool, APPEARANCE, false);
    public static ConfigItem showSelfInsteadOfSavedMessages = addConfig(R.string.ShowSelfInsteadOfSavedMessages, "ShowSelfInsteadOfSavedMessages", ConfigItem.configTypeBool, APPEARANCE, false);
    public static ConfigItem customTitleText = addConfig(R.string.CustomTitleText, "CustomTitleText", configTypeString, APPEARANCE, "Momogram");
    public static ConfigItem hideUnreadCounterOnFolderTabs = addConfig(R.string.HideUnreadCounterOnFolderTabs, "HideUnreadCounterOnFolderTabs", configTypeBool, APPEARANCE, false);
    public static ConfigItem nameAsTitleText = addConfig(R.string.NameAsTitleText, "NameAsTitleText", configTypeBool, APPEARANCE, false);
    public static ConfigItem unreadBadgeOnBackButton = addConfig(R.string.unreadBadgeOnBackButton, "unreadBadgeOnBackButton", configTypeBool, APPEARANCE, false);
    public static ConfigItem showSeconds = addConfig(R.string.showSeconds, "showSeconds", configTypeBool, APPEARANCE, false);
    public static ConfigItem pauseInactiveTabAnimation = addConfig(R.string.PauseInactiveTabAnimation, "PauseInactiveTabAnimation", configTypeBool, APPEARANCE, false);
    public static ConfigItem showAddedToFoldersAtTitleType = addConfig(R.string.ShowAddedToFoldersAtTitle, "ShowAddedToFoldersAtTitleType", configTypeInt, APPEARANCE, 0); // 1: left, 2: right
    public static ConfigItem boostedContactRatingInProfile = addConfig(R.string.BoostedContactRatingInProfile, "BoostedContactRatingInProfile", configTypeBool, APPEARANCE, true);
    public static ConfigItem hideProfileRating = addConfig(R.string.HideProfileRating, "HideProfileRating", configTypeBool, APPEARANCE, false);
    public static ConfigItem squareAvatar = addConfig(R.string.SquareAvatar, "SquareAvatar", configTypeBool, APPEARANCE, false);
    public static ConfigItem profileActionCircleBtn = addConfig(R.string.ProfileActionCircleBtn, "ProfileActionCircleBtn", configTypeBool, APPEARANCE, false);
    public static ConfigItem mediaPreview = addConfig(R.string.MediaPreview, "MediaPreview", configTypeBool, APPEARANCE, true);
    public static ConfigItem labelChannelUser = addConfig(R.string.labelChannelUser, "labelChannelUser", configTypeBool, APPEARANCE, false);
    public static ConfigItem alwaysLabelAnonAdmin = addConfig(R.string.AlwaysLabelAnonAdmin, "AlwaysLabelAnonAdmin", configTypeBool, APPEARANCE, false);
    public static ConfigItem hideChannelBottomMuteUnmute = addConfig(R.string.HideChannelBottomNotiToggle, "HideChannelBottomNotiToggle", configTypeBool, APPEARANCE, false);
    public static ConfigItem disableCustomWallpaperUser = addConfig(R.string.DisableCustomWallpaperUser, "DisableCustomWallpaperUser", ConfigItem.configTypeBool, APPEARANCE, false);
    public static ConfigItem disableCustomWallpaperChannel = addConfig(R.string.DisableCustomWallpaperChannel, "DisableCustomWallpaperChannel", ConfigItem.configTypeBool, APPEARANCE, false);
    public static ConfigItem appendOriginalTimestamp = addConfig(R.string.AppendOriginalTimestamp, "AppendOriginalTimestamp", ConfigItem.configTypeBool, APPEARANCE, false);
    public static ConfigItem showEditTimeInPopupMenu = addConfig(R.string.ShowEditTimestampInPopupMenu, "ShowEditTimestampInPopupMenu", configTypeBool, APPEARANCE, false);
    public static ConfigItem showForwardTimeInPopupMenu = addConfig(R.string.ShowForwardTimestampInPopupMenu, "ShowForwardTimestampInPopupMenu", configTypeBool, APPEARANCE, false);
    public static ConfigItem marqueeForLongChatTitles = addConfig(R.string.MarqueeForLongChatTitles, "MarqueeForLongChatTitles", configTypeBool, APPEARANCE, true);
    public static ConfigItem marqueeForLongMomoOptions = addConfig(R.string.MarqueeForLongMomoOptions, "MarqueeForLongMomoSettings", configTypeBool, APPEARANCE, false);
    public static ConfigItem forceHideShowAsList = addConfig(R.string.ForceHideShowAsList, "ForceHideShowAsList", configTypeBool, APPEARANCE, false);
    public static ConfigItem imageMessageSizeTweak = addConfig(R.string.ImageMessageSizeTweak, "ImageMessageSizeTweak", configTypeBool, APPEARANCE, false);
    public static ConfigItem smallerEmojiInChooser = addConfig(R.string.SmallerEmojisInChooser, "SmallerEmojisInChooser", configTypeBool, APPEARANCE, false);
    public static ConfigItem showChannelMsgFwdCount = addConfig(R.string.ShowChannelMsgFwdCount, "ShowChannelMsgFwdCount", configTypeBool, APPEARANCE, true);
    public static ConfigItem useEmojiForEdited = addConfig(R.string.UseEmojiForEdited, "UseEmojiForEdited", configTypeBool, APPEARANCE, false);
    public static ConfigItem ignoreTopicTabView = addConfig(R.string.IgnoreTopicTabView, "IgnoreTopicTabView", configTypeBool, APPEARANCE, false);
    public static ConfigItem unroundedChatView = addConfig(R.string.UnroundedChatView, "UnroundedChatView", configTypeBool, APPEARANCE, false);
    public static ConfigItem removeChatViewPadding = addConfig(R.string.RemoveChatViewPadding, "RemoveChatViewPadding", configTypeBool, APPEARANCE, false);
    public static ConfigItem hideBottomNavTabs = addConfig(R.string.HideBottomNavTabs, "HideBottomNavTabs", configTypeBool, APPEARANCE, false);
    public static ConfigItem hideFAB = addConfig(R.string.HideFAB, "hideFAB", configTypeBool, APPEARANCE, false);

    public static ConfigItem overrideForumStyle = addConfig(R.string.OverrideForumStyle, "OverrideForumStyle", configTypeInt, APPEARANCE, 0);
    public static String[] overrideForumStyleOptions = null;

    public static ConfigItem smoothKeyboard = addConfig("DebugMenuEnableSmoothKeyboard", configTypeBool, false);// fake
    public static ConfigItem hideProxySponsorChannel = addConfig(R.string.HideProxySponsorChannel, "HideProxySponsorChannel", configTypeBool, false);
    public static ConfigItem stickerSize = addConfig(R.string.StickerSize, "stickerSize", configTypeFloat, CHAT, 14.0f);
    public static ConfigItem disablePhotoSideAction = addConfig(R.string.DisablePhotoViewerSideAction, "DisablePhotoViewerSideAction", configTypeBool, CHAT, false);
    public static ConfigItem hideKeyboardOnChatScroll = addConfig(R.string.HideKeyboardOnChatScroll, "HideKeyboardOnChatScroll", configTypeBool, CHAT, false);
    public static ConfigItem rearVideoMessages = addConfig(R.string.RearVideoMessages, "RearVideoMessages", configTypeBool, CHAT, false);
//    public static ConfigItem pressTitleToOpenAllChats = addConfig("pressTitleToOpenAllChats", configTypeBool, false);
    public static ConfigItem disableChatAction = addConfig(R.string.DisableChatAction, "DisableChatAction", configTypeBool, false);
    public static ConfigItem hideMessageSideButtons = addConfig(R.string.HideMessageSideButtons, "HideMessageSideButtons", configTypeBool, CHAT, false);

    public static ConfigItem showAddToSavedMessages = addConfig("showAddToSavedMessages", configTypeBool, CHAT, true);
    public static ConfigItem showReport = addConfig("showReport", configTypeBool, CHAT, true);
    public static ConfigItem showViewHistory = addConfig("showViewHistory", configTypeBool, CHAT, true);
    public static ConfigItem showAdminActions = addConfig("showAdminActions", configTypeBool, CHAT, true);
    public static ConfigItem showChangePermissions = addConfig("showChangePermissions", configTypeBool, CHAT, true);
    public static ConfigItem showDeleteDownloadedFile = addConfig("showDeleteDownloadedFile", configTypeBool, CHAT, true);
    public static ConfigItem showMessageDetails = addConfig("showMessageDetails", configTypeBool, CHAT, false);
    public static ConfigItem showTranslate = addConfig("showTranslate", configTypeBool, CHAT, true);
    public static ConfigItem showRepeat = addConfig("showRepeat", configTypeBool, CHAT, false);
    public static ConfigItem showShareMessages = addConfig("showShareMessages", configTypeBool, CHAT, false);
    public static ConfigItem showMessageHide = addConfig("showMessageHide", configTypeBool, CHAT, false);
    public static ConfigItem showFBan = addConfig("showFBan", configTypeBool, CHAT, false);
    public static ConfigItem showCopyFileRef = addConfig("showCopyFileRef", configTypeBool, CHAT, false);
    public static ConfigItem higherMinimumMenuHeight = addConfig("higherMinimumMenuHeight", configTypeBool, CHAT, false);

    public static ConfigItem filterUsers = addConfig("filter_users", configTypeBool, true);
    public static ConfigItem filterContacts = addConfig("filter_contacts", configTypeBool, true);
    public static ConfigItem filterGroups = addConfig("filter_groups", configTypeBool, true);
    public static ConfigItem filterChannels = addConfig("filter_channels", configTypeBool, true);
    public static ConfigItem filterBots = addConfig("filter_bots", configTypeBool, true);
    public static ConfigItem filterAdmins = addConfig("filter_admins", configTypeBool, true);
    public static ConfigItem filterUnmuted = addConfig("filter_unmuted", configTypeBool, true);
    public static ConfigItem filterUnread = addConfig("filter_unread", configTypeBool, true);
    public static ConfigItem filterUnmutedAndUnread = addConfig("filter_unmuted_and_unread", configTypeBool, true);

    public static ConfigItem skipOpenLinkConfirm = addConfig(R.string.SkipOpenLinkConfirm, "SkipOpenLinkConfirm", configTypeBool, CHAT, false);
    public static ConfigItem confirmAVMessage = addConfig(R.string.ConfirmAVMessage, "ConfirmAVMessage", configTypeBool, CHAT, false);
    public static ConfigItem askBeforeCall = addConfig(R.string.AskBeforeCalling, "AskBeforeCalling", configTypeBool, CHAT, false);


    public static ConfigItem disableVibration = addConfig(R.string.DisableVibration, "DisableVibration", configTypeBool, CHAT, false);
    public static ConfigItem disableProximityEvents = addConfig(R.string.DisableProximityEvents, "DisableProximityEvents", configTypeBool, CHAT, false);

    public static ConfigItem ignoreContentRestrictions = addConfig(R.string.IgnoreContentRestrictions, "ignoreContentRestrictions", configTypeBool, !BuildVars.isPlay);
    public static ConfigItem useChatAttachMediaMenu = addConfig(R.string.UseChatAttachEnterMenu, "UseChatAttachEnterMenu", configTypeBool, CHAT, true);
    public static ConfigItem disableLinkPreviewByDefault = addConfig(R.string.DisableLinkPreviewByDefault, "DisableLinkPreviewByDefault", configTypeBool, CHAT, false);
    public static ConfigItem sendCommentAfterForward = addConfig(R.string.SendCommentAfterForward, "SendCommentAfterForward", configTypeBool, CHAT, true);
//    public static ConfigItem increaseVoiceMessageQuality = addConfig("IncreaseVoiceMessageQuality", configTypeBool, true);
    public static ConfigItem disableTrending = addConfig(R.string.DisableTrending, "DisableTrending", configTypeBool, CHAT, true);
    public static ConfigItem dontSendGreetingSticker = addConfig(R.string.DontSendGreetingSticker, "DontSendGreetingSticker", configTypeBool, CHAT, false);
    public static ConfigItem hideTimeForSticker = addConfig(R.string.HideTimeForSticker, "HideTimeForSticker", configTypeBool, CHAT, false);
    public static ConfigItem takeGIFasVideo = addConfig(R.string.TakeGIFasVideo, "TakeGIFasVideo", configTypeBool, CHAT, false);
    public static ConfigItem maxRecentEmojiCount = addConfig(R.string.maxRecentEmojiCount, "maxRecentEmojiCount", configTypeInt, CHAT, 48);
    public static ConfigItem maxRecentStickerCount = addConfig(R.string.maxRecentStickerCount, "maxRecentStickerCount", configTypeInt, CHAT, 20);
    public static ConfigItem disableSwipeToNext = addConfig(R.string.disableSwipeToNextChannel, "disableSwipeToNextChannel", configTypeBool, CHAT, true);
    public static ConfigItem disableRemoteEmojiInteractions = addConfig(R.string.disableRemoteEmojiInteractions, "disableRemoteEmojiInteractions", configTypeBool, CHAT, true);
    public static ConfigItem disableChoosingSticker = addConfig(R.string.disableChoosingSticker, "disableChoosingSticker", configTypeBool, CHAT, false);
    public static ConfigItem hideGroupSticker = addConfig(R.string.hideGroupSticker, "hideGroupSticker", configTypeBool, CHAT, false);
    public static ConfigItem disablePremiumStickerAnimation = addConfig(R.string.disablePremiumStickerAnimation, "disablePremiumStickerAnimation", configTypeBool, CHAT, false);
    public static ConfigItem hideSponsoredMessage = addConfig(R.string.hideSponsoredMessage, "hideSponsoredMessage", configTypeBool, false);
    public static ConfigItem rememberAllBackMessages = addConfig(R.string.rememberAllBackMessages, "rememberAllBackMessages", configTypeBool, CHAT, false);
    public static ConfigItem hideSendAsChannel = addConfig(R.string.hideSendAsChannel, "hideSendAsChannel", configTypeBool, CHAT, false);
    public static ConfigItem showSpoilersDirectly = addConfig(R.string.showSpoilersDirectly, "showSpoilersDirectly", configTypeBool, CHAT,false);

    public static ConfigItem alwaysUseSpoilerForMedia = addConfig(R.string.AlwaysUseSpoilerForMedia, "AlwaysUseSpoilerForMedia", configTypeString, CHAT, "");
    public static ArrayList<Long> alwaysUseSpoilerForMediaChats = new ArrayList<>();

    public static ConfigItem disableReactionsWhenSelecting = addConfig(R.string.disableReactionsWhenSelecting, "disableReactionsWhenSelecting", configTypeBool, CHAT, true);
    public static ConfigItem showBottomActionsWhenSelecting = addConfig(R.string.showBottomActionsWhenSelecting, "showBottomActionsWhenSelecting", configTypeBool, CHAT, false);

    public static ConfigItem reactions = addConfig(R.string.reactions, "reactions", configTypeInt, CHAT, 0);
    public static String[] reactionsOptions = null;

    public static ConfigItem disableStories = addConfig(R.string.DisableStories, "DisableStories", ConfigItem.configTypeBool, CHAT, false);
    public static ConfigItem onlyShowStoriesFromUsers = addConfig(R.string.OnlyShowStoriesFromUsers, "OnlyShowStoriesFromUsers", ConfigItem.configTypeBool, CHAT, false);
    public static ConfigItem disableSendReadStories = addConfig(R.string.DisableSendReadStories, "DisableSendReadStories", ConfigItem.configTypeBool, CHAT, false);
    public static ConfigItem confirmToSendCommandByClick = addConfig(R.string.ConfirmToSendCommandToggle, "ConfirmToSendCommandToggle", ConfigItem.configTypeBool, CHAT, false);
    public static ConfigItem showCopyPhoto = addConfig(R.string.CopyPhoto, "CopyPhoto", ConfigItem.configTypeBool, CHAT, false);
    public static ConfigItem increasedMaxPhotoResolution = addConfig(R.string.IncreasedMaxPhotoResolution, "IncreasedMaxPhotoResolution", ConfigItem.configTypeBool, CHAT, false);
    public static ConfigItem forceAllowChooseBrowser = addConfig(R.string.ForceAllowChooseBrowser, "ForceAllowChooseBrowser", configTypeBool, CHAT, false);
    public static ConfigItem patchAndCleanupLinks = addConfig(R.string.PatchAndCleanupLinks, "PatchAndCleanupLinks", configTypeBool, CHAT, false);
    public static ConfigItem alwaysHideBotCommandButton = addConfig(R.string.AlwaysHideBotCommandButton, "AlwaysHideBotCommandButton", configTypeBool, CHAT, false);
    public static ConfigItem alwaysShowBotCommandButton = addConfig(R.string.AlwaysShowBotCommandButton, "AlwaysShowBotCommandButton", configTypeBool, CHAT, false);
    public static ConfigItem dontSendRightAfterTranslated = addConfig(R.string.DontSendRightAfterTranslated, "DontSendRightAfterTranslated", configTypeBool, CHAT, true);
    public static ConfigItem hideOriginalTextAfterTranslate = addConfig(R.string.HideOriginalTextAfterTranslate, "HideOriginalTextAfterTranslate", configTypeBool, CHAT, false);
    public static ConfigItem autoTranslate = addConfig(R.string.AutomaticTranslation, "AutomaticTranslation", configTypeBool, CHAT, false);
    public static ConfigItem useCustomProviderForAutoTranslate = addConfig(R.string.UseCustomProviderForAutoTranslate, "UseCustomProviderForAutoTranslate", configTypeBool, CHAT, true);
    public static ConfigItem autoSendMessageIfBlockedBySlowMode = addConfig(R.string.AutoSendMessageIfBlockedBySlowMode, "AutoSendMessageIfBlockedBySlowMode", configTypeBool, CHAT, false);
    public static ConfigItem replyAsQuoteByDefault = addConfig(R.string.ReplyAsQuoteByDefault, "ReplyAsQuoteByDefault", configTypeBool, CHAT, false);
    public static ConfigItem chooseBestVideoQualityByDefault = addConfig(R.string.ChooseBestVideoQualityByDefault, "ChooseBestVideoQualityByDefault", configTypeBool, CHAT, false);
    public static ConfigItem mapMobileDataSaverToRoaming = addConfig(R.string.MapMobileDataSaverToRoaming, "MapMobileDataSaverToRoaming", configTypeBool, false);
    public static ConfigItem keepBlockedBotChatHistory = addConfig(R.string.KeepBlockedBotChatHistory, "KeepBlockedBotChatHistory", configTypeBool, false);
    public static ConfigItem dontSendStartCmdOnUnblockBot = addConfig(R.string.DontSendStartCmdOnUnblockBot, "DontSendStartCmdOnUnblockBot", configTypeBool, false);
    public static ConfigItem alwaysLoadStickerSetFromServer = addConfig(R.string.AlwaysLoadStickerSetFromServer, "AlwaysLoadStickerSetFromServer", configTypeBool, CHAT, false);
    public static ConfigItem autoAttemptInstantView = addConfig(R.string.AutoAttemptInstantView, "AutoAttemptInstantView", configTypeBool, CHAT, false);
    public static ConfigItem useExtBrowserOnIVAttemptFail = addConfig(R.string.OpenExternalOnInstantViewFail, "OpenExternalOnInstantViewFail", configTypeBool, CHAT, false);
    public static ConfigItem saveIVFailDomains = addConfig(R.string.SaveIVFailDomains, "SaveIVFailDomains", configTypeBool, CHAT, true);
    public static ConfigItem enhancedVideoBitrate = addConfig(R.string.EnhancedVideoBitrate, "EnhancedVideoBitrate", configTypeBool, CHAT, false);
    public static ConfigItem keepSamePositionOnNewMsg = addConfig(R.string.KeepSamePositionOnNewMsg, "KeepSamePositionOnNewMsg", configTypeBool, CHAT, false);
    public static ConfigItem keepVoteCountAfterRetractVote = addConfig(R.string.KeepVoteCountAfterRetractVote, "KeepVoteCountAfterRetractVote", configTypeBool, CHAT, false);

    public static ConfigItem transcribeProvider = addConfig("TranscribeProvider", configTypeInt, CHAT, TRANSCRIBE_AUTO);
    public static ConfigItem cfAccountID = addConfig("cfAccountID", configTypeString, CHAT, "");
    public static ConfigItem cfApiToken = addConfig("cfApiToken", configTypeString, CHAT, "");
    public static String[] transcribeOptions;
    public static ConfigItem useSlowWhisperModel = addConfig(R.string.UseSlowWhisperModel, "UseSlowWhisperModel", configTypeBool, CHAT, false);

    public static ConfigItem showAddedToFoldersAtTitle = addConfig(R.string.ShowAddedToFoldersAtTitle, "ShowAddedToFoldersAtTitle", configTypeBool, false);
    public static String[] titleFolderIconOptions = null;

    public static ConfigItem hideMessageRegex = addConfig(R.string.HideMessageRegex, "HideMessageRegex", configTypeString, CHAT, "").setOnConfigChanged(MomoConfig::applyHideMsgRegex);
    public static String hideMessageRegexString = null;
    public static Pattern hideMessageRegexPattern = null;

    public static ConfigItem profileShowLinkedChat = addConfig("profileShowLinkedChat", configTypeBool, CHAT, true);
    public static ConfigItem profileShowAddToFolder = addConfig("profileShowAddToFolder", configTypeBool, CHAT, true);
    public static ConfigItem profileShowRecentActions = addConfig("profileShowRecentActions", configTypeBool, CHAT, false);
    public static ConfigItem profileShowClearCache = addConfig("profileShowClearCache", configTypeBool, CHAT, false);
    public static ConfigItem profileShowBlockSearch = addConfig("profileShowBlockSearch", configTypeBool, CHAT, false);
    public static ConfigItem profileShowSpoilerOnAllMedia = addConfig("profileShowSpoilerOnAllMedia", configTypeBool, CHAT, false);

    public static ConfigItem customGetQueryBlacklist = addConfig(R.string.BlacklistUrlQueryTitle, "BlacklistUrlQueryTitle", configTypeString, CHAT, "");
    public static ArrayList<String> customGetQueryBlacklistData = new ArrayList<>();

    public static ConfigItem nextCheckCustomStatusTime = addConfig("nextCheckCustomStatusTime", configTypeLong, 0L);
    public static ConfigItem disableSaveDraftToCloud = addConfig(R.string.DisableSaveDraftToCloud, "DisableSaveDraftToCloud", configTypeBool, CHAT, false);
    public static ConfigItem noStarReactionPlaceholder = addConfig(R.string.NoStarReactionPlaceholder, "NoStarReactionPlaceholder", configTypeBool, CHAT, true);
    public static ConfigItem disableEmbeddedPlayer = addConfig(R.string.DisableEmbeddedPlayer, "DisableEmbeddedPlayer", configTypeBool, CHAT, false);
    public static ConfigItem autoTestProxy = addConfig(R.string.AutoTestProxy, "AutoTestProxy", configTypeBool, CHAT, false);
    public static ConfigItem randomizeFilenameOnSend = addConfig(R.string.RandomizeFilenameOnSend, "RandomizeFilenameOnSend", configTypeBool, CHAT, false);
    public static ConfigItem hideAIEditButton = addConfig(R.string.HideAIEditButton, "HideAIEditButton", configTypeBool, CHAT, false);
    public static ConfigItem secretChatReqConfirm = addConfig(R.string.SecretChatReqConfirm, "SecretChatReqConfirm", configTypeBool, CHAT, false);


    public static ConfigItem unlimitedFavedStickers = addConfig(R.string.UnlimitedFavoredStickers, "UnlimitedFavoredStickers", configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem unlimitedPinnedDialogs = addConfig(R.string.UnlimitedPinnedDialogs, "UnlimitedPinnedDialogs", configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem useSystemEmoji = addConfig("EmojiUseDefault", configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem channelAlias = addConfig(R.string.channelAlias, "channelAlias", configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem enableStickerPin = addConfig(R.string.EnableStickerPin, "EnableStickerPin", configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem useMediaStreamInVoip = addConfig(R.string.UseMediaStreamInVoip, "UseMediaStreamInVoip", configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem customAudioBitrate = addConfig(R.string.customGroupVoipAudioBitrate, "customAudioBitrate", configTypeInt, EXPERIMENTAL, 32);
    public static ConfigItem enhancedFileLoader = addConfig(R.string.enhancedFileLoader, "enhancedFileLoader", configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem fasterReconnectHack = addConfig(R.string.FasterReconnectHack, "FasterReconnectHack", ConfigItem.configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem removePremiumAnnoyance = addConfig(R.string.RemovePremiumAnnoyance, "RemovePremiumAnnoyance", configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem overrideSettingBoolean = addConfig(R.string.OverrideSettingBoolean, "OverrideSettingBoolean", configTypeString, EXPERIMENTAL, "");
    public static ConfigItem overrideSettingInteger = addConfig(R.string.OverrideSettingInteger, "OverrideSettingInteger", configTypeString, EXPERIMENTAL, "");
    public static ConfigItem overrideSettingString = addConfig(R.string.OverrideSettingString, "OverrideSettingString", configTypeString, EXPERIMENTAL, "");
    public static ConfigItem overrideSettingLong = addConfig(R.string.OverrideSettingLong, "OverrideSettingLong", configTypeString, EXPERIMENTAL, "");
    public static ConfigItem overrideSettingFloat = addConfig(R.string.OverrideSettingFloat, "OverrideSettingFloat", configTypeString, EXPERIMENTAL, "");

    public static ConfigItem chatListFontSizeFollowChat = addConfig(R.string.ChatListFontSizeFollowChat, "ChatListFontSizeFollowChat", configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem allowDupLogin = addConfig(R.string.AllowDupLogin, "AllowDupLogin", configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem alwaysDestroyPhotoViewer = addConfig(R.string.AlwaysDestroyPhotoViewer, "AlwaysDestroyPhotoViewer", configTypeBool, EXPERIMENTAL, true);
    public static ConfigItem showQuickReconnect = addConfig(R.string.ShowQuickReconnect, "ShowQuickReconnect", configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem autoRestartOnLeak = addConfig(R.string.AutoRestartOnLeak, "AutoRestartOnLeak", configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem resumeAudioPlaybackOnLaunch = addConfig(R.string.ResumeAudioPlaybackOnLaunch, "ResumeAudioPlaybackOnLaunch", configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem ignoreTranslatorCache = addConfig(R.string.IgnoreTranslatorCache, "IgnoreTranslatorCache", configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem aidlOnLaunch = addConfig(R.string.AIDLOnLaunch, "AIDLOnLaunch", configTypeBool, EXPERIMENTAL, true);
    public static ConfigItem autoDecryptPGPMessages = addConfig(R.string.AutoDecryptPGPMsg, "AutoDecryptPGPMsg", configTypeBool, EXPERIMENTAL, false);
    public static ConfigItem bufferCleaner = addConfig(R.string.BufferCleaner, "BufferCleaner", configTypeBool, EXPERIMENTAL, true);
    public static ConfigItem tempDebug = addConfig(R.string.SettingsDebug, "TempDebug", configTypeBool, EXPERIMENTAL, false);

    // anti-spam
    public static ConfigItem autoArchiveAndMute = addConfig(R.string.AutoArchiveAndMute, "AutoArchiveAndMute", ConfigItem.configTypeBool, ANTI_SPAM, false);
    public static ConfigItem autoArchiveAndMuteNoCommonGroupOnly = addConfig(R.string.AutoArchiveAndMuteNoCommonGroupOnly, "AutoArchiveAndMuteNoCommonGroupOnly", ConfigItem.configTypeBool, ANTI_SPAM, false);
    public static ConfigItem ignoreBlocked = addConfig(R.string.IgnoreBlocked, "IgnoreBlocked", configTypeBool, ANTI_SPAM, false);
    public static ConfigItem muteBlockedFromGroup = addConfig(R.string.MuteBlockedFromGroup, "MuteBlockedFromGroup", configTypeBool, ANTI_SPAM, false);
    public static ConfigItem muteBotsFromGroup = addConfig(R.string.MuteBotsFromGroup, "MuteBotsFromGroup", configTypeBool, ANTI_SPAM, false);
    public static ConfigItem ignoreAllReactions = addConfig(R.string.IgnoreAllReactions, "IgnoreAllReactions", ConfigItem.configTypeBool, ANTI_SPAM, false);
    public static ConfigItem debugAntiSpam = addConfig(R.string.DebugAntiSpam, "DebugAntiSpam", configTypeBool, ANTI_SPAM, false);
    public static ConfigItem autoDismissJoinReq = addConfig(R.string.AutoDismissJoinReq, "AutoDismissJoinReq", configTypeBool, ANTI_SPAM, false);
    public static ConfigItem autoDismissJoinReqBio = addConfig(R.string.AutoDismissJoinReqBio, "AutoDismissJoinReqBio", configTypeBool, ANTI_SPAM, false);
    public static ConfigItem autoDismissNameUseOpenCC = addConfig(R.string.AutoDismissUseOpenCC, "AutoDismissNameUseOpenCC", configTypeBool, ANTI_SPAM, false);
    public static ConfigItem autoDismissRegex = addConfig(R.string.AutoDismissReqRegex, "AutoDismissReqRegex", configTypeString, ANTI_SPAM,
            "群发|纸飞机|跑U|棋牌|招商|变现|群發|稳赚|全网").setOnConfigChanged(MomoConfig::applyAutoBanByRegex);
    public static ConfigItem autoDismissDummy = addConfig(R.string.AutoDismissReqDummy, "AutoDismissReqDummy", configTypeBool, ANTI_SPAM, false);
    public static String autoDismissRegexString;
    public static Pattern autoDismissRegexPattern;
    public static ConfigItem autoDismissSuggestedChats = addConfig(R.string.AutoDismissSuggestedChats, "AutoDismissSuggestedChats", configTypeInt, ANTI_SPAM, AUTO_DISMISS_DISABLED);
    public static String[] autoDismissSuggestedChatsOptions = null;

    // internal
    public static ConfigItem nextPromptUpdateTime = addConfig("nextPromptUpdateTime", configTypeLong, 0L);
    public static ConfigItem cachePath = addConfig(R.string.StoragePath, "cache_path", configTypeString, "");
    public static ConfigItem customAllChatsName = addConfig("CustomAllChatsName", configTypeString, "");
    public static ConfigItem lastCrashError = addConfig("LastCrashError", configTypeString, null);
    public static ConfigItem chatNameOverride = addConfig(R.string.ChatNameOverride, "ChatNameOverride", configTypeBool, false);
    public static ConfigItem customCacheSize = addConfig("CustomCacheSize", configTypeInt, -1);
    public static ConfigItem warnedClients = addConfig("WarnedClients", configTypeString, "");
    public static ConfigItem prevSessionCheck = addConfig("PrevSessionCheck", configTypeLong, 0L);

    // deprecated
    public static ConfigItem ccToLang = addConfig("opencc_to_lang", configTypeString, "");
    public static ConfigItem ccInputLang = addConfig("opencc_input_to_lang", configTypeString, "");
    public static ConfigItem useCustomEmoji = addConfig(R.string.useCustomEmoji, "useCustomEmoji", configTypeBool, false);

    static {
        loadConfig(false);
        checkMigrate(false);
    }

    public static ConfigItem addConfig(String k, int t, Object d) {
        ConfigItem a = new ConfigItem(k, t, d);
        configs.add(a);
        return a;
    }

    public static ConfigItem addConfig(String k, int t, int p, Object d) {
        ConfigItem a = new ConfigItem(k, t, p, d);
        configs.add(a);
        return a;
    }

    public static ConfigItem addConfig(int id, String k, int t, Object d) {
        ConfigItem a = new ConfigItem(id, k, t, d);
        configs.add(a);
        return a;
    }

    public static ConfigItem addConfig(int id, String k, int t, int p, Object d) {
        ConfigItem a = new ConfigItem(id, k, t, p, d);
        configs.add(a);
        return a;
    }
    public static void loadConfig(boolean force) {
        synchronized (sync) {
            if (configLoaded && !force) {
                return;
            }
            for (int i = 0; i < configs.size(); i++) {
                ConfigItem o = configs.get(i);

                try {
                    if (o.type == configTypeBool) {
                        o.value = preferences.getBoolean(o.key, (boolean) o.defaultValue);
                    }
                    if (o.type == configTypeInt) {
                        try {
                            o.value = preferences.getInt(o.key, (int) o.defaultValue);
                        } catch (Exception e) {
                            Log.e("030-ex", String.format("error while loading %s : %s", String.valueOf(o.key), String.valueOf(o.defaultValue)), e);
                            o.value = 0;
                        }
                    }
                    if (o.type == configTypeLong) {
                        o.value = preferences.getLong(o.key, (Long) o.defaultValue);
                    }
                    if (o.type == configTypeFloat) {
                        o.value = preferences.getFloat(o.key, (Float) o.defaultValue);
                    }
                    if (o.type == configTypeString) {
                        o.value = preferences.getString(o.key, (String) o.defaultValue);
                    }
                    if (o.type == configTypeSetInt) {
                        Set<String> ss = preferences.getStringSet(o.key, new HashSet<>());
                        HashSet<Integer> si = new HashSet<>();
                        for (String s : ss) {
                            si.add(Integer.parseInt(s));
                        }
                        o.value = si;
                    }
                    if (o.type == configTypeMapIntInt) {
                        String cv = preferences.getString(o.key, "");
                        // Log.e("NC", String.format("Getting pref %s val %s", o.key, cv));
                        if (cv.length() == 0) {
                            o.value = new HashMap<Integer, Integer>();
                        } else {
                            try {
                                byte[] data = Base64.decode(cv, Base64.DEFAULT);
                                ObjectInputStream ois = new ObjectInputStream(
                                        new ByteArrayInputStream(data));
                                o.value = (HashMap<Integer, Integer>) ois.readObject();
                                if (o.value == null) {
                                    o.value = new HashMap<Integer, Integer>();
                                }
                                ois.close();
                            } catch (Exception e) {
                                o.value = new HashMap<Integer, Integer>();
                            }
                        }
                    }
                } catch (Exception ex) {
                    preferences.edit().remove(o.key).commit();
                    Log.e("030-cfg", String.format("removed %s from config due to error", o.key), ex);
                }
            }
            configLoaded = true;
        }
    }

    public static void checkMigrate(boolean force) {
        if (migrate030.Int() < 1) {
            // fix tablet mode not applied properly when tablet mode is already on by default
            if ((MomoConfig.tabletMode.Int() < 2 && SharedConfig.forceDisableTabletMode) ||
                    (MomoConfig.tabletMode.Int() == 2 && !SharedConfig.forceDisableTabletMode)) {
                SharedConfig.toggleForceDisableTabletMode();
            }
            migrate030.setConfigInt(1);
        }

        if (migrate030.Int() < 2) {
            // switch to new app name
            migrate030.setConfigInt(2); // set mig lvl first
            Utilities.globalQueue.postRunnable(() -> {
                LauncherIconController.switchAppName(false);
            });
        }

        if (migrate030.Int() < 3) {
            migrate030.setConfigInt(3);
            alwaysDestroyPhotoViewer.setConfigBool(true);
        }

        if (migrate030.Int() < 4) {
            migrate030.setConfigInt(4);
            showAddedToFoldersAtTitleType.setConfigInt(showAddedToFoldersAtTitle.Bool() ? 1 : 0);
        }

        if (migrate030.Int() < 6) {
            if (migrate030.Int() < 6) migrate030.setConfigInt(7);
            // ShowVoteCountBeforeVote -> KeepVoteCountAfterRetractVote
            if (preferences.getBoolean("ShowVoteCountBeforeVote", false)) {
                keepVoteCountAfterRetractVote.setConfigBool(true);
            }
            if (ignoreTopicTabView.Bool()) {
                overrideForumStyle.setConfigInt(NekoXConfig.ForumViewOverride.CLASSIC.value);
            }
        }

        if (migrate030.Int() < 7) {
            migrate030.setConfigInt(7);
            bufferCleaner.setConfigBool(true);
        }

        if (migrate030.Int() < 8) {
            migrate030.setConfigInt(8);
            if (disableNumberRounding.Bool()) {
                numberRounding.setConfigInt(1);
            }
        }



        // TODO remove this after some versions.
        if (migrate.Bool() || force)
            return;

        migrate.setConfigBool(true);

        // MomoConfig.java read & migrate
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekoconfig", Activity.MODE_PRIVATE);

        if (preferences.contains("typeface"))
            typeface.setConfigBool(preferences.getInt("typeface", 0) != 0);
        if (preferences.contains("nameOrder"))
            nameOrder.setConfigInt(preferences.getInt("nameOrder", 1));
        if (preferences.contains("mapPreviewProvider"))
            mapPreviewProvider.setConfigInt(preferences.getInt("mapPreviewProvider", 0));
        if (preferences.contains("transparentStatusBar"))
            transparentStatusBar.setConfigBool(preferences.getBoolean("transparentStatusBar", false));
        if (preferences.contains("hideProxySponsorChannel"))
            hideProxySponsorChannel.setConfigBool(preferences.getBoolean("hideProxySponsorChannel", false));
        if (preferences.contains("showAddToSavedMessages"))
            showAddToSavedMessages.setConfigBool(preferences.getBoolean("showAddToSavedMessages", true));
        if (preferences.contains("showReport"))
            showReport.setConfigBool(preferences.getBoolean("showReport", true));
        if (preferences.contains("showViewHistory"))
            showViewHistory.setConfigBool(preferences.getBoolean("showViewHistory", true));
        if (preferences.contains("showAdminActions"))
            showAdminActions.setConfigBool(preferences.getBoolean("showAdminActions", true));
        if (preferences.contains("showChangePermissions"))
            showChangePermissions.setConfigBool(preferences.getBoolean("showChangePermissions", true));
        if (preferences.contains("showDeleteDownloadedFile"))
            showDeleteDownloadedFile.setConfigBool(preferences.getBoolean("showDeleteDownloadedFile", true));
        if (preferences.contains("showMessageDetails"))
            showMessageDetails.setConfigBool(preferences.getBoolean("showMessageDetails", false));
        if (preferences.contains("showTranslate"))
            showTranslate.setConfigBool(preferences.getBoolean("showTranslate", true));
        if (preferences.contains("showRepeat"))
            showRepeat.setConfigBool(preferences.getBoolean("showRepeat", false));
        if (preferences.contains("showShareMessages"))
            showShareMessages.setConfigBool(preferences.getBoolean("showShareMessages", false));
        if (preferences.contains("showMessageHide"))
            showMessageHide.setConfigBool(preferences.getBoolean("showMessageHide", false));

        if (preferences.contains("actionBarDecoration"))
            actionBarDecoration.setConfigInt(preferences.getInt("actionBarDecoration", 0));
        if (preferences.contains("newYear"))
            newYear.setConfigBool(preferences.getBoolean("newYear", false));
        if (preferences.contains("stickerSize"))
            stickerSize.setConfigFloat(preferences.getFloat("stickerSize", 14.0f));
        if (preferences.contains("unlimitedFavedStickers"))
            unlimitedFavedStickers.setConfigBool(preferences.getBoolean("unlimitedFavedStickers", false));
        if (preferences.contains("unlimitedPinnedDialogs"))
            unlimitedPinnedDialogs.setConfigBool(preferences.getBoolean("unlimitedPinnedDialogs", false));
        if (preferences.contains("translationProvider"))
            translationProvider.setConfigInt(preferences.getInt("translationProvider", 1));
        if (preferences.contains("disablePhotoSideAction"))
            disablePhotoSideAction.setConfigBool(preferences.getBoolean("disablePhotoSideAction", true));
        if (preferences.contains("openArchiveOnPull"))
            openArchiveOnPull.setConfigBool(preferences.getBoolean("openArchiveOnPull", false));
        if (preferences.contains("showHiddenFeature"))             //showHiddenFeature.setConfigBool(preferences.getBoolean("showHiddenFeature", false));
            if (preferences.contains("hideKeyboardOnChatScroll"))
                hideKeyboardOnChatScroll.setConfigBool(preferences.getBoolean("hideKeyboardOnChatScroll", false));
        if (preferences.contains("avatarBackgroundBlur"))
            avatarBackgroundBlur.setConfigBool(preferences.getBoolean("avatarBackgroundBlur", false));
        if (preferences.contains("avatarBackgroundDarken"))
            avatarBackgroundDarken.setConfigBool(preferences.getBoolean("avatarBackgroundDarken", false));
        if (preferences.contains("useSystemEmoji"))
            useSystemEmoji.setConfigBool(preferences.getBoolean("useSystemEmoji", false));
        if (preferences.contains("showTabsOnForward"))
            showTabsOnForward.setConfigBool(preferences.getBoolean("showTabsOnForward", false));
        if (preferences.contains("rearVideoMessages"))
            rearVideoMessages.setConfigBool(preferences.getBoolean("rearVideoMessages", false));
        if (preferences.contains("disable_chat_action"))
            disableChatAction.setConfigBool(preferences.getBoolean("disable_chat_action", false));
        if (preferences.contains("sort_by_unread"))
            sortByUnread.setConfigBool(preferences.getBoolean("sort_by_unread", false));
        if (preferences.contains("sort_by_unmuted"))
            sortByUnmuted.setConfigBool(preferences.getBoolean("sort_by_unmuted", true));
        if (preferences.contains("sort_by_user"))
            sortByUser.setConfigBool(preferences.getBoolean("sort_by_user", true));
        if (preferences.contains("sort_by_contacts"))
            sortByContacts.setConfigBool(preferences.getBoolean("sort_by_contacts", true));

        if (preferences.contains("disable_undo"))
            disableUndo.setConfigBool(preferences.getBoolean("disable_undo", false));

        if (preferences.contains("filter_users"))
            filterUsers.setConfigBool(preferences.getBoolean("filter_users", true));
        if (preferences.contains("filter_contacts"))
            filterContacts.setConfigBool(preferences.getBoolean("filter_contacts", true));
        if (preferences.contains("filter_groups"))
            filterGroups.setConfigBool(preferences.getBoolean("filter_groups", true));
        if (preferences.contains("filter_channels"))
            filterChannels.setConfigBool(preferences.getBoolean("filter_channels", true));
        if (preferences.contains("filter_bots"))
            filterBots.setConfigBool(preferences.getBoolean("filter_bots", true));
        if (preferences.contains("filter_admins"))
            filterAdmins.setConfigBool(preferences.getBoolean("filter_admins", true));
        if (preferences.contains("filter_unmuted"))
            filterUnmuted.setConfigBool(preferences.getBoolean("filter_unmuted", true));
        if (preferences.contains("filter_unread"))
            filterUnread.setConfigBool(preferences.getBoolean("filter_unread", true));
        if (preferences.contains("filter_unmuted_and_unread"))
            filterUnmutedAndUnread.setConfigBool(preferences.getBoolean("filter_unmuted_and_unread", true));

        if (preferences.contains("disable_system_account"))
            disableSystemAccount.setConfigBool(preferences.getBoolean("disable_system_account", false));
        if (preferences.contains("skip_open_link_confirm"))
            skipOpenLinkConfirm.setConfigBool(preferences.getBoolean("skip_open_link_confirm", false));

        if (preferences.contains("ignore_muted_count"))
            ignoreMutedCount.setConfigBool(preferences.getBoolean("ignore_muted_count", true));
//        if (preferences.contains("use_default_theme"))
//            useDefaultTheme.setConfigBool(preferences.getBoolean("use_default_theme", false));
        if (preferences.contains("show_id_and_dc"))
            showIdAndDc.setConfigBool(preferences.getBoolean("show_id_and_dc", false));

        if (preferences.contains("google_cloud_translate_key"))
            googleCloudTranslateKey.setConfigString(preferences.getString("google_cloud_translate_key", null));
        if (preferences.contains("cache_path"))
            cachePath.setConfigString(preferences.getString("cache_path", null));

        if (preferences.contains("trans_to_lang"))
            translateToLang.setConfigString(preferences.getString("trans_to_lang", ""));
        if (preferences.contains("trans_input_to_lang"))
            translateInputLang.setConfigString(preferences.getString("trans_input_to_lang", "en"));

        if (preferences.contains("opencc_to_lang"))
            ccToLang.setConfigString(preferences.getString("opencc_to_lang", null));
        if (preferences.contains("opencc_input_to_lang"))
            ccInputLang.setConfigString(preferences.getString("opencc_input_to_lang", null));

        if (preferences.contains("tabsTitleType"))
            tabsTitleType.setConfigInt(preferences.getInt("tabsTitleType", NekoXConfig.TITLE_TYPE_TEXT));
        if (preferences.contains("confirmAVMessage"))
            confirmAVMessage.setConfigBool(preferences.getBoolean("confirmAVMessage", false));
        if (preferences.contains("askBeforeCall"))
            askBeforeCall.setConfigBool(preferences.getBoolean("askBeforeCall", false));
        if (preferences.contains("disableNumberRounding"))
            disableNumberRounding.setConfigBool(preferences.getBoolean("disableNumberRounding", false));

        if (preferences.contains("useSystemDNS"))
            useSystemDNS.setConfigBool(preferences.getBoolean("useSystemDNS", false));
        if (preferences.contains("customDoH"))
            customDoH.setConfigString(preferences.getString("customDoH", ""));
        if (preferences.contains("hide_proxy_by_default"))
            hideProxyByDefault.setConfigBool(preferences.getBoolean("hide_proxy_by_default", false));

        if (preferences.contains("disableAppBarShadow"))
            disableAppBarShadow.setConfigBool(preferences.getBoolean("disableAppBarShadow", false));
        if (preferences.contains("mediaPreview"))
            mediaPreview.setConfigBool(preferences.getBoolean("mediaPreview", true));

        if (preferences.contains("openPGPApp"))
            openPGPApp.setConfigString(preferences.getString("openPGPApp", ""));
        if (preferences.contains("openPGPKeyId"))
            openPGPKeyId.setConfigLong(preferences.getLong("openPGPKeyId", 0L));

        if (preferences.contains("disableVibration"))
            disableVibration.setConfigBool(preferences.getBoolean("disableVibration", false));
        if (preferences.contains("autoPauseVideo"))
            autoPauseVideo.setConfigBool(preferences.getBoolean("autoPauseVideo", false));
        if (preferences.contains("disableProximityEvents"))
            disableProximityEvents.setConfigBool(preferences.getBoolean("disableProximityEvents", false));

        if (preferences.contains("ignoreContentRestrictions"))
            ignoreContentRestrictions.setConfigBool(preferences.getBoolean("ignoreContentRestrictions", !BuildVars.isPlay));
        if (preferences.contains("useChatAttachMediaMenu"))
            useChatAttachMediaMenu.setConfigBool(preferences.getBoolean("useChatAttachMediaMenu", true));
        if (preferences.contains("disableLinkPreviewByDefault"))
            disableLinkPreviewByDefault.setConfigBool(preferences.getBoolean("disableLinkPreviewByDefault", false));
        if (preferences.contains("sendCommentAfterForward"))
            sendCommentAfterForward.setConfigBool(preferences.getBoolean("sendCommentAfterForward", true));
//        if (preferences.contains("increaseVoiceMessageQuality"))
//            increaseVoiceMessageQuality.setConfigBool(preferences.getBoolean("increaseVoiceMessageQuality", true));
        if (preferences.contains("disableTrending"))
            disableTrending.setConfigBool(preferences.getBoolean("disableTrending", true));
        if (preferences.contains("dontSendGreetingSticker"))
            dontSendGreetingSticker.setConfigBool(preferences.getBoolean("dontSendGreetingSticker", false));
        if (preferences.contains("hideTimeForSticker"))
            hideTimeForSticker.setConfigBool(preferences.getBoolean("hideTimeForSticker", false));
        if (preferences.contains("takeGIFasVideo"))
            takeGIFasVideo.setConfigBool(preferences.getBoolean("takeGIFasVideo", false));
        if (preferences.contains("maxRecentStickerCount"))
            maxRecentStickerCount.setConfigInt(preferences.getInt("maxRecentStickerCount", 20));
        if (preferences.contains("disableSwipeToNext"))
            disableSwipeToNext.setConfigBool(preferences.getBoolean("disableSwipeToNext", true));
        if (preferences.contains("disableRemoteEmojiInteractions"))
            disableRemoteEmojiInteractions.setConfigBool(preferences.getBoolean("disableRemoteEmojiInteractions", true));
        if (preferences.contains("disableChoosingSticker"))
            disableChoosingSticker.setConfigBool(preferences.getBoolean("disableChoosingSticker", false));

        if (preferences.contains("disableAutoDownloadingWin32Executable"))
            disableAutoDownloadingWin32Executable.setConfigBool(preferences.getBoolean("disableAutoDownloadingWin32Executable", true));
        if (preferences.contains("disableAutoDownloadingArchive"))
            disableAutoDownloadingArchive.setConfigBool(preferences.getBoolean("disableAutoDownloadingArchive", true));

        if (preferences.contains("enableStickerPin"))
            enableStickerPin.setConfigBool(preferences.getBoolean("enableStickerPin", false));
        if (preferences.contains("useMediaStreamInVoip"))
            useMediaStreamInVoip.setConfigBool(preferences.getBoolean("useMediaStreamInVoip", false));
        if (preferences.contains("customAudioBitrate"))
            customAudioBitrate.setConfigInt(preferences.getInt("customAudioBitrate", 32));
    }

    public static boolean fixDriftingForGoogleMaps() {
        return BuildVars.isGServicesCompiled && !useOSMDroidMap.Bool() && mapDriftingFixForGoogleMaps.Bool();
    }

    public static void initStrings() {
        String title = customTitleText.String();
        if ((isAppName(title) && !title.equals(getAppName())) || StringUtils.isBlank(title)) {
            customTitleText.setConfigString(getAppName());
        }

        if (customSavePath.String() == null || StrUtil.isAppName(customSavePath.String())) {
            customSavePath.setConfigString(StrUtil.getShortAppName());
        }

        reactionsOptions = new String[]{
            getString(R.string.doubleTapSendReaction),
            getString(R.string.doubleTapShowReactionsMenu),
            getString(R.string.doubleTapDisable),
            getString(R.string.Translate),
            getString(R.string.Reply),
            getString(R.string.AddToSavedMessages),
            getString(R.string.Forward),
            getString(R.string.NoQuoteForward),
        };
        perfClassOverrideOptions = new String[] {
                getString(R.string.Default),
                getString(R.string.AutoDownloadLow),
                getString(R.string.AutoDownloadMedium),
                getString(R.string.AutoDownloadHigh),
        };
        transcribeOptions = new String[]{
                getString(R.string.TranscribeProviderAuto),
                getString(R.string.TelegramPremium),
                getString(R.string.TranscribeProviderWorkersAI),
                getString(R.string.WhisperLocal)
        };
        titleFolderIconOptions = new String[] {
                getString(R.string.Disable),
                getString(R.string.PositionLeft),
                getString(R.string.PositionRight)
        };
        overrideForumStyleOptions = new String[] {
                getString(R.string.OverrideForumStyleDefault),
                getString(R.string.OverrideForumStyleClassic),
                getString(R.string.OverrideForumStyleTab)
        };
        autoDismissSuggestedChatsOptions = new String[] {
                getString(R.string.Disable),
                getString(R.string.AllChatsShort),
                getString(R.string.AutoDismissSuggestedChatsByRule)
        };
        disableNumberRoundingOptions = new String[] {
                getString(R.string.Enable),
                getString(R.string.DisableNumRounding),
                getString(R.string.DisableNumRoundingWithCommas)
        };
        applyHideMsgRegex();
        applyAutoBanByRegex();
    }

    public static void setTranscribeProvider(int provider) {
        transcribeProvider.setConfigInt(provider);
    }

    public static void setCfAccountID(String accountID) {
        cfAccountID.setConfigString(accountID);
    }

    public static void setCfApiToken(String apiToken) {
        cfApiToken.setConfigString(apiToken);
    }

    public static void updateUseSpoilerMediaChatList() {
        alwaysUseSpoilerForMediaChats.clear();
        String str = alwaysUseSpoilerForMedia.String();
        if ((str = StringUtils.trim(str)).isEmpty()) return;

        String[] chatIds = str.split(",");
        for (String chatId : chatIds) {
            try {
                alwaysUseSpoilerForMediaChats.add(Long.parseLong(chatId));
            } catch (Exception ex) {
                Log.e("nx-spoiler", String.format("failed to parse '%s' to long", chatId));
            }
        }
    }

    public static void updatePreferredTranslateTargetLangList() {
        AndroidUtilities.runOnUIThread(() -> {
            preferredTranslateTargetLangList.clear();
            String str = preferredTranslateTargetLang.String();
            if ((str = StringUtils.trim(str)).isEmpty()) return;

            String[] languages = str.split(",");
            if (languages.length == 0 || languages[0].trim().isEmpty()) return;
            for (String lang : languages) {
                lang = StringUtils.trim(lang).toLowerCase();
                preferredTranslateTargetLangList.add(lang);
            }
        }, 1000);
    }

    public static void applyOverriddenValue() {
        int type = 0;
        String target = null;
        ConfigItem[] checklist = new ConfigItem[] {
                overrideSettingBoolean,
                overrideSettingInteger,
                overrideSettingString,
                overrideSettingLong,
                overrideSettingFloat
        };
        for (int i = 0; i < checklist.length; ++i) {
            String s = checklist[i].String().trim();
            if (!StringUtils.isEmpty(s)) {
                target = s;
                type = i;
                break;
            }
        }
        if (target == null) return;
        Log.d("030-override", String.format("%d %s", type, target));
        String[] input = target.split(" ");
        if (input.length != 2) return;

        SharedPreferences.Editor edit = MessagesController.getGlobalMainSettings().edit();
        try {
            switch (type) {
                case 0:
                    edit.putBoolean(input[0], Boolean.parseBoolean(input[1])).commit();
                    break;
                case 1:
                    edit.putInt(input[0], Integer.parseInt(input[1])).commit();
                    break;
                case 2:
                    edit.putString(input[0], input[1]).commit();
                    break;
                case 3:
                    edit.putLong(input[0], Long.parseLong(input[1])).commit();
                    break;
                case 4:
                    edit.putFloat(input[0], Float.parseFloat(input[1])).commit();
            }
        } catch (Exception ex) {
            Log.e("030-override", "error parsing input", ex);
        }

        final int ftype = type;
        AndroidUtilities.runOnUIThread(() -> {
            checklist[ftype].setConfigString("");
        }, 100);
    }

    public static void applyCustomGetQueryBlacklist() {
        String[] queries = customGetQueryBlacklist.String().split(",");
        customGetQueryBlacklistData.clear();
        for (String q : queries) {
            customGetQueryBlacklistData.add(q.trim());
        }
    }

    public static void replaceCustomGetQueryBlacklist(Collection<String> newList) {
        customGetQueryBlacklistData.clear();
        customGetQueryBlacklistData.addAll(newList);
        String str = Arrays.toString(customGetQueryBlacklistData.toArray()).replace(" ", "");
        customGetQueryBlacklist.setConfigString(str.substring(1, str.length() - 1));
    }

    public static void applySearchBlacklist() {
        String[] queries = searchBlacklist.String().split(",");
        searchBlacklistData.clear();
        if (queries.length <= 1) return;
        for (String q : queries) {
            if (q.isBlank()) continue;
            try {
                searchBlacklistData.add(Long.valueOf(q.trim()));
            } catch (Exception ignored) {}
        }
        saveSearchBlacklist();
    }

    public static void saveSearchBlacklist() {
        String str = Arrays.toString(searchBlacklistData.toArray()).replace(" ", "");
        searchBlacklist.setConfigString(str.substring(1, str.length() - 1));
    }

    public static void applyPerformanceClassOverride(Integer c) {
        if (c == null && (c = MomoConfig.perfClassOverride.Int()) == -1) return;
        SharedConfig.overrideDevicePerformanceClass((c == 0) ? -1 : (c - 1));
    }

    public static void applyHideMsgRegex() {
        hideMessageRegexString = hideMessageRegex.String();
        if (TextUtils.isEmpty(hideMessageRegexString)) {
            hideMessageRegexPattern = null;
            Log.d("030-hide", "new regex: null");
            return;
        }
        hideMessageRegexPattern = Pattern.compile(hideMessageRegexString);
        Log.d("030-hide", "new regex: " + hideMessageRegexString);
    }
    public static void applyAutoBanByRegex() {
        autoDismissRegexString = autoDismissRegex.String();
        if (TextUtils.isEmpty(autoDismissRegexString)) {
            autoDismissRegexPattern = null;
            Log.d("030-autoban", "new regex: null");
            return;
        }
        autoDismissRegexPattern = Pattern.compile(autoDismissRegexString);
        Log.d("030-autoban", "new regex: " + autoDismissRegexString);
    }


    public static String customAllChatsText = null;
    public static ArrayList<TLRPC.MessageEntity> customAllChatsTextEntities = null;
    public static void loadCustomAllChatsText() {
        Log.d("030-???", MomoConfig.customAllChatsName.String());
        String[] spl = MomoConfig.customAllChatsName.String().split("\n");
        if (spl.length > 0) customAllChatsText = spl[0];
        if (spl.length < 2) {
            customAllChatsTextEntities = null;
            return;
        }
        try {
            if (customAllChatsTextEntities == null) customAllChatsTextEntities = new ArrayList<TLRPC.MessageEntity>();
            else customAllChatsTextEntities.clear();

            ArrayList<TLRPC.TL_messages_stickerSet> packs =
                    MediaDataController.getInstance(UserConfig.selectedAccount)
                            .getStickerSets(MediaDataController.TYPE_EMOJIPACKS);

            boolean found;
            for (int i = 1; i < spl.length; ++i) {
                found = false;
                String[] data = spl[i].split(",");
                TLRPC.TL_messageEntityCustomEmoji e = new TLRPC.TL_messageEntityCustomEmoji();
                e.offset = Integer.parseInt(data[0]);
                e.length = Integer.parseInt(data[1]);
                e.document_id = Long.parseLong(data[2]);
                // AnimatedEmojiSpan span = null;
                for (TLRPC.TL_messages_stickerSet p : packs) {
                    for (TLRPC.Document d : p.documents) {
                        if (d.id == e.document_id) {
                            e.document = d;
                            // span = new AnimatedEmojiSpan(d, null);
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                customAllChatsTextEntities.add((TLRPC.MessageEntity) e);
            }
        } catch (Exception ex) {
            Log.e("030-aci", "error while recreating entity, skipping", ex);
        }
    }

    public static void promptReportCrash(String errStr) {
        BaseFragment fragment = LaunchActivity.getLastFragment();
        if (fragment == null || SharedConfig.activeAccounts.isEmpty()) {
            AndroidUtilities.runOnUIThread(() -> MomoConfig.promptReportCrash(errStr), 100);
            return;
        }
        Context context = fragment.getContext();
        new AlertDialog.Builder(context)
                .setTitle(LocaleController.getString(useOldName.Bool() ? R.string.CrashDialogTitle : R.string.CrashDialogMomoTitle))
                .setMessage(LocaleController.getString(R.string.CrashDialogMessage))
                .setNeutralButton(LocaleController.getString(R.string.Copy), (__, ___) -> {
                    AndroidUtilities.addToClipboard(errStr);
                    lastCrashError.setConfigString(null);
                })
                .setPositiveButton(LocaleController.getString(R.string.Send), (__, ___) -> {
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
                    File cacheFile = new File(ApplicationLoader.applicationContext.getCacheDir(), timestamp + ".m0m0-crash.txt");
                    FileUtil.writeUtf8String(errStr + getNekoConfigValuesAsString(), cacheFile);
                    ShareUtil.shareFile(context, cacheFile);
                })
                .setNegativeButton(LocaleController.getString(R.string.Cancel), (__, ___) -> lastCrashError.setConfigString(null))
                .create().show();
    }

    public static void init() {
        initStrings();
        try {
            SharedConfig.loadConfig();
            if (lastCrashError.String() != null && !lastCrashError.String().isBlank()) {
                final String errStr = lastCrashError.String();
                AndroidUtilities.runOnUIThread(() -> {
                    BaseFragment fragment = LaunchActivity.getLastFragment();
                    // delay if not ready
                    if (fragment == null || SharedConfig.activeAccounts.isEmpty()) {
                        AndroidUtilities.runOnUIThread(() -> MomoConfig.promptReportCrash(errStr), 100);
                    } else {
                        MomoConfig.promptReportCrash(errStr);
                    }
                });
                lastCrashError.setConfigString(null);
            } else {
                Log.d("030-cfg", "lastCrashError is empty");
            }
            updateUseSpoilerMediaChatList();
            updatePreferredTranslateTargetLangList();
            applyCustomGetQueryBlacklist();
            applySearchBlacklist();
            applyPerformanceClassOverride(null);
            loadCustomAllChatsText();
            if (aidlOnLaunch.Bool() && translationProvider.Int() == Translator.providerFirefox) {
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
            EvilLeakerKiller.threshold = memLeakThreshold.Int();
            if (MomoConfig.disableSystemAccount.Bool()) {
                NekoXConfig.ensureSystemAccountState(UserConfig.selectedAccount, true);
            }
            ModUtil.filterJoinRequestsJob(true);

            if (!MomoConfig.enableUnifiedPush.Bool() || UnifiedPush.getSavedDistributor(ApplicationLoader.applicationContext) != null)
                return;

            AndroidUtilities.runOnUIThread(() -> {
                BaseFragment fragment = LaunchActivity.getLastFragment();
                if (fragment == null || SharedConfig.activeAccounts.isEmpty()) return;
                Context context = fragment.getContext();
                new AlertDialog.Builder(context)
                        .setTitle(LocaleController.getString(R.string.SetupUnifiedPush))
                        .setMessage(LocaleController.getString(R.string.SetupUnifiedPushInfo))
                        .setNeutralButton(LocaleController.getString(R.string.SettingsHelp), (__, ___) -> {
                            Browser.openUrl(context, "https://github.com/im030/Momogram/tree/test?tab=readme-ov-file#how-do-i-get-notifications-working");
                        })
                        .setPositiveButton(LocaleController.getString(R.string.Close), null)
                        .create().show();
            });
        } catch (Exception ex) {
            Log.e("030-neko", "failed to load part of neko config", ex);
        }
    }

    public static HashMap<Integer, ArrayList<Pair<Integer, String>>> momoConfigStrings;
    public static HashMap<Integer, ArrayList<Pair<Integer, String>>> getStringsForSearch() {
        if (momoConfigStrings != null) return momoConfigStrings;
        HashMap<Integer, ArrayList<Pair<Integer, String>>> ret = new HashMap<>();
        for (Field f : MomoConfig.class.getDeclaredFields()) {
            if (f.getType() == ConfigItem.class && Modifier.isStatic(f.getModifiers())) {
                try {
                    ConfigItem item = (ConfigItem) f.get(null);
                    assert item != null;
                    if (item.page == 0) continue;
                    ret.putIfAbsent(item.page, new ArrayList<>());
                    int id = item.id;
                    if (id == 0) {
                        ret.get(item.page).add(Pair.create(-1, LocaleController.getString(item.key)));
                    } else {
                        ret.get(item.page).add(Pair.create(id, LocaleController.getString(id)));
                    }
                } catch (IllegalAccessException e) {
                    Log.e("030-cfg", "error getting field " + f.getName(), e);
                }
            }
        }
        return momoConfigStrings = ret;
    }

    public static HashMap<String, Pair<Integer, ConfigItem>> momoConfigKeyMap;
    public static Pair<Integer, ConfigItem> getConfigByKey(String key) {
        Log.d("030-mo", "getConfigByKey: " + key);
        if (momoConfigKeyMap != null) return momoConfigKeyMap.get(key);
        HashMap<String, Pair<Integer, ConfigItem>> ret = new HashMap<>();
        for (Field f : MomoConfig.class.getDeclaredFields()) {
            if (f.getType() == ConfigItem.class && Modifier.isStatic(f.getModifiers())) {
                try {
                    ConfigItem item = (ConfigItem) f.get(null);
                    assert item != null;
                    if (item.page == 0) continue;
                    Log.d("030-mo", String.format("found '%s' in page %d", item.key, item.page));
                    ret.put(item.key, Pair.create(item.page, item));
                } catch (IllegalAccessException e) {
                    Log.e("030-cfg", "error getting field " + f.getName(), e);
                }
            }
        }
        ret.put(MomoChatSettingsActivity.MSG_MENU_KEY, Pair.create(CHAT, null));
        ret.put(MomoChatSettingsActivity.PROFILE_MENU_KEY, Pair.create(CHAT, null));
        momoConfigKeyMap = ret;
        return ret.get(key);
    }

    public static String getNekoConfigValuesAsString() {
        StringBuilder sb = new StringBuilder("\n\nMomoConfig:\n");
        for (Field f : MomoConfig.class.getDeclaredFields()) {
            if (f.getType() == ConfigItem.class && Modifier.isStatic(f.getModifiers())) {
                if (f.getName().contains("Api"))
                    continue;

                try {
                    ConfigItem item = (ConfigItem) f.get(null);
                    if (item == null) {
                        sb.append(String.format("(failed to get %s)\n", f.getName()));
                    } else {
                        sb.append(String.format("%s = %s\n", f.getName(), item.value));
                    }
                } catch (IllegalAccessException e) {
                    sb.append(String.format("(failed to get %s, %s: %s | %s)\n",
                            f.getName(), e.getClass().getName(), e.getMessage(), e.getCause()));
                }
            }
        }
        return sb.toString();
    }

    public static void findAndOpenMomoSettings(LaunchActivity activity, String s) {
        if (activity == null || TextUtils.isEmpty(s)) {
            return;
        }
        String k = s;
        Uri uri = Uri.parse(s);
        String queryKey = uri.getQueryParameter("k");
        if (!TextUtils.isEmpty(queryKey)) {
            k = queryKey;
        } else if (s.contains("=")) {
            String[] spl = s.split("=", 2);
            if (spl.length == 2 && !TextUtils.isEmpty(spl[1])) {
                k = spl[1];
            }
        }
        Pair<Integer, ConfigItem> loc = MomoConfig.getConfigByKey(k);
        if (loc != null) {
            switch (loc.first) {
                case ConfigItem.GENERAL:
                    activity.presentFragment(new MomoGeneralSettingsActivity().setScrollTo(k, 0, true));
                    break;
                case ConfigItem.APPEARANCE:
                    activity.presentFragment(new MomoAppearanceSettingsActivity().setScrollTo(k, 0, true));
                    break;
                case ConfigItem.CHAT:
                    activity.presentFragment(new MomoChatSettingsActivity().setScrollTo(k, 0, true));
                    break;
                case ConfigItem.EXPERIMENTAL:
                    activity.presentFragment(new MomoExperimentalSettingsActivity().setScrollTo(k, 0, true));
                    break;
                default:
                    break;
            }
        } else {
            BulletinFactory.of(LaunchActivity.getLastFragment())
                    .createSimpleBulletin(R.raw.error, LocaleController.getString(R.string.MomoSettingNotFound))
                    .show();
        }
    }

    public static int resetModConfig() {
        int err = 0;
        for (Field f : MomoConfig.class.getDeclaredFields()) {
            if (f.getType() == ConfigItem.class && Modifier.isStatic(f.getModifiers())) {
                if (f.getName().contains("Api"))
                    continue;

                try {
                    ConfigItem item = (ConfigItem) f.get(null);
                    if (item == null) {
                        ++err;
                    } else {
                        item.reset();
                    }
                } catch (IllegalAccessException e) {
                    Log.e("030-neko", String.format("(failed to get %s, %s: %s | %s)\n",
                            f.getName(), e.getClass().getName(), e.getMessage(), e.getCause()), e);
                }
            }
        }
        return err;
    }

}
