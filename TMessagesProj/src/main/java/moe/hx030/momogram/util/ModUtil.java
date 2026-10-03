package moe.hx030.momogram.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;

import org.apache.commons.lang3.function.TriConsumer;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MemberRequestsController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_communities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import moe.hx030.momogram.MomoConfig;
import moe.hx030.momogram.cc.CCConverter;
import moe.hx030.momogram.cc.CCTarget;

public class ModUtil {

    private static ArrayDeque<Long> bannedUserIds;
    private static final AtomicInteger banned = new AtomicInteger(0), dismissed = new AtomicInteger(0);

    public static SharedPreferences modCache = ApplicationLoader.applicationContext
            .getSharedPreferences("momo_mod", Context.MODE_PRIVATE);

    public static final int SCAN_SKIP = -1;
    public static final int SCAN_PASS = 0;
    public static final int SCAN_DISMISS = 1;
    public static final int SCAN_BAN = 2;

    public static void filterJoinRequestsJob(boolean scheduled) {
        Log.d("030-mod", "filterJoinRequestsJob invoked");
        Utilities.globalQueue.postRunnable(() -> {
            if (!MomoConfig.autoDismissJoinReq.Bool()) return;
            Log.d("030-mod", "filterJoinRequestsJob start");
            final HashMap<Integer, HashSet<Long>> grp = new HashMap<>();
            modCache.getAll().forEach((k, v) -> {
                if (!(Boolean) v) return;
                String[] s = k.split("_");
                try {
                    int acc = Integer.parseInt(s[0]);
                    long chatId = Long.parseLong(s[1]);
                    grp.compute(acc, (_acc, set) -> {
                        if (set == null) set = new HashSet<>();
                        set.add(chatId);
                        return set;
                    });
                } catch (Exception e) {
                    Log.e("030-mod", "", e);
                }
            });
            grp.forEach((currentAccount, chatIds) -> {
                MemberRequestsController controller = MemberRequestsController.getInstance(currentAccount);
                LongSparseArray<TLRPC.User> users = new LongSparseArray<>();
                for (long chatId : chatIds) {
                    getAllImporters(currentAccount, controller, chatId, users, null);
                }
            });
        });
        if (!scheduled) return;
        AndroidUtilities.runOnUIThread(() -> ModUtil.filterJoinRequestsJob(true), 30*60*1000);
    }

    public static int getAllImporters(int currentAccount, MemberRequestsController controller, long chatId, LongSparseArray<TLRPC.User> users, TLRPC.TL_chatInviteImporter last) {
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(chatId);
        String title = (chat == null ? "''" : chat.title);
        Log.d("030-mod", String.format(Locale.US, "filterJoinRequestsJob: getAllImporters: acc=%d chat=%s(%d)",
                currentAccount, title, chatId));
        AtomicInteger count = new AtomicInteger(0);
        controller.getImporters(chatId, null, last, users, (response, error) -> {
            if (error != null) {
                if (error.code == 400 && "CHAT_ID_INVALID".equals(error.text)) {
                    Log.w("030-mod", "got CHAT_ID_INVALID, wait 10s and retry");
                    AndroidUtilities.runOnUIThread(() -> getAllImporters(currentAccount, controller, chatId, users, last), 10000);
                } else {
                    Log.e("030-mod", String.format(Locale.US, "error from filterJoinRequestsJob: %d - %s, disabling job for %d", error.code, error.text, chatId));
                    modCache.edit().putBoolean(String.format(Locale.US, "%d_%d", currentAccount, chatId), false).apply();
                }
                return;
            }
            TLRPC.TL_messages_chatInviteImporters importers = (TLRPC.TL_messages_chatInviteImporters) response;
            int origCount = importers.count;

            if (importers.count == 0 || importers.importers.isEmpty())
                return;

            filterJoinRequests(currentAccount, chatId, importers);
            int newCount = importers.count;
            TLRPC.TL_chatInviteImporter newLast = (newCount == 0) ? null
                    : Collections.min(importers.importers, Comparator.comparingInt(x -> x.date));
            count.addAndGet(newCount - origCount);
            if (origCount >= MemberRequestsController.LIMIT) {
                Log.d("030-mod", "fetch next for " + title);
                count.addAndGet(getAllImporters(currentAccount, controller, chatId, users, newLast));
            }
        }, false);
        return count.get();
    }

    public static TLRPC.TL_messages_chatInviteImporters filterJoinRequests(int currentAccount, long chatId, TLRPC.TL_messages_chatInviteImporters importers) {
        if (importers == null || !MomoConfig.autoDismissJoinReq.Bool()) {
            Log.d("030-filterJoinReq", String.format("importers=%s autoDismiss=%s", importers != null , MomoConfig.autoDismissJoinReq.Bool()));
            return importers;
        }
        if (bannedUserIds == null) bannedUserIds = new ArrayDeque<>(60);
        final boolean bio = MomoConfig.autoDismissJoinReqBio.Bool();
        final boolean dummy = MomoConfig.autoDismissDummy.Bool();
        final boolean regex = MomoConfig.autoDismissRegexPattern != null;
        final boolean useOpenCC = MomoConfig.autoDismissNameUseOpenCC.Bool();
        int oldSize = importers.importers.size();
        if (oldSize > 0) {
            modCache.edit().putBoolean(String.format(Locale.US, "%d_%d", currentAccount, chatId), true).apply();
            Log.d("030-mod", String.format(Locale.US, "cached chatId for filterJoinRequestsJob: %d", chatId));
        }
        Log.d("030-filterJoinReq", String.format("b4 | count=%d size=%d", importers.count, importers.importers.size()));

        Map<Long, TLRPC.User> currentUsers = new HashMap<>(importers.users.size());
        for (TLRPC.User u : importers.users) {
            currentUsers.put(u.id, u);
        }

        final ArrayList<TLRPC.TL_chatInviteImporter> finalImporters = new ArrayList<>();
        for (TLRPC.TL_chatInviteImporter i : importers.importers) {
            TLRPC.User u = currentUsers.get(i.user_id);
            switch (scanUser(currentAccount, chatId, u, i.about, dummy, regex, bio, useOpenCC)) {
                case SCAN_PASS:
                    finalImporters.add(i);
                    break;
                case SCAN_DISMISS:
                    dismissJoinRequest(currentAccount, chatId, i, u);
                    dismissed.addAndGet(1);
                    break;
                case SCAN_BAN:
                    dismissJoinRequest(currentAccount, chatId, i, u);
                    MessagesController.getInstance(currentAccount).banUserFromChat(chatId, u, (response, error) -> {
                        if (error != null) {
                            Log.e("030-filterJoinReq", String.format("ban err %d: %s", error.code, error.text));
                        } else if (u != null) {
                            Log.d("030-filterJoinReq", String.format("banned %d %s", u.id, u.first_name));
                        }
                    });
                    banned.addAndGet(1);
                    break;
            }
        }
        importers.importers = finalImporters;
        importers.count -= (oldSize - finalImporters.size());
        Log.d("030-filterJoinReq", String.format("after | count=%d size=%d", importers.count, importers.importers.size()));
        if (banned.get() > 0 || dismissed.get() > 0) {
            scheduleShowStats();
        }
        return importers;
    }

    public static int scanUser(int currentAccount, long chatId, TLRPC.User u, Consumer<Integer> callback) {
        if (u == null) return SCAN_PASS;
        final boolean bio = MomoConfig.autoDismissJoinReqBio.Bool();
        final boolean dummy = MomoConfig.autoDismissDummy.Bool();
        final boolean regex = MomoConfig.autoDismissRegexPattern != null;
        final boolean useOpenCC = MomoConfig.autoDismissNameUseOpenCC.Bool();
        TLRPC.UserFull uf = MessagesController.getInstance(currentAccount).getUserFull(u.id);
        if (uf == null) {
            MessagesController.getInstance(currentAccount).loadFullUser(u, 0, true, (full) -> {
                int res = scanUser(currentAccount, chatId, u, full.about, dummy, regex, bio, useOpenCC);
                callback.accept(res);
            });
            return SCAN_SKIP;
        }
        String bioStr = uf.about;
        int r = scanUser(currentAccount, chatId, u, bioStr, dummy, regex, bio, useOpenCC);
        callback.accept(r);
        return r;
    }

    public static int scanUser(int currentAccount, long chatId, TLRPC.User u, String bioStr, boolean dummy, boolean regex, boolean bio, boolean useOpenCC) {
        if (u == null) return SCAN_PASS;
        if (dummy && TextUtils.isEmpty(u.username) && !ImageLocation.isUserHasPhoto(u)) {
            return SCAN_DISMISS;
        } else if (u.deleted || (regex &&
                FilterUtils.checkName(MomoConfig.autoDismissRegexPattern, u.first_name, u.last_name, useOpenCC)) ||
                (bio && FilterUtils.checkString(MomoConfig.autoDismissRegexPattern, bioStr, useOpenCC))) {
            return SCAN_BAN;
        } else {
            return SCAN_PASS;
        }
    }

    private static void dismissJoinRequest(int currentAccount, long chatId, TLRPC.TL_chatInviteImporter i, TLRPC.User u) {
        TLRPC.TL_messages_hideChatJoinRequest req = new TLRPC.TL_messages_hideChatJoinRequest();
        req.approved = false;
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(-chatId);
        req.user_id = MessagesController.getInstance(currentAccount).getInputUser(u);
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error != null) {
                Log.e("030-filterJoinReq", String.format("dismiss err %d: %s", error.code, error.text));
            }
        });
        Log.d("030-filterJoinReq", String.format("send dismiss req for %s %d (DA=%s)", u.first_name, i.user_id, u.deleted));
    }

    public static void dismissAllJoinRequests(int currentAccount, long chatId) {
        TLRPC.TL_messages_hideAllChatJoinRequests req = new TLRPC.TL_messages_hideAllChatJoinRequests();
        req.approved = false;
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(-chatId);
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error != null) {
                Log.e("030-filterJoinReq", String.format("dismiss err %d: %s", error.code, error.text));
            }
        });
        Log.d("030-filterJoinReq", String.format("send all dismiss reqs for %d", chatId));
    }

    public static void banAllJoinRequests(int currentAccount, long chatId, Runnable onDone) {
        AtomicInteger last = new AtomicInteger(0);
        MemberRequestsController.getInstance(currentAccount)
                .getImporters(chatId, null, null, null, (obj, err) -> {
                    TLRPC.TL_messages_chatInviteImporters importers = ((TLRPC.TL_messages_chatInviteImporters) obj);
                    Map<Long, TLRPC.User> currentUsers = new HashMap<>(importers.users.size());
                    for (TLRPC.User u : importers.users) {
                        currentUsers.put(u.id, u);
                    }

                    for (TLRPC.TL_chatInviteImporter i : importers.importers) {
                        TLRPC.User u = currentUsers.get(i.user_id);
                        if (u == null) continue;
                        if (bannedUserIds.contains(u.id)) continue;
                        bannedUserIds.add(u.id);

                        dismissJoinRequest(currentAccount, chatId, i, u);
                        MessagesController.getInstance(currentAccount).banUserFromChat(chatId, u, (response, error) -> {
                            if (error != null) {
                                Log.e("030-filterJoinReq", String.format("ban err %d: %s", error.code, error.text));
                            } else {
                                Log.d("030-filterJoinReq", String.format("banned %d %s", u.id, u.first_name));
                            }
                        });
                        last.addAndGet(1);
                    }

                    if (last.get() > 0) banAllJoinRequests(currentAccount, chatId, onDone);
                    else if (onDone != null) onDone.run();
                });
    }

    public static boolean maybeFilterChatSuggestion(int currentAccount, List<TL_communities.CommunityPeerRequest> reqs, BiConsumer<Long, Boolean> callback) {
        final int mode = MomoConfig.autoDismissSuggestedChats.Int();
        if (mode == MomoConfig.AUTO_DISMISS_DISABLED) return false;
        else if (mode == MomoConfig.AUTO_DISMISS_ALL) return true;
        final MessagesController messagesController = MessagesController.getInstance(currentAccount);

        final boolean bio = MomoConfig.autoDismissJoinReqBio.Bool();
        final boolean dummy = MomoConfig.autoDismissDummy.Bool();
        final boolean regex = MomoConfig.autoDismissRegexPattern != null;
        final boolean useOpenCC = MomoConfig.autoDismissNameUseOpenCC.Bool();

        BiConsumer<TLRPC.User, TLRPC.ChatFull> handler = (user, chat) -> {

        };

        reqs.stream().forEach(req -> {
            Log.d("030-filter", String.format("req by %d | chat id %d | channel id %d | user id %d", req.requested_by, req.peer.chat_id, req.peer.channel_id, req.peer.user_id));
            long reqChatId = -req.peer.chat_id;
            if (reqChatId == 0) reqChatId = -req.peer.channel_id;
            if (reqChatId == 0) reqChatId = req.peer.user_id;
            long chatId = Math.abs(reqChatId);


            TLRPC.User user = messagesController.getUser(req.requested_by);
            TLRPC.Chat chat = messagesController.getChat(chatId);
            TLRPC.ChatFull chatFull = messagesController.getChatFull(chatId);

            if (user == null && chat == null) {
                Log.w("030-filter", String.format("null user(%d) & chat(%d), skipping", req.requested_by, chatId));
                return;
            }
            if (user != null) {
                if (dummy && TextUtils.isEmpty(user.username) && !ImageLocation.isUserHasPhoto(user)) {
                    Log.d("030-filter", String.format("rejected %d by no username + no pfp", chatId));
                    callback.accept(reqChatId, false);
                    return;
                } else if (user.deleted || (regex &&
                        FilterUtils.checkName(MomoConfig.autoDismissRegexPattern, user.first_name, user.last_name, useOpenCC))) {
                    Log.d("030-filter", String.format("rejected %d by name pattern(%s %s)", chatId, user.first_name, user.last_name));
                    callback.accept(reqChatId, false);
                    return;
                } else if (bio) {
                    TLRPC.UserFull userFull = messagesController.getUserFull(req.requested_by);
                    if (userFull == null) {
                        long finalReqChatId = reqChatId;
                        messagesController.loadFullUser(user, 0, true, u -> {
                            if (FilterUtils.checkString(MomoConfig.autoDismissRegexPattern, u.about, useOpenCC)) {
                                Log.d("030-filter", String.format("rejected %d by bio(%s)", chatId, u.about));
                                callback.accept(finalReqChatId, false);
                            }
                        });
                    } else if (FilterUtils.checkString(MomoConfig.autoDismissRegexPattern, userFull.about, useOpenCC)) {
                        callback.accept(reqChatId, false);
                        return;
                    }
                }
            }
            Log.d("030-filter", String.format("chat: %d | name: %s | bio: %s",
                    chatId, (chat != null ? chat.title : "N/A (chat == null)"),
                    (chatFull != null ? chatFull.about : "N/A (chatFull == null)")));
            if ((chat != null && FilterUtils.checkString(MomoConfig.autoDismissRegexPattern, chat.title, useOpenCC)) ||
                    (chatFull != null && FilterUtils.checkString(MomoConfig.autoDismissRegexPattern, chatFull.about, useOpenCC))) {
                callback.accept(reqChatId, false);
            }
        });

        return false;
    }

    private static final Runnable showStats = () -> {
        int ban = banned.get(), dismiss = dismissed.get();
        banned.set(0);
        dismissed.set(0);
        BaseFragment frag = LaunchActivity.getLastFragment();
        if (frag == null) return;
        String msg;
        if (ban > 0 && dismiss > 0) {
            msg = LocaleController.formatString(R.string.AutoReqStats, ban, dismiss);
        } else if (ban > 0) {
            msg = LocaleController.formatString(R.string.AutoReqStatsBanned, ban);
        } else {
            msg = LocaleController.formatString(R.string.AutoReqStatsDismissed, dismiss);
        }
        BulletinFactory.of(frag).createSimpleBulletin(
                frag.getContext().getResources().getDrawable(R.drawable.profile_info), msg)
                .show(true);
    };
    private static void scheduleShowStats() {
        ApplicationLoader.applicationHandler.removeCallbacks(showStats);
        ApplicationLoader.applicationHandler.postDelayed(showStats, 1000);
    }

    public static void bamHammer(int currentAccount, TLObject target, TLRPC.Chat currentChat, MessageObject selectedObject) {
        // ban
        if (target == null) {
            Log.e("030-mod", "no target");
            return;
        }
        MessagesController.getInstance(currentAccount).banUserFromChat(currentChat.id, target, (r, e) -> {
            if (e == null) return;
            Log.e("030-bam", String.format("attempt to ban %s from dialog %d failed, %d %s", target, currentChat.id, e.code, e.text));
        });

        // report
        if (selectedObject != null) {
            TLRPC.TL_channels_reportSpam req = new TLRPC.TL_channels_reportSpam();
            req.channel = MessagesController.getInputChannel(currentChat);
            if (target instanceof TLRPC.User) {
                req.participant = MessagesController.getInputPeer((TLRPC.User) target);
            } else if (target instanceof TLRPC.Chat) {
                req.participant = MessagesController.getInputPeer((TLRPC.Chat) target);
            }
            ArrayList<Integer> msgId = new ArrayList<>();
            msgId.add(selectedObject.getId());
            req.id = msgId;
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (r, e) -> {
                if (e == null) return;
                Log.e("030-bam", String.format("attempt to report spam for %s from dialog %d failed, %d %s", target, currentChat.id, e.code, e.text));
            });
        }

        // delete all msgs + reactions
        if (target instanceof TLRPC.User) {
            MessagesController.getInstance(currentAccount)
                    .deleteUserChannelHistory(currentChat, (TLRPC.User) target, null, 0);
        } else if (target instanceof TLRPC.Chat) {
            MessagesController.getInstance(currentAccount)
                    .deleteUserChannelHistory(currentChat, null, (TLRPC.Chat) target, 0);
        }
        if (target instanceof TLRPC.User) {
            MessagesController.getInstance(currentAccount)
                    .deleteUserChannelAllReactions(currentChat, (TLRPC.User) target, null);
        } else if (target instanceof TLRPC.Chat) {
            MessagesController.getInstance(currentAccount)
                    .deleteUserChannelAllReactions(currentChat, null, (TLRPC.Chat) target);
        }
    }
}
