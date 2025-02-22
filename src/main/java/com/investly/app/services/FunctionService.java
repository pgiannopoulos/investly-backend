package com.investly.app.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class FunctionService {
    private static final Logger LOGGER = Logger.getLogger(FunctionService.class.getName());

    private final TradeService tradeService;

    @Autowired
    public FunctionService(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    public String handleFunctionCall(String functionName, JsonObject arguments) {
        LOGGER.info("Handling function call: " + functionName + " with arguments: " + arguments.toString());

        switch (functionName) {
            case "getBalance":
                return tradeService.getBalance();

            case "place_order":
                String symbol = arguments.get("symbol").getAsString();
                String side = arguments.get("side").getAsString();
                double amount = arguments.get("amount").getAsDouble();
                String currency = arguments.has("currency") ? arguments.get("currency").getAsString() : "USDT";
                return tradeService.placeOrder(symbol, side, amount, currency);

            case "cancel_order":
                return tradeService.cancelOrder(
                        arguments.get("orderId").getAsLong(),
                        arguments.get("symbol").getAsString()
                );

            case "fetch_trade_history":
                String ssymbol = arguments.has("symbol") ? arguments.get("symbol").getAsString() : null; // Allow null to fetch all
                int limit = arguments.has("limit") ? arguments.get("limit").getAsInt() : 5; // Default limit if missing
                return tradeService.fetchTradeHistory(ssymbol, limit);

            case "get_profit_loss":
                return tradeService.getProfitLoss(arguments.get("symbol").getAsString());

            case "create_widget":

                String type = getStringOrDefault(arguments, "type", "PORTFOLIO");
                List<String> assets = new ArrayList<>();

                // Handle assets based on widget type
                if ("PORTFOLIO".equals(type)) {
                    // For portfolio, we accept empty assets
                    assets = new ArrayList<>();
                } else {
                    // For other types, parse assets array if present
                    JsonElement assetsElement = arguments.get("assets");
                    if (assetsElement != null && !assetsElement.isJsonNull()) {
                        if (assetsElement.isJsonArray()) {
                            JsonArray assetsArray = assetsElement.getAsJsonArray();
                            for (JsonElement element : assetsArray) {
                                assets.add(element.getAsString());
                            }
                        } else if (!assetsElement.getAsString().isEmpty()) {
                            // Handle single asset as string
                            assets.add(assetsElement.getAsString());
                        }
                    }
                }

                String timeframe = getStringOrDefault(arguments, "timeframe", null);
                String startDate = getStringOrDefault(arguments, "startDate", null);
                String endDate = getStringOrDefault(arguments, "endDate", null);
                Boolean isBuy = getBooleanOrDefault(arguments, "isBuy", null);

                return tradeService.createWidget(type, assets, timeframe, startDate, endDate, isBuy);
            default:
                return "{\"error\": \"Invalid function name\"}";
        }
    }

    private String getStringOrDefault(JsonObject obj, String key, String defaultValue) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }
        return element.getAsString();
    }

    private Boolean getBooleanOrDefault(JsonObject obj, String key, Boolean defaultValue) {
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }
        return element.getAsBoolean();
    }


}
