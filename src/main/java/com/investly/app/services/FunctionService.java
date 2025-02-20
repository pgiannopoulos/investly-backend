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

                String widgetType = arguments.get("type").getAsString();

                // Convert JSON array to List<String>
                List<String> assets = new ArrayList<>();
                JsonArray assetsArray = arguments.getAsJsonArray("assets");
                for (JsonElement element : assetsArray) {
                    assets.add(element.getAsString());
                }

                String timeframe = arguments.get("timeframe").getAsString();
                String startDate = arguments.get("startDate").getAsString();
                String endDate = arguments.get("endDate").getAsString();
                Boolean isBuy = arguments.get("isBuy").getAsBoolean();

                return tradeService.createWidget(
                        widgetType,
                        assets,
                        timeframe,
                        startDate,
                        endDate,
                        isBuy
                );
            default:
                return "{\"error\": \"Invalid function name\"}";
        }
    }

}
