package core;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.Scanner;

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
        m_commands.put("/help",    new CommandEntry(this::Help,    "Display the necessary information to control the program",      0, "/help"));
        m_commands.put("/exit",    new CommandEntry(this::Exit,    "Close the peer and exit the program",                           0, "/exit"));
        m_commands.put("/print",   new CommandEntry(this::Print,   "Will invoke the print function",                                0, "/print"));
        m_commands.put("/connect", new CommandEntry(this::Connect, "Will connect to bootstrapped node",                             2, "/connect [ip] [port]"));
        m_commands.put("/clear",   new CommandEntry(this::Clear,   "Will reset the console",                                        0, "/clear"));
        m_commands.put("/store",   new CommandEntry(this::Store,   "Store a key/value pair in the distributed system",              2, "/store [key] [value]"));
        m_commands.put("/get",     new CommandEntry(this::Get,     "Get the value of the respective key in the distributed system", 1, "/get [key]"));
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
                if(command.get().m_argc == curr_input.split(" ").length - 1)
                    command.get().m_cmd.Parse(curr_input.split(" "));
                else System.out.println("~ Incorrect parameter amount");
            } else System.out.println("~ unknown command");
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

    private void Connect(String[] tokens)
    {
        m_kademlia.ConnectToBootStrapped(tokens[1], Integer.parseInt(tokens[2]));
    }

    private void Clear(String[] tokens) throws IOException
    {
        final String os = System.getProperty("os.name");

        if (os.contains("Windows"))
            Runtime.getRuntime().exec("cls");
        else Runtime.getRuntime().exec("clear");

        PrintStartUp();
    }

    private void Print(String[] tokens)
    {
        System.out.println("~ Select option");
        System.out.println("[rt] - routing table");
        System.out.println("[dt] - data table");
        System.out.println("[info] - peer info");

        switch (GetInput())
        {
            case "rt" -> this.m_kademlia.GetPeer().PrintRoutingTable();
            case "dt" -> this.m_kademlia.GetPeer().PrintDataTable();
            case "info" -> this.m_kademlia.PrintInfo();
            default -> System.out.println("~ Option not valid");
        }
    }

    private void Store(String[] tokens) throws NoSuchAlgorithmException, InterruptedException
    {
        this.m_kademlia.StoreData(Arrays.copyOfRange(tokens, 1, tokens.length));
    }

    private void Get(String[] tokens)
    {
        this.m_kademlia.GetData(tokens[1]);
    }

    private void Help(String[] tokens)
    {
        System.out.println("Commands\n----------");
        for(var command : m_commands.entrySet())
        {
            System.out.format("~ %-8s (%d) - %-65s | %s\n", command.getKey(), command.getValue().m_argc, command.getValue().m_desc, command.getValue().m_exp);
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

    private boolean m_alive;
    private Scanner m_stdin;
    private Kademlia m_kademlia;
    private HashMap<String, CommandEntry> m_commands;
}
