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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
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
	ArrayList<String> userLines = new ArrayList<String>();
	
	ArrayList<String> productLines = new ArrayList<String>();
	
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
	JLabel titleLabel, connectingToDBLabel, restockingLabel, barcodeLabel, itemSearchLabel, scanQuantityLabel;
	JTextArea itemsTextArea;
	JButton homeButton, restockButton, usersButton, reportsButton, itemLogButton, checkOutButton, receiveButton, newItemButton, editItemLogButton, deleteItemLogButton, doneButton, printButton, undoButton, editItemButton, viewKitsButton, viewItemsButton, usageReportButton, snapshotReportButton, productReportButton;
	JRadioButton yearRB, monthRB, dayRB, checkOutRB, receiveRB;
	ButtonGroup checkOutOrReceiveBG;
	JComboBox<Integer> yearComboBox, dayComboBox;
	JComboBox<String> monthComboBox;
	JTextField barcodeTextField, itemSearchTextField;
	JPasswordField passwordField, confirmPasswordField;
	JScrollPane itemsScrollPane, itemTableScrollPane, userScrollPane, sortByScrollPane, productScrollPane, kitScrollPane, kitItemTableScrollPane;
	SpinnerModel scanQuantity;
	JSpinner scanSpinner;
	JList<String> userList, sortByList, productList, kitList;
	JComponent scanEditor;
	JPanel loadingPanel, newItemPanel, editItemPanel, userPanel, sortByPanel, chooseDatePanel, productPanel, kitPanel;
	JProgressBar connectingToDBProgressBar = new JProgressBar();
	JTable itemTable, kitItemTable;
	DefaultTableModel dtm;
	
	String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMMM dd yyyy");
	DateTimeFormatter dtfSQL = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	int iterations = 65536;
	int keyLength = 512;
	int saltLength = 16;
	String hashAlgorithm = "PBKDF2WithHmacSHA512";
	SecureRandom sr = new SecureRandom();
	boolean passwordCorrect = false;
	
	String usernameDB, passwordDB;
	int restockingNumber;
	String user = null;
	String sortByName = "";
	String productName = null;
	String kitName = null;
	String kitItem = null;
	String reportType = "";
	LocalDateTime dateTime;
	int itemLog;
	int year, month, day;
	String dateOption;
	String[] sortBy = {"Past 4 Weeks", "Date", "User", "Product"};
	
	String barcodeInput = "";
	String[] barcodeItem = new String[5];
		
	InventoryApp(){
		
		/**
		 * This initializes the program (reads inventory, creates GUI, etc)
		 * 
		 */
		
	    setTitle("Inventory Management Program");
	    
	    /*
	    try {
	    	InputStream is = getClass().getResourceAsStream("/Secrets.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String fileLine;
	    	while((fileLine = br.readLine()) != null) {
	    		
	    	}
			br.close();
		}
		catch(IOException e) {
			JOptionPane.showMessageDialog(program, "There is a problem loading the secret data of the program.", "Could not load secret data", JOptionPane.WARNING_MESSAGE);
		}
		*/
	    
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
		connectingToDBLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/80));
		
		loadingPanel.add(connectingToDBLabel);
		loadingPanel.add(connectingToDBProgressBar);
				
	    titleLabel = new JLabel("Inventory Management");
	    titleLabel.setBounds(0, 0, screenSize.width, screenSize.height/10);
	    titleLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/40));
	    titleLabel.setAlignmentX(CENTER_ALIGNMENT);
	    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
	    
	    homeButton = new JButton("Home");
		homeButton.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.025), screenSize.width/5, screenSize.height/20);
		homeButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		homeButton.addMouseListener(this);
		
		reportsButton = new JButton("Reports");
		reportsButton.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.1), screenSize.width/5, screenSize.height/20);
		reportsButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		reportsButton.addMouseListener(this);
		
		restockButton = new JButton("Restock List");
		restockButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.1), screenSize.width/5, screenSize.height/20);
		restockButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		restockButton.addMouseListener(this);
		
		usersButton = new JButton("Users");
		usersButton.setBounds((int)(screenSize.width * 0.4), (int)(screenSize.height * 0.575), screenSize.width/5, screenSize.height/10);
		usersButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		usersButton.addMouseListener(this);
		
		itemLogButton = new JButton("Item Log");
		itemLogButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.025), screenSize.width/5, screenSize.height/20);
		itemLogButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		itemLogButton.addMouseListener(this);
		
		editItemLogButton = new JButton("Edit Item Log");
		editItemLogButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		editItemLogButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		editItemLogButton.addMouseListener(this);
		
		deleteItemLogButton = new JButton("Delete Item Log");
		deleteItemLogButton.setBounds((int)(screenSize.width * 0.55), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		deleteItemLogButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		deleteItemLogButton.addMouseListener(this);
						
		itemsTextArea = new JTextArea("Quantity\tIn Stock\tItem\n");
	    itemsTextArea.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
	    itemsTextArea.setEditable(false);
	    
	    itemsScrollPane = new JScrollPane(itemsTextArea);
	    itemsScrollPane.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.175), (int)(screenSize.width * 0.9), (int)(screenSize.height * 0.575));
	    itemsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
	    
	    dtm = new DefaultTableModel() {
			@Override
		    public boolean isCellEditable(int row, int column) {
		       return false;
		    }
		};
	    
	    itemTable = new JTable();
		itemTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, screenSize.width/96));
		itemTable.setFont(new Font("Arial", Font.BOLD, screenSize.width/96));
		itemTable.setRowHeight(screenSize.width/50);
		itemTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		itemTable.setModel(dtm);
		itemTable.addMouseListener(this);
		
		kitItemTable = new JTable();
		kitItemTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, screenSize.width/96));
		kitItemTable.setFont(new Font("Arial", Font.BOLD, screenSize.width/96));
		kitItemTable.setRowHeight(screenSize.width/50);
		kitItemTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		kitItemTable.addMouseListener(this);
	    
	    itemTableScrollPane = new JScrollPane();
		itemTableScrollPane.setViewportView(itemTable);
	    itemTableScrollPane.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.175), (int)(screenSize.width * 0.9), (int)(screenSize.height * 0.575));
	    itemTableScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
	    itemTableScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
	    
	    kitItemTableScrollPane = new JScrollPane();
		kitItemTableScrollPane.setViewportView(kitItemTable);
	    kitItemTableScrollPane.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.175), (int)(screenSize.width * 0.9), (int)(screenSize.height * 0.575));
	    kitItemTableScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
	    kitItemTableScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
	    
		checkOutButton = new JButton("Check Out(-)");
		checkOutButton.setBounds((int)(screenSize.width * 0.15), (int)(screenSize.height * 0.45), screenSize.width/5, screenSize.height/10);
		checkOutButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		checkOutButton.addMouseListener(this);
		
		receiveButton = new JButton("Receive(+)");
		receiveButton.setBounds((int)(screenSize.width * 0.65), (int)(screenSize.height * 0.45), screenSize.width/5, screenSize.height/10);
		receiveButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		receiveButton.addMouseListener(this);
		
		editItemButton = new JButton("Edit Item");
		editItemButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		editItemButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		editItemButton.addMouseListener(this);
		
		viewKitsButton = new JButton("View Kits");
		viewKitsButton.setBounds((int)(screenSize.width * 0.55), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		viewKitsButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		viewKitsButton.addMouseListener(this);
		
		viewItemsButton = new JButton("View Items");
		viewItemsButton.setBounds((int)(screenSize.width * 0.4), (int)(screenSize.height * 0.7), screenSize.width/5, screenSize.height/10);
		viewItemsButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		viewItemsButton.addMouseListener(this);
		
		doneButton = new JButton("Done");
		doneButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		doneButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		doneButton.addMouseListener(this);
		
		newItemButton = new JButton("New Item");
		newItemButton.setBounds((int)(screenSize.width * 0.35), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		newItemButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		newItemButton.addMouseListener(this);
		
		printButton = new JButton("Print");
		printButton.setBounds((int)(screenSize.width * 0.75), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		printButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		printButton.addMouseListener(this);
		
		undoButton = new JButton("Undo");
		undoButton.setBounds((int)(screenSize.width * 0.55), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		undoButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		undoButton.addMouseListener(this);
		
		usageReportButton = new JButton("Usage");
		usageReportButton.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		usageReportButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		usageReportButton.addMouseListener(this);
		
		snapshotReportButton = new JButton("Snapshot");
		snapshotReportButton.setBounds((int)(screenSize.width * 0.25), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		snapshotReportButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		snapshotReportButton.addMouseListener(this);
		
		productReportButton = new JButton("Product");
		productReportButton.setBounds((int)(screenSize.width * 0.45), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		productReportButton.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		productReportButton.addMouseListener(this);
		
		restockingLabel = new JLabel(restockingNumber + " item(s) needs restocking", SwingConstants.CENTER);
		restockingLabel.setBounds((int)(screenSize.width * 0.4), (int)(screenSize.height * 0.075), screenSize.width/5, screenSize.height/10);
		restockingLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		
		barcodeLabel = new JLabel("Barcode:");
		barcodeLabel.setBounds((int)(screenSize.width * 0.15), (int)(screenSize.height * 0.75), screenSize.width/5, screenSize.height/20);
		barcodeLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		
		barcodeTextField = new JTextField();
		barcodeTextField.setBounds((int)(screenSize.width * 0.15), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		barcodeTextField.setFont(new Font("Arial", Font.PLAIN, screenSize.width/40));
		barcodeTextField.addActionListener(barcodeScanned);
		
		itemSearchLabel = new JLabel("Search:");
		itemSearchLabel.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.75), screenSize.width/5, screenSize.height/20);
		itemSearchLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		
		itemSearchTextField = new JTextField();
		itemSearchTextField.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		itemSearchTextField.setFont(new Font("Arial", Font.PLAIN, screenSize.width/40));
		itemSearchTextField.addActionListener(itemSearched);
		itemSearchTextField.addMouseListener(this);
		
		passwordField = new JPasswordField();
		passwordField.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
		
		confirmPasswordField = new JPasswordField();
		confirmPasswordField.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));

		
		scanQuantityLabel = new JLabel("Quantity:");
		scanQuantityLabel.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.75), screenSize.width/5, screenSize.height/20);
		scanQuantityLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		
		scanQuantity = new SpinnerNumberModel(1, 1, 1000, 1);
		scanSpinner = new JSpinner(scanQuantity);
		scanSpinner.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.8), screenSize.width/10, screenSize.height/10);
		scanSpinner.setFont(new Font("Arial", Font.PLAIN, screenSize.width/40));
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
		
		checkOutRB = new JRadioButton("Check Out(-)");
		checkOutRB.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		checkOutRB.setBounds((int)(screenSize.width * 0.05), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		checkOutRB.addMouseListener(this);
		
		receiveRB = new JRadioButton("Receive(+)");
		receiveRB.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
		receiveRB.setBounds((int)(screenSize.width * 0.25), (int)(screenSize.height * 0.8), screenSize.width/5, screenSize.height/10);
		receiveRB.addMouseListener(this);
		
		checkOutOrReceiveBG = new ButtonGroup();
		checkOutOrReceiveBG.add(checkOutRB);
		checkOutOrReceiveBG.add(receiveRB);
		
		checkOutRB.setSelected(true);
		
		// add elements to GUI
		add(connectingToDBProgressBar);
		add(loadingPanel);
		add(titleLabel);
		add(homeButton);
		add(reportsButton);
		add(usersButton);
		add(restockButton);
		add(itemLogButton);
		add(checkOutButton);
		add(receiveButton);
		add(editItemButton);
		add(viewItemsButton);
		add(viewKitsButton);
		add(restockingLabel);
		add(barcodeLabel);
		add(scanQuantityLabel);
		add(scanSpinner);
		add(barcodeTextField);
		add(itemSearchLabel);
		add(itemSearchTextField);
		add(doneButton);
		add(printButton);
		add(newItemButton);
		add(editItemLogButton);
		add(deleteItemLogButton);
		add(undoButton);
		add(usageReportButton);
		add(snapshotReportButton);
		add(productReportButton);
		add(itemsScrollPane);
		add(itemTableScrollPane);
		add(checkOutRB);
		add(receiveRB);
		
		// show GUI home page on screen
		barcodeLabel.setVisible(false);
		scanQuantityLabel.setVisible(false);
		barcodeTextField.setVisible(false);
		itemSearchLabel.setVisible(false);
		itemSearchTextField.setVisible(false);
		scanSpinner.setVisible(false);
		doneButton.setVisible(false);
		printButton.setVisible(false);
		newItemButton.setVisible(false);
		viewKitsButton.setVisible(false);
		editItemButton.setVisible(false);
		editItemLogButton.setVisible(false);
		deleteItemLogButton.setVisible(false);
		checkOutRB.setVisible(false);
		receiveRB.setVisible(false);
		undoButton.setVisible(false);
		usageReportButton.setVisible(false);
		snapshotReportButton.setVisible(false);
		productReportButton.setVisible(false);
		itemsTextArea.setVisible(false);
		itemsScrollPane.setVisible(false);
		itemTableScrollPane.setVisible(false);
		setLayout(null);
		setVisible(true);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		/*
		this.addComponentListener(new ComponentAdapter() {
		    public void componentResized(ComponentEvent componentEvent) {
		        System.out.println(getBounds().width + " " + getBounds().height);
		    }
		});
		*/
	}
	public static void main(String[] args) {
		UIManager.put("OptionPane.messageFont", new Font("Arial", Font.BOLD, Toolkit.getDefaultToolkit().getScreenSize().width/69));
		UIManager.put("OptionPane.buttonFont", new Font("Arial", Font.BOLD, Toolkit.getDefaultToolkit().getScreenSize().width/69));
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
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;"); // try to connect with your attributes 
	    } catch (ClassNotFoundException e) { // 
	    	JOptionPane.showMessageDialog(program, "There is a problem loading the JDBC driver.", "Could not load driver", JOptionPane.WARNING_MESSAGE);	    
	    } catch (SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem connecting to the database.\n" + e.toString(), "Could not connect to DB", JOptionPane.WARNING_MESSAGE);
	    }
		finally {
			try { conn.close(); } catch (Exception e) { /* ignored */ }
		}
		connectingToDBProgressBar.setVisible(false);
		loadingPanel.setVisible(false);

	}
	
	public Optional<String> generateSalt (int length){
		byte[] salt = new byte[length];
		sr.nextBytes(salt);
		
		return Optional.of(Base64.getEncoder().encodeToString(salt));
	}
	
	public Optional<String> hashPassword(String password, String salt) {
		char[] chars = password.toCharArray();
		byte[] bytes = salt.getBytes();
		PBEKeySpec spec = new PBEKeySpec(chars, bytes, iterations, keyLength);
		Arrays.fill(chars, Character.MIN_VALUE);
		try {
			SecretKeyFactory fac = SecretKeyFactory.getInstance(hashAlgorithm);
			byte[] securePassword = fac.generateSecret(spec).getEncoded();
			return Optional.of(Base64.getEncoder().encodeToString(securePassword));
		}
		catch(NoSuchAlgorithmException | InvalidKeySpecException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem hashing the password.", "Could not hash password", JOptionPane.WARNING_MESSAGE);	    
	    	return Optional.empty();
		}
		finally {
			spec.clearPassword();
		}
	}
	
	public boolean verifyPassword(String password, String key, String salt) {
		Optional<String> optEncrypted = hashPassword(password.toUpperCase(), salt);
		if(!optEncrypted.isPresent()) {
			return false;
		}
		return optEncrypted.get().equals(key);
	}
	
	public void createNewItem() {
		
		/**
		 * This method is for adding new items into the Inventory database
		 * 
		 */
		
		JTextField barcode = new JTextField(20);
		barcode.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
		JTextField name = new JTextField(20);
		name.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
		SpinnerModel inStockQuantity = new SpinnerNumberModel(0, 0, 1000, 1);
		SpinnerModel restockQuantity = new SpinnerNumberModel(0, 0, 1000, 1);
		
		JSpinner inStock = new JSpinner(inStockQuantity);
		inStock.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
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
		restock.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
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
		
		JCheckBox kitCheckBox = new JCheckBox("Kit");
		kitCheckBox.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		kitCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
		kitCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(kitCheckBox.isSelected()) {
					inStock.setEnabled(false);
					restock.setEnabled(false);
				}
				else {
					inStock.setEnabled(true);
					restock.setEnabled(true);
				}
			}
		});
		
		JPanel newItemPanel = new JPanel();
		newItemPanel.setLayout(new GridLayout(5,2));
		JLabel nameLabel = new JLabel("Name");
		nameLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		newItemPanel.add(nameLabel);
		newItemPanel.add(name);
		JLabel bcLabel = new JLabel("Barcode");
		bcLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		newItemPanel.add(kitCheckBox);
		newItemPanel.add(new JLabel(""));
		newItemPanel.add(bcLabel);
		newItemPanel.add(barcode);
		JLabel inStockLabel = new JLabel("In Stock Quantity");
		inStockLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		newItemPanel.add(inStockLabel);
		newItemPanel.add(inStock);
		JLabel restockLabel = new JLabel("Restock Quantity");
		restockLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		newItemPanel.add(restockLabel);
		newItemPanel.add(restock);
		
		int result;
		boolean validNewItem = false, cancel = false;
		while(!validNewItem && !cancel) {
			// makes it so the barcode text field is in focus
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
			result = JOptionPane.showConfirmDialog(program, newItemPanel, 
					"Please enter the item description:", JOptionPane.OK_CANCEL_OPTION);
			if(result == JOptionPane.OK_OPTION && !barcode.getText().isEmpty() && !name.getText().isEmpty()) {
				int matchingName = 0;
				try {	
			        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
					s = conn.createStatement();
					// get table size
			        String sqlStatement = "SELECT COUNT(p_id) FROM product WHERE p_name = '" + name.getText().trim() + "';";
			        r = s.executeQuery(sqlStatement);
			        while(r.next()) {
			        	matchingName = Integer.parseInt(r.getString(1));
			        }
				}
				catch(SQLException e) {
			    	JOptionPane.showMessageDialog(program, "There is a problem finding the items in the database.\n" + e.toString(), "Could not find items in DB", JOptionPane.WARNING_MESSAGE);
				}
		 		finally {
					try { s.close(); } catch (Exception e) { /* ignored */ }
					try { r.close(); } catch (Exception e) { /* ignored */ }
					try { conn.close(); } catch (Exception e) { /* ignored */ }
				}
				if(matchingName != 0) {
					JOptionPane.showMessageDialog(program, "The name you entered already exists for another item. Please enter another one.", "Duplicate name", JOptionPane.WARNING_MESSAGE);
					barcode.setText("");
					barcode.requestFocusInWindow();
				}
				else if(findItem(barcode.getText().trim())[0] != null) {
					JOptionPane.showMessageDialog(program, "The barcode you entered already exists for another item. Please enter another one.", "Duplicate barcode", JOptionPane.WARNING_MESSAGE);
					barcode.setText("");
					barcode.requestFocusInWindow();
				}
				else if(barcode.getText().trim().length() > 30) {
					JOptionPane.showMessageDialog(program, "The barcode must be 30 characters or less. Please enter another one.", "Barcode too long", JOptionPane.WARNING_MESSAGE);
					barcode.setText("");
					barcode.requestFocusInWindow();
				}
				else if(name.getText().trim().length() > 300) {
					JOptionPane.showMessageDialog(program, "The name must be 300 characters or less. Please enter another one.", "Name too long", JOptionPane.WARNING_MESSAGE);
					barcode.setText("");
					barcode.requestFocusInWindow();
				}
				else {
					if(kitCheckBox.isSelected()) {
						try {
					        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
							s = conn.createStatement();
							String sqlStatement;
					        sqlStatement = "INSERT INTO product VALUES ('" + barcode.getText().trim() + "','" + name.getText() + "', 0, 0, 1);";
					        s.execute(sqlStatement);
					        updateRestockingNumber();
							if(titleLabel.getText().equals("View Items")) {
								itemSearchTextField.setText(name.getText());
								refreshItemTable();
							}
							else {
								barcodeTextField.requestFocusInWindow();
							}
							validNewItem = true;
						}
						catch(SQLException e) {
					    	JOptionPane.showMessageDialog(program, "There is a problem adding the kit into the database.\n" + e.toString(), "Could not add kit into DB", JOptionPane.WARNING_MESSAGE);
						}
						finally {
							try { s.close(); } catch (Exception e) { /* ignored */ }
							try { r.close(); } catch (Exception e) { /* ignored */ }
							try { conn.close(); } catch (Exception e) { /* ignored */ }
						}
						kitName = name.getText();
						editKit();
					}
					else {
						chooseUser();
						if(user != null) {
							enterPassword();
							if(passwordCorrect) {
								try {
							        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
									s = conn.createStatement();
									String sqlStatement;
									sqlStatement = "INSERT INTO product VALUES ('" + barcode.getText().trim() + "','" + name.getText() + "'," + inStock.getValue() + "," + restock.getValue() + ", 0);";
							        s.execute(sqlStatement);
							        updateRestockingNumber();
									updateLog("add", (Integer)inStock.getValue(), barcode.getText().trim());
									if(titleLabel.getText().equals("View Items")) {
										refreshItemTable();
									}
									else {
										barcodeTextField.requestFocusInWindow();
									}
									validNewItem = true;
									itemSearchTextField.setText(name.getText());
									refreshItemTable();
								}
								catch(SQLException e) {
							    	JOptionPane.showMessageDialog(program, "There is a problem adding the item into the database.\n" + e.toString(), "Could not add item into DB", JOptionPane.WARNING_MESSAGE);
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
		if(titleLabel.getText().equals("View Items")) {
			itemSearchTextField.requestFocusInWindow();
		}
		else if(titleLabel.getText().equals("Receive(+)")) {
			barcodeTextField.requestFocusInWindow();
		}
	}
	public void editUsers() {
		
		/**
		 * This method is used to manage the users of the system
		 */
		
		boolean exitEditUser = false;
		while(!exitEditUser) {
			user = null;
			userLines.clear();
			try {	
		        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
				s = conn.createStatement();
		        String sqlStatement = "SELECT e_fname, e_lname FROM employee WHERE active = 1 ORDER BY e_fname;";
		        r = s.executeQuery(sqlStatement);
		        while(r.next()) {
		        	userLines.add(r.getString(1) + " " + r.getString(2));
		        }
			}
			catch(SQLException e) {
		    	JOptionPane.showMessageDialog(program, "There is a problem finding the users in the database.\n" + e.toString(), "Could not find users in DB", JOptionPane.WARNING_MESSAGE);
			}
			finally {
				try { s.close(); } catch (Exception e) { /* ignored */ }
				try { r.close(); } catch (Exception e) { /* ignored */ }
				try { conn.close(); } catch (Exception e) { /* ignored */ }
			}
			String[] users = new String[userLines.size()];
			users =	userLines.toArray(users);
			userList = new JList<String>(users);
			userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			userList.setLayoutOrientation(JList.VERTICAL);
			userList.setVisibleRowCount(-1);
			userList.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
			userList.setSelectedIndex(0);
			
			userScrollPane = new JScrollPane(userList);
			userScrollPane.setViewportView(userList);
			userScrollPane.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
		    userScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		    userScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			
			userPanel = new JPanel();
			userPanel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
			userScrollPane.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
			userPanel.add(userScrollPane);
			
			String[] options = {"New User", "Change Password", "Delete User", "Cancel"};
			int result;
			result = JOptionPane.showOptionDialog(program, userPanel, "Edit Users", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, null);
			if(result == 0) {
				JPanel userInputPanel = new JPanel();
				JLabel newUserLabel = new JLabel("Please enter the new user details:");
				newUserLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
				JLabel newUserFNameLabel = new JLabel("First name:");
				newUserFNameLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
				JLabel newUserLNameLabel = new JLabel("Last name:");
				newUserLNameLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
				JTextField userFNameTextField = new JTextField();
				userFNameTextField.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
				JTextField userLNameTextField = new JTextField();
				userLNameTextField.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
				JLabel passwordLabel = new JLabel("Password (at least 4 characters):");
				passwordLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
				JLabel confirmPasswordLabel = new JLabel("Confirm password:");
				confirmPasswordLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
				JCheckBox adminCheckBox = new JCheckBox("Admin:");
				adminCheckBox.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
				adminCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
				
				userInputPanel.setLayout(new GridLayout(10,1));
				
				userInputPanel.add(newUserLabel);
				userInputPanel.add(newUserFNameLabel);
				userInputPanel.add(userFNameTextField);
				userInputPanel.add(newUserLNameLabel);
				userInputPanel.add(userLNameTextField);
				userInputPanel.add(adminCheckBox);
				userInputPanel.add(passwordLabel);
				userInputPanel.add(passwordField);
				userInputPanel.add(confirmPasswordLabel);
				userInputPanel.add(confirmPasswordField);
				boolean validUser = false;
				while(!validUser) {
					passwordField.setText("");
					confirmPasswordField.setText("");
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
					userResult = JOptionPane.showConfirmDialog(program, userInputPanel, "New User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
					
					StringBuilder fNameSB = new StringBuilder(userFNameTextField.getText().trim());
					StringBuilder lNameSB = new StringBuilder(userLNameTextField.getText().trim());
					
					fNameSB.replace(0, fNameSB.length(), fNameSB.toString().toLowerCase());
					lNameSB.replace(0, lNameSB.length(), lNameSB.toString().toLowerCase());
					
					if(fNameSB.length() != 0 && lNameSB.length() != 0) {
						fNameSB.setCharAt(0, Character.toTitleCase(fNameSB.charAt(0)));
						lNameSB.setCharAt(0, Character.toTitleCase(lNameSB.charAt(0)));
					}					
					if(userResult == JOptionPane.OK_OPTION && (userFNameTextField.getText().trim().isEmpty() || userLNameTextField.getText().trim().isEmpty())) {
						JOptionPane.showMessageDialog(program, "The user fields cannot be blank.", "Problem with creating user", JOptionPane.WARNING_MESSAGE);
					}
					else if(userResult == JOptionPane.OK_OPTION && (!userFNameTextField.getText().trim().matches("[a-zA-Z]+") || !userLNameTextField.getText().trim().matches("[a-zA-Z]+"))) {
						JOptionPane.showMessageDialog(program, "The user fields cannot contain characters other than letters.", "Problem with creating user", JOptionPane.WARNING_MESSAGE);
					}
					else if(userResult == JOptionPane.OK_OPTION && (userFNameTextField.getText().trim().toLowerCase().equals("admin") || userLNameTextField.getText().trim().toLowerCase().equals("admin"))) {
						JOptionPane.showMessageDialog(program, "This user cannot be named \"admin\".", "Problem with creating user", JOptionPane.WARNING_MESSAGE);
					}
					else if(userResult == JOptionPane.OK_OPTION && userLines.contains(fNameSB.toString() + " " + lNameSB.toString())) {
						JOptionPane.showMessageDialog(program, "This user is already in the database.", "Problem with creating user", JOptionPane.WARNING_MESSAGE);
					}
					else if(userResult == JOptionPane.OK_OPTION && !(Arrays.equals(passwordField.getPassword(), confirmPasswordField.getPassword()))) {
						JOptionPane.showMessageDialog(program, "The passwords do not match.", "Problem with creating user", JOptionPane.WARNING_MESSAGE);
					}
					else if(userResult == JOptionPane.OK_OPTION && passwordField.getPassword().length < 4) {
						JOptionPane.showMessageDialog(program, "The password must have at least 4 characters.", "Problem with creating user", JOptionPane.WARNING_MESSAGE);
					}
					else if(userResult == JOptionPane.OK_OPTION && userFNameTextField.getText().trim().length() > 30) {
						JOptionPane.showMessageDialog(program, "The first name must be 30 characters or less.", "Problem with creating user", JOptionPane.WARNING_MESSAGE);
					}
					else if(userResult == JOptionPane.OK_OPTION && userLNameTextField.getText().trim().length() > 30) {
						JOptionPane.showMessageDialog(program, "The last name must be 30 characters or less.", "Problem with creating user", JOptionPane.WARNING_MESSAGE);
					}
					else if(userResult == JOptionPane.OK_OPTION && !userFNameTextField.getText().trim().isEmpty() && !userLNameTextField.getText().trim().isEmpty()) {
						String userFName = fNameSB.toString();
						String userLName = lNameSB.toString();
						String userSalt = generateSalt(saltLength).get();
						String userPasswordHash = hashPassword(String.valueOf(passwordField.getPassword()).toUpperCase(), userSalt).get();
						if(!adminCheckBox.isSelected()) {
							try {
						        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
								s = conn.createStatement();
						        String sqlStatement = "INSERT INTO employee VALUES ('" + userLName + "', '" + userFName + "', '" + userSalt + "', '" + userPasswordHash + "', 0, 1);";
						        s.execute(sqlStatement);
								JOptionPane.showMessageDialog(program, "Successfully created user!", "User creation complete", JOptionPane.PLAIN_MESSAGE);
							}
							catch(SQLException e) {
						    	JOptionPane.showMessageDialog(program, "There is a problem adding the user into the database.\n" + e.toString(), "Could not add user into DB", JOptionPane.WARNING_MESSAGE);
							}
							finally {
								try { s.close(); } catch (Exception e) { /* ignored */ }
								try { r.close(); } catch (Exception e) { /* ignored */ }
								try { conn.close(); } catch (Exception e) { /* ignored */ }
							}
						}
						else {
							enterAdminPassword();
							if(passwordCorrect) {
								try {
							        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
									s = conn.createStatement();
							        String sqlStatement = "INSERT INTO employee VALUES ('" + userLName + "', '" + userFName + "', '" + userSalt + "', '" + userPasswordHash + "', 1, 1);";
							        s.execute(sqlStatement);
									JOptionPane.showMessageDialog(program, "Successfully created administrator!", "Administrator creation complete", JOptionPane.PLAIN_MESSAGE);
								}
								catch(SQLException e) {
							    	JOptionPane.showMessageDialog(program, "There is a problem adding the admin user into the database.\n" + e.toString(), "Could not add admin user into DB", JOptionPane.WARNING_MESSAGE);
								}
								finally {
									try { s.close(); } catch (Exception e) { /* ignored */ }
									try { r.close(); } catch (Exception e) { /* ignored */ }
									try { conn.close(); } catch (Exception e) { /* ignored */ }
								}
							}
						}
						validUser = true;
					}
					else {
						validUser = true;
					}
				}
			}
			else if(result == 1) {
				JPanel userInputPanel = new JPanel();
				JLabel newUserLabel = new JLabel("Please enter the new user details:");
				newUserLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/50));
				JLabel newUserFNameLabel = new JLabel("First name:");
				newUserFNameLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
				JLabel newUserLNameLabel = new JLabel("Last name:");
				newUserLNameLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
				JTextField userFNameTextField = new JTextField();
				userFNameTextField.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
				JTextField userLNameTextField = new JTextField();
				userLNameTextField.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
				JLabel passwordLabel = new JLabel("Password (at least 4 characters):");
				passwordLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
				JLabel confirmPasswordLabel = new JLabel("Confirm password:");
				confirmPasswordLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
				userInputPanel.setLayout(new GridLayout(9,1));
				userInputPanel.add(newUserLabel);
				userInputPanel.add(newUserFNameLabel);
				userInputPanel.add(userFNameTextField);
				userInputPanel.add(newUserLNameLabel);
				userInputPanel.add(userLNameTextField);
				userInputPanel.add(passwordLabel);
				userInputPanel.add(passwordField);
				userInputPanel.add(confirmPasswordLabel);
				userInputPanel.add(confirmPasswordField);
				userFNameTextField.setEditable(false);
				userLNameTextField.setEditable(false);

				user = userList.getSelectedValue();
				String[] userParts = new String[2];
				userParts = user.split("\\s+");
				
				userFNameTextField.setText(userParts[0]);
				userLNameTextField.setText(userParts[1]);
				
				boolean validPasswordChange = false;
				while(!validPasswordChange) {
					passwordField.setText("");
					confirmPasswordField.setText("");
					passwordField.addAncestorListener(new AncestorListener() {     
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
					while(!validPasswordChange) {
						userResult = JOptionPane.showConfirmDialog(program, userInputPanel, "Change Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
											
						if(userResult == JOptionPane.OK_OPTION && !(Arrays.equals(passwordField.getPassword(), confirmPasswordField.getPassword()))) {
							JOptionPane.showMessageDialog(program, "The passwords do not match.", "Problem with creating user", JOptionPane.WARNING_MESSAGE);
						}
						else if(userResult == JOptionPane.OK_OPTION && passwordField.getPassword().length < 4) {
							JOptionPane.showMessageDialog(program, "The password must have at least 4 characters.", "Problem with creating user", JOptionPane.WARNING_MESSAGE);
						}
						else if(userResult == JOptionPane.CANCEL_OPTION || userResult == JOptionPane.CLOSED_OPTION) {
							validPasswordChange = true;
						}
						else {
							enterAdminPassword();
							if(passwordCorrect) {
								String userSalt = generateSalt(saltLength).get();
								String userPasswordHash = hashPassword(String.valueOf(confirmPasswordField.getPassword()).toUpperCase(), userSalt).get();
								try {	
							        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
									s = conn.createStatement();
							        String sqlStatement = "UPDATE employee SET salt= '" + userSalt + "', password_hash= '" + userPasswordHash + "' WHERE e_fname = '" + userParts[0] + "' AND e_lname = '" + userParts[1] + "';";
							        s.execute(sqlStatement);
									JOptionPane.showMessageDialog(program, "Successfully changed password!", "Password change complete", JOptionPane.PLAIN_MESSAGE);
								}
								catch(SQLException e) {
							    	JOptionPane.showMessageDialog(program, "There is a problem editing the password in the database.\n" + e.toString(), "Could not edit password in DB", JOptionPane.WARNING_MESSAGE);
								}
								finally {
									try { s.close(); } catch (Exception e) { /* ignored */ }
									try { r.close(); } catch (Exception e) { /* ignored */ }
									try { conn.close(); } catch (Exception e) { /* ignored */ }
								}
							}
							validPasswordChange = true;
						}
					}
				}
			}
			else if(result == 2) {
				user = userList.getSelectedValue();
				String[] userParts = new String[2];
				userParts = user.split("\\s+");
				if(user != null) {
					int choice = JOptionPane.showConfirmDialog(	program, 
							("Are you sure you want to delete " + user + "?"), "Delete User", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if(choice == 0) {
						enterAdminPassword();
						if(passwordCorrect) {
							try {
						        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
								s = conn.createStatement();
							    String sqlStatement = "UPDATE employee SET active = 0 WHERE e_lname = '" + userParts[1] + "' AND e_fname = '" + userParts[0] + "';";
							    s.execute(sqlStatement);
								JOptionPane.showMessageDialog(program, "Successfully deleted user!", "User deletion complete", JOptionPane.PLAIN_MESSAGE);
							}
							catch(SQLException e){
						    	JOptionPane.showMessageDialog(program, "There is a problem deleting the user from the database.\n" + e.toString(), "Could not delete user from DB", JOptionPane.WARNING_MESSAGE);
							}
							finally {
								try { s.close(); } catch (Exception e) { /* ignored */ }
								try { r.close(); } catch (Exception e) { /* ignored */ }
								try { conn.close(); } catch (Exception e) { /* ignored */ }
							}
						}
					}
				}
				else {
					JOptionPane.showMessageDialog(program, "Could not delete user because there are no users.", "Problem with deleting user", JOptionPane.WARNING_MESSAGE);
				}
			}
			else {
				exitEditUser = true;
			}
		}

		if(titleLabel.getText().equals("View Items")) {
			itemSearchTextField.requestFocusInWindow();
		}
		else if(titleLabel.getText().equals("Receive(+)")) {
			barcodeTextField.requestFocusInWindow();
		}
	}
	public int getEmployeeID(String userName) {
		int eid = 1;
		if(userName != null) {
			String[] userParts = new String[2];
			userParts = userName.split("\\s+");
			try {	
		        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
				s = conn.createStatement();
		        String sqlStatement;
		        sqlStatement = "SELECT e_id FROM employee WHERE e_fname = '" + userParts[0] + "' AND e_lname = '" + userParts[1] + "';";
		        r = s.executeQuery(sqlStatement);
		        while(r.next()) {
		        	eid = Integer.parseInt(r.getString(1));
		        }
			}
			catch(SQLException e) {
		    	JOptionPane.showMessageDialog(program, "There is a problem finding the employee id from the database.\n" + e.toString(), "Could not get employee id from DB", JOptionPane.WARNING_MESSAGE);
			}
			finally {
				try { s.close(); } catch (Exception e) { /* ignored */ }
				try { r.close(); } catch (Exception e) { /* ignored */ }
				try { conn.close(); } catch (Exception e) { /* ignored */ }
			}
		}
		return eid;
	}
	public String getProductID(String productName) {
		String pid = "";
		try {	
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
			s = conn.createStatement();
	        String sqlStatement;
	        sqlStatement = "SELECT p_id FROM product WHERE p_name = '" + productName + "';";
	        r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	pid = r.getString(1);
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem finding the product id from the database.\n" + e.toString(), "Could not get product id from DB", JOptionPane.WARNING_MESSAGE);
		}
		finally {
			try { s.close(); } catch (Exception e) { /* ignored */ }
			try { r.close(); } catch (Exception e) { /* ignored */ }
			try { conn.close(); } catch (Exception e) { /* ignored */ }
		}
		return pid;
	}
	public void chooseBarcodeOrKeyword() {
		
		/**
		 * This method is for editing the attributes of items found in the Inventory database
		 * 
		 */
		
		barcodeInput = "";
		barcodeItem = new String[5];
				
		boolean validItem = false;
		while(!validItem) {
			JPanel barcodeInputPanel = new JPanel();
			JLabel editItemLabel = new JLabel("Please enter the barcode or keyword of the item:");
			editItemLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
			JTextField barcodeTextField = new JTextField();
			barcodeTextField.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
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
			barcodeResult = JOptionPane.showConfirmDialog(program, barcodeInputPanel, "Choose Barcode Or Keyword", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
			if(barcodeResult == JOptionPane.OK_OPTION) {
				barcodeInput = barcodeTextField.getText().trim();
				barcodeTextField.setText("");
	        	barcodeItem = findItem(barcodeInput);
	        	if(barcodeItem[0] == null) {
					searchKeywords();
					barcodeTextField.requestFocusInWindow();
				}
	        	if(barcodeItem[0] != null) {
	        		if(titleLabel.getText().equals("Check Out(-) Item Log") || titleLabel.getText().equals("Receive(+) Item Log")) {
		        		sortByAndShow();
	        		}
	        		// if you are getting a kit item
	        		if(titleLabel.getText().equals("View Items")) {
	        			kitItem = barcodeItem[1];
	        		}
		        	validItem = true;
	        	}
			}
			if(barcodeResult == JOptionPane.CANCEL_OPTION || barcodeResult == JOptionPane.CLOSED_OPTION) {
				validItem = true;
			}
		}
	}
	public void chooseDate() {
		
		dateTime = LocalDateTime.now();
		chooseDatePanel = new JPanel();
		chooseDatePanel.setLayout(new GridLayout(3,2));
		int currentYear, currentMonth, currentDay, earliestYear;
		currentYear = LocalDateTime.now().getYear();
		currentMonth = LocalDateTime.now().getMonthValue();
		currentDay = LocalDateTime.now().getDayOfMonth();
		earliestYear = currentYear;
		year = currentYear;
		month = currentMonth;
		day = currentDay;
		yearRB = new JRadioButton("Year");
		yearRB.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		monthRB = new JRadioButton("Month");
		monthRB.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		dayRB = new JRadioButton("Day");
		dayRB.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		ButtonGroup bg = new ButtonGroup();
		bg.add(yearRB);
		bg.add(monthRB);
		bg.add(dayRB);
		if(titleLabel.getText().equals("Reports") && reportType.equals("Snapshot")) {
			dayRB.setSelected(true);
		}
		else {
			monthRB.setSelected(true);

		}
		yearComboBox = new JComboBox<Integer>();
		yearComboBox.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
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
		monthComboBox.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
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
		dayComboBox.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		dayRB.addActionListener(new ActionListener() {
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
		for(int i = 0 ; i < 31; i ++) {
			dayComboBox.addItem(i + 1);
		}
		if(titleLabel.getText().equals("Reports") && reportType.equals("Snapshot")) {
			dayComboBox.setEnabled(true);
		}
		else {
			dayComboBox.setEnabled(false);
		}
		try {	
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
			s = conn.createStatement();
		    String sqlStatement = "SELECT MIN(YEAR(co_date)) FROM checkout;";
		    r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	if(r.getString(1) != null) {
	        		earliestYear = Integer.parseInt(r.getString(1));
	        
	        	}
	        }
	        for(int i = earliestYear; i <= currentYear; i++) {
	        	yearComboBox.addItem(i);
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem detecting the years available.\n" + e.toString(), "Could not find years from DB", JOptionPane.WARNING_MESSAGE);
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
		if(titleLabel.getText().equals("Reports") && reportType.equals("Snapshot")) {
			dayComboBox.setSelectedIndex(0);
		}
		else {
			dayComboBox.setSelectedItem(currentDay);
		}
		yearRB.addMouseListener(this);
		monthRB.addMouseListener(this);
		dayRB.addMouseListener(this);
		chooseDatePanel.add(yearRB);
		chooseDatePanel.add(yearComboBox);
		chooseDatePanel.add(monthRB);
		chooseDatePanel.add(monthComboBox);
		chooseDatePanel.add(dayRB);
		chooseDatePanel.add(dayComboBox);
		if(titleLabel.getText().equals("Reports") && reportType.equals("Snapshot")) {
			monthRB.setEnabled(false);
			yearRB.setEnabled(false);
		}
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
			if(titleLabel.getText().equals("Check Out(-) Item Log") || titleLabel.getText().equals("Receive(+) Item Log")) {
				sortByAndShow();
			}
		}
	}
	public void editItem() {
		barcodeItem = new String[5];
		
		barcodeItem[0] = itemTable.getValueAt(itemTable.getSelectedRow(), 1).toString();
		barcodeItem[1] = itemTable.getValueAt(itemTable.getSelectedRow(), 0).toString();
		barcodeItem[2] = itemTable.getValueAt(itemTable.getSelectedRow(), 2).toString();
		barcodeItem[3] = itemTable.getValueAt(itemTable.getSelectedRow(), 3).toString();
		barcodeItem[4] = itemTable.getValueAt(itemTable.getSelectedRow(), 4).toString();
		int originalStock = Integer.parseInt(barcodeItem[2]);
		if(barcodeItem[4].equals("Yes")) {
			barcodeItem[4] = "1";
		}
		else {
			barcodeItem[4] = "0";
		}
		
		JTextField name = new JTextField(50);
		name.setText(barcodeItem[1]);
		name.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
		SpinnerModel inStockQuantity = new SpinnerNumberModel(Integer.parseInt(barcodeItem[2]), 0, 1000, 1);
		SpinnerModel restockQuantity = new SpinnerNumberModel(Integer.parseInt(barcodeItem[3]), 0, 1000, 1);
		
		JSpinner inStock = new JSpinner(inStockQuantity);
		inStock.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
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
		restock.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
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
		editItemPanel.setLayout(new GridLayout(8,1));
		JLabel nameLabel = new JLabel("Name");
		nameLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		editItemPanel.add(nameLabel);
		editItemPanel.add(name);
		JCheckBox kitCheckBox = new JCheckBox("Kit");
		kitCheckBox.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		kitCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
		kitCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(kitCheckBox.isSelected()) {
					inStock.setEnabled(false);
					restock.setEnabled(false);
				}
				else {
					inStock.setEnabled(true);
					restock.setEnabled(true);
				}
			}
		});
		if(barcodeItem[4].equals("1")) {
			kitCheckBox.setSelected(true);
			inStock.setEnabled(false);
			restock.setEnabled(false);
		}
		
		editItemPanel.add(kitCheckBox);
		JLabel inStockLabel = new JLabel("In Stock Quantity");
		inStockLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		editItemPanel.add(inStockLabel);
		editItemPanel.add(inStock);
		JLabel restockLabel = new JLabel("Restock Quantity");
		restockLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
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
			String[] options = {"OK", "Cancel"};
			result = JOptionPane.showOptionDialog(program, editItemPanel, 
					"Edit Item:", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, null);
			if(result == 1 || result == JOptionPane.CLOSED_OPTION) {
				int choice = JOptionPane.showConfirmDialog(	program, 
						"All progress will be lost. Are you sure you want to exit?" ,
						"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(choice == 0) {
					cancel = true;
				}
			}
			else if(name.getText().trim().length() > 300) {
				JOptionPane.showMessageDialog(program, "The name must be 300 characters or less. Please enter another one.", "Name too long", JOptionPane.WARNING_MESSAGE);
				name.requestFocusInWindow();
			}
			else {
				enterAdminPassword();
				if(passwordCorrect) {
					try {	
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
						s = conn.createStatement();
				        String sqlStatement;
				        if(kitCheckBox.isSelected()) {
				        	sqlStatement = "UPDATE product SET p_name = '" + name.getText() + "', instock= 0, restock= 0, kit = 1 WHERE p_id = '" + barcodeItem[0] + "';";
				        }
				        else {
				        	sqlStatement = "UPDATE product SET p_name = '" + name.getText() + "', instock= " + inStock.getValue() + ", restock= " + restock.getValue() + ", kit = 0 WHERE p_id = '" + barcodeItem[0] + "';";
				        }
				        s.execute(sqlStatement);
						// updateLog("new", Integer.parseInt(inStock.getValue().toString()), name.getText());
						refreshItemTable();
						updateRestockingNumber();
						cancel = true;
					}
					catch(SQLException e) {
				    	JOptionPane.showMessageDialog(program, "There is a problem editing the item in the database.\n" + e.toString(), "Could not edit item in DB", JOptionPane.WARNING_MESSAGE);
					}
					finally {
						try { s.close(); } catch (Exception e) { /* ignored */ }
						try { r.close(); } catch (Exception e) { /* ignored */ }
						try { conn.close(); } catch (Exception e) { /* ignored */ }
					}
					
					// add to edited table
					
					try {	
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
						s = conn.createStatement();
				        String sqlStatement;
				        int difference = (Integer.parseInt(inStock.getValue().toString()) - originalStock);
				        sqlStatement = "INSERT INTO edited VALUES('" + barcodeItem[0] + "', " + difference +  ", GETDATE());";
				        if(difference != 0) {	
				        	s.execute(sqlStatement);
				        }
						updateRestockingNumber();
						refreshItemTable();
						cancel = true;
					}
					catch(SQLException e) {
				    	JOptionPane.showMessageDialog(program, "There is a problem editing the item in the database.\n" + e.toString(), "Could not edit item in DB", JOptionPane.WARNING_MESSAGE);
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
	
	public void editKit() {
		JPanel editKitPanel = new JPanel();
		
		refreshKitItemTable();
		
		kitItemTableScrollPane = new JScrollPane(kitItemTable);
		kitItemTableScrollPane.setViewportView(kitItemTable);
		kitItemTableScrollPane.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
	    kitItemTableScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    kitItemTableScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);	 
		
		editKitPanel = new JPanel();
		editKitPanel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		editKitPanel.add(kitItemTableScrollPane);
		
		String[] options = {"Add Kit Item", "Edit Kit Item", "Delete Kit Item", "Cancel"};
		boolean exitEditKitItem = false;
		int result;
		while(!exitEditKitItem) {
			kitItem = null;
			refreshKitItemTable();
			result = JOptionPane.showOptionDialog(program, editKitPanel, kitName, JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, null);
			if(result == 0) {
				editKitItem("add");
			}
			else if(result == 1) {
				if(kitItemTable == null || kitItemTable.getRowCount() < 1) {
			    	JOptionPane.showMessageDialog(program, "There are no kit items to edit.", "Could not edit kit item in DB", JOptionPane.WARNING_MESSAGE);
				}
				else {
					kitItem = (kitItemTable.getValueAt(kitItemTable.getSelectedRow(), kitItemTable.getColumnModel().getColumnCount() - 1)).toString();
					editKitItem("edit");
				}
			}
			else if(result == 2) {
				if(kitItemTable == null || kitItemTable.getRowCount() < 1) {
			    	JOptionPane.showMessageDialog(program, "There are no kit items to delete.", "Could not delete kit item in DB", JOptionPane.WARNING_MESSAGE);
				}
				else {
					kitItem = (kitItemTable.getValueAt(kitItemTable.getSelectedRow(), kitItemTable.getColumnModel().getColumnCount() - 1)).toString();
					editKitItem("delete");
				}
			}
			else {
				exitEditKitItem = true;
			}
		}
	}
	public void editKitItem(String option) {
		if(option.equals("delete")) {
			int choice = JOptionPane.showConfirmDialog(	program, 
					"Are you sure you want to delete " + kitItem + " from this kit?",
					"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(choice == 0) {
				enterAdminPassword();
				if(passwordCorrect) {
					try {	
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
						s = conn.createStatement();
				        String sqlStatement = "DELETE FROM kitItem WHERE kitItem_id = " + kitItemTable.getModel().getValueAt(kitItemTable.getSelectedRow(), kitItemTable.getModel().getColumnCount() - 1) + ";";
				        s.execute(sqlStatement);
					}
					catch(SQLException e) {
						
				    	JOptionPane.showMessageDialog(program, "There is a problem deleting the kit item from the database.\n" + e.toString(), "Could not delete kit item from DB", JOptionPane.WARNING_MESSAGE);
					}
			 		finally {
						try { s.close(); } catch (Exception e) {    }
						try { r.close(); } catch (Exception e) {    }
						try { conn.close(); } catch (Exception e) {    }
					}
				}
			}
		}
		else {
			// gets current kit items
			ArrayList<String> kitItems = new ArrayList<String>();
			try {	
		        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
				s = conn.createStatement();
		        String sqlStatement = "SELECT p1.p_name FROM (kitItem LEFT JOIN product p1 ON kitItem.kitItem_product = p1.p_id) LEFT JOIN product ON kitItem.kit_product = product.p_id WHERE product.p_name = '" + kitName +"';";
		        r = s.executeQuery(sqlStatement);
		        while(r.next()) {
		        	kitItems.add(r.getString(1));
		        }
			}
			catch(SQLException e) {
				
		    	JOptionPane.showMessageDialog(program, "There is a problem finding the kit items in the database.\n" + e.toString(), "Could not find kit items in DB", JOptionPane.WARNING_MESSAGE);
			}
	 		finally {
				try { s.close(); } catch (Exception e) {    }
				try { r.close(); } catch (Exception e) {    }
				try { conn.close(); } catch (Exception e) {    }
			}
			
			JLabel quantityLabel = new JLabel("Quantity");
			quantityLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
			SpinnerModel quantitySpinner = new SpinnerNumberModel(1, 1, 1000, 1);
			JSpinner quantity = new JSpinner(quantitySpinner);
			quantity.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
			JComponent quantityEditor = quantity.getEditor();
			if(quantityEditor instanceof JSpinner.DefaultEditor) {
				JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor)quantityEditor;
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
			JLabel productLabel = new JLabel("Product");
			productLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
			JTextField productTextField = new JTextField(20);
			productTextField.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
			productTextField.addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent e) {
					quantity.requestFocusInWindow();
					chooseBarcodeOrKeyword();
					if(barcodeItem[0] != null) {
						productTextField.setText(barcodeItem[1]);
					}
				}
				@Override
				public void focusLost(FocusEvent e) {
				}
			});
			
			JPanel editKitItemPanel = new JPanel();
			editKitItemPanel.setLayout(new GridLayout(4, 1));
			editKitItemPanel.add(quantityLabel);
			editKitItemPanel.add(quantity);
			editKitItemPanel.add(productLabel);
			editKitItemPanel.add(productTextField);
			productTextField.setCaretPosition(0);
			
			if(option.equals("edit")) {
				quantity.setValue(Integer.parseInt(kitItemTable.getValueAt(kitItemTable.getSelectedRow(), 0).toString()));
				productTextField.setText(kitItemTable.getValueAt(kitItemTable.getSelectedRow(), 1).toString());
			}
			
			int result;
			boolean cancel = false;
			while(!cancel) {
				quantity.requestFocusInWindow();
				String[] options = {"OK", "Cancel"};
				result = JOptionPane.showOptionDialog(program, editKitItemPanel, 
						"Edit Kit Item:", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, null);
				
				if(result == 1 || result == JOptionPane.CLOSED_OPTION) {
					int choice = JOptionPane.showConfirmDialog(	program, 
							"All progress will be lost. Are you sure you want to exit?" ,
							"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if(choice == 0) {
						cancel = true;
					}
				}
				else if(result == 0 && productTextField.getText().isEmpty()) {
	        		JOptionPane.showMessageDialog(program, "Please enter a product to add to the kit.", "Empty product field", JOptionPane.WARNING_MESSAGE);
				}
				else if(result == 0 && productTextField.getText().equals(kitName)) {
	        		JOptionPane.showMessageDialog(program, "You cannot add a kit inside of a kit.", "Cannot add kit item", JOptionPane.WARNING_MESSAGE);
				}
				else if(result == 0 && kitItems.contains(productTextField.getText())) {
	        		JOptionPane.showMessageDialog(program, "That item is already inside the kit.", "Cannot add kit item", JOptionPane.WARNING_MESSAGE);
				}
				else {
					try {	
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
						s = conn.createStatement();
				        String sqlStatement;
				        if(option.equals("add")) {
				        	sqlStatement = "INSERT INTO kitItem (kit_product, kitItem_product, quantity) SELECT a1.p_id, a2.p_id, " + quantity.getValue() + " FROM product a1, product a2 WHERE a1.p_name = '" + kitName + "' AND a2.p_name = '" + kitItem + "';";
				        }
				        else {
				        	sqlStatement = "UPDATE kitItem SET quantity = " + quantity.getValue() + ", kitItem_product = (SELECT p_id FROM product WHERE p_name = '" + kitItem + "') WHERE kitItem_id = " + kitItemTable.getModel().getValueAt(kitItemTable.getSelectedRow(), kitItemTable.getModel().getColumnCount() - 1) + ";";
				        }
				        s.execute(sqlStatement);
						cancel = true;
					}
					catch(SQLException e) {
						JOptionPane.showMessageDialog(program, "There is a problem editing the kit item in the database.\n" + e.toString(), "Could not edit kit item in DB", JOptionPane.WARNING_MESSAGE);
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
	
	public void enterPassword() {
		boolean passwordAccepted = false;
		passwordCorrect = false;
		while(!passwordAccepted) {
			JPanel passwordInputPanel = new JPanel();
			JLabel enterPasswordLabel = new JLabel("Please enter password for " + user + ":");
			enterPasswordLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
			passwordField.setText("");
			confirmPasswordField.setText("");
			passwordInputPanel.setLayout(new GridLayout(2,1));
			passwordInputPanel.add(enterPasswordLabel);
			passwordInputPanel.add(passwordField);
			passwordField.addAncestorListener(new AncestorListener() {     
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
			int passwordResult;
			passwordResult = JOptionPane.showConfirmDialog(program, passwordInputPanel, "Enter Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
			if(passwordResult == JOptionPane.OK_OPTION && passwordField.getPassword() != null) {
				user = userList.getSelectedValue();
				String[] userParts = new String[2];
				userParts = user.split("\\s+");
				try {	
			        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
					s = conn.createStatement();
			        String sqlStatement = "SELECT salt, password_hash FROM employee WHERE (e_fname = '" + userParts[0] + "' AND e_lname = '" + userParts[1] + "') OR admin = 1;";
			        r = s.executeQuery(sqlStatement);
			        while(r.next()) {
			        	if(verifyPassword(new String(passwordField.getPassword()), r.getString(2), r.getString(1))) {
			        		passwordAccepted = true;
			        		passwordCorrect = true;
			        		break;
			        	}
			        }
				}
				catch(SQLException e) {
			    	JOptionPane.showMessageDialog(program, "There is a problem validating your password.\n" + e.toString(), "Could not validate password", JOptionPane.WARNING_MESSAGE);
				}
				finally {
					try { s.close(); } catch (Exception e) { /* ignored */ }
					try { r.close(); } catch (Exception e) { /* ignored */ }
					try { conn.close(); } catch (Exception e) { /* ignored */ }
				}
				if(!passwordCorrect) {
	        		JOptionPane.showMessageDialog(program, "That password is incorrect. Please try again.", "Incorrect password", JOptionPane.WARNING_MESSAGE);
				}
			}
			else {
				passwordAccepted = true;
			}
		}
	}
	public void enterAdminPassword() {
		ArrayList<String> adminSalts = new ArrayList<String>();
		ArrayList<String> adminPasswordHashes = new ArrayList<String>();
		passwordCorrect = false;
		boolean passwordAccepted = false;
		try {	
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
			s = conn.createStatement();
	        String sqlStatement = "SELECT salt, password_hash FROM employee WHERE admin = 1;";
	        r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	adminSalts.add(r.getString(1));
	        	adminPasswordHashes.add(r.getString(2));
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem retrieving admin credentials.\n" + e.toString(), "Could not retrieve admin credentials", JOptionPane.WARNING_MESSAGE);
		}
		finally {
			try { s.close(); } catch (Exception e) { /* ignored */ }
			try { r.close(); } catch (Exception e) { /* ignored */ }
			try { conn.close(); } catch (Exception e) { /* ignored */ }
		}
		JPanel passwordInputPanel = new JPanel();
		JLabel enterPasswordLabel = new JLabel("Please enter password for an admin user:");
		enterPasswordLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		passwordInputPanel.setLayout(new GridLayout(2,1));
		passwordInputPanel.add(enterPasswordLabel);
		
		// use passwordField as temporary variable for admin password
		passwordInputPanel.add(passwordField);
		passwordField.addAncestorListener(new AncestorListener() {     
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
		while(!passwordAccepted) {
			int passwordResult;
			passwordField.setText("");
			passwordResult = JOptionPane.showConfirmDialog(program, passwordInputPanel, "Enter Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
			if(passwordResult == JOptionPane.OK_OPTION && passwordField.getPassword() != null) {
				for(int i = 0; i < adminSalts.size(); i++) {
					if(verifyPassword(new String(passwordField.getPassword()), adminPasswordHashes.get(i), adminSalts.get(i))) {
		        		passwordAccepted = true;
		        		passwordCorrect = true;
		        		break;
		        	}
				}
				if(!passwordCorrect) {
			    	JOptionPane.showMessageDialog(program, "That password is incorrect. Please contact an administrator to gain access.", "Incorrect password", JOptionPane.WARNING_MESSAGE);
				}
			}
			else {
				passwordAccepted = true;
			}
		}
	}
	public void deleteItemLog() {
		int[] selectedRows = itemTable.getSelectedRows();
		int choice = JOptionPane.showConfirmDialog(	program, 
				"Are you sure you want to delete all " + selectedRows.length + " records? Item stock levels will be adjusted." ,
				"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if(choice == 0) {
			enterAdminPassword();
			if(passwordCorrect) {
				for(int i = 0; i < selectedRows.length; i++) {
					int productColumn = 2;
					int quantityColumn = 3;
					if(sortByName.equals("Product")) {
		        		productColumn = 0;
		        	}
					// updating the inventory
					try {	
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
						s = conn.createStatement();
				        String sqlStatement;
				        if(titleLabel.getText().equals("Check Out(-) Item Log")) {
				        	sqlStatement = "UPDATE product SET instock = instock + 1 WHERE p_id = (SELECT p_id FROM product WHERE p_name = '" + itemTable.getModel().getValueAt(selectedRows[i], productColumn) + "');";
				        }
				        else{
				        	sqlStatement = "UPDATE product SET instock = instock - " + itemTable.getModel().getValueAt(selectedRows[i], quantityColumn) + " WHERE p_id = (SELECT p_id FROM product WHERE p_name = '" + itemTable.getModel().getValueAt(selectedRows[i], productColumn) + "')";
				        }
				        s.execute(sqlStatement);
					}
					catch(SQLException e) {
				    	JOptionPane.showMessageDialog(program, "There is a problem deleting the item logs from the database.\n" + e.toString(), "Could not delete item logs from DB", JOptionPane.WARNING_MESSAGE);
					}
			 		finally {
						try { s.close(); } catch (Exception e) {    }
						try { r.close(); } catch (Exception e) {    }
						try { conn.close(); } catch (Exception e) {    }
					}
					// deleting the record
					try {	
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
						s = conn.createStatement();
				        String sqlStatement;
				        if(titleLabel.getText().equals("Check Out(-) Item Log")) {
				        	sqlStatement = "DELETE FROM checkout WHERE co_id = " + itemTable.getModel().getValueAt(selectedRows[i], itemTable.getModel().getColumnCount() - 1) + ";";
				        }
				        else{
				        	sqlStatement = "DELETE FROM receive WHERE rc_id = " + itemTable.getModel().getValueAt(selectedRows[i], itemTable.getModel().getColumnCount() - 1) + ";";
				        }
				        s.execute(sqlStatement);
					}
					catch(SQLException e) {
						
				    	JOptionPane.showMessageDialog(program, "There is a problem deleting the item logs from the database.\n" + e.toString(), "Could not delete item logs from DB", JOptionPane.WARNING_MESSAGE);
					}
			 		finally {
						try { s.close(); } catch (Exception e) {    }
						try { r.close(); } catch (Exception e) {    }
						try { conn.close(); } catch (Exception e) {    }
					}
				}
				sortByAndShow();
			}
		}
	}
	public void editItemLog(int itemLogNumber) {
		JPanel editItemLogPanel = new JPanel();
		JLabel editItemLogLabel = new JLabel("Edit the information of the item log:");
		editItemLogLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		JLabel editDateLabel = new JLabel("Date (YYYY-MM-DD):");
		editDateLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		JTextField dateTextField = new JTextField();
		dateTextField.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
		JLabel editUserLabel = new JLabel("User:");
		editUserLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		JTextField userTextField = new JTextField();
		userTextField.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
		JLabel editAccountNumLabel = new JLabel("Account Number:");
		editAccountNumLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		JTextField accountNumTextField = new JTextField();
		accountNumTextField.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
		JLabel editAddressLabel = new JLabel("Address:");
		editAddressLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		JTextField addressTextField = new JTextField();
		addressTextField.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
		JLabel editNotesLabel = new JLabel("Notes:");
		editNotesLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		JTextField notesTextField = new JTextField();
		notesTextField.setFont(new Font("Arial", Font.PLAIN, screenSize.width/69));
		
		JCheckBox statusCheckBox = new JCheckBox();
		if(checkOutRB.isSelected()) {
			statusCheckBox.setText("Defective:");
		}
		else {
			statusCheckBox.setText("Returned:");
		}
		statusCheckBox.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		statusCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
		
		accountNumTextField.setText("");
		addressTextField.setText("");
		notesTextField.setText("");
		
		int[] selectedRows = itemTable.getSelectedRows();
		
		for(int i = 0; i < itemTable.getColumnCount(); i++) {
			if(itemTable.getColumnName(i).equals("Date")) {
				dateTextField.setText((itemTable.getValueAt(itemTable.getSelectedRow(), i).toString()));
			}
			else if(itemTable.getColumnName(i).equals("User") && itemTable.getValueAt(itemTable.getSelectedRow(), i) != null) {
				userTextField.setText((itemTable.getValueAt(itemTable.getSelectedRow(), i).toString()));
			}
			else if(itemTable.getColumnName(i).equals("Account No.") && itemTable.getValueAt(itemTable.getSelectedRow(), i) != null) {
				accountNumTextField.setText((itemTable.getValueAt(itemTable.getSelectedRow(), i).toString()));
			}
			else if(itemTable.getColumnName(i).equals("Address")  && itemTable.getValueAt(itemTable.getSelectedRow(), i) != null) {
				addressTextField.setText((itemTable.getValueAt(itemTable.getSelectedRow(), i).toString()));			
			}
			else if(itemTable.getColumnName(i).equals("Defective") || itemTable.getColumnName(i).equals("Returned")) {
				if((itemTable.getValueAt(itemTable.getSelectedRow(), i).toString()).equals("Yes")) {
					statusCheckBox.setSelected(true);
				}
				else {
					statusCheckBox.setSelected(false);
				}
			}
			else if(itemTable.getColumnName(i).equals("Notes")  && itemTable.getValueAt(itemTable.getSelectedRow(), i) != null) {
				notesTextField.setText((itemTable.getValueAt(itemTable.getSelectedRow(), i).toString()));
			}
		}
		
		editItemLogPanel.setLayout(new GridLayout(12,1));
		editItemLogPanel.add(editItemLogLabel);
		editItemLogPanel.add(editDateLabel);
		editItemLogPanel.add(dateTextField);
		editItemLogPanel.add(editUserLabel);
		editItemLogPanel.add(userTextField);
		editItemLogPanel.add(editAccountNumLabel);
		editItemLogPanel.add(accountNumTextField);
		editItemLogPanel.add(editAddressLabel);
		editItemLogPanel.add(addressTextField);
		editItemLogPanel.add(editNotesLabel);
		editItemLogPanel.add(notesTextField);
		editItemLogPanel.add(statusCheckBox);
		dateTextField.addAncestorListener(new AncestorListener() {     
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
		userTextField.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
			}
			@Override
			public void mouseEntered(MouseEvent arg0) {
			}
			@Override
			public void mouseExited(MouseEvent arg0) {
			}
			@Override
			public void mousePressed(MouseEvent arg0) {
				chooseUser();
				if(user != null) {
					userTextField.setText(user);
				}
				dateTextField.requestFocusInWindow();
				itemTable.getSelectionModel().removeSelectionInterval(0, 0);
				for(int i = 0; i < selectedRows.length; i++) {
					itemTable.getSelectionModel().addSelectionInterval(selectedRows[i], selectedRows[i]);
				}
			}
			@Override
			public void mouseReleased(MouseEvent e) {
			}
		});
		int itemLogResult;
		boolean validDate = false, cancel = false;
		while(!cancel) {
			itemLogResult = JOptionPane.showConfirmDialog(program, editItemLogPanel, "Edit Item Log", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
			sdf.setLenient(false);
			Date editedDate = new Date();
			if(itemLogResult == JOptionPane.OK_OPTION) {
				int e_id = getEmployeeID(userTextField.getText());
				try {
					editedDate = sdf.parse(dateTextField.getText());
					validDate = true;
				}
				catch(Exception e) {

				}
				if(!validDate) {
			    	JOptionPane.showMessageDialog(program, "The date is not valid. Please try again", "Could not edit item log in DB", JOptionPane.WARNING_MESSAGE);
				}
				else if(accountNumTextField.getText().trim().length() > 20) {
					JOptionPane.showMessageDialog(program, "The account number must be 20 characters or less. Please enter another one.", "Account number too long", JOptionPane.WARNING_MESSAGE);
					accountNumTextField.requestFocusInWindow();
				}
				else if(addressTextField.getText().trim().length() > 300) {
					JOptionPane.showMessageDialog(program, "The address must be 300 characters or less. Please enter another one.", "Address too long", JOptionPane.WARNING_MESSAGE);
					addressTextField.requestFocusInWindow();
				}
				else if(notesTextField.getText().trim().length() > 1000) {
					JOptionPane.showMessageDialog(program, "The notes must be 1000 characters or less. Please enter another one.", "Notes too long", JOptionPane.WARNING_MESSAGE);
					notesTextField.requestFocusInWindow();
				}
				else {
					if(itemTable.getSelectedRowCount() > 1) {
						JPanel editItemLogFieldsPanel = new JPanel();
						JLabel editItemLogFieldsLabel = new JLabel("Select the fields you want to edit in the " + itemTable.getSelectedRowCount() + " records:");
						editItemLogFieldsLabel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
						JCheckBox dateCB = new JCheckBox("Date");
						dateCB.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
						dateCB.setHorizontalTextPosition(SwingConstants.LEFT);
						JCheckBox userCB = new JCheckBox("User");
						userCB.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
						userCB.setHorizontalTextPosition(SwingConstants.LEFT);
						JCheckBox accountCB = new JCheckBox("Account No.");
						accountCB.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
						accountCB.setHorizontalTextPosition(SwingConstants.LEFT);
						JCheckBox addressCB = new JCheckBox("Address");
						addressCB.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
						addressCB.setHorizontalTextPosition(SwingConstants.LEFT);
						JCheckBox statusCB = new JCheckBox();
						statusCB.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
						statusCB.setHorizontalTextPosition(SwingConstants.LEFT);
						JCheckBox notesCB = new JCheckBox("Notes");
						notesCB.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
						notesCB.setHorizontalTextPosition(SwingConstants.LEFT);
						
						if(checkOutRB.isSelected()) {
							statusCB.setText("Defective");
						}
						else {
							statusCB.setText("Returned");
						}
						
						editItemLogFieldsPanel.setLayout(new GridLayout(7,1));
						editItemLogFieldsPanel.add(editItemLogFieldsLabel);
						editItemLogFieldsPanel.add(dateCB);
						editItemLogFieldsPanel.add(userCB);
						editItemLogFieldsPanel.add(accountCB);
						editItemLogFieldsPanel.add(addressCB);
						editItemLogFieldsPanel.add(statusCB);
						editItemLogFieldsPanel.add(notesCB);
						int editMultiple = JOptionPane.showConfirmDialog(program, editItemLogFieldsPanel,
								"Select the values you want to edit:" ,
								 JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
						if(editMultiple == JOptionPane.OK_OPTION) {
							String editOptions = "SET ";
							if(dateCB.isSelected()) {
								if(checkOutRB.isSelected()) {
									editOptions += "co_date = '" + sdf.format(editedDate) + "', ";
								}
								else {
									editOptions += "rc_date = '" + sdf.format(editedDate) + "', ";
								}
							}
							if(userCB.isSelected()) {
								editOptions += "e_id = " + e_id + ", ";
							}
							if(accountCB.isSelected()) {
								editOptions += "acc_id = '" + accountNumTextField.getText() + "', ";
							}
							if(addressCB.isSelected()) {
								editOptions += "address = '" + addressTextField.getText() + "', ";
							}
							if(statusCB.isSelected()) {
								if(checkOutRB.isSelected()) {
									if(!statusCheckBox.isSelected()) {
										editOptions += "defective = 0, ";
									}
									else {
										editOptions += "defective = 1, ";
									}
								}
								else {
									if(!statusCheckBox.isSelected()) {
										editOptions += "returned = 0, ";
									}
									else {
										editOptions += "returned = 1, ";
									}
								}
							}
							if(notesCB.isSelected()) {
								editOptions += "notes = '" + notesTextField.getText() + "', ";
							}
							// if user did not check any of the boxes
							if(editOptions.equals("SET ")) {
						    	JOptionPane.showMessageDialog(program, "Please specify what fields need to be edited.", "Could not edit item log in DB", JOptionPane.WARNING_MESSAGE);
							}
							else {
								editOptions = editOptions.substring(0, editOptions.length() - 2);
								for(int i = 0; i < selectedRows.length; i++) {
									try {	
								        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
										s = conn.createStatement();
										String sqlStatement;
										if(checkOutRB.isSelected()) {
								        	sqlStatement = "UPDATE checkout " + editOptions + " WHERE co_id = " + itemTable.getModel().getValueAt(selectedRows[i], itemTable.getModel().getColumnCount() - 1) + ";";
										}
										else {
								        	sqlStatement = "UPDATE receive " + editOptions + " WHERE rc_id = " + itemTable.getModel().getValueAt(selectedRows[i], itemTable.getModel().getColumnCount() - 1) + ";";
										}
								        s.execute(sqlStatement);								        
									}
									catch(SQLException e) {
								    	JOptionPane.showMessageDialog(program, "There is a problem editing the item log in the database.\n" + e.toString(), "Could not edit item log in DB", JOptionPane.WARNING_MESSAGE);
								    	
									}
									finally {
										try { s.close(); } catch (Exception e) {  }
										try { r.close(); } catch (Exception e) {  }
										try { conn.close(); } catch (Exception e) {  }
									}
								}
								sortByAndShow();
								cancel = true;
							}
						}
					}
					else {
						try {	
					        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
							s = conn.createStatement();
							String sqlStatement;
							if(checkOutRB.isSelected()) {
								if(!statusCheckBox.isSelected()) {
					        		sqlStatement = "UPDATE checkout SET co_date = '" + sdf.format(editedDate) + "', e_id = " + e_id + ", acc_id = '" + accountNumTextField.getText() + "', address= '" + addressTextField.getText() + "', defective = 0, notes = '" + notesTextField.getText() + "' WHERE co_id = " + itemTable.getModel().getValueAt(itemTable.getSelectedRow(), itemTable.getModel().getColumnCount() - 1) + ";";
					        		
								}
								else {
					        		sqlStatement = "UPDATE checkout SET co_date = '" + sdf.format(editedDate) + "', e_id = " + e_id + ", acc_id = '" + accountNumTextField.getText() + "', address= '" + addressTextField.getText() + "', defective = 1, notes = '" + notesTextField.getText() + "' WHERE co_id = " + itemTable.getModel().getValueAt(itemTable.getSelectedRow(), itemTable.getModel().getColumnCount() - 1) + ";";
								}
							}
							else {
								if(!statusCheckBox.isSelected()) {
					        		sqlStatement = "UPDATE receive SET rc_date = '" + sdf.format(editedDate) + "', e_id = " + e_id + ", acc_id = '" + accountNumTextField.getText() + "', address= '" + addressTextField.getText() + "', returned = 0, notes = '" + notesTextField.getText() + "' WHERE rc_id = " + itemTable.getModel().getValueAt(itemTable.getSelectedRow(), itemTable.getModel().getColumnCount() - 1) + ";";
								}
								else {
					        		sqlStatement = "UPDATE receive SET rc_date = '" + sdf.format(editedDate) + "', e_id = " + e_id + ", acc_id = '" + accountNumTextField.getText() + "', address= '" + addressTextField.getText() + "', returned = 1, notes = '" + notesTextField.getText() + "' WHERE rc_id = " + itemTable.getModel().getValueAt(itemTable.getSelectedRow(), itemTable.getModel().getColumnCount() - 1) + ";";
								}
							}
					        s.execute(sqlStatement);
					        sortByAndShow();
					        
						}
						catch(SQLException e) {
					    	JOptionPane.showMessageDialog(program, "There is a problem editing the item log in the database.\n" + e.toString(), "Could not edit item log in DB", JOptionPane.WARNING_MESSAGE);
					    	
						}
						finally {
							try { s.close(); } catch (Exception e) {  }
							try { r.close(); } catch (Exception e) {  }
							try { conn.close(); } catch (Exception e) {  }
						}
						cancel = true;
					}
				}
			}
			else {
				int choice = JOptionPane.showConfirmDialog(	program, 
						"All progress will be lost. Are you sure you want to exit?" ,
						"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(choice == 0) {
					cancel = true;
				}
			}
		}
	}
	public void chooseUser() {
		
		/**
		 * This method is used to finalize a transaction and take the user's name
		 */
		
		user = null;
		userLines.clear();
		try {	
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
			s = conn.createStatement();
	        String sqlStatement;
	        if(titleLabel.getText().equals("Check Out(-) Item Log") || titleLabel.getText().equals("Receive(+) Item Log")) {
	        	sqlStatement = "SELECT e_fname, e_lname FROM employee ORDER BY e_fname;";
	        }
	        else {
	        	sqlStatement = "SELECT e_fname, e_lname FROM employee WHERE active = 1 ORDER BY e_fname;";
	        }
	        r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	userLines.add(r.getString(1) + " " + r.getString(2));
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem loading the users from the database.\n" + e.toString(), "Could not load users from DB", JOptionPane.WARNING_MESSAGE);
		}
		finally {
			try { s.close(); } catch (Exception e) { /* ignored */ }
			try { r.close(); } catch (Exception e) { /* ignored */ }
			try { conn.close(); } catch (Exception e) { /* ignored */ }
		}
		String[] users = new String[userLines.size()];
		users =	userLines.toArray(users);
		userList = new JList<String>(users);
		userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		userList.setLayoutOrientation(JList.VERTICAL);
		userList.setVisibleRowCount(-1);
		userList.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
		userList.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		userList.addMouseListener(this);
		
		userScrollPane = new JScrollPane(userList);
		userScrollPane.setViewportView(userList);
		userScrollPane.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
	    userScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	    userScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		
		userPanel = new JPanel();
		userPanel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		userPanel.add(userScrollPane);
		
		int result;
		boolean validUser = false, cancel = false;
		while(!validUser && !cancel) {
			result = JOptionPane.showConfirmDialog(program, userPanel, 
					"Please choose your name:", JOptionPane.OK_CANCEL_OPTION);
			if(result == JOptionPane.OK_OPTION) {
				user = userList.getSelectedValue();
				if(user != null) {
					validUser = true;
					if(titleLabel.getText().equals("Check Out(-) Item Log") || titleLabel.getText().equals("Receive(+) Item Log")) {
						sortByAndShow();
					}
				}
				else {
					JOptionPane.showMessageDialog(program, "No user was selected.", "User select failed", JOptionPane.WARNING_MESSAGE);
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
		sortByList.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		sortByList.setSelectedIndex(0);
		sortByList.addMouseListener(this);
		
		sortByScrollPane = new JScrollPane(sortByList);
		sortByScrollPane.setViewportView(sortByList);
		sortByScrollPane.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
	    sortByScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	    sortByScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		sortByPanel = new JPanel();
		sortByPanel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		sortByScrollPane.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
		sortByPanel.add(sortByScrollPane);
		
		int result = JOptionPane.showConfirmDialog(program, sortByPanel, "Sort Item Log", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
		if(result == JOptionPane.OK_OPTION) {
			if(sortByList.getSelectedValue().equals("Past 4 Weeks")) {
				sortByName = "Past 4 Weeks";
				sortByAndShow();
			}
			else if(sortByList.getSelectedValue().equals("Date")) {
				sortByName = "Date";
				chooseDate();
			}
			else if(sortByList.getSelectedValue().equals("User")) {
				sortByName = "User";
				chooseUser();
			}
			else if(sortByList.getSelectedValue().equals("Product")) {
				sortByName = "Product";
				chooseBarcodeOrKeyword();
			}
		}
	}
	public void sortByAndShow() {

		String[] fields;
		String[][] values;
		DefaultTableModel newTableModel;
		int length = 0;
		int count = 0;
		values = null;
		fields =  new String[] {"Date", "User", "Product", "Account No.", "Address", "Defective", "Notes", "Log No."};
		if(sortByName.equals("Past 4 Weeks")) {
			values = null;
			if(checkOutRB.isSelected()) {
				fields =  new String[] {"Date", "User", "Product", "Account No.", "Address", "Defective", "Notes", "Log No."};
				titleLabel.setText("Check Out(-) Item Log");
				try {	
			        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
					s = conn.createStatement();
					// get table size
			        String sqlStatement = "SELECT COUNT(co_id) FROM checkout WHERE co_date > DATEADD(day, -28, GETDATE());";
			        r = s.executeQuery(sqlStatement);
			        while(r.next()) {
			        	length = Integer.parseInt(r.getString(1));
			        }
			        values = new String[length][fields.length];
			        s = conn.createStatement();
			        sqlStatement = "SELECT co_date, (employee.e_fname + ' ' + employee.e_lname) AS e_name, product.p_name, acc_id, address, defective, notes, co_id FROM (checkout LEFT JOIN employee ON checkout.e_id = employee.e_id LEFT JOIN product ON checkout.p_id = product.p_id) WHERE co_date > DATEADD(day, -28, GETDATE()) ORDER BY co_date DESC, e_name ASC, product.p_name ASC;";
			        r = s.executeQuery(sqlStatement);
			        while(r.next()) {
			        	for(int i = 0; i < fields.length; i++) {
			        		if(fields[i].equals("Defective")) {
			        			if(r.getString(i + 1).equals("0")) {
			        				values[count][i] = "No";
			        			}
			        			else {
			        				values[count][i] = "Yes";
			        			}
			        		}
			        		else{
			        			values[count][i] = r.getString(i + 1);
			        		}
			        	}
			        	count++;
			        }
				}
				catch(SQLException e) {
			    	JOptionPane.showMessageDialog(program, "There is a problem finding the items in the database.\n" + e.toString(), "Could not find items in DB", JOptionPane.WARNING_MESSAGE);
				}
		 		finally {
					try { s.close(); } catch (Exception e) { /* ignored */ }
					try { r.close(); } catch (Exception e) { /* ignored */ }
					try { conn.close(); } catch (Exception e) { /* ignored */ }
				}
			}
			else {
				fields =  new String[] {"Date", "User", "Product", "Quantity", "Account No.", "Address", "Returned", "Notes", "Log No."};
				titleLabel.setText("Receive(+) Item Log");
				try {	
			        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
					s = conn.createStatement();
					// get table size
			        String sqlStatement = "SELECT COUNT(rc_id) FROM receive WHERE rc_date > DATEADD(day, -28, GETDATE());";
			        r = s.executeQuery(sqlStatement);
			        while(r.next()) {
			        	length = Integer.parseInt(r.getString(1));
			        }
			        values = new String[length][fields.length];
			        s = conn.createStatement();
			        sqlStatement = "SELECT rc_date, (employee.e_fname + ' ' + employee.e_lname) AS e_name, product.p_name, quantity, acc_id, address, returned, notes, rc_id FROM receive LEFT JOIN employee ON receive.e_id = employee.e_id LEFT JOIN product ON receive.p_id = product.p_id WHERE rc_date > DATEADD(day, -28, GETDATE()) ORDER BY rc_date DESC, e_name ASC, product.p_name ASC;";
			        r = s.executeQuery(sqlStatement);
			        while(r.next()) {
			        	for(int i = 0; i < fields.length; i++) {
			        		if(fields[i].equals("Defective")) {
			        			if(r.getString(i + 1).equals("0")) {
			        				values[count][i] = "No";
			        			}
			        			else {
			        				values[count][i] = "Yes";
			        			}
			        		}
			        		else{
			        			values[count][i] = r.getString(i + 1);
			        		}
			        	}
			        	count++;
			        }
				}
				catch(SQLException e) {
			    	JOptionPane.showMessageDialog(program, "There is a problem finding the items in the database.\n" + e.toString(), "Could not find items in DB", JOptionPane.WARNING_MESSAGE);
				}
		 		finally {
					try { s.close(); } catch (Exception e) { /* ignored */ }
					try { r.close(); } catch (Exception e) { /* ignored */ }
					try { conn.close(); } catch (Exception e) { /* ignored */ }
				}
			}
		}
		else if(sortByName.equals("Date")) {
			values = null;
			if(checkOutRB.isSelected()) {
				fields =  new String[] {"Date", "User", "Product", "Account No.", "Address", "Defective", "Notes", "Log No."};
				titleLabel.setText("Check Out(-) Item Log");
				try {	
			        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
					s = conn.createStatement();
					// get table size
			        String sqlStatement = "SELECT COUNT(co_id) FROM checkout;";
			        if(dateOption != null) {
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
				        if(dateOption.equals("Year")) {
				        	sqlStatement = "SELECT co_date, (employee.e_fname + ' ' + employee.e_lname) AS e_name, product.p_name, acc_id, address, defective, notes, co_id FROM checkout LEFT JOIN employee ON checkout.e_id = employee.e_id LEFT JOIN product ON checkout.p_id = product.p_id WHERE YEAR(co_date) = " + year + " ORDER BY co_date DESC, e_name ASC, product.p_name ASC;";
				        }
				        else if(dateOption.equals("Month")) {
				        	sqlStatement = "SELECT co_date, (employee.e_fname + ' ' + employee.e_lname) AS e_name, product.p_name, acc_id, address, defective, notes, co_id FROM checkout LEFT JOIN employee ON checkout.e_id = employee.e_id LEFT JOIN product ON checkout.p_id = product.p_id WHERE YEAR(co_date) = " + year + " AND MONTH(co_date) = " + month + " ORDER BY co_date DESC, e_name ASC, product.p_name ASC;";
				        }
				        else {
				        	sqlStatement = "SELECT co_date, (employee.e_fname + ' ' + employee.e_lname) AS e_name, product.p_name, acc_id, address, defective, notes, co_id FROM checkout LEFT JOIN employee ON checkout.e_id = employee.e_id LEFT JOIN product ON checkout.p_id = product.p_id WHERE YEAR(co_date) = " + year + " AND MONTH(co_date) = " + month + " AND DAY(co_date) = " + day + " ORDER BY co_date DESC, e_name ASC, product.p_name ASC;";
				        }
				        r = s.executeQuery(sqlStatement);
				        while(r.next()) {
				        	for(int i = 0; i < fields.length; i++) {
				        		if(fields[i].equals("Defective")) {
				        			if(r.getString(i + 1).equals("0")) {
				        				values[count][i] = "No";
				        			}
				        			else {
				        				values[count][i] = "Yes";
				        			}
				        		}
				        		else{
				        			values[count][i] = r.getString(i + 1);
				        		}
				        	}
				        	count++;
				        }
			        }
				}
				catch(SQLException e) {
			    	JOptionPane.showMessageDialog(program, "There is a problem finding the items in the database.\n" + e.toString(), "Could not find items in DB", JOptionPane.WARNING_MESSAGE);
				}
		 		finally {
					try { s.close(); } catch (Exception e) { /* ignored */ }
					try { r.close(); } catch (Exception e) { /* ignored */ }
					try { conn.close(); } catch (Exception e) { /* ignored */ }
				}
			}
			else {
				fields =  new String[] {"Date", "User", "Product", "Quantity", "Account No.", "Address", "Returned", "Notes", "Log No."};
				titleLabel.setText("Receive(+) Item Log");
				try {	
			        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
					s = conn.createStatement();
					// get table size
			        String sqlStatement = "SELECT COUNT(rc_id) FROM receive;";
			        if(dateOption != null) {
				        if(dateOption.equals("Year")) {
				        	sqlStatement = "SELECT COUNT(rc_id) FROM receive WHERE YEAR(rc_date) = " + year + ";";
				        }
				        else if(dateOption.equals("Month")) {
				        	sqlStatement = "SELECT COUNT(rc_id) FROM receive WHERE YEAR(rc_date) = " + year + " AND MONTH(rc_date) = " + month + ";";
				        }
				        else {
				        	sqlStatement = "SELECT COUNT(rc_id) FROM receive WHERE YEAR(rc_date) = " + year + " AND MONTH(rc_date) = " + month + " AND DAY(rc_date) = " + day + ";";
				        }
				        r = s.executeQuery(sqlStatement);
				        while(r.next()) {
				        	length = Integer.parseInt(r.getString(1));
				        }
				        values = new String[length][fields.length];
				        s = conn.createStatement();
				        if(dateOption.equals("Year")) {
				        	sqlStatement = "SELECT rc_date, (employee.e_fname + ' ' + employee.e_lname) AS e_name, product.p_name, quantity, acc_id, address, returned, notes, rc_id FROM receive LEFT JOIN employee ON receive.e_id = employee.e_id LEFT JOIN product ON receive.p_id = product.p_id WHERE YEAR(rc_date) = " + year + " ORDER BY rc_date DESC, e_name ASC, product.p_name ASC;";
				        }
				        else if(dateOption.equals("Month")) {
				        	sqlStatement = "SELECT rc_date, (employee.e_fname + ' ' + employee.e_lname) AS e_name, product.p_name, quantity, acc_id, address, returned, notes, rc_id FROM receive LEFT JOIN employee ON receive.e_id = employee.e_id LEFT JOIN product ON receive.p_id = product.p_id WHERE YEAR(rc_date) = " + year + " AND MONTH(rc_date) = " + month + " ORDER BY rc_date DESC, e_name ASC, product.p_name ASC;";
				        }
				        else {
				        	sqlStatement = "SELECT rc_date, (employee.e_fname + ' ' + employee.e_lname) AS e_name, product.p_name, quantity, acc_id, address, returned, notes, rc_id FROM receive LEFT JOIN employee ON receive.e_id = employee.e_id LEFT JOIN product ON receive.p_id = product.p_id WHERE YEAR(rc_date) = " + year + " AND MONTH(rc_date) = " + month + " AND DAY(rc_date) = " + day + " ORDER BY rc_date DESC, e_name ASC, product.p_name ASC;";
				        }
				        r = s.executeQuery(sqlStatement);
				        while(r.next()) {
				        	for(int i = 0; i < fields.length; i++) {
				        		if(fields[i].equals("Returned")) {
				        			if(r.getString(i + 1).equals("0")) {
				        				values[count][i] = "No";
				        			}
				        			else {
				        				values[count][i] = "Yes";
				        			}
				        		}
				        		else{
				        			values[count][i] = r.getString(i + 1);
				        		}
				        	}
				        	count++;
				        }
			        }
				}
				catch(SQLException e) {
			    	JOptionPane.showMessageDialog(program, "There is a problem finding the items in the database.\n" + e.toString(), "Could not find items in DB", JOptionPane.WARNING_MESSAGE);
				}
		 		finally {
					try { s.close(); } catch (Exception e) { /* ignored */ }
					try { r.close(); } catch (Exception e) { /* ignored */ }
					try { conn.close(); } catch (Exception e) { /* ignored */ }
				}
			}
		}
		else if(sortByName.equals("User")) {
			values = null;
			if(checkOutRB.isSelected()) {
				fields =  new String[] {"User", "Date", "Product", "Account No.", "Address", "Defective", "Notes", "Log No."};
				titleLabel.setText("Check Out(-) Item Log");
				if(user != null) {
					String[] userParts = new String[2];
					userParts = user.split("\\s+");
					try {	
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
						s = conn.createStatement();
						// get table size
				        String sqlStatement = "SELECT COUNT(co_id) FROM (checkout LEFT JOIN employee ON checkout.e_id = employee.e_id) WHERE e_lname = '" + userParts[1] + "' AND e_fname = '" + userParts[0] + "';";
				        r = s.executeQuery(sqlStatement);
				        while(r.next()) {
				        	length = Integer.parseInt(r.getString(1));
				        }
				        values = new String[length][fields.length];
				        s = conn.createStatement();
				        sqlStatement = "SELECT (employee.e_fname + ' ' + employee.e_lname) AS e_name, co_date, product.p_name, acc_id, address, defective, notes, co_id FROM (checkout LEFT JOIN employee ON checkout.e_id = employee.e_id LEFT JOIN product ON checkout.p_id = product.p_id) WHERE e_lname = '" + userParts[1] + "' AND e_fname = '" + userParts[0] + "' ORDER BY co_date DESC, e_name ASC, product.p_name ASC;";
				        r = s.executeQuery(sqlStatement);
				        while(r.next()) {
				        	for(int i = 0; i < fields.length; i++) {
				        		if(fields[i].equals("Defective")) {
				        			if(r.getString(i + 1).equals("0")) {
				        				values[count][i] = "No";
				        			}
				        			else {
				        				values[count][i] = "Yes";
				        			}
				        		}
				        		else{
				        			values[count][i] = r.getString(i + 1);
				        		}
				        	}
				        	count++;
				        }
					}
					catch(SQLException e) {
				    	JOptionPane.showMessageDialog(program, "There is a problem finding the items in the database.\n" + e.toString(), "Could not find items in DB", JOptionPane.WARNING_MESSAGE);
					}
			 		finally {
						try { s.close(); } catch (Exception e) { /* ignored */ }
						try { r.close(); } catch (Exception e) { /* ignored */ }
						try { conn.close(); } catch (Exception e) { /* ignored */ }
					}
				}
			}
			else {
				fields =  new String[] {"User", "Date", "Product", "Quantity", "Account No.", "Address", "Returned", "Notes", "Log No."};
				titleLabel.setText("Receive(+) Item Log");
				if(user != null) {
					String[] userParts = new String[2];
					userParts = user.split("\\s+");
					try {	
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
						s = conn.createStatement();
						// get table size
				        String sqlStatement = "SELECT COUNT(rc_id) FROM (receive LEFT JOIN employee ON receive.e_id = employee.e_id) WHERE e_lname = '" + userParts[1] + "' AND e_fname = '" + userParts[0] + "';";
				        r = s.executeQuery(sqlStatement);
				        while(r.next()) {
				        	length = Integer.parseInt(r.getString(1));
				        }
				        values = new String[length][fields.length];
				        s = conn.createStatement();
				        sqlStatement = "SELECT (employee.e_fname + ' ' + employee.e_lname) AS e_name, rc_date, product.p_name, quantity, acc_id, address, returned, notes, rc_id FROM (receive LEFT JOIN employee ON receive.e_id = employee.e_id LEFT JOIN product ON receive.p_id = product.p_id) WHERE e_lname = '" + userParts[1] + "' AND e_fname = '" + userParts[0] + "' ORDER BY rc_date DESC, e_name ASC, product.p_name ASC;";
				        r = s.executeQuery(sqlStatement);
				        while(r.next()) {
				        	for(int i = 0; i < fields.length; i++) {
				        		if(fields[i].equals("Returned")) {
				        			if(r.getString(i + 1).equals("0")) {
				        				values[count][i] = "No";
				        			}
				        			else {
				        				values[count][i] = "Yes";
				        			}
				        		}
				        		else{
				        			values[count][i] = r.getString(i + 1);
				        		}
				        	}
				        	count++;
				        }
					}
					catch(SQLException e) {
				    	JOptionPane.showMessageDialog(program, "There is a problem finding the items in the database.\n" + e.toString(), "Could not find items in DB", JOptionPane.WARNING_MESSAGE);
					}
			 		finally {
						try { s.close(); } catch (Exception e) { /* ignored */ }
						try { r.close(); } catch (Exception e) { /* ignored */ }
						try { conn.close(); } catch (Exception e) { /* ignored */ }
					}
				}
			}
		}
		else if(sortByName.equals("Product")) {
			values = null;
			if(checkOutRB.isSelected()) {
				fields =  new String[] {"Product", "Date", "User", "Account No.", "Address", "Defective", "Notes", "Log No."};
				titleLabel.setText("Check Out(-) Item Log");
				if(barcodeItem[0] != null) {
					try {	
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
						s = conn.createStatement();
						// get table size
				        String sqlStatement = "SELECT COUNT(co_id) FROM checkout WHERE p_id = '" + barcodeItem[0] + "';";
				        r = s.executeQuery(sqlStatement);
				        while(r.next()) {
				        	length = Integer.parseInt(r.getString(1));
				        }
				        values = new String[length][fields.length];
				        s = conn.createStatement();
				        sqlStatement = "SELECT product.p_name, co_date, (employee.e_fname + ' ' + employee.e_lname) AS e_name, acc_id, address, defective, notes, co_id FROM (checkout LEFT JOIN employee ON checkout.e_id = employee.e_id LEFT JOIN product ON checkout.p_id = product.p_id) WHERE product.p_id = '" + barcodeItem[0] + "' ORDER BY co_date DESC, e_name ASC, product.p_name ASC;";
				        r = s.executeQuery(sqlStatement);
				        while(r.next()) {
				        	for(int i = 0; i < fields.length; i++) {
				        		if(fields[i].equals("Defective")) {
				        			if(r.getString(i + 1).equals("0")) {
				        				values[count][i] = "No";
				        			}
				        			else {
				        				values[count][i] = "Yes";
				        			}
				        		}
				        		else{
				        			values[count][i] = r.getString(i + 1);
				        		}
				        	}
				        	count++;
				        }
					}
					catch(SQLException e) {
				    	JOptionPane.showMessageDialog(program, "There is a problem finding the items in the database.\n" + e.toString(), "Could not find items in DB", JOptionPane.WARNING_MESSAGE);
					}
			 		finally {
						try { s.close(); } catch (Exception e) { /* ignored */ }
						try { r.close(); } catch (Exception e) { /* ignored */ }
						try { conn.close(); } catch (Exception e) { /* ignored */ }
					}
				}
			}
			else {
				fields =  new String[] {"Product", "Date", "User", "Quantity", "Account No.", "Address", "Returned", "Notes", "Log No."};
				titleLabel.setText("Receive(+) Item Log");
				if(barcodeItem[0] != null) {
					try {	
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
						s = conn.createStatement();
						// get table size
				        String sqlStatement = "SELECT COUNT(rc_id) FROM receive WHERE p_id = '" + barcodeItem[0] + "';";
				        r = s.executeQuery(sqlStatement);
				        while(r.next()) {
				        	length = Integer.parseInt(r.getString(1));
				        }
				        values = new String[length][fields.length];
				        s = conn.createStatement();
				        sqlStatement = "SELECT product.p_name, rc_date, (employee.e_fname + ' ' + employee.e_lname) AS e_name, quantity, acc_id, address, returned, notes, rc_id FROM (receive LEFT JOIN employee ON receive.e_id = employee.e_id LEFT JOIN product ON receive.p_id = product.p_id) WHERE product.p_id = '" + barcodeItem[0] + "' ORDER BY rc_date DESC, e_name ASC, product.p_name ASC;";
				        r = s.executeQuery(sqlStatement);
				        while(r.next()) {
				        	for(int i = 0; i < fields.length; i++) {
				        		if(fields[i].equals("Returned")) {
				        			if(r.getString(i + 1).equals("0")) {
				        				values[count][i] = "No";
				        			}
				        			else {
				        				values[count][i] = "Yes";
				        			}
				        		}
				        		else{
				        			values[count][i] = r.getString(i + 1);
				        		}
				        	}
				        	count++;
				        }
					}
					catch(SQLException e) {
				    	JOptionPane.showMessageDialog(program, "There is a problem finding the items in the database.\n" + e.toString(), "Could not find items in DB", JOptionPane.WARNING_MESSAGE);
					}
			 		finally {
						try { s.close(); } catch (Exception e) { /* ignored */ }
						try { r.close(); } catch (Exception e) { /* ignored */ }
						try { conn.close(); } catch (Exception e) { /* ignored */ }
					}
				}
			}
		}
		newTableModel = new DefaultTableModel(values, fields) {
			@Override
		    public boolean isCellEditable(int row, int column) {
		       return false;
		    }
		};
		dtm = newTableModel;
		itemTable.setModel(dtm);
		if(itemTable.getRowCount() > 0) {
			itemTable.setRowSelectionInterval(0, 0);
		}
		if(sortByName.equals("Past 4 Weeks") || sortByName.equals("Date")) {
			itemTable.getColumnModel().getColumn(0).setPreferredWidth(screenSize.width/12);
			itemTable.getColumnModel().getColumn(1).setPreferredWidth(screenSize.width/9);
			itemTable.getColumnModel().getColumn(2).setPreferredWidth(screenSize.width/2);
			itemTable.getColumnModel().getColumn(3).setPreferredWidth(screenSize.width/12);
			
		}
		else if(sortByName.equals("User")) {
			itemTable.getColumnModel().getColumn(0).setPreferredWidth(screenSize.width/9);
			itemTable.getColumnModel().getColumn(1).setPreferredWidth(screenSize.width/12);
			itemTable.getColumnModel().getColumn(2).setPreferredWidth(screenSize.width/2);
			itemTable.getColumnModel().getColumn(3).setPreferredWidth(screenSize.width/12);
		}
		else if(sortByName.equals("Product")) {
			itemTable.getColumnModel().getColumn(0).setPreferredWidth(screenSize.width/2);
			itemTable.getColumnModel().getColumn(1).setPreferredWidth(screenSize.width/9);
			itemTable.getColumnModel().getColumn(2).setPreferredWidth(screenSize.width/9);
			itemTable.getColumnModel().getColumn(3).setPreferredWidth(screenSize.width/12);
		}
		if(checkOutRB.isSelected()) {
			itemTable.getColumnModel().getColumn(4).setPreferredWidth(screenSize.width/5);
			itemTable.getColumnModel().getColumn(5).setPreferredWidth(screenSize.width/12);
			itemTable.getColumnModel().getColumn(6).setPreferredWidth(screenSize.width/5);
			itemTable.getColumnModel().getColumn(7).setPreferredWidth(screenSize.width/19);
		}
		else {
			itemTable.getColumnModel().getColumn(4).setPreferredWidth(screenSize.width/12);
			itemTable.getColumnModel().getColumn(5).setPreferredWidth(screenSize.width/12);
			itemTable.getColumnModel().getColumn(6).setPreferredWidth(screenSize.width/19);
			itemTable.getColumnModel().getColumn(7).setPreferredWidth(screenSize.width/5);
			itemTable.getColumnModel().getColumn(8).setPreferredWidth(screenSize.width/19);
		}
		if(itemTable == null || itemTable.getRowCount() < 1) {
			editItemLogButton.setVisible(false);
			deleteItemLogButton.setVisible(false);
		}
		else {
			editItemLogButton.setVisible(true);
			deleteItemLogButton.setVisible(true);
		}
		itemTable.getColumnModel().removeColumn(itemTable.getColumnModel().getColumn(itemTable.getModel().getColumnCount() - 1));
		itemTableScrollPane.getVerticalScrollBar().setValue(0);
		itemTableScrollPane.getHorizontalScrollBar().setValue(0);
		itemSearchTextField.requestFocusInWindow();
	}
	public void viewItems() {
		itemSearchTextField.setText("");
		itemTableScrollPane.getVerticalScrollBar().setValue(0);
		itemTableScrollPane.getHorizontalScrollBar().setValue(0);
		refreshItemTable();
		itemSearchTextField.requestFocusInWindow();
	}
	
	public void viewKits() {
		kitName = null;
		ArrayList<String> kitLines = new ArrayList<String>();
		try {	
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
			s = conn.createStatement();
			// get table size
	        String sqlStatement = "SELECT p_name FROM product WHERE kit = '" + 1 + "';";
	        r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	kitLines.add(r.getString(1));
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem finding the kits in the database.\n" + e.toString(), "Could not find kits in DB", JOptionPane.WARNING_MESSAGE);
		}
 		finally {
			try { s.close(); } catch (Exception e) {  }
			try { r.close(); } catch (Exception e) {  }
			try { conn.close(); } catch (Exception e) {  }
		}
		
		String[] kits = new String[kitLines.size()];
		kitList = new JList<String>(kitLines.toArray(kits));
		kitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		kitList.setLayoutOrientation(JList.VERTICAL);
		kitList.setVisibleRowCount(-1);
		kitList.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		kitList.addMouseListener(this);
		
		kitScrollPane = new JScrollPane(kitList);
		kitScrollPane.setViewportView(kitList);
		kitScrollPane.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
	    kitScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    kitScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		
		kitPanel = new JPanel();
		kitPanel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		kitPanel.add(kitScrollPane);
		
		if(kitList.getModel().getSize() != 0) {
			kitList.setSelectedIndex(0);
		}
		
		String[] options = {"Edit Kit", "Cancel"};
		boolean exitEditKit = false;
		int result;
		while(!exitEditKit) {
			result = JOptionPane.showOptionDialog(program, kitPanel, "View Kits", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, null);
			if(result == 0) {
				if(kitList.getSelectedIndex() != -1) {
					kitName = kitList.getSelectedValue();
					editKit();
				}
			}
			else {
				exitEditKit = true;
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
		
		String[] matchedLineEntries = new String[5];
		
		try {
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
			s = conn.createStatement();
	        String sqlStatement = "SELECT p_id, p_name, instock, restock, kit FROM product WHERE p_id = '" + barcode + "';";
	        r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	matchedLineEntries[0] = r.getString(1);
	        	matchedLineEntries[1] = r.getString(2);
	        	matchedLineEntries[2] = r.getString(3);
	        	matchedLineEntries[3] = r.getString(4);
	        	matchedLineEntries[4] = r.getString(5);
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem finding the item in the database.\n" + e.toString(), "Could not find item in DB", JOptionPane.WARNING_MESSAGE);
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
			        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
					s = conn.createStatement();
			        String sqlStatement = "UPDATE product SET instock= " + (Integer.parseInt(replacedLine[2]) - selectedQuantity) + " WHERE p_id = '" + replacedLine[0] + "';";
			        s.execute(sqlStatement);
				}
				catch(SQLException e) {
			    	JOptionPane.showMessageDialog(program, "There is a problem subtracting item " + replacedLine[0] + " in the database.\n" + e.toString(), "Could not subtract item into DB", JOptionPane.WARNING_MESSAGE);
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
			        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
					s = conn.createStatement();
			        String sqlStatement = "UPDATE product SET instock= " + (Integer.parseInt(replacedLine[2]) + selectedQuantity) + " WHERE p_id = '" + replacedLine[0] + "';";
			        s.execute(sqlStatement);
				}
				catch(SQLException e) {
			    	JOptionPane.showMessageDialog(program, "There is a problem adding the item " + replacedLine[0] + " into the database.\n" + e.toString(), "Could not add item into DB", JOptionPane.WARNING_MESSAGE);
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
			barcodeTextField.requestFocusInWindow();
	    	JOptionPane.showMessageDialog(program, "Successfully updated items!", "Item Update Complete", JOptionPane.INFORMATION_MESSAGE);
		}
			
		itemsTextArea.setText("Quantity\tIn Stock\tItem\n");
		itemsOrder.clear();
		namesList.clear();
		quantityList.clear();
		itemsStack.clear();
		undoButton.setVisible(false);
	}
	
	public void updateLog(String operator, int quantity, String itemBarcode) {
		user = userList.getSelectedValue();
		String[] userParts = new String[2];
		userParts = user.split("\\s+");
		if(operator.equals("sub")) {
			try {	
				for(int i = 0; i < quantity; i++) {
			        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
					s = conn.createStatement();
			        String sqlStatement = "INSERT INTO checkout (co_date, e_id, p_id, address) SELECT '" + dtfSQL.format(LocalDateTime.now()) + "', e_id, '" + itemBarcode + "', NULL FROM employee WHERE e_lname = '" + userParts[1] + "' AND e_fname = '" + userParts[0] + "';";
			        s.execute(sqlStatement);
				}
			}
			catch(SQLException e) {
		    	JOptionPane.showMessageDialog(program, "There is a problem subtracting the amount from the database.\n" + e.toString(), "Could not subtract stock from DB", JOptionPane.WARNING_MESSAGE);
			}
			finally {
				try { s.close(); } catch (Exception e) { /* ignored */ }
				try { r.close(); } catch (Exception e) { /* ignored */ }
				try { conn.close(); } catch (Exception e) { /* ignored */ }
			}
		}
		else if(operator.equals("add")) {
			if(quantity != 0) {
				try {	
				    conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
					s = conn.createStatement();
				    String sqlStatement = "INSERT INTO receive (rc_date, e_id, p_id, quantity) SELECT '" + dtfSQL.format(LocalDateTime.now()) + "', e_id, '" + itemBarcode + "', " + quantity + " FROM employee WHERE e_lname = '" + userParts[1] + "' AND e_fname = '" + userParts[0] + "';";
				    s.execute(sqlStatement);
				}
				catch(SQLException e) {
			    	JOptionPane.showMessageDialog(program, "There is a problem adding the amount to the database.\n" + e.toString(), "Could not add stock to DB", JOptionPane.WARNING_MESSAGE);
				}
				finally {
					try { s.close(); } catch (Exception e) { /* ignored */ }
					try { r.close(); } catch (Exception e) { /* ignored */ }
					try { conn.close(); } catch (Exception e) { /* ignored */ }
				}
			}
		}
		/*
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
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
			s = conn.createStatement();
	        String sqlStatement = "SELECT COUNT(p_id) FROM product WHERE instock < restock;";
	        r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	restockingNumber = Integer.parseInt(r.getString(1));
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem determining the restocking numbers from the database.\n" + e.toString(), "Could not determine restock numbers", JOptionPane.WARNING_MESSAGE);
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
		
		String itemsFormatted = "Restock Inventory List: Created on " + dtf.format(LocalDateTime.now()) + "\n\nIn Stock\tItem\n";
		String currentItemName = "";
 		try {	
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
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
	    	JOptionPane.showMessageDialog(program, "There is a problem finding the item in the database.\n" + e.toString(), "Could not find item in DB", JOptionPane.WARNING_MESSAGE);
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
			JOptionPane.showMessageDialog(program, "Could not write to the restocking file.\n" + e.toString(), "Problem writing to file", JOptionPane.WARNING_MESSAGE);
		}
 		itemsTextArea.setText(itemsFormatted);
	}
	public void updateReports() {
		itemsTextArea.setText("");
		String itemsFormatted = "";
		int totalItems = 0;
		String currentItemName = "";
		if(reportType.equals("Usage")) {
			totalItems = 0;
			if(dateOption.equals("Year")) {
				itemsFormatted = "Reports : Created on " + dtf.format(LocalDateTime.now()) + "\n\nUsage Report for " + year  + "\n\n";
			}
			else if(dateOption.equals("Month")) {
				itemsFormatted = "Reports : Created on " + dtf.format(LocalDateTime.now()) + "\n\nUsage Report for " + months[month - 1] + " " + year  + "\n\n";
			}
			else if(dateOption.equals("Day")) {
				itemsFormatted = "Reports : Created on " + dtf.format(LocalDateTime.now()) + "\n\nUsage Report for " + months[month - 1] + " " + day + " " + year  + "\n\n";
			}
			try {	
		        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
				s = conn.createStatement();
				String sqlStatement = "";
				if(dateOption.equals("Year")) {
		        	sqlStatement = "SELECT COUNT(co_id), p_name, employee.e_fname FROM (checkout LEFT JOIN product ON checkout.p_id = product.p_id LEFT JOIN employee ON checkout.e_id = employee.e_id) WHERE YEAR(checkout.co_date) = " + year + " GROUP BY p_name, employee.e_fname;";
				}
				else if(dateOption.equals("Month")) {
			        sqlStatement = "SELECT COUNT(co_id), p_name, employee.e_fname FROM (checkout LEFT JOIN product ON checkout.p_id = product.p_id LEFT JOIN employee ON checkout.e_id = employee.e_id) WHERE MONTH(checkout.co_date) = " + month + " AND YEAR(checkout.co_date) = " + year + " GROUP BY p_name, employee.e_fname;";

				}
				else if(dateOption.equals("Day")) {
			        sqlStatement = "SELECT COUNT(co_id), p_name, employee.e_fname FROM (checkout LEFT JOIN product ON checkout.p_id = product.p_id LEFT JOIN employee ON checkout.e_id = employee.e_id) WHERE DAY(checkout.co_date) = " + day + "AND MONTH(checkout.co_date) = " + month + " AND YEAR(checkout.co_date) = " + year + " GROUP BY p_name, employee.e_fname;";
				}
		        r = s.executeQuery(sqlStatement);
			    while(r.next()) {
			    	currentItemName = r.getString(2);
		        	if(currentItemName.length() > 80) {
						currentItemName = currentItemName.substring(0,80) + "...";
					}
			    	itemsFormatted += r.getString(1) + "\t" + r.getString(3) + "\t" + currentItemName + "\n";
			    	totalItems += Integer.parseInt(r.getString(1));
			    }
			    if(totalItems != 0) {
			    	itemsFormatted += "\nTotal Check Out: " + totalItems + "\n\n";
			    }
			}
			catch(SQLException e) {
		    	JOptionPane.showMessageDialog(program, "There is a problem finding check out item logs for reports.\n" + e.toString(), "Could not find check out item logs in DB", JOptionPane.WARNING_MESSAGE);
			}
	 		finally {
				try { s.close(); } catch (Exception e) { /* ignored */ }
				try { r.close(); } catch (Exception e) { /* ignored */ }
				try { conn.close(); } catch (Exception e) { /* ignored */ }
			}
			
		}
		else if(reportType.equals("Snapshot")) {
			itemsFormatted = "Reports : Created on " + dtf.format(LocalDateTime.now()) + "\n\nSnapshot Report for beginning of " + months[month - 1] + " " + day + " " + year + "\n\n";
			try {	
		        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
				s = conn.createStatement();
				// gets the snapshot of inventory by reversing the changes done to it
		        String sqlStatement = 	"SELECT a.p_name, SUM(a.total) FROM " + 
		        							"((SELECT p_name, SUM(instock) total FROM ((SELECT p_id, p_name, instock FROM product) UNION ALL (SELECT edited.p_id, p_name, quantity * -1 as instock FROM (edited LEFT JOIN product on edited.p_id = product.p_id) WHERE edit_date BETWEEN '" + month + "-" + day + "-" + year + "' AND '" + dtfSQL.format(LocalDateTime.now()) + "')) t GROUP BY p_name) UNION ALL " + 
		        							"(SELECT p_name, SUM(1) total FROM (product LEFT JOIN checkout ON product.p_id = checkout.p_id) WHERE checkout.co_date BETWEEN '" + month + "-" + day + "-" + year + "' AND '" + dtfSQL.format(LocalDateTime.now()) + "' GROUP BY p_name) UNION ALL " + 
		        							"(SELECT p_name, SUM(quantity * -1) total FROM (product LEFT JOIN receive ON product.p_id = receive.p_id) WHERE receive.rc_date BETWEEN '" + month + "-" + day + "-" + year + "' AND '" + dtfSQL.format(LocalDateTime.now()) + "' GROUP BY p_name)) AS a " + 
		        						"GROUP BY a.p_name;";
		        r = s.executeQuery(sqlStatement);
			    while(r.next()) {
			    	currentItemName = r.getString(1);
		        	if(currentItemName.length() > 70) {
						currentItemName = currentItemName.substring(0,70) + "...";
					}
			    	itemsFormatted += r.getString(2) + "\t" + currentItemName + "\n";
			    }
			}
			catch(SQLException e) {
		    	JOptionPane.showMessageDialog(program, "There is a problem finding item quantities for reports.\n" + e.toString(), "Could not find item information from DB", JOptionPane.WARNING_MESSAGE);
			}
	 		finally {
				try { s.close(); } catch (Exception e) { /* ignored */ }
				try { r.close(); } catch (Exception e) { /* ignored */ }
				try { conn.close(); } catch (Exception e) { /* ignored */ }
			}
		}
		try {
 			File file = new File("Reports.csv");
 			file.setWritable(true);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(itemsFormatted.replaceAll("\\t", ",,"));
			file.setWritable(false);
			bw.close();
		}
		catch(IOException e){
			JOptionPane.showMessageDialog(program, "Could not write to the reports file.\n" + e.toString(), "Problem writing to file", JOptionPane.WARNING_MESSAGE);
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
		int totalItems = 0;
		for(int i = 0; i < itemsOrder.size(); i++) {
			itemsFormatted += (quantityList.get(itemsOrder.get(i)) + "\t" + findItem(itemsOrder.get(i))[2] + "\t" + namesList.get(itemsOrder.get(i)) + "\n");
			totalItems += quantityList.get(itemsOrder.get(i));
		}
		itemsFormatted += "\nTotal Items: " + totalItems;
		return itemsFormatted;
	}
	
	public String createSearchTermsString(String searchTerms, String tName) {
		String[] itemSearchInputParts = searchTerms.split("\\s+");
		String searchTermsString = "";
		if(!tName.equals("")) {
			searchTermsString = "(" + tName + ".p_name LIKE '%";
		}
		else {
			searchTermsString = "(p_name LIKE '%";
		}
		for(int i = 0; i < itemSearchInputParts.length; i++) {
			if(i == itemSearchInputParts.length - 1) {
				searchTermsString += itemSearchInputParts[i];
			}
			else {
				if(!tName.equals("")) {
					searchTermsString += itemSearchInputParts[i] + "%' AND " + tName + ".p_name LIKE '%";
				}
				else {
					searchTermsString += itemSearchInputParts[i] + "%' AND p_name LIKE '%";
				}
			}
		}
		searchTermsString += "%')";
		return searchTermsString;
	}
	public void refreshKitItemTable() {
		String[] fields;
		String[][] values;
		DefaultTableModel newTableModel;
		int length = 0;
		int count = 0;
		values = null;
		fields =  new String[] {"Quantity", "Kit Item", "Kit Item ID"};
		try {	
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
			s = conn.createStatement();
			// get table size
	        String sqlStatement = "SELECT COUNT(kitItem_id) FROM (kitItem LEFT JOIN product p1 ON kitItem.kitItem_product = p1.p_id) LEFT JOIN product ON kitItem.kit_product = product.p_id WHERE product.p_name = '" + kitName +"';";
	        r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	length = Integer.parseInt(r.getString(1));
	        }
	        if (length > 0) {
	        	values = new String[length][fields.length];
		        s = conn.createStatement();
		        sqlStatement = "SELECT quantity, p1.p_name, kitItem_id FROM (kitItem LEFT JOIN product p1 ON kitItem.kitItem_product = p1.p_id) LEFT JOIN product ON kitItem.kit_product = product.p_id WHERE product.p_name = '" + kitName +"';";
		        r = s.executeQuery(sqlStatement);
		        while(r.next()) {
		        	for(int i = 0; i < fields.length; i++) {
			        	values[count][i] = r.getString(i + 1);
		        	}
		        	count++;
		        }
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem finding the kit items in the database.\n" + e.toString(), "Could not find kit items in DB", JOptionPane.WARNING_MESSAGE);
		}
 		finally {
			try { s.close(); } catch (Exception e) {    }
			try { r.close(); } catch (Exception e) {    }
			try { conn.close(); } catch (Exception e) {    }
		}
		
		newTableModel = new DefaultTableModel(values, fields) {
			@Override
		    public boolean isCellEditable(int row, int column) {
		       return false;
		    }
		};
		
		kitItemTable.setModel(newTableModel);
		kitItemTable.getColumnModel().getColumn(0).setPreferredWidth(screenSize.width/12);
		kitItemTable.getColumnModel().getColumn(1).setPreferredWidth(screenSize.width/3);

		if(kitItemTable.getRowCount() > 0) {
			kitItemTable.setRowSelectionInterval(0, 0);
		}	    
		kitItemTable.getColumnModel().removeColumn(kitItemTable.getColumnModel().getColumn(kitItemTable.getModel().getColumnCount() - 1));
	}
	public void refreshItemTable() {
		String itemSearchInput = itemSearchTextField.getText().trim();
    	String[] fields;
		String[][] values;
		DefaultTableModel newTableModel;
		int length = 0;
		int count = 0;
		values = null;
		fields =  new String[] {"Name", "Barcode", "In Stock", "Restock", "Kit"};
		try {	
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
			s = conn.createStatement();
			// get table size
	        String sqlStatement = 	"SELECT COUNT(*) FROM (SELECT p_name, p_id, instock, restock, kit FROM product WHERE " + createSearchTermsString(itemSearchInput, "") + " OR p_id = '" + itemSearchInput + "' UNION " + 
	        			        	"SELECT p2.p_name, p2.p_id, p2.instock, p2.restock, p2.kit FROM ((product p1 LEFT JOIN kitItem ON kit_product = p1.p_id) LEFT JOIN product p2 ON kitItem_product = p2.p_id) WHERE (p2.p_id = kitItem.kitItem_product AND " + createSearchTermsString(itemSearchInput, "p1") + ")) t;";
	        r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	length = Integer.parseInt(r.getString(1));
	        }
	        if(length < 1) {
	        	editItemButton.setVisible(false);
	        }
	        else {
	        	editItemButton.setVisible(true);
	        }
	        values = new String[length][fields.length];
	        s = conn.createStatement();
	        sqlStatement = 			"SELECT p_name, p_id, instock, restock, kit FROM product WHERE " + createSearchTermsString(itemSearchInput, "") + " OR p_id = '" + itemSearchInput + "' UNION " + 
	        						"SELECT p2.p_name, p2.p_id, p2.instock, p2.restock, p2.kit FROM ((product p1 LEFT JOIN kitItem ON kit_product = p1.p_id) LEFT JOIN product p2 ON kitItem_product = p2.p_id) WHERE (p2.p_id = kitItem.kitItem_product AND " + createSearchTermsString(itemSearchInput, "p1") + ");";
	        r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	for(int i = 0; i < fields.length; i++) {
	        		if(i == fields.length - 1) {
	        			if(r.getString(i + 1).equals("0")) {
	        				values[count][i] = "No";
	        			}
	        			else {
	        				values[count][i] = "Yes";
	        			}
	        		}
	        		else {
		        		values[count][i] = r.getString(i + 1);
	        		}
	        	}
	        	count++;
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem finding the items in the database.\n" + e.toString(), "Could not find items in DB", JOptionPane.WARNING_MESSAGE);
		}
 		finally {
			try { s.close(); } catch (Exception e) { /* ignored */ }
			try { r.close(); } catch (Exception e) { /* ignored */ }
			try { conn.close(); } catch (Exception e) { /* ignored */ }
		}
		newTableModel = new DefaultTableModel(values, fields) {
			@Override
		    public boolean isCellEditable(int row, int column) {
		       return false;
		    }
		};
		dtm = newTableModel;
		itemTable.setModel(dtm);
		itemTable.getColumnModel().getColumn(0).setPreferredWidth(screenSize.width/2);
		itemTable.getColumnModel().getColumn(1).setPreferredWidth(screenSize.width/6);
		itemTable.getColumnModel().getColumn(2).setPreferredWidth(screenSize.width/12);
		itemTable.getColumnModel().getColumn(3).setPreferredWidth(screenSize.width/12);
		itemTable.getColumnModel().getColumn(4).setPreferredWidth(screenSize.width/19);

		if(itemTable.getRowCount() > 0) {
			itemTable.setRowSelectionInterval(0, 0);
		}
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
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
					usersButton.setVisible(true);
					checkOutButton.setVisible(true);
					receiveButton.setVisible(true);
					editItemButton.setVisible(false);
					viewItemsButton.setVisible(true);
					barcodeLabel.setVisible(false);
					scanQuantityLabel.setVisible(false);
					barcodeTextField.setVisible(false);
					itemSearchLabel.setVisible(false);
					itemSearchTextField.setVisible(false);
					scanSpinner.setVisible(false);
					newItemButton.setVisible(false);
					viewKitsButton.setVisible(false);
					editItemLogButton.setVisible(false);
					deleteItemLogButton.setVisible(false);
					checkOutRB.setVisible(false);
					receiveRB.setVisible(false);
					doneButton.setVisible(false);
					printButton.setVisible(false);
					undoButton.setVisible(false);
					usageReportButton.setVisible(false);
					snapshotReportButton.setVisible(false);
					productReportButton.setVisible(false);
					itemsTextArea.setVisible(false);
					itemsScrollPane.setVisible(false);
					itemTableScrollPane.setVisible(false);
				}
		        updateRestockingNumber();
				barcodeTextField.requestFocusInWindow();
			}
			else {
				titleLabel.setText("Inventory Management");
				usersButton.setVisible(true);
				checkOutButton.setVisible(true);
				receiveButton.setVisible(true);
				editItemButton.setVisible(false);
				viewItemsButton.setVisible(true);
				barcodeLabel.setVisible(false);
				scanQuantityLabel.setVisible(false);
				barcodeTextField.setVisible(false);
				itemSearchLabel.setVisible(false);
				itemSearchTextField.setVisible(false);
				scanSpinner.setVisible(false);
				newItemButton.setVisible(false);
				viewKitsButton.setVisible(false);
				editItemLogButton.setVisible(false);
				deleteItemLogButton.setVisible(false);
				checkOutRB.setVisible(false);
				receiveRB.setVisible(false);
				doneButton.setVisible(false);
				printButton.setVisible(false);
				usageReportButton.setVisible(false);
				snapshotReportButton.setVisible(false);
				productReportButton.setVisible(false);
				itemsTextArea.setVisible(false);
				itemsScrollPane.setVisible(false);
				itemTableScrollPane.setVisible(false);
		        updateRestockingNumber();
			}
		}
		else if(e.getSource() == reportsButton) {
			if(!namesList.isEmpty()) {
				int choice = JOptionPane.showConfirmDialog(	program, 
												"You are not finished entering in your items. All progress will be lost. Are you sure you want to exit?" ,
												"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(choice == 0) {
					titleLabel.setText("Reports");
					usersButton.setVisible(false);
					checkOutButton.setVisible(false);
					receiveButton.setVisible(false);
					editItemButton.setVisible(false);
					viewItemsButton.setVisible(false);
					barcodeLabel.setVisible(false);
					scanQuantityLabel.setVisible(false);
					barcodeTextField.setVisible(false);
					itemSearchLabel.setVisible(false);
					itemSearchTextField.setVisible(false);
					scanSpinner.setVisible(false);
					newItemButton.setVisible(false);
					viewKitsButton.setVisible(false);
					editItemLogButton.setVisible(false);
					deleteItemLogButton.setVisible(false);
					checkOutRB.setVisible(false);
					receiveRB.setVisible(false);
					doneButton.setVisible(false);
					printButton.setVisible(true);
					undoButton.setVisible(false);
					usageReportButton.setVisible(true);
					snapshotReportButton.setVisible(true);
					// productReportButton.setVisible(true);
					itemsTextArea.setVisible(true);
					itemsScrollPane.setVisible(true);
					itemTableScrollPane.setVisible(false);
					reportType = "Usage";
					dateOption = "Day";
					year = LocalDate.now().getYear();
					month = LocalDate.now().getMonthValue();
					day = LocalDate.now().getDayOfMonth();
					updateReports();
					itemsTextArea.setCaretPosition(0);
				}
			}
			else {
				titleLabel.setText("Reports");
				usersButton.setVisible(false);
				checkOutButton.setVisible(false);
				receiveButton.setVisible(false);
				editItemButton.setVisible(false);
				viewItemsButton.setVisible(false);
				barcodeLabel.setVisible(false);
				scanQuantityLabel.setVisible(false);
				barcodeTextField.setVisible(false);
				itemSearchLabel.setVisible(false);
				itemSearchTextField.setVisible(false);
				scanSpinner.setVisible(false);
				newItemButton.setVisible(false);
				viewKitsButton.setVisible(false);
				editItemLogButton.setVisible(false);
				deleteItemLogButton.setVisible(false);
				checkOutRB.setVisible(false);
				receiveRB.setVisible(false);
				doneButton.setVisible(false);
				printButton.setVisible(true);
				usageReportButton.setVisible(true);
				snapshotReportButton.setVisible(true);
				//productReportButton.setVisible(true);
				itemsTextArea.setVisible(true);
				itemsScrollPane.setVisible(true);
				itemTableScrollPane.setVisible(false);
				reportType = "Usage";
				dateOption = "Day";
				year = LocalDate.now().getYear();
				month = LocalDate.now().getMonthValue();
				day = LocalDate.now().getDayOfMonth();
				updateReports();
				itemsTextArea.setCaretPosition(0);
			}
		}
		else if(e.getSource() == restockButton) {
			if(!namesList.isEmpty()) {
				int choice = JOptionPane.showConfirmDialog(	program, 
												"You are not finished entering in your items. All progress will be lost. Are you sure you want to exit?" ,
												"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(choice == 0) {
					titleLabel.setText("Restock List");
					usersButton.setVisible(false);
					checkOutButton.setVisible(false);
					receiveButton.setVisible(false);
					editItemButton.setVisible(false);
					viewItemsButton.setVisible(false);
					barcodeLabel.setVisible(false);
					scanQuantityLabel.setVisible(false);
					barcodeTextField.setVisible(false);
					itemSearchLabel.setVisible(false);
					itemSearchTextField.setVisible(false);
					scanSpinner.setVisible(false);
					newItemButton.setVisible(false);
					viewKitsButton.setVisible(false);
					editItemLogButton.setVisible(false);
					deleteItemLogButton.setVisible(false);
					checkOutRB.setVisible(false);
					receiveRB.setVisible(false);
					doneButton.setVisible(false);
					printButton.setVisible(true);
					undoButton.setVisible(false);
					usageReportButton.setVisible(false);
					snapshotReportButton.setVisible(false);
					productReportButton.setVisible(false);
					itemsTextArea.setVisible(true);
					itemsScrollPane.setVisible(true);
					itemTableScrollPane.setVisible(false);
					updateRestockingItems();
					itemsTextArea.setCaretPosition(0);
				}
			}
			else {
				titleLabel.setText("Restock List");
				usersButton.setVisible(false);
				checkOutButton.setVisible(false);
				receiveButton.setVisible(false);
				editItemButton.setVisible(false);
				viewItemsButton.setVisible(false);
				barcodeLabel.setVisible(false);
				scanQuantityLabel.setVisible(false);
				barcodeTextField.setVisible(false);
				itemSearchLabel.setVisible(false);
				itemSearchTextField.setVisible(false);
				scanSpinner.setVisible(false);
				newItemButton.setVisible(false);
				viewKitsButton.setVisible(false);
				editItemLogButton.setVisible(false);
				deleteItemLogButton.setVisible(false);
				checkOutRB.setVisible(false);
				receiveRB.setVisible(false);
				doneButton.setVisible(false);
				printButton.setVisible(true);
				usageReportButton.setVisible(false);
				snapshotReportButton.setVisible(false);
				productReportButton.setVisible(false);
				itemsTextArea.setVisible(true);
				itemsScrollPane.setVisible(true);
				itemTableScrollPane.setVisible(false);
				updateRestockingItems();
				itemsTextArea.setCaretPosition(0);
			}
		}
		else if(e.getSource() == itemLogButton) {
			if(!namesList.isEmpty()) {
				int choice = JOptionPane.showConfirmDialog(	program, 
												"You are not finished entering in your items. All progress will be lost. Are you sure you want to exit?" ,
												"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(choice == 0) {
					titleLabel.setText("Check Out(-) Item Log");
					usersButton.setVisible(false);
					checkOutButton.setVisible(false);
					receiveButton.setVisible(false);
					editItemButton.setVisible(false);
					viewItemsButton.setVisible(false);
					barcodeLabel.setVisible(false);
					scanQuantityLabel.setVisible(false);
					barcodeTextField.setVisible(false);
					itemSearchLabel.setVisible(false);
					itemSearchTextField.setVisible(false);
					scanSpinner.setVisible(false);
					newItemButton.setVisible(false);
					viewKitsButton.setVisible(false);
					editItemLogButton.setVisible(true);
					deleteItemLogButton.setVisible(true);
					checkOutRB.setVisible(true);
					receiveRB.setVisible(true);
					doneButton.setVisible(false);
					printButton.setVisible(false);
					undoButton.setVisible(false);
					usageReportButton.setVisible(false);
					snapshotReportButton.setVisible(false);
					productReportButton.setVisible(false);
					itemsTextArea.setVisible(true);
					itemsScrollPane.setVisible(false);
					itemTableScrollPane.setVisible(true);
					itemsTextArea.setCaretPosition(0);
					checkOutRB.setSelected(true);
					if(itemTable != null) {
						while(dtm.getRowCount() > 0) {
							dtm.removeRow(0);
						}
					}
					sortByName = "Past 4 Weeks";
					sortByAndShow();
				}
			}
			else {
				titleLabel.setText("Check Out(-) Item Log");
				usersButton.setVisible(false);
				checkOutButton.setVisible(false);
				receiveButton.setVisible(false);
				editItemButton.setVisible(false);
				viewItemsButton.setVisible(false);
				barcodeLabel.setVisible(false);
				scanQuantityLabel.setVisible(false);
				barcodeTextField.setVisible(false);
				itemSearchLabel.setVisible(false);
				itemSearchTextField.setVisible(false);
				scanSpinner.setVisible(false);
				newItemButton.setVisible(false);
				viewKitsButton.setVisible(false);
				editItemLogButton.setVisible(true);
				deleteItemLogButton.setVisible(true);
				checkOutRB.setVisible(true);
				receiveRB.setVisible(true);
				doneButton.setVisible(false);
				printButton.setVisible(false);
				usageReportButton.setVisible(false);
				snapshotReportButton.setVisible(false);
				productReportButton.setVisible(false);
				itemsTextArea.setVisible(true);
				itemsScrollPane.setVisible(false);
				itemTableScrollPane.setVisible(true);
				itemsTextArea.setCaretPosition(0);
				checkOutRB.setSelected(true);
				if(itemTable != null) {
					while(dtm.getRowCount() > 0) {
						dtm.removeRow(0);
					}
				}
				sortByName = "Past 4 Weeks";
				year = LocalDate.now().getYear();
				sortByAndShow();
				if(itemTable == null || itemTable.getRowCount() < 1) {
					editItemLogButton.setVisible(false);
					deleteItemLogButton.setVisible(false);
				}
				else {
					editItemLogButton.setVisible(true);
					deleteItemLogButton.setVisible(true);
				}
			}
			
		}
		else if(e.getSource() == usersButton) {
			editUsers();
		}
		else if(e.getSource() == checkOutButton) {
			titleLabel.setText("Check Out(-)");
			itemsTextArea.setText("Quantity\tIn Stock\tItem\n\nTotal Items: 0");
			itemsOrder.clear();
			namesList.clear();
			quantityList.clear();
			itemsStack.clear();
			scanQuantity.setValue(1);
			usersButton.setVisible(false);
			checkOutButton.setVisible(false);
			receiveButton.setVisible(false);
			editItemButton.setVisible(false);
			viewItemsButton.setVisible(false);
			barcodeLabel.setVisible(true);
			scanQuantityLabel.setVisible(true);
			barcodeTextField.setVisible(true);
			itemSearchLabel.setVisible(false);
			itemSearchTextField.setVisible(false);
			scanSpinner.setVisible(true);
			doneButton.setVisible(true);
			editItemLogButton.setVisible(false);
			deleteItemLogButton.setVisible(false);
			checkOutRB.setVisible(false);
			receiveRB.setVisible(false);
			newItemButton.setVisible(false);
			viewKitsButton.setVisible(false);
			itemsTextArea.setVisible(true);
			itemsScrollPane.setVisible(true);
			itemTableScrollPane.setVisible(false);
			barcodeTextField.requestFocusInWindow();
		}
		else if(e.getSource() == receiveButton) {
			titleLabel.setText("Receive(+)");
			itemsTextArea.setText("Quantity\tIn Stock\tItem\n\nTotal Items: 0");
			itemsOrder.clear();
			namesList.clear();
			quantityList.clear();
			itemsStack.clear();
			scanQuantity.setValue(1);
			usersButton.setVisible(false);
			checkOutButton.setVisible(false);
			receiveButton.setVisible(false);
			editItemButton.setVisible(false);
			viewItemsButton.setVisible(false);
			barcodeLabel.setVisible(true);
			scanQuantityLabel.setVisible(true);
			barcodeTextField.setVisible(true);
			itemSearchLabel.setVisible(false);
			itemSearchTextField.setVisible(false);
			scanSpinner.setVisible(true);
			doneButton.setVisible(true);
			editItemLogButton.setVisible(false);
			deleteItemLogButton.setVisible(false);
			checkOutRB.setVisible(false);
			receiveRB.setVisible(false);
			newItemButton.setVisible(true);
			viewKitsButton.setVisible(false);
			itemsTextArea.setVisible(true);
			itemsScrollPane.setVisible(true);
			itemTableScrollPane.setVisible(false);
			barcodeTextField.requestFocusInWindow();
		}
		else if(e.getSource() == viewItemsButton) {
			titleLabel.setText("View Items");
			usersButton.setVisible(false);
			checkOutButton.setVisible(false);
			receiveButton.setVisible(false);
			editItemButton.setVisible(true);
			viewItemsButton.setVisible(false);
			barcodeLabel.setVisible(false);
			scanQuantityLabel.setVisible(false);
			barcodeTextField.setVisible(false);
			itemSearchLabel.setVisible(true);
			itemSearchTextField.setVisible(true);
			scanSpinner.setVisible(false);
			newItemButton.setVisible(true);
			viewKitsButton.setVisible(true);
			editItemLogButton.setVisible(false);
			deleteItemLogButton.setVisible(false);
			checkOutRB.setVisible(false);
			receiveRB.setVisible(false);
			doneButton.setVisible(false);
			printButton.setVisible(false);
			usageReportButton.setVisible(false);
			snapshotReportButton.setVisible(false);
			productReportButton.setVisible(false);
			itemsTextArea.setVisible(true);
			itemsScrollPane.setVisible(false);
			itemTableScrollPane.setVisible(true);
			itemsTextArea.setCaretPosition(0);
			viewItems();
		}
		else if(e.getSource() == viewKitsButton) {
			viewKits();
		}
		else if(e.getSource() == editItemButton) {
			itemSearchTextField.requestFocusInWindow();
			editItem();
		}
		else if(e.getSource() == editItemLogButton) {
			itemLog = Integer.parseInt(itemTable.getModel().getValueAt(itemTable.getSelectedRow(), itemTable.getModel().getColumnCount() - 1).toString());
			editItemLog(itemLog);
		}
		else if(e.getSource() == deleteItemLogButton) {
			deleteItemLog();
		}
		else if(e.getSource() == doneButton) {
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
					if(user != null) {
						enterPassword();
						if(passwordCorrect) {
							if(titleLabel.getText().equals("Check Out(-)")) {
								calculateInventory("checkOut");
							}
		
							if(titleLabel.getText().equals("Receive(+)")) {
								calculateInventory("receive");
							}
						}
					}
				}
			}
			barcodeTextField.requestFocusInWindow();
		}
		else if(e.getSource() == printButton) {
			try {
				Desktop desktop = null;
				if(Desktop.isDesktopSupported()) {
					desktop = Desktop.getDesktop();
				}
				if(titleLabel.getText().equals("Restock List")) {
					desktop.print(new File("Restock.csv"));
					JOptionPane.showMessageDialog(program, "Successfully printed restock list!", "Restock list printed", JOptionPane.PLAIN_MESSAGE);
				}
				else if(titleLabel.getText().equals("Reports")) {
					desktop.print(new File("Reports.csv"));
					JOptionPane.showMessageDialog(program, "Successfully printed report!", "Report printed", JOptionPane.PLAIN_MESSAGE);
				}
			}
			catch(IOException ioe) {
				JOptionPane.showMessageDialog(program, "Could not print the specified document.\n" + e.toString(), "Problem with printing", JOptionPane.WARNING_MESSAGE);
			}
		}
		else if(e.getSource() == newItemButton) {
			createNewItem();
		}
		else if(e.getSource() == undoButton) {
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
		else if(e.getSource() == yearRB) {
			if(!reportType.equals("Snapshot")) {
				monthComboBox.setEnabled(false);
				dayComboBox.setEnabled(false);
			}
		}
		else if(e.getSource() == monthRB) {
			monthComboBox.setEnabled(true);
			if(!reportType.equals("Snapshot")) {
				dayComboBox.setEnabled(false);
			}
		}
		else if(e.getSource() == dayRB) {
			monthComboBox.setEnabled(true);
			dayComboBox.setEnabled(true);
		}
		else if(e.getSource() == checkOutRB) {
			checkOutRB.setSelected(true);
			chooseSortBy();
		}
		else if(e.getSource() == receiveRB) {
			receiveRB.setSelected(true);
			chooseSortBy();
		}
		else if(e.getSource() == itemTable) {
			if((titleLabel.getText().equals("Check Out(-) Item Log") || titleLabel.getText().equals("Receive(+) Item Log")) && e.getClickCount() == 2 && itemTable.getSelectedRow() != -1) {
				editItemLog(Integer.parseInt(itemTable.getModel().getValueAt(itemTable.getSelectedRow(), itemTable.getModel().getColumnCount() - 1).toString()));
			}
			else if(titleLabel.getText().equals("View Items") && e.getClickCount() == 2 && itemTable.getSelectedRow() != -1) {
				editItem();
			}
		}
		else if(e.getSource() == itemSearchTextField) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					itemSearchTextField.selectAll();
				}
			});
		}
		else if(e.getSource() == usageReportButton) {
			reportType = "Usage";
			chooseDate();
			updateReports();
			itemsTextArea.setCaretPosition(0);
		}
		else if(e.getSource() == snapshotReportButton) {
			reportType = "Snapshot";
			chooseDate();
			updateReports();
			itemsTextArea.setCaretPosition(0);
		}
		/*
		 * Save this for future implementation
		 * 
		else if(e.getSource() == productReportButton) {
			reportType = "Product";
			chooseBarcodeOrKeyword();
			if(barcodeItem[0] != null) {
				chooseDate();
				updateReports();
			}
			itemsTextArea.setCaretPosition(0);
		}
		*/
		else if(e.getSource() == userList) {
			if(e.getClickCount() == 2 && userList.getSelectedIndex() != -1) {
				Component component = (Component) e.getSource();
		        JDialog dialog = (JDialog) SwingUtilities.getRoot(component);
		        user = userList.getSelectedValue();
				if(user != null) {
					if(titleLabel.getText().equals("Check Out(-) Item Log") || titleLabel.getText().equals("Receive(+) Item Log")) {
						sortByAndShow();
					}
					dialog.dispose();
				}
			}
		}
		else if(e.getSource() == productList) {
			if(e.getClickCount() == 2 && productList.getSelectedIndex() != -1) {
				Component component = (Component) e.getSource();
		        JDialog dialog = (JDialog) SwingUtilities.getRoot(component);
		        productName = productList.getSelectedValue();
				if(productName != null) {
					try {
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
						s = conn.createStatement();
				        String sqlStatement = "SELECT p_id, p_name, instock, restock, kit FROM product WHERE p_name = '" + productName + "' ORDER BY p_name ASC;";
				        r = s.executeQuery(sqlStatement);
				        while(r.next()) {
				        	barcodeItem[0] = r.getString(1);
				        	barcodeItem[1] = r.getString(2);
				        	barcodeItem[2] = r.getString(3);
				        	barcodeItem[3] = r.getString(4);
				        	barcodeItem[4] = r.getString(5);
				        }
					}
					catch(SQLException sqle) {
				    	JOptionPane.showMessageDialog(program, "There is a problem finding the item in the database.\n" + sqle.toString(), "Could not find item in DB", JOptionPane.WARNING_MESSAGE);
					}
					finally {
						try { s.close(); } catch (Exception e1) { /* ignored */ }
						try { r.close(); } catch (Exception e2) { /* ignored */ }
						try { conn.close(); } catch (Exception e3) { /* ignored */ }
					}
				}
		        dialog.dispose();
			}
		}
		else if(e.getSource() == sortByList) {
			if(e.getClickCount() == 2 && sortByList.getSelectedIndex() != -1) {
				Component component = (Component) e.getSource();
		        JDialog dialog = (JDialog) SwingUtilities.getRoot(component);
		        if(sortByList.getSelectedValue().equals("Past 4 Weeks")) {
					sortByName = "Past 4 Weeks";
					sortByAndShow();
				}
				else if(sortByList.getSelectedValue().equals("Date")) {
					sortByName = "Date";
					chooseDate();
				}
				else if(sortByList.getSelectedValue().equals("User")) {
					sortByName = "User";
					chooseUser();
				}
				else if(sortByList.getSelectedValue().equals("Product")) {
					sortByName = "Product";
					chooseBarcodeOrKeyword();
				}
		        if(itemTable == null || itemTable.getRowCount() < 1) {
					editItemLogButton.setVisible(false);
					deleteItemLogButton.setVisible(false);
				}
				else {
					editItemLogButton.setVisible(true);
					deleteItemLogButton.setVisible(true);
				}
		        dialog.dispose();
			}
		}
		else if(e.getSource() == kitList){
			if(e.getClickCount() == 2 && kitList.getSelectedIndex() != -1) {
				if(kitList.getSelectedIndex() != -1) {
					kitName = kitList.getSelectedValue();
					editKit();
				}
			}
		}
		else if(e.getSource() == kitItemTable) {
			if(e.getClickCount() == 2 && kitItemTable.getSelectedRow() != -1) {
				kitItem = kitItemTable.getValueAt(kitItemTable.getSelectedRow(), kitItemTable.getColumnModel().getColumnCount() - 1).toString();
				editKitItem("edit");
			}
		}
	}
	
	public void searchKeywords() {
		productName = null;
		productLines.clear();
		String itemSearchInput = barcodeInput;
		String searchTermsString = createSearchTermsString(itemSearchInput, "");
		try {	
	        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
			s = conn.createStatement();
	        String sqlStatement = "SELECT p_name FROM product WHERE " + searchTermsString + " ORDER BY p_name ASC;";
	        r = s.executeQuery(sqlStatement);
	        while(r.next()) {
	        	productLines.add(r.getString(1));
	        }
		}
		catch(SQLException e) {
	    	JOptionPane.showMessageDialog(program, "There is a problem loading the products from the database.\n" + e.toString(), "Could not load products from DB", JOptionPane.WARNING_MESSAGE);
		}
		finally {
			try { s.close(); } catch (Exception e) { /* ignored */ }
			try { r.close(); } catch (Exception e) { /* ignored */ }
			try { conn.close(); } catch (Exception e) { /* ignored */ }
		}
		
		// get the kit items for all products
		for(int i = 0; i < productLines.size(); i++) {
			try {	
		        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
				s = conn.createStatement();
		        String sqlStatement = "SELECT p2.p_name from ((product p1 LEFT JOIN kitItem ON p1.p_id = kit_product) LEFT JOIN product p2 ON p2.p_id = kitItem_product) WHERE p1.p_name = '" + productLines.get(i) + "';";
		        r = s.executeQuery(sqlStatement);
		        while(r.next()) {
		        	if(r.getString(1) != null) {
		        		if(!productLines.contains(r.getString(1))) {
		        			productLines.add(r.getString(1));
		        		}
		        	}
		        }
			}
			catch(SQLException e) {
		    	JOptionPane.showMessageDialog(program, "There is a problem loading the products from the database.\n" + e.toString(), "Could not load products from DB", JOptionPane.WARNING_MESSAGE);
			}
			finally {
				try { s.close(); } catch (Exception e) { /* ignored */ }
				try { r.close(); } catch (Exception e) { /* ignored */ }
				try { conn.close(); } catch (Exception e) { /* ignored */ }
			}
		}
		
		String[] products = new String[productLines.size()];
		products =	productLines.toArray(products);
		productList = new JList<String>(products);
		productList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		productList.setLayoutOrientation(JList.VERTICAL);
		productList.setVisibleRowCount(-1);
		productList.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		productList.addMouseListener(this);
		
		productScrollPane = new JScrollPane(productList);
		productScrollPane.setViewportView(productList);
		productScrollPane.setPreferredSize(new Dimension(screenSize.width/2, screenSize.height/2));
	    productScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    productScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		
		productPanel = new JPanel();
		productPanel.setFont(new Font("Arial", Font.BOLD, screenSize.width/69));
		productPanel.add(productScrollPane);
		
		int result;
		boolean validProduct = false, cancel = false;
		if(productList.getModel().getSize() != 0) {
			while(!validProduct && !cancel) {
				if(productList.getModel().getSize() == 1) {
					productList.setSelectedIndex(0);
					productName = productList.getSelectedValue();
					validProduct = true;
					try {
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
						s = conn.createStatement();
				        String sqlStatement = "SELECT p_id, p_name, instock, restock, kit FROM product WHERE p_name = '" + productName + "' ORDER BY p_name ASC;";
				        r = s.executeQuery(sqlStatement);
				        while(r.next()) {
				        	barcodeItem[0] = r.getString(1);
				        	barcodeItem[1] = r.getString(2);
				        	barcodeItem[2] = r.getString(3);
				        	barcodeItem[3] = r.getString(4);
				        	barcodeItem[4] = r.getString(5);
				        }
					}
					catch(SQLException e) {
				    	JOptionPane.showMessageDialog(program, "There is a problem finding the item in the database.\n" + e.toString(), "Could not find item in DB", JOptionPane.WARNING_MESSAGE);
					}
					finally {
						try { s.close(); } catch (Exception e) { /* ignored */ }
						try { r.close(); } catch (Exception e) { /* ignored */ }
						try { conn.close(); } catch (Exception e) { /* ignored */ }
					}
				}
				else {
					result = JOptionPane.showConfirmDialog(program, productPanel, 
							"Please choose your product:", JOptionPane.OK_CANCEL_OPTION);
					if(result == JOptionPane.OK_OPTION) {
						productName = productList.getSelectedValue();
						if(productName != null) {
							validProduct = true;
							try {
						        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
								s = conn.createStatement();
						        String sqlStatement = "SELECT p_id, p_name, instock, restock, kit FROM product WHERE p_name = '" + productName + "' ORDER BY p_name ASC;";
						        r = s.executeQuery(sqlStatement);
						        while(r.next()) {
						        	barcodeItem[0] = r.getString(1);
						        	barcodeItem[1] = r.getString(2);
						        	barcodeItem[2] = r.getString(3);
						        	barcodeItem[3] = r.getString(4);
						        	barcodeItem[4] = r.getString(5);
						        }
							}
							catch(SQLException e) {
						    	JOptionPane.showMessageDialog(program, "There is a problem finding the item in the database.\n" + e.toString(), "Could not find item in DB", JOptionPane.WARNING_MESSAGE);
							}
							finally {
								try { s.close(); } catch (Exception e) { /* ignored */ }
								try { r.close(); } catch (Exception e) { /* ignored */ }
								try { conn.close(); } catch (Exception e) { /* ignored */ }
							}
						}
					}
					else {
						cancel = true;
					}
				}
			}
		}
	}
	
	Action barcodeScanned = new AbstractAction(){
		
		/**
		 * This method takes care of what happens when you scan a barcode
		 */
		
	    @Override
	    public void actionPerformed(ActionEvent e){
	    	barcodeInput = "";
			barcodeItem = new String[5];
	        barcodeInput = barcodeTextField.getText().trim();
	        barcodeTextField.setText("");
	        barcodeItem = findItem(barcodeInput);
			if(!barcodeInput.trim().isEmpty() && barcodeItem[0] == null) {
				searchKeywords();
				barcodeTextField.requestFocusInWindow();
			}
			if(barcodeItem[0] != null){
				if(barcodeItem[4].equals("1")) {
					boolean notEnoughItems = false;
					try {
				        conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
						s = conn.createStatement();
						String sqlStatement = "SELECT quantity, kitItem_product, p_name, instock FROM (kitItem LEFT JOIN product ON kitItem_product = p_id) WHERE kit_product = '" + barcodeItem[0] + "';";
				        r = s.executeQuery(sqlStatement);
				        while(r.next()) {
				        	namesList.put(r.getString(2), r.getString(3));
							itemsStack.push(r.getString(2) + "," + (Integer.parseInt(scanQuantity.getValue().toString()) * Integer.parseInt(r.getString(1))));
							undoButton.setVisible(true);
							if(quantityList.containsKey(r.getString(2))) {
								quantityList.replace(r.getString(2), quantityList.get(r.getString(2)) + ((int)scanQuantity.getValue() * Integer.parseInt(r.getString(1))));
							}
							else {
								quantityList.put(r.getString(2), ((int)scanQuantity.getValue() * Integer.parseInt(r.getString(1))));
								itemsOrder.add(r.getString(2));
							}
							if(titleLabel.getText().equals("Check Out(-)") && quantityList.get(r.getString(2)) > Integer.parseInt(r.getString(4))) {
								notEnoughItems = true;
							}
				        }
					}
					catch(SQLException sqle) {
						
				    	JOptionPane.showMessageDialog(program, "There is a problem finding the kit in the database.\n" + sqle.toString(), "Could not find kit in DB", JOptionPane.WARNING_MESSAGE);
					}
					finally {
						try { s.close(); } catch (Exception e1) { /* ignored */ }
						try { r.close(); } catch (Exception e2) { /* ignored */ }
						try { conn.close(); } catch (Exception e3) { /* ignored */ }
					}
					if(notEnoughItems) {
		        		JOptionPane.showMessageDialog(program, "Could not check out this kit because there is no more stock for an item.", "Exceed stock quantity", JOptionPane.WARNING_MESSAGE);
						try {
							conn = DriverManager.getConnection("jdbc:sqlserver://DESKTOP-QHQ11UT;database=Inventory;user=InventoryUser;password=InventoryPassword;");
							s = conn.createStatement();
							String sqlStatement = "SELECT quantity, kitItem_product, p_name, instock FROM (kitItem LEFT JOIN product ON kitItem_product = p_id) WHERE kit_product = '" + barcodeItem[0] + "';";
					        r = s.executeQuery(sqlStatement);
				        	while(r.next()) {
								quantityList.replace(r.getString(2), quantityList.get(r.getString(2)) - ((int)scanQuantity.getValue() * Integer.parseInt(r.getString(1))));
								itemsStack.pop();
								if(quantityList.get(r.getString(2)) < 1) {
									quantityList.remove(r.getString(2));
									namesList.remove(r.getString(2));
									itemsOrder.remove(r.getString(2));
								}
					        }
						}
						catch(SQLException sqle) {
							
					    	JOptionPane.showMessageDialog(program, "There is a problem finding the kit in the database.\n" + sqle.toString(), "Could not find kit in DB", JOptionPane.WARNING_MESSAGE);
						}
						finally {
							try { s.close(); } catch (Exception e1) { /* ignored */ }
							try { r.close(); } catch (Exception e2) { /* ignored */ }
							try { conn.close(); } catch (Exception e3) { /* ignored */ }
						}
						itemsTextArea.setText(updateItems());
						if(itemsStack.isEmpty()) {
							undoButton.setVisible(false);
						}
			        }
					
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
	Action itemSearched = new AbstractAction(){
		/**
		 * This method takes care of what happens when you search an item in the database
		 */
	    @Override
	    public void actionPerformed(ActionEvent ae){
	    	refreshItemTable();
	    }
	};
	@Override
	public void mouseEntered(MouseEvent e) {
		
	}
	@Override
	public void mouseExited(MouseEvent e) {
		
	}
	@Override
	public void mouseClicked(MouseEvent e) {
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		
	}
}
