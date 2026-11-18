package moe.hx030.momogram.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.Strictness;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.QuoteSpan;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.Components.spoilers.SpoilerEffect;
import org.telegram.ui.ProfileActivity;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MessageDetailsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private static final int MAX_SERIALIZE_DEPTH = 32;
    private static final int MAX_COLLECTION_ITEMS = 512;
    private static final int MAX_ARRAY_ITEMS = 256;
    private static final int MAX_MAP_ENTRIES = 1024;
    private static final int MAX_OBJECT_FIELDS = 1024;
    private static final int MAX_TOTAL_NODES = 20000;
    private static final int MAX_TOTAL_CHARS = 2 * 1024 * 1024;
    private static final int MAX_STRING_LENGTH = 16 * 1024;
    private static final int MAX_BYTE_ARRAY_LENGTH = 64 * 1024;

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private MessageObject messageObject;
    private TL_stories.StoryItem storyItem;
    private TLRPC.MessageMedia media;
    private TLRPC.Chat fromChat;
    private TLRPC.User fromUser;
    private String filePath;
    private String fileName;
    private String messageDetailsJson;
    private String messageDetailsPrettyJsonHead;
    private String messageDetailsPrettyJson;
    private boolean detailExpanded = false;
    private final Object jsonSource;

    private int rowCount;

    private int idRow;
    private int scheduledRow;
    private int messageRow;
    private int captionRow;
    private int groupRow;
    private int channelRow;
    private int fromRow;
    private int botRow;
    private int dateRow;
    private int editedRow;
    private int forwardRow;
    private int fileNameRow;
    private int filePathRow;
    private int fileSizeRow;
    private int dcRow;
    private int buttonsRow;
    private int emptyRow;
    private int rawRow;
    private int emptyRow2;
    private int exportRow;
    private int endRow;

    private UndoView copyTooltip;

    public static final Gson gson = new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .setExclusionStrategies(new Exclusion())
            .registerTypeHierarchyAdapter(byte[].class, new ByteArrayToBase64TypeAdapter()).create();

    public static final Gson prettyGson = new GsonBuilder()
            .setPrettyPrinting()
            .setStrictness(Strictness.LENIENT)
            .setExclusionStrategies(new Exclusion())
            .registerTypeHierarchyAdapter(byte[].class, new ByteArrayToBase64TypeAdapter()).create();

    private static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Base64.decode(json.getAsString(), Base64.NO_WRAP);
        }

        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.encodeToString(src, Base64.NO_WRAP));
        }
    }

    public static class Exclusion implements ExclusionStrategy {
        public boolean shouldSkipClass(Class<?> arg0) {
            return false;
        }

        public boolean shouldSkipField(FieldAttributes f) {
            boolean ret = f.getName().equals("disableFree") ||
                    f.getName().equals("networkType") ||
                    f.getDeclaringClass() == android.content.res.ColorStateList.class ||
//                    f.getDeclaringClass() == TLRPC.TL_messageEntityBlockquote.class ||
                    f.getDeclaringClass() == QuoteSpan.class;
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d(String.format("serializing field %s (%s) = %s",
                        f.getName(), f.getDeclaringClass().getName(), ret));
            } else if (ret) {
                Log.d("030-json", String.format("skipped unsupported field with name '%s' %s",
                        f.getName(), f.getDeclaringClass().getName()));
            }
            return ret;
        }
    }


    public MessageDetailsActivity(MessageObject messageObject) {
        this.messageObject = messageObject;
        this.jsonSource = messageObject;
        initFromPeer(messageObject.messageOwner.from_id);
        initFile();
        initJsonState();
    }

    public MessageDetailsActivity(TL_stories.StoryItem story) {
        this.storyItem = story;
        this.jsonSource = story;
        initFromPeer(story.from_id);
        initFile();
        initJsonState();
    }

    private void initJsonState() {
        messageDetailsJson = LocaleController.getString(R.string.Loading);
        messageDetailsPrettyJson = messageDetailsJson;
        messageDetailsPrettyJsonHead = messageDetailsJson;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        generateJsonAsync(jsonSource);

        return true;
    }

    @SuppressLint({"NewApi", "RtlHardcoded"})
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.MessageDetails));

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
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == exportRow) {
                if (AndroidUtilities.addToClipboard(messageDetailsJson)) {
                    copyTooltip.setInfoText(LocaleController.getString(R.string.TextCopied));
                    copyTooltip.showWithAction(0, UndoView.ACTION_TEXT_COPIED, null, null);
                } else {
                    copyTooltip.setInfoText(LocaleController.getString(R.string.ErrorOccurred));
                    copyTooltip.showWithAction(0, UndoView.ACTION_GIGAGROUP_CANCEL, null, null);
                }
            } else if (position == rawRow) {
                ((TextDetailSettingsCell) view).setValue(detailExpanded ? messageDetailsPrettyJsonHead : messageDetailsPrettyJson);
                detailExpanded = !detailExpanded;
            } else if (position != endRow && position != emptyRow && (view instanceof TextDetailSettingsCell textCell)) {
                try {
                    if (AndroidUtilities.addToClipboard(textCell.getValueTextView().getText())) {
                        copyTooltip.showWithAction(0, UndoView.ACTION_TEXT_COPIED, null, null);
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }

        });
        listView.setOnItemLongClickListener((view, position) -> {
            if (position == filePathRow) {
                AndroidUtilities.runOnUIThread(() -> {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("application/octet-stream");
                    if (Build.VERSION.SDK_INT >= 24) {
                        try {
                            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getParentActivity(), BuildConfig.APPLICATION_ID + ".provider", new File(filePath)));
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception ignore) {
                            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
                        }
                    } else {
                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
                    }
                    startActivityForResult(Intent.createChooser(intent, LocaleController.getString(R.string.ShareFile)), 500);
                });
            } else if (position == channelRow || position == groupRow) {
                if (fromChat != null) {
                    Bundle args = new Bundle();
                    args.putLong("chat_id", fromChat.id);
                    ProfileActivity fragment = new ProfileActivity(args);
                    presentFragment(fragment);
                }
            } else if (position == fromRow) {
                if (fromUser != null) {
                    Bundle args = new Bundle();
                    args.putLong("user_id", fromUser.id);
                    ProfileActivity fragment = new ProfileActivity(args);
                    presentFragment(fragment);
                }
            } else if (position == rawRow) {
                if (AndroidUtilities.addToClipboard(messageDetailsPrettyJson)) {
                    copyTooltip.setInfoText(LocaleController.getString(R.string.TextCopied));
                    copyTooltip.showWithAction(0, UndoView.ACTION_TEXT_COPIED, null, null);
                }
            } else {
                return false;
            }
            return true;
        });

        copyTooltip = new UndoView(context);
        copyTooltip.setInfoText(LocaleController.getString(R.string.TextCopied));
        frameLayout.addView(copyTooltip, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void updateRows() {
        rowCount = 0;
        idRow = rowCount++;
        scheduledRow = !isStory() && messageObject.scheduled ? rowCount++ : -1;
        messageRow = isStory() || TextUtils.isEmpty(messageObject.messageText) ? -1 : rowCount++;
        captionRow = TextUtils.isEmpty(getCaption()) ? -1 : rowCount++;
        groupRow = fromChat != null && !fromChat.broadcast ? rowCount++ : -1;
        channelRow = fromChat != null && fromChat.broadcast ? rowCount++ : -1;
        fromRow = fromUser != null || (messageObject != null && messageObject.messageOwner.post_author != null) ? rowCount++ : -1;
        botRow = fromUser != null && fromUser.bot ? rowCount++ : -1;
        dateRow = getDate() != 0 ? rowCount++ : -1;
        editedRow = getEditDate() != 0 ? rowCount++ : -1;
        forwardRow = !isStory() && messageObject.isForwarded() ? rowCount++ : -1;
        fileNameRow = TextUtils.isEmpty(fileName) ? -1 : rowCount++;
        filePathRow = TextUtils.isEmpty(filePath) ? -1 : rowCount++;
        fileSizeRow = !isStory() && messageObject.getSize() != 0 ? rowCount++ : -1;
        if (media != null && ((media.photo != null && media.photo.dc_id > 0) ||
                        (media.document != null && media.document.dc_id > 0))) {
            dcRow = rowCount++;
        } else {
            dcRow = -1;
        }
        buttonsRow = messageObject != null && messageObject.messageOwner.reply_markup instanceof TLRPC.TL_replyInlineMarkup ? rowCount++ : -1;
        emptyRow = rowCount++;
        rawRow = rowCount++;
        emptyRow2 = rowCount++;
        exportRow = rowCount++;
        endRow = rowCount++;
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

    private void initFromPeer(TLRPC.Peer from_id) {
        if (from_id != null && from_id.channel_id != 0) {
            fromChat = getMessagesController().getChat(from_id.channel_id);
        } else if (from_id != null && from_id.chat_id != 0) {
            fromChat = getMessagesController().getChat(from_id.chat_id);
        }
        if (from_id != null && from_id.user_id != 0) {
            fromUser = getMessagesController().getUser(from_id.user_id);
        }
    }

    private void initFile() {
        TLRPC.Message messageOwner = (storyItem != null) ? null : messageObject.messageOwner;
        filePath = (storyItem != null) ? storyItem.attachPath : messageOwner.attachPath;

        if (!TextUtils.isEmpty(filePath)) {
            File temp = new File(filePath);
            if (!temp.exists()) {
                filePath = null;
            }
        }
        if (filePath == null && storyItem != null) return;
        if (TextUtils.isEmpty(filePath)) {
            filePath = FileLoader.getInstance(currentAccount).getPathToMessage(messageOwner).toString();
            File temp = new File(filePath);
            if (!temp.exists()) {
                filePath = null;
            }
        }
        if (TextUtils.isEmpty(filePath)) {
            filePath = FileLoader.getInstance(currentAccount).getPathToAttach(messageObject.getDocument(), true).toString();
            File temp = new File(filePath);
            if (!temp.isFile()) {
                filePath = null;
            }
        }

        media = isStory() ? storyItem.media : messageObject.messageOwner.media;
        if (media != null && media.document != null) {
            if (TextUtils.isEmpty(media.document.file_name)) {
                for (int a = 0; a < media.document.attributes.size(); a++) {
                    if (media.document.attributes.get(a) instanceof TLRPC.TL_documentAttributeFilename) {
                        fileName = media.document.attributes.get(a).file_name;
                    }
                }
            } else {
                fileName = media.document.file_name;
            }
        }
    }

    private void generateJsonAsync(Object obj) {
        Utilities.globalQueue.postRunnable(() -> generateJson(obj));
    }

    private void generateJson(Object obj) {
        try {
            String generatedJson = "failed to generate json";
            String generatedPrettyJson = generatedJson;
            String generatedPrettyJsonHead = generatedJson;
            try {
                Object safeCopy = buildSafeCopy(obj, new SafeCopyContext(), 0, MAX_SERIALIZE_DEPTH);
                generatedJson = gson.toJson(safeCopy);
                generatedPrettyJson = prettyGson.toJson(safeCopy);
                String[] spl = generatedPrettyJson.split("\n");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(3, spl.length); ++i) sb.append(spl[i]).append("\n");
                sb.append("...");
                generatedPrettyJsonHead = sb.toString();
            } catch (Throwable t) {
                generatedJson += (", " + t.getMessage());
                generatedPrettyJson = generatedJson;
                generatedPrettyJsonHead = generatedPrettyJson;
                if (t instanceof OutOfMemoryError) {
                    Log.e("030-json", "Failed to serialize message details", t);
                } else {
                    FileLog.e(t);
                }
            }
            final String finalJson = generatedJson;
            final String finalPrettyJson = generatedPrettyJson;
            final String finalPrettyJsonHead = generatedPrettyJsonHead;
            AndroidUtilities.runOnUIThread(() -> {
                messageDetailsJson = finalJson;
                messageDetailsPrettyJson = finalPrettyJson;
                messageDetailsPrettyJsonHead = finalPrettyJsonHead;
                if (listAdapter != null) {
                    listAdapter.notifyItemChanged(rawRow);
                }
            });
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    private String safeToJson(Object obj) {
        try {
            Object safeCopy = buildSafeCopy(obj, new SafeCopyContext(), 0, Math.min(4, MAX_SERIALIZE_DEPTH));
            return gson.toJson(safeCopy);
        } catch (Throwable t) {
            return "[ERROR:" + t.getClass().getSimpleName() + "]";
        }
    }

    private static class SafeCopyContext {
        final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        int nodes;
        long chars;
        boolean exhausted;

        boolean consumeNode() {
            if (exhausted) return false;
            if (++nodes > MAX_TOTAL_NODES) {
                exhausted = true;
                return false;
            }
            return true;
        }

        boolean consumeChars(int count) {
            if (exhausted) return false;
            chars += count;
            if (chars > MAX_TOTAL_CHARS) {
                exhausted = true;
                return false;
            }
            return true;
        }
    }

    private Object buildSafeCopy(Object obj, SafeCopyContext ctx, int depth, int maxDepth) {
        if (obj == null) return null;
        if (!ctx.consumeNode()) return "[TRUNCATED]";
        if (ctx.visited.contains(obj)) return "[REFERENCE]";
        if (depth > maxDepth) return "[MAX_DEPTH]";

        // Primitives and strings
        if (obj instanceof String) {
            String str = (String) obj;
            if (str.length() > MAX_STRING_LENGTH) {
                str = str.substring(0, MAX_STRING_LENGTH) + "[TRUNCATED:" + str.length() + "]";
            }
            if (!ctx.consumeChars(str.length())) return "[TRUNCATED]";
            return str;
        }
        if (obj instanceof Number || obj instanceof Boolean || obj instanceof Character) {
            return obj;
        }
        if (obj instanceof Enum<?>) {
            return ((Enum<?>) obj).name();
        }
        if (obj instanceof Class<?>) {
            return ((Class<?>) obj).getName();
        }
        if (obj instanceof byte[]) {
            byte[] bytes = (byte[]) obj;
            if (bytes.length > MAX_BYTE_ARRAY_LENGTH) {
                return "[BYTES:" + bytes.length + "]";
            }
            if (!ctx.consumeChars((int) ((long) bytes.length * 2))) return "[TRUNCATED]";
            return bytes;
        }
        if (shouldSummarizeObject(obj)) {
            return summarizeObject(obj);
        }

        ctx.visited.add(obj);

        // Collections
        if (obj instanceof Collection<?>) {
            Collection<?> col = (Collection<?>) obj;
            List<Object> copy = new ArrayList<>(Math.min(col.size(), MAX_COLLECTION_ITEMS) + 1);
            int index = 0;
            for (Object item : col) {
                if (index >= MAX_COLLECTION_ITEMS) {
                    copy.add("[TRUNCATED:" + col.size() + "]");
                    break;
                }
                copy.add(buildSafeCopy(item, ctx, depth + 1, maxDepth));
                if (ctx.exhausted) break;
                index++;
            }
            return copy;
        }

        // Maps
        if (obj instanceof Map<?, ?>) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            int index = 0;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                if (index >= MAX_MAP_ENTRIES) {
                    copy.put("[TRUNCATED]", ((Map<?, ?>) obj).size());
                    break;
                }
                Object key = entry.getKey();
                Object value = entry.getValue();
                copy.put(buildSafeCopy(key, ctx, depth + 1, maxDepth),
                        buildSafeCopy(value, ctx, depth + 1, maxDepth));
                if (ctx.exhausted) break;
                index++;
            }
            return copy;
        }

        // Arrays
        if (obj.getClass().isArray()) {
            int len = Array.getLength(obj);
            List<Object> copy = new ArrayList<>(Math.min(len, MAX_ARRAY_ITEMS) + 1);
            for (int i = 0; i < Math.min(len, MAX_ARRAY_ITEMS); i++) {
                copy.add(buildSafeCopy(Array.get(obj, i), ctx, depth + 1, maxDepth));
                if (ctx.exhausted) break;
            }
            if (len > MAX_ARRAY_ITEMS) {
                copy.add("[TRUNCATED:" + len + "]");
            }
            return copy;
        }

        // Fallback: arbitrary object
        Map<String, Object> copy = new LinkedHashMap<>();
        Class<?> cls = obj.getClass();
        int fieldCount = 0;
        while (cls != null) {
            if (shouldStopReflectingAt(cls)) {
                break;
            }
            Field[] fields = cls.getDeclaredFields();
            for (Field f : fields) {
                if (Modifier.isStatic(f.getModifiers())) continue; // skip static
                String fieldName = f.getName();
                if (fieldName.equals("parentRichText") ||
                        fieldName.equals("networkType") ||
                        fieldName.equals("disableFree")) {
                    continue;
                }
                if (fieldCount >= MAX_OBJECT_FIELDS) {
                    copy.put("[TRUNCATED_FIELDS]", cls.getName());
                    return copy;
                }
                f.setAccessible(true);
                try {
                    Object value = f.get(obj);
                    copy.put(fieldName, buildSafeCopy(value, ctx, depth + 1, maxDepth));
                    if (ctx.exhausted) return copy;
                } catch (IllegalAccessException e) {
                    copy.put(fieldName, "[ACCESS_ERROR]");
                } catch (Throwable t) {
                    copy.put(fieldName, "[ERROR:" + t.getClass().getSimpleName() + "]");
                }
                fieldCount++;
            }
            cls = cls.getSuperclass();
        }
        return copy;
    }

    private boolean shouldSummarizeObject(Object obj) {
        return obj instanceof SpoilerEffect ||
                obj instanceof ChatMessageCell ||
                obj instanceof Context ||
                obj instanceof View ||
                obj instanceof ViewGroup ||
                obj instanceof Bundle ||
                obj instanceof Throwable ||
                obj instanceof Thread ||
                obj instanceof ClassLoader;
    }

    private boolean shouldStopReflectingAt(Class<?> cls) {
        String name = cls.getName();
        return name.startsWith("java.lang.Class") ||
                name.startsWith("java.lang.ClassLoader") ||
                name.startsWith("java.lang.Thread") ||
                name.startsWith("android.") ||
                name.startsWith("androidx.") ||
                name.startsWith("java.lang.reflect.");
    }

    private String summarizeObject(Object obj) {
        return "[OBJECT:" + obj.getClass().getSimpleName() + "@" + System.identityHashCode(obj) + "]";
    }


    private boolean isStory() {
        return storyItem != null;
    }

    private CharSequence getCaption() {
        if (isStory()) return storyItem.caption;
        return messageObject.caption;
    }

    private int getDate() {
        if (isStory()) return storyItem.date;
        return messageObject.messageOwner.date;
    }

    private int getEditDate() {
        if (isStory()) return storyItem.edited ? 1 : 0;
        return messageObject.messageOwner.edit_date;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 1: {
                    if (position == endRow) {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case 2: {
                    TextDetailSettingsCell textCell = (TextDetailSettingsCell) holder.itemView;
                    textCell.setMultilineDetail(true);
                    boolean divider = position + 1 != emptyRow;
                    if (position == idRow) {
                        textCell.setTextAndValue("ID", String.valueOf(isStory() ? storyItem.id : messageObject.messageOwner.id), divider);
                    } else if (position == messageRow) {
                        textCell.setTextAndValue("Message", messageObject.messageText, divider);
                    } else if (position == captionRow) {
                        textCell.setTextAndValue("Caption", getCaption(), divider);
                    } else if (position == channelRow || position == groupRow) {
                        StringBuilder builder = new StringBuilder();
                        builder.append(fromChat.title);
                        builder.append("\n");
                        if (!TextUtils.isEmpty(fromChat.username)) {
                            builder.append("@");
                            builder.append(fromChat.username);
                            builder.append("\n");
                        }
                        builder.append(fromChat.id);
                        textCell.setTextAndValue(position == channelRow ? "Channel" : "Group", builder.toString(), divider);
                    } else if (position == fromRow) {
                        StringBuilder builder = new StringBuilder();
                        if (fromUser != null) {
                            builder.append(ContactsController.formatName(fromUser.first_name, fromUser.last_name));
                            builder.append("\n");
                            if (!TextUtils.isEmpty(fromUser.username)) {
                                builder.append("@");
                                builder.append(fromUser.username);
                                builder.append("\n");
                            }
                            builder.append(fromUser.id);
                        } else if (!isStory()) {
                            builder.append(messageObject.messageOwner.post_author);
                        }
                        textCell.setTextAndValue("From", builder.toString(), divider);
                    } else if (position == botRow) {
                        textCell.setTextAndValue("Bot", "Yes", divider);
                    } else if (position == dateRow) {
                        long date = (long) getDate() * 1000;
                        textCell.setTextAndValue((!isStory() && messageObject.scheduled) ? "Scheduled date" : "Date",
                                getDate() == 0x7ffffffe ? "When online" :
                                        LocaleController.formatString(R.string.formatDateAtTime, LocaleController.getInstance().getFormatterYear().format(new Date(date)),
                                            LocaleController.getInstance().getFormatterDay().format(new Date(date))), divider);
                    } else if (position == editedRow) {
                        long date = getEditDate();
                        String dateStr = (date == 1) ? "Unknown" :
                                LocaleController.formatString(R.string.formatDateAtTime,
                                    LocaleController.getInstance().getFormatterYear().format(new Date(date * 1000)), LocaleController.getInstance().getFormatterDay().format(new Date(date * 1000)));
                        textCell.setTextAndValue("Edited", dateStr, divider);
                    } else if (position == forwardRow) {
                        StringBuilder builder = new StringBuilder();
                        if (messageObject.messageOwner.fwd_from.from_id == null) {
                            builder.append(messageObject.messageOwner.fwd_from.from_name).append('\n');
                        } else {
                            if (messageObject.messageOwner.fwd_from.from_id.channel_id != 0) {
                                TLRPC.Chat chat = getMessagesController().getChat(messageObject.messageOwner.fwd_from.from_id.channel_id);
                                builder.append(chat.title);
                                builder.append("\n");
                                if (!TextUtils.isEmpty(chat.username)) {
                                    builder.append("@");
                                    builder.append(chat.username);
                                    builder.append("\n");
                                }
                                builder.append(chat.id);
                            } else if (messageObject.messageOwner.fwd_from.from_id.user_id != 0) {
                                TLRPC.User user = getMessagesController().getUser(messageObject.messageOwner.fwd_from.from_id.channel_id);
                                if(user!=null){
                                    builder.append(ContactsController.formatName(user.first_name, user.last_name));
                                    builder.append("\n");
                                    if (!TextUtils.isEmpty(user.username)) {
                                        builder.append("@");
                                        builder.append(user.username);
                                        builder.append("\n");
                                    }
                                    builder.append(user.id);
                                } else builder.append("null user");
                            } else if (!TextUtils.isEmpty(messageObject.messageOwner.fwd_from.from_name)) {
                                builder.append(messageObject.messageOwner.fwd_from.from_name);
                            }
                        }

                        long original_date = (long) messageObject.messageOwner.fwd_from.date * 1000;
                        String year = LocaleController.getInstance().getFormatterYear().format(new Date(original_date));
                        String day = LocaleController.getInstance().getFormatterDay().format(new Date(original_date));
                        builder.append("\n")
                                .append(LocaleController.formatString(R.string.formatDateAtTime, year, day));

                        textCell.setTextAndValue("Forward from", builder.toString(), divider);
                    } else if (position == fileNameRow) {
                        textCell.setTextAndValue("File name", fileName, divider);
                    } else if (position == filePathRow) {
                        textCell.setTextAndValue("File path", filePath, divider);
                    } else if (position == fileSizeRow) {
                        textCell.setTextAndValue("File size", AndroidUtilities.formatFileSize(messageObject.getSize()), divider);
                    } else if (position == dcRow) {
                        if (media.photo != null && media.photo.dc_id > 0) {
                            textCell.setTextAndValue("DC", String.valueOf(media.photo.dc_id), divider);
                        } else if (media.document != null && media.document.dc_id > 0) {
                            textCell.setTextAndValue("DC", String.valueOf(media.document.dc_id), divider);
                        }
                    } else if (position == scheduledRow) {
                        textCell.setTextAndValue("Scheduled", "Yes", divider);
                    } else if (position == buttonsRow) {
                        textCell.setTextAndValue("Buttons", safeToJson(messageObject.messageOwner.reply_markup), divider);
                    } else if (position == rawRow) {
                        textCell.setTextAndValue("Raw JSON", messageDetailsPrettyJsonHead, divider);
                    }
                    break;
                }
                case 3: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == exportRow) {
                        textCell.setText(LocaleController.getString(R.string.ExportAsJson), false);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position != endRow && position != emptyRow;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;
            switch (viewType) {
                case 1:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 2:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == endRow || position == emptyRow || position == emptyRow2) {
                return 1;
            } else if (position == exportRow) {
                return 3;
            } else {
                return 2;
            }
        }
    }
}
