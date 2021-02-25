package inventoryapp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

public class A7ToMax {

	public static void main(String[] args) {
		try {
			BufferedReader br = new BufferedReader(new FileReader("Maximizer.txt"));
			String fileLine;
			String[] maximizerFields = new String[] {
				"Account No.\t",
				"Address Line 1\t",
				"Address Line 2\t",
				"Alarm Permit\t",
				"City\t",
				"Company/Organization\t",
				"Country\t",
				"Department\t",
				"Deposit\t",
				"Division\t",
				"Email\t",
				"First Name\t",
				"Full Name\t",
				"IDentification\t",
				"Initial\t",
				"Last Name\t",
				"Mr/Ms\t",
				"Notes (Client)\t",
				"Others\t",
				"Permit\t",
				"Phone 1\t",
				"Phone 1 Extension\t",
				"Phone 2\t",
				"Phone 2 Extension\t",
				"Phone 3\t",
				"Phone 3 Extension\t",
				"Phone 4\t",
				"Phone 4 Extension\t",
				"Position\t",
				"Rental\t",
				"Salutation\t",
				"Security\t",
				"State / Province\t",
				"Status\t",
				"Zip / Postal Code"
			};
			ArrayList<String[]> maximizerLines = new ArrayList<String[]>();
			maximizerLines.add(maximizerFields);
			ArrayList<String[]> fileLines = new ArrayList<String[]>();
			while((fileLine = br.readLine()) != null) {
				fileLines.add(fileLine.split("\t"));
				if(fileLines.size() > 1) {
					maximizerLines.add(new String[] {
							(fileLines.get(fileLines.size() - 1)[0] + "\t").replaceAll("\"", ""),
							(fileLines.get(fileLines.size() - 1)[5] + "\t").replaceAll("\"", ""),
							(fileLines.get(fileLines.size() - 1)[6] + "\t").replaceAll("\"", ""),
							"\t",
							(fileLines.get(fileLines.size() - 1)[7] + "\t").replaceAll("\"", ""),
							(getCompOrgName(fileLines.get(fileLines.size() - 1)[3], fileLines.get(fileLines.size() - 1)[4]) + "\t").replaceAll("\"", ""),
							"\t",
							"\t",
							"\t",
							"\t",
							"\t",
							"\t",
							"\t",
							"\t",
							"\t",
							"\t",
							"\t",
							"\t",
							"\t",
							(fileLines.get(fileLines.size() - 1)[21] + "\t").replaceAll("\"", ""),
							(fileLines.get(fileLines.size() - 1)[1] + "\t").replaceAll("\"", ""),
							(fileLines.get(fileLines.size() - 1)[2] + "\t").replaceAll("\"", ""),
							(fileLines.get(fileLines.size() - 1)[15] + "\t").replaceAll("\"", ""),
							(fileLines.get(fileLines.size() - 1)[16] + "\t").replaceAll("\"", ""),
							"\t",
							"\t",
							(fileLines.get(fileLines.size() - 1)[20] + "\t").replaceAll("\"", ""),
							"\t",
							"\t",
							"\t",
							"\t",
							"\t",
							(fileLines.get(fileLines.size() - 1)[8] + "\t").replaceAll("\"", ""),
							"\t",
							(fileLines.get(fileLines.size() - 1)[9]).replaceAll("\"", ""),
					});
				}
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(LocalDate.now() + " - A7ToMax.TXT"));
			for(int i = 0; i < maximizerLines.size(); i++) {
				for(int j = 0; j < 35; j++) {
					bw.write(maximizerLines.get(i)[j]);
				}
				bw.write("\n");
			}
			br.close();
			bw.close();
		}
		catch(IOException e) {
			
		}
	}
	public static String getCompOrgName(String lastName, String firstName) {
		if(lastName.isEmpty() && firstName.isEmpty()) {
			return "";
		}
		else if(firstName.isEmpty()) {
			return lastName;
		}
		else {
			return lastName + ", " + firstName;
		}
	}
}
