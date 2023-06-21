package client;

import java.net.*;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class ChatClient {
	private static final int PORT = 55555;
	private static final String ADDRESS = "localhost";
	private DatagramSocket socket;
	private InetAddress serverAddress;
	private int serverPort;
	private Thread listenerThread;
	private JFrame chatFrame;
	private JTextArea chatTextArea;
	private JTextField messageField;
	private JButton sendButton;
	private JButton pmButton;
	private JButton closeButton;
	
	public ChatClient(InetAddress serverAddress, int serverPort) {
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		
		initializeGUI();
		initializeChatClient();
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				InetAddress address;
				try {
					address = InetAddress.getByName(ADDRESS);
					new ChatClient(address, PORT);
				}
				catch (UnknownHostException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		});
	}
	
	private void initializeGUI() {
		Dimension frameSize = getFrameDimension();
		
		chatFrame = new JFrame("Local Chatroom");
		chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		chatFrame.setSize(frameSize);
		//chatFrame.setMinimumSize(new Dimension(?, ?)); // for smaller screens, this may be necessary
		chatFrame.setResizable(false);
		chatFrame.setLayout(new BorderLayout());
		
		JPanel northPanel = createNorthPanel();
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
	
	private JPanel createNorthPanel() {
		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.setBackground(BannerConstants.BANNER_COLOR);
		northPanel.setPreferredSize(new Dimension(25, 75));
		
		JLabel titleLabel = new JLabel("Local Chatroom");
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
		pmButton = new JButton("PM");
		pmButton.addActionListener(buttonListener);
		closeButton = new JButton("Close");
		closeButton.addActionListener(buttonListener);
		
		buttonPanel.add(sendButton);
		buttonPanel.add(pmButton);
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
			listenerThread.interrupt();
			socket.close();
		}));
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
					
					chatTextArea.append(message + "\n");
				}
				catch(IOException e) {
						break;
				}
			}
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
			else if(button == closeButton)
				executeCloseButton();
		}
		
		private void executeSendButton() {
			String message = messageField.getText().trim();
			
			if(!message.isEmpty()) {
				messageField.setText("");
				byte[] buffer = message.getBytes();
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
			
		}
		
		private void executeCloseButton() {
			listenerThread.interrupt();
			socket.close();
			chatFrame.dispose();
		}
	}
}