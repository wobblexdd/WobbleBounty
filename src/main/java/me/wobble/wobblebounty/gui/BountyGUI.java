package me.wobble.wobblebounty.gui;

import me.wobble.wobblebounty.WobbleBounty;
import me.wobble.wobblebounty.model.Bounty;
import me.wobble.wobblebounty.service.BountyService;
import me.wobble.wobblebounty.util.ChatUtil;
import me.wobble.wobblebounty.util.TextStyleUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class BountyGUI {

    public static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public static final int SORT_SLOT = 50;
    public static final int SEARCH_SLOT = 48;
    public static final int RESET_SLOT = 51;

    private final WobbleBounty plugin;
    private final BountyService bountyService;
    private final Pagination<Bounty> pagination;

    public BountyGUI(WobbleBounty plugin, BountyService bountyService) {
        this.plugin = plugin;
        this.bountyService = bountyService;
        this.pagination = new Pagination<>();
    }

    public void open(Player player) {
        open(player, 1, BountyService.SortType.HIGHEST, null);
    }

    public void open(Player player, int page, BountyService.SortType sortType, String searchQuery) {
        String title = plugin.getConfig().getString("gui.title", "<dark_gray>ʙᴏᴜɴᴛʏ");
        int size = plugin.getConfig().getInt("gui.size", 54);

        List<Bounty> allBounties = prepareBounties(sortType, searchQuery);
        int pageSize = getPageSize();
        int maxPage = pagination.maxPage(allBounties, pageSize);

        if (page < 1) {
            page = 1;
        }
        if (page > maxPage) {
            page = maxPage;
        }

        Inventory inventory = ManagedGui.createInventory(ManagedGui.Type.BOUNTY_MAIN, size, ChatUtil.mm(title));

        fillBackground(inventory);
        placeFrameAccents(inventory);
        placeControlButtons(inventory, page, maxPage, allBounties.size(), sortType, searchQuery);

        if (allBounties.isEmpty()) {
            inventory.setItem(22, simpleItem(
                    Material.PAPER,
                    "<red>" + TextStyleUtil.smallCaps("No Results"),
                    List.of(
                            ChatUtil.mm(TextStyleUtil.hint(searchQuery == null || searchQuery.isBlank()
                                    ? "there are no active bounties yet"
                                    : "no players matched your search")),
                            ChatUtil.mm("<dark_gray>—"),
                            ChatUtil.mm("<gray>Search:</gray> <yellow>" + TextStyleUtil.displaySearch(searchQuery) + "</yellow>"),
                            ChatUtil.mm("<gray>Use:</gray> <gold>/bounty place <player> <amount></gold>")
                    ),
                    true
            ));
            player.openInventory(inventory);
            return;
        }

        List<Bounty> pageBounties = pagination.page(allBounties, page, pageSize);

        for (int i = 0; i < Math.min(pageBounties.size(), CONTENT_SLOTS.length); i++) {
            Bounty bounty = pageBounties.get(i);

            OfflinePlayer target = Bukkit.getOfflinePlayer(bounty.getTargetId());
            String name = bounty.getTargetName() == null || bounty.getTargetName().isBlank() ? displayName(target) : bounty.getTargetName();

            List<Component> lore = new ArrayList<>();
            lore.add(ChatUtil.mm(TextStyleUtil.normalLabel("Target", name)));
            lore.add(ChatUtil.mm("<gray>Bounty:</gray> <gold>" + bountyService.format(bounty.getAmount()) + "</gold>"));
            lore.add(ChatUtil.mm("<gray>Created:</gray> <yellow>" + relativeTime(bounty.getCreatedAt()) + "</yellow>"));
            lore.add(ChatUtil.mm("<dark_gray>—"));
            lore.add(ChatUtil.mm(TextStyleUtil.hint("click to open confirm menu")));

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.displayName(ChatUtil.mm("<gold>" + name + "</gold>"));
            meta.lore(lore);
            meta.setOwningPlayer(target);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            head.setItemMeta(meta);

            inventory.setItem(CONTENT_SLOTS[i], head);
        }

        player.openInventory(inventory);
    }

    public int getPageSize() {
        int configured = plugin.getConfig().getInt("gui.items-per-page", CONTENT_SLOTS.length);
        return Math.min(configured, CONTENT_SLOTS.length);
    }

    public int getResultCount(BountyService.SortType sortType, String searchQuery) {
        return prepareBounties(sortType, searchQuery).size();
    }

    public int getMaxPage(BountyService.SortType sortType, String searchQuery) {
        return pagination.maxPage(prepareBounties(sortType, searchQuery), getPageSize());
    }

    public Bounty getBountyBySlot(int slot, int page, BountyService.SortType sortType, String searchQuery) {
        List<Bounty> allBounties = prepareBounties(sortType, searchQuery);
        List<Bounty> pageBounties = pagination.page(allBounties, page, getPageSize());

        for (int i = 0; i < CONTENT_SLOTS.length && i < pageBounties.size(); i++) {
            if (CONTENT_SLOTS[i] == slot) {
                return pageBounties.get(i);
            }
        }

        return null;
    }

    private List<Bounty> prepareBounties(BountyService.SortType sortType, String searchQuery) {
        List<Bounty> source = new ArrayList<>(bountyService.getAllBounties());

        if (searchQuery != null && !searchQuery.isBlank()) {
            String query = searchQuery.toLowerCase(Locale.ROOT);

            source.removeIf(bounty -> {
                String baseName = bounty.getTargetName() == null || bounty.getTargetName().isBlank() ? displayName(Bukkit.getOfflinePlayer(bounty.getTargetId())) : bounty.getTargetName();
                String name = baseName.toLowerCase(Locale.ROOT);
                return !name.contains(query);
            });

            source.sort(Comparator.comparingInt((Bounty bounty) -> {
                String baseName = bounty.getTargetName() == null || bounty.getTargetName().isBlank() ? displayName(Bukkit.getOfflinePlayer(bounty.getTargetId())) : bounty.getTargetName();
                String name = baseName.toLowerCase(Locale.ROOT);
                return name.equals(query) ? 0 : 1;
            }));
        }

        switch (sortType) {
            case HIGHEST -> source.sort(Comparator.comparingDouble(Bounty::getAmount).reversed());
            case LOWEST -> source.sort(Comparator.comparingDouble(Bounty::getAmount));
            case NEWEST -> source.sort(Comparator.comparingLong(Bounty::getCreatedAt).reversed());
            case OLDEST -> source.sort(Comparator.comparingLong(Bounty::getCreatedAt));
            case ALPHABETICAL -> source.sort(Comparator.comparing(bounty ->
                    ((bounty.getTargetName() == null || bounty.getTargetName().isBlank()) ? displayName(Bukkit.getOfflinePlayer(bounty.getTargetId())) : bounty.getTargetName()).toLowerCase(Locale.ROOT)));
        }

        return source;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = simpleItem(Material.BLACK_STAINED_GLASS_PANE, "<dark_gray> ", List.of(), true);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private void placeFrameAccents(Inventory inventory) {
        ItemStack accent = simpleItem(Material.GRAY_STAINED_GLASS_PANE, "<gray> ", List.of(), true);

        int[] accentSlots = {
                0, 1, 2, 3, 5, 6, 7, 8,
                9, 17, 18, 26, 27, 35, 36, 44,
                46, 47, 52
        };

        for (int slot : accentSlots) {
            inventory.setItem(slot, accent);
        }
    }

    private void placeControlButtons(Inventory inventory, int page, int maxPage, int totalEntries, BountyService.SortType sortType, String searchQuery) {
        int prevSlot = plugin.getConfig().getInt("gui.slots.previous-page", 45);
        int infoSlot = plugin.getConfig().getInt("gui.slots.info", 4);
        int closeSlot = plugin.getConfig().getInt("gui.slots.close", 49);
        int nextSlot = plugin.getConfig().getInt("gui.slots.next-page", 53);

        boolean hasPrevious = page > 1;
        boolean hasNext = page < maxPage;
        boolean hasSearch = searchQuery != null && !searchQuery.isBlank();

        inventory.setItem(prevSlot, simpleItem(
                hasPrevious ? Material.ARROW : Material.GRAY_DYE,
                hasPrevious ? "<yellow>" + TextStyleUtil.smallCaps("Previous Page") : "<gray>" + TextStyleUtil.smallCaps("Previous Page"),
                List.of(
                        ChatUtil.mm(TextStyleUtil.hint("go to previous page")),
                        ChatUtil.mm("<gray>Page:</gray> <yellow>" + page + "</yellow><gray>/</gray><yellow>" + maxPage + "</yellow>")
                ),
                true
        ));

        inventory.setItem(nextSlot, simpleItem(
                hasNext ? Material.ARROW : Material.GRAY_DYE,
                hasNext ? "<yellow>" + TextStyleUtil.smallCaps("Next Page") : "<gray>" + TextStyleUtil.smallCaps("Next Page"),
                List.of(
                        ChatUtil.mm(TextStyleUtil.hint("go to next page")),
                        ChatUtil.mm("<gray>Page:</gray> <yellow>" + page + "</yellow><gray>/</gray><yellow>" + maxPage + "</yellow>")
                ),
                true
        ));

        inventory.setItem(closeSlot, simpleItem(
                Material.BARRIER,
                "<red>" + TextStyleUtil.smallCaps("Close"),
                List.of(ChatUtil.mm(TextStyleUtil.hint("click to close the menu"))),
                true
        ));

        inventory.setItem(infoSlot, simpleItem(
                Material.BOOK,
                "<gold>" + TextStyleUtil.smallCaps("WBountyHunters"),
                List.of(
                        ChatUtil.mm("<gray>Page:</gray> <yellow>" + page + "</yellow><gray>/</gray><yellow>" + maxPage + "</yellow>"),
                        ChatUtil.mm("<gray>Total entries:</gray> <gold>" + totalEntries + "</gold>"),
                        ChatUtil.mm("<gray>Sort:</gray> <yellow>" + TextStyleUtil.sortDisplay(sortType.name()) + "</yellow>"),
                        ChatUtil.mm("<gray>Search:</gray> <yellow>" + TextStyleUtil.displaySearch(searchQuery) + "</yellow>"),
                        ChatUtil.mm("<gray>Results:</gray> <gold>" + totalEntries + "</gold>"),
                        ChatUtil.mm("<dark_gray>—"),
                        ChatUtil.mm(TextStyleUtil.hint("browse, sort and search bounties"))
                ),
                true
        ));

        inventory.setItem(SORT_SLOT, simpleItem(
                Material.HOPPER,
                "<gold>" + TextStyleUtil.smallCaps("Sort"),
                List.of(
                        ChatUtil.mm("<gray>Current:</gray> <yellow>" + TextStyleUtil.sortDisplay(sortType.name()) + "</yellow>"),
                        ChatUtil.mm("<gray>Modes:</gray> <yellow>5</yellow>"),
                        ChatUtil.mm("<dark_gray>—"),
                        ChatUtil.mm(TextStyleUtil.hint("click to cycle sort mode"))
                ),
                true
        ));

        inventory.setItem(SEARCH_SLOT, simpleItem(
                hasSearch ? Material.WRITABLE_BOOK : Material.NAME_TAG,
                "<gold>" + TextStyleUtil.smallCaps("Search"),
                List.of(
                        ChatUtil.mm("<gray>Current:</gray> <yellow>" + TextStyleUtil.displaySearch(searchQuery) + "</yellow>"),
                        ChatUtil.mm("<gray>Results:</gray> <gold>" + totalEntries + "</gold>"),
                        ChatUtil.mm("<dark_gray>—"),
                        ChatUtil.mm(TextStyleUtil.hint("click to search by player name")),
                        ChatUtil.mm(TextStyleUtil.hint("exact matches appear first"))
                ),
                true
        ));

        inventory.setItem(RESET_SLOT, simpleItem(
                hasSearch ? Material.REDSTONE : Material.GRAY_DYE,
                hasSearch ? "<red>" + TextStyleUtil.smallCaps("Reset Filter") : "<gray>" + TextStyleUtil.smallCaps("Reset Filter"),
                List.of(
                        ChatUtil.mm("<gray>Search:</gray> <yellow>" + TextStyleUtil.displaySearch(searchQuery) + "</yellow>"),
                        ChatUtil.mm("<dark_gray>—"),
                        ChatUtil.mm(TextStyleUtil.hint("click to clear current search"))
                ),
                true
        ));
    }

    private ItemStack simpleItem(Material material, String name, List<Component> lore, boolean hideFlags) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ChatUtil.mm(name));
        meta.lore(lore);
        if (hideFlags) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        }
        item.setItemMeta(meta);
        return item;
    }

    private String displayName(OfflinePlayer player) {
        return player.getName() == null ? "Unknown" : player.getName();
    }

    private String relativeTime(long millis) {
        if (millis <= 0L) {
            return "Unknown";
        }

        long diffSeconds = Math.max(0L, (System.currentTimeMillis() - millis) / 1000L);
        long minutes = diffSeconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;

        if (days > 0) {
            return days + "d ago";
        }
        if (hours > 0) {
            return hours + "h ago";
        }
        if (minutes > 0) {
            return minutes + "m ago";
        }
        return diffSeconds + "s ago";
    }
}
