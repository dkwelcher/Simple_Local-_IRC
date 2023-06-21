package server;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server {

	public static void main(String[] args) throws IOException{
		// TODO Auto-generated method stub
		
		boolean run = true;
		int PORT = 194;
		
		DatagramSocket socket = new DatagramSocket(PORT);
		DataManager serverData = new DataManager();
		
		System.out.println("Server started");
		
		
		while(run){
			try {
				System.out.println("in loop");
				byte[] buffer = new byte[256];
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				socket.receive(request);
				System.out.println("recieved");
				System.out.println("Recieved connection at " + request.getAddress());
				serverData.addUser(request);
				String text = new String(request.getData(), 0, request.getLength());
				System.out.println(text);
				new ClientHandler(request, socket, serverData);
				//
				
			}catch(Exception e){
				System.out.println(e.getMessage());
			}
		
		}
		
	}
	
	

}

class DataManager{
	
	ArrayList<String> global;
	ArrayList<String[]> users;
	
	DataManager(){
		global = new ArrayList<String>();
		users = new ArrayList<String[]>();
	}
	
	public void addToTextLog(String text) {
		global.add(text);
	}
	
	public void addUser(DatagramPacket request) {
		//String username = new String(request.getData());
		//String ip = new String(request.getAddress().toString());
		String[] userip = {new String(request.getData()).split(" ")[0], 
				new String(request.getAddress().toString()), Integer.toString(request.getPort())};
		if(!users.contains(userip)) {
			users.add(userip);
			addToTextLog(userip[0] + "joined the room\n");
		}
	}
	public ArrayList<String[]> getUsers(){
		return users;
	}
	
}

class ClientHandler implements Runnable {
	
	DatagramSocket socket;
	DatagramPacket packet;
	boolean run;
	DataManager manager;
	
	DatagramSocket clientsocket;
	DatagramPacket clientpacket;
	
	ClientHandler(DatagramPacket pkt, DatagramSocket sock, DataManager data){
		this.packet = pkt;
		this.socket = sock;
		this.run = true;
		this.manager = data;
		new Thread(this).start();
		System.out.println("created thread");
		
		
		
	}
	
	public void run() {
		try {
			while(run) {
				byte[] buffer = new byte[256];
				this.packet = new DatagramPacket(buffer, buffer.length);
				this.socket.receive(this.packet);
				String text = new String(this.packet.getData(), 0, this.packet.getLength());
				System.out.println("recieved: " + text);
				try {
					this.manager.addToTextLog(text.split(" ")[1]);
				}catch(Exception e) {
					e.printStackTrace();
					this.manager.addToTextLog(text);
				}
				this.broadcast(text);
				
				
			}
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		
	}//message, address, port
	public void broadcast(String message) {
		for(String[] entry : this.manager.getUsers()){
			byte[] buffer = message.getBytes();
			try {
				DatagramPacket toclient = new DatagramPacket(buffer, buffer.length, 
						InetAddress.getByName(entry[1]), Integer.valueOf(entry[2]));
				this.socket.send(toclient);
			}catch(Exception e){
				System.out.println(e.getMessage());
			}
		}
	}
	
	
	
}


