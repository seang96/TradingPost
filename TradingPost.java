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
		if (!setupEconomy()) {
			log.severe(String.format(
					"[%s] - Disabled due to no Vault dependency found!",
					getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		} else {
			log.info(String.format("[%s] - Vault dependency found!",
					getDescription().getName()));
		}
		setupPermissions();
		config = new YamlConfiguration();
		data = new YamlConfiguration();
		loadYamls();
		if (config.getBoolean("Debug", true)) {
			log.info(String.format("[%s] - You are in debug mode!",
					getDescription().getName()));
		}

	}

	@Override
	public void onDisable() {
		log.info(String.format("[%s] Disabled Version %s", getDescription()
				.getName(), getDescription().getVersion()));
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
		if (!configFile.exists()) {
			configFile.getParentFile().mkdirs();
			copy(getResource("config.yml"), configFile);
		}
		if (!dataFile.exists()) {
			dataFile.getParentFile().mkdirs();
			copy(getResource("data.yml"), dataFile);
		}
	}

	private void copy(InputStream in, File file) {
		try {
			OutputStream out = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
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
		RegisteredServiceProvider<Economy> rsp = getServer()
				.getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> rsp = getServer()
				.getServicesManager().getRegistration(Permission.class);
		perms = rsp.getProvider();
		return perms != null;
	}

	public boolean onCommand(CommandSender sender, Command command,
			String commandLabel, String[] args) {
		if (!command.getLabel().equals("shop")) {
			return false;
		}
		Player p = (Player) sender;
		boolean confirm;

		if (args[0].equalsIgnoreCase("Sell")) {
			if (args.length > 5) {
				sender.sendMessage(String.format("[%s] Syntax error.",
						getDescription().getName()));
				sender.sendMessage(String
						.format("[%s] Please type /shop sell <Item|ID> <Amount> <Price> [confirm]",
								getDescription().getName()));
				return false;
			}
			if (args.length < 4) {
				sender.sendMessage(String.format("[%s] Syntax error.",
						getDescription().getName()));
				sender.sendMessage(String
						.format("[%s] Please type /shop sell <Item|ID> <Amount> <Price> [confirm]",
								getDescription().getName()));
				return false;
			}
			if (args.length > 4 && args[4].equalsIgnoreCase("confirm")) {
				confirm = true;
			} else {
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
			ItemStack is = new ItemStack(mat, amount);
			if (confirm) {
				if (p.getInventory().contains(mat, amount)) {
					p.getInventory().removeItem(is);
					EconomyResponse r = econ.withdrawPlayer(p.getName(), tax);
					if (r.transactionSuccess()) {
						IDcount++;
						p.sendMessage(String.format("[%s] You have added "
								+ args[2] + " " + args[1] + " for " + args[3]
								+ " to the " + "market. You have paid "
								+ String.valueOf(tax) + " for taxes.",
								getDescription().getName()));
						loadYamls();
						data.set("Total", IDcount);
						data.set(IDcount + ".Player",
								((Player) sender).getDisplayName());
						data.set(IDcount + ".Item", id);
						data.set(IDcount + ".TotalAmount", amount);
						data.set(IDcount + ".Amount", amount);
						data.set(IDcount + ".Price", price);
						data.set(IDcount + ".Status", "Selling");
						data.set(IDcount + ".Check", "T");
						saveYamls();
					} else {
						p.sendMessage(String.format(
								"[%s] An error occured: %s", getDescription()
										.getName(), r.errorMessage));
					}
				}
			} else {
				if (config.getBoolean("Debug")) {
					log.info(String.format("[%s] t = " + t1, getDescription()
							.getName()));
				}
				p.sendMessage(String.format(
						"[%s] You will sell " + args[2] + " " + args[1]
								+ ". The total price will be "
								+ String.valueOf(totalprice)
								+ ". With the cost of " + String.valueOf(tax)
								+ " for taxes.", getDescription().getName()));
			}
		} else if (args[0].equalsIgnoreCase("List")) {
			if (args.length > 3) {
				sender.sendMessage(String.format("[%s] Syntax error.",
						getDescription().getName()));
				sender.sendMessage(String
						.format("[%s] Please type /shop list <amount|common|expensive|recent> [Item|ID]",
								getDescription().getName()));
				return false;
			}
			if (args.length < 2) {
				sender.sendMessage(String.format("[%s] Syntax error.",
						getDescription().getName()));
				sender.sendMessage(String
						.format("[%s] Please type /shop sell <Item|ID> <Amount> <Price> [confirm]",
								getDescription().getName()));
				return false;
			}
			if (args[1].equalsIgnoreCase("amount")) {
				if (args.length > 2 && Material.matchMaterial(args[2]) != null) {
					Material mat = Material.matchMaterial(args[2]);
					int amount = 0;
					for (int i = 1; i <= data.getInt("Total"); i++) {
						if (data.getInt(i + ".Item") == mat.getId()
								&& data.getString(i + ".Status").equals(
										"Selling")) {
							amount += data.getInt(i + ".Amount");
							if (config.getBoolean("Debug")) {
								log.info(String.format("[%s] A = " + amount,
										getDescription().getName()));
							}
						}
					}
					if (amount == 1) {
						p.sendMessage(String.format("[%s] There is only "
								+ amount + " " + args[2] + " left.",
								getDescription().getName()));
					} else if (amount == 0) {
						p.sendMessage(String.format("[%s] There is no "
								+ args[2] + " on the market.", getDescription()
								.getName()));
					} else {
						p.sendMessage(String.format("[%s] There are " + amount
								+ " " + args[2] + " left.", getDescription()
								.getName()));
					}
				} else {
					p.sendMessage(String
							.format("[%s] Please specify an item. /shop list amount <Item|ID>",
									getDescription().getName()));
				}
			}
			if (args[1].equalsIgnoreCase("common")) {
				// Most amount of items in ID, displays the top 10
				int i = data.getInt("Total");
				int printed_items = 0;
				int amount = -1;
				while (i > 0 && printed_items < 10) {
					if (data.getString(i + ".Status").equals("Selling")
							&& (data.getInt(i + ".Amount") < amount || amount < 0)) {
						amount = data.getInt(i + ".Amount");
						p.sendMessage(String.format(
								"[%s] A(n) total amount of "
										+ data.getInt(i + ".Amount")
										+ " "
										+ Material.getMaterial(data.getInt(i
												+ ".Item"))
										+ " has been added to the list.",
								getDescription().getName()));
						printed_items++;
					}
					i--;
				}
			}
			if (args[1].equalsIgnoreCase("expensive")) {
				// Most expensive items on the market by ID, displays top 10,
				// (also checks the cheapest of that item ID to be the most
				// expensive)
				int i = data.getInt("Total");
				int printed_items = 0;
				int price = -1;
				while (i > 0 && printed_items < 10) {
					if (data.getString(i + ".Status").equals("Selling")
							&& (data.getInt(i + ".Price") < price || price < 0)) {
						price = data.getInt(i + ".Price");
						p.sendMessage(String.format(
								"[%s] A(n) amount of "
										+ data.getInt(i + ".Amount")
										+ " "
										+ Material.getMaterial(data.getInt(i
												+ ".Item"))
										+ " has been added for "
										+ data.getInt(i + ".Price") + ".",
								getDescription().getName()));
						printed_items++;
					}
					i--;
				}
			}
			if (args[1].equalsIgnoreCase("recent")) {
				// Most recent items added to being sold
				int i = data.getInt("Total");
				int printed_items = 0;
				while (i > 0 && printed_items < 10) {
					if (data.getString(i + ".Status").equals("Selling")) {
						p.sendMessage(String.format(
								"[%s] A(n) amount of "
										+ data.getInt(i + ".Amount")
										+ " "
										+ Material.getMaterial(data.getInt(i
												+ ".Item"))
										+ " has been added for "
										+ data.getInt(i + ".Price") + ".",
								getDescription().getName()));
						printed_items++;
					}
					i--;
				}
			}
		} else if (args[0].equalsIgnoreCase("Buy")) {
			// Find cheapest price of item, buy that first until it is empty,
			// delete lines in yml if empty, and if buyer has more amount then
			// go for the next cheapest

			if (args.length > 4) {
				p.sendMessage(String.format("[%s] Syntax error.",
						getDescription().getName()));
				p.sendMessage(String
						.format("[%s] Please type /shop buy <Item|ID> <Amount> [confirm]",
								getDescription().getName()));
				return false;
			}
			if (args.length < 3) {
				p.sendMessage(String.format("[%s] Syntax error.",
						getDescription().getName()));
				p.sendMessage(String
						.format("[%s] Please type /shop buy <Item|ID> <Amount> [confirm]",
								getDescription().getName()));
				return false;
			}
			if (args.length > 3 && args[3].equalsIgnoreCase("confirm")) {
				confirm = true;
			} else {
				confirm = false;
			}

			Material mat = Material.matchMaterial(args[1]);
			int id = mat.getId();
			int amount = Integer.parseInt(args[2]);
			int totalprice = 0;
			int currentamount = 0;
			int j = 0;
			int i = 0;
			int lowestPrice;
			while (amount > 0) {
				if (config.getBoolean("Debug")) {
					log.info(String.format("[%s] New Loop", getDescription()
							.getName()));
				}
				lowestPrice = -1;
				for (i = 1; i <= data.getInt("Total"); i++) {
					if ((data.getInt(i + ".Item") == id)
							&& (data.getString(i + ".Status").equals("Selling"))
							&& (data.getString(i + ".Check").equals("T"))) {
						if (lowestPrice < 0
								|| data.getInt(i + ".Price") < lowestPrice) {
							lowestPrice = data.getInt(i + ".Price");
							j = i;
						}
					}
				}
				if (lowestPrice < 0) {
					p.sendMessage(String.format("[%s] There is not enough "
							+ mat + " on sale.", getDescription().getName()));
					return false;
				}
				if (config.getBoolean("Debug")) {
					log.info(String.format("[%s] J= "
							+ String.valueOf(j)
							+ " Name: "
							+ data.getString(j + ".Player", getDescription()
									.getName())));
				}
				int buyamount = (data.getInt(j + ".Amount") < amount) ? data
						.getInt(j + ".Amount") : amount;
				int price = data.getInt(j + ".Price") * buyamount;
				totalprice += price;
				currentamount += buyamount;
				amount -= buyamount;
				if (confirm) {
					EconomyResponse r1 = econ.depositPlayer(
							data.getString(j + ".Player"), price);
					if (r1.transactionSuccess()) {
						data.set(j + ".Amount",
								(data.getInt(j + ".Amount") - buyamount));
						if (data.getInt(j + ".Amount") == 0) {
							data.set(j + ".Status", "Sold");
						}
						saveYamls();
					}
				} else {
					data.set(j + ".Check", "F");
					saveYamls();
				}
			}
			if (confirm) {
				if (config.getBoolean("Debug")) {
					log.info(String.format("[%s] " + args[3], getDescription()
							.getName()));
				}
				EconomyResponse r = econ
						.withdrawPlayer(p.getName(), totalprice);
				if (r.transactionSuccess()) {
					ItemStack is = new ItemStack(mat, currentamount);
					p.getInventory().addItem(is);
					p.sendMessage(String.format("[%s] You have bought "
							+ currentamount + " of " + mat + " for "
							+ totalprice + ".", getDescription().getName()));
				}
			} else {
				p.sendMessage(String.format("[%s] You will pay " + totalprice
						+ " for " + currentamount + " of " + mat + ".",
						getDescription().getName()));
				for (i = 1; i <= data.getInt("Total"); i++) {
					if ((data.getString(i + ".Check").equals("F"))) {
						loadYamls();
						data.set(i + ".Check", "T");
						saveYamls();
					}
				}
			}
		} else if (args[0].equalsIgnoreCase("transaction")
				|| args[0].equalsIgnoreCase("transactions")) {
			boolean list = false;
			if (args.length == 1) {
				list = true;
			}
			if (args.length > 3) {
				p.sendMessage(String.format("[%s] Syntax error.",
						getDescription().getName()));
				p.sendMessage(String.format(
						"Please type /shop transaction[s] <list|cancel> [ID]",
						getDescription().getName()));
				return false;
			}
			if (args.length < 1) {
				p.sendMessage(String.format("[%s] Syntax error.",
						getDescription().getName()));
				p.sendMessage(String.format(
						"Please type /shop transaction[s] <list|cancel> [ID]",
						getDescription().getName()));
				return false;
			}
			if (args.length > 2 && args[1].equalsIgnoreCase("cancel")) {
				if (config.getBoolean("Debug")) {
					log.info(String.format(
							"[%s]P = " + ((Player) sender).getDisplayName()
									+ " LP = "
									+ data.getString(args[2] + ".Player"),
							getDescription().getName()));
				}
				try {
					Integer.parseInt(args[2]);
					if (data.getString(args[2] + ".Player").equals(
							((Player) sender).getDisplayName())
							&& data.getString(args[2] + ".Status").equals(
									"Selling")) {
						data.set(args[2] + ".Status", "Cancelled");
						saveYamls();
						ItemStack is = new ItemStack(data.getInt(args[2]
								+ ".Item"), data.getInt(args[2] + ".Amount"));
						p.getInventory().addItem(is);
						p.sendMessage(String.format(
								"[%s] You have cancelled transaction ID #"
										+ args[2] + ".", getDescription()
										.getName()));
						return false;
					} else {
						p.sendMessage(String.format(
								"[%s] You cannot cancel transaction ID #"
										+ args[2] + ".", getDescription()
										.getName()));
						return false;
					}
				} catch (Exception e) {
					p.sendMessage(String.format(
							"[%s] Syntax error ID must be an integer. ",
							getDescription().getName()));
					p.sendMessage(String.format(
							"[%s] /shop transaction[s] cancel <ID>",
							getDescription().getName()));
					return false;
				}
			} else if (args.length > 1 && args[1].equalsIgnoreCase("list")
					|| list == true) {
				int i = data.getInt("Total");
				int printed_items = 0;
				p.sendMessage(String.format("[%s] ID Item Amount Price/Unit "
						+ "Total Price Status", getDescription().getName()));
				while (i > 0 && printed_items < 10) {
					if (config.getBoolean("Debug")) {
						log.info(String.format(
								"[%s]P = " + ((Player) sender).getDisplayName()
										+ " LP = "
										+ data.getString(i + ".Player")
										+ " S = "
										+ data.getString(i + ".Status"),
								getDescription().getName()));
					}
					if (data.getString(i + ".Player").equals(
							((Player) sender).getDisplayName())) {
						p.sendMessage(String.format(
								"[%s] "
										+ i
										+ " "
										+ Material.getMaterial(data.getInt(i
												+ ".Item")) + " "
										+ data.getInt(i + ".TotalAmount") + " "
										+ data.getInt(i + ".Price") + " "
										+ data.getInt(i + ".Price")
										* data.getInt(i + ".TotalAmount") + " "
										+ data.getString(i + ".Status"),
								getDescription().getName()));
						printed_items++;
					}
					i--;
				}
			} else {
				p.sendMessage(String.format(
						"[%s] Syntax error. Please type /shop transaction[s].",
						getDescription().getName()));
			}
			return true;
		}
		return false;
	}
}
