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
	public void onEnable() {
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
		} else {
			log.info(String.format("[%s] - Vault dependency found!", getDescription().getName()));
		}
		setupPermissions();
		config = new YamlConfiguration();
		data = new YamlConfiguration();
		loadYamls();
		if (config.getBoolean("Debug", true)) {
			log.info(String.format("[%s] - You are in debug mode!", getDescription().getName()));
		}

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
		if(!configFile.exists()) {
			configFile.getParentFile().mkdirs();
			copy(getResource("config.yml"), configFile);
		}
		if(!dataFile.exists()) {
			dataFile.getParentFile().mkdirs();
			copy(getResource("data.yml"), dataFile);
		}
	}
	private void copy(InputStream in, File file) {
		try {
			OutputStream out = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int len;
			while((len=in.read(buf))>0) {
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
		if(!command.getLabel().equals("shop")) {
			return false;
		}
		Player p = (Player) sender;
		boolean confirm;
		
		if(args[0].equalsIgnoreCase("Sell")) {
			if(args.length > 5) {
				sender.sendMessage("Correct Usage:");
				sender.sendMessage("/shop sell <Item|ID> <Amount> <Price> [confirm]");
				return false;
			}
			if(args.length < 4) {
				sender.sendMessage("Correct Usage:");
				sender.sendMessage("/shop sell <Item|ID> <Amount> <Price> [confirm]");
				return false;
			}
			if(args.length > 4 && args[4].equalsIgnoreCase("confirm")) {
				confirm = true;
			}
			else {
				confirm = false;
			}
			long IDcount = data.getInt("Total");
			int amount = Integer.parseInt(args[2]);
			int price = Integer.parseInt(args[3]);
			int totalprice = price * amount;
			double t = config.getInt("Taxes");
			double t1 = t / 100;
			double tax = price * amount * t1;
			tax = (double) ((int) ((tax + 0.005) * 100) % Integer.MAX_VALUE) / 100;
			Material mat = Material.matchMaterial(args[1]);
			int id = mat.getId();
			ItemStack is = new ItemStack (mat, amount);
			if(confirm) {
				if(p.getInventory().contains(mat, amount)) {
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
						data.set(IDcount + ".Check", "T");
						saveYamls();
					} else {
						sender.sendMessage(String.format("An error occured: %s", r.errorMessage));
					}
				}
			}
			else {
				if (config.getBoolean("Debug")) {
					log.info(String.format("t = " + t1));
				}
				sender.sendMessage("You will sell " + args[2] + " " + args[1] + ". The total price will be " + String.valueOf(totalprice) + ". With the cost of " + String.valueOf(tax) + " for taxes.");
			}
		}
		else if(args[0].equalsIgnoreCase("List")) {
			if(args.length > 3) {
				sender.sendMessage(String.format("Sytax Error"));
				return false;
			}
			if(args.length < 2) {
				sender.sendMessage(String.format("Sytax Error"));
				return false;
			}
			if(args[1].equalsIgnoreCase("amount")) {
				if(args.length > 2 && Material.matchMaterial(args[2]) != null) {
					Material mat = Material.matchMaterial(args[2]);
					int amount = 0;
					for(int i = 1; i <= data.getInt("Total"); i++) {
						if(data.getInt(i + ".Item") == mat.getId() && data.getString(i + ".Status").equals("Selling")) {
							amount += data.getInt(i + ".Amount");
							if(config.getBoolean("Debug")) {
								log.info(String.format("A = " + amount));
							}
						}
					}
					if(amount == 1) {
						sender.sendMessage(String.format("There is only " + amount + " " + args[2] + " left."));
					} else if(amount == 0) {
						sender.sendMessage(String.format("There is no " + args[2] + " on the market."));
					} else {
						sender.sendMessage(String.format("There are " + amount + " " + args[2] + " left."));
					}
				}
				else {
					sender.sendMessage(String.format("Please specify an item."));
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
				int i = data.getInt("Total");
				int printed_items = 0;
				while(i>0 && printed_items <10){
					if(data.getString(i + ".Status").equals("Selling"))
					{
						sender.sendMessage(String.format("A(n) amount of " + data.getInt(i + ".Amount") + " " + Material.getMaterial(data.getInt(i + ".Item")) + " has been added for " + data.getInt(i + ".Price") + "."));
						printed_items++;
					}
					i--;
				}
			}
		}
		else if(args[0].equalsIgnoreCase("Buy")) {
			//Find cheapest price of item, buy that first until it is empty, delete lines in yml if empty, and if buyer has more amount then go for the next cheapest
			
			if(args.length > 4) {
				sender.sendMessage("Correct Usage:");
				sender.sendMessage("/shop buy <Item|ID> <Amount> [confirm]");
				return false;
			}
			if(args.length < 3) {
				sender.sendMessage("Correct Usage:");
				sender.sendMessage("/shop buy <Item|ID> <Amount> [confirm]");
				return false;
			}
			if(args.length > 3 && args[3].equalsIgnoreCase("confirm")) {
				confirm = true;
			}
			else {
				confirm = false;
			}
			
			Material mat = Material.matchMaterial(args[1]);
			int id = mat.getId();
			int amount = Integer.parseInt(args[2]);
			int totalprice = 0;
			int currentamount = 0;
			int totalamount = amount;
			int j = 0;
			int i = 0;
			int k = 0;
			int datatotalamount = 0;
			int lowestPrice;
			while(currentamount != totalamount) {
				if (config.getBoolean("Debug")) {
					log.info(String.format("New Loop"));
				}
				lowestPrice = -1;
				for(i = 1; i <= data.getInt("Total"); i++) {
					if((data.getInt(i + ".Item") == id) && (data.getString(i + ".Status").equals("Selling")) && (data.getString(i + ".Check").equals("T"))) {
						if (config.getBoolean("Debug")) {
							getLogger().info("dta= " + String.valueOf(datatotalamount) + " i= " + String.valueOf(i) + " a = " + String.valueOf(amount));
						}
						if(lowestprice < 0 || data.getInt(i + ".Price") < lowestPrice) {
							lowestPrice = data.getInt(i + ".Price");
							j= i;
						}
					}
				}
				if(lowestprice < 0) {
					sender.sendMessage(String.format("There is not enough " + mat + " on sale."));
					return false;
				}
				if (config.getBoolean("Debug")) {
					getLogger().info("J= " + String.valueOf(j) + " Name: " + data.getString(j + ".Player"));
				}
				if(data.getInt(j + ".Amount") <= amount) {
					int price = data.getInt(j + ".Price") * data.getInt(j + ".Amount");
					totalprice += price;
					currentamount += data.getInt(j + ".Amount");
					amount -= data.getInt(j + ".Amount");
					if(confirm) {
						EconomyResponse r1 = econ.depositPlayer(data.getString(j + ".Player"), price);
						if(r1.transactionSuccess()) {
							data.set(j + ".Status", "Sold");
							saveYamls();
						}
					} else {
						data.set(j + ".Check", "F");
						saveYamls();
					}
				} else if(data.getInt(j + ".Amount") > amount) {
					int price = amount * data.getInt(j + ".Price");
					totalprice += price;
					currentamount += amount;
					if(confirm) {
						EconomyResponse r1 = econ.depositPlayer(data.getString(j + ".Player"), price);
						if(r1.transactionSuccess()) {
							data.set(j + ".Amount", (data.getInt(j + ".Amount") - amount));
							saveYamls();
						}
					}
					else {
						data.set(j + ".Check", "F");
						saveYamls();
					}
				}
			}
			if(confirm) {
				if (config.getBoolean("Debug")) {
					log.info(String.format(args[3]));
				}
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
					sender.sendMessage(String.format("You will pay " + totalprice + " for " + currentamount + " of " + mat + "."));
					for(k = 1; k <= data.getInt("Total"); k++) {
						if((data.getString(k + ".Check").equals("F"))) {
							loadYamls();
							data.set(k + ".Check", "T");
							saveYamls();
						}
					}
				}
			}
		return true;
	}
	return false;
	}
}
