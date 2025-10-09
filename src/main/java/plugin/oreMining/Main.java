package plugin.oreMining;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.oreMining.command.OreMiningCommand;

public final class Main extends JavaPlugin {

  @Override
  public void onEnable() {
    OreMiningCommand oreMiningCommand = new OreMiningCommand(this);
    Bukkit.getPluginManager().registerEvents(oreMiningCommand,this);
    getCommand("oreMining").setExecutor(oreMiningCommand);
  }
}