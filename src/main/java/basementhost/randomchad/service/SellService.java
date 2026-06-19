package basementhost.randomchad.service;

import basementhost.randomchad.SellwandPlugin;
import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import me.gypopo.economyshopgui.api.objects.SellPrice;
import me.gypopo.economyshopgui.api.objects.SellPrices;
import me.gypopo.economyshopgui.util.EcoType;
import me.gypopo.economyshopgui.util.EconomyType;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class SellService {
    private final SellwandPlugin plugin;
    private final EcoType vaultEcoType;
    private final Set<Material> allowedContainers;

    public SellService(SellwandPlugin plugin) {
        this.plugin = plugin;
        String economyType = plugin.getConfig().getString("economy-type", "VAULT");
        EcoType configuredEcoType = EconomyType.getFromString(economyType == null ? "VAULT" : economyType);
        this.vaultEcoType = configuredEcoType == null ? EconomyType.getFromString("VAULT") : configuredEcoType;
        this.allowedContainers = loadAllowedContainers();
    }

    public SellResult sellContainer(Player player, Inventory inventory) {
        ItemStack[] before = inventory.getContents();
        ItemStack[] working = cloneContents(before);

        // Avoid removing items that are sellable only for non-Vault economy types.
        boolean[] eligibleSlots = markVaultSellableSlots(player, working);

        SellPrices sellPrices = EconomyShopGUIHook.getCutSellPrices(player, working, true);
        if (sellPrices == null || sellPrices.isEmpty()) {
            return SellResult.nothingToSell();
        }

        double money = sellPrices.getPrice(vaultEcoType);
        if (money <= 0D) {
            return SellResult.nothingToSell();
        }

        int amountSold = countSold(before, working, eligibleSlots);
        if (amountSold <= 0) {
            return SellResult.nothingToSell();
        }

        EconomyResponse response = plugin.economy().depositPlayer(player, money);
        if (response == null || !response.transactionSuccess()) {
            return SellResult.vaultFailed();
        }

        ItemStack[] after = mergeUnsellableSlots(before, working, eligibleSlots);
        inventory.setContents(after);
        sellPrices.updateLimits();
        return SellResult.sold(amountSold, money);
    }

    public boolean isAllowedContainer(Material material) {
        return allowedContainers.contains(material);
    }

    private boolean[] markVaultSellableSlots(Player player, ItemStack[] items) {
        boolean[] eligibleSlots = new boolean[items.length];
        for (int i = 0; i < items.length; i++) {
            eligibleSlots[i] = hasVaultSellPrice(player, items[i]);
            if (!eligibleSlots[i]) {
                items[i] = null;
            }
        }
        return eligibleSlots;
    }

    private boolean hasVaultSellPrice(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        ItemStack single = item.clone();
        single.setAmount(1);
        Optional<?> optional = EconomyShopGUIHook.getSellPrice(player, single);
        if (optional.isEmpty()) {
            return false;
        }

        Object priceObject = optional.get();
        if (!(priceObject instanceof SellPrice sellPrice)) {
            return false;
        }

        return sellPrice.getPrice(vaultEcoType) >= 0D;
    }

    private ItemStack[] cloneContents(ItemStack[] source) {
        ItemStack[] clone = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            clone[i] = source[i] == null ? null : source[i].clone();
        }
        return clone;
    }

    /**
     * getCutSellPrices mutates only the array we pass in. Slots marked ineligible were intentionally
     * excluded from selling, so this method restores them from the original inventory.
     */
    private ItemStack[] mergeUnsellableSlots(ItemStack[] before, ItemStack[] workingAfterSell, boolean[] eligibleSlots) {
        ItemStack[] after = new ItemStack[before.length];
        for (int i = 0; i < before.length; i++) {
            if (!eligibleSlots[i]) {
                after[i] = before[i] == null ? null : before[i].clone();
            } else {
                after[i] = workingAfterSell[i] == null ? null : workingAfterSell[i].clone();
            }
        }
        return after;
    }

    private int countSold(ItemStack[] before, ItemStack[] after, boolean[] eligibleSlots) {
        int sold = 0;
        for (int i = 0; i < before.length; i++) {
            if (!eligibleSlots[i]) {
                continue;
            }

            ItemStack original = before[i];
            if (original == null || original.getType().isAir()) {
                continue;
            }

            ItemStack remaining = after[i];
            int remainingAmount = 0;
            if (remaining != null && !remaining.getType().isAir() && original.isSimilar(remaining)) {
                remainingAmount = remaining.getAmount();
            }
            sold += Math.max(0, original.getAmount() - remainingAmount);
        }
        return sold;
    }

    private Set<Material> loadAllowedContainers() {
        Set<Material> materials = new HashSet<>();
        for (String entry : plugin.getConfig().getStringList("allowed-containers")) {
            if (entry.equalsIgnoreCase("SHULKER_BOXES")) {
                addAllShulkerBoxes(materials);
                continue;
            }

            Material material = Material.matchMaterial(entry);
            if (material != null) {
                materials.add(material);
            } else {
                plugin.getLogger().warning("Unknown material in allowed-containers: " + entry);
            }
        }

        if (materials.isEmpty()) {
            materials.add(Material.CHEST);
            materials.add(Material.TRAPPED_CHEST);
            materials.add(Material.BARREL);
            addAllShulkerBoxes(materials);
        }
        return materials;
    }

    private void addAllShulkerBoxes(Set<Material> materials) {
        for (Material material : Material.values()) {
            if (material.name().endsWith("SHULKER_BOX")) {
                materials.add(material);
            }
        }
    }
}
