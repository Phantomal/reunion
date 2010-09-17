package com.googlecode.reunion.jreunion.game;

import java.util.Iterator;

import com.googlecode.reunion.jreunion.server.Client;
import com.googlecode.reunion.jreunion.server.DatabaseUtils;
import com.googlecode.reunion.jreunion.server.ItemFactory;
import com.googlecode.reunion.jreunion.server.Server;

/**
 * @author Aidamina
 * @license http://reunion.googlecode.com/svn/trunk/license.txt
 */
public class Warehouse extends Npc {

	public Warehouse(int id) {
		super(id);
	}

	/****** Open stash ******/
	public void openStash(Player player) {

		Client client = Server.getInstance().getNetworkModule()
				.getClient(player);

		if (client == null) {
			return;
		}

		Iterator<StashItem> stashIter = player.getStash().itemListIterator();
		while (stashIter.hasNext()) {
			StashItem stashItem = stashIter.next();

			String packetData = "stash " + stashItem.getPos() + " "
					+ stashItem.getItem().getType() + " "
					+ stashItem.getItem().getGemNumber() + " "
					+ stashItem.getItem().getExtraStats() + " "
					+ player.getStash().getQuantity(stashItem.getPos()) + "\n";
					client.SendData( packetData);
		}
				client.SendData( "stash_end");
	}

	/****** Add/Remove items from stash ******/
	public void stashClick(Player player, int pos, int type, int gems,
			int special) {

		Client client = Server.getInstance().getNetworkModule()
				.getClient(player);

		if (client == null) {
			return;
		}

		Stash stash = player.getStash();
		StashItem stashItem;
		String packetData = null;

		if (pos == 12) {
			if (gems > player.getLime()) {
				packetData = "msg WARNING: Lime cheating detected!\n";
						client.SendData( packetData);
				return;
			}
			stashItem = stash.getItem(pos);
			stashItem.getItem().setGemNumber(
					stashItem.getItem().getGemNumber() + gems / 100);
			player.updateStatus(10, gems * -1, 0);
			DatabaseUtils.getInstance().updateItemInfo(stashItem.getItem());
			if (gems >= 0) {
				packetData = "stash_to " + stashItem.getPos() + " "
						+ stashItem.getItem().getType() + " "
						+ stashItem.getItem().getGemNumber() + " "
						+ stashItem.getItem().getExtraStats() + "\n";
			} else {
				packetData = "stash_from " + stashItem.getPos() + " "
						+ stashItem.getItem().getType() + " "
						+ stashItem.getItem().getGemNumber() + " "
						+ stashItem.getItem().getExtraStats() + "\n";
			}
		} else {
			if (player.getInventory().getItemSelected() == null) {
				stashItem = stash.getItem(pos);
				Item item = ItemFactory.loadItem(stashItem.getItem()
						.getEntityId());
				DatabaseUtils.getInstance().loadItemInfo(item);
				stash.removeItem(stashItem);
				player.getInventory().setItemSelected(
						new InventoryItem(item, 0, 0, 0));

				packetData = "stash_from " + stashItem.getPos() + " "
						+ item.getEntityId() + " "
						+ stashItem.getItem().getType() + " "
						+ stashItem.getItem().getGemNumber() + " "
						+ stashItem.getItem().getExtraStats() + " "
						+ player.getStash().getQuantity(pos) + "\n";
			} else {
				if (stash.checkPosEmpty(pos)) {
					Item item = player.getInventory().getItemSelected()
							.getItem();
					stashItem = new StashItem(pos, item);
					stash.addItem(stashItem);
					player.getInventory().setItemSelected(null);

					packetData = "stash_to " + stashItem.getPos() + " "
							+ stashItem.getItem().getType() + " "
							+ stashItem.getItem().getGemNumber() + " "
							+ stashItem.getItem().getExtraStats() + " "
							+ player.getStash().getQuantity(pos) + " 0\n";
				} else {
					stashItem = stash.getItem(pos);

					if (stashItem.getItem().getType() == type
							&& stashItem.getItem().getGemNumber() == player
									.getInventory().getItemSelected().getItem()
									.getGemNumber()
							&& stashItem.getItem().getExtraStats() == special) {
						StashItem newStashItem = new StashItem(pos, player
								.getInventory().getItemSelected().getItem());
						stash.addItem(newStashItem);
						player.getInventory().setItemSelected(null);

						packetData = "stash_to " + stashItem.getPos() + " "
								+ stashItem.getItem().getType() + " "
								+ stashItem.getItem().getGemNumber() + " "
								+ stashItem.getItem().getExtraStats() + " "
								+ stash.getQuantity(pos) + " 0\n";
					}
				}
			}
		}

		// S_DatabaseUtils.getInstance().saveStash(client);
		if (packetData != null) {
					client.SendData( packetData);
		}
	}
}