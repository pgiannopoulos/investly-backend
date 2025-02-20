package com.investly.app.services;

import com.google.gson.*;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class TradeService {

    @Value("${binance.api.base-url}")
    private String binanceBaseUrl;

    @Value("${binance.api.key}")
    private String binanceApiKey;

    @Value("${binance.api.secret}")
    private String apiSecret;

    private final OkHttpClient httpClient;

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeService.class);

    @Autowired
    public TradeService(@Value("${binance.api.key}") String binanceApiKey,
                        @Value("${binance.api.baseurl}") String binanceBaseUrl) {
        this.httpClient = new OkHttpClient();
        this.binanceApiKey = binanceApiKey;
        this.binanceBaseUrl = binanceBaseUrl;
    }

    public String placeOrder(String symbol, String side, double amount, String currency) {
        try {
            // If the amount is in fiat (EUR), convert to USDT first
            if (currency.equalsIgnoreCase("EUR")) {
                double usdtRate = getCryptoPrice("EURUSDT");
                if (usdtRate <= 0) {
                    return "{\"error\": \"Failed to fetch EUR/USDT conversion rate\"}";
                }
                amount = amount * usdtRate; // Convert EUR to USDT
            }

            // Convert USDT to crypto
            double cryptoPrice = getCryptoPrice(symbol);
            if (cryptoPrice <= 0) {
                return "{\"error\": \"Failed to fetch price for " + symbol + "\"}";
            }

            double cryptoAmount = amount / cryptoPrice; // Convert USDT to the requested crypto
            cryptoAmount = roundQuantity(symbol, cryptoAmount);

            long timestamp = System.currentTimeMillis() + getServerTimeOffset();
            String queryString = "symbol=" + symbol + "&side=" + side.toUpperCase() +
                    "&type=MARKET&quantity=" + cryptoAmount + "&timestamp=" + timestamp;
            String signature = generateSignature(queryString);
            String url = binanceBaseUrl + "/api/v3/order?" + queryString + "&signature=" + signature;

            LOGGER.info("Sending Binance order request: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(null, new byte[0])) // Binance requires an empty body
                    .addHeader("X-MBX-APIKEY", binanceApiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.error("Failed to place order. HTTP Code: " + response.code());
                    return "{\"error\": \"Failed to place order\"}";
                }
                return response.body().string();
            }
        } catch (Exception e) {
            LOGGER.error("Exception in placeOrder: " + e.getMessage());
            return "{\"error\": \"Error placing order\"}";
        }
    }

    public String cancelOrder(long orderId, String symbol) {
        try {
            long timeOffset = getServerTimeOffset();
            long timestamp = System.currentTimeMillis() + timeOffset;
            String endpoint = "/api/v3/order";

            String queryString = "symbol=" + symbol +
                    "&orderId=" + orderId +
                    "&timestamp=" + timestamp;
            String signature = generateSignature(queryString);

            String url = binanceBaseUrl + endpoint + "?" + queryString + "&signature=" + signature;
            LOGGER.info("Sending Binance cancel order request: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("X-MBX-APIKEY", binanceApiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.error("Failed to cancel order. HTTP Code: " + response.code());
                    return "{\"error\": \"Failed to cancel order\"}";
                }

                return response.body().string();
            }
        } catch (Exception e) {
            LOGGER.error("Exception in cancelOrder: " + e.getMessage());
            return "{\"error\": \"Failed to cancel order\"}";
        }
    }

    public String fetchTradeHistory(String symbol, int limit) {
        try {
            long timestamp = System.currentTimeMillis();

            // If symbol is null, fetch balances and get all active pairs
            if (symbol == null) {
                String balancesResponse = getBalance();
                JsonObject balancesJson = JsonParser.parseString(balancesResponse).getAsJsonObject();
                JsonArray balances = balancesJson.getAsJsonArray("balances");

                JsonArray combinedTrades = new JsonArray();
                for (JsonElement balanceElement : balances) {
                    JsonObject balance = balanceElement.getAsJsonObject();
                    String asset = balance.get("asset").getAsString();
                    double freeBalance = balance.get("free").getAsDouble();

                    // Skip assets with 0 balance
                    if (freeBalance > 0) {
                        String tradeHistory = fetchTradeHistory(asset + "USDT", limit); // Assume USDT pairs
                        if (!tradeHistory.contains("error")) {
                            JsonArray assetTrades = JsonParser.parseString(tradeHistory).getAsJsonArray();
                            combinedTrades.addAll(assetTrades);
                        }
                    }
                }

                JsonObject result = new JsonObject();
                result.add("all_trades", combinedTrades);
                return result.toString();
            }

            // Fetch trade history for a specific pair if symbol is provided
            String queryString = "symbol=" + symbol + "&limit=" + limit + "&timestamp=" + timestamp;
            String signature = generateSignature(queryString);

            String url = binanceBaseUrl + "/api/v3/myTrades?" + queryString + "&signature=" + signature;
            LOGGER.info("Fetching trade history from Binance: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("X-MBX-APIKEY", binanceApiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.error("Failed to fetch trade history. HTTP Code: " + response.code());
                    return "{\"error\": \"Failed to fetch trade history\"}";
                }
                return response.body().string();
            }
        } catch (Exception e) {
            LOGGER.error("Exception in fetchTradeHistory: " + e.getMessage());
            return "{\"error\": \"Failed to fetch trade history\"}";
        }
    }

    public String getProfitLoss(String symbol) {
        try {
            String tradeHistory = fetchTradeHistory(symbol, 10);

            // Check if response contains an error
            if (tradeHistory.contains("error")) {
                return tradeHistory; // Return error message
            }

            JsonArray trades = JsonParser.parseString(tradeHistory).getAsJsonArray();
            double totalBuy = 0, totalSell = 0;

            for (JsonElement tradeElement : trades) {
                JsonObject trade = tradeElement.getAsJsonObject();
                double price = trade.get("price").getAsDouble();
                double qty = trade.get("qty").getAsDouble();
                boolean isBuyer = trade.get("isBuyer").getAsBoolean();

                if (isBuyer) {
                    totalBuy += price * qty;
                } else {
                    totalSell += price * qty;
                }
            }

            double profitLoss = totalSell - totalBuy;
            JsonObject result = new JsonObject();
            result.addProperty("profit_loss", profitLoss);
            return result.toString();
        } catch (Exception e) {
            LOGGER.error("Exception in getProfitLoss: " + e.getMessage());
            return "{\"error\": \"Failed to calculate profit/loss\"}";
        }
    }

    public String getBalance() {
        try {
            long timeOffset = getServerTimeOffset();
            long timestamp = System.currentTimeMillis() + timeOffset;

            String endpoint = "/api/v3/account";
            String queryString = "timestamp=" + timestamp;
            String signature = generateSignature(queryString);

            String url = binanceBaseUrl + endpoint + "?" + queryString + "&signature=" + signature;
            LOGGER.info("Sending request to Binance: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("X-MBX-APIKEY", binanceApiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.error("Failed to fetch balance. HTTP Code: " + response.code());
                    return "{\"error\": \"Failed to retrieve balance\"}";
                }

                // Parse Binance API response
                String responseBody = response.body().string();
                JsonObject binanceResponse = JsonParser.parseString(responseBody).getAsJsonObject();

                // Extract only relevant balance data
                JsonArray balances = binanceResponse.getAsJsonArray("balances");
                JsonObject cleanedResponse = new JsonObject();
                JsonArray topBalances = new JsonArray();

                // Define top pairs
                Set<String> topPairs = Set.of("BTC", "ETH", "XRP", "DOGE", "ADA", "BNB", "SOL", "MATIC", "DOT", "LTC");

                for (JsonElement balanceElement : balances) {
                    JsonObject balance = balanceElement.getAsJsonObject();
                    String asset = balance.get("asset").getAsString();

                    if (topPairs.contains(asset)) {
                        JsonObject assetDetails = new JsonObject();
                        assetDetails.addProperty("asset", asset);
                        assetDetails.addProperty("free", balance.get("free").getAsString());
                        assetDetails.addProperty("locked", balance.get("locked").getAsString());
                        topBalances.add(assetDetails);
                    }
                }

                cleanedResponse.add("balances", topBalances);
                LOGGER.info("Cleaned Binance Balance Response: " + cleanedResponse.toString());
                return cleanedResponse.toString();

            }
        } catch (Exception e) {
            LOGGER.error("Exception in getBalance: " + e.getMessage());
            return "{\"error\": \"Failed to retrieve balance\"}";
        }
    }

    public String createWidget(String type, List<String> assets, String timeframe, String startDate, String endDate, Boolean isBuy) {
        JsonObject response = new JsonObject();

        try {
            // Handle type
            type = (type != null) ? type : "QUICK_TRADE";  // Default to QUICK_TRADE if null
            response.addProperty("type", type);

            // Handle assets
            if (assets == null) {
                assets = new ArrayList<>();
            }
            response.add("assets", JsonParser.parseString(new Gson().toJson(assets)));

            // For QUICK_TRADE type, we only need assets and isBuy
            if ("QUICK_TRADE".equals(type)) {
                response.addProperty("timeframe", "");
                response.addProperty("startDate", "");
                response.addProperty("endDate", "");
                response.addProperty("isBuy", true);  // Default to true if null
            } else {
                // For other widget types, handle timeframe and dates
                String currentDate = java.time.LocalDate.now().toString();
                String defaultEndDate = currentDate;
                String defaultStartDate = java.time.LocalDate.now().minusMonths(1).toString();

                response.addProperty("timeframe", timeframe != null ? timeframe : "1d");
                response.addProperty("startDate", startDate != null ? startDate : defaultStartDate);
                response.addProperty("endDate", endDate != null ? endDate : defaultEndDate);
                response.addProperty("isBuy", false);  // isBuy is only relevant for QUICK_TRADE
            }

            response.addProperty("status", "success");
            response.addProperty("message", "Widget created successfully");

            return response.toString();

        } catch (Exception e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Failed to create widget: " + e.getMessage());
            return response.toString();
        }
    }

    private String generateSignature(String data) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            return bytesToHex(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC-SHA256 signature");
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public long getServerTimeOffset() {
        String url = binanceBaseUrl + "/api/v3/time";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOGGER.error("Failed to fetch Binance server time");
                return 0;
            }

            String responseBody = response.body().string();
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            long serverTime = json.get("serverTime").getAsLong();
            long localTime = System.currentTimeMillis();
            return serverTime - localTime;  // Calculate offset
        } catch (IOException e) {
            LOGGER.error("Exception in getServerTimeOffset: " + e.getMessage());
            return 0;
        }
    }

    private double roundQuantity(String symbol, double quantity) {
        try {
            // Fetch trading precision dynamically
            double stepSize = getStepSize(symbol);
            if (stepSize == 0) {
                LOGGER.warn("Using default rounding (2 decimals) for " + symbol);
                stepSize = 0.01; // Fallback default
            }

            // Compute precision from step size
            int precision = (int) Math.round(-Math.log10(stepSize));
            double scale = Math.pow(10, precision);
            return Math.floor(quantity * scale) / scale; // Ensure Binance-compatible rounding

        } catch (Exception e) {
            LOGGER.error("Failed to fetch stepSize for " + symbol + ", defaulting to 2 decimals.");
            return Math.floor(quantity * 100) / 100; // Fallback rounding
        }
    }

    private double getStepSize(String symbol) {
        try {
            String url = binanceBaseUrl + "/api/v3/exchangeInfo";
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.error("Failed to fetch exchange info. HTTP Code: " + response.code());
                    return 0;
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonArray symbols = jsonResponse.getAsJsonArray("symbols");

                for (JsonElement element : symbols) {
                    JsonObject symbolObj = element.getAsJsonObject();
                    if (symbolObj.get("symbol").getAsString().equals(symbol)) {
                        JsonArray filters = symbolObj.getAsJsonArray("filters");

                        for (JsonElement filter : filters) {
                            JsonObject filterObj = filter.getAsJsonObject();
                            if (filterObj.get("filterType").getAsString().equals("LOT_SIZE")) {
                                return filterObj.get("stepSize").getAsDouble();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception in getStepSize: " + e.getMessage());
        }
        return 0; // Default (error case)
    }

    public double getCryptoPrice(String symbol) {
        try {
            String url = binanceBaseUrl + "/api/v3/ticker/price?symbol=" + symbol;
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("X-MBX-APIKEY", binanceApiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.error("Failed to fetch price. HTTP Code: " + response.code());
                    return 0;
                }
                JsonObject jsonResponse = JsonParser.parseString(response.body().string()).getAsJsonObject();
                return jsonResponse.get("price").getAsDouble();
            }
        } catch (Exception e) {
            LOGGER.error("Exception in getCryptoPrice: " + e.getMessage());
            return 0;
        }
    }


}
