package core;

import core.peer.Runner;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        m_commands.put("/help",          new CommandEntry(this::Help,          "Display the necessary information to control the program",             0, "/help"));
        m_commands.put("/exit",          new CommandEntry(this::Exit,          "Close the peer and exit the program",                                  0, "/exit"));
        m_commands.put("/print",         new CommandEntry(this::Print,         "Will invoke the print function",                                       1, "/print [rt/dt/info/idx/data]"));
        m_commands.put("/connect",       new CommandEntry(this::Connect,       "Will connect to bootstrapped node if specified, otherwise broadcast.", 0, "/connect opt:[ [ip] [port] ]"));
        m_commands.put("/clear",         new CommandEntry(this::Clear,         "Will reset the data table within the peer",                            0, "/clear"));
        m_commands.put("/store",         new CommandEntry(this::Store,         "Store a key/value pair in the distributed system",                     2, "/store [key] [value]"));
        m_commands.put("/get",           new CommandEntry(this::Get,           "Get the value of the respective key in the distributed system",        1, "/get [key]"));
        m_commands.put("/init",          new CommandEntry(this::Init,          "Initialise the peer, creating its socket and thread for joining.",     0, "/init [opt:nickname] [opt:port]"));
        m_commands.put("/robot",         new CommandEntry(this::Robot,         "Automated robot option to generate example data in the network.",      1, "/robot [size]"));
        m_commands.put("/togglelink",    new CommandEntry(this::ToggleLink,    "Toggle the connection of the peer to the network.",                    0, "/togglelink"));
        m_commands.put("/getkeys",       new CommandEntry(this::GetKeys,       "Return all known data keys within the network.",                       0, "/getkeys"));
        m_commands.put("/storeweather",  new CommandEntry(this::StoreWeather,  "Stores the current weather of the passed city.",                       1, "/storeweather [city]"));
        m_commands.put("/storefile",     new CommandEntry(this::StoreFile,     "Stores the bytes found in the file of the passed path.",               2, "/storefile [name] [path]"));
        m_commands.put("/togglebeat",    new CommandEntry(this::ToggleBeat,    "Fetches the missing data items in the peers local data table.",        0, "/togglebeat"));
        m_commands.put("/obtainmissing", new CommandEntry(this::ObtainMissing, "Toggles the current heartbeat.",                                       0, "/obtainmissing"));
        m_commands.put("/rmdata",        new CommandEntry(this::RemoveData,    "Removes an piece of data from the peers local data table.",            1, "/rmdata [name]"));
        m_commands.put("/export",        new CommandEntry(this::Export,        "Export a piece of data towards a file.",                               2, "/export [name] [path]"));
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

    private void ObtainMissing(String[] tokens) throws InterruptedException, NoSuchAlgorithmException, IOException
    {
        if(m_kademlia.GetPeer() != null)
        {
            m_kademlia.ObtainMissing();
        } else System.out.println("~ Please use the /init command to create the peer to access it's print functionality");
    }

    private void ToggleLink(String[] tokens)
    {
        if(m_kademlia.GetPeer() != null)
        {
            this.m_kademlia.ToggleLink();
        } else System.out.println("~ Please use the /init command to create the peer to access it's print functionality");
    }

    private void ToggleBeat(String[] tokens)
    {
        if(m_kademlia.GetPeer() != null)
        {
            ((Runner)this.m_kademlia.GetPeer().m_heartbeat).ToggleLink();
            System.out.println("~ Heartbeat has been toggled");
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
        m_kademlia.GetPeer().m_data_keys = new HashMap<>();
    }

    private void Print(String[] tokens) throws NoSuchAlgorithmException
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
                    BigInteger key_hash = Lib.SHA1(option, BigInteger.valueOf(1).shiftLeft(m_kademlia.GetPeer().m_m_bits));
                    if(m_kademlia.GetPeer().m_data_table.containsKey(key_hash))
                        System.out.println("Data" + " " + "(" + option + ")" + ":" + " " + new String((byte[])m_kademlia.GetPeer().m_data_table.get(key_hash).value));
                    else System.out.println("~ Option not valid");
                    break;
            }
        } else System.out.println("~ Please use the /init command to create the peer to access it's print functionality");
    }

    private void Store(String[] tokens) throws NoSuchAlgorithmException, InterruptedException, IOException
    {
        if(m_kademlia.GetPeer() != null)
        {
            String key = tokens[0];
            String value = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
            this.m_kademlia.StoreData(key, value.getBytes(), false);
        } else System.out.println("~ Please use the /init command to initialise the peer before accessing data in the network");
    }

    private void StoreWeather(String[] tokens) throws NoSuchAlgorithmException, InterruptedException, IOException
    {
        if(m_kademlia.GetPeer() != null)
        {
            String city = String.join(" ", tokens);
            String apiUrl = "wttr.in/" + city.replace(" ", "%20") + "?format=%l:%20(Temp)+%t%20(Wind)+%w%20(Humidity)+%h";

            ProcessBuilder processBuilder = new ProcessBuilder("curl", "-s", apiUrl);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            InputStream reader = process.getInputStream();

            String result = new String(reader.readAllBytes());

            this.m_kademlia.StoreData(city, result.getBytes(), false);
        } else System.out.println("~ Please use the /init command to initialise the peer before accessing data in the network");
    }

    private void StoreFile(String[] tokens) throws NoSuchAlgorithmException, InterruptedException, IOException
    {
        if(m_kademlia.GetPeer() != null)
        {
            Path file = Paths.get(tokens[1]);

            if(!Files.exists(file))
            {
                System.out.println("~ Path given is not valid");
                return;
            }

            if(Files.size(file) > m_kademlia.GetPeer().m_socket.MAX_RECEIVE_SIZE)
            {
                System.out.println("~ File at specified path is too large (limit (16KB))");
                return;
            }

            byte[] file_bytes = Files.readAllBytes(file);

            this.m_kademlia.StoreData(tokens[0], file_bytes, true);
        } else System.out.println("~ Please use the /init command to initialise the peer before accessing data in the network");
    }

    private void Export(String[] tokens) throws NoSuchAlgorithmException, InterruptedException, IOException
    {
        if(m_kademlia.GetPeer() != null)
        {
            BigInteger hash_key = Lib.SHA1(tokens[0], BigInteger.valueOf(1).shiftLeft(m_kademlia.GetPeer().m_m_bits));

            if(m_kademlia.GetPeer().m_data_table.containsKey(hash_key))
            {
                Path file = Paths.get(tokens[1]);

                if (Files.exists(file))
                {
                    System.out.println("~ Path given has an already file present");
                    return;
                }

                Files.createFile(file);
                byte[] file_bytes = (byte[])m_kademlia.GetPeer().m_data_table.get(hash_key).value;
                Files.write(file, file_bytes);

            } else System.out.println("~ The data item" + " " + "(" + tokens[0] + ")" + " "  + "has not been found");
        } else System.out.println("~ Please use the /init command to initialise the peer before accessing data in the network");
    }

    private void Robot(String[] tokens) throws NoSuchAlgorithmException, InterruptedException, IOException
    {
        if(m_kademlia.GetPeer() != null)
        {
            int value_size = Integer.parseInt(tokens[0]);
            String key = RandomLetters(10);
            String value = RandomLetters(value_size);

            this.m_kademlia.StoreData(key, value.getBytes(), false);
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

    private void Get(String[] tokens) throws NoSuchAlgorithmException, InterruptedException, IOException
    {
        if(m_kademlia.GetPeer() != null)
        {
            this.m_kademlia.GetPeer().GetDataItem(tokens[0]);
        } else System.out.println("~ Please use the /init command to initialise the peer before accessing data in the network");
    }


    private void RemoveData(String[] tokens) throws NoSuchAlgorithmException
    {
        if(m_kademlia.GetPeer() != null)
        {
            boolean removed = this.m_kademlia.GetPeer().RemoveDataItem(tokens[0]);

            if(removed)
                System.out.println("~ Data item (" + tokens[0] + ")" + " " + "has been removed");
            else System.out.println("~ Data item (" + tokens[0] + ")" + " " + "was not found in the data table");

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

        if(m_kademlia.GetPeer() != null)
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
