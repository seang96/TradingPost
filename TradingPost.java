     		if(args[0].equalsIgnoreCase("Buy")) {
     			//Find cheapest price of item, buy that first until it is empty, delete lines in yml if empty, and if buyer has more amount then go for the next cheapest
 				Material mat = Material.matchMaterial(args[1]);
 				int id = mat.getId();
 				int amount = Integer.parseInt(args[2]);
 				int totalprice = 0;
				int currentamount = 0;
				int totalamount = amount;
				int i = 0;
				int j = 0;
				while(currentamount != totalamount) {
					log.info(String.format("test"));
					int lowestPrice = 2147483647;
					for(i = 1; i <= data.getInt("Total"); i++) {
	 					if((data.getInt(i + ".Item") == id) && (data.getString(i + ".Status") == "Selling") && (data.getInt(i + ".Price") < lowestPrice)) {
	 						lowestPrice = data.getInt(i + ".Price"); 
	 						j= i;
	 					}
					}
 					if(data.getInt(j + ".Amount") <= amount) {
 						int price = data.getInt(j + ".Price") * data.getInt(j + ".Amount");
 			 			totalprice += price;
	 					currentamount += data.getInt(j + ".Amount");
	 					log.info(String.format(String.valueOf(currentamount) + String.valueOf(totalamount)));
 			 	 		amount -= data.getInt(j + ".Amount");
						if(args[3].equalsIgnoreCase("confirm")) {
							EconomyResponse r1 = econ.depositPlayer(data.getString(j + ".Player"), price);
							if(r1.transactionSuccess()) {
								data.set(j + ".Status", "Sold");
								saveYamls();
		 						getLogger().info(String.valueOf(currentamount));
							}
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
 		 			}
				}
				if(args[3].equalsIgnoreCase("confirm")) {
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
				else if(!(data.getString(j + ".Check") == "False")) {
					loadYamls();
					data.set(j + ".Check", "False");
		 			saveYamls();
	 				if(currentamount == totalamount) {
	 					sender.sendMessage(String.format("You will pay " + totalprice + " for " + amount + " of " + mat + "."));
		 				for(j = 1; j <= data.getInt("Total"); j++) {
							if((data.getString(j + ".Check") == "False") && (data.getInt(j + ".Item") == id)) {
								loadYamls();
								data.set(j + ".Check", null);
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
