# Investly Backend

Investly is our 2 day hackathon project! An AI-powered investment assistant backend built with Spring Boot that integrates with OpenAI's Assistant API and Binance API to provide intelligent trading and portfolio management capabilities.

https://github.com/user-attachments/assets/de449a2f-9ca6-4b29-b2e3-db56baf42d6c

## Features

- **AI Investment Assistant**: Powered by OpenAI's Assistant API for intelligent conversation and trading advice
- **Real-time Trading**: Execute trades on Binance (currently using testnet)
- **Portfolio Management**: Track balances, profit/loss, and trade history
- **WebSocket Support**: Real-time communication for chat functionality
- **Widget Generation**: Dynamic UI components for trading interfaces
- **Multi-currency Support**: Handle both crypto and fiat conversions

## Technology Stack

- **Java 17**
- **Spring Boot 3.3.8**
  - Spring Data JPA
  - Spring Web
  - Spring WebSocket
- **PostgreSQL** - Primary database
- **H2 Database** - For testing
- **LangChain4j** - AI integration
- **Retrofit** - HTTP client for API calls
- **OkHttp** - HTTP client
- **Lombok** - Reduce boilerplate code
- **Maven** - Build tool

## Prerequisites

- Java 17 or higher
- PostgreSQL database
- Maven 3.6+
- OpenAI API key
- Binance API credentials (testnet or production)

## Configuration

Create or update `src/main/resources/application.properties`:

```properties
# Server Configuration
spring.application.name=app
server.port=8086

# Database Configuration
spring.jpa.hibernate.ddl-auto=none
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
spring.datasource.url=jdbc:postgresql://localhost:5432/investly
spring.datasource.username=investly
spring.datasource.password=investly
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.open-in-view=false

# OpenAI Configuration
openai.api.key=YOUR_OPENAI_API_KEY
openai.assistant.id=YOUR_ASSISTANT_ID
openai.timeout.connection=30
openai.timeout.read=30
openai.timeout.write=30

# Binance Configuration
binance.api.key=YOUR_BINANCE_API_KEY
binance.api.secret=YOUR_BINANCE_API_SECRET
binance.api.base-url=https://testnet.binance.vision  # Use https://api.binance.com for production
```

## Database Setup

1. Create a PostgreSQL database named `investly`
2. Run the SQL scripts in order from `src/main/database/`:
   - `0_Create_messages_and_masks.sql`
   - `1_Create_response.sql`
   - `2_Alter_masks.sql`
   - `3_Alter_messages.sql`
   - `4_Alter_response.sql`
   - `5_Alter_messages.sql`
   - `6_Delete_masks.sql`

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd investly-backend
```

2. Build the project:
```bash
./mvnw clean install
```

3. Run the application:
```bash
./mvnw spring-boot:run
```

The application will start on port 8086 by default.

## API Endpoints

### AI Controller
- `POST /ai/process` - Process AI messages

### Message Controller (WebSocket)
- WebSocket endpoint: `/ws`
- Message mapping: `/messages/new`
- Topic subscription: `/topic/messages`

### Core Functionality

#### Trading Operations
- **Place Order**: Execute buy/sell orders with automatic price conversion
- **Cancel Order**: Cancel pending orders
- **Get Balance**: Retrieve portfolio balances with USD values
- **Trade History**: Fetch historical trades
- **Profit/Loss**: Calculate unrealized P&L

#### AI Assistant Features
The AI assistant supports various functions:
- `getBalance` - Retrieve account balances
- `place_order` - Execute trades
- `get_profit_loss` - Check P&L for assets
- `fetch_trade_history` - Get trade history
- `cancel_order` - Cancel orders
- `get_top_movers` - Get top performing cryptocurrencies
- `create_widget` - Generate UI widgets
- `general_investment_advice` - Provide investment guidance

#### Widget Types
- `QUICK_TRADE` - Quick trading interface
- `PORTFOLIO` - Portfolio overview
- `PROFIT_LOSS` - P&L tracking
- `MARKET_OVERVIEW` - Market analysis

## Project Structure

```
src/main/java/com/investly/app/
├── config/
│   ├── GlobalCorsConfig.java    # CORS configuration
│   └── WebSocketConfig.java     # WebSocket configuration
├── controllers/
│   ├── AIController.java        # REST AI endpoints
│   ├── MessageController.java   # WebSocket message handler
│   └── WebSocketController.java # WebSocket controller
├── dao/
│   ├── MessageEntity.java       # Message entity
│   ├── MessageRepository.java   # Message repository
│   ├── ResponseEntity.java      # Response entity
│   └── ResponseRepository.java  # Response repository
├── dto/
│   └── MessageRequest.java      # Message DTO
├── services/
│   ├── AIService.java          # OpenAI integration
│   ├── FunctionService.java    # Function handling
│   ├── MessageService.java     # Message management
│   ├── ResponseService.java    # Response management
│   └── TradeService.java       # Binance trading
└── AppApplication.java         # Main application class
```

## Development

### Running Tests
```bash
./mvnw test
```

### Building for Production
```bash
./mvnw clean package
java -jar target/app-0.0.1-SNAPSHOT.jar
```

## Security Considerations

1. **API Keys**: Store sensitive keys in environment variables or secure configuration
2. **CORS**: Configure appropriate CORS settings for production
3. **Database**: Use strong passwords and secure connections
4. **SSL/TLS**: Enable HTTPS in production
5. **Input Validation**: All user inputs are validated before processing

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is proprietary software. All rights reserved.

## Support

For support and questions, please contact the Investly development team.

## Acknowledgments

- OpenAI for the Assistant API
- Binance for the trading API
- Spring Boot community for the excellent framework
