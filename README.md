Algo trading system that is integrated with Interactive Brokers (IB).

### Main features:
- Users are able to build a strategy in a single class and use the exact same class for historical back testing as well as for live trading.
- Granular level ticks events are all available for users to take action on (note: IB does aggregate and throttle ticks to a max of 4 per second per security) and ticks build Bars, which can range in time from 1 second to 1 day and Bars build Variables (MACD, RSI, BollingerBands, Moving Average, Pivot Points, RSI and more) which can be applied to multplie Bar sizes simultaneously.
- Historical data can be downloaded for back testing and can also be downloaded at runtime to ensure all bars, ticks and variables are fully populated.
- During live trading IB will stream all tick events to the application, if MongoDB is running on the same host there is an option to persis all tick events. This is critical for truly creating an accurate back test since the most granular data provided by IB for historical data is 1 second intervals.
- Dependencies for PMML and H2O models are included.
- Updates can be pushed directly to slack for real time trade information, simply provide your slack webhook.

### Getting up and running

To build a strategy, simply extend the Strategy class (and if running as a standalone app add the main method):

```java
public class ShortMomentumLive extends Strategy {

public static void main(String... args) {
Strategy.initialize(ShortMomentumLive.class);
}
}
```

When setting up the class, users need to decide several thing inside the `run` method:
- Which securities they want to use in the strategy. Multiple securities can be registered but IB has a 50 security limit for their non-commercial accounts. After registereing the security all ticks for each security will be pushed to the `tickReceived` function. The reccomended way to load securities is from a file (see the examples directoy for more information) but they can be registered individually.
- Which bar sizes should be calculated for each of the securities (note: each bar sized registered will be created for each security). All bars that are created will be pushed to the class' `barReceived` function. The bar sizes should be added to a list which is then registered.
- Whether or not the data shoulnd be persisted
- Whehter the current execution is for a back test or for live trading


```java
protected void run() {
// Load the securities. Securities can be loaded from multiple files or from a single file.
List<String> fileNames = new ArrayList<String>();
fileNames.add("/Location/To/Securities");

// Register the bars
BarSize exampleBar1 = BarSize._1_HOUR;
BarSize exampleBar2 = BarSize._30_SECS;
List<BarSize> allBarSizes = new ArrayList<BarSize>();
allBarSizes.add(exampleBar1);
allBarSizes.add(exampleBar1);

registerSecuritiesFromFileBackTest(allBarSizes, fileNames);

// Set the persistance to on
setEntityPersistMode(EntityPersistMode.ALL);

// Set the strategy to run in live mode
// Set to localhost if running locally, the IP address of the cloud instance if not running locally
// The port is usually 4001 but should be checked within the IB application
start("localhost", 4001, 0);
// for back testing:
// startBacktest();
}
```


Thats all there is to it. Ticks will be pushed to the `tickReceived` method and bars are pushed to the `barReceived` method. To create a variable (RSI, MovingAverage, etc.) simply envoke the appropriate class within the `tickReceived` or `barReceived` function. The variables are managed for you by the strategy class which exposes the getVariables method. Because it is crucial to be able to compare historical variables (i.e. the current moving average vs the moving average from 2, 5, N periods ago) the getVariables function allows you to pull the variables from N periods ago based off of a certain point in time.


```java
// This creates or retrieves the most recent moving average for the tick that was creat
protected void tickReceived(Tick tick) {
// The 0 at the end says to get the most recent value
currentMA = getVariables(MovingAverage.createParameters(tick.getSecurity().getKey(), exampleBar1, 9),tick.getDateTime(), 0));
// The 3 at the end says to get the value 3 periods ago
threePeriodOldMA = getVariables(MovingAverage.createParameters(tick.getSecurity().getKey(), exampleBar1, 9),tick.getDateTime(), 0));
}
```

Finally, there are a plethora of functions available within the Strategy class that allow for managing the account and placing trades. These can be invoked within the `tickReceived` or `barReceived` classes:

```java
shortPositionExists(tick)
longPositionExists(bar)
getUnrealizedPercent(tick) // the percentage % in value since the position opened

static int baseShares = 100;
placeMarketOrder(bar, baseShares, OrderAction.BUY);
closePosition(tick)
```

View the examples folder for more in-depth overviews of how to use this.

### Run some examples

To hackishly compile the entire application run the following to build an uber-jar with all of the dependencies included.
```bash
mvn package
```

Under `example-stragies/target` you can run
```bash
java -cp example-strategies-1-SNAPSHOT.jar com.g2m.strategies.examples.CLASS_NAME
# Where CLASS_NAME is the name of the class in the examples directory that you want to run, i.e.:
java -cp example-strategies-1-SNAPSHOT.jar com.g2m.strategies.examples.MACrossoverExample
```

### Example output from running a backtest:

Attributes are automatically collected from every trade including when it was opened, how long it was held, what security it was for, whether it was short or long, the maximum drawdown / increase, etc. These are then aggregated and made avaialable to the user to use. The examples either print to the console or to a file however the Mongo service manager easily allows persisting the trades into their own collection. When opening and closing trades you can specify a reason which will also be included in the analytics, further allowing users to drill down into where their model is performing well or poorly.

Examples from individual trades:
```bash
----------------------------------
Symbol: GBP.AUD
Trade type: SHORT
Win/Loss: UNKNOWN
Price difference: -2.0862981510166465
Quantity: -100000
Realized profit: 208629.81510166466
Trade opened: Fri Nov 27 16:58:05 EST 2015
Trade closed: null
Open price:  2.0862981510166465
Close price: 0.0
Max Drawdown(broke?): 3.599999999996939E-4
Max Drawup(broke?): 0.0037018489833533508
----------------------------------
Symbol: GBP.USD
Trade type: SHORT
Win/Loss: LOSS
Price difference: 6.807139453868238E-4
Quantity: -100000
Realized profit: -68.07139453868238
Trade opened: Sun Nov 22 18:05:04 EST 2015
Trade closed: Sun Nov 22 18:06:02 EST 2015
Open price:  1.5182362282071966
Close price: 1.5189169421525834
Max Drawdown: 4.500000000007276E-5
Max Drawup: 3.887717928034995E-4
----------------------------------
```

Best / Worst trade information:
```bash
------------MAX TRADE PROFIT------------
Symbol: GBP.AUD
Trade type: SHORT
Win/Loss: WIN
Price difference: -0.0026913827737788942
Quantity: -100000
Realized profit: 269.13827737788944
Trade opened: Tue Nov 24 09:33:00 EST 2015
Trade closed: Tue Nov 24 10:17:00 EST 2015
Open price:  2.086443425099732
Close price: 2.083752042325953
Max Drawdown(broke?): 0.004954999999999821
Max Drawup(broke?): 0.0022799999999998377
------------MAX TRADE LOSS------------
Symbol: GBP.AUD
Trade type: SHORT
Win/Loss: LOSS
Price difference: 0.009866585091320879
Quantity: -100000
Realized profit: -986.6585091320878
Trade opened: Wed Nov 25 19:30:00 EST 2015
Trade closed: Wed Nov 25 19:32:00 EST 2015
Open price:  2.0806185116912195
Close price: 2.0904850967825404
Max Drawdown(broke?): 0.0033000000000003027
Max Drawup(broke?): 0.011206488308780571
------------LONGEST TRADE------------
Symbol: EUR.GBP
Trade type: SHORT
Win/Loss: LOSS
Price difference: 0.0011768579822828418
Quantity: -100000
Realized profit: -117.68579822828417
Trade opened: Fri Nov 27 09:37:00 EST 2015
Trade closed: Fri Nov 27 10:45:00 EST 2015
Open price:  0.702793663082677
Close price: 0.7039705210649598
Max Drawdown(broke?): 6.300000000000194E-4
Max Drawup(broke?): 0.0017049999999999566
------------SHORTEST TRADE------------
Symbol: EUR.AUD
Trade type: SHORT
Win/Loss: LOSS
Price difference: 9.696478099179284E-4
Quantity: -100000
Realized profit: -96.96478099179285
Trade opened: Thu Nov 26 15:42:27 EST 2015
Trade closed: Thu Nov 26 15:42:27 EST 2015
Open price:  1.4666870933990102
Close price: 1.467656741208928
Max Drawdown(broke?): 0.0
Max Drawup(broke?): 3.129066009899262E-4
---------------------------------------
```

And finally aggregated analytics from the entire strategy:

```bash
ANALYTICS - Total Trades: 14124
ANALYTICS - Average Trade Time: 00:01:14
ANALYTICS - Win Loss Count: {LOSS=12372, UNKNOWN=5, NOPROFIT=0, WIN=1750}
ANALYTICS - Average WIN: 19.690108377725114
ANALYTICS - Average LOSS: -47.709326884826474
ANALYTICS - WIN : LOSS Profit Ratio: 0.4127098339756157
ANALYTICS - WIN : LOSS Count Ratio: 0.1414484319430973
ANALYTICS - WIN Percentage: 12.0
ANALYTICS - Dollar of Profit Per Dollar of Loss: 0.05837715886334687
ANALYTICS - Total Profit (from Analytics): -555802.102558055
ANALYTICS - Total Comissions (from Analytics): 56508.0
ANALYTICS - Total Profit (from Account):   634070.0761232636
ACCOUNT - Available Funds: 634070.0761232636
```



More to come.
