package inventoryapp;

/**
 * This is a program that will serve as an inventory management system.
 * - reads barcodes and deducts them from inventory
 * - allows inputting when receiving inventory
 * - prints reports when inventory is low
 * 
 * @author Clayton Jian
 */

import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

@SuppressWarnings("serial")
public class InventoryApp extends JFrame implements MouseListener{
	/**
	 * 
	 */
	JLabel titleLabel, restockingLabel, scannedLabel;
	JButton homeButton, reorderButton, checkOutButton, receiveInventoryButton, doneButton;
	JTextField scannedTextField;
	InventoryApp(){
		/**
		 * This creates the GUI (graphic user interface) for the program
		 * 
		 */
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int restockingNumber = 0;
		
	    setBounds(0,0,screenSize.width, screenSize.height);
	    
	    titleLabel = new JLabel("Inventory Management");
	    titleLabel.setBounds((int)(screenSize.width * 0.25), 0, screenSize.width/2, screenSize.height/10);
	    titleLabel.setFont(new Font("Times New Roman", Font.BOLD, 48));
	    titleLabel.setHorizontalAlignment(JLabel.CENTER);
	    
	    
	    homeButton = new JButton("Home");
		homeButton.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.1), screenSize.width/5, screenSize.height/20);
		homeButton.setFont(new Font("Times New Roman", Font.BOLD, 36));
		homeButton.addMouseListener(this);
		
		reorderButton = new JButton("Reorder List");
		reorderButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.1), screenSize.width/5, screenSize.height/20);
		reorderButton.setFont(new Font("Times New Roman", Font.BOLD, 36));
		
		checkOutButton = new JButton("Check Out");
		checkOutButton.setBounds((int)(screenSize.width * 0.15), (int)(screenSize.height * 0.45), screenSize.width/5, screenSize.height/10);
		checkOutButton.setFont(new Font("Times New Roman", Font.BOLD, 36));
		
		receiveInventoryButton = new JButton("Receive Inventory");
		receiveInventoryButton.setBounds((int)(screenSize.width * 0.65), (int)(screenSize.height * 0.45), screenSize.width/5, screenSize.height/10);
		receiveInventoryButton.setFont(new Font("Times New Roman", Font.BOLD, 36));
		
		doneButton = new JButton("Done");
		doneButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		doneButton.setFont(new Font("Times New Roman", Font.BOLD, 36));
		
		restockingLabel = new JLabel(restockingNumber + " items need restocking");
		restockingLabel.setBounds((int)(screenSize.width * 0.6), (int)(screenSize.height * 0.075), screenSize.width/5, screenSize.height/10);
		restockingLabel.setFont(new Font("Times New Roman", Font.BOLD, 20));
		
		scannedLabel = new JLabel("Scanned:");
		scannedLabel.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.75), screenSize.width/5, screenSize.height/20);
		scannedLabel.setFont(new Font("Times New Roman", Font.BOLD, 36));
		
		scannedTextField = new JTextField("Barcode");
		scannedTextField.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		scannedTextField.setFont(new Font("Times New Roman", Font.BOLD, 36));
		
		// add elements to GUI
		add(titleLabel);
		add(reorderButton);
		add(homeButton);
		add(checkOutButton);
		add(receiveInventoryButton);
		add(restockingLabel);
		add(scannedLabel);
		add(scannedTextField);
		add(doneButton);
		
		// show GUI home page on screen
		scannedLabel.setVisible(false);
		scannedTextField.setVisible(false);
		doneButton.setVisible(false);
		setLayout(null);  
		setVisible(true);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
	public static void main(String[] args) {
		ArrayList<String> inventoryLines = new ArrayList<String>();
		inventoryLines = readFile(System.getenv("INVENTORY_LIST"));
		new InventoryApp();
		String input = "";
		String[] scannedItem;
		Scanner scanner = new Scanner(System.in);
		while(!input.equals("exit")) {
			System.out.println("Enter a barcode:");
			input = scanner.nextLine();
			scannedItem = findItem(inventoryLines, input);
			System.out.println(scannedItem[0] + " \n" + scannedItem[1]);
		}
		scanner.close();
	}
	public static ArrayList<String> readFile(String inventoryFile) {
		/**
		 * This method reads the inventory master list and stores it in an array.
		 * 
		 * The inventory list is a .csv file that is organized with the following schema:
		 * --------------------------------------------------------------
		 * |Barcode|Product Description|Quantity On-Hand|Restock Quantity|
		 * --------------------------------------------------------------
		 * 
		 * @param fileName - the name of the inventory file
		 * @return fileLines - a list of all the inventory items
		 */
		ArrayList<String> fileLines = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(inventoryFile));
			String fileLine;
			while ((fileLine = br.readLine()) != null) {
				fileLines.add(fileLine);
			}
			br.close();
		}
		catch(FileNotFoundException e){
			System.out.println("File not found");
		}
		catch(IOException e) {
			System.out.println("Problem with reading file");
		}
		return(fileLines);
	}
	public static String[] findItem(ArrayList<String> inventoryList, String barcode) {
		/**
		 * This method retrieves the info for the scanned barcode using the same schema as mentioned before
		 * 
		 * @param inventoryList - a list of all the inventory items
		 * @param barcode - the barcode of the scanned item
		 * @return matchedLineEntries - a list of the entries of the matched item
		 */
		String currentLine = "", matchedLine = "";
		String[] matchedLineEntries = new String[4];
		int lineNumber;
		boolean lookingForMatch;
		lineNumber = 0;
		lookingForMatch = true;
		
		while(lookingForMatch){
			currentLine = inventoryList.get(lineNumber);
			if(!barcode.equals("") && barcode.equals(currentLine.substring(0,currentLine.indexOf(",")))){
				lookingForMatch = false;
				matchedLine = currentLine;
				matchedLineEntries = matchedLine.split("\\,");
			}
			else{
				if(++lineNumber == inventoryList.size()){
					lookingForMatch = false;
					System.out.println("No match found");
				}
			}
		}
		return(matchedLineEntries);
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getSource() == homeButton) {
			System.out.println("Home");
		}
		if(e.getSource() == reorderButton) {
			System.out.println("Reorder");
		}
		if(e.getSource() == checkOutButton) {
			System.out.println("Check Out");
		}
		if(e.getSource() == receiveInventoryButton) {
			System.out.println("Receive Inventory");
		}
		if(e.getSource() == doneButton) {
			System.out.println("Done");
		}
	}
	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
}
