package client;

import java.net.*;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.swing.*;
import javax.swing.text.DefaultCaret;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class ChatClient {
	private static final int PORT = 194;
	private static final String ADDRESS = "127.0.0.1";
	private DatagramSocket socket;
	private InetAddress serverAddress;
	private String username;
	private int serverPort;
	private Thread listenerThread;
	private JFrame chatFrame;
	private JTextArea chatTextArea;
	private JTextField messageField;
	private JButton sendButton;
	private JButton pmButton;
	private JButton listButton;
	private JButton closeButton;
	private JFrame clientListFrame;
	private JTextArea clientListTextArea;
	private Timer timer;
	private boolean receivedClientList = false;
	
	public ChatClient(InetAddress serverAddress, int serverPort, String username) {
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.username = username;
		
		initializeGUI();
		initializeChatClient();
		initializeClientListTimer();
		initializeServerCommunication();
		
		messageField.requestFocusInWindow();
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				InetAddress address;
				try {
					String username = getUsername();
					address = InetAddress.getByName(ADDRESS);
					new ChatClient(address, PORT, username);
				}
				catch (UnknownHostException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		});
	}
	
	private static String getUsername() {
		String username = null;
		while(username == null || username.trim().isEmpty()) {
			username = JOptionPane.showInputDialog(null, "Enter your username:");
			if(username == null)
				System.exit(0);
			else if(username.trim().isEmpty())
				JOptionPane.showMessageDialog(null, "Please enter a username that isn't blank.", "Username Error", JOptionPane.ERROR_MESSAGE);
		}
		return username.trim();
	}
	
	private void initializeGUI() {
		Dimension frameSize = getFrameDimension();
		
		chatFrame = new JFrame("Local Chatroom");
		chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		chatFrame.setSize(frameSize);
		chatFrame.setMinimumSize(new Dimension(1000, 1000));
		chatFrame.setResizable(false);
		chatFrame.setLayout(new BorderLayout());
		
		JPanel northPanel = createNorthPanel("Local Chatroom", BannerConstants.MAIN_CHATROOM_DIMENSION);
		JPanel centerPanel = createCenterPanel();
		centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		JPanel southPanel = createSouthPanel();
		
		chatFrame.add(northPanel, BorderLayout.NORTH);
		chatFrame.add(centerPanel, BorderLayout.CENTER);
		chatFrame.add(southPanel, BorderLayout.SOUTH);
		
		chatFrame.setLocationRelativeTo(null);
		chatFrame.setVisible(true);
	}
	
	private Dimension getFrameDimension() {
		Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
		int frameWidth = (int) (screenDimension.width * 0.4);
		int frameHeight = (int) (screenDimension.height * 0.7);
		return new Dimension(frameWidth, frameHeight);
	}
	
	private JPanel createNorthPanel(String title, Dimension dimension) {
		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.setBackground(BannerConstants.BANNER_COLOR);
		northPanel.setPreferredSize(dimension);
		
		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(BannerConstants.TITLE_FONT);
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setHorizontalAlignment(JLabel.CENTER);
		
		JLabel disclaimerLabel = new JLabel("THIS SOFTWARE IS INTENDED FOR EDUCATIONAL USE");
		disclaimerLabel.setFont(BannerConstants.DISCLAIMER_FONT);
		disclaimerLabel.setForeground(Color.WHITE);
		disclaimerLabel.setHorizontalAlignment(JLabel.CENTER);
		
		northPanel.add(titleLabel, BorderLayout.CENTER);
		northPanel.add(disclaimerLabel, BorderLayout.SOUTH);
		return northPanel;
	}
	
	private JPanel createCenterPanel() {
		JPanel centerPanel = new JPanel(new BorderLayout());
		chatTextArea = new JTextArea();
		chatTextArea.setLineWrap(true);
		chatTextArea.setWrapStyleWord(true);
		chatTextArea.setEditable(false);
		chatTextArea.setFont(new Font("Verdana", Font.PLAIN, 12));
		chatTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		DefaultCaret caret = (DefaultCaret)chatTextArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		JScrollPane scrollPane = new JScrollPane(chatTextArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		centerPanel.add(scrollPane, BorderLayout.CENTER);
		return centerPanel;
	}
	
	private JPanel createSouthPanel() {
		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		
		JPanel messagePanel = new JPanel(new FlowLayout());
		JLabel messageLabel = new JLabel("Message:   ");
		messageField = new JTextField();
		messageField.setPreferredSize(new Dimension(500, 30));
		messagePanel.add(messageLabel);
		messagePanel.add(messageField);
		
		JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 5, 5));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		ButtonListener buttonListener = new ButtonListener();
		
		sendButton = new JButton("Send");
		sendButton.addActionListener(buttonListener);
		chatFrame.getRootPane().setDefaultButton(sendButton);
		pmButton = new JButton("PM");
		pmButton.addActionListener(buttonListener);
		listButton = new JButton("List");
		listButton.addActionListener(buttonListener);
		closeButton = new JButton("Close");
		closeButton.addActionListener(buttonListener);
		
		buttonPanel.add(sendButton);
		buttonPanel.add(pmButton);
		buttonPanel.add(listButton);
		buttonPanel.add(closeButton);
		
		southPanel.add(messagePanel, BorderLayout.WEST);
		southPanel.add(buttonPanel, BorderLayout.CENTER);
		
		return southPanel;
	}
	
	private void initializeChatClient() {
		try {
			socket = new DatagramSocket();
			listenerThread = new Thread(new ListenerRunnable(), "ListenerThread");
			listenerThread.start();
		}
		catch (SocketException e) {
			System.out.println("Error occurred while attempting to initialize datagram socket.");
			e.printStackTrace();
			System.exit(1);
		}
		
		// handles edge cases where an unexpected shutdown occurs
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if(!socket.isClosed()) {
				sendExitMessageToServer("CLIENTEXIT");
				listenerThread.interrupt();
				socket.close();
			}
		}));
	}
	
	// delays displaying client list to allow server to send all userInfo packets
	private void initializeClientListTimer() {
		timer = new Timer(500, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(receivedClientList) {
					clientListFrame.setVisible(true);
					receivedClientList = false;
				}
			}
		});
		timer.setRepeats(false);
	}
	
	private void initializeServerCommunication() {
		String message = "[CONNECTED]";
		
		String timeStamp = getTimeStamp();
		String formattedMessage = username + " (" + timeStamp + "): " + message;
			
		byte[] buffer = formattedMessage.getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
			
		try {
			socket.send(packet);
			}
		catch(IOException e) {
			System.out.println("Error occurred while attempting to send packet.");
			e.printStackTrace();
		}
		
	}
	
	private class ListenerRunnable implements Runnable {
		@Override
		public void run() {
			byte[] buffer = new byte[1024];
			
			while(!Thread.currentThread().isInterrupted()) {
				DatagramPacket packet= new DatagramPacket(buffer, buffer.length);
				
				try {
					socket.receive(packet);
					
					String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
					
					if(message.startsWith("DATA: ")) {
						displayClientList(message);
						timer.restart();
					}
					else
						chatTextArea.append(message + "\n");
				}
				catch(IOException e) {
						break;
				}
			}
		}
	}
	
	private void displayClientList(String clientList) {
		if (clientListFrame == null) {
			clientListFrame = new JFrame("Client List");
			clientListFrame.setSize(new Dimension(400, 300));
			clientListFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			clientListFrame.setLayout(new BorderLayout());

			JPanel northPanel = createNorthPanel("Client List", BannerConstants.CLIENT_LIST_DIMENSION);
			JPanel centerPanel = new JPanel(new BorderLayout());

			clientListTextArea = new JTextArea();
			clientListTextArea.setLineWrap(true);
			clientListTextArea.setWrapStyleWord(true);
			clientListTextArea.setEditable(false);
			clientListTextArea.setFont(new Font("Verdana", Font.PLAIN, 12));
			clientListTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

			JScrollPane scrollPane = new JScrollPane(clientListTextArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

			centerPanel.add(scrollPane, BorderLayout.CENTER);

			clientListFrame.add(northPanel, BorderLayout.NORTH);
			clientListFrame.add(centerPanel, BorderLayout.CENTER);

			clientListFrame.setLocationRelativeTo(null);
			clientListFrame.setVisible(true);
		} 
		
		if(receivedClientList) {
			clientListTextArea.setText("");
			receivedClientList = false;
		}
		
		clientListTextArea.append(clientList.substring(6) + "\n");
	}
	
	private String getTimeStamp() {
		LocalTime time = LocalTime.now(ZoneId.systemDefault());
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
		return time.format(formatter);
	}
	
	private void sendExitMessageToServer(String command) {
		byte[] buffer = command.getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
		try {
			socket.send(packet);
		}
		catch(IOException e) {
			System.out.println("Error occurred while attempting to send exit command.");
			e.printStackTrace();
		}
	}
	
	private class ButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			Object button = e.getSource();
			
			if(button == sendButton)
				executeSendButton();
			else if(button == pmButton)
				executePmButton();
			else if(button == listButton)
				executeListButton();
			else if(button == closeButton)
				executeCloseButton();
		}
		
		private void executeSendButton() {
			String message = messageField.getText().trim();
			
			if(!message.isEmpty()) {
				messageField.setText("");
				
				String timeStamp = getTimeStamp();
				String formattedMessage = username + " (" + timeStamp + "): " + message;
				
				byte[] buffer = formattedMessage.getBytes();
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
				
				try {
					socket.send(packet);
				}
				catch(IOException e) {
					System.out.println("Error occurred while attempting to send packet.");
					e.printStackTrace();
				}
			}
		}
		
		private void executePmButton() {
			String recipientUsername = getRecipientUsername();
			
			if(recipientUsername == null)
				return;
			
			String message = JOptionPane.showInputDialog(null, "Message to " + recipientUsername + ":");
			
			if(message == null)
				return;
			
			if(message.trim().isEmpty()) {
				JOptionPane.showMessageDialog(null, "Please enter a message that isn't blank", "Private Message Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			String formattedMessage = "SENDPM: " + username + " to " + recipientUsername + " " + message;
			
			byte[] buffer = formattedMessage.getBytes();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
			
			try {
				socket.send(packet);
			}
			catch(IOException e) {
				System.out.println("Error occurred while attempting to send private message packet");
				e.printStackTrace();
			}
		}
		
		private String getRecipientUsername() {
			String recipientUsername = null;
			
			while(recipientUsername == null || recipientUsername.trim().isEmpty()) {
				recipientUsername = JOptionPane.showInputDialog(null, "Enter the recipient's username:");
				if(recipientUsername == null)
					return null;
				else if(recipientUsername.trim().isEmpty())
					JOptionPane.showMessageDialog(null, "Please enter a username that isn't blank", "Username Error", JOptionPane.ERROR_MESSAGE);
				
			}
			return recipientUsername.trim();
		}
		
		private void executeListButton() {
			String fetchCommand = "USER_REQUEST";
			byte[] buffer = fetchCommand.getBytes();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
			
			try {
				socket.send(packet);
				receivedClientList = true;
				if(clientListFrame != null)
					clientListFrame.setVisible(true);
			}
			catch(IOException e) {
				System.out.println("Error occurred while attempting to send user request command.");
				e.printStackTrace();
			}
		}
		
		private void executeCloseButton() {
			sendExitMessageToServer("CLIENTEXIT");
			
			listenerThread.interrupt();
			socket.close();
			if(clientListFrame != null)
				clientListFrame.dispose();
			chatFrame.dispose();
		}
	}
}