package moe.hx030.momogram.utils;

import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessageSuggestionParams;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessageChatArguments;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_iv;
import org.telegram.tgnet.tl.TL_keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RichMessageForwardHelper {

    private static final int CAPTION_LIMIT = 969;
    private static final int TEXT_LIMIT = 4000;
    private static final int GROUP_SIZE = 10;

    private static final ArrayList<PendingText> pendingTexts = new ArrayList<>();
    private static final HashMap<Integer, NotificationCenter.NotificationCenterDelegate> observers = new HashMap<>();

    public static boolean canUse(int account) {
        UserConfig config = UserConfig.getInstance(account);
        return !config.isPremium() && !config.isBot();
    }

    public static boolean isEligible(ArrayList<MessageObject> messages) {
        return messages != null && messages.size() == 1 && messages.get(0) != null &&
                messages.get(0).messageOwner != null && messages.get(0).messageOwner.rich_message != null;
    }

    public static boolean trySend(int account, ArrayList<MessageObject> messages, long peer, boolean notify, int scheduleDate,
                                  int scheduleRepeatPeriod, MessageObject replyToTopMsg, long monoForumPeer,
                                  SendMessageChatArguments chatArguments,
                                  MessageSuggestionParams suggestionParams) {
        if (!canUse(account) || !isEligible(messages)) {
            return false;
        }
        TL_iv.RichMessage richMessage = messages.get(0).messageOwner.rich_message;
        ArrayList<MediaItem> media = collectMedia(account, richMessage);
        FormattedText text = toFormattedText(richMessage);
        if (media.isEmpty() && text.length() == 0) {
            return false;
        }

        if (media.isEmpty()) {
            sendTextParts(account, peer, text, null, notify, scheduleDate, scheduleRepeatPeriod,
                    replyToTopMsg, monoForumPeer, chatArguments, suggestionParams);
            return true;
        }

        PendingText pending = null;
        if (text.length() > CAPTION_LIMIT) {
            pending = new PendingText(account, peer, text, notify, scheduleDate, scheduleRepeatPeriod,
                    replyToTopMsg, monoForumPeer, chatArguments, suggestionParams);
            synchronized (pendingTexts) {
                pendingTexts.add(pending);
            }
            ensureObserver(account);
        }

        FormattedText caption = pending == null && text.length() > 0 ? text : null;
        sendMedia(account, peer, media, caption, notify, scheduleDate, scheduleRepeatPeriod,
                replyToTopMsg, monoForumPeer, chatArguments, suggestionParams, pending);
        return true;
    }

    private static void sendMedia(int account, long peer, ArrayList<MediaItem> media, FormattedText caption, boolean notify,
                                  int scheduleDate, int scheduleRepeatPeriod, MessageObject replyToTopMsg,
                                  long monoForumPeer, SendMessageChatArguments chatArguments,
                                  MessageSuggestionParams suggestionParams, PendingText pending) {
        long groupId = 0;
        for (int i = 0; i < media.size(); i++) {
            MediaItem item = media.get(i);
            boolean first = i == 0;
            boolean last = i == media.size() - 1;
            if (i % GROUP_SIZE == 0) {
                groupId = media.size() > 1 ? Utilities.random.nextLong() : 0;
            }

            SendMessagesHelper.SendMessageParams params;
            if (item.photo != null) {
                params = SendMessagesHelper.SendMessageParams.of(item.photo, item.path, peer, null, replyToTopMsg,
                        first && caption != null ? caption.text : null, first && caption != null ? caption.entities : null,
                        null, null, notify, scheduleDate, scheduleRepeatPeriod,
                        0, item.photo, false, false);
            } else {
                params = SendMessagesHelper.SendMessageParams.of(item.document, null, item.path, peer, null,
                        replyToTopMsg, first && caption != null ? caption.text : null,
                        first && caption != null ? caption.entities : null, null, null, notify, scheduleDate,
                        scheduleRepeatPeriod, 0, item.document, null, false, false);
            }
            if (params.params == null) {
                params.params = new HashMap<>();
            }
            params.params.put("send_by_ref", "1");
            if (groupId != 0) {
                params.params.put("groupId", "" + groupId);
                if (i % GROUP_SIZE == GROUP_SIZE - 1 || last) {
                    params.params.put("final", "1");
                }
            }
            params.monoForumPeer = monoForumPeer;
            params.suggestionParams = suggestionParams;
            params.sendMessageChatArguments = chatArguments;
            if (last && pending != null) {
                params.messageObjectsListener = objects -> {
                    if (objects != null && !objects.isEmpty()) {
                        synchronized (pendingTexts) {
                            pending.localId = objects.get(0).getId();
                        }
                    }
                };
            }
            SendMessagesHelper.getInstance(account).sendMessage(params);
        }
    }

    private static void sendTextParts(int account, long peer, FormattedText text, MessageObject replyTo, boolean notify,
                                      int scheduleDate, int scheduleRepeatPeriod, MessageObject replyToTopMsg,
                                      long monoForumPeer, SendMessageChatArguments chatArguments,
                                      MessageSuggestionParams suggestionParams) {
        ArrayList<FormattedText> parts = splitText(text);
        for (int i = 0; i < parts.size(); i++) {
            FormattedText part = parts.get(i);
            SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(
                    part.text, peer, i == 0 ? replyTo : null, replyToTopMsg, null, true, part.entities, null, null,
                    notify, scheduleDate, scheduleRepeatPeriod, null, false);
            params.monoForumPeer = monoForumPeer;
            params.suggestionParams = suggestionParams;
            params.sendMessageChatArguments = chatArguments;
            SendMessagesHelper.getInstance(account).sendMessage(params);
        }
    }

    private static ArrayList<FormattedText> splitText(FormattedText source) {
        ArrayList<FormattedText> parts = new ArrayList<>();
        if (source == null || source.text.isEmpty()) {
            return parts;
        }
        String text = source.text;
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + TEXT_LIMIT, text.length());
            if (end < text.length()) {
                int min = Math.max(start, end - 300);
                for (int i = end - 1; i >= min; i--) {
                    if (Character.isWhitespace(text.charAt(i))) {
                        end = i;
                        break;
                    }
                }
                if (end > start && end < text.length() && Character.isLowSurrogate(text.charAt(end))) {
                    end--;
                }
            }
            int contentStart = start;
            int contentEnd = end;
            while (contentStart < contentEnd && Character.isWhitespace(text.charAt(contentStart))) {
                contentStart++;
            }
            while (contentEnd > contentStart && Character.isWhitespace(text.charAt(contentEnd - 1))) {
                contentEnd--;
            }
            FormattedText part = substring(source, contentStart, contentEnd);
            if (part.length() > 0) {
                parts.add(part);
            }
            start = end;
            while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
                start++;
            }
        }
        return parts;
    }

    private static void ensureObserver(int account) {
        synchronized (observers) {
            if (observers.containsKey(account)) {
                return;
            }
            NotificationCenter.NotificationCenterDelegate delegate = (id, account1, args) -> {
                if (id != NotificationCenter.messageReceivedByServer || args == null || args.length < 3) {
                    return;
                }
                if (!(args[0] instanceof Integer) || !(args[2] instanceof MessageObject)) {
                    return;
                }
                int oldId = (Integer) args[0];
                MessageObject serverObject = (MessageObject) args[2];
                PendingText matched = null;
                synchronized (pendingTexts) {
                    for (int i = 0; i < pendingTexts.size(); i++) {
                        PendingText pending = pendingTexts.get(i);
                        if (pending.account == account1 && pending.localId != 0 && pending.localId == oldId) {
                            pendingTexts.remove(i);
                            matched = pending;
                            break;
                        }
                    }
                }
                if (matched != null) {
                    final PendingText result = matched;
                    AndroidUtilities.runOnUIThread(() -> sendTextParts(result.account, result.peer, result.text, serverObject,
                            result.notify, result.scheduleDate, result.scheduleRepeatPeriod, result.replyToTopMsg,
                            result.monoForumPeer, result.chatArguments, result.suggestionParams));
                }
            };
            observers.put(account, delegate);
            NotificationCenter.getInstance(account).addObserver(delegate, NotificationCenter.messageReceivedByServer);
        }
    }

    private static ArrayList<MediaItem> collectMedia(int account, TL_iv.RichMessage richMessage) {
        ArrayList<MediaItem> items = new ArrayList<>();
        if (richMessage == null) {
            return items;
        }
        if (richMessage.photos != null) {
            for (int i = 0; i < richMessage.photos.size(); i++) {
                MediaItem item = MediaItem.ofPhoto(account, richMessage.photos.get(i));
                if (item != null) {
                    items.add(item);
                }
            }
        }
        if (richMessage.documents != null) {
            for (int i = 0; i < richMessage.documents.size(); i++) {
                MediaItem item = MediaItem.ofDocument(account, richMessage.documents.get(i));
                if (item != null) {
                    items.add(item);
                }
            }
        }
        return items;
    }

    private static FormattedText toFormattedText(TL_iv.RichMessage richMessage) {
        if (richMessage == null) {
            return new FormattedText("", new ArrayList<>());
        }
        return formatBlocks(richMessage.blocks);
    }

    private static FormattedText formatBlocks(List<TL_iv.PageBlock> blocks) {
        TextBuilder builder = new TextBuilder();
        if (blocks != null) {
            for (int i = 0; i < blocks.size(); i++) {
                appendBlock(builder, blocks.get(i));
            }
        }
        return builder.build();
    }

    private static FormattedText formatText(TL_iv.RichText text) {
        TextBuilder builder = new TextBuilder();
        builder.appendInline(text);
        return builder.build();
    }

    private static void appendBlock(TextBuilder out, TL_iv.PageBlock block) {
        if (block == null || block instanceof TL_iv.pageBlockUnsupported || block instanceof TL_iv.pageBlockAnchor) {
            return;
        }
        if (block instanceof TL_iv.pageBlockTitle) {
            appendHeading(out, 1, block.text);
        } else if (block instanceof TL_iv.pageBlockSubtitle) {
            appendHeading(out, 2, block.text);
        } else if (block instanceof TL_iv.pageBlockHeader) {
            appendHeading(out, 2, block.text);
        } else if (block instanceof TL_iv.pageBlockSubheader) {
            appendHeading(out, 3, block.text);
        } else if (block instanceof TL_iv.pageBlockHeading1) {
            appendHeading(out, 1, block.text);
        } else if (block instanceof TL_iv.pageBlockHeading2) {
            appendHeading(out, 2, block.text);
        } else if (block instanceof TL_iv.pageBlockHeading3) {
            appendHeading(out, 3, block.text);
        } else if (block instanceof TL_iv.pageBlockHeading4) {
            appendHeading(out, 4, block.text);
        } else if (block instanceof TL_iv.pageBlockHeading5) {
            appendHeading(out, 5, block.text);
        } else if (block instanceof TL_iv.pageBlockHeading6) {
            appendHeading(out, 6, block.text);
        } else if (block instanceof TL_iv.pageBlockKicker) {
            appendTextBlock(out, block.text);
        } else if (block instanceof TL_iv.pageBlockAuthorDate) {
            appendTextBlock(out, ((TL_iv.pageBlockAuthorDate) block).author);
        } else if (block instanceof TL_iv.pageBlockParagraph || block instanceof TL_iv.pageBlockFooter) {
            appendTextBlock(out, block.text);
        } else if (block instanceof TL_iv.pageBlockPreformatted) {
            appendPreformatted(out, (TL_iv.pageBlockPreformatted) block);
        } else if (block instanceof TL_iv.pageBlockDivider) {
            if (out.length() > 0) {
                out.append("———");
                out.blankLine();
            }
        } else if (block instanceof TL_iv.pageBlockList) {
            appendList(out, (TL_iv.pageBlockList) block);
        } else if (block instanceof TL_iv.pageBlockOrderedList) {
            appendOrderedList(out, (TL_iv.pageBlockOrderedList) block);
        } else if (block instanceof TL_iv.pageBlockBlockquoteBlocks) {
            TL_iv.pageBlockBlockquoteBlocks quote = (TL_iv.pageBlockBlockquoteBlocks) block;
            appendQuote(out, formatBlocks(quote.blocks), quote.caption, false);
        } else if (block instanceof TL_iv.pageBlockBlockquote) {
            TL_iv.pageBlockBlockquote quote = (TL_iv.pageBlockBlockquote) block;
            appendQuote(out, formatText(quote.text), quote.caption, quote.collapsed);
        } else if (block instanceof TL_iv.pageBlockPullquote) {
            TL_iv.pageBlockPullquote quote = (TL_iv.pageBlockPullquote) block;
            appendQuote(out, formatText(quote.text), quote.caption, false);
        } else if (block instanceof TL_iv.pageBlockTable) {
            appendTable(out, (TL_iv.pageBlockTable) block);
        } else if (block instanceof TL_iv.pageBlockDetails) {
            appendDetails(out, (TL_iv.pageBlockDetails) block);
        } else if (block instanceof TL_iv.pageBlockRelatedArticles) {
            appendRelatedArticles(out, (TL_iv.pageBlockRelatedArticles) block);
        } else if (block instanceof TL_iv.pageBlockCover) {
            appendBlock(out, ((TL_iv.pageBlockCover) block).cover);
        } else if (block instanceof TL_iv.pageBlockEmbedPost) {
            appendEmbedPost(out, (TL_iv.pageBlockEmbedPost) block);
        } else if (block instanceof TL_iv.pageBlockEmbed) {
            appendEmbed(out, (TL_iv.pageBlockEmbed) block);
        } else if (block instanceof TL_iv.pageBlockCollage) {
            appendMediaGroup(out, ((TL_iv.pageBlockCollage) block).items, block.caption);
        } else if (block instanceof TL_iv.pageBlockSlideshow) {
            appendMediaGroup(out, ((TL_iv.pageBlockSlideshow) block).items, block.caption);
        } else if (block instanceof TL_iv.pageBlockButtonRow) {
            appendButtons(out, (TL_iv.pageBlockButtonRow) block);
        } else if (block instanceof TL_iv.pageBlockMath) {
            String source = ((TL_iv.pageBlockMath) block).source;
            if (!TextUtils.isEmpty(source)) {
                int start = out.length();
                out.append(source);
                out.addEntity(new TLRPC.TL_messageEntityCode(), start, out.length());
                out.blankLine();
            }
        } else if (block instanceof TL_iv.pageBlockThinking) {
            appendQuote(out, formatText(block.text), null, false);
        } else if (block instanceof TL_iv.pageBlockChannel) {
            TLRPC.Chat channel = ((TL_iv.pageBlockChannel) block).channel;
            if (channel != null) {
                out.append(channel.title);
                out.blankLine();
            }
        } else {
            appendCaption(out, block.caption);
        }
    }

    private static void appendHeading(TextBuilder out, int level, TL_iv.RichText text) {
        if (isRichTextEmpty(text)) {
            return;
        }
        int start = out.length();
        out.appendInline(text);
        if (out.length() > start) {
            out.addEntity(new TLRPC.TL_messageEntityBold(), start, out.length());
            out.blankLine();
        }
    }

    private static void appendTextBlock(TextBuilder out, TL_iv.RichText text) {
        FormattedText value = formatText(text);
        if (value.length() == 0) {
            return;
        }
        out.appendFormatted(value);
        out.blankLine();
    }

    private static void appendPreformatted(TextBuilder out, TL_iv.pageBlockPreformatted block) {
        String code = plainText(block.text);
        if (TextUtils.isEmpty(code)) {
            return;
        }
        int start = out.length();
        out.append(code.replace("\r", ""));
        TLRPC.TL_messageEntityPre entity = new TLRPC.TL_messageEntityPre();
        entity.language = block.language == null ? "" : block.language.trim();
        out.addEntity(entity, start, out.length());
        out.blankLine();
    }

    private static void appendList(TextBuilder out, TL_iv.pageBlockList block) {
        if (block.items == null) {
            return;
        }
        for (int i = 0; i < block.items.size(); i++) {
            TL_iv.PageListItem item = block.items.get(i);
            if (item == null) {
                continue;
            }
            FormattedText text = item instanceof TL_iv.TL_pageListItemText
                    ? formatText(((TL_iv.TL_pageListItemText) item).text)
                    : item instanceof TL_iv.TL_pageListItemBlocks
                    ? formatBlocks(((TL_iv.TL_pageListItemBlocks) item).blocks)
                    : null;
            if (text == null || text.length() == 0) {
                continue;
            }
            String marker = item.checkbox ? (item.checked ? "[x] " : "[ ] ") : "• ";
            appendListItem(out, text, marker);
        }
        out.blankLine();
    }

    private static void appendOrderedList(TextBuilder out, TL_iv.pageBlockOrderedList block) {
        if (block.items == null) {
            return;
        }
        int number = block.start > 0 ? block.start : 1;
        for (int i = 0; i < block.items.size(); i++) {
            TL_iv.PageListOrderedItem item = block.items.get(i);
            if (item == null) {
                continue;
            }
            FormattedText text = item instanceof TL_iv.TL_pageListOrderedItemText
                    ? formatText(((TL_iv.TL_pageListOrderedItemText) item).text)
                    : item instanceof TL_iv.TL_pageListOrderedItemBlocks
                    ? formatBlocks(((TL_iv.TL_pageListOrderedItemBlocks) item).blocks)
                    : null;
            if (text == null || text.length() == 0) {
                continue;
            }
            String marker;
            if (item.checkbox) {
                marker = item.checked ? "[x] " : "[ ] ";
            } else {
                marker = (item.num != null && !item.num.isEmpty() ? item.num : Integer.toString(number)) + ". ";
            }
            appendListItem(out, text, marker);
            number++;
        }
        out.blankLine();
    }

    private static void appendListItem(TextBuilder out, FormattedText text, String marker) {
        StringBuilder padding = new StringBuilder(marker.length());
        for (int i = 0; i < marker.length(); i++) {
            padding.append(' ');
        }
        String continuation = padding.toString();
        int start = 0;
        boolean first = true;
        while (start <= text.length()) {
            int end = text.text.indexOf('\n', start);
            if (end < 0) {
                end = text.length();
            }
            FormattedText line = substring(text, start, end);
            if (line.length() > 0) {
                out.append(first ? marker : continuation);
                out.appendFormatted(line);
            }
            out.append('\n');
            first = false;
            if (end >= text.length()) {
                break;
            }
            start = end + 1;
        }
    }

    private static void appendQuote(TextBuilder out, FormattedText inner, TL_iv.RichText caption, boolean collapsed) {
        int start = out.length();
        out.appendFormatted(inner);
        FormattedText captionText = formatText(caption);
        if (captionText.length() > 0) {
            if (out.length() > start && out.charAt(out.length() - 1) != '\n') {
                out.append('\n');
            }
            out.append("— ");
            out.appendFormatted(captionText);
        }
        int end = out.length();
        while (end > start && out.charAt(end - 1) == '\n') {
            end--;
        }
        if (end > start) {
            TLRPC.TL_messageEntityBlockquote entity = new TLRPC.TL_messageEntityBlockquote();
            entity.collapsed = collapsed;
            out.addEntity(entity, start, end);
        }
        out.blankLine();
    }

    private static void appendTable(TextBuilder out, TL_iv.pageBlockTable block) {
        if (!isRichTextEmpty(block.title)) {
            int titleStart = out.length();
            out.appendInline(block.title);
            if (out.length() > titleStart) {
                out.addEntity(new TLRPC.TL_messageEntityBold(), titleStart, out.length());
                out.blankLine();
            }
        }
        if (block.rows == null || block.rows.isEmpty()) {
            appendCaption(out, block.caption);
            return;
        }
        int columns = 0;
        for (int i = 0; i < block.rows.size(); i++) {
            TL_iv.pageTableRow row = block.rows.get(i);
            if (row != null && row.cells != null) {
                columns = Math.max(columns, row.cells.size());
            }
        }
        if (columns == 0) {
            appendCaption(out, block.caption);
            return;
        }
        for (int i = 0; i < block.rows.size(); i++) {
            TL_iv.pageTableRow row = block.rows.get(i);
            out.append('|');
            for (int j = 0; j < columns; j++) {
                out.append(' ');
                if (row != null && row.cells != null && j < row.cells.size()) {
                    TL_iv.pageTableCell cell = row.cells.get(j);
                    FormattedText cellText = formatText(cell == null ? null : cell.text);
                    if (cellText.text.indexOf('\n') >= 0 || cellText.text.indexOf('\r') >= 0) {
                        cellText = new FormattedText(cellText.text.replace('\n', ' ').replace('\r', ' '), cellText.entities);
                    }
                    out.appendFormatted(cellText);
                }
                out.append(' ');
                out.append('|');
            }
            out.append('\n');
            if (i == 0) {
                out.append('|');
                for (int j = 0; j < columns; j++) {
                    out.append(" --- |");
                }
                out.append('\n');
            }
        }
        out.blankLine();
        appendCaption(out, block.caption);
    }

    private static void appendDetails(TextBuilder out, TL_iv.pageBlockDetails block) {
        if (!isRichTextEmpty(block.title)) {
            int start = out.length();
            out.appendInline(block.title);
            if (out.length() > start) {
                out.addEntity(new TLRPC.TL_messageEntityBold(), start, out.length());
                out.blankLine();
            }
        }
        FormattedText inner = formatBlocks(block.blocks);
        if (inner.length() > 0) {
            out.appendFormatted(inner);
            out.blankLine();
        }
    }

    private static void appendRelatedArticles(TextBuilder out, TL_iv.pageBlockRelatedArticles block) {
        if (!isRichTextEmpty(block.title)) {
            int start = out.length();
            out.appendInline(block.title);
            if (out.length() > start) {
                out.addEntity(new TLRPC.TL_messageEntityBold(), start, out.length());
                out.blankLine();
            }
        }
        if (block.articles == null) {
            return;
        }
        for (int i = 0; i < block.articles.size(); i++) {
            TL_iv.pageRelatedArticle article = block.articles.get(i);
            if (article == null) {
                continue;
            }
            String text = !TextUtils.isEmpty(article.title) ? article.title : article.url;
            if (TextUtils.isEmpty(text)) {
                continue;
            }
            out.append("• ");
            int start = out.length();
            out.append(text);
            if (!TextUtils.isEmpty(article.url)) {
                TLRPC.TL_messageEntityTextUrl entity = new TLRPC.TL_messageEntityTextUrl();
                entity.url = article.url;
                out.addEntity(entity, start, out.length());
            }
            out.append('\n');
        }
        out.blankLine();
    }

    private static void appendEmbedPost(TextBuilder out, TL_iv.pageBlockEmbedPost block) {
        if (!TextUtils.isEmpty(block.author)) {
            int start = out.length();
            out.append(block.author);
            out.addEntity(new TLRPC.TL_messageEntityBold(), start, out.length());
            out.blankLine();
        }
        if (!TextUtils.isEmpty(block.url)) {
            int start = out.length();
            out.append(block.url);
            out.addEntity(new TLRPC.TL_messageEntityUrl(), start, out.length());
            out.blankLine();
        }
        FormattedText inner = formatBlocks(block.blocks);
        if (inner.length() > 0) {
            out.appendFormatted(inner);
            out.blankLine();
        }
        appendCaption(out, block.caption);
    }

    private static void appendEmbed(TextBuilder out, TL_iv.pageBlockEmbed block) {
        if (!TextUtils.isEmpty(block.url)) {
            int start = out.length();
            out.append(block.url);
            out.addEntity(new TLRPC.TL_messageEntityUrl(), start, out.length());
            out.blankLine();
        }
        appendCaption(out, block.caption);
    }

    private static void appendMediaGroup(TextBuilder out, List<TL_iv.PageBlock> items, TL_iv.PageCaption caption) {
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                appendBlock(out, items.get(i));
            }
        }
        appendCaption(out, caption);
    }

    private static void appendButtons(TextBuilder out, TL_iv.pageBlockButtonRow block) {
        if (block.buttons == null || block.buttons.isEmpty()) {
            return;
        }
        boolean first = true;
        for (int i = 0; i < block.buttons.size(); i++) {
            TL_keyboard.PageButton button = block.buttons.get(i);
            if (button == null || isRichTextEmpty(button.text)) {
                continue;
            }
            if (!first) {
                out.append(" · ");
            }
            out.appendInline(button.text);
            first = false;
        }
        if (!first) {
            out.blankLine();
        }
    }

    private static void appendCaption(TextBuilder out, TL_iv.PageCaption caption) {
        if (caption == null) {
            return;
        }
        FormattedText text = formatText(caption.text);
        if (text.length() > 0) {
            out.appendFormatted(text);
            out.blankLine();
        }
        FormattedText credit = formatText(caption.credit);
        if (credit.length() > 0) {
            int start = out.length();
            out.appendFormatted(credit);
            out.addEntity(new TLRPC.TL_messageEntityItalic(), start, out.length());
            out.blankLine();
        }
    }

    private static boolean isRichTextEmpty(TL_iv.RichText text) {
        if (text == null || text instanceof TL_iv.textEmpty) {
            return true;
        }
        if (text instanceof TL_iv.textPlain) {
            return TextUtils.isEmpty(((TL_iv.textPlain) text).text);
        }
        return false;
    }

    private static String plainText(TL_iv.RichText text) {
        if (text == null || text instanceof TL_iv.textEmpty) {
            return "";
        }
        if (text instanceof TL_iv.textPlain) {
            String value = ((TL_iv.textPlain) text).text;
            return value == null ? "" : value;
        }
        if (text instanceof TL_iv.textConcat) {
            StringBuilder builder = new StringBuilder();
            if (text.texts != null) {
                for (int i = 0; i < text.texts.size(); i++) {
                    builder.append(plainText(text.texts.get(i)));
                }
            }
            return builder.toString();
        }
        if (text instanceof TL_iv.textCustomEmoji) {
            String alt = ((TL_iv.textCustomEmoji) text).alt;
            return alt == null ? "" : alt;
        }
        if (text instanceof TL_iv.textMath) {
            String source = ((TL_iv.textMath) text).source;
            return source == null ? "" : source;
        }
        if (text instanceof TL_iv.textDiff) {
            TL_iv.textDiff diff = (TL_iv.textDiff) text;
            return isRichTextEmpty(diff.text) ? plainText(diff.old_text) : plainText(diff.text);
        }
        if (text.text != null) {
            return plainText(text.text);
        }
        if (text.texts != null && !text.texts.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < text.texts.size(); i++) {
                builder.append(plainText(text.texts.get(i)));
            }
            return builder.toString();
        }
        return "";
    }

    private static FormattedText substring(FormattedText source, int start, int end) {
        if (source == null || start >= end || start < 0 || start >= source.text.length()) {
            return new FormattedText("", new ArrayList<>());
        }
        end = Math.min(end, source.text.length());
        return new FormattedText(source.text.substring(start, end), clipEntities(source.entities, start, end));
    }

    private static ArrayList<TLRPC.MessageEntity> clipEntities(List<TLRPC.MessageEntity> source, int start, int end) {
        ArrayList<TLRPC.MessageEntity> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (int i = 0; i < source.size(); i++) {
            TLRPC.MessageEntity entity = source.get(i);
            if (entity == null) {
                continue;
            }
            int entityStart = Math.max(entity.offset, start);
            int entityEnd = Math.min(entity.offset + entity.length, end);
            if (entityEnd > entityStart) {
                TLRPC.MessageEntity copy = copyEntity(entity);
                if (copy != null) {
                    copy.offset = entityStart - start;
                    copy.length = entityEnd - entityStart;
                    result.add(copy);
                }
            }
        }
        return result;
    }

    private static TLRPC.MessageEntity copyEntity(TLRPC.MessageEntity entity) {
        return TLObject.deepCopy(entity, TLRPC.MessageEntity::TLdeserialize);
    }

    private static class FormattedText {
        final String text;
        final ArrayList<TLRPC.MessageEntity> entities;

        FormattedText(String text, ArrayList<TLRPC.MessageEntity> entities) {
            this.text = text;
            this.entities = entities;
        }

        int length() {
            return text.length();
        }
    }

    private static class TextBuilder {
        final StringBuilder text = new StringBuilder();
        final ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>();

        int length() {
            return text.length();
        }

        char charAt(int index) {
            return text.charAt(index);
        }

        void append(char c) {
            text.append(c);
        }

        void append(String value) {
            if (value != null) {
                text.append(value);
            }
        }

        void appendFormatted(FormattedText value) {
            if (value == null || value.text.isEmpty()) {
                return;
            }
            int base = text.length();
            text.append(value.text);
            for (int i = 0; i < value.entities.size(); i++) {
                TLRPC.MessageEntity copy = copyEntity(value.entities.get(i));
                if (copy != null) {
                    copy.offset += base;
                    entities.add(copy);
                }
            }
        }

        void addEntity(TLRPC.MessageEntity entity, int start, int end) {
            if (entity == null || end <= start) {
                return;
            }
            entity.offset = start;
            entity.length = end - start;
            entities.add(entity);
        }

        void blankLine() {
            if (text.length() == 0) {
                return;
            }
            if (text.charAt(text.length() - 1) != '\n') {
                text.append('\n');
            }
            if (text.length() < 2 || text.charAt(text.length() - 2) != '\n') {
                text.append('\n');
            }
        }

        void appendInline(TL_iv.RichText richText) {
            if (richText == null || richText instanceof TL_iv.textEmpty || richText instanceof TL_iv.textImage) {
                return;
            }
            if (richText instanceof TL_iv.textPlain) {
                String value = ((TL_iv.textPlain) richText).text;
                if (value != null) {
                    text.append(value);
                }
                return;
            }
            if (richText instanceof TL_iv.textConcat) {
                if (richText.texts != null) {
                    for (int i = 0; i < richText.texts.size(); i++) {
                        appendInline(richText.texts.get(i));
                    }
                }
                return;
            }
            if (richText instanceof TL_iv.textMath) {
                String source = ((TL_iv.textMath) richText).source;
                if (source != null) {
                    text.append(source);
                }
                return;
            }
            if (richText instanceof TL_iv.textCustomEmoji) {
                TL_iv.textCustomEmoji emoji = (TL_iv.textCustomEmoji) richText;
                String alt = emoji.alt;
                if (!TextUtils.isEmpty(alt)) {
                    int start = text.length();
                    text.append(alt);
                    TLRPC.TL_messageEntityCustomEmoji entity = new TLRPC.TL_messageEntityCustomEmoji();
                    entity.document_id = emoji.document_id;
                    addEntity(entity, start, text.length());
                }
                return;
            }
            if (richText instanceof TL_iv.textDiff) {
                TL_iv.textDiff diff = (TL_iv.textDiff) richText;
                TL_iv.RichText value = isRichTextEmpty(diff.text) ? diff.old_text : diff.text;
                int start = text.length();
                appendInline(value);
                if (text.length() > start) {
                    if (!isRichTextEmpty(diff.text) && !isRichTextEmpty(diff.old_text)) {
                        TLRPC.TL_messageEntityDiffReplace entity = new TLRPC.TL_messageEntityDiffReplace();
                        entity.old_text = plainText(diff.old_text);
                        addEntity(entity, start, text.length());
                    } else if (!isRichTextEmpty(diff.text)) {
                        addEntity(new TLRPC.TL_messageEntityDiffInsert(), start, text.length());
                    } else {
                        addEntity(new TLRPC.TL_messageEntityDiffDelete(), start, text.length());
                    }
                }
                return;
            }

            int start = text.length();
            appendInline(richText.text);
            if (text.length() == start) {
                if (richText.texts != null) {
                    for (int i = 0; i < richText.texts.size(); i++) {
                        appendInline(richText.texts.get(i));
                    }
                }
                if (text.length() == start) {
                    return;
                }
            }

            TLRPC.MessageEntity entity = null;
            if (richText instanceof TL_iv.textBold) {
                entity = new TLRPC.TL_messageEntityBold();
            } else if (richText instanceof TL_iv.textItalic) {
                entity = new TLRPC.TL_messageEntityItalic();
            } else if (richText instanceof TL_iv.textUnderline) {
                entity = new TLRPC.TL_messageEntityUnderline();
            } else if (richText instanceof TL_iv.textStrike) {
                entity = new TLRPC.TL_messageEntityStrike();
            } else if (richText instanceof TL_iv.textFixed) {
                entity = new TLRPC.TL_messageEntityCode();
            } else if (richText instanceof TL_iv.textSpoiler) {
                entity = new TLRPC.TL_messageEntitySpoiler();
            } else if (richText instanceof TL_iv.textUrl) {
                String url = ((TL_iv.textUrl) richText).url;
                if (!TextUtils.isEmpty(url)) {
                    TLRPC.TL_messageEntityTextUrl textUrl = new TLRPC.TL_messageEntityTextUrl();
                    textUrl.url = url;
                    entity = textUrl;
                }
            } else if (richText instanceof TL_iv.textEmail) {
                entity = new TLRPC.TL_messageEntityEmail();
            } else if (richText instanceof TL_iv.textPhone) {
                entity = new TLRPC.TL_messageEntityPhone();
            } else if (richText instanceof TL_iv.textMention) {
                entity = new TLRPC.TL_messageEntityMention();
            } else if (richText instanceof TL_iv.textHashtag) {
                entity = new TLRPC.TL_messageEntityHashtag();
            } else if (richText instanceof TL_iv.textBotCommand) {
                entity = new TLRPC.TL_messageEntityBotCommand();
            } else if (richText instanceof TL_iv.textCashtag) {
                entity = new TLRPC.TL_messageEntityCashtag();
            } else if (richText instanceof TL_iv.textAutoUrl) {
                entity = new TLRPC.TL_messageEntityUrl();
            } else if (richText instanceof TL_iv.textAutoEmail) {
                entity = new TLRPC.TL_messageEntityEmail();
            } else if (richText instanceof TL_iv.textAutoPhone) {
                entity = new TLRPC.TL_messageEntityPhone();
            } else if (richText instanceof TL_iv.textBankCard) {
                entity = new TLRPC.TL_messageEntityBankCard();
            } else if (richText instanceof TL_iv.textTonAddress) {
                entity = new TLRPC.TL_messageEntityTonAddress();
            } else if (richText instanceof TL_iv.textMentionName) {
                TLRPC.TL_messageEntityMentionName mentionName = new TLRPC.TL_messageEntityMentionName();
                mentionName.user_id = ((TL_iv.textMentionName) richText).user_id;
                entity = mentionName;
            } else if (richText instanceof TL_iv.textDate) {
                TL_iv.textDate date = (TL_iv.textDate) richText;
                TLRPC.TL_messageEntityFormattedDate formattedDate = new TLRPC.TL_messageEntityFormattedDate();
                formattedDate.date = date.date;
                formattedDate.relative = date.relative;
                formattedDate.short_time = date.short_time;
                formattedDate.long_time = date.long_time;
                formattedDate.short_date = date.short_date;
                formattedDate.long_date = date.long_date;
                formattedDate.day_of_week = date.day_of_week;
                entity = formattedDate;
            }
            addEntity(entity, start, text.length());
        }

        FormattedText build() {
            int start = 0;
            while (start < text.length() && text.charAt(start) == '\n') {
                start++;
            }
            int end = text.length();
            while (end > start && text.charAt(end - 1) == '\n') {
                end--;
            }
            if (start >= end) {
                return new FormattedText("", new ArrayList<>());
            }
            String value = text.substring(start, end);
            if (value.trim().isEmpty()) {
                return new FormattedText("", new ArrayList<>());
            }
            return new FormattedText(value, clipEntities(entities, start, end));
        }
    }

    private static class PendingText {
        final int account;
        final long peer;
        final FormattedText text;
        final boolean notify;
        final int scheduleDate;
        final int scheduleRepeatPeriod;
        final MessageObject replyToTopMsg;
        final long monoForumPeer;
        final SendMessageChatArguments chatArguments;
        final MessageSuggestionParams suggestionParams;
        int localId;

        PendingText(int account, long peer, FormattedText text, boolean notify, int scheduleDate, int scheduleRepeatPeriod,
                    MessageObject replyToTopMsg, long monoForumPeer,
                    SendMessageChatArguments chatArguments, MessageSuggestionParams suggestionParams) {
            this.account = account;
            this.peer = peer;
            this.text = text;
            this.notify = notify;
            this.scheduleDate = scheduleDate;
            this.scheduleRepeatPeriod = scheduleRepeatPeriod;
            this.replyToTopMsg = replyToTopMsg;
            this.monoForumPeer = monoForumPeer;
            this.chatArguments = chatArguments;
            this.suggestionParams = suggestionParams;
        }
    }

    private static class MediaItem {
        TLRPC.TL_photo photo;
        TLRPC.TL_document document;
        String path;

        static MediaItem ofPhoto(int account, TLRPC.Photo rawPhoto) {
            if (!(rawPhoto instanceof TLRPC.TL_photo)) {
                return null;
            }
            TLRPC.Photo copied = TLObject.deepCopy((TLRPC.TL_photo) rawPhoto, TLRPC.Photo::TLdeserialize);
            if (!(copied instanceof TLRPC.TL_photo)) {
                return null;
            }
            TLRPC.TL_photo photo = (TLRPC.TL_photo) copied;
            photo.file_reference = rawPhoto.file_reference != null ? rawPhoto.file_reference.clone() : new byte[0];
            MediaItem item = new MediaItem();
            item.photo = photo;
            if (photo.sizes != null) {
                for (int i = photo.sizes.size() - 1; i >= 0; i--) {
                    TLRPC.PhotoSize size = photo.sizes.get(i);
                    if (size != null && size.location != null) {
                        item.path = FileLoader.getInstance(account).getPathToAttach(size.location, true).toString();
                        break;
                    }
                }
            }
            return item;
        }

        static MediaItem ofDocument(int account, TLRPC.Document rawDocument) {
            if (!(rawDocument instanceof TLRPC.TL_document)) {
                return null;
            }
            TLRPC.Document copied = TLObject.deepCopy((TLRPC.TL_document) rawDocument, TLRPC.Document::TLdeserialize);
            if (!(copied instanceof TLRPC.TL_document)) {
                return null;
            }
            TLRPC.TL_document document = (TLRPC.TL_document) copied;
            document.file_reference = rawDocument.file_reference != null ? rawDocument.file_reference.clone() : new byte[0];
            MediaItem item = new MediaItem();
            item.document = document;
            item.path = FileLoader.getInstance(account).getPathToAttach(document, true).toString();
            return item;
        }
    }
}
