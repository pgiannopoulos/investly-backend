package com.investly.app.services;

import com.google.gson.*;
import com.investly.app.dao.MessageRepository;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Service
public class AIService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private FunctionService functionService;

    private static final String OPENAI_THREADS_URL = "https://api.openai.com/v1/threads";
    private static final Logger LOGGER = Logger.getLogger(AIService.class.getName());

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.assistant.id}")
    private String assistantId;

    private final OkHttpClient client = new OkHttpClient();

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public String processUserMessage(String userMessage) {
        try {
            // Always create a new thread
            String threadId = createThread();
            LOGGER.info("Created new thread: " + threadId);

            boolean messageAdded = addMessageToThread(threadId, userMessage);
            if (!messageAdded) {
                LOGGER.severe("Failed to add message to thread: " + threadId);
                return "Error: Failed to add message to thread.";
            }

            LOGGER.info("Attempting to start assistant run on thread: " + threadId);
            String runId = runAssistant(threadId);
            if (runId == null) {
                LOGGER.severe("Error: Failed to start assistant run.");
                return "Error: Failed to start assistant run.";
            }

            LOGGER.info("Waiting for assistant completion on thread: " + threadId + ", run ID: " + runId);
            boolean completed = waitForCompletion(threadId);
            if (!completed) {
                LOGGER.severe("Error: Assistant did not complete.");
                return "Error: Assistant did not complete.";
            }

            LOGGER.info("Fetching assistant response for thread: " + threadId);
            return fetchAssistantResponse(threadId);

        } catch (IOException | InterruptedException e) {
            LOGGER.severe("Exception: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    private String runAssistant(String threadId) throws IOException {
        JsonObject runData = new JsonObject();
        runData.addProperty("assistant_id", assistantId);
        runData.addProperty("response_format", "auto");

        // Define valid function schemas
        JsonArray tools = new JsonArray();

        // getBalance function
        tools.add(createFunctionSchema("getBalance",
                "Retrieve the user's Binance account balance",
                new String[]{}, new String[]{}));

        // place_order function
        tools.add(createFunctionSchema("place_order",
                "Execute a trade on Binance",
                new String[]{"symbol", "side", "amount"},
                new String[]{"symbol", "side", "amount"},
                new String[]{"BUY", "SELL"}));

        // get_profit_loss function
        tools.add(createFunctionSchema("get_profit_loss",
                "Check unrealized profit/loss for an asset",
                new String[]{"symbol"},
                new String[]{"symbol"}));

        // fetch_trade_history function
        tools.add(createFunctionSchema("fetch_trade_history",
                "Retrieve past executed trades",
                new String[]{"limit"},
                new String[]{"limit"}));

        // cancel_order function
        tools.add(createFunctionSchema("cancel_order",
                "Cancel an open Binance order",
                new String[]{"orderId"},
                new String[]{"orderId"}));

        // get_top_movers function
        tools.add(createFunctionSchema("get_top_movers",
                "Retrieve the top moving cryptocurrencies by percentage change",
                new String[]{"timeframe", "limit"},
                new String[]{"timeframe", "limit"},
                new String[]{"1h", "24h", "7d"}));

        // create_widget function
        tools.add(createFunctionSchema("create_widget",
                "Generate a widget for the user based on their request",
                new String[]{"type", "asset"},
                new String[]{"type", "asset"},
                new String[]{"PROFIT_LOSS", "QUICK_TRADE", "MARKET_OVERVIEW"}));

        tools.add(createFunctionSchema("general_investment_advice",
                "Provides general investment advice based on user input",
                new String[]{"textPrompt"},
                new String[]{"textPrompt"}));

        runData.add("tools", tools);
        runData.addProperty("tool_choice", "auto");
        runData.addProperty("parallel_tool_calls", true); // Allow multiple functions in one request


        Request request = new Request.Builder()
                .url(OPENAI_THREADS_URL + "/" + threadId + "/runs")
                .post(RequestBody.create(MediaType.parse("application/json"), runData.toString()))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            LOGGER.severe("error running assistant: " + response.body().string());
            return null;
        }

        JsonObject responseBody = JsonParser.parseString(response.body().string()).getAsJsonObject();
        return responseBody.get("id").getAsString();
    }

    private JsonObject createFunctionSchema(String name, String description, String[] requiredParams, String[] paramNames) {
        return createFunctionSchema(name, description, requiredParams, paramNames, null);
    }

    private JsonObject createFunctionSchema(String name, String description, String[] requiredParams, String[] paramNames, String[] enumValues) {
        JsonObject function = new JsonObject();
        function.addProperty("type", "function");

        JsonObject details = new JsonObject();
        details.addProperty("name", name);
        details.addProperty("description", description);

        JsonObject params = new JsonObject();
        params.addProperty("type", "object");

        JsonObject properties = new JsonObject();

        // 🔹 Prevent NullPointerException by checking for null
        if (paramNames != null) {
            for (String paramName : paramNames) {
                if (enumValues != null && paramName.equals("type")) {
                    properties.add(paramName, createEnumProperty(enumValues));
                } else {
                    properties.add(paramName, createStringOrIntegerProperty(paramName));
                }
            }
        }

        params.add("properties", properties);

        // 🔹 Handle `requiredParams` safely
        if (requiredParams != null) {
            params.add("required", JsonParser.parseString(gson.toJson(requiredParams)));
        } else {
            params.add("required", new JsonArray()); // Empty array instead of null
        }

        params.addProperty("additionalProperties", false);

        details.add("parameters", params);
        function.add("function", details);
        return function;
    }

    private JsonObject createStringOrIntegerProperty(String paramName) {
        JsonObject property = new JsonObject();
        if (paramName.equals("orderId") || paramName.equals("limit")) {
            property.addProperty("type", "integer");
        } else {
            property.addProperty("type", "string");
        }
        return property;
    }

    private JsonObject createEnumProperty(String[] values) {
        JsonObject property = new JsonObject();
        property.addProperty("type", "string");
        property.add("enum", JsonParser.parseString(gson.toJson(values)));
        return property;
    }

    private boolean waitForCompletion(String threadId) throws IOException, InterruptedException {
        String url = OPENAI_THREADS_URL + "/" + threadId + "/runs";
        LOGGER.info("waiting for assistant completion on thread: " + threadId);

        int maxRetries = 10; // retry limit
        int retryCount = 0;

        while (retryCount < maxRetries) {
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("OpenAI-Beta", "assistants=v2")
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                LOGGER.severe("error polling assistant run: " + response.body().string());
                return false;
            }

            String responseString = response.body().string();
            LOGGER.info("assistant run status response: " + responseString);

            JsonObject responseBody = JsonParser.parseString(responseString).getAsJsonObject();
            JsonArray runs = responseBody.getAsJsonArray("data");

            if (runs == null || runs.size() == 0) {
                LOGGER.severe("no assistant run found for thread: " + threadId);
                return false;
            }

            for (JsonElement runElement : runs) {
                JsonObject run = runElement.getAsJsonObject(); // Convert JsonElement to JsonObject
                String status = run.get("status").getAsString();

                if ("completed".equals(status)) {
                    return true;
                } else if ("requires_action".equals(status)) {
                    LOGGER.info("assistant requires action on thread: " + threadId);
                    return handleFunctionCall(threadId, run);
                }
            }


            retryCount++;
            TimeUnit.SECONDS.sleep(5); // wait before retrying
        }

        LOGGER.severe("timeout: assistant did not complete within expected time.");
        return false;
    }

    public String createThread() throws IOException {
        Request request = new Request.Builder()
                .url(OPENAI_THREADS_URL)
                .post(RequestBody.create(MediaType.parse("application/json"), "{}"))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            LOGGER.severe("Error creating thread: " + response.body().string());
            return null;
        }

        JsonObject responseBody = JsonParser.parseString(response.body().string()).getAsJsonObject();
        return responseBody.get("id").getAsString();
    }

    private boolean addMessageToThread(String threadId, String userMessage) throws IOException {
        JsonObject messageData = new JsonObject();
        messageData.addProperty("role", "user");

        // Creating an array of content objects
        JsonArray contentArray = new JsonArray();
        JsonObject contentObject = new JsonObject();
        contentObject.addProperty("type", "text");
        contentObject.addProperty("text", userMessage); // Send raw message
        contentArray.add(contentObject);

        messageData.add("content", contentArray);

        LOGGER.info("Sending message to thread: " + threadId + " -> " + messageData);

        Request request = new Request.Builder()
                .url(OPENAI_THREADS_URL + "/" + threadId + "/messages")
                .post(RequestBody.create(MediaType.parse("application/json"), messageData.toString()))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        Response response = client.newCall(request).execute();
        String responseBodyString = response.body().string();

        if (!response.isSuccessful()) {
            LOGGER.severe("OpenAI API rejected message: " + responseBodyString);
            return false;
        }

        LOGGER.info("Message successfully added to thread.");
        return true;
    }

    private String fetchAssistantResponse(String threadId) throws IOException, InterruptedException {
        String url = OPENAI_THREADS_URL + "/" + threadId + "/messages";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        LOGGER.info("waiting for assistant to process function results on thread: " + threadId);

        for (int i = 0; i < 10; i++) { // retry for up to 10 seconds
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("OpenAI-Beta", "assistants=v2")
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                LOGGER.severe("error fetching assistant response: " + response.body().string());
                return "error retrieving response.";
            }

            String responseBodyString = response.body().string();
            LOGGER.info("raw openai api response: " + responseBodyString);

            JsonObject responseBody = JsonParser.parseString(responseBodyString).getAsJsonObject();
            JsonArray messages = responseBody.getAsJsonArray("data");

            if (messages == null || messages.size() == 0) {
                LOGGER.warning("no messages found yet, retrying...");
                TimeUnit.SECONDS.sleep(2);
                continue; // Wait and retry
            }

            for (int j = messages.size() - 1; j >= 0; j--) {
                JsonObject message = messages.get(j).getAsJsonObject();
                String role = message.get("role").getAsString();

                if ("assistant".equals(role)) {
                    // check if assistant is calling a function
                    if (message.has("tool_calls")) {
                        JsonArray toolCalls = message.getAsJsonArray("tool_calls");
                        List<JsonObject> toolOutputs = new ArrayList<>();

                        for (JsonElement toolCallElement : toolCalls) {
                            JsonObject toolCall = toolCallElement.getAsJsonObject();
                            String toolId = toolCall.get("id").getAsString();
                            String functionName = toolCall.getAsJsonObject("function").get("name").getAsString();
                            JsonObject arguments = toolCall.getAsJsonObject("function").getAsJsonObject("arguments");

                            LOGGER.info("assistant requested function: " + functionName + " with arguments: " + arguments.toString());

                            // execute function and capture result
                            String functionResult = functionService.handleFunctionCall(functionName, arguments);

                            // prepare tool output for submission
                            JsonObject toolOutput = new JsonObject();
                            toolOutput.addProperty("tool_call_id", toolId);
                            toolOutput.addProperty("output", functionResult);
                            toolOutputs.add(toolOutput);
                        }

                        String runId = getActiveRunId(threadId); // extract the active run ID
                        if (runId == null) {
                            LOGGER.severe("no active run found for thread: " + threadId);
                            return "error: no active run found.";
                        }

                        boolean success = submitFunctionOutputs(threadId, runId, toolOutputs);

                        if (!success) {
                            LOGGER.severe("error submitting function result.");
                            return "error: failed to submit function result.";
                        }
                    }

                    // otherwise, check for normal text response
                    JsonArray contentArray = message.getAsJsonArray("content");
                    if (contentArray != null && contentArray.size() > 0) {
                        JsonObject firstContent = contentArray.get(0).getAsJsonObject();
                        if ("text".equals(firstContent.get("type").getAsString())) {
                            String rawResponse = firstContent.getAsJsonObject("text").get("value").getAsString();
                            rawResponse = rawResponse.replaceAll("^```json\\s*|```$", "").trim();
                            try {
                                JsonObject jsonResponse = JsonParser.parseString(rawResponse).getAsJsonObject();
                                return jsonResponse.toString(); // Ensures full JSON is passed
                            } catch (Exception e) {
                                LOGGER.warning("Failed to parse response as JSON. Returning raw response.");
                                return rawResponse; // Fallback
                            }

                        }
                    }
                }
            }

            LOGGER.warning("no assistant response yet, retrying...");
            TimeUnit.SECONDS.sleep(2);
        }

        return "no valid assistant response found.";
    }

    private boolean submitFunctionOutputs(String threadId, String runId, List<JsonObject> toolOutputs) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.add("tool_outputs", JsonParser.parseString(toolOutputs.toString()));

        Request request = new Request.Builder()
                .url(OPENAI_THREADS_URL + "/" + threadId + "/runs/" + runId + "/submit_tool_outputs")
                .post(RequestBody.create(MediaType.parse("application/json"), requestBody.toString()))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        Response response = client.newCall(request).execute();
        String responseString = response.body().string();

        if (!response.isSuccessful()) {
            LOGGER.severe("error submitting function outputs: " + responseString);
            return false;
        }

        LOGGER.info("successfully submitted function outputs.");
        return true;
    }

    private boolean handleFunctionCall(String threadId, JsonObject run) throws IOException {
        JsonObject requiredAction = run.getAsJsonObject("required_action");
        if (requiredAction == null || !requiredAction.has("submit_tool_outputs")) {
            LOGGER.severe("unexpected action required by assistant");
            return false;
        }

        JsonArray toolCalls = requiredAction.getAsJsonObject("submit_tool_outputs").getAsJsonArray("tool_calls");
        if (toolCalls == null || toolCalls.size() == 0) {
            LOGGER.severe("no tool calls found in required action");
            return false;
        }

        List<JsonObject> toolOutputs = new ArrayList<>();
        for (JsonElement toolCallElement : toolCalls) {
            JsonObject toolCall = toolCallElement.getAsJsonObject();
            JsonObject functionData = toolCall.getAsJsonObject("function");

            String functionName = functionData.get("name").getAsString();
            JsonObject arguments = JsonParser.parseString(functionData.get("arguments").getAsString()).getAsJsonObject();

            LOGGER.info("executing function: " + functionName + " with arguments: " + arguments.toString());

            // call the function from FunctionService
            String functionResponse = functionService.handleFunctionCall(functionName, arguments);

            // prepare response for OpenAI
            JsonObject toolOutput = new JsonObject();
            toolOutput.addProperty("tool_call_id", toolCall.get("id").getAsString());
            toolOutput.addProperty("output", functionResponse);
            toolOutputs.add(toolOutput);
        }

        String runId = getActiveRunId(threadId);
        if (runId == null) {
            LOGGER.severe("no active run found for thread: " + threadId);
            return false;
        }
        return submitFunctionOutputs(threadId, runId, toolOutputs);

    }

    private String getActiveRunId(String threadId) throws IOException {
        String url = OPENAI_THREADS_URL + "/" + threadId + "/runs";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            LOGGER.severe("error fetching active runs: " + response.body().string());
            return null;
        }

        String responseBodyString = response.body().string();
        JsonObject responseBody = JsonParser.parseString(responseBodyString).getAsJsonObject();
        JsonArray runs = responseBody.getAsJsonArray("data");

        if (runs == null || runs.size() == 0) {
            LOGGER.severe("no active runs found for thread: " + threadId);
            return null;
        }

        for (JsonElement runElement : runs) {
            JsonObject run = runElement.getAsJsonObject();
            String status = run.get("status").getAsString();
            if ("requires_action".equals(status)) {
                return run.get("id").getAsString();
            }
        }

        LOGGER.severe("no matching active run found for thread: " + threadId);
        return null;
    }

}
