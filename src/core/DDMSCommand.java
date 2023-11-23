package core;

public interface DDMSCommand
{
    public void Parse(Lib.Pair<Lib.Pair<String, Integer>, String> request, String[] command) throws InterruptedException;
}
