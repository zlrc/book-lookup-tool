import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import java.net.MalformedURLException;
import java.net.URL;

public class BLT extends JFrame {

    // Declare fields for GUI components here
		private JPanel top, bottom, inputPane;
		private JLabel title, instructions;
		private JTextField isbnInputField;
		private List isbnList;
		private JButton enterButton, submitButton;
		JDialog loadPopUp, results;

		private String[] bookIsbn = new String[20];
		private int nextIndex = 0;

    public BLT() {
        super("Book Lookup Tool");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Size this JFrame so that it is just big enough to hold the components.
        this.setSize(450,600);

				// Position JFrame onto the center of the monitor
				Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
				this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);

        // Declare and instantiate panels
        top = new JPanel();
				bottom = new JPanel();
				inputPane = new JPanel();
				top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
				top.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS) );
				bottom.setBorder(BorderFactory.createEmptyBorder(0, 50, 10, 50));

        // Instantiate top components
				title = new JLabel("Book Lookup Tool");
				instructions = new JLabel("Enter an ISBN:");
				isbnInputField = new JTextField(20);
				enterButton = new JButton("\u2192"); // unicode for right-facing arrow
				title.setFont(new Font("Verdana", Font.BOLD, 30));
				isbnInputField.setFont(new Font("Courier", Font.PLAIN, 19));
				title.setAlignmentX(Component.CENTER_ALIGNMENT);
				instructions.setAlignmentX(Component.CENTER_ALIGNMENT);

				// Instantiate bottom components
				isbnList = new List(10, false);
				submitButton = new JButton("Submit");
				submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
				isbnList.setFont(new Font("Courier New", Font.PLAIN, 15));
				isbnList.setEnabled(false);

				// Instantiate Dialog Window
				loadPopUp = new JDialog(this);
				loadPopUp.setTitle("Please Wait");
				loadPopUp.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				loadPopUp.addWindowListener( new closeDialogListener() );
				loadPopUp.setSize(450,100);
				loadPopUp.setResizable(false);
				loadPopUp.setLocationRelativeTo(this);

				// Dialog Components
				JPanel mainPanel = new JPanel();
				JLabel loadLabel = new JLabel("Getting Book Data...");
				mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

				mainPanel.add(loadLabel);
				loadPopUp.add(mainPanel);
				loadPopUp.setVisible(false);

        // Add components to top panel and input pane
				top.add(title);
				top.add(Box.createRigidArea(new Dimension(0,10))); // empty space
				top.add(instructions);
				top.add(inputPane);
				inputPane.add(isbnInputField);
				inputPane.add(enterButton);

				// Add components to bottom panel
				bottom.add(isbnList);
				bottom.add(Box.createRigidArea(new Dimension(0,10))); // empty space
				bottom.add(submitButton);

				// Action Listener(s)
				enterButton.addActionListener( new enterButtonListener() );
				submitButton.addActionListener( new submitButtonListener() );

        // Add the main panel to this JFrame
        this.add(top, BorderLayout.NORTH);
				this.add(bottom, BorderLayout.CENTER);

        // Make this JFrame visible on the screen
        this.setVisible(true);
				this.setResizable(false);
    }



		/**
		 * Class to store information on each book we're searching for
		**/
		private static class Book {
			private String name, author, isbn10, cover;
			private double amazonPrice;
			public static double totalCost = 0;

			// Constructor(s)
			public Book (String n, String a, String i, String c, double ap)
			{
				name = n;
				author = a;
				isbn10 = i;
				cover = c;
				amazonPrice = ap;
				totalCost += amazonPrice;
				System.out.println("New Book: " + name + " by " + author + " ($" + amazonPrice + ")" );
			}

			// Accessors
			public String getTitle(){ return truncateString(name,30); }
			public String getAuthor(){ return truncateString(author,42); }
			public String getISBN(){ return isbn10; }
			public String getCoverUrl(){ return cover; }
			public double getAmazonPrice(){ return amazonPrice; }

			/**
			 * Cuts a string down to a specified length if it exceeds it
			 * @param s the string we're looking to truncate
			 * @param limit the truncated length of the string
			**/
			private String truncateString(String s, int limit)
			{
				if (s.length() > limit)
				{
					String truncString = "";
					for (int i = 0; i < limit-3; i++)
						truncString += s.charAt(i);

					truncString += "...";
					return truncString;
				}
				else
					return s;
			}

		} // end of Book class



   /**
    *  Listener for '->' button, adds isbn to list.
   **/
    private class enterButtonListener implements ActionListener {
      public void actionPerformed(ActionEvent e)
			{

				// Parse input string to only include numbers
				String input = isbnInputField.getText();
				String isbn = "";
				for (int i = 0; i < input.length(); i++)
				{
					if ( Character.isDigit(input.charAt(i)) )
						isbn += input.charAt(i);
				}

				// Add isbn to list and array
				bookIsbn[nextIndex] = isbn;
				isbnList.add(isbn);
				nextIndex++;

				if (nextIndex >= bookIsbn.length) // disable presses once full
					enterButton.setEnabled(false);

				// Clear input field
				isbnInputField.setText("");

      }
    }

		/**
     *  Listener for "Submit" button, grabs data for each book.
    **/
     private class submitButtonListener implements ActionListener {
       public void actionPerformed(ActionEvent e)
			 {

				// Run python script on a new thread
				Thread t = new Thread(new Runnable() {
					public void run()
					{

						try {
							// Execute BookLookup.py
							TimeUnit.SECONDS.sleep(1);
							String execInput = "python3 BookLookup.py ";
							for (int i = 0; i < nextIndex; i++)
								execInput += bookIsbn[i] + " "; // appends isbn arguments to the command
							Process p = Runtime.getRuntime().exec(execInput); // run the command
							BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream())); // reads output

							// Read data from output string to object array
							String line;
							int booksLength = 0;
							Book[] books = new Book[20];
							try {

								while ( (line = in.readLine()) != null)
								{
									books[booksLength] = new Book(line, in.readLine(), in.readLine(), in.readLine(), Double.parseDouble(in.readLine()) );
									in.readLine(); // skip the store page url for now
									booksLength++;
								}

							}
							catch (NullPointerException pointerError) {
								// Ignore the exception
							}

							in.close();
							System.out.println("Total Cost: $" + Book.totalCost + "\n[Finished]");

							results = generateResultsDialog(books, booksLength);
							//results.addWindowListener( new closeDialogListener() ); // exit program upon closing this dialog
							results.setVisible(true); // show results
						}
						catch (Exception error) {
							System.out.println("Error retrieving data:\n" + error);
						}
						finally {
							loadPopUp.setVisible(false);
						}

					} // end of run()
				}); // end of thread object creation
				t.start(); // start the thread

				// Show loading dialog while it runs
				BLT.this.setEnabled(false);
		    loadPopUp.setVisible(true);

			} // end of actionPerformed method

		} // end of listener class

		 /**
      *  Listener for "New Search" button.
     **/
		 private class newSearchButtonListener implements ActionListener {
			 public void actionPerformed(ActionEvent e)
			 {
				 BLT.this.setEnabled(true);
				 isbnInputField.setText(""); // clear input field
				 isbnList.removeAll();
				 Book.totalCost = 0;
				 nextIndex = 0;
				 results.dispose(); // get rid of the dialog window
			 }
		 }

		 /**
		  * Listener for when the user closes a dialog box
		 **/
		 private class closeDialogListener extends WindowAdapter {
			 public void windowClosed(WindowEvent e)
			 {
				 System.exit(0);
			 }
		 }



		 /**
		  *	Creates a JDialog window that displays info on books that the user
			* provided ISBNs for.
			* @param b Array of objects that belong to the Book class
			* @param bLength The number of objects inside the Book array
			* @return Object reference for the results dialog (JDialog)
		 **/
		 private JDialog generateResultsDialog(Book[] b, int bLength)
		 {
			 // Instantiate Dialog Window "r"
			 JDialog r = new JDialog(BLT.this);
			 r.setTitle("Book Lookup Tool");
			 r.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			 r.setSize(450,300);
			 r.setResizable(false);
			 r.setLocationRelativeTo(BLT.this);

			 // Dialog Components
			 JPanel wrapper = new JPanel();
			 wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
			 wrapper.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
			 JPanel[] bookInfo = new JPanel[bLength];
			 JScrollPane scroller = new JScrollPane(wrapper);
			 scroller.getVerticalScrollBar().setUnitIncrement(12);

			 JLabel totalCostLabel = new JLabel( String.format("Total Cost: $%.2f",Book.totalCost) );
			 JButton newSearchButton = new JButton("New Search");

			 // Create a new panel for each book
			 for (int i = 0; i < bLength; i++)
			 {
				 bookInfo[i] = new JPanel();
				 bookInfo[i].setLayout(new BoxLayout(bookInfo[i], BoxLayout.X_AXIS));
 				 bookInfo[i].setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
				 bookInfo[i].setAlignmentX(Component.LEFT_ALIGNMENT);

				 // Create wrapper for text components
				 JPanel bookInfoText = new JPanel();
				 bookInfoText.setLayout(new BoxLayout(bookInfoText, BoxLayout.Y_AXIS));
				 bookInfoText.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
				 bookInfoText.setAlignmentY(Component.TOP_ALIGNMENT);

				 // Title and Author
				 JLabel titleLabel = new JLabel( b[i].getTitle() );
				 JLabel authorLabel = new JLabel( b[i].getAuthor() );
				 titleLabel.setFont(new Font("Verdana", Font.BOLD, 14));
				 authorLabel.setFont(new Font("Verdana", Font.ITALIC, 11));

				 // Get listed price from Amazon
				 String formattedAmazonPrice = String.format("Amazon: $%.2f",b[i].getAmazonPrice());
				 JLabel amazonPriceLabel = new JLabel(formattedAmazonPrice);

				 bookInfoText.add(titleLabel);
				 bookInfoText.add(authorLabel);
				 bookInfoText.add(amazonPriceLabel);

				 // Get cover image
				 Image coverImg = null;
				 if ( !b[i].getCoverUrl().equals("No Image") )
				 {
					 URL url = null;

					 try {
						 url = new URL( b[i].getCoverUrl() );
						 coverImg = ImageIO.read(url);
					 }
					 catch (MalformedURLException ex) {
						 System.out.println("Error: Malformed Cover URL for ISBN: " + b[i].getISBN());
					 }
					 catch (IOException iox) {
						 System.out.println("Error: Can not load cover image file for ISBN: " + b[i].getISBN());
					 } // end of try-catch block

				 }
				 else // assigns the placeholder image if the book has none
				 {
					 try { coverImg = ImageIO.read( new File("blank_cover.png") ); }
					 catch (IOException iox) { System.out.println("Error retrieving placeholder cover image."); }
				 } // end of book cover if-statement
				 JLabel img = new JLabel( new ImageIcon(coverImg) );
				 img.setAlignmentY(Component.TOP_ALIGNMENT);

				 // Add everything to the book's info panel
				 bookInfo[i].add(img);
				 bookInfo[i].add(bookInfoText);

				 // Add to wrapper
				 wrapper.add(bookInfo[i]);
				 wrapper.add(new JSeparator(SwingConstants.HORIZONTAL));
			} // end of for loop

			 // Add components to the JDialog
			 wrapper.add(totalCostLabel);
			 wrapper.add(Box.createRigidArea(new Dimension(0,10))); // empty space
			 wrapper.add(newSearchButton);
			 r.add(scroller);

			 newSearchButton.addActionListener( new newSearchButtonListener() );

			 // Return the object reference
			 return r;
		 }



		/**
		 * Main class
		**/
    public static void main( String[] args ) throws IOException
    {
        BLT frame = new BLT();
    }
}
