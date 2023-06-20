package server;
import java.io.*;
import java.net.*;

public class Server {

	public static void main(String[] args) throws IOException{
		// TODO Auto-generated method stub
		
		boolean run = true;
		
		DatagramSocket socket = new DatagramSocket(17);
		System.out.println("Server started");
		
		while(run){
			try {
				
				byte[] buffer = new byte[256];
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				socket.receive(request);
				System.out.println("Recieved connection at " + request.getAddress());
				
				//
				
			}catch(Exception e){
				System.out.println(e.getMessage());
			}
		
		}
		
		
		
	}
	
	

}

class Handler implements Runnable {
	
	Handler(){
		
	}
	
	public void run() {
		
	}
}