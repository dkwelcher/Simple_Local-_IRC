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
	ArrayList<InetAddress> ips;
	ArrayList<Integer> ports;
	//make list of clients
	
	DataManager(){
		global = new ArrayList<String>();
		users = new ArrayList<String[]>();
		ips = new ArrayList<InetAddress>();
		ports = new ArrayList<Integer>();
	}
	
	public void addToTextLog(String text) {
		global.add(text);
		
	}
	
	public void addUser(DatagramPacket request) {
		//String username = new String(request.getData());
		//String ip = new String(request.getAddress().toString());
		
		if(!ips.contains(request.getAddress())) {
			ips.add(request.getAddress());
			ports.add(request.getPort());
			addToTextLog(request.getAddress().toString() + "joined the room\n");
		}
	}
	public ArrayList<String[]> getUsers(){
		return users;
	}
	public ArrayList<InetAddress> getips(){
		return ips;
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
			
			
			
			String text = new String(this.packet.getData(), 0, this.packet.getLength());
			try {
				this.manager.addToTextLog(text.split(" ")[1]);
			}catch(Exception e) {
				
				this.manager.addToTextLog(text);
			}
			this.broadcast(this.packet, this.manager);
				
				
			
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		
	}//message, address, port
	public void broadcast(DatagramPacket packet, DataManager data) throws UnknownHostException {
		int i = 0;
		for(InetAddress entry : this.manager.getips()){
			
			System.out.println("before");
			
			try {
				DatagramPacket toclient = new DatagramPacket(packet.getData(), packet.getLength(), 
						entry, manager.ports.get(i));
				this.socket.send(toclient);
				System.out.println("attempted broadcast");
			}catch(Exception e){
				System.out.println(e.getMessage());
				System.out.println("failed broadcast to" + entry);
			}
			System.out.println("attempted broadcast");
			i++;
		}
	}
	
	
	
}


