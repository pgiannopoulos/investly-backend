package com.investly.app.services;

import com.google.gson.JsonObject;
import org.springframework.stereotype.Service;
import java.util.logging.Logger;

@Service
public class FunctionService {
    private static final Logger LOGGER = Logger.getLogger(FunctionService.class.getName());

    public String handleFunctionCall(String functionName, JsonObject arguments) {
        LOGGER.info("Handling function call: " + functionName + " with arguments: " + arguments.toString());

        return switch (functionName) {
            case "getBalance" -> getBalance();
            case "place_order" -> placeOrder(arguments);
            case "get_profit_loss" -> getProfitLoss(arguments);
            case "fetch_trade_history" -> fetchTradeHistory(arguments);
            case "cancel_order" -> cancelOrder(arguments);
            case "create_widget" -> createWidget(arguments);
            default -> "Unknown function call: " + functionName;
        };
    }

    private String getBalance() {
        LOGGER.info("Mocking Binance balance retrieval...");
        return "{ \"balance\": \"1234.56 USDT\" }";
    }

    private String placeOrder(JsonObject arguments) {
        String symbol = arguments.has("symbol") ? arguments.get("symbol").getAsString() : "BTCUSDT";
        String side = arguments.has("side") ? arguments.get("side").getAsString() : "BUY";
        double amount = arguments.has("amount") ? arguments.get("amount").getAsDouble() : 10.0;

        LOGGER.info("Placing mock order: " + side + " " + amount + " of " + symbol);
        return "{ \"order_status\": \"success\", \"symbol\": \"" + symbol + "\", \"side\": \"" + side + "\", \"amount\": " + amount + " }";
    }

    private String getProfitLoss(JsonObject arguments) {
        String symbol = arguments.has("symbol") ? arguments.get("symbol").getAsString() : "BTCUSDT";
        LOGGER.info("Fetching mock profit/loss for: " + symbol);
        return "{ \"symbol\": \"" + symbol + "\", \"profit_loss\": \"+250 USDT\" }";
    }

    private String fetchTradeHistory(JsonObject arguments) {
        int limit = arguments.has("limit") ? arguments.get("limit").getAsInt() : 5;
        LOGGER.info("Fetching mock trade history (limit: " + limit + ")");
        return "{ \"trades\": [ { \"symbol\": \"BTCUSDT\", \"amount\": \"0.01 BTC\", \"price\": \"40000 USDT\" }, { \"symbol\": \"ETHUSDT\", \"amount\": \"0.5 ETH\", \"price\": \"2500 USDT\" } ] }";
    }

    private String cancelOrder(JsonObject arguments) {
        int orderId = arguments.has("orderId") ? arguments.get("orderId").getAsInt() : 12345;
        LOGGER.info("Cancelling mock order ID: " + orderId);
        return "{ \"orderId\": " + orderId + ", \"status\": \"cancelled\" }";
    }

    private String createWidget(JsonObject arguments) {
        String type = arguments.has("type") ? arguments.get("type").getAsString() : "PROFIT_LOSS";
        LOGGER.info("Creating mock widget of type: " + type);
        return "{ \"widget_type\": \"" + type + "\", \"status\": \"created\" }";
    }
}
