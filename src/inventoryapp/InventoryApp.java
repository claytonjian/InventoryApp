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
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

@SuppressWarnings("serial")
public class InventoryApp extends JFrame implements MouseListener{
	
	static InventoryApp program;
	
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	
	// stores the lines of the Inventory.csv file
	ArrayList<String> inventoryLines = new ArrayList<String>();
	
	// stores the order of how to display scanned items
	ArrayList<String> itemsOrder = new ArrayList<String>();
	
	// stores the sequence of scanned items in a stack
	Deque<String> itemsStack = new ArrayDeque<>();
	
	/* stores the barcode and name of scanned items
	 * 
	 * -----------
	 * |Item|Name|
	 * -----------
	 */
	
	HashMap<String, String> namesList = new HashMap<String, String>();
	
	/*	stores the items currently scanned + quantity of scanned item
	*	---------------
	*	|Item|Quantity|
	*	---------------
	*/
	HashMap<String, Integer> quantityList = new HashMap<String, Integer>();
	
	
	// GUI elements
	JLabel titleLabel, restockingLabel, barcodeLabel;
	JTextArea itemsTextArea;
	JButton homeButton, reorderButton, checkOutButton, receiveButton, newItemButton, doneCheckOutButton, doneReceivingButton, printButton, undoButton, editItemButton;
	JTextField barcodeTextField;
	JScrollPane itemsScrollPane;
	JPanel newItemPanel, editItemPanel;
	
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	
	int restockingNumber;
	
	InventoryApp(){
		
		/**
		 * This initializes the program (reads inventory, creates GUI, etc)
		 * 
		 */
		
		inventoryLines = readFile("Inventory.csv");
		
		setIconImage(Toolkit.getDefaultToolkit().getImage("Logo.jpg"));
		
	    setTitle("Inventory Management Program");
		setBounds(0,0,screenSize.width, screenSize.height);
			    
	    titleLabel = new JLabel("Inventory Management");
	    titleLabel.setBounds((int)(screenSize.width * 0.35), 0, screenSize.width/2, screenSize.height/10);
	    titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
	    
	    homeButton = new JButton("Home");
		homeButton.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.1), screenSize.width/5, screenSize.height/20);
		homeButton.setFont(new Font("Arial", Font.BOLD, 36));
		homeButton.addMouseListener(this);
		
		reorderButton = new JButton("Reorder List");
		reorderButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.1), screenSize.width/5, screenSize.height/20);
		reorderButton.setFont(new Font("Arial", Font.BOLD, 36));
		reorderButton.addMouseListener(this);
						
		itemsTextArea = new JTextArea("Quantity\tIn Stock\tItem\n");
	    itemsTextArea.setFont(new Font("Arial", Font.BOLD, 28));
	    itemsTextArea.setEditable(false);
	    
	    itemsScrollPane = new JScrollPane(itemsTextArea);
	    itemsScrollPane.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.175), (int)(screenSize.width * 0.9), (int)(screenSize.height * 0.575));
	    itemsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
	    
		checkOutButton = new JButton("Check Out(-)");
		checkOutButton.setBounds((int)(screenSize.width * 0.15), (int)(screenSize.height * 0.45), screenSize.width/5, screenSize.height/10);
		checkOutButton.setFont(new Font("Arial", Font.BOLD, 36));
		checkOutButton.addMouseListener(this);
		
		receiveButton = new JButton("Receive(+)");
		receiveButton.setBounds((int)(screenSize.width * 0.65), (int)(screenSize.height * 0.45), screenSize.width/5, screenSize.height/10);
		receiveButton.setFont(new Font("Arial", Font.BOLD, 36));
		receiveButton.addMouseListener(this);
		
		editItemButton = new JButton("Edit Item");
		editItemButton.setBounds((int)(screenSize.width * 0.4), (int)(screenSize.height * 0.65), screenSize.width/5, screenSize.height/10);
		editItemButton.setFont(new Font("Arial", Font.BOLD, 36));
		editItemButton.addMouseListener(this);
		
		doneCheckOutButton = new JButton("Done");
		doneCheckOutButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		doneCheckOutButton.setFont(new Font("Arial", Font.BOLD, 36));
		doneCheckOutButton.addMouseListener(this);
		
		doneReceivingButton = new JButton("Done");
		doneReceivingButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		doneReceivingButton.setFont(new Font("Arial", Font.BOLD, 36));
		doneReceivingButton.addMouseListener(this);
		
		newItemButton = new JButton("New Item");
		newItemButton.setBounds((int)(screenSize.width * 0.3), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		newItemButton.setFont(new Font("Arial", Font.BOLD, 36));
		newItemButton.addMouseListener(this);
		
		printButton = new JButton("Print");
		printButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		printButton.setFont(new Font("Arial", Font.BOLD, 36));
		printButton.addMouseListener(this);
		
		undoButton = new JButton("Undo");
		undoButton.setBounds((int)(screenSize.width * 0.525), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		undoButton.setFont(new Font("Arial", Font.BOLD, 36));
		undoButton.addMouseListener(this);
		
		restockingLabel = new JLabel(restockingNumber + " item(s) needs restocking");
		restockingLabel.setBounds((int)(screenSize.width * 0.5), (int)(screenSize.height * 0.075), screenSize.width/5, screenSize.height/10);
		restockingLabel.setFont(new Font("Arial", Font.BOLD, 20));
		updateRestockingNumber();
		
		barcodeLabel = new JLabel("Barcode:");
		barcodeLabel.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.75), screenSize.width/5, screenSize.height/20);
		barcodeLabel.setFont(new Font("Arial", Font.BOLD, 28));
		
		barcodeTextField = new JTextField();
		barcodeTextField.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		barcodeTextField.setFont(new Font("Arial", Font.BOLD, 48));
		barcodeTextField.addActionListener(barcodeScanned);
		
		// add elements to GUI
		add(titleLabel);
		add(reorderButton);
		add(homeButton);
		add(checkOutButton);
		add(receiveButton);
		add(editItemButton);
		add(restockingLabel);
		add(barcodeLabel);
		add(barcodeTextField);
		add(doneCheckOutButton);
		add(doneReceivingButton);
		add(printButton);
		add(newItemButton);
		add(undoButton);
		add(itemsScrollPane);
		
		// show GUI home page on screen
		barcodeLabel.setVisible(false);
		barcodeTextField.setVisible(false);
		doneCheckOutButton.setVisible(false);
		doneReceivingButton.setVisible(false);
		printButton.setVisible(false);
		newItemButton.setVisible(false);
		undoButton.setVisible(false);
		itemsTextArea.setVisible(false);
		itemsScrollPane.setVisible(false);
		setLayout(null);
		setVisible(true);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
	public static void main(String[] args) {
		program = new InventoryApp();
	}
	public static ArrayList<String> readFile(String inventoryFile) {
		
		/**
		 * This method reads the inventory master list and stores it in an array.
		 * 
		 * The inventory list is a .csv file that is organized with the following schema:
		 * --------------------------------------------------------------
		 * |Barcode|Product Description|In-stock Quantity|Restock Quantity|
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
			JOptionPane.showMessageDialog(program, "Could not find the inventory file. Please include a \"Inventory.csv\" file in the Inventory App folder.", "File not found", JOptionPane.WARNING_MESSAGE);
		}
		catch(IOException e) {
			JOptionPane.showMessageDialog(program, "Could not read the inventory file.", "Problem reading file", JOptionPane.WARNING_MESSAGE);
		}
		return(fileLines);
	}
	public void updateInventoryFile() {
		
		/**
		 * This method creates a new Inventory.csv file based on the current data
		 * 
		 */
		
		try {
			File file = new File("Inventory.csv");
			file.setWritable(true);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			for(int i = 0; i < inventoryLines.size(); i++) {
				bw.write(inventoryLines.get(i));
				if(i != inventoryLines.size() - 1) {
					bw.newLine();
				}
			}
			file.setWritable(false);
			bw.close();
		}
		catch(IOException e){
			JOptionPane.showMessageDialog(program, "Could not write to the inventory file.", "Problem writing to file", JOptionPane.WARNING_MESSAGE);
		}
	}
	public void createNewItem() {
		
		/**
		 * This method is for adding new items into the Inventory.csv file
		 * 
		 */
		
		JTextField barcode = new JTextField(20);
		barcode.setFont(new Font("Arial", Font.BOLD, 28));
		JTextField name = new JTextField(20);
		name.setFont(new Font("Arial", Font.BOLD, 28));
		SpinnerModel inStockQuantity = new SpinnerNumberModel(0, 0, 1000, 1);
		SpinnerModel restockQuantity = new SpinnerNumberModel(0, 0, 1000, 1);
		
		JSpinner inStock = new JSpinner(inStockQuantity);
		inStock.setFont(new Font("Arial", Font.BOLD, 28));
		JComponent inStockEditor = inStock.getEditor();
		if(inStockEditor instanceof JSpinner.DefaultEditor) {
			JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor)inStockEditor;
			spinnerEditor.getTextField().setHorizontalAlignment(JTextField.LEFT);
		}
		
		JSpinner restock = new JSpinner(restockQuantity);
		restock.setFont(new Font("Arial", Font.BOLD, 28));
		JComponent restockEditor = restock.getEditor();
		if(restockEditor instanceof JSpinner.DefaultEditor) {
			JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor)restockEditor;
			spinnerEditor.getTextField().setHorizontalAlignment(JTextField.LEFT);
		}
		
		
		JPanel newItemPanel = new JPanel();
		newItemPanel.setLayout(new GridLayout(4,2));
		JLabel bcLabel = new JLabel("Barcode");
		bcLabel.setFont(new Font("Arial", Font.BOLD, 28));
		newItemPanel.add(bcLabel);
		newItemPanel.add(barcode);
		JLabel nameLabel = new JLabel("Name");
		nameLabel.setFont(new Font("Arial", Font.BOLD, 28));
		newItemPanel.add(nameLabel);
		newItemPanel.add(name);
		JLabel inStockLabel = new JLabel("In Stock Quantity");
		inStockLabel.setFont(new Font("Arial", Font.BOLD, 28));
		newItemPanel.add(inStockLabel);
		newItemPanel.add(inStock);
		JLabel restockLabel = new JLabel("Restock Quantity");
		restockLabel.setFont(new Font("Arial", Font.BOLD, 28));
		newItemPanel.add(restockLabel);
		newItemPanel.add(restock);
		
		int result;
		boolean validNewItem = false, cancel = false;
		while(!validNewItem && !cancel) {
			result = JOptionPane.showConfirmDialog(program, newItemPanel, 
					"Please enter the item description:", JOptionPane.OK_CANCEL_OPTION);
			if(result == JOptionPane.OK_OPTION && !barcode.getText().equals("") && !name.getText().equals("")) {
				if(findItem(barcode.getText())[0] != null) {
					JOptionPane.showMessageDialog(program, "The barcode you entered already exists for another item. Please enter another one.", "Duplicate barcode", JOptionPane.WARNING_MESSAGE);
				}
				else {
					String newInventoryLine = barcode.getText() + "," + name.getText() + "," + inStock.getValue() + "," + restock.getValue();
					inventoryLines.add(newInventoryLine);
					updateInventoryFile();
					updateRestockingNumber();
					updateLog("new", Integer.parseInt(inStock.getValue().toString()), name.getText());
					JOptionPane.showMessageDialog(program, "Successfully added item!", "Item creation complete", JOptionPane.PLAIN_MESSAGE);
					barcodeTextField.requestFocusInWindow();
					validNewItem = true;
				}
			}
			else {
				if(result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
					cancel = true;
				}
			}
		}
	}
	public void editItem() {
		
		/**
		 * This method is for editing the attributes of items found in the Inventory.csv file
		 * 
		 */
		
		String barcodeInput = "";
		String[] barcodeItem = new String[4];
		
		boolean validItem = false, cancel = false;
		while(!validItem && !cancel) {
			barcodeInput = JOptionPane.showInputDialog(program, "Please enter the barcode of the item:", "Edit Item", JOptionPane.INFORMATION_MESSAGE);
			if(barcodeInput != null) {
	        	barcodeItem = findItem(barcodeInput);
			}
			if(barcodeInput == null) {
				cancel = true;
			}
			else if(barcodeItem[0] == null) {
				JOptionPane.showMessageDialog(program, "Could not find the desired item. It is either not in the system or an invalid barcode.", "No match found", JOptionPane.WARNING_MESSAGE);
			}
			else {
				validItem = true;
				
				JTextField name = new JTextField(20);
				name.setText(barcodeItem[1]);
				name.setFont(new Font("Arial", Font.BOLD, 28));
				SpinnerModel inStockQuantity = new SpinnerNumberModel(Integer.parseInt(barcodeItem[2]), 0, 1000, 1);
				SpinnerModel restockQuantity = new SpinnerNumberModel(Integer.parseInt(barcodeItem[3]), 0, 1000, 1);
				
				JSpinner inStock = new JSpinner(inStockQuantity);
				inStock.setFont(new Font("Arial", Font.BOLD, 28));
				JComponent inStockEditor = inStock.getEditor();
				if(inStockEditor instanceof JSpinner.DefaultEditor) {
					JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor)inStockEditor;
					spinnerEditor.getTextField().setHorizontalAlignment(JTextField.LEFT);
				}
				
				JSpinner restock = new JSpinner(restockQuantity);
				restock.setFont(new Font("Arial", Font.BOLD, 28));
				JComponent restockEditor = restock.getEditor();
				if(restockEditor instanceof JSpinner.DefaultEditor) {
					JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor)restockEditor;
					spinnerEditor.getTextField().setHorizontalAlignment(JTextField.LEFT);
				}
				
				editItemPanel = new JPanel();
				editItemPanel.setLayout(new GridLayout(3,2));
				JLabel nameLabel = new JLabel("Name");
				nameLabel.setFont(new Font("Arial", Font.BOLD, 28));
				editItemPanel.add(nameLabel);
				editItemPanel.add(name);
				JLabel inStockLabel = new JLabel("In Stock Quantity");
				inStockLabel.setFont(new Font("Arial", Font.BOLD, 28));
				editItemPanel.add(inStockLabel);
				editItemPanel.add(inStock);
				JLabel restockLabel = new JLabel("Restock Quantity");
				restockLabel.setFont(new Font("Arial", Font.BOLD, 28));
				editItemPanel.add(restockLabel);
				editItemPanel.add(restock);
				
				int result;
				cancel = false;
				while(!cancel) {
					result = JOptionPane.showConfirmDialog(program, editItemPanel, 
							"Please enter the item description:", JOptionPane.OK_CANCEL_OPTION);
					
					if(result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
						int choice = JOptionPane.showConfirmDialog(	program, 
								"All progress will be lost. Are you sure you want to exit?" ,
								"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						if(choice == 0) {
							cancel = true;
						}
					}
					else {
						String editedItem = barcodeItem[0] + "," +  name.getText() + "," + inStock.getValue() + "," + restock.getValue();
						updateLine(editedItem, barcodeItem[0].length());
						updateInventoryFile();
						updateRestockingNumber();
						updateLog("new", Integer.parseInt(inStock.getValue().toString()), name.getText());
						JOptionPane.showMessageDialog(program, "Successfully edited item!", "Item edit complete", JOptionPane.PLAIN_MESSAGE);
						cancel = true;
					}
				}
			}
		}
	}
	public String[] findItem(String barcode) {
		
		/**
		 * This method retrieves the info for the barcode using the same schema as mentioned before
		 * 
		 * @param barcode - the barcode of the barcode item
		 * @return matchedLineEntries - a list of the entries of the matched item
		 */
		
		String currentLine = "", matchedLine = "";
		String[] matchedLineEntries = new String[4];
		int lineNumber;
		boolean lookingForMatch = true;
		lineNumber = 0;
		
		while(lookingForMatch){
			currentLine = inventoryLines.get(lineNumber);
			if(!barcode.equals("") && barcode.length() <= currentLine.length() && barcode.equals(currentLine.substring(0,currentLine.indexOf(",")))){
				lookingForMatch = false;
				matchedLine = currentLine;
				matchedLineEntries = matchedLine.split("\\,");
			}
			else{
				if(++lineNumber == inventoryLines.size()){
					lookingForMatch = false;
				}
			}
		}
		return(matchedLineEntries);
	}
	public void calculateInventory(String checkOutOrReceive) {
		
		/**
		 * This method adds or subtracts inventory based on what the user is doing
		 * 
		 * @param checkOutOrReceive - this is a string that says either "add" or "subtract"
		 */
		
		int selectedQuantity = 0;
		String[] replacedLine;
		String replacedLineString;
		for(int i = 0; i < quantityList.size(); i++) {
			selectedQuantity = quantityList.get(itemsOrder.get(i));
			replacedLine = findItem(itemsOrder.get(i));
			if(checkOutOrReceive.equals("checkOut")) {
				replacedLineString = replacedLine[0] + "," + replacedLine[1] + "," + (Integer.parseInt(replacedLine[2]) - selectedQuantity) + "," + replacedLine[3];
				updateLog("sub", selectedQuantity, replacedLine[1]);
			}
			else {
				replacedLineString = replacedLine[0] + "," + replacedLine[1] + "," + (Integer.parseInt(replacedLine[2]) + selectedQuantity) + "," + replacedLine[3];
				updateLog("add", selectedQuantity, replacedLine[1]);
			}
			updateLine(replacedLineString, replacedLine[0].length());
		}
		updateInventoryFile();
		updateRestockingNumber();
		if(namesList.size() != 0) {
			JOptionPane.showMessageDialog(program, "Successfully updated items!", "Update complete", JOptionPane.PLAIN_MESSAGE);
			barcodeTextField.requestFocusInWindow();
		}
			
		itemsTextArea.setText("Quantity\tIn Stock\tItem\n");
		itemsOrder.clear();
		namesList.clear();
		quantityList.clear();
		itemsStack.clear();
		undoButton.setVisible(false);
	}
	public void updateLog(String operator, int quantity, String item) {
		
		/**
		 * This method creates a log file for troubleshooting bugs
		 * 
		 * @param operator - describes what action is taking place
		 * @param quantity - how much of an item is changing
		 * @param item - what item is being changed
		 */
		
		try {
			File file = new File("Log.txt");
 			file.setWritable(true);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
			if(operator.equals("add")) {
				bw.write(dtf.format(LocalDateTime.now()) + "\tAdded " + quantity + " " + item + "\n");
			}
			else if(operator.equals("sub")) {
				bw.write(dtf.format(LocalDateTime.now()) + "\tSubtracted " + quantity + " " + item + "\n");
			}
			else {
				bw.write(dtf.format(LocalDateTime.now()) + "\tIntroduced/Changed " + item + "\n");
			}
			file.setWritable(false);
			bw.close();
		}
		catch(IOException e){
			JOptionPane.showMessageDialog(program, "Could not write to the log file.", "Problem writing to file", JOptionPane.WARNING_MESSAGE);
		}
	}
	public void updateLine(String newLine, int barcodeLength) {
		
		/**
		 * This method rewrites a line in the Inventory.csv file
		 * 
		 * @param newLine - the line that will replace the old line
		 * @param barcodeLength - the barcode of the item that is meant to be changed
		 */
		
		String currentLine = "";
		int lineNumber;
		boolean lookingForMatch = true;
		lineNumber = 0;
		
		while(lookingForMatch){
			currentLine = inventoryLines.get(lineNumber);
			if((newLine.substring(0, barcodeLength)).length() <= currentLine.length() && newLine.substring(0, barcodeLength).equals(currentLine.substring(0, barcodeLength))){
				lookingForMatch = false;
				inventoryLines.set(lineNumber, newLine);
			}
			else{
				if(++lineNumber == inventoryLines.size()){
					lookingForMatch = false;
				}
			}
		}
	}
	public void updateRestockingNumber() {
		
		/**
		 * This method updates the restocking number at the top of the GUI
		 */
		
		restockingNumber = 0;
		String currentItemLine = "";
		String[] currentItemEntries = new String[4];
 		for(int i = 0; i < inventoryLines.size(); i++) {
			currentItemLine = inventoryLines.get(i);
			currentItemEntries = currentItemLine.split("\\,");
			try {
				if(currentItemEntries.length > 2 && currentItemEntries[2] != null && currentItemEntries[3] != null && Integer.parseInt(currentItemEntries[2]) < Integer.parseInt(currentItemEntries[3])) {
					restockingNumber++;
				}
			}
			catch(NumberFormatException e){
				
			}
		}
 		if(restockingNumber != 0) {
 			restockingLabel.setText(restockingNumber + " item(s) needs restocking");
 			restockingLabel.setForeground(Color.RED);
 		}
 		else {
 			restockingLabel.setText(restockingNumber + " item(s) needs restocking");
 			restockingLabel.setForeground(Color.BLACK);
 		}
	}
	public void updateRestockingItems() {
		
		/**
		 * This method updates the list of items that needs to be restocked
		 */
		
		String itemsFormatted = "Restock Inventory List: " + dtf.format(LocalDateTime.now()) + "\n\nNeed\tItem\n";
		String currentItemLine = "";
		String[] currentItemEntries = new String[4];
 		for(int i = 0; i < inventoryLines.size(); i++) {
			currentItemLine = inventoryLines.get(i);
			currentItemEntries = currentItemLine.split("\\,");
			try {
				if(currentItemEntries.length > 2 && currentItemEntries[2] != null && currentItemEntries[3] != null && Integer.parseInt(currentItemEntries[2]) < Integer.parseInt(currentItemEntries[3])) {
					itemsFormatted += (Integer.parseInt(currentItemEntries[3]) - Integer.parseInt(currentItemEntries[2])) + "\t" + currentItemEntries[1] + "\n";
				}
			}
			catch(NumberFormatException e){
				
			}
		}
 		try {
 			File file = new File("Restock.csv");
 			file.setWritable(true);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(itemsFormatted.replaceAll("\\t", ",,"));
			file.setWritable(false);
			bw.close();
		}
		catch(IOException e){
			JOptionPane.showMessageDialog(program, "Could not write to the restocking file.", "Problem writing to file", JOptionPane.WARNING_MESSAGE);
		}
 		itemsTextArea.setText(itemsFormatted);
	}
	public String updateItems() {
		
		/**
		 * This method updates the items that appear on the screen
		 * 
		 * @return itemsFormatted - a string containing the list of items
		 */
		
		String itemsFormatted = "Quantity\tIn Stock\tItem\n";
		for(int i = 0; i < itemsOrder.size(); i++) {
			itemsFormatted += (quantityList.get(itemsOrder.get(i)) + "\t" + findItem(itemsOrder.get(i))[2] + "\t" + namesList.get(itemsOrder.get(i)) + "\n");
		}
		return itemsFormatted;
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		
		/**
		 * This method takes care of the different button press actions
		 */
		
		if(e.getSource() == homeButton) {
			if(!namesList.isEmpty()) {
				int choice = JOptionPane.showConfirmDialog(	program, 
												"You are not finished entering in your items. All progress will be lost. Are you sure you want to exit?" ,
												"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(choice == 0) {
					itemsOrder.clear();
					namesList.clear();
					quantityList.clear();
					itemsStack.clear();
					checkOutButton.setVisible(true);
					receiveButton.setVisible(true);
					editItemButton.setVisible(true);
					barcodeLabel.setVisible(false);
					barcodeTextField.setVisible(false);
					newItemButton.setVisible(false);
					doneCheckOutButton.setVisible(false);
					doneReceivingButton.setVisible(false);
					printButton.setVisible(false);
					undoButton.setVisible(false);
					itemsTextArea.setVisible(false);
					itemsScrollPane.setVisible(false);
				}
				barcodeTextField.requestFocusInWindow();
			}
			else {
				checkOutButton.setVisible(true);
				receiveButton.setVisible(true);
				editItemButton.setVisible(true);
				barcodeLabel.setVisible(false);
				barcodeTextField.setVisible(false);
				newItemButton.setVisible(false);
				doneCheckOutButton.setVisible(false);
				doneReceivingButton.setVisible(false);
				printButton.setVisible(false);
				itemsTextArea.setVisible(false);
				itemsScrollPane.setVisible(false);
			}
		}
		if(e.getSource() == reorderButton) {
			if(!namesList.isEmpty()) {
				int choice = JOptionPane.showConfirmDialog(	program, 
												"You are not finished entering in your items. All progress will be lost. Are you sure you want to exit?" ,
												"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(choice == 0) {
					itemsOrder.clear();
					namesList.clear();
					quantityList.clear();
					itemsStack.clear();
					checkOutButton.setVisible(false);
					receiveButton.setVisible(false);
					editItemButton.setVisible(false);
					barcodeLabel.setVisible(false);
					barcodeTextField.setVisible(false);
					newItemButton.setVisible(false);
					doneCheckOutButton.setVisible(false);
					doneReceivingButton.setVisible(false);
					printButton.setVisible(true);
					undoButton.setVisible(false);
					itemsTextArea.setVisible(true);
					itemsScrollPane.setVisible(true);
					updateRestockingItems();
				}
			}
			else {
				checkOutButton.setVisible(false);
				receiveButton.setVisible(false);
				editItemButton.setVisible(false);
				barcodeLabel.setVisible(false);
				barcodeTextField.setVisible(false);
				newItemButton.setVisible(false);
				doneCheckOutButton.setVisible(false);
				doneReceivingButton.setVisible(false);
				printButton.setVisible(true);
				itemsTextArea.setVisible(true);
				itemsScrollPane.setVisible(true);
				updateRestockingItems();
			}
		}
		if(e.getSource() == checkOutButton) {
			itemsTextArea.setText("Quantity\tIn Stock\tItem\n");
			checkOutButton.setVisible(false);
			receiveButton.setVisible(false);
			editItemButton.setVisible(false);
			barcodeLabel.setVisible(true);
			barcodeTextField.setVisible(true);
			doneCheckOutButton.setVisible(true);
			doneReceivingButton.setVisible(false);
			itemsTextArea.setVisible(true);
			itemsScrollPane.setVisible(true);
			barcodeTextField.requestFocusInWindow();
		}
		if(e.getSource() == receiveButton) {
			itemsTextArea.setText("Quantity\tIn Stock\tItem\n");
			checkOutButton.setVisible(false);
			receiveButton.setVisible(false);
			editItemButton.setVisible(false);
			barcodeLabel.setVisible(true);
			barcodeTextField.setVisible(true);
			doneCheckOutButton.setVisible(false);
			doneReceivingButton.setVisible(true);
			newItemButton.setVisible(true);
			itemsTextArea.setVisible(true);
			itemsScrollPane.setVisible(true);
			barcodeTextField.requestFocusInWindow();
		}
		if(e.getSource() == editItemButton) {
			editItem();
		}
		if(e.getSource() == doneCheckOutButton) {			
			calculateInventory("checkOut");
		}
		if(e.getSource() == doneReceivingButton) {
			calculateInventory("receive");
		}
		if(e.getSource() == printButton) {
			try {
				Desktop desktop = null;
				if(Desktop.isDesktopSupported()) {
					desktop = Desktop.getDesktop();
				}
				desktop.print(new File("Restock.csv"));
			}
			catch(IOException ioe) {
				JOptionPane.showMessageDialog(program, "Could not print the restock list.", "Problem with printing", JOptionPane.WARNING_MESSAGE);
			}
		}
		if(e.getSource() == newItemButton) {
			createNewItem();
		}
		if(e.getSource() == undoButton) {
			String itemPopped = itemsStack.pop();
			quantityList.replace(itemPopped, quantityList.get(itemPopped) - 1);
			if(quantityList.get(itemPopped) < 1) {
				quantityList.remove(itemPopped);
				namesList.remove(itemPopped);
				itemsOrder.remove(itemPopped);
			}
			itemsTextArea.setText(updateItems());
			if(itemsStack.isEmpty()) {
				undoButton.setVisible(false);
			}
			barcodeTextField.requestFocusInWindow();
		}
	}
	Action barcodeScanned = new AbstractAction(){
		
		/**
		 * This method takes care of what happens when you scan a barcode
		 */
		
	    @Override
	    public void actionPerformed(ActionEvent e){
	    	String barcodeInput = "";
			String[] barcodeItem;
	        barcodeInput = barcodeTextField.getText().trim();
	        barcodeTextField.setText("");
	        barcodeItem = findItem(barcodeInput);
			if(barcodeItem[0] == null) {
				JOptionPane.showMessageDialog(program, "Could not find the desired item. It is either not in the system or an invalid barcode.", "No match found", JOptionPane.WARNING_MESSAGE);
				barcodeTextField.requestFocusInWindow();
			}
			else {
				namesList.put(barcodeItem[0], barcodeItem[1]);
				itemsStack.push(barcodeItem[0]);
				undoButton.setVisible(true);
				if(quantityList.containsKey(barcodeItem[0])) {
					quantityList.replace(barcodeItem[0], quantityList.get(barcodeItem[0]) + 1);
				}
				else {
					quantityList.put(barcodeItem[0], 1);
					itemsOrder.add(barcodeItem[0]);
				}
				// this is assuming all the in-stock entries are numbers
				if(doneCheckOutButton.isVisible()) {
					if(quantityList.get(barcodeItem[0]) > Integer.parseInt(barcodeItem[2])) {
						JOptionPane.showMessageDialog(program, "Could not check out this item because there is no more stock.", "Exceed stock quantity", JOptionPane.WARNING_MESSAGE);
						quantityList.replace(barcodeItem[0], quantityList.get(barcodeItem[0]) - 1);
						if(quantityList.get(barcodeItem[0]) < 1) {
							itemsStack.pop();
							quantityList.remove(barcodeItem[0]);
							namesList.remove(barcodeItem[0]);
							itemsOrder.remove(barcodeItem[0]);
						}
						itemsTextArea.setText(updateItems());
						if(itemsStack.isEmpty()) {
							undoButton.setVisible(false);
						}
					}
				}
				itemsTextArea.setText(updateItems());
				barcodeTextField.requestFocusInWindow();
			}
	    }
	};
	@Override
	public void mouseEntered(MouseEvent e) {
		
	}
	@Override
	public void mouseExited(MouseEvent e) {
		
	}
	@Override
	public void mousePressed(MouseEvent e) {
		
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		
	}
}
