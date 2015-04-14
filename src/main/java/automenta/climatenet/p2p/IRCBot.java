package automenta.climatenet.p2p;

import automenta.climatenet.ElasticChannel;
import automenta.climatenet.data.elastic.ElasticSpacetime;
import automenta.knowtention.Core;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.elasticsearch.common.joda.time.DateTime;
import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;

/**
 * https://code.google.com/p/pircbotx/wiki/Documentation
 */
public class IRCBot extends ListenerAdapter {

    static {
        Logger.getLogger("org.pircbotx.InputParser").setLevel(Level.WARN);
    }

    private final PircBotX irc;
    private final ElasticChannel serverChannel;

    public IRCBot(ElasticSpacetime db, String nick, String server, String... channels) throws Exception {


        serverChannel = new ElasticChannel(db, "feature");

        Configuration.Builder<PircBotX> config = new Configuration.Builder()
                .setName(nick) //Nick of the bot. CHANGE IN YOUR CODE
                .setLogin(nick) //Login part of hostmask, eg name:login@host
                .setVersion("xchat 2.8.8 Linux 3.19.3-3-ARCH [x86_64/1.40GHz/SMP]")
                .setAutoNickChange(true) //Automatically change nick when the current one is in use
                .setServer(server, 6667);

        for (String c : channels)
            config.addAutoJoinChannel(c);


        config.addListener(this);
        this.irc = new PircBotX(config.buildConfiguration());

        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    irc.startBot();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    @Override
    public void onPing(PingEvent event) throws Exception {

    }

    @Override
    public void onMessage(MessageEvent m) throws Exception {
        log(m.getChannel(), m.getUser(), Event.MESSAGE, m.getMessage());
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent m) throws Exception {
        log(m.getUser().getServer(), m.getUser().getNick(), Event.PRIVATE, m.getMessage());
    }

    @Override
    public void onChannelInfo(ChannelInfoEvent event) throws Exception {

    }

    @Override
    public void onJoin(JoinEvent event) throws Exception {
        onJoin(event.getChannel(), event.getUser());
    }

    @Override
    public void onPart(PartEvent event) throws Exception {
        log(event.getChannel(), event.getUser(), Event.LEAVE, event.getReason());
    }

    @Override
    public void onTopic(TopicEvent event) throws Exception {

    }

    @Override
    public void onWhois(WhoisEvent event) throws Exception {
        log(event.getServer(), event.getNick(), Event.IS,
                event.getHostname() + " " + event.getRealname() + " " + event.getChannels().toString());
    }

    @Override
    public void onUserList(UserListEvent u) throws Exception {
        for (Object x : u.getUsers())
            onJoin(u.getChannel(), (User)x);
    }

    protected void onJoin(Channel channel, User user) {
        log(channel, user, Event.JOIN, null);
    }
    protected void log(Channel channel, User user, Event event, String value) {
        log(channel.getName(), user.getNick(), event, value);
    }

    public enum Event {
        IS, JOIN, LEAVE, MESSAGE, PRIVATE
    }

    protected void log(String channel, String nick, Event event, String value) {
        System.out.println(channel + " " + nick + " " + event + " " + value);


        ObjectNode o = Core.newJson.objectNode();

        switch (event) {
            case IS:
                o.put("inh.Identity", 1);
                break;
            case JOIN:
                o.put("inh.Join", 1);
                break;
            case LEAVE:
                o.put("inh.Leave", 1);
                break;
            case PRIVATE:
                o.put("inh.Private", 1);
            case MESSAGE:
                o.put("inh.Message", 1);
                break;
        }

        if (channel!=null)
            o.put("channel", channel);

        if (nick!=null)
            o.put("nick", nick);

        if (value!=null)
            o.put("description", value);

        o.put("startTime", new DateTime().toString());

        serverChannel.commit(o);
    }
}
