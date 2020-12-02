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
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
	
	// stores the lines of the .csv file
	ArrayList<String> inventoryLines = new ArrayList<String>();
	
	// stores the order of how to display scanned items
	ArrayList<String> itemsOrder = new ArrayList<String>();
	
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
	JButton homeButton, reorderButton, checkOutButton, receiveInventoryButton, newItemButton, doneButton, printButton;
	JTextField barcodeTextField;
	JScrollPane itemsScrollPane;
	
	InventoryApp(){
		/**
		 * This initializes the program (reads inventory, creates GUI, etc)
		 * 
		 */
		inventoryLines = readFile(System.getenv("INVENTORY_LIST"));
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int restockingNumber = 0;
		
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
		
		doneButton = new JButton("Done");
		doneButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		doneButton.setFont(new Font("Arial", Font.BOLD, 36));
		doneButton.addMouseListener(this);
		
		newItemButton = new JButton("New Item");
		newItemButton.setBounds((int)(screenSize.width * 0.3), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		newItemButton.setFont(new Font("Arial", Font.BOLD, 36));
		newItemButton.addMouseListener(this);
		
		printButton = new JButton("Print");
		printButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		printButton.setFont(new Font("Arial", Font.BOLD, 36));
		printButton.addMouseListener(this);
		
		restockingLabel = new JLabel(restockingNumber + " item(s) need restocking");
		restockingLabel.setBounds((int)(screenSize.width * 0.6), (int)(screenSize.height * 0.075), screenSize.width/5, screenSize.height/10);
		restockingLabel.setFont(new Font("Arial", Font.BOLD, 20));
		
		barcodeLabel = new JLabel("Barcode:");
		barcodeLabel.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.75), screenSize.width/5, screenSize.height/20);
		barcodeLabel.setFont(new Font("Arial", Font.BOLD, 28));
		
		barcodeTextField = new JTextField();
		barcodeTextField.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		barcodeTextField.setFont(new Font("Arial", Font.BOLD, 48));
		barcodeTextField.addActionListener(barcode);
		
		// add elements to GUI
		add(titleLabel);
		add(reorderButton);
		add(homeButton);
		add(checkOutButton);
		add(receiveInventoryButton);
		add(restockingLabel);
		add(barcodeLabel);
		add(barcodeTextField);
		add(doneButton);
		add(printButton);
		add(newItemButton);
		add(itemsScrollPane);
		
		// show GUI home page on screen
		barcodeLabel.setVisible(false);
		barcodeTextField.setVisible(false);
		doneButton.setVisible(false);
		printButton.setVisible(false);
		newItemButton.setVisible(false);
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
			System.out.println("File not found");
		}
		catch(IOException e) {
			System.out.println("Problem with reading file");
		}
		return(fileLines);
	}
	public static String[] findItem(ArrayList<String> inventoryList, String barcode) {
		/**
		 * This method retrieves the info for the barcode barcode using the same schema as mentioned before
		 * 
		 * @param inventoryList - a list of all the inventory items
		 * @param barcode - the barcode of the barcode item
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
				}
			}
		}
		return(matchedLineEntries);
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
					checkOutButton.setVisible(true);
					receiveInventoryButton.setVisible(true);
					barcodeLabel.setVisible(false);
					barcodeTextField.setVisible(false);
					newItemButton.setVisible(false);
					doneButton.setVisible(false);
					printButton.setVisible(false);
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
				doneButton.setVisible(false);
				printButton.setVisible(false);
				itemsTextArea.setVisible(false);
				itemsScrollPane.setVisible(false);
			}
		}
		if(e.getSource() == reorderButton) {
			checkOutButton.setVisible(false);
			receiveInventoryButton.setVisible(false);
			barcodeLabel.setVisible(false);
			barcodeTextField.setVisible(false);
			doneButton.setVisible(false);
			printButton.setVisible(true);
			itemsTextArea.setVisible(false);
			itemsScrollPane.setVisible(false);
		}
		if(e.getSource() == checkOutButton) {
			checkOutButton.setVisible(false);
			receiveInventoryButton.setVisible(false);
			barcodeLabel.setVisible(true);
			barcodeTextField.setVisible(true);
			doneButton.setVisible(true);
			itemsTextArea.setVisible(true);
			itemsScrollPane.setVisible(true);
			barcodeTextField.requestFocusInWindow();
		}
		if(e.getSource() == receiveInventoryButton) {
			checkOutButton.setVisible(false);
			receiveInventoryButton.setVisible(false);
			barcodeLabel.setVisible(true);
			barcodeTextField.setVisible(true);
			doneButton.setVisible(true);
			newItemButton.setVisible(true);
			itemsTextArea.setVisible(true);
			itemsScrollPane.setVisible(true);
			barcodeTextField.requestFocusInWindow();
		}
		if(e.getSource() == doneButton) {			
			// insert code to subtract/add items from inventory
			
			itemsTextArea.setText("Quantity\tIn Stock\tItem\n");
			itemsOrder.clear();
			namesList.clear();
			quantityList.clear();
		}
		if(e.getSource() == printButton) {
			
		}
		if(e.getSource() == newItemButton) {
			
		}
	}
	Action barcode = new AbstractAction()
	{
	    @Override
	    public void actionPerformed(ActionEvent e)
	    {
	    	String barcodeInput = "";
			String[] barcodeItem;
	        barcodeInput = barcodeTextField.getText().trim();
	        barcodeTextField.setText("");
	        barcodeItem = findItem(inventoryLines, barcodeInput);
			if(barcodeItem[0] == null) {
				JOptionPane.showMessageDialog(program, "Could not find the desired item. It is either not in the system or an invalid barcode.", "No match found", JOptionPane.WARNING_MESSAGE);
			}
			else {
				namesList.put(barcodeItem[0], barcodeItem[1]);
				if(quantityList.containsKey(barcodeItem[0])) {
					quantityList.replace(barcodeItem[0], quantityList.get(barcodeItem[0]) + 1);
				}
				else {
					quantityList.put(barcodeItem[0], 1);
					itemsOrder.add(barcodeItem[0]);
				}
				String itemsFormatted = "Quantity\tIn Stock\tItem\n";
				for(int i = 0; i < itemsOrder.size(); i++) {
					itemsFormatted += (quantityList.get(itemsOrder.get(i)) + "\t1\t" + namesList.get(itemsOrder.get(i)) + "\n");
				}
				itemsTextArea.setText(itemsFormatted);
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
