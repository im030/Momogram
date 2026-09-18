package moe.hx030.momogram.util;

import android.content.DialogInterface;
import android.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.SessionsActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import moe.hx030.momogram.MomoConfig;
import moe.hx030.momogram.utils.StrUtil;

public class SessionsUtil {

    private static final String TAG = "SessionsUtil";
    private static final long MIN_INTERVAL = 1000 * 60 * 10;
    public static final Map<String, Set<Integer>> maliciousClients = Map.of(
            "cherrygram", Set.of(R.string.MaliciousClientReasonClosedSource, R.string.MaliciousClientReasonDataCollection),
            "nekogram", Set.of(R.string.MaliciousClientReasonClosedSource, R.string.MaliciousClientReasonDataCollection),
            "momogram", Set.of(R.string.MaliciousClientReasonTest) // test
    );

    private static CountDownLatch checking = new CountDownLatch(0);
    private static final ConcurrentHashMap<Integer, String> knownClientNames = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, ArrayList<TLRPC.TL_authorization>> sessionMap = new ConcurrentHashMap<>();
    private static ArrayList<Integer> warnedClients = new ArrayList<>();
    private static boolean shownWarning = false;

    public static void checkSessions(LaunchActivity context, int currentAccount) {
        SessionsUtil.checkSessions(warnings -> {
            if (warnings.isEmpty()) return;

            if (shownWarning) {
                getWarnedClientIds();
                for (MaliciousSessions warn : warnings) {
                    for (SessionsUtil.MaliciousClient client : warn.clients) {
                        if (!warnedClients.contains(client.apiId)) {
                            shownWarning = false;
                            break;
                        }
                    }
                }
            }

            boolean hasWarningForCurrentAccount = false;
            StringBuilder msg = new StringBuilder(LocaleController.getString(R.string.MaliciousClientSessionWarning)).append("\n");
            HashSet<String> newWarnedClients = new HashSet<>();
            boolean showNames = warnings.size() > 1;

            for (MaliciousSessions warn : warnings) {
                if (warn.account() == currentAccount) hasWarningForCurrentAccount = true;
                long uid = UserConfig.getInstance(warn.account).getClientUserId();
                TLRPC.User user = MessagesController.getInstance(warn.account).getUser(uid);
                if (showNames) msg.append("\n").append(user.first_name).append(":\n");
                else msg.append("\n");
                for (SessionsUtil.MaliciousClient client : warn.clients()) {
                    newWarnedClients.add(String.valueOf(client.apiId));
                    msg.append(client.client()).append(": ");
                    StringBuilder reasons = new StringBuilder();
                    for (int reason : client.reasons()) {
                        reasons.append(LocaleController.getString(reason)).append(", ");
                    }
                    msg.append(reasons.substring(0, reasons.length() - 2));
                    msg.append("\n");
                }
            }

            msg.append("\n").append(LocaleController.getString(R.string.MaliciousClientSessionWarningBottom));
            if (hasWarningForCurrentAccount) {
                msg.append(" ").append(LocaleController.getString(R.string.MaliciousClientSessionWarningBottomOpenList));
            }
            msg.append(" ").append(LocaleController.getString(R.string.MaliciousClientSessionWarningBottomDismiss));

            boolean finalHasWarningForCurrentAccount = hasWarningForCurrentAccount;
            AndroidUtilities.runOnUIThread(() -> {
                AlertDialog.Builder dialog = new AlertDialog.Builder(context)
                        .setTitle(StrUtil.getAppName())
                        .setMessage(msg.toString())
                        .setButton(DialogInterface.BUTTON_NEGATIVE, LocaleController.getString(R.string.Dismiss), (dlg, which) -> {
                            StrUtil.appendToCSConfigString(MomoConfig.warnedClients, newWarnedClients);
                        })
                        .setButton(DialogInterface.BUTTON_NEUTRAL, LocaleController.getString(R.string.LogOut), (dlg, which) -> {
                            confirmLogoutFromAllWarningSessions(context, warnings);
                        })
                        .setCancelable(false)
                        .setTimeout(3, DialogInterface.BUTTON_NEGATIVE)
                        .setWidth((int) (AndroidUtilities.displaySize.x * 0.8));
                if (finalHasWarningForCurrentAccount) {
                    dialog.setButton(DialogInterface.BUTTON_POSITIVE, LocaleController.getString(R.string.OK),
                            (dlg, which) -> context.presentFragment(new SessionsActivity(0)));
                }
                dialog.show();

                shownWarning = true;
            });
        });
    }

    public static void checkSessions(Consumer<List<MaliciousSessions>> callback) {
        if (checking.getCount() > 0) {
            Log.d(TAG, "checkSessions(): already checking");
            return;
        }
        if ((System.currentTimeMillis() - MomoConfig.prevSessionCheck.Long()) < MIN_INTERVAL) {
            Log.d(TAG, "checkSessions(): interval too short, skipping " + (System.currentTimeMillis() - MomoConfig.prevSessionCheck.Long()));
            return;
        }
        checking = new CountDownLatch(SharedConfig.activeAccounts.size());
        for (int a : SharedConfig.activeAccounts) {
            if (!UserConfig.getInstance(a).isClientActivated()) continue;
            checkSessions(a);
        }
        try {
            checking.await();
            MomoConfig.prevSessionCheck.setConfigLong(System.currentTimeMillis());
        } catch (InterruptedException e) {
            Log.e(TAG, "interrupted while waiting for session check", e);
        }

        ArrayList<MaliciousSessions> warnings = new ArrayList<>();
        getWarnedClientIds();
        for (Map.Entry<Integer, ArrayList<TLRPC.TL_authorization>> e : sessionMap.entrySet()) {
            int acc = e.getKey();
            HashSet<Integer> added = new HashSet<>();
            ArrayList<MaliciousClient> clients = null;
            for (TLRPC.TL_authorization auth : e.getValue()) {
                String name = knownClientNames.get(auth.api_id);

                Log.d(TAG, String.format("acc %d - %d %s %s, official=%s", acc, auth.api_id, auth.app_name, auth.app_version, auth.official_app));
                if (name != null && !name.equals(auth.app_name)) Log.w(TAG, String.format("%d was renamed from '%s' to '%s'", auth.api_id, name, auth.app_name));
                knownClientNames.put(auth.api_id, auth.app_name);

                for (String client : maliciousClients.keySet()) {
                    if (auth.app_name.toLowerCase().contains(client)) {
                        boolean skip = false;
                        if (!MomoConfig.tempDebug.Bool()) {
                            for (int id : warnedClients) {
                                if (id == auth.api_id) {
                                    skip = true;
                                    break;
                                }
                            }
                        }
                        if (skip) continue;
                        if (clients == null) clients = new ArrayList<>();
                        if (added.add(auth.api_id)) {
                            Set<Integer> reasons = maliciousClients.get(client);
                            if (reasons == null) continue;
                            if (!MomoConfig.tempDebug.Bool() && reasons.contains(R.string.MaliciousClientReasonTest)) continue;
                            clients.add(new MaliciousClient(auth.api_id, auth.app_name, reasons));
                        }
                    }
                }
            }

            if (clients != null && !clients.isEmpty()) {
                warnings.add(new MaliciousSessions(acc, clients));
            }
        }

        callback.accept(warnings);
    }

    private static void checkSessions(int currentAccount) {
        TL_account.getAuthorizations req = new TL_account.getAuthorizations();

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                TL_account.authorizations res = (TL_account.authorizations) response;
                ArrayList<TLRPC.TL_authorization> list = sessionMap.get(currentAccount);
                if (list == null) list = new ArrayList<>();
                else list.clear();
                list.addAll(res.authorizations);
                sessionMap.put(currentAccount, list);
            } else {
                ArrayList<TLRPC.TL_authorization> list = sessionMap.get(currentAccount);
                if (list != null) list.clear();
            }
            checking.countDown();
        }));
    }

    public static List<Integer> getWarnedClientIds() {
        if (MomoConfig.warnedClients.String().isEmpty()) return new ArrayList<>();
        try {
            warnedClients.clear();
            for (String s : MomoConfig.warnedClients.String().split(","))
                warnedClients.add(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            Log.e(TAG, "failed to parse dismissed clients " + MomoConfig.warnedClients.String(), e);
            MomoConfig.warnedClients.setConfigString("");
        }

        return warnedClients;
    }

    public static String getSessionsString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, String> client : knownClientNames.entrySet()) {
            sb.append(client.getKey()).append(": ").append(client.getValue()).append("\n");
        }
        return sb.toString();
    }

    private static Set<Integer> getCurrentWarningApiIds(List<MaliciousSessions> sessions) {
        HashSet<Integer> ret = new HashSet<>();
        for (MaliciousSessions s : sessions) {
            for (MaliciousClient c : s.clients) {
                ret.add(c.apiId);
            }
        }
        return ret;
    }

    private static void confirmLogoutFromAllWarningSessions(LaunchActivity context, final List<MaliciousSessions> sessions) {
        AndroidUtilities.runOnUIThread(() -> {
            new AlertDialog.Builder(context)
                    .setTitle(StrUtil.getAppName())
                    .setMessage(LocaleController.getString(R.string.BatchLogoutConfirm))
                    .setButton(DialogInterface.BUTTON_POSITIVE, LocaleController.getString(R.string.OK), (dlg, w) -> logoutFromAllWarningSessions(sessions))
                    .setButton(DialogInterface.BUTTON_NEGATIVE, LocaleController.getString(R.string.Cancel), null)
                    .show();
        });
    }

    private static void logoutFromAllWarningSessions(List<MaliciousSessions> sessions) {
        Log.d(TAG, "logoutFromAllWarningSessions()");
        if (MomoConfig.tempDebug.Bool()) return;
        Set<Integer> targetApiIds = getCurrentWarningApiIds(sessions);
        for (MaliciousSessions s : sessions) {
            var allSessions = sessionMap.get(s.account);
            if (allSessions == null) continue;
            for (var authorization : allSessions) {
                if (!authorization.current && targetApiIds.contains(authorization.api_id)) {
                    TL_account.resetAuthorization req = new TL_account.resetAuthorization();
                    req.hash = authorization.hash;
                    final String targetAppName = authorization.app_name;
                    ConnectionsManager.getInstance(s.account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                        if (error != null) {
                            new AlertDialog.Builder(LaunchActivity.instance)
                                    .setTitle(LocaleController.getString(R.string.ErrorOccurred))
                                    .setMessage(LocaleController.formatString(R.string.LogoutError, targetAppName, error.text))
                                    .setButton(DialogInterface.BUTTON_POSITIVE, LocaleController.getString(R.string.OK), null)
                                    .show();
                        }
                    }));
                }
            }
        }
    }

    public record MaliciousSessions(int account, List<MaliciousClient> clients) {}

    public record MaliciousClient(int apiId, String client, Set<Integer> reasons) {}
}
