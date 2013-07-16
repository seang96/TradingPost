package com.spgrn.tradingpost;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
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
	File usersFile;
	FileConfiguration config;
	FileConfiguration data;
	FileConfiguration users;

	@Override
	public void onEnable() {
		configFile = new File(getDataFolder(), "config.yml");
		dataFile = new File(getDataFolder(), "data.yml");
		usersFile = new File(getDataFolder(), "users.yml");
		try {
			firstRun();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!setupEconomy()) {
			log.severe(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "- Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		} else {
			log.info(String.format("[%s] Vault dependency found!",
					getDescription().getName()));
		}
		setupPermissions();
		config = new YamlConfiguration();
		data = new YamlConfiguration();
		users = new YamlConfiguration();
		loadYamls();
		if (config.getBoolean("Debug", true)) {
			log.info(String.format("[%s] You are in debug mode!",
					getDescription().getName()));
		}

	}

	@Override
	public void onDisable() {
		log.info(String.format(" Disabled Version %s", getDescription()
				.getName(), getDescription().getVersion()));
	}

	public void saveYamls() {
		try {
			config.save(configFile);
			data.save(dataFile);
			users.save(usersFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadYamls() {
		try {
			config.load(configFile);
			data.load(dataFile);
			users.save(usersFile);
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
		if (!usersFile.exists()) {
			usersFile.getParentFile().mkdirs();
			copy(getResource("users.yml"), usersFile);
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
		if (args.length < 1) {
			sender.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please type /shop <sell|buy|list|transaction[s]|setting[s]>", getDescription().getName()));
			return false;
		}
		if (args[0].equalsIgnoreCase("Sell")) {
			if (args.length == 1) {
				sender.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please type /shop sell <Item|ID> <Amount> <Price> [confirm]", getDescription().getName()));
				return false;
			}
			if (args.length > 5) {
				sender.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
				sender.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please type /shop sell <Item|ID> <Amount> <Price> [confirm]", getDescription().getName()));
				return false;
			}
			if (args.length < 4) {
				sender.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
				sender.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please type /shop sell <Item|ID> <Amount> <Price> [confirm]", getDescription().getName()));
				return false;
			}
			if (args.length > 4 && args[4].equalsIgnoreCase("confirm")) {
				confirm = true;
			} else {
				confirm = false;
				if (args.length >= 5) {
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Error: Please enter a valid item name. If you used a space try replacing with _ (underscore).", getDescription().getName()));
					return false;
				}
			}
			if (config.getBoolean("AutoConfirm")) {
				if (!users.getBoolean(((Player) sender).getDisplayName()
						+ ".Confirm")) {
					confirm = true;
				}
			} else {
				if (users.getBoolean(((Player) sender).getDisplayName()
						+ ".Confirm")) {
					confirm = true;
				}
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
			if (Material.matchMaterial(args[1]) != null) {
				int id = mat.getId();
				ItemStack is = new ItemStack(mat, amount);
				if (confirm) {
					if (p.getInventory().contains(mat, amount)) {
						p.getInventory().removeItem(is);
						EconomyResponse r = econ.withdrawPlayer(p.getName(),
								tax);
						if (r.transactionSuccess()) {
							IDcount++;
							p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "You have added " + args[2] + " " + args[1] + " for " + args[3] + " to the " + "market. You have paid " + String.valueOf(tax) + " for taxes.", getDescription() .getName()));
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
							p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "An error occured: %s", getDescription() .getName(), r.errorMessage));
						}
					}
					else {
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "You do not have " + args[3] + " " + args[2] + " please try selling at a lower amount.", getDescription() .getName()));
					}
				} 
					else {
					if (config.getBoolean("Debug")) {
						log.info(String.format(" t = " + t1, getDescription()
								.getName()));
					}
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "You will sell " + args[2] + " " + args[1] + ". The total price will be " + String.valueOf(totalprice) + ". With the cost of " + String.valueOf(tax) + " for taxes.", getDescription().getName()));
				}
			} else {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Error: Please enter a valid item name. If you used a space try replacing with _ (underscore).", getDescription().getName()));
			}
		} else if (args[0].equalsIgnoreCase("List")) {
			if (args.length == 1) {
				sender.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please type /shop list <amount|common|expensive|recent> [Item|ID]", getDescription().getName()));
				return false;
			}
			if (args.length > 3) {
				sender.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
				sender.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please type /shop list <amount|common|expensive|recent> [Item|ID]", getDescription().getName()));
				return false;
			}
			if (args.length < 2) {
				sender.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
				sender.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please type /shop list <amount|common|expensive|recent> [Item|ID]", getDescription().getName()));
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
								log.info(String.format(" A = " + amount,
										getDescription().getName()));
							}
						}
					}
					if (amount == 1) {
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "There is only " + amount + " " + args[2] + " left.", getDescription().getName()));
					} else if (amount == 0) {
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "There is no " + args[2] + " on the market.", getDescription().getName()));
					} else {
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "There are " + amount + " " + args[2] + " left.", getDescription().getName()));
					}
				} else {
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please specify an item. /shop list amount <Item|ID>", getDescription().getName()));
				}
			}
			if (args[1].equalsIgnoreCase("common")) {
				// Most amount of items in ID, displays the top 10
				int i = data.getInt("Total");
				int printed_items = 0;
				int amount = -1;
				int id = -1;
				while (i > 0 && printed_items < 10) {
					if (data.getString(i + ".Status").equals("Selling")
							&& (data.getInt(i + ".Amount") < amount || amount < 0)) {
						id = data.getInt(i + ".Item");
						for (int j = 1; j <= data.getInt("Total"); j++) {
							if (id == data.getInt(i + ".Item")) {
								amount += data.getInt(j + ".Amount");
							}
							else {
								amount = data.getInt(i + ".Amount");
							}
						}
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "A(n) total amount of " + data.getInt(i + ".Amount") + " " + Material.getMaterial(data.getInt(i + ".Item")) + " has been added to the list.", getDescription().getName()));
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
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "A(n) amount of " + data.getInt(i + ".Amount") + " " + Material.getMaterial(data.getInt(i + ".Item")) + " has been added for " + data.getInt(i + ".Price") + ".",
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
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "A(n) amount of " + data.getInt(i + ".Amount") + " " + Material.getMaterial(data.getInt(i + ".Item")) + " has been added for " + data.getInt(i + ".Price") + ".", getDescription().getName()));
						printed_items++;
					}
					i--;
				}
			}
		} else if (args[0].equalsIgnoreCase("Buy")) {
			// Find cheapest price of item, buy that first until it is empty,
			// delete lines in yml if empty, and if buyer has more amount then
			// go for the next cheapest
			if (args.length == 1) {
				sender.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please type /shop buy <Item|ID> <Amount> [confirm]", getDescription().getName()));
				return false;
			}
			if (args.length > 4) {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please type /shop buy <Item|ID> <Amount> [confirm]", getDescription().getName()));
				return false;
			}
			if (args.length < 3) {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please type /shop buy <Item|ID> <Amount> [confirm]", getDescription().getName()));
				return false;
			}
			if (args.length > 3 && args[3].equalsIgnoreCase("confirm")) {
				confirm = true;
			} else {
				confirm = false;
			}
			if (users.getBoolean(((Player) sender).getDisplayName()
					+ ".Confirm")) {
				confirm = true;
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
					log.info(String.format(" New Loop", getDescription()
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
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "There is not enough " + mat + " on sale.", getDescription().getName()));
					return false;
				}
				if (config.getBoolean("Debug")) {
					log.info(String.format(" J= "
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
				EconomyResponse r = econ
						.withdrawPlayer(p.getName(), totalprice);
				if (r.transactionSuccess()) {
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "You have bought " + currentamount + " of " + mat + " for " + totalprice + ".", getDescription() .getName()));
					ItemStack is = new ItemStack(mat, currentamount);
					HashMap<Integer, ItemStack> leftOver = new HashMap<Integer, ItemStack>();
                    leftOver.putAll((p.getInventory().addItem(is)));
                    if (!leftOver.isEmpty()) {
                        Location loc = p.getLocation();
                        p.getWorld().dropItem(loc, new ItemStack(Material.getMaterial(leftOver.get(0).getTypeId()), leftOver.get(0).getAmount()));
                        p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Your inventory is full. Rest of the items have been dropped.", getDescription().getName()));
                    }

				}
			} else {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "You will pay " + totalprice + " for " + currentamount + " of " + mat + ".", getDescription() .getName()));
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
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
				p.sendMessage(String.format("Please type /shop transaction[s] <list|cancel> [ID]", getDescription().getName()));
				return false;
			}
			if (args.length < 1) {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
				p.sendMessage(String.format("Please type /shop transaction[s] <list|cancel> [ID]", getDescription().getName()));
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
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "You have cancelled transaction ID #" + args[2] + ".", getDescription().getName()));
						return false;
					} else {
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "You cannot cancel transaction ID #" + args[2] + ".", getDescription().getName()));
						return false;
					}
				} catch (Exception e) {
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error ID must be an integer. ", getDescription().getName()));
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "/shop transaction[s] cancel <ID>", getDescription().getName()));
					return false;
				}
			} else if (args.length > 1 && args[1].equalsIgnoreCase("list")
					|| list == true) {
				int i = data.getInt("Total");
				int printed_items = 0;
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "ID " + ChatColor.AQUA + "Item " + ChatColor.GREEN + "Amount " + ChatColor.DARK_PURPLE + "Price/Unit " + ChatColor.DARK_AQUA + "Total Price " + ChatColor.RED + "Status", getDescription().getName()));
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
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + i + " " + ChatColor.AQUA + Material.getMaterial(data.getInt(i + ".Item")) + " " + ChatColor.GREEN + data.getInt(i + ".TotalAmount") + " " + ChatColor.DARK_PURPLE + data.getInt(i + ".Price") + " " + ChatColor.DARK_AQUA + data.getInt(i + ".Price") * data.getInt(i + ".TotalAmount") + " " + ChatColor.RED +data.getString(i + ".Status"), getDescription().getName()));
						printed_items++;
					}
					i--;
				}
			} else {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error. Please type /shop transaction[s].", getDescription().getName()));
			}
		} else if (args[0].equalsIgnoreCase("setting")
				|| args[0].equalsIgnoreCase("settings")) {
			if (args.length > 3) {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
				p.sendMessage(String.format("Please type /shop setting[s] <autoconfirm> [true|false]", getDescription().getName()));
				return false;
			}
			if (args.length < 1) {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
				p.sendMessage(String.format("Please type /shop setting[s] <autoconfirm> [true|false]", getDescription().getName()));
				return false;
			}
			if (args[1].equalsIgnoreCase("autoconfirm")) {
				boolean toggle = false;
				if (args.length == 2) {
					toggle = true;
					if (toggle) {
						if (users.getBoolean(((Player) sender).getDisplayName()
								+ ".Confirm")) {
							users.set(((Player) sender).getDisplayName()
									+ ".Confirm", null);
							saveYamls();
							if (config.getBoolean("AutoConfirm")) {
								p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been enabled.", getDescription().getName()));
							} else {
								p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been disabled.", getDescription().getName()));
							}
						} else {
							users.set(((Player) sender).getDisplayName()
									+ ".Confirm", true);
							saveYamls();
							if (config.getBoolean("AutoConfirm")) {
								p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been disabled.", getDescription().getName()));
							} else {
								p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been enabled.", getDescription().getName()));
							}
						}
					}
				}
				if (args.length == 3) {
					if (args[2].equalsIgnoreCase("true")
							|| args[2].equalsIgnoreCase("enable")) {
						users.set(((Player) sender).getDisplayName()
								+ ".Confirm", true);
						saveYamls();
						if (config.getBoolean("AutoConfirm")) {
							p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been disabled.", getDescription().getName()));
						} else {
							p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been enabled.", getDescription().getName()));
						}
					} else if (args[2].equalsIgnoreCase("false")
							| args[2].equalsIgnoreCase("disable")) {
						users.set(((Player) sender).getDisplayName()
								+ ".Confirm", null);
						saveYamls();
						if (config.getBoolean("AutoConfirm")) {
							p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been enabled.", getDescription().getName()));
						} else {
							p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been disabled.", getDescription().getName()));
						}
					}
				}
			}
			return true;
		}
		return false;
	}
}
