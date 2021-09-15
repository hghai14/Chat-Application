package Client;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


public class Client{
    Socket forwardsSocket;
    Socket sendMessageSocket;

    public Client(InetAddress host, int port, String userName) {
        try {
            sendMessageSocket = new Socket(host, port);
            ClientSendThread sendThread = new ClientSendThread(sendMessageSocket, userName);

        } catch (IOException e) {
            System.out.println(e);;
        }


    }

    public static void main(String[] args){

        String username = args[0];
        InetAddress server;

        try {
            server = InetAddress.getByName(args[1]);

            Client user = new Client(server, 8080, username);


        } catch (UnknownHostException e) {
            System.out.println(e);
        }

    }
}
