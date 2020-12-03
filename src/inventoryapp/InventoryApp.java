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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

@SuppressWarnings("serial")
public class InventoryApp extends JFrame implements MouseListener{
	/**
	 * All the variables used throughout the program are instantiated here
	 */
	static InventoryApp program;
	
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	
	// stores the lines of the .csv file
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
	JButton homeButton, reorderButton, checkOutButton, receiveInventoryButton, newItemButton, doneCheckOutButton, doneReceivingButton, printButton, undoButton;
	JTextField barcodeTextField;
	JScrollPane itemsScrollPane;
	JPanel newItemPanel;
	
	int restockingNumber;
	
	InventoryApp(){
		/**
		 * This initializes the program (reads inventory, creates GUI, etc)
		 * 
		 */
		inventoryLines = readFile(System.getenv("INVENTORY_LIST"));
		
	    setTitle("Inventory Management Program");
		setBounds(0,0,screenSize.width, screenSize.height);
	    
	    titleLabel = new JLabel("Inventory Management");
	    titleLabel.setBounds((int)(screenSize.width * 0.25), 0, screenSize.width/2, screenSize.height/10);
	    titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
	    titleLabel.setHorizontalAlignment(JLabel.CENTER);
	    
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
	    
		checkOutButton = new JButton("Check Out");
		checkOutButton.setBounds((int)(screenSize.width * 0.15), (int)(screenSize.height * 0.45), screenSize.width/5, screenSize.height/10);
		checkOutButton.setFont(new Font("Arial", Font.BOLD, 36));
		checkOutButton.addMouseListener(this);
		
		receiveInventoryButton = new JButton("Receive Inventory");
		receiveInventoryButton.setBounds((int)(screenSize.width * 0.65), (int)(screenSize.height * 0.45), screenSize.width/5, screenSize.height/10);
		receiveInventoryButton.setFont(new Font("Arial", Font.BOLD, 36));
		receiveInventoryButton.addMouseListener(this);
		
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
		restockingLabel.setBounds((int)(screenSize.width * 0.6), (int)(screenSize.height * 0.075), screenSize.width/5, screenSize.height/10);
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
		add(receiveInventoryButton);
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
			JOptionPane.showMessageDialog(program, "Could not fine the inventory file", "File not found", JOptionPane.WARNING_MESSAGE);
		}
		catch(IOException e) {
			JOptionPane.showMessageDialog(program, "Could not read the inventory file.", "Problem reading file", JOptionPane.WARNING_MESSAGE);
		}
		return(fileLines);
	}
	public void updateFile() {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(System.getenv("INVENTORY_LIST_2")));
			for(int i = 0; i < inventoryLines.size(); i++) {
				bw.write(inventoryLines.get(i));
				if(i != inventoryLines.size() - 1) {
					bw.newLine();
				}
			}
			bw.close();
		}
		catch(IOException e){
			JOptionPane.showMessageDialog(program, "Could not write to the inventory file.", "Problem writing to file", JOptionPane.WARNING_MESSAGE);
		}
	}
	public void createNewItem() {
		JTextField barcode = new JTextField(10);
		barcode.setFont(new Font("Arial", Font.BOLD, 28));
		barcode.setLocation(100, 100);
		JTextField name = new JTextField(10);
		name.setFont(new Font("Arial", Font.BOLD, 28));
		

		JPanel newItemPanel = new JPanel();
		newItemPanel.setLayout(new GridLayout(2,1));
		newItemPanel.add(barcode);
		newItemPanel.add(name);
		
		
		int result = JOptionPane.showConfirmDialog(program, newItemPanel, 
				"Please enter the item description:", JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {
			System.out.println("x value: " + barcode.getText());
			System.out.println("y value: " + name.getText());
		}
	}
	public String[] findItem(String barcode) {
		/**
		 * This method retrieves the info for the barcode using the same schema as mentioned before
		 * 
		 * @param inventoryList - a list of all the inventory items
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
			if(!barcode.equals("") && barcode.equals(currentLine.substring(0,currentLine.indexOf(",")))){
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
		int selectedQuantity = 0;
		String[] replacedLine;
		String replacedLineString;
		for(int i = 0; i < quantityList.size(); i++) {
			selectedQuantity = quantityList.get(itemsOrder.get(i));
			replacedLine = findItem(itemsOrder.get(i));
			if(checkOutOrReceive.equals("checkOut")) {
				replacedLineString = replacedLine[0] + "," + replacedLine[1] + "," + (Integer.parseInt(replacedLine[2]) - selectedQuantity) + "," + replacedLine[3];
			}
			else {
				replacedLineString = replacedLine[0] + "," + replacedLine[1] + "," + (Integer.parseInt(replacedLine[2]) + selectedQuantity) + "," + replacedLine[3];
			}
			updateLine(replacedLineString, replacedLine[0].length());
		}
		updateFile();
		updateRestockingNumber();
		if(namesList.size() != 0) {
			JOptionPane.showMessageDialog(program, "Successfully updated items!", "Update complete", JOptionPane.PLAIN_MESSAGE);
		}
			
		itemsTextArea.setText("Quantity\tIn Stock\tItem\n");
		itemsOrder.clear();
		namesList.clear();
		quantityList.clear();
		itemsStack.clear();
		undoButton.setVisible(false);
	}
	public void updateLine(String newLine, int barcodeLength) {
		String currentLine = "";
		int lineNumber;
		boolean lookingForMatch = true;
		lineNumber = 0;
		
		while(lookingForMatch){
			currentLine = inventoryLines.get(lineNumber);
			if(newLine.substring(0, barcodeLength).equals(currentLine.substring(0, barcodeLength))){
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
 			restockingLabel.setForeground(Color.BLACK);
 		}
	}
	public String updateItems() {
		String itemsFormatted = "Quantity\tIn Stock\tItem\n";
		for(int i = 0; i < itemsOrder.size(); i++) {
			itemsFormatted += (quantityList.get(itemsOrder.get(i)) + "\t" + findItem(itemsOrder.get(i))[2] + "\t" + namesList.get(itemsOrder.get(i)) + "\n");
		}
		return itemsFormatted;
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getSource() == homeButton) {
			if(!namesList.isEmpty()) {
				int choice = JOptionPane.showConfirmDialog(	program, 
												"You are not finished entering in your items. All progress will be lost. Are you sure you want to exit?" ,
												"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(choice == 0) {
					itemsTextArea.setText("Quantity\tIn Stock\tItem\n");
					itemsOrder.clear();
					namesList.clear();
					quantityList.clear();
					itemsStack.clear();
					checkOutButton.setVisible(true);
					receiveInventoryButton.setVisible(true);
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
			}
			else {
				checkOutButton.setVisible(true);
				receiveInventoryButton.setVisible(true);
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
					itemsTextArea.setText("Quantity\tIn Stock\tItem\n");
					itemsOrder.clear();
					namesList.clear();
					quantityList.clear();
					itemsStack.clear();
					checkOutButton.setVisible(false);
					receiveInventoryButton.setVisible(false);
					barcodeLabel.setVisible(false);
					barcodeTextField.setVisible(false);
					newItemButton.setVisible(false);
					doneCheckOutButton.setVisible(false);
					doneReceivingButton.setVisible(false);
					printButton.setVisible(true);
					undoButton.setVisible(false);
					itemsTextArea.setVisible(false);
					itemsScrollPane.setVisible(false);
				}
			}
			else {
				checkOutButton.setVisible(false);
				receiveInventoryButton.setVisible(false);
				barcodeLabel.setVisible(false);
				barcodeTextField.setVisible(false);
				newItemButton.setVisible(false);
				doneCheckOutButton.setVisible(false);
				doneReceivingButton.setVisible(false);
				printButton.setVisible(true);
				itemsTextArea.setVisible(false);
				itemsScrollPane.setVisible(false);
			}
		}
		if(e.getSource() == checkOutButton) {
			checkOutButton.setVisible(false);
			receiveInventoryButton.setVisible(false);
			barcodeLabel.setVisible(true);
			barcodeTextField.setVisible(true);
			doneCheckOutButton.setVisible(true);
			doneReceivingButton.setVisible(false);
			itemsTextArea.setVisible(true);
			itemsScrollPane.setVisible(true);
			barcodeTextField.requestFocusInWindow();
		}
		if(e.getSource() == receiveInventoryButton) {
			checkOutButton.setVisible(false);
			receiveInventoryButton.setVisible(false);
			barcodeLabel.setVisible(true);
			barcodeTextField.setVisible(true);
			doneCheckOutButton.setVisible(false);
			doneReceivingButton.setVisible(true);
			newItemButton.setVisible(true);
			itemsTextArea.setVisible(true);
			itemsScrollPane.setVisible(true);
			barcodeTextField.requestFocusInWindow();
		}
		if(e.getSource() == doneCheckOutButton) {			
			calculateInventory("checkOut");
		}
		if(e.getSource() == doneReceivingButton) {
			calculateInventory("receive");
		}
		if(e.getSource() == printButton) {
			
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
		}
	}
	Action barcodeScanned = new AbstractAction()
	{
	    @Override
	    public void actionPerformed(ActionEvent e)
	    {
	    	String barcodeInput = "";
			String[] barcodeItem;
	        barcodeInput = barcodeTextField.getText().trim();
	        barcodeTextField.setText("");
	        barcodeItem = findItem(barcodeInput);
			if(barcodeItem[0] == null) {
				JOptionPane.showMessageDialog(program, "Could not find the desired item. It is either not in the system or an invalid barcode.", "No match found", JOptionPane.WARNING_MESSAGE);
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
			}
	    }
	};
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
