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
java -cp example-strategies-1-SNAPSHOT.jar com.g2m.strategies.examples.MovingAverage_Example
```

More to come.


