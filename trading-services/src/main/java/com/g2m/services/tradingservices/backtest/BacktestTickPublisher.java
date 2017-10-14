package com.g2m.services.tradingservices.backtest;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.SecurityRegistry;
import com.g2m.services.tradingservices.TickDispatcher;
import com.g2m.services.tradingservices.TickPublisher;
import com.g2m.services.tradingservices.TickSubscriber;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.Tick;
import com.g2m.services.tradingservices.entities.Tick.TickBuilder;

/**
 * Added 4/5/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class BacktestTickPublisher implements TickPublisher {
	@Autowired
	private TickDispatcher tickDispatcher;
	private Map<SecurityKey, String> testFilePaths;
	private Set<TickSubscriber> catchAllTickSubscribers;

	public BacktestTickPublisher() {
		testFilePaths = new HashMap<SecurityKey, String>();
		catchAllTickSubscribers = new HashSet<TickSubscriber>();
	}

	@Override
	public void addTickSubscriber(SecurityKey securityKey, TickSubscriber tickSubscriber) {
		tickDispatcher.addTickSubscriber(securityKey, tickSubscriber);
	}

	public void addTestFile(SecurityKey securityKey, String filePath) {
		testFilePaths.put(securityKey, filePath);
	}

	public void addCatchAllTickSubscriber(TickSubscriber tickSubscriber) {
		catchAllTickSubscribers.add(tickSubscriber);
	}

	private void addCatchAllTickSubscribersToDispatcher() {
		Set<SecurityKey> securityKeys = tickDispatcher.getSecurityKeys();
		for (SecurityKey securityKey : securityKeys) {
			for (TickSubscriber tickSubscriber : catchAllTickSubscribers) {
				tickDispatcher.addTickSubscriber(securityKey, tickSubscriber);
			}
		}
	}

	public void startPublishing() {
		addCatchAllTickSubscribersToDispatcher();
		List<TestFile> testFiles = createAndPrepareTestFiles();
		while (true) {
			TestFile earliestTestFile = null;
			for (TestFile testFile : testFiles) {
				if (testFile.isEndOfFile()) {
					continue;
				}
				if (null == earliestTestFile) {
					earliestTestFile = testFile;
				} else if (testFile.getTimestamp() < earliestTestFile.getTimestamp()) {
					earliestTestFile = testFile;
				}
			}
			if (null == earliestTestFile) {
				break;
			}
			tickDispatcher.dispatchTick(earliestTestFile.getTick());
			earliestTestFile.readNextTick();
		}
	}

	private List<TestFile> createAndPrepareTestFiles() {
		List<TestFile> testFiles = new LinkedList<TestFile>();
		for (SecurityKey securityKey : testFilePaths.keySet()) {
			TestFile testFile = new TestFile(testFilePaths.get(securityKey), SecurityRegistry.get(securityKey));
			testFile.readNextTick();
			testFiles.add(testFile);
		}

		return testFiles;
	}

	class TestFile {
		private BufferedReader reader;
		private String filePath;
		private Tick tick;
		private Security security;
		private boolean endOfFile;

		public TestFile(String filePath, Security security) {
			this.filePath = filePath;
			this.security = security;
		}

		public Tick getTick() {
			return tick;
		}

		public long getTimestamp() {
			return tick.getDateTime().getTime();
		}

		public void readNextTick() {
			if (null == reader) {
				try {
					reader = new BufferedReader(new FileReader(filePath));
				} catch (FileNotFoundException e) {
					endOfFile = true;
					return;
				}
			}
			while (true) {
				String line;
				try {
					line = reader.readLine();
				} catch (IOException e) {
					endOfFile = true;
					return;
				}
				if (null == line) {
					endOfFile = true;
					return;
				}
				try {
					tick = createTickFromLine(line);
					break;
				} catch (Exception e) {
					// if there is a problem reading a line then keep reading until either a line is
					// read & parsed successfully or the end of the file is reached
				}
			}
		}

		public boolean isEndOfFile() {
			return endOfFile;
		}

		private Tick createTickFromLine(String line) {
			// dateTime, last, lastDateTime, volBid, volAsk, bid, ask, vol, openIntrest, settlement
			// long, double, long, int, int, double, double, int, int, int
			String[] lineItems = line.split(",");
			TickBuilder tickBuilder = new TickBuilder();
			tickBuilder.setSecurity(security);
			tickBuilder.setDateTime(new Date(Long.parseLong(lineItems[0])));
			tickBuilder.setLastPrice(Double.parseDouble(lineItems[1]));
			tickBuilder.setDateTime(new Date(Long.parseLong(lineItems[2])));
			tickBuilder.setVolumeBid(Integer.parseInt(lineItems[3]));
			tickBuilder.setVolumeAsk(Integer.parseInt(lineItems[4]));
			tickBuilder.setBidPrice(Double.parseDouble(lineItems[5]));
			tickBuilder.setAskPrice(Double.parseDouble(lineItems[6]));
			tickBuilder.setVolume(Integer.parseInt(lineItems[7]));
			tickBuilder.setOpenInterest(Integer.parseInt(lineItems[8]));
			tickBuilder.setSettlement(Double.parseDouble(lineItems[9]));
			return tickBuilder.build();
		}
	}
}
