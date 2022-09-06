package ctrmap.scriptformats.gen6;

import xstandard.text.FormattingUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * Game Freak's FNV0 implementation, reverse engineered from Pokémon XY 1.0 by yours truly.
 */
public class GFScrHash {

	public static final int GF_FNV0_PRIME = 131;
	
	public static int getHash(String str) {
		int hash = 0;

		for (int i = 0; i < str.length(); i++) {
			hash = GF_FNV0_PRIME * hash ^ str.charAt(i);
		}

		return hash;
	}

	public static long getHash64(String str) {
		long hash = 0;

		for (int i = 0; i < str.length(); i++) {
			hash = GF_FNV0_PRIME * hash ^ str.charAt(i);
		}

		return hash;
	}
	
	public static int getHash(byte[] arr) {
		int hash = 0;

		for (int i = 0; i < arr.length; i++) {
			hash = GF_FNV0_PRIME * hash ^ arr[i];
		}

		return hash;
	}

	public static void hashesGetConvertBatch(File source) throws IOException {
		Scanner src = new Scanner(source);
		BufferedWriter dst = new BufferedWriter(new FileWriter(source + "_hashed.txt"));

		while (src.hasNextLine()) {
			String name = src.nextLine();
			String hash = FormattingUtils.getFormattedHexString32LE(getHash(name));
			dst.write(name);
			dst.write(": ");
			dst.write(hash);
			dst.newLine();
		}

		dst.close();
	}
}
