package com.spgrn.tradingpost;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class TradingPost extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    public static Economy econ = null;
    public static Permission perms = null;
    File configFile;
    File dataFile;
    FileConfiguration config;
    FileConfiguration data;

    
    @Override
    public void onEnable(){
        configFile = new File(getDataFolder(), "config.yml");
        dataFile = new File(getDataFolder(), "data.yml");
        try {
            firstRun();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!setupEconomy() ) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
         }
    	 else{
    		 log.info(String.format("[%s] - Vault dependency found!", getDescription().getName()));
    	 }
         setupPermissions();
         config = new YamlConfiguration();
         data = new YamlConfiguration();
         loadYamls();
    }
 
    @Override
    public void onDisable() {
        log.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
    }
    
    public void saveYamls() {
        try {
            config.save(configFile);
            data.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void loadYamls() {
        try {
            config.load(configFile);
            data.load(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void firstRun() throws Exception {
        if(!configFile.exists()){
            configFile.getParentFile().mkdirs();
            copy(getResource("config.yml"), configFile);
        }
        if(!dataFile.exists()){
            dataFile.getParentFile().mkdirs();
            copy(getResource("data.yml"), dataFile);
        }
    }
    private void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }
    
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
    	
    	Player p = (Player) sender;
		
    	if (args.length > 4) {
            p.sendMessage("Too many arguments!");
            return false;
         } 
         if (args.length < 2) {
            p.sendMessage("Not enough arguments!");
            return false;
         }
         if(command.getLabel().equals("shop")) {
     		if(args[0].equalsIgnoreCase("Sell")) {
     			long IDcount = data.getInt("Total");
 				int amount = Integer.parseInt(args[2]);
 				int price = Integer.parseInt(args[3]);
 				double tax = price * amount * 0.15;
 				Material mat = Material.matchMaterial(args[1]);
 				int id = mat.getId();
 				ItemStack is = new ItemStack (mat, amount);
 				if(p.getInventory().contains(mat, amount)){
 					p.getInventory().removeItem(is);
 					EconomyResponse r = econ.withdrawPlayer(p.getName(), tax);
 					if(r.transactionSuccess()) {
 						IDcount++;
 						p.sendMessage("You have added " + args[2] + " " + args[1] + " for " + args[3] + " to the market. You have paid " + String.valueOf(tax) + " for taxes." );
 						loadYamls();
 						data.set("Total", IDcount);
 						data.set(IDcount + ".Player", ((Player)sender).getDisplayName());
 						data.set(IDcount + ".Item", id);
 						data.set(IDcount + ".Amount", amount);
 						data.set(IDcount + ".Price", price);
 						data.set(IDcount + ".Status", "Selling");
 						saveYamls();
 					} else {
 						sender.sendMessage(String.format("An error occured: %s", r.errorMessage));
 						}
 					}
 				}
     		if(args[0].equalsIgnoreCase("List")) {
     			if(args[2].equalsIgnoreCase("amount")) {
     				int amountc = 0 + data.getInt("Amount");
     				String totalamount = String.valueOf(amountc);
     				if(amountc == 1) {
     				sender.sendMessage(String.format("There is only " + String.valueOf(totalamount) + " of " + args[1] + " left."));
     				}
     				else if(amountc == 0) {
     					sender.sendMessage(String.format("There are none of " + args[1] + " on the market."));
     				}
     				else {
     					sender.sendMessage(String.format("There are " + String.valueOf(totalamount) + " of " + args[1] + " lerft."));
     				}
     			}
     			if(args[1].equalsIgnoreCase("common")) {
     				//Most amount of items in ID, displays the top 10
     				
     			}
     			if(args[1].equalsIgnoreCase("expensive")) {
     				//Most expensive items on the market by ID, displays top 10, (also checks the cheapest of that item ID to be the most expensive)
     				
     			}
     			if(args[1].equalsIgnoreCase("recent")) {
     				//Most recent items added to being sold
     				
     			}
     		}
     		if(args[0].equalsIgnoreCase("Buy")) {
     			//Find cheapest price of item, buy that first until it is empty, delete lines in yml if empty, and if buyer has more amount then go for the next cheapest
 				Material mat = Material.matchMaterial(args[1]);
 				int id = mat.getId();
 				int amount = Integer.parseInt(args[2]);
 				int totalprice = 0;
				int currentamount = 0;
				int totalamount = amount;
				int j = 0;
				int i = 0;
				int k = 0;
				String enough = "True";
				while(currentamount != totalamount) {
					log.info(String.format("New Loop"));
					int lowestPrice = 2147483647;
					for(i = 1; i <= data.getInt("Total"); i++) {
						if((data.getInt(i + ".Item") == id) && (data.getInt(i + ".Amount") >= amount))
						{
							if((data.getInt(i + ".Item") == id) && (data.getString(i + ".Status").equals("Selling")) && !(data.getString(j + ".Check").equals("False")) && (data.getInt(i + ".Price") < lowestPrice)) {
								lowestPrice = data.getInt(i + ".Price"); 
								j= i;
							}
						}
						else {
							enough = "False";
						}
					}
					if(enough.equals("False")) {
						sender.sendMessage(String.format("There is not enough" + mat + " on sale."));
						currentamount = totalamount;
					}
					else {
						getLogger().info("J= " + String.valueOf(j));
 						if(data.getInt(j + ".Amount") <= amount) {
 							int price = data.getInt(j + ".Price") * data.getInt(j + ".Amount");
 			 				totalprice += price;
 			 				currentamount += data.getInt(j + ".Amount");
 			 	 			amount -= data.getInt(j + ".Amount");
 			 	 			if(args[3].equalsIgnoreCase("confirm")) {
								EconomyResponse r1 = econ.depositPlayer(data.getString(j + ".Player"), price);
								if(r1.transactionSuccess()) {
									data.set(j + ".Status", 1);
									saveYamls();
		 							getLogger().info("C = " + String.valueOf(currentamount + " T = " + String.valueOf(totalamount)));
								}
							}
							else {
								data.set(j + ".Check", "False");
				 				saveYamls();
							}
 						}
 		 				else if(data.getInt(j + ".Amount") > amount){
 		 					int price = amount * data.getInt(j + ".Price");
 		 					totalprice += price;
		 					currentamount += amount;
 							if(args[3].equalsIgnoreCase("confirm")) {
 								EconomyResponse r1 = econ.depositPlayer(data.getString(j + ".Player"), price);
 								if(r1.transactionSuccess()) {
 									data.set(j + ".Amount", (data.getInt(j + ".Amount") - amount));
 									saveYamls();
 								}
 							}
 							else {
 								data.set(j + ".Check", "False");
 								saveYamls();
 							}
 		 				}
					}
				}
				if(args.length == 4 && args[3].equalsIgnoreCase("confirm")) {
					log.info(String.format(args[3]));
					if(currentamount == totalamount) {
						EconomyResponse r = econ.withdrawPlayer(p.getName(), totalprice);
						if(r.transactionSuccess()) {
							ItemStack is = new ItemStack (mat, currentamount);
							p.getInventory().addItem(is);	
							sender.sendMessage(String.format("You have bought " + totalamount + " of " + mat + " for " + totalprice + "."));
						}
					}
				}
				else {
	 				if(currentamount == totalamount) {
	 					sender.sendMessage(String.format("You will pay " + totalprice + " for " + amount + " of " + mat + "."));
		 				for(k = 1; k <= data.getInt("Total"); k++) {
							if((data.getString(k + ".Check") == "False")) {
								loadYamls();
								data.set(k + ".Check", null);
								saveYamls();
							}
						}
		 			}
	 			}
			}
     			return true;
		}
        	return false;
	}
}
