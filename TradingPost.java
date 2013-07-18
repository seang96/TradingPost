package com.spgrn.tradingpost;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
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

	public class PlayerListener implements Listener {
		@EventHandler(priority = EventPriority.LOW)
		public void onPlayerJoin(PlayerJoinEvent event) {
			Player p = event.getPlayer();
			for (int i = 1; i <= data.getInt("Total"); i++) {
				if (data.getString("Users." + p + ".Alert." + i) != null) {
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Transaction ID " + ChatColor.DARK_AQUA + i + ChatColor.GOLD + " has been sold. You have gained " + ChatColor.DARK_AQUA + data.getInt("Users." + p + ".Alert." + i + ".Price") + ChatColor.GOLD + ".", getDescription().getName()));
					data.set("Users." + p + ".Alert." + i, null);
				}
			}
		}
	}
	
	public boolean onCommand(CommandSender sender, Command command,
			String commandLabel, String[] args) {
		if (!command.getLabel().equals("shop")) {
			return false;
		}
		Player p = (Player) sender;
		boolean confirm;
		if (args.length < 1) {
			p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please type /shop <sell|buy|list|transaction[s]|setting[s]>", getDescription().getName()));
			return false;
		}
		if (args[0].equalsIgnoreCase("Sell")) {
			if (args.length == 1) {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please type /shop sell <Item|ID> <Amount> <Price> [confirm]", getDescription().getName()));
				return false;
			}
			if (args.length > 5 || args.length < 4) {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
				p.sendMessage(String.format(ChatColor.GOLD + "Please type /shop sell <Item|ID> <Amount> <Price> [confirm]", getDescription().getName()));
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
//			long IDcount = data.getInt("Total");
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
					if (config.getBoolean("UseMaxTransactions") && data.getInt("Total") == config.getInt("MaxTransactions")) {
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "The global limit of selling transactions have been reached. Wait for the current transactions to be canceled or sold.", getDescription().getName()));
					}
					else {
						if (p.getInventory().contains(mat, amount)) {
							p.getInventory().removeItem(is);
							EconomyResponse r = econ.withdrawPlayer(p.getName(),
									tax);
							if (r.transactionSuccess()) {
//								IDcount++;
								p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "You have added " + ChatColor.DARK_AQUA + args[2] + " " + args[1] + ChatColor.GOLD + " for " + ChatColor.DARK_AQUA + args[3] + ChatColor.GOLD + " to the market. You have paid " + ChatColor.DARK_AQUA + String.valueOf(tax) + ChatColor.GOLD + " for taxes.", getDescription() .getName()));
								for(int i = 1; data.getInt("Total") == 0 || i <= (data.getInt("Total") + 1); i++) {
									if (config.getBoolean("Debug")) {
										log.info(String.format(" t = " + data.getInt("Total") + " i = " + i, getDescription()
												.getName()));
									}
									if (data.getString("Transactions." + i) == null) {
										loadYamls();
										data.set("Transactions." + i + ".Player",
										((Player) sender).getDisplayName());
										data.set("Transactions." + i + ".Item", id);
										data.set("Transactions." + i + ".TotalAmount", amount);
										data.set("Transactions." + i + ".Amount", amount);
										data.set("Transactions." + i + ".Price", price);
										data.set("Transactions." + i + ".Status", "Selling");
										data.set("Transactions." + i + ".Check", "T");
										data.set("Total", i);
										saveYamls();
										return false;
									}
									if (data.getString("Transactions." + i + ".Status").equals("Sold") || data.getString("Transactions." + i + ".Status").equals("Cancelled")) {
										loadYamls();
										data.set("Transactions." + i + ".Player",
										((Player) sender).getDisplayName());
										data.set("Transactions." + i + ".Item", id);
										data.set("Transactions." + i + ".TotalAmount", amount);
										data.set("Transactions." + i + ".Amount", amount);
										data.set("Transactions." + i + ".Price", price);
										data.set("Transactions." + i + ".Status", "Selling");
										data.set("Transactions." + i + ".Check", "T");
										saveYamls();
										return false;
									}
								}
							} else {
								p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "An error occured: %s", getDescription() .getName(), r.errorMessage));
							}
						}
						else {
							p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "You do not have " + ChatColor.DARK_AQUA + args[3] + " " + args[2] + ChatColor.GOLD + " please try selling at a lower amount.", getDescription() .getName()));
						}
					}
				}
				else {
					if (config.getBoolean("Debug")) {
						log.info(String.format(" t = " + t1, getDescription()
								.getName()));
					}
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "You will sell " + ChatColor.DARK_AQUA + args[2] + " " + args[1] + ChatColor.GOLD + ". The total price will be " + ChatColor.DARK_AQUA + String.valueOf(totalprice) + ChatColor.GOLD + ". You wil spend " + ChatColor.DARK_AQUA + String.valueOf(tax) + ChatColor.GOLD + " for taxes.", getDescription().getName()));
				}
			} else {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Error: Please enter a valid item name. If you used a space try replacing with _ (underscore).", getDescription().getName()));
			}
		} else if (args[0].equalsIgnoreCase("List")) {
			if (args.length == 1) {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please type /shop list <amount|common|expensive|recent> [Item|ID]", getDescription().getName()));
				return false;
			}
			if (args.length > 3 || args.length < 2) {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
				p.sendMessage(String.format(ChatColor.GOLD + "Please type /shop list <amount|common|expensive|recent> [Item|ID]", getDescription().getName()));
				return false;
			}
			if (args[1].equalsIgnoreCase("amount")) {
				if (args.length > 2 && Material.matchMaterial(args[2]) != null) {
					Material mat = Material.matchMaterial(args[2]);
					int amount = 0;
					for (int i = 1; i <= data.getInt("Total"); i++) {
						if (data.getInt("Transactions." + i + ".Item") == mat.getId()
								&& data.getString("Transactions." + i + ".Status").equals(
										"Selling")) {
							amount += data.getInt("Transactions." + i + ".Amount");
							if (config.getBoolean("Debug")) {
								log.info(String.format(" A = " + amount,
										getDescription().getName()));
							}
						}
					}
					if (amount == 1) {
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "There is only " + ChatColor.DARK_AQUA + amount + " " + args[2] + ChatColor.GOLD + " left.", getDescription().getName()));
					} else if (amount == 0) {
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "There is no " + ChatColor.DARK_AQUA + args[2] + ChatColor.GOLD + " on the market.", getDescription().getName()));
					} else {
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "There are " + ChatColor.DARK_AQUA + amount + " " + args[2] + ChatColor.GOLD + " left.", getDescription().getName()));
					}
				} else {
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please specify a item. /shop list amount <Item|ID>", getDescription().getName()));
				}
			}
			if (args[1].equalsIgnoreCase("common")) {
				List<Integer> ids = new ArrayList<Integer>();
				// Most amount of items in ID, displays the top 10
				int i = data.getInt("Total");
				int printed_items = 0;
				int amount = -1;
				int highestamount = -1;
				int k = 0;
				while (i > 0 && printed_items < 10) {
					highestamount = -1;
					amount = -1;
					for (int j = 1; j <= data.getInt("Total"); j++) {
						if (config.getBoolean("Debug")) {
							log.info(String.format(" j = " + j,
									getDescription().getName()));
						}
						if (!ids.contains(data.getInt("Transactions." + j + ".Item")) && data.getString("Transactions." + j + ".Status").equals("Selling")) {
							if (highestamount < 0 || data.getInt("Transactions." + j + ".Amount") > highestamount) {
								highestamount= data.getInt("Transactions." + j + ".Amount");
								k = j;
								if (config.getBoolean("Debug")) {
									log.info(String.format("true h = " + highestamount + " k= " + k,
											getDescription().getName()));
								}
							}
						}
					}
					if (!ids.contains(data.getInt("Transactions." + k + ".Item")) && data.getString("Transactions." + k + ".Status").equals("Selling")) {
						for (int l = 1; l <= data.getInt("Total"); l++) {
							if (config.getBoolean("Debug")) {
								log.info(String.format(" k = " + k + " l = " + l,
									getDescription().getName()));
							}
							if (data.getInt("Transactions." + k + ".Item") == data.getInt("Transactions." + l + ".Item")) {
								if (amount == -1) {
									amount = data.getInt("Transactions."  + l + ".Amount");
								}
								else {
									amount += data.getInt("Transactions." + l + ".Amount");
								}
								if (config.getBoolean("Debug")) {
									log.info(String.format(" a = " + amount + " l = " + l,
										getDescription().getName()));
								}
							}
						}
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "A total amount of " + ChatColor.DARK_AQUA + amount + " " + Material.getMaterial(data.getInt("Transactions." + k + ".Item")) + ChatColor.GOLD + " has been added to the list.", getDescription().getName()));
						printed_items++;
						ids.add(data.getInt("Transactions." + k + ".Item"));
						i--;
					}
					else {
						printed_items = 10;
					}
				}
			}
			if (args[1].equalsIgnoreCase("expensive")) {
				// Most expensive items on the market by ID, displays top 10,
				// (also checks the cheapest of that item ID to be the most
				// expensive)
				List<Integer> ids = new ArrayList<Integer>();
				int i = data.getInt("Total");
				int printed_items = 0;
				int price = -1;
				int highestprice = -1;
				int k = 0;
				boolean loop = true;
				while (i > 0 && printed_items < 10) {
					for (int j = 1; j <= data.getInt("Total"); j++) {
						if (config.getBoolean("Debug")) {
							log.info(String.format(" j = " + j,
									getDescription().getName()));
						}
						if (!ids.contains(data.getInt("Transactions." + j + ".Item")) && data.getString("Transactions." + j + ".Status").equals("Selling")) {
							if ((highestprice < 0 || data.getInt("Transactions." + j + ".Price") < highestprice)  && (price < 0 || price > data.getInt("Transactions." + j + ".Price"))){
								for (k = 1; k <= data.getInt("Total"); k++) {
									if (highestprice < 0 || data.getInt("Transactions." + k + ".Price") > highestprice && loop) {
										highestprice = data.getInt("Transactions." + k + ".Price");
										j = k;
									}
								}
								highestprice = data.getInt("Transactions." + j + ".Price");
								price = data.getInt("Transactions."  + j + ".Price");
								if (loop) {
									j = 1;
								}
								k = j;
								loop = false;
								if (config.getBoolean("Debug")) {
									log.info(String.format("true h = " + highestprice + " k= " + k,
											getDescription().getName()));
								}
							}
						}
					}
					if (!ids.contains(data.getInt("Transactions." + k + ".Item")) && data.getString("Transactions." + k + ".Status").equals("Selling")) {
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "At a cost of " + ChatColor.DARK_AQUA + price + ChatColor.GOLD + " per unit is " + ChatColor.DARK_AQUA + Material.getMaterial(data.getInt("Transactions." + k + ".Item")) + ChatColor.GOLD + ".", getDescription().getName()));
						printed_items++;
						ids.add(data.getInt("Transactions." + k + ".Item"));
						highestprice = -1;
						price = -1;
						loop = true;
						i--;
					}
					else {
						printed_items = 10;
					}
				}
			}
			if (args[1].equalsIgnoreCase("recent")) {
				// Most recent items added to being sold
				int i = data.getInt("Total");
				int printed_items = 0;
				while (i > 0 && printed_items < 10) {
					if (data.getString("Transactions." + i + ".Status").equals("Selling")) {
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "A(n) amount of " + ChatColor.DARK_AQUA + data.getInt("Transactions." + i + ".Amount") + " " + Material.getMaterial(data.getInt("Transactions." + i + ".Item")) + ChatColor.GOLD + " has been added for " + ChatColor.DARK_AQUA + data.getInt("Transactions." + i + ".Price") + ChatColor.GOLD + ".", getDescription().getName()));
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
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Please type /shop buy <Item|ID> <Amount> [confirm]", getDescription().getName()));
				return false;
			}
			if (args.length > 4 || args.length < 3) {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
				p.sendMessage(String.format(ChatColor.GOLD + "Please type /shop buy <Item|ID> <Amount> [confirm]", getDescription().getName()));
				return false;
			}
			if (args.length > 3 && args[3].equalsIgnoreCase("confirm")) {
				confirm = true;
			} else {
				confirm = false;
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
			Material mat = Material.matchMaterial(args[1]);
			int id = mat.getId();
			int amount = Integer.parseInt(args[2]);
			int totalamount = amount;
			int totalprice = 0;
			int currentamount = 0;
			int j = 0;
			int i = 0;
			int lowestPrice;
			int buy = 0;
			int buysuccess = 0;
			if(confirm) {
				ItemStack is = new ItemStack(mat, totalamount);
				HashMap<Integer, ItemStack> leftOver = new HashMap<Integer, ItemStack>();
				leftOver.putAll((p.getInventory().addItem(is)));
				if (!leftOver.isEmpty()) {
					totalamount -= leftOver.get(0).getAmount();
					ItemStack remove = new ItemStack(mat, totalamount);
					p.getInventory().removeItem(remove);
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Your inventory is full.", getDescription().getName()));
					return false;
				}
				else {
						p.getInventory().remove(is);
					}
			}
			while (amount > 0) {
				if (config.getBoolean("Debug")) {
					log.info(String.format(" New Loop", getDescription()
							.getName()));
				}
				lowestPrice = -1;
				for (i = 1; i <= data.getInt("Total"); i++) {
					if ((data.getInt("Transactions." + i + ".Item") == id)
							&& (data.getString("Transactions." + i + ".Status").equals("Selling"))
							&& (data.getString("Transactions." + i + ".Check").equals("T") 
							/*&& (!data.getString("Transactions." + i + ".Player").equals(((Player) sender).getDisplayName()))*/)) {
						if (lowestPrice < 0
								|| data.getInt("Transactions." + i + ".Price") < lowestPrice) {
							lowestPrice = data.getInt("Transactions." + i + ".Price");
							j = i;
						}
						if (config.getBoolean("Debug")) {
							log.info(String.format(" p= " + p + " Name: " + data.getString("Transactions." + i + ".Player") + " i = " + i, getDescription().getName()));
						}
					}
				}
				if (lowestPrice < 0) {
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "There is either not enough " + ChatColor.DARK_AQUA + mat + ChatColor.GOLD + " on sale or you are selling all of the " + ChatColor.DARK_AQUA + mat + ChatColor.GOLD + ".", getDescription().getName()));
					return false;
				}
				int buyamount = (data.getInt("Transactions." + j + ".Amount") < amount) ? data
						.getInt("Transactions." + j + ".Amount") : amount;
				int price = data.getInt("Transactions." + j + ".Price") * buyamount;
				totalprice += price;
				currentamount += buyamount;
				amount -= buyamount;
				if (config.getBoolean("Debug")) {
					log.info(String.format(" J= " + String.valueOf(j) + " Name: " + data.getString("Transactions." + j + ".Player") + " b = " + buy + " b2 = " + buysuccess + " amount = " + amount + " ba = " + buyamount, getDescription().getName()));
				}
				if (confirm) {
					buy++;
					EconomyResponse r = econ.withdrawPlayer(p.getName(), price);
					EconomyResponse r1 = econ.depositPlayer(
							data.getString("Transactions." + j + ".Player"), price);
					if (r.transactionSuccess() && r1.transactionSuccess()) {
						buysuccess++;
						data.set("Transactions." + j + ".Amount",
						(data.getInt("Transactions." + j + ".Amount") - buyamount));
						if (data.getInt("Transactions." + j + ".Amount") == 0) {
							data.set("Transactions." + j + ".Status", "Sold");
							Player player = Bukkit.getPlayerExact(data.getString("Transactions." + j + ".Player"));
							if (player == null) {
								data.set("Users." + data.getString("Transactions." + j + ".Player") + ".Alert." + j, j);
								data.set("Users." + data.getString("Transactions." + j + ".Player") + ".Alert." + j + ".Price", data.getInt("Transactions." + j + ".Price"));
							}
							else {
								player.sendMessage("[" + ChatColor.LIGHT_PURPLE + "TradingPost" + ChatColor.RESET + "] " + ChatColor.GOLD + "Transaction ID " + ChatColor.DARK_AQUA + j + ChatColor.GOLD + " has been sold and you have received " + ChatColor.DARK_AQUA + data.getInt("Transactions." + j + ".TotalAmount") * data.getInt("Transactions." + j + ".Price") + ChatColor.GOLD + ".");
							}
						}
						saveYamls();
					}
				} else {
					data.set("Transactions." + j + ".Check", "F");
					saveYamls();
				}
			}
			if (confirm) {
				if (buy == buysuccess) {
					ItemStack is = new ItemStack(mat, totalamount);
					p.getInventory().addItem(is);
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "You have bought " + ChatColor.DARK_AQUA + currentamount + ChatColor.GOLD + " of " + ChatColor.DARK_AQUA + mat + ChatColor.GOLD + " for " + ChatColor.DARK_AQUA + totalprice + ChatColor.GOLD + ".", getDescription() .getName()));
				}
			} else {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "You will pay " + ChatColor.DARK_AQUA + totalprice + ChatColor.GOLD + " for " + ChatColor.DARK_AQUA + currentamount + ChatColor.GOLD + " of " + ChatColor.DARK_AQUA + mat + ChatColor.GOLD + ".", getDescription() .getName()));
				for (i = 1; i <= data.getInt("Total"); i++) {
					if ((data.getString("Transactions." + i + ".Check").equals("F"))) {
						loadYamls();
						data.set("Transactions." + i + ".Check", "T");
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
			if (args.length > 3 || args.length < 1) {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
				p.sendMessage(String.format(ChatColor.GOLD + "Please type /shop transaction[s] <list|cancel> [ID]", getDescription().getName()));
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
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "You have cancelled transaction ID #" + ChatColor.DARK_AQUA + args[2] + ChatColor.GOLD + ".", getDescription().getName()));
						return false;
					} else {
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "You cannot cancel transaction ID #" + ChatColor.DARK_AQUA + args[2] + ChatColor.GOLD + ".", getDescription().getName()));
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
				while (i > 0 && printed_items < 10) {
					if (config.getBoolean("Debug")) {
						log.info(String.format(
								"[%s]P = " + ((Player) sender).getDisplayName()
										+ " LP = "
										+ data.getString("Transactions." + i + ".Player")
										+ " S = "
										+ data.getString("Transactions." + i + ".Status"),
								getDescription().getName()));
					}
					if (data.getString("Transactions." + i + ".Player").equals(
							((Player) sender).getDisplayName())) {
						if (printed_items == 0) {
							p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "ID " + ChatColor.AQUA + "Item " + ChatColor.GREEN + "Amount " + ChatColor.DARK_PURPLE + "Price/Unit " + ChatColor.DARK_AQUA + "Total Price " + ChatColor.RED + "Status", getDescription().getName()));
						}
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + i + " " + ChatColor.AQUA + Material.getMaterial(data.getInt("Transactions." + i + ".Item")) + " " + ChatColor.GREEN + data.getInt("Transactions." + i + ".TotalAmount") + " " + ChatColor.DARK_PURPLE + data.getInt("Transactions." + i + ".Price") + " " + ChatColor.DARK_AQUA + data.getInt("Transactions." + i + ".Price") * data.getInt("Transactions." + i + ".TotalAmount") + " " + ChatColor.RED +data.getString("Transactions." + i + ".Status"), getDescription().getName()));
						printed_items++;
					}
					else if (printed_items == 0 && i == 0){
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "You do not have any transactions on sale.", getDescription().getName()));
						return false;
					}
					i--;
				}
			} else {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error. Please type /shop transaction[s].", getDescription().getName()));
			}
		} else if (args[0].equalsIgnoreCase("setting")
				|| args[0].equalsIgnoreCase("settings")) {
			if (args.length > 3 || args.length < 1) {
				p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
				p.sendMessage(String.format(ChatColor.GOLD + "Please type /shop setting[s] <autoconfirm> [true|false]", getDescription().getName()));
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
								p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been " +ChatColor.DARK_AQUA + "enabled" + ChatColor.GOLD + ".", getDescription().getName()));
							} else {
								p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been  " +ChatColor.DARK_AQUA + "disabled" + ChatColor.GOLD + ".", getDescription().getName()));
							}
						}
						else {
							users.set(((Player) sender).getDisplayName()
									+ ".Confirm", true);
							saveYamls();
							if (config.getBoolean("AutoConfirm")) {
								p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been " +ChatColor.DARK_AQUA + "disabled" + ChatColor.GOLD + ".", getDescription().getName()));
							} else {
								p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been " +ChatColor.DARK_AQUA + "enabled" + ChatColor.GOLD + ".", getDescription().getName()));
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
							p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been " +ChatColor.DARK_AQUA + "disabled" + ChatColor.GOLD + ".", getDescription().getName()));
						} else {
							p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been " +ChatColor.DARK_AQUA + "enabled" + ChatColor.GOLD + ".", getDescription().getName()));
						}
					} else if (args[2].equalsIgnoreCase("false")
							| args[2].equalsIgnoreCase("disable")) {
						users.set(((Player) sender).getDisplayName()
								+ ".Confirm", null);
						saveYamls();
						if (config.getBoolean("AutoConfirm")) {
							p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been " +ChatColor.DARK_AQUA + "enabled" + ChatColor.GOLD + ".", getDescription().getName()));
						} else {
							p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Autoconfirm has been " +ChatColor.DARK_AQUA + "disabled" + ChatColor.GOLD + ".", getDescription().getName()));
						}
					}
				}
			}
		}
			else if (args[0].equalsIgnoreCase("item")) {
				boolean held = false;
				if (args.length == 1) {
					held = true;
				}
				if (args.length > 3 || args.length < 1) {
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Syntax error.", getDescription().getName()));
					p.sendMessage(String.format(ChatColor.GOLD + "Please type /shop item [id|name> [ItemID|ItemName]", getDescription().getName()));
					return false;
				}
				if (args.length > 2 && args[1].equalsIgnoreCase("id")) {
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "The id of " + ChatColor.DARK_AQUA + Material.matchMaterial(args[2]) + ChatColor.GOLD + " is " + ChatColor.DARK_AQUA + Material.matchMaterial(args[2]).getId() + ChatColor.GOLD + ".", getDescription().getName()));
					return false;
				}
				else if (args.length > 2 && args[1].equalsIgnoreCase("name")) {
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "The name of " + ChatColor.DARK_AQUA + Material.matchMaterial(args[2]) + ChatColor.GOLD + " is " + ChatColor.DARK_AQUA + Material.matchMaterial(args[2]).getId() + ChatColor.GOLD + ".", getDescription().getName()));
					return false;
				}
				else if (args.length == 1 || held) {
					ItemStack i = p.getItemInHand();
					Material mat = Material.getMaterial(i.getTypeId());
					int id = mat.getId();
					p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "Held ID is " + ChatColor.DARK_AQUA + id + " §6with the name " + ChatColor.DARK_AQUA + Material.getMaterial(i.getTypeId()) + ChatColor.GOLD + ".", getDescription().getName()));
					if (users.getBoolean(((Player) sender).getDisplayName() + ".ItemNotice")) {
						return false;
					}
					else {
						p.sendMessage(String.format("[" + ChatColor.LIGHT_PURPLE + "%s" + ChatColor.RESET + "] " + ChatColor.GOLD + "One-time Notice: You can also use /shop item <id|name> <ItemID|ItemName> to get a specific items id or name..", getDescription().getName()));
						users.set(((Player) sender).getDisplayName() + ".ItemNotice", true);
					}
				}
			}
			else if (args[0].equalsIgnoreCase("About")) {
				p.sendMessage(ChatColor.GREEN + "------============" + ChatColor.GOLD + " About TradingPost " + ChatColor.GREEN + "============------");
				p.sendMessage(ChatColor.GOLD + "TradingPost is a plugin designed like Trading Posts in games such as GuildWars 2 or other mmo's alike.");
				p.sendMessage(ChatColor.GOLD + "The main purpose is to" + ChatColor.RED + " sell items to other players" + ChatColor.GOLD + ". The buyers will be buying at the " + ChatColor.RED + "current cheapest price per unit" + ChatColor.GOLD + ".");
				p.sendMessage(ChatColor.GOLD + "The result is that " + ChatColor.LIGHT_PURPLE + "everyone" + ChatColor.GOLD + " is competing against others prices and causes a " + ChatColor.RED + "dynamic player-based economy" + ChatColor.GOLD + ".");
				p.sendMessage(ChatColor.YELLOW + "I give out a special thanks to those who helped me with errors, design, and the coding (especially buckley310).");
				p.sendMessage(ChatColor.GOLD + "TradingPost is designed by " + ChatColor.RED + "seang96" + ChatColor.GOLD + ". Donate to " + ChatColor.AQUA + "ryuk67@gmail.com - Paypal " + ChatColor.GOLD + "to support TradingPost and my server.");
				p.sendMessage(ChatColor.GOLD + "If you find any bugs or feature requests please report it to " + ChatColor.RED + "ryuk67@gmail.com");
			}
			else if (args[0].equalsIgnoreCase("FAQ")) {
				p.sendMessage(ChatColor.GREEN + "-------===========" + ChatColor.GOLD + " TradingPost  FAQ " + ChatColor.GREEN + "============-------");
				p.sendMessage(ChatColor.LIGHT_PURPLE + "Q: " + ChatColor.GOLD + "How do I sell items?");
				p.sendMessage(ChatColor.AQUA + "A: " + ChatColor.GOLD + "Typing /shop sell shows the proper syntax. An example command is: /shop sell stone 4 10 confirm.");
				p.sendMessage(ChatColor.LIGHT_PURPLE + "Q: " + ChatColor.GOLD + "How do I buy items?");
				p.sendMessage(ChatColor.AQUA + "A: " + ChatColor.GOLD + "Typing /shop buy shows the proper syntax. An example command is: /shop buy stone 4 confirm");
				p.sendMessage(ChatColor.LIGHT_PURPLE + "Q: " + ChatColor.GOLD + "How do I cancel my items on sale?");
				p.sendMessage(ChatColor.AQUA + "A: " + ChatColor.GOLD + "Type /shop transactions will display 10 recent transactions on sale. Locate the ID for the one you wish to cancel then try typing /shop cancel <ID> to cancel it.");
				p.sendMessage(ChatColor.LIGHT_PURPLE + "Q: " + ChatColor.GOLD + "Why does my transactions only show what I am selling?");
				p.sendMessage(ChatColor.AQUA + "A: " + ChatColor.GOLD + "There is a limitation to numbers in java. TradingPost reuses the ID of canceled or bought items, therefore it is useless to show them.");
			return true;
		}
		return false;
	}
}
