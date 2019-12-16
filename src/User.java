import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class User {
    String username;
    String currentChannel;
    Socket socket;
    PrintWriter printWriter;
    boolean showTime = true;

    public User(Socket socket) {
        this.socket = socket;
        try {
            this.printWriter = new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.currentChannel = "main";
    }

    public void setTimestamp(boolean b, Server server) {
        showTime = b;
        server.sendTo(Server.ANSI_GREEN + "Succesfully set timestamp to " + b + Server.ANSI_RESET, this);
    }

    public Socket getSocket() {
        return socket;
    }

    public PrintWriter getPrintWriter() {
        return printWriter;
    }

    public void disconnect() throws Exception {
        printWriter.close();
        socket.close();
    }

    public String toString() {
        return username;
    }
}
