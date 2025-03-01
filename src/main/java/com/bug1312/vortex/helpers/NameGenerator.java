package com.bug1312.vortex.helpers;

import java.util.Random;

public class NameGenerator {
	private static final String[] PREFIXES = {
		"Pol", "Blen", "Dun", "Kil", "Ard", 
		"Bal", "Carn", "Tully", "Inver", "Strath"
	};

	private static final String[] SUFFIXES = {
		"perro", "ham", "nock", "garth", "ness", 
		"keld", "thwaite", "beck", "wick", "shaw"
	};

	private static final String[] MIDDLES = {
		"mid", "end", "ford", "ton", "bridge", "wood"
	};

	private Random random;

	private NameGenerator(long seed) {
		this.random = new Random(seed);
	}

	private String generateName() {
		// Choose a random pattern (0-5)
		String name = switch (random.nextInt(6)) {
			case 0 -> // Prefix + Suffix
				PREFIXES[random.nextInt(PREFIXES.length)] + 
				SUFFIXES[random.nextInt(SUFFIXES.length)];
			
			case 1 -> // Prefix + Middle + Suffix
				PREFIXES[random.nextInt(PREFIXES.length)] + 
				MIDDLES[random.nextInt(MIDDLES.length)] + 
				SUFFIXES[random.nextInt(SUFFIXES.length)];
			
			case 2 -> // Prefix + Prefix
				PREFIXES[random.nextInt(PREFIXES.length)] + 
				PREFIXES[random.nextInt(PREFIXES.length)].toLowerCase();
			
			case 3 -> // Suffix + Suffix
				capitalize(SUFFIXES[random.nextInt(SUFFIXES.length)]) + 
				SUFFIXES[random.nextInt(SUFFIXES.length)];
			
			case 4 -> // Middle + Suffix
				capitalize(MIDDLES[random.nextInt(MIDDLES.length)]) + 
				SUFFIXES[random.nextInt(SUFFIXES.length)];
			
			default -> // Prefix + Middle
				PREFIXES[random.nextInt(PREFIXES.length)] + 
				MIDDLES[random.nextInt(MIDDLES.length)];
		};
		
		return name;
	}

	private String capitalize(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
	}
	
	public static String genName(long seed) {
		return new NameGenerator(seed).generateName();
	}
}
