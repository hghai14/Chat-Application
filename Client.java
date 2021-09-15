import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


public class Client{
    static Socket forwardsSocket;
    static Socket sendMessageSocket;

    static class ClientSendThread extends Thread{
        String userName;
        Socket socket;
    
        ClientSendThread(Socket sendMessageSocket, String clientName){
            System.out.println("Constructing send message thread.");
            socket = sendMessageSocket;
            userName = clientName;
        }

        String sendMessage(String input){
            String message = "";
            String username = "";
            String content = "";
            int idx = 0;
            while(input.charAt(idx) != ' '){
                if(input.charAt(idx) == '@'){
                    idx++;
                    continue;
                }
                username += input.charAt(idx);
                idx++;
            }
            content = input.substring(idx+1);
            message = "SEND " + username + "\n" + "Content-length: " + Integer.toString(content.length()) + "\n" + "\n" + content;
            return message;
        }
    
        public void run(){
            // PrintWriter out;
            // BufferedReader stdIn;
            String message = "";
            try {
                PrintWriter out;
                BufferedReader stdIn ;
                while (true) {
                    System.out.print("> ");
                    out = new PrintWriter(socket.getOutputStream(), true);
                    stdIn = new BufferedReader(new InputStreamReader(System.in));
                    message = stdIn.readLine();
                    message = sendMessage(message);
                    out.println(message);
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }
    
        
    }

    static class ClientReceiveThread extends Thread{
        String userName;
        Socket socket;
    
        ClientReceiveThread(Socket forwardsSocket, String clientName){
            System.out.println("Constructing send message thread.");
            socket =  forwardsSocket;
            userName = clientName;
        }
    
        public void run(){
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line;
                    while(true) {
                        line = in.readLine();
                        if(line != null && line.length() != 0 && !line.equals(" ")){
                            System.out.println("< " + line);
                        }
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                };
        }
    
        
    }

    public Client(InetAddress host, int port, String userName) {
        try {
            sendMessageSocket = new Socket(host, port);
        } catch (IOException e) {
            System.out.println(e);;
        }
    }

    public static void main(String[] args){

        String username = args[0];
        InetAddress server;

        try {
            server = InetAddress.getByName(args[1]);
            try {
                forwardsSocket = new Socket(server, 8080);

                PrintWriter out = new PrintWriter(forwardsSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(forwardsSocket.getInputStream()));
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

                out.println("REGISTER TORECV " + username + "\n \n");

                while(true){
                    String line = in.readLine();
                    if(line == null){
                        continue;
                    }
                    else if(line.substring(0, 9).equals("ERROR 100")){
                        System.out.println(line);
                        System.out.println("Enter new username:");
                        stdIn = new BufferedReader(new InputStreamReader(System.in));
                        username = stdIn.readLine();
                        out.println("REGISTER TORECV " + username + "\n \n");
                    }
                    else if(line.substring(0, 17).equals("REGISTERED TORECV")){
                        System.out.println("User Registered to Receive");
                        break;
                    }
                }

                sendMessageSocket = new Socket(server, 8080);
                out = new PrintWriter(sendMessageSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(sendMessageSocket.getInputStream()));
                
                out.println("REGISTER TOSEND " + username + "\n \n");

                while(true){
                    String line = in.readLine();
                    if(line == null){
                        continue;
                    }
                    else if(line.substring(0, 9).equals("ERROR 100")){
                        System.out.println(line);
                        System.out.println("Enter new username:");
                        stdIn = new BufferedReader(new InputStreamReader(System.in));
                        username = stdIn.readLine();
                        out.println("REGISTER TOSEND " + username + "\n \n");
                    }
                    else if(line.substring(0, 17).equals("REGISTERED TOSEND")){
                        System.out.println("User Registered to Send");
                        break;
                    }
                }

            } catch (IOException e) {
                System.out.println(e);;
            }

            ClientReceiveThread receiveThread = new Client.ClientReceiveThread(forwardsSocket, username);
            receiveThread.start();

            ClientSendThread sendThread = new Client.ClientSendThread(sendMessageSocket, username);
            sendThread.start();

        } catch (UnknownHostException e) {
            System.out.println(e);
        }

    }
}
