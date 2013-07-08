old  new	
...	...	@@ -0,0 +1,287 @@
	1	+package com.spgrn.tradingpost;
	2	+
	3	+import java.io.File;
	4	+import java.io.FileOutputStream;
	5	+import java.io.IOException;
	6	+import java.io.InputStream;
	7	+import java.io.OutputStream;
	8	+import java.util.logging.Logger;
	9	+import org.bukkit.Material;
	10+import org.bukkit.command.Command;
	11+import org.bukkit.command.CommandSender;
	12+import org.bukkit.configuration.file.FileConfiguration;
	13+import org.bukkit.configuration.file.YamlConfiguration;
	14+import org.bukkit.entity.Player;
	15+import org.bukkit.inventory.ItemStack;
	16+import org.bukkit.plugin.java.JavaPlugin;
	17+import net.milkbowl.vault.economy.Economy;
	18+import net.milkbowl.vault.economy.EconomyResponse;
	19+import net.milkbowl.vault.permission.Permission;
	20+import org.bukkit.plugin.RegisteredServiceProvider;
	21	+
	22	+public final class TradingPost extends JavaPlugin {
	23	+
	24	+    private static final Logger log = Logger.getLogger("Minecraft");
	25	+    public static Economy econ = null;
	26	+    public static Permission perms = null;
	27	+    File configFile;
	28	+    File dataFile;
	29	+    FileConfiguration config;
	30	+    FileConfiguration data;
	31	+
	32	+    
	33	+    @Override
	34	+    public void onEnable(){
	35	+        configFile = new File(getDataFolder(), "config.yml");
	36	+        dataFile = new File(getDataFolder(), "data.yml");
	37	+        try {
	38	+            firstRun();
	39	+        } catch (Exception e) {
	40	+            e.printStackTrace();
	41	+        }
	42	+        if (!setupEconomy() ) {
	43	+            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
	44	+            getServer().getPluginManager().disablePlugin(this);
	45	+            return;
	46	+         }
	47	+    	 else{
	48	+    		 log.info(String.format("[%s] - Vault dependency found!", getDescription().getName()));
	49	+    	 }
	50	+         setupPermissions();
	51	+         config = new YamlConfiguration();
	52	+         data = new YamlConfiguration();
	53	+         loadYamls();
	54	+    }
	55	+ 
	56	+    @Override
	57	+    public void onDisable() {
	58	+        log.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
	59	+    }
	60	+    
	61	+    public void saveYamls() {
	62	+        try {
	63	+            config.save(configFile);
	64	+            data.save(dataFile);
	65	+        } catch (IOException e) {
	66	+            e.printStackTrace();
	67	+        }
	68	+    }
	69	+    public void loadYamls() {
	70	+        try {
	71	+            config.load(configFile);
	72	+            data.load(dataFile);
	73	+        } catch (Exception e) {
	74	+            e.printStackTrace();
	75	+        }
	76	+    }
	77	+    
	78	+    private void firstRun() throws Exception {
	79	+        if(!configFile.exists()){
	80	+            configFile.getParentFile().mkdirs();
	81	+            copy(getResource("config.yml"), configFile);
	82	+        }
	83	+        if(!dataFile.exists()){
	84	+            dataFile.getParentFile().mkdirs();
	85	+            copy(getResource("data.yml"), dataFile);
	86	+        }
	87	+    }
	88	+    private void copy(InputStream in, File file) {
	89	+        try {
	90	+            OutputStream out = new FileOutputStream(file);
	91	+            byte[] buf = new byte[1024];
	92	+            int len;
	93	+            while((len=in.read(buf))>0){
	94	+                out.write(buf,0,len);
	95	+            }
	96	+            out.close();
	97	+            in.close();
	98	+        } catch (Exception e) {
	99	+            e.printStackTrace();
	100	+        }
	101	+    }
	102	+    
	103	+    private boolean setupEconomy() {
	104	+        if (getServer().getPluginManager().getPlugin("Vault") == null) {
	105	+            return false;
	106	+        }
	107	+        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
	108	+        if (rsp == null) {
	109	+            return false;
	110	+        }
	111	+        econ = rsp.getProvider();
	112	+        return econ != null;
	113	+    }
	114	+
	115	+    private boolean setupPermissions() {
	116	+        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
	117	+        perms = rsp.getProvider();
	118	+        return perms != null;
	119	+    }
	120	+    
	121	+    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
	122	+    	
	123	+    	Player p = (Player) sender;
	124	+		
	125	+    	if (args.length > 4) {
	126	+            p.sendMessage("Too many arguments!");
	127	+            return false;
	128	+         } 
	129	+         if (args.length < 2) {
	130	+            p.sendMessage("Not enough arguments!");
	131	+            return false;
	132	+         }
	133	+         if(command.getLabel().equals("shop")) {
	134	+     		if(args[0].equalsIgnoreCase("Sell")) {
	135	+     			int IDcount = data.getInt("Total");
	136	+ 				int amount = Integer.parseInt(args[2]);
	137	+ 				int price = Integer.parseInt(args[3]);
	138	+ 				double tax = price * amount * 0.15;
	139	+ 				Material mat = Material.matchMaterial(args[1]);
	140	+ 				int id = mat.getId();
	141	+ 				ItemStack is = new ItemStack (mat, amount);
	142	+ 				if(p.getInventory().contains(mat, amount)){
	143	+ 					p.getInventory().removeItem(is);
	144	+ 					EconomyResponse r = econ.withdrawPlayer(p.getName(), tax);
	145	+ 					if(r.transactionSuccess()) {
	146	+ 						IDcount++;
	147	+ 						p.sendMessage("You have added " + args[2] + " " + args[1] + " for " + args[3] + " to the market. You have paid " + String.valueOf(tax) + " for taxes." );
	148	+ 						loadYamls();
	149	+ 						data.set("Total", IDcount);
	150	+ 						data.set(IDcount + ".Player", ((Player)sender).getDisplayName());
	151	+ 						data.set(IDcount + ".Item", id);
	152	+ 						data.set(IDcount + ".Amount", amount);
	153	+ 						data.set(IDcount + ".Price", price);
	154	+ 						data.set(IDcount + ".Status", "Selling");
	155	+ 						saveYamls();
	156	+ 					} else {
	157	+ 						sender.sendMessage(String.format("An error occured: %s", r.errorMessage));
	158	+ 					}
	159	+ 				}
	160	+ 				}
	161	+     		if(args[0].equalsIgnoreCase("List")) {
	162	+     			if(args[2].equalsIgnoreCase("amount")) {
	163	+     				int amountc = 0 + data.getInt("Amount");
	164	+     				String totalamount = String.valueOf(amountc);
	165	+     				if(amountc == 1) {
	166	+     				sender.sendMessage(String.format("There is only " + String.valueOf(totalamount) + " of " + args[1] + " left."));
	167	+     				}
	168	+     				else if(amountc == 0) {
	169	+     					sender.sendMessage(String.format("There are none of " + args[1] + " on the market."));
	170	+     				}
	171	+     				else {
	172	+     					sender.sendMessage(String.format("There are " + String.valueOf(totalamount) + " of " + args[1] + " lerft."));
	173	+     				}
	174	+     			}
	175	+     			if(args[1].equalsIgnoreCase("common")) {
	176	+     				//Most amount of items in ID, displays the top 10
	177	+     				
	178	+     			}
	179	+     			if(args[1].equalsIgnoreCase("expensive")) {
	180	+     				//Most expensive items on the market by ID, displays top 10, (also checks the cheapest of that item ID to be the most expensive)
	181	+     				
	182	+     			}
	183	+     			if(args[1].equalsIgnoreCase("recent")) {
	184	+     				//Most recent items added to being sold
	185	+     				
	186	+     			}
	187	+     		}
	188	+     		if(args[0].equalsIgnoreCase("Buy")) {
	189	+     			//Find cheapest price of item, buy that first until it is empty, delete lines in yml if empty, and if buyer has more amount then go for the next cheapest
	190	+ 				Material mat = Material.matchMaterial(args[1]);
	191	+ 				int id = mat.getId();
	192	+ 				int amount = Integer.parseInt(args[2]);
	193	+ 				int totalprice = 0;
	194	+				int currentamount = 0;
	195	+				int totalamount = amount;
	196	+				while(currentamount < totalamount) {
	197	+					for(int i = 1; i <= data.getInt("Total"); i++) {
	198	+	 					if((data.getInt(i + ".Item") == id) && (data.getString(i + ".Status") == "Selling")) {
	199	+	 	 				int lowestPrice = data.getInt(i + ".Price"); 
	200	+	 	 					for(int i1 = 1; i1 <= data.getInt("Total"); i1++) {
	201	+	 	 						if((data.getInt(i1 + ".Item") == id) && (data.getString(i1 + ".Status") == "Selling") && (data.getInt(i1 + ".Price") <= lowestPrice)) {
	202	+ 									if(args[3].equalsIgnoreCase("confirm")) { 	
	203	+ 										if(data.getInt(i1 + ".Amount") < amount) {
	204	+ 											int price = data.getInt(i1 + ".Amount") * data.getInt(i1 + ".Price");
	205	+	 			 							EconomyResponse r = econ.withdrawPlayer(p.getName(), price);
	206	+	 			 							EconomyResponse r1 = econ.depositPlayer(data.getString(i1 + ".Player"), price);
	207	+	 			 							if(r.transactionSuccess() && r1.transactionSuccess()) {
	208	+	 			 								loadYamls();
	209	+	 			 								totalprice = + data.getInt(i1 + ".Price");
	210	+		 										currentamount = + data.getInt(i1 + ".Amount");
	211	+		 										int itemamount = data.getInt(i1 + ".Amount");
	212	+		 						 				ItemStack is = new ItemStack (mat, itemamount);
	213	+	 			 	 							p.getInventory().addItem(is);	
	214	+	 			 	 							if(currentamount == totalamount) {
	215	+	 			 	 								sender.sendMessage(String.format("You have bought " + totalamount + " of " + mat + " for " + totalprice + "."));
	216	+	 			 	 							}
	217	+	 			 	 							amount = - itemamount;
	218	+		 										data.set(i1 + ".Status", "Sold");
	219	+	 			 							}
	220	+ 										}
	221	+ 		 								else if(data.getInt(i1 + ".Amount") > amount){
	222	+ 		 									int price = amount * data.getInt(i1 + ".Price");
	223	+ 		 			 							EconomyResponse r = econ.withdrawPlayer(p.getName(), price);
	224	+ 		 			 							EconomyResponse r1 = econ.depositPlayer(data.getString(i1 + ".Player"), price);
	225	+ 		 			 							if(r.transactionSuccess() && r1.transactionSuccess()) {
	226	+ 		 		 									currentamount = + amount;
	227	+ 		 		 									loadYamls();
	228	+ 		 			 								totalprice = + data.getInt(i1 + ".Price");
	229	+ 			 										data.set(i1 + ".Amount", (data.getInt(i1 + ".Amount") - amount));
	230	+ 			 										saveYamls();
	231	+ 			 						 				ItemStack is = new ItemStack (mat, currentamount);
	232	+ 		 			 	 							p.getInventory().addItem(is);		
	233	+ 			 										if(currentamount == totalamount) {
	234	+ 			 										sender.sendMessage(String.format("You have bought " + totalamount + " of " + mat + " for " + totalprice + "."));
	235	+ 			 										}
	236	+ 		 			 							}
	237	+ 		 									}
	238	+ 		 								else if (data.getInt(i1 + ".Amount") == amount) {
	239	+ 		 									currentamount = amount - data.getInt(i1 + ".Amount");
	240	+ 		 									int price = amount * data.getInt(i1 + ".Price");
	241	+ 		 			 						EconomyResponse r = econ.withdrawPlayer(p.getName(), price);
	242	+ 		 			 						EconomyResponse r1 = econ.depositPlayer(data.getString(i1 + ".Player"), price);
	243	+ 		 			 						if(r.transactionSuccess() && r1.transactionSuccess()) {
	244	+ 		 			 							loadYamls();
	245	+ 			 									data.set(i1 + ".Status", "Sold");
	246	+ 		 			 							totalprice = + data.getInt(i1 + ".Price");
	247	+ 			 									saveYamls();
	248	+ 		 		 								currentamount = + amount;
	249	+ 			 					 				ItemStack is = new ItemStack (mat, currentamount);
	250	+ 		 			  							p.getInventory().addItem(is);		
	251	+ 			 									if(currentamount == totalamount) {
	252	+ 			 									sender.sendMessage(String.format("You have bought " + totalamount + " of " + mat + " for " + totalprice + "."));
	253	+ 												}
	254	+ 		 		 							}
	255	+ 		 								}
	256	+ 									}
	257	+	 								else {
	258	+	 									if(!(data.getString(i1 + ".Check") == "False")) {
	259	+	 										loadYamls();
	260	+	 										currentamount = + data.getInt(i1 + ".Amount");
	261	+	 										amount = + data.getInt(i1 + ".Amount");
	262	+	 										data.set(i1 + ".Check", "False");
	263	+ 			 								totalprice = + data.getInt(i1 + ".Price");
	264	+ 			 								saveYamls();
	265	+ 			 								if(currentamount == totalamount) {
	266	+ 			 									sender.sendMessage(String.format("You will pay " + totalprice + " for " + amount + " of " + mat + "."));
	267	+ 			 									for(int i3 = 1; i3 <= data.getInt("Total"); i3++) {
	268	+ 													if((data.getString(i3 + ".Check") == "False") && (data.getInt(i3 + ".Item") == id)) {
	269	+ 			 											loadYamls();
	270	+ 			 											data.set(i3 + ".Check", null);
	271	+ 			 											saveYamls();
	272	+ 			 										}
	273	+ 												}
	274	+ 			 								}
	275	+	 									}
	276	+	 								}
	277	+	 	 						}
	278	+	 	 					}
	279	+	 					}
	280	+					}
	281	+ 				}
	282	+ 			}  
	283	+     		return true;
	284	+         }
	285	+       return false;
	286	+    }
	287	+}
1	288	\ No newline at end of file
