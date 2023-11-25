package core;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class Input
{
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
        m_commands.put("/help", new Lib.Pair<>(this::Help, "Display the necessary information to control the program"));
        m_commands.put("/exit", new Lib.Pair<>(this::Exit, "Close the peer and exit the program"));
        m_commands.put("/print", new Lib.Pair<>(this::Print, "Will invoke the print function"));
        m_commands.put("/clear", new Lib.Pair<>(this::Clear, "Will reset the console"));
        m_commands.put("/store", new Lib.Pair<>(this::Store, "Store a key/value pair in the distributed system"));
        m_commands.put("/get", new Lib.Pair<>(this::Clear, "Get the value of the respective key in the distributed system"));
    }

    public void ReceiveInput() throws InterruptedException, IOException, NoSuchAlgorithmException
    {
        PrintStartUp();
        while (m_alive)
        {
            String curr_input = GetInput();
            Command command = GetCommandFunction(curr_input);

            if(command != null)
                command.Parse(curr_input.split(" "));
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

    private void Clear(String[] tokens) throws IOException
    {
        final String os = System.getProperty("os.name");

        if (os.contains("Windows"))
            Runtime.getRuntime().exec("cls");
        else  Runtime.getRuntime().exec("clear");

        PrintStartUp();
    }

    private void Print(String[] tokens)
    {
        if(tokens.length == 2)
        {
            switch (tokens[1])
            {
                case "rt" -> this.m_kademlia.GetPeer().PrintRoutingTable();
                case "dt" -> this.m_kademlia.GetPeer().PrintDataTable();
                default -> System.out.println("~ Option not valid");
            }
        }
        else if(tokens.length == 1)
        {
            System.out.println("~ Append an option to the command");
            System.out.println("[rt] - routing table");
            System.out.println("[dt] - data table");

            String option = GetInput();
            String[] tokens_added = new String[2];
            tokens_added[1] = option;
            Print(tokens_added);
        }
        else System.out.println("~ Incorrect parameter amount");
    }

    private void Store(String[] tokens) throws NoSuchAlgorithmException, InterruptedException
    {
        if(tokens.length == 3)
            this.m_kademlia.StoreData(Arrays.copyOfRange(tokens, 1, tokens.length));
        else System.out.println("~ Incorrect parameter amount");

    }

    private void Help(String[] tokens)
    {
        System.out.println("Commands\n----------");
        for(var command : m_commands.entrySet())
        {
            System.out.format("~ %-20s - %s\n", command.getKey(), command.getValue().second);
        }
    }

    private void Exit(String[] tokens)
    {
        m_kademlia.Close();
        SetAliveState(false);
    }

    private void SetAliveState(boolean state)
    {
        this.m_alive = state;
    }

    private Command GetCommandFunction(String in)
    {
        Lib.Pair<Command, String> cmd = m_commands.get(in.split(" ")[0]);
        return cmd == null ? null : cmd.first;
    }

    private boolean m_alive;
    private Scanner m_stdin;
    private Kademlia m_kademlia;
    private HashMap<String, Lib.Pair<Command, String>> m_commands;
}
