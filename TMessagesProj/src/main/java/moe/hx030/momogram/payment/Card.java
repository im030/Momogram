package moe.hx030.momogram.payment;


import android.os.AsyncTask;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.PaymentFormActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class Card {
        /*
                Card card = new Card(
                inputFields[FIELD_CARD].getText().toString(),
                month,
                year,
                inputFields[FIELD_CVV].getText().toString(),
                inputFields[FIELD_CARDNAME].getText().toString(),
                null, null, null, null,
                inputFields[FIELD_CARD_POSTCODE].getText().toString(),
                inputFields[FIELD_CARD_COUNTRY].getText().toString(),
                null);
                cardName = card.getBrand() + " *" + card.getLast4();
        */

    private String CVV, PAN, name, postCode, country, brand;
    private final int expMonth, expYear;
    private Info info = null;

    private static final HashMap<String, Info> cache = new HashMap<>();
    public static class Info {
        public final String PANPrefix, brand, country;

        public Info(String pre, String brand, String country) {
            PANPrefix = pre;
            this.brand = brand;
            this.country = country;
        }
    }

    public Card(String pan, int month, int year, String cvv, String name, Object ignored1,
                Object ignored2, Object ignored3, Object ignored4,
                String post, String country,
                Object ignored5) {
        CVV = cvv;
        PAN = pan.replaceAll("\\D", "");
        expMonth = month;
        expYear = year;
        this.country = country;
        this.name = name;
        this.postCode = post;
    }

    public boolean validateCVC() {
        try {
            Integer.parseInt(CVV);
        } catch (Exception ignore) {
            return false;
        }
        return CVV.length() == 3;
    }

    public boolean validateExpYear() {
        return ((Calendar.getInstance().get(Calendar.YEAR) % 100) <= expYear);
    }

    public boolean validateExpMonth() {
        Calendar now = Calendar.getInstance();
        int currentYear = (now.get(Calendar.YEAR)) % 100;
        int currentMonth = now.get(Calendar.MONTH) + 1; // NOTE.. Calendar.MONTH IS 0-based
        if (expYear < currentYear) return false;
        return expYear != currentYear || expMonth >= currentMonth;
    }

    public boolean validateNumber() {
        String digitsOnly = PAN.replaceAll("\\D", "");
        int sum = 0;
        boolean alt = false;
        for (int i = digitsOnly.length() - 1; i >= 0; i--) {
            int n = digitsOnly.charAt(i) - '0';
            if (alt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }

    public boolean validateExpiryDate() {
        return validateExpMonth();
    }

    public String getNumber() { return PAN; }
    public String getCVC() { return CVV; }
    public int getExpMonth() { return expMonth; }
    public int getExpYear() { return expYear; }

    public String getLast4() {
        if (PAN == null || PAN.length() < 4) return PAN;
        return PAN.substring(PAN.length() - 4);
    }

    public String getBrand() {
        if (brand != null) return brand;
        if (!validateNumber() || !validateExpiryDate() || !validateCVC()) return "";
        try {
            return brand = fetchInfo(PAN).brand;
        } catch (Exception ex) {
            Log.e("030-card", "", ex);
            return "";
        }
    }

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    public Info fetchInfo(String pan) throws IOException, InterruptedException {
        String digits = pan.replaceAll("\\D", "");
        if (digits.length() < 6) throw new IllegalArgumentException("PAN too short for BIN extraction");

        String bin = digits.substring(0, Math.min(digits.length(), 8));
        if (cache.containsKey(bin)) return cache.get(bin);

        // Note that We only send BIN to external service (do not send full PAN/CVV)
        String url = "https://lookup.binlist.net/" + bin;
        Log.d("030-card", "url => " + url);

        Request req = new Request.Builder().get().url(url).build();
        AsyncTask<Object, Object, Info> task = new AsyncTask<>() {
            @Override
            protected Info doInBackground(Object... objects) {
                try (var resp = CLIENT.newCall(req).execute()) {
                    if (resp.code() == 200) {
                        JSONObject json = new JSONObject(resp.body().string());
                        Info info = new Info(pan.substring(0, 7),
                                json.getString("scheme"),
                                json.getJSONObject("country").getString("alpha2"));
                        cache.put(pan, info);
                        return info;
                    } else if (resp.code() == 429) {
                        throw new IOException("Rate limited by BIN service (HTTP 429)");
                    } else if (resp.code() == 404) {
                        return null; // not found
                    } else {
                        throw new IOException("Unexpected HTTP status: " + resp.code());
                    }
                } catch (JSONException | IOException e) {
                    Log.e("030-card", "unexpected response from binlist", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Info result) {
                info = result;
            }
        };

        try {
            return task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
        } catch (ExecutionException e) {
            throw new IOException(e);
        }
    }
}
