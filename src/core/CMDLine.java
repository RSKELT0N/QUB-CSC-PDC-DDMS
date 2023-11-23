package core;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Scanner;

public class CMDLine
{
    public CMDLine(Chord chord) throws SocketException, UnknownHostException, NoSuchAlgorithmException, InterruptedException
    {
        this.m_chord = chord;
        this.m_stdin = new Scanner(System.in);
        SetAliveState(true);
        chord.CreatePeer();
        InitialiseCommands();
        ReceiveInput();
    }

    private void Exit(String[] tokens)
    {
        m_chord.Close();
        SetAliveState(false);
    }

    private void InitialiseCommands()
    {
        this.m_commands = new HashMap<>();
        m_commands.put("/exit", this::Exit);
    }

    private void SetAliveState(boolean state)
    {
        this.m_alive = state;
    }

    private CMDLineCommand GetCommandFunction(String in)
    {
        return m_commands.get(in.split(" ")[0]);
    }

    private void ReceiveInput() throws InterruptedException
    {
        while (m_alive)
        {
            String curr_input = m_stdin.nextLine();
            CMDLineCommand command = GetCommandFunction(curr_input);
            command.Parse(curr_input.split(" "));
        }
        System.out.println("Exiting..");
    }

    private boolean m_alive;
    private Scanner m_stdin;
    private Chord m_chord;
    private HashMap<String, CMDLineCommand> m_commands;
}
