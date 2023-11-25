package core;

import java.io.IOException;

public interface Command
{
    public void Parse(String[] command) throws InterruptedException, IOException;
}
