package com.g2m.services.variables.mains;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Michael Borromeo
 */
public class Main2 {
	public static void main(String[] args) {
		for (Integer i : getInts()) {
			System.out.println(i);
		}
	}

	static List<Integer> getInts() {
		System.out.println("Calling getInts()");

		List<Integer> ints = new LinkedList<Integer>();

		for (int i = 0; i < 10; i++) {
			ints.add(i);
		}

		return ints;
	}
}
