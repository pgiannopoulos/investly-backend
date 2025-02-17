package com.investly.app.services;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class TradeService {

    @Value("${binance.api.base-url}")
    private String binanceApiBaseUrl;

    @Value("${binance.api.key}")
    private String apiKey;

    @Value("${binance.api.secret}")
    private String apiSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    // Execute a trade (Buy/Sell)
    public String executeTrade(String symbol, String side, double quantity) {
        String endpoint = binanceApiBaseUrl + "/api/v3/order/test"; // Test order for now

        Map<String, String> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("side", side.toUpperCase()); // BUY or SELL
        params.put("type", "MARKET");
        params.put("quantity", String.valueOf(quantity));


        try {
            String response = restTemplate.postForObject(endpoint, params, String.class);
            return "Trade Executed: " + response;
        } catch (Exception e) {
            return "Trade Failed: " + e.getMessage();
        }
    }

    // Fetch account balance
    public String getBalance() {
        String endpoint = binanceApiBaseUrl + "/api/v3/account";

        try {
            String response = restTemplate.getForObject(endpoint, String.class);
            return "Balance: " + response;
        } catch (Exception e) {
            return "Failed to fetch balance: " + e.getMessage();
        }
    }
}
