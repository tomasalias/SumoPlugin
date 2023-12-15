package com.example.sumo;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.CitizensPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SumoPlugin extends JavaPlugin implements Listener {

private Location hubLocation;
private Location arenaLocation;
private Location botLocation;
private Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
private boolean matchInProgress;
private String sumoWorldName;
private NPC sumoBot;

@Override
public void onEnable() {
    saveDefaultConfig();
    hubLocation = loadLocationFromConfig("locations.hub");
    arenaLocation = loadLocationFromConfig("locations.arena");
    botLocation = loadLocationFromConfig("locations.bot");
    getServer().getPluginManager().registerEvents(this, this);
    matchInProgress = false;
    sumoWorldName = getConfig().getString("worldName");
    setCitizensSpawnNoDamageDuration();
}

private void setCitizensSpawnNoDamageDuration() {
    // Get the Citizens plugin instance
    CitizensPlugin citizensPlugin = getCitizensPlugin();
    if (citizensPlugin != null) {
        // Check if the current spawn-nodamage-duration value is "1s"
        String currentValue = citizensPlugin.getConfig().getString("npc.default.spawn-nodamage-duration");
        if ("1s".equals(currentValue)) {
            // Set the spawn-nodamage-duration to 0
            citizensPlugin.getConfig().set("npc.default.spawn-nodamage-duration", "0s");
            citizensPlugin.saveConfig();
        }
        String currentValue1 = citizensPlugin.getConfig().getString("npc.delay-player-teleport");
        if ("-1".equals(currentValue1)) {
            // Set the spawn-nodamage-duration to 0
            citizensPlugin.getConfig().set("npc.delay-player-teleport", 0);
            citizensPlugin.saveConfig();
        }
        String currentValue2 = citizensPlugin.getConfig().getString("npc.skins.player-join-update-delay");
        if ("3s".equals(currentValue2)) {
            // Set the spawn-nodamage-duration to 0
            citizensPlugin.getConfig().set("npc.skins.player-join-update-delay", "0s");
            citizensPlugin.saveConfig();
        }
        String currentValue3 = citizensPlugin.getConfig().getString("npc.packets.update-delay");
        if ("30".equals(currentValue3)) {
            // Set the spawn-nodamage-duration to 0
            citizensPlugin.getConfig().set("npc.packets.update-delay", 0);
            citizensPlugin.saveConfig();
        }
    }
}

public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
        sender.sendMessage("This command can only be used by a player.");
        return true;
    }

    Player player = (Player) sender;

    if (args.length < 1) {
        return false;
    }

    switch (args[0].toLowerCase()) {
        case "hub":
            hubLocation = player.getLocation();
            saveLocationToConfig("locations.hub", hubLocation);
            player.sendMessage("Hub location set.");
            break;

        case "arena":
            arenaLocation = player.getLocation();
            saveLocationToConfig("locations.arena", arenaLocation);
            player.sendMessage("Arena location set.");
            break;

        case "bot":
            botLocation = player.getLocation();
            saveLocationToConfig("locations.bot", botLocation);
            player.sendMessage("Bot location set.");
            break;

        case "world":
            if (args.length != 2) {
                player.sendMessage("Usage: /sumo world <worldname>");
                return true;
            }
            sumoWorldName = args[1];
            getConfig().set("worldName", sumoWorldName);
            saveConfig();
            player.sendMessage("Sumo world name set to: " + sumoWorldName);
            break;

        default:
            return false;
    }
    return true;
}

private Location loadLocationFromConfig(String path) {
    if (getConfig().contains(path + ".world")) {
        World world = Bukkit.getWorld(getConfig().getString(path + ".world"));
        double x = getConfig().getDouble(path + ".x");
        double y = getConfig().getDouble(path + ".y");
        double z = getConfig().getDouble(path + ".z");
        float yaw = (float) getConfig().getDouble(path + ".yaw");
        float pitch = (float) getConfig().getDouble(path + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }
    return null;
}

private CitizensPlugin getCitizensPlugin() {
    Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("Citizens");
    if (plugin == null || !(plugin instanceof CitizensPlugin)) {
        return null;
    }
    return (CitizensPlugin) plugin;
}

@SuppressWarnings("deprecation")
@EventHandler
public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
        return;
    }

    Player player = event.getPlayer();
    Block block = event.getClickedBlock();

    if (block.getType() != Material.OAK_SIGN && block.getType() != Material.OAK_WALL_SIGN) {
        return;
    }

    Sign sign = (Sign) block.getState();
    if (sign.getLine(0).equalsIgnoreCase("[sumo]") && sign.getLine(1).equalsIgnoreCase("join")) {
        if (!player.hasPermission("sumo.place")) {
            player.sendMessage("You don't have permission to use this sign.");
            return;
        }

        if (hubLocation == null || arenaLocation == null || botLocation == null || sumoWorldName == null) {
            player.sendMessage("Set commands first.");
            return;
        }

        if (matchInProgress) {
            player.sendMessage(ChatColor.RED + "The match has already started.");
            return;
        }

        matchInProgress = true;
        teleportToArena(player, arenaLocation);
    }
}

private void saveLocationToConfig(String path, Location location) {
    getConfig().set(path + ".world", location.getWorld().getName());
    getConfig().set(path + ".x", location.getX());
    getConfig().set(path + ".y", location.getY());
    getConfig().set(path + ".z", location.getZ());
    getConfig().set(path + ".yaw", location.getYaw());
    getConfig().set(path + ".pitch", location.getPitch());
    saveConfig();
}

public void teleportToArena(Player player, Location arenaLocation) {
    // Teleport the player to the arena
    player.teleport(arenaLocation);
        spawnSumoBot(player);
}

private void spawnSumoBot(Player player) {
	
    NPCRegistry registry = CitizensAPI.getNPCRegistry();
    NPC sumoBot = registry.createNPC(EntityType.PLAYER, "SumoBot");
    sumoBot.spawn(botLocation);
    SumoBotAI sumoBotAI = new SumoBotAI(this, player);
    sumoBot.addTrait(sumoBotAI);
    sumoBot.setProtected(false);
    
    // Add regeneration effect to the SumoBot with hidden particles
    int maxLevel = Integer.MAX_VALUE;
    int maxDuration = Integer.MAX_VALUE;
    boolean showParticles = false;
    ((LivingEntity) sumoBot.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, maxDuration, maxLevel, false, showParticles));
    
    // Assign the newly created NPC to the sumoBot variable
    this.sumoBot = sumoBot;

    // ...

    // Set the sumo bot's noDamageTicks to 0
    if (sumoBot.getEntity() instanceof Player) {
        Player sumoBotPlayer = (Player) sumoBot.getEntity();
        sumoBotPlayer.setNoDamageTicks(0);
    }
}

public void despawnSumoBot() {
    if (sumoBot != null && sumoBot.isSpawned()) {
        sumoBot.despawn();
        sumoBot.destroy();
    }
}

@EventHandler
public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();

    // If the player is not in the sumo world, do nothing.
    if (!player.getWorld().getName().equals(sumoWorldName)) {
        return;
    }

    // If the player is in the arena and falls into the water, declare the bot as the winner.
    if (player.getLocation().getBlock().getType() == Material.WATER && player.getWorld().equals(arenaLocation.getWorld())) {
        player.teleport(hubLocation);
        player.sendMessage(ChatColor.RED + "You lost the match!");

        // Make the bot disappear.
        	despawnSumoBot();

        // Reset the matchInProgress flag.
        matchInProgress = false;
        restoreInventory(player);
    }
}

private void restoreInventory(Player player) {
    UUID uuid = player.getUniqueId();
    if (savedInventories.containsKey(uuid)) {
        player.getInventory().setContents(savedInventories.get(uuid));
        savedInventories.remove(uuid);
    }
}

public void onSumoBotVictory(Player player, NPC npc) {
        		
                player.teleport(hubLocation);
                player.sendMessage(ChatColor.GREEN + "Congratulations! You defeated the Sumo PvP-bot!");
                
                // Make the bot disappear.
            	despawnSumoBot();
            	
                // Reset the matchInProgress flag.
                matchInProgress = false;
                restoreInventory(player);
                
}


class SumoBotAI extends Trait {

private final SumoPlugin plugin;
private final Player player;
private boolean inWater;

public SumoBotAI(SumoPlugin plugin, Player player) {
    super("SumoBotAI");
    this.plugin = plugin;
    this.player = player;
    this.inWater = false;
}

@Override
public void run() {
    if (!inWater) {
        if (npc.getEntity().getLocation().getBlock().getType() == Material.WATER) {
            inWater = true;
            plugin.onSumoBotVictory(player, npc);
        } else {
            npc.getNavigator().setTarget(player, true);
        }
    }
}

@Override
public void onAttach() {
    npc.getNavigator().getDefaultParameters().baseSpeed();
    npc.getNavigator().getDefaultParameters().attackDelayTicks(10);
    npc.getNavigator().getDefaultParameters().attackRange(3.0);
}
}
}