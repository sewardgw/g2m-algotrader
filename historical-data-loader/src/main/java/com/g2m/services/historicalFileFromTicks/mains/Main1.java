package com.g2m.services.historicalFileFromTicks.mains;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import com.g2m.services.historicalFileFromTicks.TickFormatter;

public class Main1 {

	static HashMap <String, ArrayList<String>> tickQueue = new HashMap<String, ArrayList<String>>();
	static boolean inputHeader = false;
	static String inputFile = "/Users/grantseward/HistoricalDataLoader/HistoricalData/AllData/dec2015Jan2016";
	static String outputFileLocation = "/Users/grantseward/HistoricalDataLoader/HistoricalData/AllData/OtherFiles/";
	static long currTime;
	static HashMap <String, Boolean> headerWritten = new HashMap<String, Boolean>();
	static String header = "dateTime, lastPrice, lastDateTime, volBid, volAsk, bidPrice, askPrice, volPrice, openIntrest, settlement" + System.lineSeparator();
	static String noTradeSequence = "0,0,0,0,0,0";

	public static void main(String[] args) {
		currTime = System.currentTimeMillis();
		File file = loadFile();
		readFileAndWriteOutput(file);
		clearQueues();
	}

	private static void clearQueues() {
		for (String key : tickQueue.keySet()){
			writeTicksToFile(tickQueue.get(key),key);
			tickQueue.get(key).clear();
		}
	}

	
	// This is the main function, we specify an input file where all of the tick data
	// is located. This input file can have multiple securities within it. This function
	// will call the other functions as needed in order to parse the file into
	// multiple files, one for each security, that can be fed into the back testing
	// system.
	private static void readFileAndWriteOutput(File file) {

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line;

			while((line = reader.readLine()) != null){
				if(inputHeader){
					TickFormatter tf = new TickFormatter(line);
					addTicksToWriteQueue(tf);
				} else {
					inputHeader = true;
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	// Instead of writing directly to the file each time a tick is formatted from the 
	// input file, we had the tick to a queue so that batches of records can be written
	// at one time once the queue is full. There is one queue for each security.
	private static void addTicksToWriteQueue(TickFormatter tf) {

		if (!tickQueue.containsKey(tf.getTickKey())){
			ArrayList<String> newList = new ArrayList<String>();
			tickQueue.put(tf.getTickKey(), newList);
		}
		else {
			//if(!tf.getFormattedTick().contains(noTradeSequence));
				tickQueue.get(tf.getTickKey()).add(tf.getFormattedTick());
		}
		checkTickQueueAndWriteToFile(tf);
	}

	// This function checks to see if the queue is full and then sends the ticks from the 
	// queue to be written to the file. 
	private static void checkTickQueueAndWriteToFile(TickFormatter tf) {

		if(tickQueue.get(tf.getTickKey()).size() >= 10000){
			writeTicksToFile(tickQueue.get(tf.getTickKey()), tf.getTickKey());
			tickQueue.get(tf.getTickKey()).clear();
		}
	}

	// This function writes ticks to a file  
	private static void writeTicksToFile(ArrayList<String> strings, String fileName) {
		
		boolean writeHeader = true;
		if (!headerWritten.containsKey(fileName))
			headerWritten.put(fileName, false);
		else 
			writeHeader = headerWritten.get(fileName);
		
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputFileLocation
						+ fileName
						+ "-"
						+ "TICK"
						+ "-"
						+ String.valueOf(currTime), true),"utf-8"))) {
			if (writeHeader)
				writer.append(header);
			
			for (String s : strings){
				writer.append(s);
			}

			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// This function loads the specified input file which holds all of the 
	// original unformatted tick data
	private static File loadFile() {

		File file = null;
		try {
			URL url = new File(inputFile).toURI().toURL();
			file = new File(url.toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return file;
	}

	public Main1() {
	
	}

}
