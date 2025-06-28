# AssetArc - Indian Stock Market App

A modern Android app for tracking Indian stocks and crypto assets with a beautiful dark theme UI.

## 🚀 Features

### 🔐 Authentication
- **Google Sign-In** - Seamless authentication with Firebase
- **Email/Password** - Traditional login and signup
- **Phone Number** - SMS-based authentication
- **Dark Mode Toggle** - Beautiful radial reveal animation

### 📊 Indian Stock Markets
- **Free Stock Data** - Real-time data from multiple sources
- **NSE & BSE Support** - Major Indian exchanges
- **Top Gainers/Losers** - Daily market movers
- **Market Overview** - Major indices and sector performance
- **Stock Search** - Find any Indian stock by symbol or name

### 💰 Portfolio Management
- **Multi-Asset Support** - Stocks and Cryptocurrencies
- **Real-time Prices** - Live price updates
- **Portfolio Tracking** - Total balance and individual holdings
- **Search & Filter** - Easy portfolio navigation

## 🆓 Free Indian Stock Data Sources

The app integrates multiple free sources for Indian stock data:

### 1. **Yahoo Finance** (Primary)
- **Coverage**: NSE stocks with .NS suffix
- **Data**: Real-time prices, volume, high/low, open/close
- **Limits**: No API key required, generous rate limits
- **Reliability**: High accuracy, widely used

### 2. **Alpha Vantage** (Backup)
- **Coverage**: Global markets including NSE
- **Data**: OHLCV, technical indicators
- **Limits**: Free tier with API key (500 requests/day)
- **Setup**: Get free API key from [alphavantage.co](https://www.alphavantage.co/)

### 3. **NSE India** (Reference)
- **Coverage**: Official NSE data
- **Data**: Delayed quotes, historical data
- **Access**: Public APIs available
- **Use**: Market reference and validation

### 4. **BSE India** (Reference)
- **Coverage**: Official BSE data
- **Data**: Delayed quotes, company info
- **Access**: Public APIs available
- **Use**: Cross-reference with NSE data

## 📱 Popular Indian Stocks Included

The app includes 50+ major Indian stocks:

### **NIFTY 50 Stocks**
- **RELIANCE** - Reliance Industries
- **TCS** - Tata Consultancy Services
- **HDFCBANK** - HDFC Bank
- **INFY** - Infosys
- **ICICIBANK** - ICICI Bank
- **HINDUNILVR** - Hindustan Unilever
- **ITC** - ITC Limited
- **SBIN** - State Bank of India

### **Banking & Finance**
- **KOTAKBANK** - Kotak Mahindra Bank
- **AXISBANK** - Axis Bank
- **BAJFINANCE** - Bajaj Finance
- **BAJAJFINSV** - Bajaj Finserv
- **INDUSINDBK** - IndusInd Bank

### **Technology**
- **HCLTECH** - HCL Technologies
- **WIPRO** - Wipro
- **TECHM** - Tech Mahindra

### **Manufacturing & Auto**
- **MARUTI** - Maruti Suzuki
- **TATAMOTORS** - Tata Motors
- **EICHERMOT** - Eicher Motors
- **HEROMOTOCO** - Hero MotoCorp
- **BAJAJ-AUTO** - Bajaj Auto

### **Pharmaceuticals**
- **SUNPHARMA** - Sun Pharmaceutical
- **DRREDDY** - Dr Reddy's Laboratories
- **CIPLA** - Cipla
- **DIVISLAB** - Divi's Laboratories

## 🛠️ Setup Instructions

### 1. **Clone the Repository**
```bash
git clone https://github.com/yourusername/AssetArc.git
cd AssetArc
```

### 2. **Firebase Setup**
1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com/)
2. Add your Android app (package: `com.example.assetarc`)
3. Download `google-services.json` and place it in the `app/` directory
4. Enable Authentication with Google Sign-In

### 3. **Alpha Vantage API (Optional)**
1. Get a free API key from [alphavantage.co](https://www.alphavantage.co/)
2. Add to `gradle.properties`:
```properties
ALPHA_VANTAGE_API_KEY=your_api_key_here
```

### 4. **Build and Run**
```bash
./gradlew build
./gradlew installDebug
```

## 📊 API Usage Examples

### Fetch Indian Stock Data
```kotlin
val stockService = IndianStockService()

// Get stock price
val stockData = stockService.getStockPrice("RELIANCE", context)

// Search stocks
val results = stockService.searchStocks("TCS")

// Get top gainers
val gainers = stockService.getTopGainers(context)
```

### Yahoo Finance API
```kotlin
// NSE stock with .NS suffix
val url = "https://query1.finance.yahoo.com/v8/finance/chart/RELIANCE.NS"
```

### Alpha Vantage API
```kotlin
val url = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=RELIANCE.NS&apikey=YOUR_API_KEY"
```

## 🎨 UI Features

### Dark Theme
- **Primary**: `#3390EC` (Blue)
- **Background**: `#17212B` (Dark Blue-Gray)
- **Surface**: `#232E3C` (Medium Blue-Gray)
- **Text**: `#FFFFFF` (White)
- **Secondary Text**: `#AEBACB` (Light Gray)

### Animations
- **Radial Reveal** - Dark mode toggle
- **Slide Transitions** - Tab switching
- **Fade Effects** - Loading states
- **Smooth Scrolling** - LazyColumn performance

## 🔧 Customization

### Add More Stocks
Edit `IndianStockService.kt`:
```kotlin
val INDIAN_STOCKS = mapOf(
    "YOURSTOCK" to "Your Stock Name",
    // Add more stocks here
)
```

### Change Data Sources
Modify the `fetchFromYahooFinance()` and `fetchFromAlphaVantage()` methods in `IndianStockService.kt`.

### Custom Themes
Update colors in `ui/theme/Color.kt` and `ui/theme/Theme.kt`.

## 📈 Future Enhancements

- [ ] **Real-time Charts** - Interactive price charts
- [ ] **Technical Indicators** - RSI, MACD, Moving averages
- [ ] **News Integration** - Stock-related news feeds
- [ ] **Watchlists** - Custom stock lists
- [ ] **Alerts** - Price and volume alerts
- [ ] **Portfolio Analytics** - Performance metrics
- [ ] **Export Data** - CSV/PDF reports
- [ ] **Widgets** - Home screen widgets

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Yahoo Finance** - Free stock data API
- **Alpha Vantage** - Financial data provider
- **NSE India** - National Stock Exchange
- **BSE India** - Bombay Stock Exchange
- **Firebase** - Authentication and backend services
- **Jetpack Compose** - Modern UI toolkit

## 📞 Support

For questions or support:
- Create an issue on GitHub
- Email: support@assetarc.com
- Documentation: [docs.assetarc.com](https://docs.assetarc.com)

---

**Note**: This app uses free APIs with rate limits. For production use, consider upgrading to paid plans for higher limits and better reliability.

