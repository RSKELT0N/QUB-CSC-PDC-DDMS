package core;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Input
{
    public static class CommandEntry
    {
        public CommandEntry(Command cmd, String desc, int argc, String exp)
        {
            this.m_cmd = cmd;
            this.m_desc = desc;
            this.m_argc = argc;
            this.m_exp = exp;
        }

        public Command m_cmd;
        public String m_desc;
        public int m_argc;
        public String m_exp;
    }

    public Input(Kademlia kademlia)
    {
        this.m_kademlia = kademlia;
        this.m_stdin = new Scanner(System.in);
        SetAliveState(true);
        InitialiseCommands();
    }

    private void InitialiseCommands()
    {
        this.m_commands = new HashMap<>();
        m_commands.put("/help",         new CommandEntry(this::Help,       "Display the necessary information to control the program",             0, "/help"));
        m_commands.put("/exit",         new CommandEntry(this::Exit,       "Close the peer and exit the program",                                  0, "/exit"));
        m_commands.put("/print",        new CommandEntry(this::Print,      "Will invoke the print function",                                       1, "/print [rt/dt/info/idx]"));
        m_commands.put("/connect",      new CommandEntry(this::Connect,    "Will connect to bootstrapped node if specified, otherwise broadcast.", 0, "/connect opt:[ [ip] [port] ]"));
        m_commands.put("/clear",        new CommandEntry(this::Clear,      "Will reset the data table within the peer",                            0, "/clear"));
        m_commands.put("/store",        new CommandEntry(this::Store,      "Store a key/value pair in the distributed system",                     2, "/store [key] [value]"));
        m_commands.put("/get",          new CommandEntry(this::Get,        "Get the value of the respective key in the distributed system",        1, "/get [key]"));
        m_commands.put("/init",         new CommandEntry(this::Init,       "Initialise the peer, creating its socket and thread for joining.",     0, "/init [opt:nickname] [opt:port]"));
        m_commands.put("/robot",        new CommandEntry(this::Robot,      "Automated robot option to generate example data in the network.",      1, "/robot [size]"));
        m_commands.put("/togglelink",   new CommandEntry(this::ToggleLink, "Toggle the connection of the peer to the network.",                    0, "/togglelink"));
        m_commands.put("/getkeys",      new CommandEntry(this::GetKeys,    "Return all known data keys within the network.",                       0, "/getkeys"));
    }

    public void ReceiveInput() throws InterruptedException, IOException, NoSuchAlgorithmException
    {
        PrintStartUp();
        while (m_alive)
        {
            String curr_input = GetInput();
            Optional<CommandEntry> command = GetCommandFunction(curr_input);

            if(command.isPresent())
            {
                if((curr_input.split(" ").length - 1) >= command.get().m_argc)
                {
                    String[] tokens = curr_input.split(" ");
                    command.get().m_cmd.Parse(Arrays.copyOfRange(tokens, 1, tokens.length));
                }
                else System.out.println("~ Incorrect parameter amount");
            }
            else System.out.println("~ unknown command");
        }
        System.out.println("Exiting..");
    }

    private String GetInput()
    {
        System.out.print("> ");
        String curr_input = m_stdin.nextLine();
        System.out.println();

        return curr_input;
    }

    private void PrintStartUp()
    {
        System.out.println("~ Prefix with command with '/'\n~ ('/help' for more info)");
    }

    private void GetKeys(String[] tokens) throws InterruptedException
    {
        if(m_kademlia.GetPeer() != null)
        {
            m_kademlia.GetallDataKeys();
        } else System.out.println("~ Please use the /init command to create the peer to access it's print functionality");
    }

    private void ToggleLink(String[] tokens)
    {
        if(m_kademlia.GetPeer() != null)
        {
            this.m_kademlia.ToggleLink();
        } else System.out.println("~ Please use the /init command to create the peer to access it's print functionality");
    }

    private void Connect(String[] tokens) throws IOException, NoSuchAlgorithmException, InterruptedException
    {
        if(m_kademlia.GetPeer() == null)
            m_kademlia.InitPeer(RandomLetters(10), 0);

        if(tokens.length == 2)
        {
            m_kademlia.ConnectToBootStrapped(tokens[0], Integer.parseInt(tokens[1]));
        } else m_kademlia.ConnectThroughBroadcast();
    }

    private void Clear(String[] tokens)
    {
        m_kademlia.GetPeer().m_data_table = new TreeMap<>();
    }

    private void Print(String[] tokens)
    {
        if(this.m_kademlia.GetPeer() != null)
        {
            String option = tokens[0];

            switch (option) {
                case "rt":
                    this.m_kademlia.GetPeer().PrintRoutingTable();
                    break;
                case "idx":
                    this.m_kademlia.GetPeer().PrintIndexTable();
                    break;
                case "dt":
                    this.m_kademlia.GetPeer().PrintDataTable();
                    break;
                case "info":
                    this.m_kademlia.PrintInfo();
                    break;
                default:
                    System.out.println("~ Option not valid");
                    break;
            }
        } else System.out.println("~ Please use the /init command to create the peer to access it's print functionality");
    }

    private void Store(String[] tokens) throws NoSuchAlgorithmException, InterruptedException
    {
        if(m_kademlia.GetPeer() != null)
        {
            this.m_kademlia.StoreData(tokens);
        } else System.out.println("~ Please use the /init command to initialise the peer before accessing data in the network");
    }

    private void Robot(String[] tokens) throws NoSuchAlgorithmException, InterruptedException
    {
        if(m_kademlia.GetPeer() != null)
        {
            String[] data = new String[2];

            int value_size = Integer.parseInt(tokens[0]);
            String key = RandomLetters(10);
            String value = RandomLetters(value_size);
            data[0] = key;
            data[1] = value;

            this.m_kademlia.StoreData(data);
        } else System.out.println("~ Please use the /init command to initialise the peer before accessing data in the network");
    }

    private void Init(String[] tokens) throws NoSuchAlgorithmException, IOException
    {
        String nickname = RandomLetters(10);
        int port = 0;

        switch (tokens.length)
        {
            case 1:
                nickname = CheckValidNickname(tokens[0]);
                break;
            case 2:
                nickname = CheckValidNickname(tokens[0]);
                port = Integer.parseInt(tokens[1]);
                break;
            default:
                break;
        }
        this.m_kademlia.InitPeer(nickname, port);
    }

    private String CheckValidNickname(String nick_name)
    {
        if(nick_name.length() > 10)
        {
            nick_name = nick_name.substring(0, 10);
        }
        return nick_name;
    }

    private void Get(String[] tokens) throws NoSuchAlgorithmException, InterruptedException
    {
        if(m_kademlia.GetPeer() != null)
        {
            this.m_kademlia.GetPeer().GetDataItem(tokens[0]);
        } else System.out.println("~ Please use the /init command to initialise the peer before accessing data in the network");
    }

    private void Help(String[] tokens)
    {
        System.out.println("Commands\n----------");
        for(var command : m_commands.entrySet())
        {
            System.out.format("~ %-15s (%d) - %-70s | %s\n", command.getKey(), command.getValue().m_argc, command.getValue().m_desc, command.getValue().m_exp);
        }
    }

    private void Exit(String[] tokens)
    {
        SetAliveState(false);
        m_kademlia.Close();
    }

    private void SetAliveState(boolean state)
    {
        this.m_alive = state;
    }

    private Optional<CommandEntry> GetCommandFunction(String in)
    {
        return Optional.ofNullable(m_commands.get(in.split(" ")[0]));
    }

    private String RandomLetters(int size)
    {
        StringBuilder ss = new StringBuilder();

        for(int i = 0; i < size; i++)
        {
            char curr = (char) ((Math.random() * ('z' - 'a' + 1)) + 'a');
            ss.append(curr);
        }
        return ss.toString();
    }

    private boolean m_alive;
    private Scanner m_stdin;
    private Kademlia m_kademlia;
    private HashMap<String, CommandEntry> m_commands;
}
