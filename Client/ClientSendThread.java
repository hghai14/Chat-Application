package Client;
import java.io.*;
import java.net.Socket;

public class ClientSendThread {
    Thread t;
    String threadName;
    String userName;
    Socket socket;

    public ClientSendThread(Socket sendMessageSocket, String clientName){
        socket = sendMessageSocket;
        userName = clientName;
        t = new Thread("Send Message Thread");
        t.start();
    }

    public void run(){
        while (true) {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
                out.println(stdIn.readLine());

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
