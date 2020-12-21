package inventoryapp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class PopulateDB {

	public static void main(String[] args) {
		File f = new File("InventoryDB.csv");
		ArrayList<String> fileLines = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String fileLine;
			while ((fileLine = br.readLine()) != null) {
				fileLines.add(fileLine);
			}
			br.close();
		}
		catch(FileNotFoundException e){

		}
		catch(IOException e) {

		}
		try {
			File file = new File("Inventory.txt");
			file.setWritable(true);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			for(int i = 0; i < fileLines.size(); i++) {
				String insert = "";
				String[] parts = fileLines.get(i).split("\\,");
				insert = "INSERT INTO product VALUES" + "('" + parts[0] + "', '" + parts[1] + "', " + parts[2] + ", " + parts[3] + ");";
				bw.write(insert);
				if(i != fileLines.size() - 1) {
					bw.newLine();
				}
			}
			file.setWritable(false);
			bw.close();
		}
		catch(IOException e){

		}
	}

}
