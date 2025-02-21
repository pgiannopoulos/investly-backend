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
import java.util.Map;
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
    private volatile String singleThreadId;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.assistant.id}")
    private String assistantId;

    private final OkHttpClient client = new OkHttpClient();

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public String processUserMessage(String userMessage) {
        try {
            if (singleThreadId == null) {
                singleThreadId = createThread();
                LOGGER.info("Created single thread: " + singleThreadId);
            }

            // Cancel any active run on this thread
            String activeRunId = getActiveRunId(singleThreadId);
            if (activeRunId != null) {
                LOGGER.info("Found active run: " + activeRunId + ". Attempting to cancel...");
                boolean cancelled = cancelActiveRun(singleThreadId, activeRunId);
                if (!cancelled) {
                    LOGGER.severe("Failed to cancel active run: " + activeRunId);
                    return "Error: Failed to cancel active run.";
                }
                LOGGER.info("Successfully cancelled active run: " + activeRunId);
            }

            // Capture the current timestamp for the user message
            long lastUserTimestamp = System.currentTimeMillis();

            boolean messageAdded = addMessageToThread(singleThreadId, userMessage);
            if (!messageAdded) {
                LOGGER.severe("Failed to add message to thread: " + singleThreadId);
                return "Error: Failed to add message to thread.";
            }

            LOGGER.info("Attempting to start assistant run on thread: " + singleThreadId);
            String runId = runAssistant(singleThreadId);
            if (runId == null) {
                LOGGER.severe("Error: Failed to start assistant run.");
                return "Error: Failed to start assistant run.";
            }

            LOGGER.info("Waiting for assistant completion on thread: " + singleThreadId + ", run ID: " + runId);
            boolean completed = waitForCompletion(singleThreadId);
            if (!completed) {
                LOGGER.severe("Error: Assistant did not complete.");
                return "Error: Assistant did not complete.";
            }

            LOGGER.info("Fetching assistant response for thread: " + singleThreadId);
            // Now call fetchAssistantResponse with the threadId, current runId, and lastUserTimestamp.
            return fetchAssistantResponse(singleThreadId, runId, lastUserTimestamp);

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
                new String[]{"type", "assets", "timeframe", "startDate", "endDate", "isBuy"},
                new String[]{"type", "assets", "timeframe", "startDate", "endDate", "isBuy"},
                new String[]{"PROFIT_LOSS", "QUICK_TRADE", "MARKET_OVERVIEW", "PORTFOLIO"}));

        tools.add(createFunctionSchema("general_investment_advice",
                "Provides general investment advice based on user input",
                new String[]{"textPrompt"},
                new String[]{"textPrompt"}));

        runData.add("tools", tools);
        runData.addProperty("tool_choice", "auto");
        runData.addProperty("parallel_tool_calls", true);

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

    private boolean waitForCompletion(String threadId) throws IOException, InterruptedException {
        String url = OPENAI_THREADS_URL + "/" + threadId + "/runs";
        LOGGER.info("waiting for assistant completion on thread: " + threadId);

        int maxRetries = 10;
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
                JsonObject run = runElement.getAsJsonObject();
                String status = run.get("status").getAsString();

                switch (status) {
                    case "completed":
                        return true;
                    case "requires_action":
                        LOGGER.info("assistant requires action on thread: " + threadId);
                        return handleFunctionCall(threadId, run);
                    case "failed":
                        LOGGER.severe("Run failed: " + run.toString());
                        return false;
                    case "expired":
                        LOGGER.severe("Run expired: " + run.toString());
                        return false;
                    case "cancelled":
                        LOGGER.severe("Run was cancelled: " + run.toString());
                        return false;
                    default:
                        // in_progress, queued, etc.
                        LOGGER.info("Run status: " + status + ", waiting...");
                        break;
                }
            }

            retryCount++;
            TimeUnit.SECONDS.sleep(5);
        }

        LOGGER.severe("timeout: assistant did not complete within expected time.");
        return false;
    }

    private String fetchAssistantResponse(String threadId, String currentRunId, long lastUserTimestamp) throws IOException, InterruptedException {
        String url = OPENAI_THREADS_URL + "/" + threadId + "/messages";
        int maxRetries = 5; // Reduced from 10
        int retryCount = 0;

        while (retryCount < maxRetries) {
            // Wait if there's an active run
            String activeRunId = getActiveRunId(threadId);
            if (activeRunId != null) {
                LOGGER.info("Found active run while fetching response. Waiting for completion...");
                boolean completed = waitForCompletion(threadId);
                if (!completed) {
                    return "{\"error\": \"Assistant run did not complete successfully.\"}";
                }
            }

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("OpenAI-Beta", "assistants=v2")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.severe("Error fetching assistant response: " + response.body().string());
                    return "{\"error\": \"Failed to retrieve response.\"}";
                }

                String responseBodyString = response.body().string();
                LOGGER.info("Raw OpenAI API response: " + responseBodyString);

                JsonObject responseBody = JsonParser.parseString(responseBodyString).getAsJsonObject();
                JsonArray messages = responseBody.getAsJsonArray("data");

                if (messages == null || messages.size() == 0) {
                    LOGGER.warning("No messages found, retrying...");
                    TimeUnit.SECONDS.sleep(2);
                    retryCount++;
                    continue;
                }

                // Reverse iterate to get the most recent assistant message
                for (int i = messages.size() - 1; i >= 0; i--) {
                    JsonObject message = messages.get(i).getAsJsonObject();

                    // Check if it's an assistant message
                    if (!"assistant".equals(message.get("role").getAsString())) {
                        continue;
                    }

                    // Check if the message is associated with the current run
                    if (!message.has("run_id") || message.get("run_id").isJsonNull() ||
                            !message.get("run_id").getAsString().equals(currentRunId)) {
                        continue;
                    }

                    // Check if the message content is text type
                    JsonArray contentArray = message.getAsJsonArray("content");
                    if (contentArray == null || contentArray.size() == 0) {
                        continue;
                    }

                    JsonObject firstContent = contentArray.get(0).getAsJsonObject();
                    if (!"text".equals(firstContent.get("type").getAsString())) {
                        continue;
                    }

                    // Extract and parse the response
                    String rawResponse = firstContent.getAsJsonObject("text").get("value").getAsString();

                    // Remove potential JSON string escaping
                    rawResponse = rawResponse.replace("\\n", "\n").replace("\\\"", "\"");

                    LOGGER.info("Parsed raw response: " + rawResponse);

                    return rawResponse;
                }

                // If no suitable message found
                LOGGER.warning("No valid assistant response found in the current batch.");
                TimeUnit.SECONDS.sleep(2);
                retryCount++;
            } catch (Exception e) {
                LOGGER.severe("Exception in fetchAssistantResponse: " + e.getMessage());
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        }

        return "{\"error\": \"No valid assistant response found after multiple attempts.\"}";
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

        try (Response response = client.newCall(request).execute()) {
            String responseString = response.body().string();

            if (!response.isSuccessful()) {
                if (response.code() == 409) {
                    LOGGER.warning("Conflict while submitting function outputs - run may have completed or been cancelled");
                    return false;
                }
                LOGGER.severe("error submitting function outputs: " + responseString);
                return false;
            }

            JsonObject responseBody = JsonParser.parseString(responseString).getAsJsonObject();
            String status = responseBody.get("status").getAsString();

            if ("failed".equals(status) || "cancelled".equals(status) || "expired".equals(status)) {
                LOGGER.severe("Run entered terminal state: " + status);
                return false;
            }

            LOGGER.info("successfully submitted function outputs. Run status: " + status);
            return true;
        } catch (Exception e) {
            LOGGER.severe("Exception while submitting function outputs: " + e.getMessage());
            return false;
        }
    }

    private boolean cancelActiveRun(String threadId, String runId) throws IOException {
        Request request = new Request.Builder()
                .url(OPENAI_THREADS_URL + "/" + threadId + "/runs/" + runId + "/cancel")
                .post(RequestBody.create(MediaType.parse("application/json"), "{}"))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOGGER.severe("Error cancelling run: " + response.body().string());
                return false;
            }
            return true;
        }
    }

    private String getActiveRunId(String threadId) throws IOException {
        String url = OPENAI_THREADS_URL + "/" + threadId + "/runs";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOGGER.severe("Error fetching active runs: " + response.body().string());
                return null;
            }

            String responseBodyString = response.body().string();
            JsonObject responseBody = JsonParser.parseString(responseBodyString).getAsJsonObject();
            JsonArray runs = responseBody.getAsJsonArray("data");

            if (runs == null || runs.size() == 0) {
                return null;
            }

            for (JsonElement runElement : runs) {
                JsonObject run = runElement.getAsJsonObject();
                String status = run.get("status").getAsString();
                // Check for both requires_action and in_progress states
                if ("requires_action".equals(status) || "in_progress".equals(status)) {
                    return run.get("id").getAsString();
                }
            }

            return null;
        }
    }

    public String createThread() throws IOException {
        Request request = new Request.Builder()
                .url(OPENAI_THREADS_URL)
                .post(RequestBody.create(MediaType.parse("application/json"), "{}"))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOGGER.severe("Error creating thread: " + response.body().string());
                return null;
            }

            JsonObject responseBody = JsonParser.parseString(response.body().string()).getAsJsonObject();
            return responseBody.get("id").getAsString();
        }
    }

    private boolean addMessageToThread(String threadId, String userMessage) throws IOException {
        JsonObject messageData = new JsonObject();
        messageData.addProperty("role", "user");

        JsonArray contentArray = new JsonArray();
        JsonObject contentObject = new JsonObject();
        contentObject.addProperty("type", "text");
        contentObject.addProperty("text", userMessage);
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

        try (Response response = client.newCall(request).execute()) {
            String responseBodyString = response.body().string();

            if (!response.isSuccessful()) {
                LOGGER.severe("OpenAI API rejected message: " + responseBodyString);
                return false;
            }

            LOGGER.info("Message successfully added to thread.");
            return true;
        }
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

            String functionResponse = functionService.handleFunctionCall(functionName, arguments);

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

        if (requiredParams != null) {
            params.add("required", JsonParser.parseString(gson.toJson(requiredParams)));
        } else {
            params.add("required", new JsonArray());
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
}