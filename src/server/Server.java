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
				
				byte[] buffer = new byte[256];
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				socket.receive(request);
				
				System.out.println("Recieved connection at " + request.getAddress());
				
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
	ArrayList<String> users;
	ArrayList<InetAddress> ips;
	ArrayList<Integer> ports;
	//make list of clients
	
	DataManager(){
		global = new ArrayList<String>();
		users = new ArrayList<String>();
		ips = new ArrayList<InetAddress>();
		ports = new ArrayList<Integer>();
	}
	
	public void addToTextLog(String text) {
		global.add(text);
		
	}
	
	public boolean attemptaddUser(DatagramPacket request) {
		
		if(!ips.contains(request.getAddress())) {
			ips.add(request.getAddress());
			ports.add(request.getPort());
			users.add((new String(request.getData(), 0, request.getLength()).split(" ")[0]));
			addToTextLog(request.getAddress().toString() + "joined the room\n");
			
			return true;
		}else {
			return false;
		}
		
	}
	public ArrayList<String> getUsers(){
		return users;
	}
	public ArrayList<InetAddress> getips(){
		return ips;
	}
	public ArrayList<String> getChatLog(){
		return global;
	}
	public ArrayList<Integer> getports(){
		return ports;
	}
	
	
}

class ClientHandler implements Runnable {
	
	DatagramSocket socket;
	DatagramPacket packet;
	
	DataManager manager;
	
	DatagramSocket clientsocket;
	DatagramPacket clientpacket;
	
	ClientHandler(DatagramPacket pkt, DatagramSocket sock, DataManager data){
		this.packet = pkt;
		this.socket = sock;
		
		this.manager = data;
		
		
		
		
		
		new Thread(this).start();
		System.out.println("created thread");
		
		
	}
	
	public void run() {
		try {
			
			if(this.manager.attemptaddUser(this.packet)) {
				this.sendHistory(this.packet);
			}
			
			String text = new String(this.packet.getData(), 0, this.packet.getLength());
			try {
				this.manager.addToTextLog(text);
			}catch(Exception e) {
				
				this.manager.addToTextLog(text);
			}
			
			
			
			if(text.contains("USER_REQUEST")) {
				System.out.println("user data sent");
				this.sendUserInfo(this.packet);
			}else if(text.contains("GIVE_HISTORY")){
				
				this.sendHistory(this.packet);
			}else{
				System.out.println("chat history sent");
				this.broadcast(this.packet, this.manager);
			}
				
			
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		
	}//message, address, port
	public void broadcast(DatagramPacket packet, DataManager data) throws UnknownHostException {
		int i = 0;
		System.out.println("broadcasting...");
		for(InetAddress entry : this.manager.getips()){
			
			
			
			try {
				DatagramPacket toclient = new DatagramPacket(packet.getData(), packet.getLength(), 
						entry, manager.ports.get(i));
				this.socket.send(toclient);
				System.out.println("broadcast sent to " + entry + "/"+ manager.ports.get(i));
			}catch(Exception e){
				System.out.println(e.getMessage());
				System.out.println("failed broadcast to" + entry);
			}
			
			i++;
		}
	}
	
	public void sendHistory(DatagramPacket packet) {
		
		for(String oldmessage : this.manager.getChatLog()) {
			try {
				byte[] data = oldmessage.getBytes();
				DatagramPacket toclient = new DatagramPacket(data, data.length, 
				packet.getAddress(), packet.getPort());
				this.socket.send(toclient);
				System.out.println("sending chat history message" + oldmessage);
			}catch(Exception e){
				System.out.println(e.getMessage());
				System.out.println("failed to send history message to" + packet.getAddress());
			}
		}
		
	}
	
	public void sendUserInfo(DatagramPacket packet) {
		ArrayList<String> userinfo = new ArrayList<String>();
		int i = 0;
		
		for(String user : this.manager.getUsers()) {
			String info = "DATA: " + user + " " + this.manager.getips().get(i).toString() + " " + this.manager.getports().get(i);
			userinfo.add(info);
			i++;
		}
		
		for(String user : userinfo) {
			try {
				byte[] data = user.getBytes();
				DatagramPacket toclient = new DatagramPacket(data, data.length, 
										packet.getAddress(), packet.getPort());
				this.socket.send(toclient);
				System.out.println("sending user from list: " + user);
			}catch(Exception e){
				System.out.println(e.getMessage());
				System.out.println("failed to send username to " + packet.getAddress());
			}
		}
	}
}


