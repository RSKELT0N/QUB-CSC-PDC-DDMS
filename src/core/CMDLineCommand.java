package core;

public interface CMDLineCommand
{
    public void Parse(String[] command) throws InterruptedException;
}
