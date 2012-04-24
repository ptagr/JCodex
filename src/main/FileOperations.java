package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileOperations {
	public static void writeToFile(String file, String textToSave,
			boolean append) {
		// File f = new File(file);
		// if(f.exists() && !append)
		// f.delete();
		try {
			BufferedWriter out = new BufferedWriter(
					new FileWriter(file, append));
			out.write(textToSave);
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void writeToFile(String file, byte[] bytestosave,
			boolean append) {
		// File f = new File(file);
		// if(f.exists() && !append)
		// f.delete();
		try {
			FileOutputStream out = new FileOutputStream(file, append);
			out.write(bytestosave);
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static List<String> readFromFile(String file) {
		List<String> readStr = new ArrayList<String>();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
			String temp;
			while ((temp = br.readLine()) != null) {
				readStr.add(temp);
			}
			return readStr;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}
	
	public static byte[] readBytesFromFile(String file) {
		File f = new File(file);
		byte[] readBytes = new byte[(int) f.length()];
		try {
			FileInputStream fis = new FileInputStream(file);
			fis.read(readBytes);
			fis.close();
			return readBytes;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}
	
	
	public static String readStringFromFile(String file) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
			return br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}
	
	

}
