import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//Abgabe 16.12 spätestens
//Test 11.12 zu RegEx (Stunde zu regex am 9.12.)

/*******
 TODO:
 1. Socket und PrintWriter in einer Hilfsklasse verwalten (Konrad) #
 1a. Abmelden reparieren #
 1b. Ende-Befehl #
 2. SendToAll nicht an sich selbst (Jireh) #
 3. SendTo mit Empfänger (Florian, Michael) #
 5. Farben mit Terminalemulation (Veit) (#### fdsafsdfsf ####)#
 4. Usernames (Florian) #
 15. Meldung mit Uhrzeit #
 6. Beitreten und Abmelden wird an alle gesendet #
 8. Liste Mitglieder auf (Tobias)#
 9. MOTD Gruß (Michael)
 12. Hilfe-Befehl (Tobias)#
 13. Status-Meldungen
 7. Zitieren (Jan)
 10. Sprach-Filter (Jan)
 14. Timeout für unflätige Nutzer
 11. Gruppen (Konrad)#
 16. Protokoll als TXT-Datei#
 ******/

public class Server {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_BLACK_BACKGROUND = "\u001B[40m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
    public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    public static final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
    public static final String ANSI_CYAN_BACKGROUND = "\u001B[46m";
    public static final String ANSI_WHITE_BACKGROUND = "\u001B[47m";
    public static final String logFile = "log.txt";

    HashMap<String, Channel> channels = new HashMap();
    ArrayList<User> users = new ArrayList<>();
    private ServerSocket serverSocket;

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Listening on " + serverSocket + "(Port " + port + ")");
        channels.put("main", new Channel("main", this));
        listen(port);
    }

    static public void main(String args[]) throws Exception {
        int port = 3000;
        new Server(port);
    }

    void log(String log) {
        try {
            Helpers.writeFile(logFile, new Date() + "->" + log);
        } catch (Exception e) {
            System.out.println("Error in logging!");
        }
    }

    void sendTo(String message, User reciever) {
        reciever.getPrintWriter().println(message);
        reciever.getPrintWriter().flush();
    }

    void alert(User reciever) {
        reciever.getPrintWriter().println("\007");
        reciever.getPrintWriter().flush();
    }

    private void listen(int port) throws IOException {


        while (true) {
            Socket s = serverSocket.accept();
            System.out.println("New connection from " + s);
            User newUser = new User(s);
            new TCPChatClientThread(this, newUser);
        }
    }

    void removeConnection(User user) {
        try {
            user.disconnect();
            for (Map.Entry<String, Channel> entry : channels.entrySet()) {
                entry.getValue().sendToAll(ANSI_GREEN + "###" + user.username + " left###" + ANSI_RESET);
            }
            users.remove(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void execute(String sentCommand, User executer) {
        String command = "", subcommand = "", params = "";
        Pattern p = Pattern.compile("^\\/([a-z]+)( ([a-z0-9]+))?( (.+))?$");
        Matcher m = p.matcher(sentCommand);
        if (m.find()) {
            command = m.group(1);
            subcommand = m.group(3);
            params = m.group(5);
        }
        switch (command) {
            case "pm":
                if (subcommand == null || params == null) {
                    sendTo(ANSI_YELLOW + "pm " + ANSI_RESET + "commands:", executer);
                    sendTo(ANSI_CYAN + "[receiver] [message]" + ANSI_RESET + " \t\t Send [message] to [receiver].", executer);
                    return;
                }
                if (getUser(subcommand) != null) {
                    sendTo(ANSI_GREEN + executer.username + "@" + getUser(subcommand).username + ": " + ANSI_RESET + params + "\007", getUser(subcommand));
                } else {
                    sendTo(ANSI_YELLOW + "User " + subcommand + " not found!" + ANSI_RESET, executer);
                }
                break;
            case "channel":
                if (subcommand != null) {
                    switch (subcommand) {
                        case "join":
                            if (params == null) {
                                sendTo(ANSI_RED + "Channel name must not be null" + ANSI_RESET, executer);
                                return;
                            }
                            channels.get(executer.currentChannel).leave(executer);
                            if (channels.get(params) == null) {
                                channels.put(params, new Channel(params, this));
                                log("Created channel " + params);
                            }
                            channels.get(params).join(executer);
                            return;
                        case "members":
                            String channelName = params == null ? executer.currentChannel : params;
                            if (getChannel(channelName) == null) {
                                sendTo("Channel " + channelName + " not found!", executer);
                            } else {
                                sendTo("Users in channel " + channelName + ":" + ANSI_GREEN +
                                        getChannel(channelName).getUsernames() + ANSI_RESET, executer);
                            }
                            return;
                        case "get":
                            sendTo(ANSI_GREEN + "You are currently in channel " + executer.currentChannel + ANSI_RESET, executer);
                            return;
                    }
                }
                sendTo(ANSI_YELLOW + "channel " + ANSI_RESET + "commands:", executer);
                sendTo(ANSI_CYAN + "join [channelName]" + ANSI_RESET + " \t Join channel [channelName].", executer);
                sendTo(ANSI_CYAN + "members <channelName>" + ANSI_RESET + " \t Get all members of channelName or current channel.", executer);
                sendTo(ANSI_CYAN + "get" + ANSI_RESET + " \t\t\t Get your current channel", executer);
                break;
            case "set":
                if (subcommand != null) {
                    switch (subcommand) {
                        case "username":
                            if (params == null) {
                                sendTo(ANSI_RED + "/set username: Please also enter a username!\007" + ANSI_RESET, executer);
                            } else {
                                if (setUsername(executer, params)) {
                                    sendTo(ANSI_GREEN + "Successfully changed username to " + params + ANSI_RESET, executer);
                                }
                            }
                            return;
                        case "timestamp":
                            if (params == null) {
                                executer.setTimestamp(!executer.showTime, this);
                                executer.showTime = !executer.showTime;
                            } else if (params.equals("on")) {
                                executer.setTimestamp(true, this);
                            } else if (params.equals("off")) {
                                executer.setTimestamp(false, this);
                            } else {
                                sendTo(ANSI_YELLOW + "Params must be on or off" + ANSI_RESET, executer);
                            }
                            return;
                    }
                }

                sendTo(ANSI_YELLOW + "set " + ANSI_RESET + "commands:", executer);
                sendTo(ANSI_CYAN + "username [newUsername]" + ANSI_RESET + " \t\t Set your username to [newUsername]", executer);
                sendTo(ANSI_CYAN + "timestamp <on|off>" + ANSI_RESET + " \t\t Turn on/off the timestamp before messages", executer);

                break;
            case "get":
                if (subcommand != null) {
                    switch (subcommand) {
                        case "username":
                            sendTo("Your username is " + executer.username, executer);
                            return;
                        case "members":
                            if (params != null) {
                                execute("/channel members " + params, executer);
                            } else {
                                sendTo("Users in chat: " + this.users, executer);
                            }
                            return;
                        case "channels":
                            sendTo("Registered channels in chat: " + this.channels.values(), executer);
                            return;
                        case "channel":
                            execute("/channel get", executer);
                            return;
                    }
                }
                sendTo(ANSI_YELLOW + "get " + ANSI_RESET + "commands:", executer);
                sendTo(ANSI_CYAN + "username" + ANSI_RESET + " \t\t Get your username", executer);
                sendTo(ANSI_CYAN + "members <channel>" + ANSI_RESET + " \t Get all members in the chatroom or channel [channel]", executer);
                sendTo(ANSI_CYAN + "channels" + ANSI_RESET + " \t\t Get all registered channels", executer);
                sendTo(ANSI_CYAN + "channel" + ANSI_RESET + " \t\t Get user's current channel", executer);

                break;
            case "help":
                sendTo(ANSI_CYAN + "/pm [reciever] [message]: \t\t" + ANSI_RESET + "Send a personal message to [reciever]", executer);
                sendTo(ANSI_CYAN + "/set: \t\t" + ANSI_RESET + "Set parameters of your account", executer);
                sendTo(ANSI_CYAN + "/get: \t\t" + ANSI_RESET + "Information about your account and the chatroom", executer);
                sendTo(ANSI_CYAN + "/channel: \t\t" + ANSI_RESET + "Information about the channel", executer);
                sendTo(ANSI_CYAN + "/quit: \t\t" + ANSI_RESET + "Quit programm", executer);

                break;
            case "quit":
                try {
                    executer.disconnect();
                } catch (Exception e) {
                    log("An error occured at removing " + executer);
                }
                break;
            default:
                sendTo(ANSI_RED + "Could not find command <" + sentCommand + ">! Type /help for help.\007" + ANSI_RESET, executer);

        }
    }

    boolean setUsername(User user, String username) {
        if (user.username != null) {
            log(user.username + " changed username to " + username + ".");
        }
        if (!username.matches("[\\w\\d]+")) {
            sendTo(ANSI_RED + "Usernames can only contain letters and numbers.\007" + ANSI_RESET, user);
            sendTo(ANSI_YELLOW + "Please enter another username!" + ANSI_RESET, user);

            return false;
        } else {
            if (getUser(username) == null) {
                user.username = username;
                return true;
            } else {
                sendTo(ANSI_RED + "Username already taken. Type in other username.\007" + ANSI_RESET, user);
                sendTo(ANSI_YELLOW + "Please enter another username!" + ANSI_RESET, user);
                return false;
            }
        }
    }

    User getUser(String username) {
        for (User user : users) {
            if (user.username != null && user.username.equals(username)) {
                return user;
            }
        }
        return null;
    }

    Channel getChannel(String name) {
        return channels.get(name);
    }

    class TCPChatClientThread extends Thread {

        private Server server;

        private User user;
        private boolean running = true;

        public TCPChatClientThread(Server server, User user) {
            this.server = server;
            this.user = user;
            users.add(user);
            start();
        }

        public void run() {
            try {
                Scanner din = new Scanner(user.getSocket().getInputStream());
                String username;
                server.sendTo("Welcome to the chat!", user);
                server.sendTo("Type in your username", user);
                username = din.nextLine();
                while (running) {
                    if (user.username == null) {
                        if (server.setUsername(user, username)) {
                            server.sendTo("Successully set your username to " + username + "!", user);
                            channels.get("main").join(user);
                        } else {
                            username = din.nextLine();
                        }

                    } else {
                        String message = "";
                        try {
                            message = din.nextLine();
                        } catch (Exception e) {
                            running = false;
                            break;
                        }
                        if (message.startsWith("/")) {
                            server.execute(message, this.user);
                        } else {
                            server.channels.get(user.currentChannel).sendToAll(message, user);
                        }
                    }
                }
            } catch (IOException ie) {
                ie.printStackTrace();
            } finally {
                getChannel(user.currentChannel).leave(user);
                server.removeConnection(user);
            }
        }

    }
}
