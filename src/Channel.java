import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Channel {
    String channelName;
    Server server;
    private ArrayList<User> users = new ArrayList();

    public Channel(String name, Server server) {
        this.channelName = name;
        this.server = server;
    }

    void sendToAll(String message) {
        synchronized (users) {
            for (User user : users) {
                user.getPrintWriter().println(message);
                user.getPrintWriter().flush();
            }
        }
    }

    void sendToAll(String message, User sender) {
        Date date = new Date();   // given date
        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
        calendar.setTime(date);   // assigns calendar to given date
        server.log(sender.username + "@" + channelName + ": " + message);
        synchronized (users) {
            for (User user : users) {
                if (sender != user) {
                    String messageHeader = sender.username;
                    if (user.showTime) {
                        messageHeader += " (" + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ")";
                    }
                    user.getPrintWriter().println(messageHeader + ": " + message);
                    user.getPrintWriter().flush();
                }
            }
        }
    }

    void joinMessage(User joining) {
        synchronized (users) {
            for (User user : users) {
                if (!joining.equals(user)) {
                    user.getPrintWriter().println(Server.ANSI_GREEN + "###" + joining.username + " joined the chat###\007" + Server.ANSI_RESET);
                    user.getPrintWriter().flush();
                }
            }
        }
        server.sendTo(Server.ANSI_GREEN + "###You have joined " + channelName + "###" + Server.ANSI_RESET, joining);
    }

    void leaveMessage(User leaving) {
        synchronized (users) {
            for (User user : users) {
                if (!leaving.equals(user)) {
                    user.getPrintWriter().println(Server.ANSI_GREEN + "###" + leaving.username + " left the chat###\007" + Server.ANSI_RESET);
                    user.getPrintWriter().flush();
                }
            }
        }
        server.sendTo(Server.ANSI_GREEN + "###You have left " + channelName + "###" + Server.ANSI_RESET, leaving);
    }

    public void join(User user) {
        users.add(user);
        user.currentChannel = channelName;
        joinMessage(user);
        server.log(user.username + " joined " + channelName);
    }

    public void leave(User user) {
        users.remove(user);
        user.currentChannel = null;
        leaveMessage(user);
        server.log(user.username + " left " + channelName);
        if (users.size() == 0 && !channelName.equals("main")) {
            server.log("Deleted channel " + channelName);
            server.channels.remove(channelName);
        }
    }

    ArrayList<User> getUsers() {
        return this.users;
    }

    String getUsernames() {
        if (users.size() == 0) {
            return "[]";
        }
        String out = "[";
        for (User user : users) {
            out += user.username + ", ";
        }
        out = out.substring(0, out.length() - 2);
        out += "]";
        return out;
    }

    @Override
    public String toString() {
        return channelName;
    }
}
