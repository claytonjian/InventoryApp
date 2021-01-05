package inventoryapp;

/**
 * This is a program that will serve as an inventory management system.
 * - reads barcodes and deducts them from inventory
 * - allows inputting when receiving inventory
 * - prints reports when inventory is low
 * 
 * @author Clayton Jian
 */

import java.io.FileWriter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

@SuppressWarnings("serial")
public class InventoryApp extends JFrame implements MouseListener{
	static InventoryApp program;
	
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	
	Connection conn;
	Statement s;
	ResultSet r = null;
	
	// stores the users of the system
	ArrayList<String> usersLines = new ArrayList<String>();
	
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
	JLabel titleLabel, connectingToDBLabel, restockingLabel, barcodeLabel, scanQuantityLabel;
	JTextArea itemsTextArea;
	JButton homeButton, restockButton, usersButton, itemLogButton, checkOutButton, receiveButton, newItemButton, editAddressButton, doneButton, printButton, undoButton, editItemButton;
	JRadioButton yearRB, monthRB, dayRB;
	JComboBox<Integer> yearComboBox, dayComboBox;
	JComboBox<String> monthComboBox;
	JTextField barcodeTextField;
	JScrollPane itemsScrollPane, itemLogScrollPane, usersScrollPane, sortByScrollPane;
	SpinnerModel scanQuantity;
	JSpinner scanSpinner;
	JList<String> usersList, sortByList;
	JComponent scanEditor;
	JPanel loadingPanel, newItemPanel, editItemPanel, usersPanel, sortByPanel, chooseDatePanel;
	JProgressBar connectingToDBProgressBar = new JProgressBar();
	JTable itemLogTable;
	DefaultTableModel dtm;
	
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMMM dd yyyy");
	DateTimeFormatter dtfSQL = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	String usernameDB, passwordDB;
	int restockingNumber;
	String user = "";
	String sortByName = "";
	LocalDateTime dateTime;
	int itemLog;
	int year, month, day;
	String dateOption;
	String[] sortBy = {"Incomplete", "Date", "User", "Product", "Address"};
	
	String barcodeInput = "";
	String[] barcodeItem = new String[4];
		
	InventoryApp(){
		
		/**
		 * This initializes the program (reads inventory, creates GUI, etc)
		 * 
		 */
		
		setIconImage(Toolkit.getDefaultToolkit().getImage("Logo.jpg"));
		
	    setTitle("Inventory Management Program");
		setBounds(0,0,screenSize.width, screenSize.height);
		
		loadingPanel = new JPanel();
		loadingPanel.setLayout(new GridLayout(2,1));
		
		connectingToDBProgressBar.setIndeterminate(true);
		connectingToDBProgressBar.setBounds((int)(screenSize.width * 0.45), (int)(screenSize.height * 0.45), screenSize.width/10, screenSize.height/40);
		
		loadingPanel.setBounds(screenSize.width/4, screenSize.height/4, screenSize.width/2, screenSize.height/4);
		loadingPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		connectingToDBLabel = new JLabel("Connecting to database...");
	    connectingToDBLabel.setBounds((int)(screenSize.width * 0.45), (int)(screenSize.height * 0.35), screenSize.width, screenSize.height);
	    connectingToDBLabel.setHorizontalAlignment(SwingConstants.CENTER);
		connectingToDBLabel.setFont(new Font("Arial", Font.BOLD, 24));
		
		loadingPanel.add(connectingToDBLabel);
		loadingPanel.add(connectingToDBProgressBar);
				
	    titleLabel = new JLabel("Inventory Management");
	    titleLabel.setBounds(0, 0, screenSize.width, screenSize.height/10);
	    titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
	    titleLabel.setAlignmentX(CENTER_ALIGNMENT);
	    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
	    
	    homeButton = new JButton("Home");
		homeButton.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.025), screenSize.width/5, screenSize.height/20);
		homeButton.setFont(new Font("Arial", Font.BOLD, 36));
		homeButton.addMouseListener(this);
		
		restockButton = new JButton("Restock List");
		restockButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.1), screenSize.width/5, screenSize.height/20);
		restockButton.setFont(new Font("Arial", Font.BOLD, 36));
		restockButton.addMouseListener(this);
		
		usersButton = new JButton("Users");
		usersButton.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.1), screenSize.width/5, screenSize.height/20);
		usersButton.setFont(new Font("Arial", Font.BOLD, 36));
		usersButton.addMouseListener(this);
		
		itemLogButton = new JButton("Item Log");
		itemLogButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.025), screenSize.width/5, screenSize.height/20);
		itemLogButton.setFont(new Font("Arial", Font.BOLD, 36));
		itemLogButton.addMouseListener(this);
		
		editAddressButton = new JButton("Edit Address");
		editAddressButton.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		editAddressButton.setFont(new Font("Arial", Font.BOLD, 36));
		editAddressButton.addMouseListener(this);
						
		itemsTextArea = new JTextArea("Quantity\tIn Stock\tItem\n");
	    itemsTextArea.setFont(new Font("Arial", Font.BOLD, 28));
	    itemsTextArea.setEditable(false);
	    
	    itemsScrollPane = new JScrollPane(itemsTextArea);
	    itemsScrollPane.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.175), (int)(screenSize.width * 0.9), (int)(screenSize.height * 0.575));
	    itemsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
	    
	    itemLogScrollPane = new JScrollPane();
	    itemLogScrollPane.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.175), (int)(screenSize.width * 0.9), (int)(screenSize.height * 0.575));
	    itemLogScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
	    itemLogScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    
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
		
		doneButton = new JButton("Done");
		doneButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		doneButton.setFont(new Font("Arial", Font.BOLD, 36));
		doneButton.addMouseListener(this);
		
		newItemButton = new JButton("New Item");
		newItemButton.setBounds((int)(screenSize.width * 0.35), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		newItemButton.setFont(new Font("Arial", Font.BOLD, 36));
		newItemButton.addMouseListener(this);
		
		printButton = new JButton("Print");
		printButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		printButton.setFont(new Font("Arial", Font.BOLD, 36));
		printButton.addMouseListener(this);
		
		undoButton = new JButton("Undo");
		undoButton.setBounds((int)(screenSize.width * 0.55), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		undoButton.setFont(new Font("Arial", Font.BOLD, 36));
		undoButton.addMouseListener(this);
		
		restockingLabel = new JLabel(restockingNumber + " item(s) needs restocking");
		restockingLabel.setBounds((int)(screenSize.width * 0.5), (int)(screenSize.height * 0.075), screenSize.width/5, screenSize.height/10);
		restockingLabel.setFont(new Font("Arial", Font.BOLD, 20));
		
		barcodeLabel = new JLabel("Barcode:");
		barcodeLabel.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.75), screenSize.width/5, screenSize.height/20);
		barcodeLabel.setFont(new Font("Arial", Font.BOLD, 28));
		
		barcodeTextField = new JTextField();
		barcodeTextField.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		barcodeTextField.setFont(new Font("Arial", Font.PLAIN, 48));
		barcodeTextField.addActionListener(barcodeScanned);
		
		scanQuantityLabel = new JLabel("Quantity:");
		scanQuantityLabel.setBounds((int)(screenSize.width * 0.25), (int)(screenSize.height * 0.75), screenSize.width/5, screenSize.height/20);
		scanQuantityLabel.setFont(new Font("Arial", Font.BOLD, 28));
		
		scanQuantity = new SpinnerNumberModel(1, 1, 1000, 1);
		scanSpinner = new JSpinner(scanQuantity);
		scanSpinner.setBounds((int)(screenSize.width * 0.25), (int)(screenSize.height * 0.8), screenSize.width/10, screenSize.height/10);
		scanSpinner.setFont(new Font("Arial", Font.PLAIN, 48));
		scanEditor = scanSpinner.getEditor();
		if(scanEditor instanceof JSpinner.DefaultEditor) {
			// for interactions with the scan quantity spinner
			JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor)scanEditor;
			spinnerEditor.getTextField().setHorizontalAlignment(JTextField.LEFT);
			spinnerEditor.getTextField().addFocusListener(new FocusListener() {
				public void focusGained(FocusEvent e) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							spinnerEditor.getTextField().selectAll();
						}
					});
				}
				@Override
				public void focusLost(FocusEvent e) {
					
				}
			});
			spinnerEditor.getTextField().addKeyListener(new KeyListener() {

				@Override
				public void keyPressed(KeyEvent e) {
					if(e.getKeyCode() == KeyEvent.VK_ENTER) {
						barcodeTextField.requestFocusInWindow();
					}
				}

				@Override
				public void keyReleased(KeyEvent e) {
					
				}

				@Override
				public void keyTyped(KeyEvent e) {
					
				}
				
			});
		}
		
		// add elements to GUI
		add(connectingToDBProgressBar);
		add(loadingPanel);
		add(titleLabel);
		add(homeButton);
		add(usersButton);
		add(restockButton);
		add(itemLogButton);
		add(checkOutButton);
		add(receiveButton);
		add(editItemButton);
		add(restockingLabel);
		add(barcodeLabel);
		add(scanQuantityLabel);
		add(scanSpinner);
		add(barcodeTextField);
		add(doneButton);
		add(printButton);
		add(newItemButton);
		add(editAddressButton);
		add(undoButton);
		add(itemsScrollPane);
		add(itemLogScrollPane);
		
		// show GUI home page on screen
		barcodeLabel.setVisible(false);
		scanQuantityLabel.setVisible(false);
		barcodeTextField.setVisible(false);
		scanSpinner.setVisible(false);
		doneButton.setVisible(false);
		printButton.setVisible(false);
		newItemButton.setVisible(false);
		editAddressButton.setVisible(false);
		undoButton.setVisible(false);
		itemsTextArea.setVisible(false);
		itemsScrollPane.setVisible(false);
		itemLogScrollPane.setVisible(false);
		setLayout(null);
		setVisible(true);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
	public static void main(String[] args) {
		UIManager.put("OptionPane.messageFont", new Font("Arial", Font.BOLD, 28));
		UIManager.put("OptionPane.buttonFont", new Font("Arial", Font.BOLD, 28));
		try {
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } 
	    catch (Exception e) {
			JOptionPane.showMessageDialog(program, "There is a problem loading the appearance of the program.", "Could not load Look & Feel", JOptionPane.WARNING_MESSAGE);
	    }
		program = new InventoryApp();
		program.setExtendedState(program.getExtendedState()|JFrame.MAXIMIZED_BOTH );
		program.usernameDB = System.getenv("INVENTORYAPP_USER");
		program.passwordDB = System.getenv("INVENTORYAPP_PASSWORD");
		program.connectToDB();
		program.updateRestockingNumber();
	}
	public void connectToDB() {
		loadingPanel.setVisible(true);
		connectingToDBProgressBar.setVisible(true);
		try {
	        Class.forName("java.sql.Driver"); // load driver
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";"); // try to connect with your attributes 
	    } catch (ClassNotFoundException e) { // 
	    	JOptionPane.showMessageDialog(program, "There is a problem loading the JDBC driver.", "Could not load driver", JOptionPane.WARNING_MESSAGE);	    
	    } catch (SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem connecting to the database.", "Could not connect to DB", JOptionPane.WARNING_MESSAGE);
	    }
		finally {
			try { conn.close(); } catch (Exception e) { /* ignored */ }
		}
		connectingToDBProgressBar.setVisible(false);
		loadingPanel.setVisible(false);

	}
	
	public void createNewItem() {
		
		/**
		 * This method is for adding new items into the Inventory.csv file
		 * 
		 */
		
		JTextField barcode = new JTextField(20);
		barcode.setFont(new Font("Arial", Font.PLAIN, 28));
		JTextField name = new JTextField(20);
		name.setFont(new Font("Arial", Font.PLAIN, 28));
		SpinnerModel inStockQuantity = new SpinnerNumberModel(1, 0, 1000, 1);
		SpinnerModel restockQuantity = new SpinnerNumberModel(1, 0, 1000, 1);
		
		JSpinner inStock = new JSpinner(inStockQuantity);
		inStock.setFont(new Font("Arial", Font.PLAIN, 28));
		JComponent inStockEditor = inStock.getEditor();
		if(inStockEditor instanceof JSpinner.DefaultEditor) {
			JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor)inStockEditor;
			spinnerEditor.getTextField().setHorizontalAlignment(JTextField.LEFT);
			spinnerEditor.getTextField().addFocusListener(new FocusListener() {
				public void focusGained(FocusEvent e) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							spinnerEditor.getTextField().selectAll();
						}
					});
				}
				@Override
				public void focusLost(FocusEvent e) {
					
				}
			});
		}
		
		JSpinner restock = new JSpinner(restockQuantity);
		restock.setFont(new Font("Arial", Font.PLAIN, 28));
		JComponent restockEditor = restock.getEditor();
		if(restockEditor instanceof JSpinner.DefaultEditor) {
			JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor)restockEditor;
			spinnerEditor.getTextField().setHorizontalAlignment(JTextField.LEFT);
			spinnerEditor.getTextField().addFocusListener(new FocusListener() {
				public void focusGained(FocusEvent e) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							spinnerEditor.getTextField().selectAll();
						}
					});
				}
				@Override
				public void focusLost(FocusEvent e) {
					
				}
			});
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
			// makes it so the barcode text field is in focus
			barcode.addAncestorListener(new AncestorListener() {     
				@Override
			    public void ancestorRemoved(AncestorEvent e) {
					
				}
				@Override
			    public void ancestorMoved(AncestorEvent e) {
					
				}            
				@Override
			    public void ancestorAdded(final AncestorEvent e) {
			        SwingUtilities.invokeLater(new Runnable() {
			            public void run() {
			                e.getComponent().requestFocusInWindow();
			            }
			        });
			    }
			});
			result = JOptionPane.showConfirmDialog(program, newItemPanel, 
					"Please enter the item description:", JOptionPane.OK_CANCEL_OPTION);
			if(result == JOptionPane.OK_OPTION && !barcode.getText().isEmpty() && !name.getText().isEmpty()) {
				if(findItem(barcode.getText().trim())[0] != null) {
					JOptionPane.showMessageDialog(program, "The barcode you entered already exists for another item. Please enter another one.", "Duplicate barcode", JOptionPane.WARNING_MESSAGE);
					barcode.setText("");
				}
				else {
					try {
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
						s = conn.createStatement();
				        String sqlStatement = "INSERT INTO product VALUES ('" + barcode.getText().trim() + "','" + name.getText() + "'," + inStock.getValue() + "," + restock.getValue() + ");";
				        s.execute(sqlStatement);
				        updateRestockingNumber();
						// updateLog("new", Integer.parseInt(inStock.getValue().toString()), name.getText());
						JOptionPane.showMessageDialog(program, "Successfully added item!", "Item creation complete", JOptionPane.PLAIN_MESSAGE);
						barcodeTextField.requestFocusInWindow();
						validNewItem = true;
					}
					catch(SQLException e) {
				    	JOptionPane.showMessageDialog(program, "There is a problem adding the item into the database.", "Could not add item into DB", JOptionPane.WARNING_MESSAGE);
					}
					finally {
						try { s.close(); } catch (Exception e) { /* ignored */ }
						try { r.close(); } catch (Exception e) { /* ignored */ }
						try { conn.close(); } catch (Exception e) { /* ignored */ }
					}
				}
			}
			else if(result == JOptionPane.OK_OPTION && (barcode.getText().isEmpty() || name.getText().isEmpty())) {
				JOptionPane.showMessageDialog(program, "All fields need to be filled in to create an item.", "Invalid fields", JOptionPane.WARNING_MESSAGE);
			}
			else {
				if(result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
					cancel = true;
				}
			}
		}
	}
	public void editUsers() {
		
		/**
		 * This method is used to manage the users of the system
		 */
		
		user = "";
		usersLines.clear();
		try {	
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
			s = conn.createStatement();
	        String sqlStatement = "SELECT e_fname, e_lname FROM employee ORDER BY e_fname;";
	        r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	usersLines.add(r.getString(1) + " " + r.getString(2));
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem finding the item in the database.", "Could not find item in DB", JOptionPane.WARNING_MESSAGE);
		}
		finally {
			try { s.close(); } catch (Exception e) { /* ignored */ }
			try { r.close(); } catch (Exception e) { /* ignored */ }
			try { conn.close(); } catch (Exception e) { /* ignored */ }
		}
		String[] users = new String[usersLines.size()];
		users =	usersLines.toArray(users);
		usersList = new JList<String>(users);
		usersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		usersList.setLayoutOrientation(JList.VERTICAL);
		usersList.setVisibleRowCount(-1);
		usersList.setFont(new Font("Arial", Font.BOLD, 28));
		usersList.setSelectedIndex(0);
		
		usersScrollPane = new JScrollPane(usersList);
		usersScrollPane.setViewportView(usersList);
		usersScrollPane.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
	    usersScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	    usersScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		usersPanel = new JPanel();
		usersPanel.setFont(new Font("Arial", Font.BOLD, 28));
		usersScrollPane.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
		usersPanel.add(usersScrollPane);
		
		String[] options = {"New User", "Delete User", "Cancel"};
		int result;
		result = JOptionPane.showOptionDialog(program, usersPanel, "Edit Users", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, null);
		if(result == 0) {
			JPanel userInputPanel = new JPanel();
			JLabel newUserLabel = new JLabel("Please enter the new user name:");
			newUserLabel.setFont(new Font("Arial", Font.BOLD, 36));
			JLabel newUserFNameLabel = new JLabel("First name:");
			newUserFNameLabel.setFont(new Font("Arial", Font.BOLD, 28));
			JLabel newUserLNameLabel = new JLabel("Last name:");
			newUserLNameLabel.setFont(new Font("Arial", Font.BOLD, 28));
			JTextField userFNameTextField = new JTextField();
			userFNameTextField.setFont(new Font("Arial", Font.PLAIN, 28));
			JTextField userLNameTextField = new JTextField();
			userLNameTextField.setFont(new Font("Arial", Font.PLAIN, 28));
			userInputPanel.setLayout(new GridLayout(5,1));
			userInputPanel.add(newUserLabel);
			userInputPanel.add(newUserFNameLabel);
			userInputPanel.add(userFNameTextField);
			userInputPanel.add(newUserLNameLabel);
			userInputPanel.add(userLNameTextField);
			userFNameTextField.addAncestorListener(new AncestorListener() {     
				@Override
			    public void ancestorRemoved(AncestorEvent e) {
					
				}
				@Override
			    public void ancestorMoved(AncestorEvent e) {
					
				}            
				@Override
			    public void ancestorAdded(final AncestorEvent e) {
			        SwingUtilities.invokeLater(new Runnable() {
			            public void run() {
			                e.getComponent().requestFocusInWindow();
			            }
			        });
			    }
			});
			int userResult;
			userResult = JOptionPane.showConfirmDialog(program, userInputPanel, "Edit Item", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
			
			
			if(userResult == JOptionPane.OK_OPTION && (userFNameTextField.getText().trim().isEmpty() || userLNameTextField.getText().trim().isEmpty())) {
				JOptionPane.showMessageDialog(program, "The user fields cannot be blank.", "Problem with creating user", JOptionPane.WARNING_MESSAGE);
			}
			else if(userResult == JOptionPane.OK_OPTION && (userFNameTextField.getText().trim().contains(" ") || userLNameTextField.getText().trim().contains(" "))) {
				JOptionPane.showMessageDialog(program, "The user fields cannot contain spaces.", "Problem with creating user", JOptionPane.WARNING_MESSAGE);
			}
			else if(userResult == JOptionPane.OK_OPTION && !userFNameTextField.getText().trim().isEmpty() && !userLNameTextField.getText().trim().isEmpty()) {
				String userFName = userFNameTextField.getText().trim();
				String userLName = userLNameTextField.getText().trim();
				try {
			        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
					s = conn.createStatement();
			        String sqlStatement = "INSERT INTO employee VALUES ('" + userLName + "', '" + userFName + "');";
			        s.execute(sqlStatement);
					JOptionPane.showMessageDialog(program, "Successfully created user!", "User creation complete", JOptionPane.PLAIN_MESSAGE);
				}
				catch(SQLException e) {
			    	JOptionPane.showMessageDialog(program, "There is a problem adding the user into the database.", "Could not add user into DB", JOptionPane.WARNING_MESSAGE);
			    	e.printStackTrace();
				}
				finally {
					try { s.close(); } catch (Exception e) { /* ignored */ }
					try { r.close(); } catch (Exception e) { /* ignored */ }
					try { conn.close(); } catch (Exception e) { /* ignored */ }
				}
			}
		}
		if(result == 1) {
			user = usersList.getSelectedValue();
			String[] userParts = new String[2];
			userParts = user.split("\\s+");
			if(user != null) {
				int choice = JOptionPane.showConfirmDialog(	program, 
						("Are you sure you want to delete " + user + "?"), "Delete User", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(choice == 0) {
					try {
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
						s = conn.createStatement();
					    String sqlStatement = "DELETE FROM employee WHERE e_lname = '" + userParts[1] + "' AND e_fname = '" + userParts[0] + "';";
					    s.execute(sqlStatement);
						JOptionPane.showMessageDialog(program, "Successfully deleted user!", "User deletion complete", JOptionPane.PLAIN_MESSAGE);
					}
					catch(SQLException e){
				    	JOptionPane.showMessageDialog(program, "There is a problem deleting the user from the database.", "Could not delete user from DB", JOptionPane.WARNING_MESSAGE);
					}
					finally {
						try { s.close(); } catch (Exception e) { /* ignored */ }
						try { r.close(); } catch (Exception e) { /* ignored */ }
						try { conn.close(); } catch (Exception e) { /* ignored */ }
					}
				}
			}
			else {
				JOptionPane.showMessageDialog(program, "Could not delete user because there are no users.", "Problem with deleting user", JOptionPane.WARNING_MESSAGE);
			}
		}
	}
	public void chooseBarcode() {
		
		/**
		 * This method is for editing the attributes of items found in the Inventory.csv file
		 * 
		 */
		
		barcodeInput = "";
		barcodeItem = new String[4];
		
		boolean validItem = false;
		while(!validItem) {
			JPanel barcodeInputPanel = new JPanel();
			JLabel editItemLabel = new JLabel("Please enter the barcode of the item:");
			editItemLabel.setFont(new Font("Arial", Font.BOLD, 28));
			JTextField barcodeTextField = new JTextField();
			barcodeTextField.setFont(new Font("Arial", Font.PLAIN, 28));
			barcodeInputPanel.setLayout(new GridLayout(2,1));
			barcodeInputPanel.add(editItemLabel);
			barcodeInputPanel.add(barcodeTextField);
			barcodeTextField.addAncestorListener(new AncestorListener() {     
				@Override
			    public void ancestorRemoved(AncestorEvent e) {
					
				}
				@Override
			    public void ancestorMoved(AncestorEvent e) {
					
				}            
				@Override
			    public void ancestorAdded(final AncestorEvent e) {
			        SwingUtilities.invokeLater(new Runnable() {
			            public void run() {
			                e.getComponent().requestFocusInWindow();
			            }
			        });
			    }
			});
			int barcodeResult;
			barcodeResult = JOptionPane.showConfirmDialog(program, barcodeInputPanel, "Edit Item", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
			if(barcodeResult == JOptionPane.OK_OPTION && barcodeTextField.getText() != null) {
				barcodeInput = barcodeTextField.getText().trim();
	        	barcodeItem = findItem(barcodeInput);
			}
			if(barcodeResult == JOptionPane.CANCEL_OPTION || barcodeResult == JOptionPane.CLOSED_OPTION) {
				validItem = true;
			}
			else if(barcodeItem[0] == null) {
				JOptionPane.showMessageDialog(program, "Could not find the desired item. It is either not in the system or an invalid barcode.", "No match found", JOptionPane.WARNING_MESSAGE);
			}
			else {
				validItem = true;
				
				
			}
		}
	}
	public void chooseDate() {
		
		dateTime = LocalDateTime.now();
		chooseDatePanel = new JPanel();
		chooseDatePanel.setLayout(new GridLayout(3,2));
		String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
		int currentYear, currentMonth, currentDay, earliestYear;
		currentYear = LocalDateTime.now().getYear();
		currentMonth = LocalDateTime.now().getMonthValue();
		currentDay = LocalDateTime.now().getDayOfMonth();
		earliestYear = currentYear;
		year = currentYear;
		month = currentMonth;
		day = currentDay;
		yearRB = new JRadioButton("Year");
		yearRB.setFont(new Font("Arial", Font.BOLD, 28));
		monthRB = new JRadioButton("Month");
		monthRB.setFont(new Font("Arial", Font.BOLD, 28));
		dayRB = new JRadioButton("Day");
		dayRB.setFont(new Font("Arial", Font.BOLD, 28));
		ButtonGroup bg = new ButtonGroup();
		dayRB.setSelected(true);
		yearComboBox = new JComboBox<Integer>();
		yearComboBox.setFont(new Font("Arial", Font.BOLD, 28));
		yearComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(dayRB.isSelected()) {
					int month = 1;
					int selectedDay = dayComboBox.getSelectedIndex() + 1;
					try {
						Date selectedDate = new SimpleDateFormat("MMMM", Locale.ENGLISH).parse(monthComboBox.getSelectedItem().toString());
						Calendar cal = Calendar.getInstance();
						cal.setTime(selectedDate);
						month = cal.get(Calendar.MONTH) + 1;
					}
					catch(Exception e) {
						
					}
					YearMonth yearMonthObject = YearMonth.of(Integer.parseInt(yearComboBox.getSelectedItem().toString()), month);
					dayComboBox.removeAllItems();
					for(int i = 0; i < yearMonthObject.lengthOfMonth(); i++) {
						dayComboBox.addItem((Integer)(i + 1));
					}
					if(dayComboBox.getItemCount() >= selectedDay) {
						dayComboBox.setSelectedItem(selectedDay);
					}
				}
			}
		});
		monthComboBox = new JComboBox<String>(months);
		monthComboBox.setFont(new Font("Arial", Font.BOLD, 28));
		monthComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(dayRB.isSelected()) {
					int month = 1;
					int selectedDay = dayComboBox.getSelectedIndex() + 1;
					try {
						Date selectedDate = new SimpleDateFormat("MMMM", Locale.ENGLISH).parse(monthComboBox.getSelectedItem().toString());
						Calendar cal = Calendar.getInstance();
						cal.setTime(selectedDate);
						month = cal.get(Calendar.MONTH) + 1;
					}
					catch(Exception e) {
						
					}
					YearMonth yearMonthObject = YearMonth.of(Integer.parseInt(yearComboBox.getSelectedItem().toString()), month);
					dayComboBox.removeAllItems();
					for(int i = 0; i < yearMonthObject.lengthOfMonth(); i++) {
						dayComboBox.addItem((Integer)(i + 1));
					}
					if(dayComboBox.getItemCount() >= selectedDay) {
						dayComboBox.setSelectedItem(selectedDay);
					}
				}
			}
		});
		dayComboBox = new JComboBox<Integer>();
		dayComboBox.setFont(new Font("Arial", Font.BOLD, 28));
		for(int i = 0 ; i < 31; i ++) {
			dayComboBox.addItem(i + 1);
		}
		try {	
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
			s = conn.createStatement();
		    String sqlStatement = "SELECT MIN(YEAR(co_date)) FROM checkout;";
		    r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	earliestYear = Integer.parseInt(r.getString(1));
	        }
	        for(int i = earliestYear; i <= currentYear; i++) {
	        	yearComboBox.addItem(i);
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem detecting the years available.", "Could not find years from DB", JOptionPane.WARNING_MESSAGE);
		}
		finally {
			try { s.close(); } catch (Exception e) { /* ignored */ }
			try { r.close(); } catch (Exception e) { /* ignored */ }
			try { conn.close(); } catch (Exception e) { /* ignored */ }
		}
		bg.add(yearRB);
		bg.add(monthRB);
		bg.add(dayRB);
		yearComboBox.setSelectedItem(currentYear);
		monthComboBox.setSelectedIndex(currentMonth - 1);
		dayComboBox.setSelectedItem(currentDay);
		yearRB.addMouseListener(this);
		monthRB.addMouseListener(this);
		dayRB.addMouseListener(this);
		chooseDatePanel.add(yearRB);
		chooseDatePanel.add(yearComboBox);
		chooseDatePanel.add(monthRB);
		chooseDatePanel.add(monthComboBox);
		chooseDatePanel.add(dayRB);
		chooseDatePanel.add(dayComboBox);
		int dateResult;
		dateResult = JOptionPane.showConfirmDialog(program, chooseDatePanel, "Choose Date", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
		if(dateResult == JOptionPane.OK_OPTION) {
			year = Integer.parseInt(yearComboBox.getSelectedItem().toString());
			month = monthComboBox.getSelectedIndex() + 1;
			day = Integer.parseInt(dayComboBox.getSelectedItem().toString());
			if(yearRB.isSelected()) {
				dateOption = "Year";
			}
			else if(monthRB.isSelected()) {
				dateOption = "Month";
			}
			else {
				dateOption = "Day";
			}
		}
	}
	public void editItem() {
		if(barcodeItem[0] != null) {
			JTextField name = new JTextField(20);
			name.setText(barcodeItem[1]);
			name.setFont(new Font("Arial", Font.PLAIN, 28));
			SpinnerModel inStockQuantity = new SpinnerNumberModel(Integer.parseInt(barcodeItem[2]), 0, 1000, 1);
			SpinnerModel restockQuantity = new SpinnerNumberModel(Integer.parseInt(barcodeItem[3]), 0, 1000, 1);
			
			JSpinner inStock = new JSpinner(inStockQuantity);
			inStock.setFont(new Font("Arial", Font.PLAIN, 28));
			JComponent inStockEditor = inStock.getEditor();
			if(inStockEditor instanceof JSpinner.DefaultEditor) {
				JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor)inStockEditor;
				spinnerEditor.getTextField().setHorizontalAlignment(JTextField.LEFT);
				spinnerEditor.getTextField().addFocusListener(new FocusListener() {
					public void focusGained(FocusEvent e) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								spinnerEditor.getTextField().selectAll();
							}
						});
					}
					@Override
					public void focusLost(FocusEvent e) {
						
					}
				});
			}
			
			JSpinner restock = new JSpinner(restockQuantity);
			restock.setFont(new Font("Arial", Font.PLAIN, 28));
			JComponent restockEditor = restock.getEditor();
			if(restockEditor instanceof JSpinner.DefaultEditor) {
				JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor)restockEditor;
				spinnerEditor.getTextField().setHorizontalAlignment(JTextField.LEFT);
				spinnerEditor.getTextField().addFocusListener(new FocusListener() {
					public void focusGained(FocusEvent e) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								spinnerEditor.getTextField().selectAll();
							}
						});
					}
					@Override
					public void focusLost(FocusEvent e) {
						
					}
				});
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
			boolean cancel = false;
			while(!cancel) {
				// makes the name text field in focus
				name.addAncestorListener(new AncestorListener() {     
					@Override
				    public void ancestorRemoved(AncestorEvent e) {
						
					}
					@Override
				    public void ancestorMoved(AncestorEvent e) {
						
					}            
					@Override
				    public void ancestorAdded(final AncestorEvent e) {
				        SwingUtilities.invokeLater(new Runnable() {
				            public void run() {
				                e.getComponent().requestFocusInWindow();
				            }
				        });
				    }
				});
				String[] options = {"OK", "Delete Item", "Cancel"};
				result = JOptionPane.showOptionDialog(program, editItemPanel, 
						"Edit Item:", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, null);
				
				if(result == 2 || result == JOptionPane.CLOSED_OPTION) {
					int choice = JOptionPane.showConfirmDialog(	program, 
							"All progress will be lost. Are you sure you want to exit?" ,
							"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if(choice == 0) {
						cancel = true;
					}
				}
				else if(result == 1) {
					int choice = JOptionPane.showConfirmDialog(	program, 
							"Are you sure you want to delete this item from the database? You cannot undo this action." ,
							"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if(choice == 0) {
						try {	
					        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
							s = conn.createStatement();
						    String sqlStatement = "DELETE FROM product WHERE p_id = '" + barcodeItem[0] + "';";
					        s.execute(sqlStatement);
							updateRestockingNumber();
							cancel = true;
						}
						catch(SQLException e) {
					    	JOptionPane.showMessageDialog(program, "There is a problem deleting the item from the database.", "Could not delete item from DB", JOptionPane.WARNING_MESSAGE);
						}
						finally {
							try { s.close(); } catch (Exception e) { /* ignored */ }
							try { r.close(); } catch (Exception e) { /* ignored */ }
							try { conn.close(); } catch (Exception e) { /* ignored */ }
						}
						JOptionPane.showMessageDialog(program, "Successfully deleted item!", "Item deletion complete", JOptionPane.PLAIN_MESSAGE);
						cancel = true;
					}
				}
				else {
					try {	
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
						s = conn.createStatement();
				        String sqlStatement = "UPDATE product SET p_name= '" + name.getText() + "', instock= " + inStock.getValue() + ", restock= " + restock.getValue() + " WHERE p_id = '" + barcodeItem[0] + "';";
				        s.execute(sqlStatement);
						updateRestockingNumber();
						// updateLog("new", Integer.parseInt(inStock.getValue().toString()), name.getText());
						JOptionPane.showMessageDialog(program, "Successfully edited item!", "Item edit complete", JOptionPane.PLAIN_MESSAGE);
						cancel = true;
					}
					catch(SQLException e) {
				    	JOptionPane.showMessageDialog(program, "There is a problem editing the item in the database.", "Could not edit item in DB", JOptionPane.WARNING_MESSAGE);
					}
					finally {
						try { s.close(); } catch (Exception e) { /* ignored */ }
						try { r.close(); } catch (Exception e) { /* ignored */ }
						try { conn.close(); } catch (Exception e) { /* ignored */ }
					}
				}
			}
		}
	}
	public void editAddress(int itemLogNumber) {
		JPanel addressInputPanel = new JPanel();
		JLabel editAddressLabel = new JLabel("Please enter the address of the item:");
		editAddressLabel.setFont(new Font("Arial", Font.BOLD, 28));
		JTextField addressTextField = new JTextField();
		addressTextField.setFont(new Font("Arial", Font.PLAIN, 28));
		addressInputPanel.setLayout(new GridLayout(2,1));
		addressInputPanel.add(editAddressLabel);
		addressInputPanel.add(addressTextField);
		addressTextField.addAncestorListener(new AncestorListener() {     
			@Override
		    public void ancestorRemoved(AncestorEvent e) {
				
			}
			@Override
		    public void ancestorMoved(AncestorEvent e) {
				
			}            
			@Override
		    public void ancestorAdded(final AncestorEvent e) {
		        SwingUtilities.invokeLater(new Runnable() {
		            public void run() {
		                e.getComponent().requestFocusInWindow();
		            }
		        });
		    }
		});
		int addressResult;
		addressResult = JOptionPane.showConfirmDialog(program, addressInputPanel, "Edit Address", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
		if(addressResult == JOptionPane.OK_OPTION && addressTextField.getText() != null) {
			try {	
		        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
				s = conn.createStatement();
		        String sqlStatement = "UPDATE checkout SET address= '" + addressTextField.getText() + "' WHERE co_id = " + itemLogNumber + ";";
		        s.execute(sqlStatement);
			}
			catch(SQLException e) {
		    	JOptionPane.showMessageDialog(program, "There is a problem edting the address in the database.", "Could not edit address in DB", JOptionPane.WARNING_MESSAGE);
			}
			finally {
				try { s.close(); } catch (Exception e) { /* ignored */ }
				try { r.close(); } catch (Exception e) { /* ignored */ }
				try { conn.close(); } catch (Exception e) { /* ignored */ }
			}
		}
	}
	public void chooseUser() {
		
		/**
		 * This method is used to finalize a transaction and take the user's name
		 */
		
		user = "";
		usersLines.clear();
		try {	
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
			s = conn.createStatement();
	        String sqlStatement = "SELECT e_fname, e_lname FROM employee ORDER BY e_fname;";
	        r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	usersLines.add(r.getString(1) + " " + r.getString(2));
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem loading the users from the database.", "Could not load users from DB", JOptionPane.WARNING_MESSAGE);
		}
		finally {
			try { s.close(); } catch (Exception e) { /* ignored */ }
			try { r.close(); } catch (Exception e) { /* ignored */ }
			try { conn.close(); } catch (Exception e) { /* ignored */ }
		}
		String[] users = new String[usersLines.size()];
		users =	usersLines.toArray(users);
		usersList = new JList<String>(users);
		usersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		usersList.setLayoutOrientation(JList.VERTICAL);
		usersList.setVisibleRowCount(-1);
		usersList.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
		usersList.setFont(new Font("Arial", Font.BOLD, 28));
		usersScrollPane = new JScrollPane(usersList);
		usersScrollPane.setViewportView(usersList);
		usersScrollPane.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
	    usersScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	    usersScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		
		usersPanel = new JPanel();
		usersPanel.setFont(new Font("Arial", Font.BOLD, 28));
		usersPanel.add(usersScrollPane);
		
		int result;
		boolean validUser = false, cancel = false;
		while(!validUser && !cancel) {
			result = JOptionPane.showConfirmDialog(program, usersPanel, 
					"Please choose your name:", JOptionPane.OK_CANCEL_OPTION);
			if(result == JOptionPane.OK_OPTION) {
				user = usersList.getSelectedValue();
				if(user != null) {
					validUser = true;
					if(titleLabel.getText().equals("Check Out(-)")) {
						calculateInventory("checkOut");
					}

					if(titleLabel.getText().equals("Receive(+)")) {
						calculateInventory("receive");
					}
				}
				else {
					JOptionPane.showMessageDialog(program, "No user was selected.", "Sign in failed", JOptionPane.WARNING_MESSAGE);
				}
			}
			else {
				cancel = true;
			}
		}
		
	}
	public void chooseSortBy() {
		sortByName = "";
		sortByList = new JList<String>(sortBy);
		sortByList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sortByList.setLayoutOrientation(JList.VERTICAL);
		sortByList.setVisibleRowCount(-1);
		sortByList.setFont(new Font("Arial", Font.BOLD, 28));
		sortByList.setSelectedIndex(0);
		
		sortByScrollPane = new JScrollPane(sortByList);
		sortByScrollPane.setViewportView(sortByList);
		sortByScrollPane.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
	    sortByScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	    sortByScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		sortByPanel = new JPanel();
		sortByPanel.setFont(new Font("Arial", Font.BOLD, 28));
		sortByScrollPane.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
		sortByPanel.add(sortByScrollPane);
		
		int result = JOptionPane.showConfirmDialog(program, sortByPanel, "Sort Item Log", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
		if(result == JOptionPane.OK_OPTION) {
			if(sortByList.getSelectedValue().equals("Incomplete")) {
				sortByName = "Incomplete";
				sortByAndShow();
			}
			else if(sortByList.getSelectedValue().equals("Date")) {
				sortByName = "Date";
				sortByAndShow();
			}
			else if(sortByList.getSelectedValue().equals("User")) {
				sortByName = "User";
				sortByAndShow();
			}
			else if(sortByList.getSelectedValue().equals("Product")) {
				sortByName = "Product";
				sortByAndShow();
			}
			else {
				sortByName = "Address";
				sortByAndShow();
			}
		}
	}
	public void sortByAndShow() {

		String[] fields;
		String[][] values;
		int length = 0;
		int count = 0;
		if(sortByName.equals("Incomplete")) {
			fields =  new String[] {"Date", "User", "Product", "Address", "Log Number"};
			values = null;
			try {	
		        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
				s = conn.createStatement();
				// get table size
		        String sqlStatement = "SELECT COUNT(co_id) FROM checkout WHERE address IS NULL;";
		        r = s.executeQuery(sqlStatement);
		        while(r.next()) {
		        	length = Integer.parseInt(r.getString(1));
		        }
		        values = new String[length][fields.length];
		        s = conn.createStatement();
		        sqlStatement = "SELECT co_date, CONCAT(employee.e_fname, ' ', employee.e_lname), product.p_name, address, co_id FROM (checkout LEFT JOIN employee ON checkout.e_id = employee.e_id LEFT JOIN product ON checkout.p_id = product.p_id) WHERE address IS NULL ORDER BY co_date;";
		        r = s.executeQuery(sqlStatement);
		        while(r.next()) {
		        	for(int i = 0; i < fields.length; i++) {
		        		values[count][i] = r.getString(i + 1);
		        	}
		        	count++;
		        }
			}
			catch(SQLException e) {
		    	JOptionPane.showMessageDialog(program, "There is a problem finding the items in the database.", "Could not find items in DB", JOptionPane.WARNING_MESSAGE);
			}
	 		finally {
				try { s.close(); } catch (Exception e) { /* ignored */ }
				try { r.close(); } catch (Exception e) { /* ignored */ }
				try { conn.close(); } catch (Exception e) { /* ignored */ }
			}
		}
		else if(sortByName.equals("Date")) {
			chooseDate();
			fields =  new String[] {"Date", "User", "Product", "Address", "Log Number"};
			values = null;
			try {	
		        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
				s = conn.createStatement();
				// get table size
		        String sqlStatement = "SELECT COUNT(co_id) FROM checkout;";
		        if(dateOption.equals("Year")) {
		        	sqlStatement = "SELECT COUNT(co_id) FROM checkout WHERE YEAR(co_date) = " + year + ";";
		        }
		        else if(dateOption.equals("Month")) {
		        	sqlStatement = "SELECT COUNT(co_id) FROM checkout WHERE YEAR(co_date) = " + year + " AND MONTH(co_date) = " + month + ";";
		        }
		        else {
		        	sqlStatement = "SELECT COUNT(co_id) FROM checkout WHERE YEAR(co_date) = " + year + " AND MONTH(co_date) = " + month + " AND DAY(co_date) = " + day + ";";
		        }
		        r = s.executeQuery(sqlStatement);
		        while(r.next()) {
		        	length = Integer.parseInt(r.getString(1));
		        }
		        values = new String[length][fields.length];
		        s = conn.createStatement();
		        sqlStatement = "SELECT co_date, CONCAT(employee.e_fname, ' ',employee.e_lname), product.p_name, address, co_id FROM checkout LEFT JOIN employee ON checkout.e_id = employee.e_id LEFT JOIN product ON checkout.p_id = product.p_id ORDER BY co_date DESC;";
		        if(dateOption.equals("Year")) {
		        	sqlStatement = "SELECT co_date, CONCAT(employee.e_fname, ' ',employee.e_lname), product.p_name, address, co_id FROM checkout LEFT JOIN employee ON checkout.e_id = employee.e_id LEFT JOIN product ON checkout.p_id = product.p_id WHERE YEAR(co_date) = " + year + " ORDER BY co_date DESC;";
		        }
		        else if(dateOption.equals("Month")) {
		        	sqlStatement = "SELECT co_date, CONCAT(employee.e_fname, ' ',employee.e_lname), product.p_name, address, co_id FROM checkout LEFT JOIN employee ON checkout.e_id = employee.e_id LEFT JOIN product ON checkout.p_id = product.p_id WHERE YEAR(co_date) = " + year + " AND MONTH(co_date) = " + month + " ORDER BY co_date DESC;";
		        }
		        else {
		        	sqlStatement = "SELECT co_date, CONCAT(employee.e_fname, ' ',employee.e_lname), product.p_name, address, co_id FROM checkout LEFT JOIN employee ON checkout.e_id = employee.e_id LEFT JOIN product ON checkout.p_id = product.p_id WHERE YEAR(co_date) = " + year + " AND MONTH(co_date) = " + month + " AND DAY(co_date) = " + day + " ORDER BY co_date DESC;";
		        }
		        r = s.executeQuery(sqlStatement);
		        while(r.next()) {
		        	for(int i = 0; i < fields.length; i++) {
		        		values[count][i] = r.getString(i + 1);
		        	}
		        	count++;
		        }
			}
			catch(SQLException e) {
		    	JOptionPane.showMessageDialog(program, "There is a problem finding the items in the database.", "Could not find items in DB", JOptionPane.WARNING_MESSAGE);
			}
	 		finally {
				try { s.close(); } catch (Exception e) { /* ignored */ }
				try { r.close(); } catch (Exception e) { /* ignored */ }
				try { conn.close(); } catch (Exception e) { /* ignored */ }
			}
		}
		else if(sortByName.equals("User")) {
			chooseUser();
			String[] userParts = new String[2];
			userParts = user.split("\\s+");
			fields =  new String[] {"User", "Date", "Product", "Address", "Log Number"};
			values = null;
			if(!user.equals("")) {
				try {	
			        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
					s = conn.createStatement();
					// get table size
			        String sqlStatement = "SELECT COUNT(co_id) FROM (checkout LEFT JOIN employee ON checkout.e_id = employee.e_id) WHERE e_lname = '" + userParts[1] + "' AND e_fname = '" + userParts[0] + "';";
			        r = s.executeQuery(sqlStatement);
			        while(r.next()) {
			        	length = Integer.parseInt(r.getString(1));
			        }
			        values = new String[length][fields.length];
			        s = conn.createStatement();
			        sqlStatement = "SELECT CONCAT(employee.e_fname, ' ',employee.e_lname), co_date, product.p_name, address, co_id FROM (checkout LEFT JOIN employee ON checkout.e_id = employee.e_id LEFT JOIN product ON checkout.p_id = product.p_id) WHERE e_lname = '" + userParts[1] + "' AND e_fname = '" + userParts[0] + "' ORDER BY co_date DESC;";
			        r = s.executeQuery(sqlStatement);
			        while(r.next()) {
			        	for(int i = 0; i < fields.length; i++) {
			        		values[count][i] = r.getString(i + 1);
			        	}
			        	count++;
			        }
				}
				catch(SQLException e) {
			    	JOptionPane.showMessageDialog(program, "There is a problem finding the items in the database.", "Could not find items in DB", JOptionPane.WARNING_MESSAGE);
				}
		 		finally {
					try { s.close(); } catch (Exception e) { /* ignored */ }
					try { r.close(); } catch (Exception e) { /* ignored */ }
					try { conn.close(); } catch (Exception e) { /* ignored */ }
				}
			}
		}
		else if(sortByName.equals("Product")) {
			chooseBarcode();
			fields =  new String[] {"Product", "Date", "User", "Address", "Log Number"};
			values = null;
			if(barcodeItem[0] != null) {
				try {	
			        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
					s = conn.createStatement();
					// get table size
			        String sqlStatement = "SELECT COUNT(co_id) FROM checkout WHERE p_id = '" + barcodeItem[0] + "';";
			        r = s.executeQuery(sqlStatement);
			        while(r.next()) {
			        	length = Integer.parseInt(r.getString(1));
			        }
			        values = new String[length][fields.length];
			        s = conn.createStatement();
			        sqlStatement = "SELECT product.p_name, co_date, CONCAT(employee.e_fname, ' ',employee.e_lname), address, co_id FROM (checkout LEFT JOIN employee ON checkout.e_id = employee.e_id LEFT JOIN product ON checkout.p_id = product.p_id) WHERE product.p_id = '" + barcodeItem[0] + "' ORDER BY co_date DESC;";
			        r = s.executeQuery(sqlStatement);
			        while(r.next()) {
			        	for(int i = 0; i < fields.length; i++) {
			        		values[count][i] = r.getString(i + 1);
			        	}
			        	count++;
			        }
				}
				catch(SQLException e) {
			    	JOptionPane.showMessageDialog(program, "There is a problem finding the items in the database.", "Could not find items in DB", JOptionPane.WARNING_MESSAGE);
				}
		 		finally {
					try { s.close(); } catch (Exception e) { /* ignored */ }
					try { r.close(); } catch (Exception e) { /* ignored */ }
					try { conn.close(); } catch (Exception e) { /* ignored */ }
				}
			}
		}
		else {
			fields =  new String[] {"Address", "Date", "User", "Product", "Log Number"};
			values = null;
			try {	
		        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
				s = conn.createStatement();
				// get table size
		        String sqlStatement = "SELECT COUNT(co_id) FROM checkout;";
		        r = s.executeQuery(sqlStatement);
		        while(r.next()) {
		        	length = Integer.parseInt(r.getString(1));
		        }
		        values = new String[length][fields.length];
		        s = conn.createStatement();
		        sqlStatement = "SELECT address, co_date, CONCAT(employee.e_fname, ' ',employee.e_lname), product.p_name, co_id FROM checkout LEFT JOIN employee ON checkout.e_id = employee.e_id LEFT JOIN product ON checkout.p_id = product.p_id ORDER BY co_date DESC;";
		        r = s.executeQuery(sqlStatement);
		        while(r.next()) {
		        	for(int i = 0; i < fields.length; i++) {
		        		values[count][i] = r.getString(i + 1);
		        	}
		        	count++;
		        }
			}
			catch(SQLException e) {
		    	JOptionPane.showMessageDialog(program, "There is a problem finding the items in the database.", "Could not find items in DB", JOptionPane.WARNING_MESSAGE);
			}
	 		finally {
				try { s.close(); } catch (Exception e) { /* ignored */ }
				try { r.close(); } catch (Exception e) { /* ignored */ }
				try { conn.close(); } catch (Exception e) { /* ignored */ }
			}
		}
		itemLogTable = new JTable(values, fields);
		dtm = new DefaultTableModel(values, fields) {
			@Override
		    public boolean isCellEditable(int row, int column) {
		       return false;
		    }
		};
		itemLogTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 20));
		itemLogTable.setFont(new Font("Arial", Font.BOLD, 16));
		itemLogTable.setRowHeight(50);
		itemLogTable.setModel(dtm);
		itemLogScrollPane.setViewportView(itemLogTable);
		if(itemLogTable.getRowCount() != 0) {
			itemLogTable.setRowSelectionInterval(0, 0);
		}
	}
	public String[] findItem(String barcode) {
		
		/**
		 * This method retrieves the info for the barcode using the same schema as mentioned before
		 * 
		 * @param barcode - the barcode of the barcode item
		 * @return matchedLineEntries - a list of the entries of the matched item
		 */
		
		String[] matchedLineEntries = new String[4];
		
		try {
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
			s = conn.createStatement();
	        String sqlStatement = "SELECT * FROM product WHERE p_id = '" + barcode + "';";
	        r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	matchedLineEntries[0] = r.getString(1);
	        	matchedLineEntries[1] = r.getString(2);
	        	matchedLineEntries[2] = r.getString(3);
	        	matchedLineEntries[3] = r.getString(4);
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem finding the item in the database.", "Could not find item in DB", JOptionPane.WARNING_MESSAGE);
		}
		finally {
			try { s.close(); } catch (Exception e) { /* ignored */ }
			try { r.close(); } catch (Exception e) { /* ignored */ }
			try { conn.close(); } catch (Exception e) { /* ignored */ }
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
		for(int i = 0; i < quantityList.size(); i++) {
			selectedQuantity = quantityList.get(itemsOrder.get(i));
			replacedLine = findItem(itemsOrder.get(i));
			if(checkOutOrReceive.equals("checkOut")) {
				try {	
			        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
					s = conn.createStatement();
			        String sqlStatement = "UPDATE product SET instock= " + (Integer.parseInt(replacedLine[2]) - selectedQuantity) + " WHERE p_id = '" + replacedLine[0] + "';";
			        s.execute(sqlStatement);
				}
				catch(SQLException e) {
			    	JOptionPane.showMessageDialog(program, "There is a problem subtracting item " + replacedLine[0] + " in the database.", "Could not subtract item in DB", JOptionPane.WARNING_MESSAGE);
				}
				finally {
					try { s.close(); } catch (Exception e) { /* ignored */ }
					try { r.close(); } catch (Exception e) { /* ignored */ }
					try { conn.close(); } catch (Exception e) { /* ignored */ }
				}
				
				updateLog("sub", selectedQuantity, replacedLine[0]);
			}
			else {
				try {	
			        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
					s = conn.createStatement();
			        String sqlStatement = "UPDATE product SET instock= " + (Integer.parseInt(replacedLine[2]) + selectedQuantity) + " WHERE p_id = '" + replacedLine[0] + "';";
			        s.execute(sqlStatement);
				}
				catch(SQLException e) {
			    	JOptionPane.showMessageDialog(program, "There is a problem adding the item in the database.", "Could not add item in DB", JOptionPane.WARNING_MESSAGE);
				}
				finally {
					try { s.close(); } catch (Exception e) { /* ignored */ }
					try { r.close(); } catch (Exception e) { /* ignored */ }
					try { conn.close(); } catch (Exception e) { /* ignored */ }
				}
				updateLog("add", selectedQuantity, replacedLine[0]);
				
				
			}
		}
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
	
	public void updateLog(String operator, int quantity, String itemBarcode) {
		if(operator.equals("sub")) {
			user = usersList.getSelectedValue();
			String[] userParts = new String[2];
			userParts = user.split("\\s+");
			try {	
				for(int i = 0; i < quantity; i++) {
			        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
					s = conn.createStatement();
			        String sqlStatement = "INSERT INTO checkout (co_date, e_id, p_id, address) SELECT '" + dtfSQL.format(LocalDateTime.now()) + "', e_id, '" + itemBarcode + "', NULL FROM employee WHERE e_lname = '" + userParts[1] + "' AND e_fname = '" + userParts[0] + "';";
			        s.execute(sqlStatement);
				}
			}
			catch(SQLException e) {

			}
			finally {
				try { s.close(); } catch (Exception e) { /* ignored */ }
				try { r.close(); } catch (Exception e) { /* ignored */ }
				try { conn.close(); } catch (Exception e) { /* ignored */ }
			}
		}
		/*
		else if(operator.equals("add")) {
			bw.write(dtf.format(LocalDateTime.now()) + "\t" + user + " added " + quantity + " " + item + "\n");
		}
		
		else {
			bw.write(dtf.format(LocalDateTime.now()) + "\t" + user + " introduced/changed " + item + "\n");
		}
		*/
	}
	
	public void updateRestockingNumber() {
		
		/**
		 * This method updates the restocking number at the top of the GUI
		 */
		
		restockingNumber = 0;
 		
 		try {	
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
			s = conn.createStatement();
	        String sqlStatement = "SELECT COUNT(p_id) FROM product WHERE instock < restock;";
	        r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	restockingNumber = Integer.parseInt(r.getString(1));
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem determining the restocking numbers from the database.", "Could not determine restock numbers", JOptionPane.WARNING_MESSAGE);
		}
 		finally {
			try { s.close(); } catch (Exception e) { /* ignored */ }
			try { r.close(); } catch (Exception e) { /* ignored */ }
			try { conn.close(); } catch (Exception e) { /* ignored */ }
		}
 		if(restockingNumber != 0) {
 			restockingLabel.setForeground(Color.RED);
 			restockingLabel.setText(restockingNumber + " item(s) needs restocking");
 		}
 		else {
 			restockingLabel.setForeground(Color.BLACK);
 			restockingLabel.setText(restockingNumber + " item(s) needs restocking");
 		}
 		
	}
	public void updateRestockingItems() {
		
		/**
		 * This method updates the list of items that needs to be restocked
		 */
		
		String itemsFormatted = "Restock Inventory List: " + dtf.format(LocalDateTime.now()) + "\n\nIn Stock\tItem\n";
		String currentItemName = "";
 		try {	
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=" + usernameDB + ";password=" +passwordDB + ";");
			s = conn.createStatement();
	        String sqlStatement = "SELECT instock, p_name FROM product WHERE instock < restock ORDER BY p_name;";
	        r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	currentItemName = r.getString(2);
	        	if(currentItemName.length() > 70) {
					currentItemName = currentItemName.substring(0,70) + "...";
				}
				itemsFormatted += r.getString(1) + "\t" + currentItemName + "\n";
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem finding the item in the database.", "Could not find item in DB", JOptionPane.WARNING_MESSAGE);
		}
 		finally {
			try { s.close(); } catch (Exception e) { /* ignored */ }
			try { r.close(); } catch (Exception e) { /* ignored */ }
			try { conn.close(); } catch (Exception e) { /* ignored */ }
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
					titleLabel.setText("Inventory Management");
					checkOutButton.setVisible(true);
					receiveButton.setVisible(true);
					editItemButton.setVisible(true);
					barcodeLabel.setVisible(false);
					scanQuantityLabel.setVisible(false);
					barcodeTextField.setVisible(false);
					scanSpinner.setVisible(false);
					newItemButton.setVisible(false);
					editAddressButton.setVisible(false);
					doneButton.setVisible(false);
					printButton.setVisible(false);
					undoButton.setVisible(false);
					itemsTextArea.setVisible(false);
					itemsScrollPane.setVisible(false);
					itemLogScrollPane.setVisible(false);
				}
				barcodeTextField.requestFocusInWindow();
			}
			else {
				titleLabel.setText("Inventory Management");
				checkOutButton.setVisible(true);
				receiveButton.setVisible(true);
				editItemButton.setVisible(true);
				barcodeLabel.setVisible(false);
				scanQuantityLabel.setVisible(false);
				barcodeTextField.setVisible(false);
				scanSpinner.setVisible(false);
				newItemButton.setVisible(false);
				editAddressButton.setVisible(false);
				doneButton.setVisible(false);
				printButton.setVisible(false);
				itemsTextArea.setVisible(false);
				itemsScrollPane.setVisible(false);
				itemLogScrollPane.setVisible(false);
			}
		}
		if(e.getSource() == restockButton) {
			if(!namesList.isEmpty()) {
				int choice = JOptionPane.showConfirmDialog(	program, 
												"You are not finished entering in your items. All progress will be lost. Are you sure you want to exit?" ,
												"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(choice == 0) {
					itemsOrder.clear();
					namesList.clear();
					quantityList.clear();
					itemsStack.clear();
					titleLabel.setText("Restock");
					checkOutButton.setVisible(false);
					receiveButton.setVisible(false);
					editItemButton.setVisible(false);
					barcodeLabel.setVisible(false);
					scanQuantityLabel.setVisible(false);
					barcodeTextField.setVisible(false);
					scanSpinner.setVisible(false);
					newItemButton.setVisible(false);
					editAddressButton.setVisible(false);
					doneButton.setVisible(false);
					printButton.setVisible(true);
					undoButton.setVisible(false);
					itemsTextArea.setVisible(true);
					itemsScrollPane.setVisible(true);
					itemLogScrollPane.setVisible(false);
					updateRestockingItems();
					itemsTextArea.setCaretPosition(0);
				}
			}
			else {
				titleLabel.setText("Restock");
				checkOutButton.setVisible(false);
				receiveButton.setVisible(false);
				editItemButton.setVisible(false);
				barcodeLabel.setVisible(false);
				scanQuantityLabel.setVisible(false);
				barcodeTextField.setVisible(false);
				scanSpinner.setVisible(false);
				newItemButton.setVisible(false);
				editAddressButton.setVisible(false);
				doneButton.setVisible(false);
				printButton.setVisible(true);
				itemsTextArea.setVisible(true);
				itemsScrollPane.setVisible(true);
				itemLogScrollPane.setVisible(false);
				updateRestockingItems();
				itemsTextArea.setCaretPosition(0);

			}
		}
		if(e.getSource() == itemLogButton) {
			if(!namesList.isEmpty()) {
				int choice = JOptionPane.showConfirmDialog(	program, 
												"You are not finished entering in your items. All progress will be lost. Are you sure you want to exit?" ,
												"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(choice == 0) {
					itemsOrder.clear();
					namesList.clear();
					quantityList.clear();
					itemsStack.clear();
					titleLabel.setText("Item Log");
					checkOutButton.setVisible(false);
					receiveButton.setVisible(false);
					editItemButton.setVisible(false);
					barcodeLabel.setVisible(false);
					scanQuantityLabel.setVisible(false);
					barcodeTextField.setVisible(false);
					scanSpinner.setVisible(false);
					newItemButton.setVisible(false);
					editAddressButton.setVisible(true);
					doneButton.setVisible(false);
					printButton.setVisible(true);
					undoButton.setVisible(false);
					itemsTextArea.setVisible(true);
					itemsScrollPane.setVisible(false);
					itemLogScrollPane.setVisible(true);
					itemsTextArea.setCaretPosition(0);
					chooseSortBy();
				}
			}
			else {
				titleLabel.setText("Item Log");
				checkOutButton.setVisible(false);
				receiveButton.setVisible(false);
				editItemButton.setVisible(false);
				barcodeLabel.setVisible(false);
				scanQuantityLabel.setVisible(false);
				barcodeTextField.setVisible(false);
				scanSpinner.setVisible(false);
				newItemButton.setVisible(false);
				editAddressButton.setVisible(true);
				doneButton.setVisible(false);
				printButton.setVisible(true);
				itemsTextArea.setVisible(true);
				itemsScrollPane.setVisible(false);
				itemLogScrollPane.setVisible(true);
				itemsTextArea.setCaretPosition(0);
				chooseSortBy();
			}
			
		}
		if(e.getSource() == usersButton) {
			editUsers();
		}
		if(e.getSource() == checkOutButton) {
			titleLabel.setText("Check Out(-)");
			itemsTextArea.setText("Quantity\tIn Stock\tItem\n");
			scanQuantity.setValue(1);
			checkOutButton.setVisible(false);
			receiveButton.setVisible(false);
			editItemButton.setVisible(false);
			barcodeLabel.setVisible(true);
			scanQuantityLabel.setVisible(true);
			barcodeTextField.setVisible(true);
			scanSpinner.setVisible(true);
			doneButton.setVisible(true);
			editAddressButton.setVisible(false);
			newItemButton.setVisible(false);
			itemsTextArea.setVisible(true);
			itemsScrollPane.setVisible(true);
			itemLogScrollPane.setVisible(false);
			barcodeTextField.requestFocusInWindow();
		}
		if(e.getSource() == receiveButton) {
			titleLabel.setText("Receive(+)");
			itemsTextArea.setText("Quantity\tIn Stock\tItem\n");
			scanQuantity.setValue(1);
			checkOutButton.setVisible(false);
			receiveButton.setVisible(false);
			editItemButton.setVisible(false);
			barcodeLabel.setVisible(true);
			scanQuantityLabel.setVisible(true);
			barcodeTextField.setVisible(true);
			scanSpinner.setVisible(true);
			doneButton.setVisible(true);
			newItemButton.setVisible(true);
			itemsTextArea.setVisible(true);
			itemsScrollPane.setVisible(true);
			itemLogScrollPane.setVisible(false);
			barcodeTextField.requestFocusInWindow();
		}
		if(e.getSource() == editItemButton) {
			chooseBarcode();
			editItem();
		}
		if(e.getSource() == editAddressButton) {
			itemLog = Integer.parseInt(itemLogTable.getValueAt(itemLogTable.getSelectedRow(), itemLogTable.getColumnCount() - 1).toString());
			editAddress(itemLog);
			sortByAndShow();
			if(itemLogTable.getRowCount() != 0) {
				itemLogTable.setRowSelectionInterval(0, 0);
			}
		}
		if(e.getSource() == doneButton) {
			// this also takes care of interactions with the done button for it to function like "Enter"
			if(!barcodeTextField.getText().isEmpty()) {
				barcodeTextField.requestFocusInWindow();
				try {
					Robot r = new Robot();
					r.keyPress(KeyEvent.VK_ENTER);
					r.keyRelease(KeyEvent.VK_ENTER);
				}
				catch(AWTException awte) {
					
				}
			}
			else if((Integer)scanSpinner.getValue() != 1) {
				barcodeTextField.requestFocusInWindow();
			}
			else {
				if(!namesList.isEmpty()) {
					chooseUser();
				}
			}
			barcodeTextField.requestFocusInWindow();
		}
		if(e.getSource() == printButton) {
			try {
				Desktop desktop = null;
				if(Desktop.isDesktopSupported()) {
					desktop = Desktop.getDesktop();
				}
				if(titleLabel.getText().equals("Restock")) {
					desktop.print(new File("Restock.csv"));
					JOptionPane.showMessageDialog(program, "Successfully printed restock list!", "Restock list printed", JOptionPane.PLAIN_MESSAGE);
				}
				/*
				else if(titleLabel.getText().equals("Item Log")) {
					File logFile = new File("Log.csv");
					if(logFile.length() != 0) {
						logFile.setWritable(true);
						desktop.print(logFile);
						JOptionPane.showMessageDialog(program, "Successfully printed item log!", "Item log printed", JOptionPane.PLAIN_MESSAGE);
						BufferedWriter bw = new BufferedWriter(new FileWriter(logFile));
						bw.write("");
						logFile.setWritable(false);
						bw.close();
						itemsTextArea.setText("Item Log: " + "\n\n");
					}
					else {
						JOptionPane.showMessageDialog(program, "There is nothing to print in the item log file.", "Could not print item log", JOptionPane.WARNING_MESSAGE);
					}
				}
				*/
			}
			catch(IOException ioe) {
				JOptionPane.showMessageDialog(program, "Could not print the specified document.", "Problem with printing", JOptionPane.WARNING_MESSAGE);
			}
		}
		if(e.getSource() == newItemButton) {
			createNewItem();
		}
		if(e.getSource() == undoButton) {
			String itemPopped = itemsStack.pop();
			String itemBarcode = itemPopped.substring(0, itemPopped.indexOf(","));
			String itemQuantity = itemPopped.substring(itemPopped.lastIndexOf(",") + 1);
			quantityList.replace(itemBarcode, quantityList.get(itemBarcode) - Integer.parseInt(itemQuantity));
			if(quantityList.get(itemBarcode) < 1) {
				quantityList.remove(itemBarcode);
				namesList.remove(itemBarcode);
				itemsOrder.remove(itemBarcode);
			}
			itemsTextArea.setText(updateItems());
			if(itemsStack.isEmpty()) {
				undoButton.setVisible(false);
			}
			scanQuantity.setValue(1);
			barcodeTextField.requestFocusInWindow();
		}
		if(e.getSource() == yearRB) {
			monthComboBox.setEnabled(false);
			dayComboBox.setEnabled(false);
		}
		if(e.getSource() == monthRB) {
			monthComboBox.setEnabled(true);
			dayComboBox.setEnabled(false);
		}
		if(e.getSource() == dayRB) {
			monthComboBox.setEnabled(true);
			dayComboBox.setEnabled(true);
		}
	}
	Action barcodeScanned = new AbstractAction(){
		
		/**
		 * This method takes care of what happens when you scan a barcode
		 */
		
	    @Override
	    public void actionPerformed(ActionEvent e){
	    	barcodeInput = "";
			barcodeItem = new String[4];
	        barcodeInput = barcodeTextField.getText().trim();
	        barcodeTextField.setText("");
	        barcodeItem = findItem(barcodeInput);
			if(barcodeItem[0] == null) {
				JOptionPane.showMessageDialog(program, "Could not find the desired item. It is either not in the system or an invalid barcode.", "No match found", JOptionPane.WARNING_MESSAGE);
				barcodeTextField.requestFocusInWindow();
			}
			else {
				namesList.put(barcodeItem[0], barcodeItem[1]);
				itemsStack.push(barcodeItem[0] + "," + scanQuantity.getValue());
				undoButton.setVisible(true);
				if(quantityList.containsKey(barcodeItem[0])) {
					quantityList.replace(barcodeItem[0], quantityList.get(barcodeItem[0]) + (int)scanQuantity.getValue());
				}
				else {
					quantityList.put(barcodeItem[0], (int)scanQuantity.getValue());
					itemsOrder.add(barcodeItem[0]);
				}
				// this is assuming all the in-stock entries are numbers
				if(doneButton.isVisible()) {
					if(titleLabel.getText().equals("Check Out(-)") && quantityList.get(barcodeItem[0]) > Integer.parseInt(barcodeItem[2])) {
						JOptionPane.showMessageDialog(program, "Could not check out this item because there is no more stock.", "Exceed stock quantity", JOptionPane.WARNING_MESSAGE);
						quantityList.replace(barcodeItem[0], quantityList.get(barcodeItem[0]) - (int)scanQuantity.getValue());
						itemsStack.pop();
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
				scanQuantity.setValue(1);
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
