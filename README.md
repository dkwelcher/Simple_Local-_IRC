# Simple_Local-_IRC
Simple IRC client and server
This project is a simple chat program that works by sending UDP messages over an LAN network. 
The project features a simple GUI that includes a chat box showing previous messages, 
a text box for inputting messages, a button that allows for private messaging between two clients, and a button for displaying the client list. 
Upon joining the chat room, the client will be greeted with a welcome message, and the previous chat history will be displayed. 
By starting a new thread whenever there is a new client connection, the UDP server is able to handle multiple clients at once. 
The server maintains a list of active clients, along with their username and IP address.
