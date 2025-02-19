ALTER TABLE public.masks RENAME COLUMN "label" TO description;
ALTER TABLE public.masks ADD function_name varchar NULL;

TRUNCATE TABLE masks RESTART IDENTITY CASCADE;


INSERT INTO masks (name, function_name, description) VALUES
( 'Fetch Balance', 'fetch_balance', 'Retrieve the user Binance account balance.' ),
( 'Place Order', 'place_order', 'Execute a trade on Binance (BUY/SELL crypto).' ),
( 'Get Profit/Loss', 'get_profit_loss', 'Retrieve the user unrealized profit/loss for a specific asset.' ),
( 'Fetch Trade History', 'fetch_trade_history', 'Retrieve past executed trades from Binance.' ),
( 'Cancel Order', 'cancel_order', 'Cancel an open Binance order.' ),
( 'Get Top Movers', 'fetch_top_movers', 'Retrieve top market gainers and losers for today.' ),
( 'Create Widget', 'create_widget', 'Generate an investment-related widget (profit/loss tracker, quick trade, etc.).' ),
( 'No Mask', null, 'No user mask');


