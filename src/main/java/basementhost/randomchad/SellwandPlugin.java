package basementhost.randomchad;

import basementhost.randomchad.listener.SellwandListener;
import basementhost.randomchad.service.SellService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class SellwandPlugin extends JavaPlugin {
    private Economy economy;
    private SellService sellService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!hookVault()) {
            getLogger().severe("Vault or a Vault economy provider was not found. Disabling SimpleSellWand.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (!isEconomyShopGuiLoaded()) {
            getLogger().severe("EconomyShopGUI/EconomyShopGUI-Premium was not found. Disabling SimpleSellWand.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        reloadServices();
        Bukkit.getPluginManager().registerEvents(new SellwandListener(this, sellService), this);
        getLogger().info("SimpleSellWand is enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SimpleSellWand is disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("simplesellwand.reload")) {
                sender.sendMessage(color(getConfig().getString("messages.prefix", "")
                        + getConfig().getString("messages.no-permission", "&cNo permission.")));
                return true;
            }

            reloadConfig();
            reloadServices();
            sender.sendMessage(color(getConfig().getString("messages.prefix", "")
                    + getConfig().getString("messages.reloaded", "&aReloaded.")));
            return true;
        }

        sender.sendMessage(color("&eUsage: /" + label + " reload"));
        return true;
    }

    public Economy economy() {
        return economy;
    }

    public String message(String path) {
        return color(getConfig().getString("messages.prefix", "") + getConfig().getString("messages." + path, ""));
    }

    public String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }

    private void reloadServices() {
        this.sellService = new SellService(this);
    }

    private boolean hookVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> registration = getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            return false;
        }

        economy = registration.getProvider();
        return economy != null;
    }

    private boolean isEconomyShopGuiLoaded() {
        return Bukkit.getPluginManager().getPlugin("EconomyShopGUI-Premium") != null
                || Bukkit.getPluginManager().getPlugin("EconomyShopGUI") != null;
    }
}
