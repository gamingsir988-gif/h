
package com.gamingsir85.godwand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class GodWand extends JavaPlugin implements Listener {

    private NamespacedKey wandKey;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        wandKey = new NamespacedKey(this, "god_wand");
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("GodWand enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("GodWand disabled!");
    }

    private ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "GOD WAND");
        meta.setLore(java.util.Arrays.asList(
            ChatColor.YELLOW + "Left Click: Lightning",
            ChatColor.RED + "Right Click: Ultimate Attack"
        ));

        meta.getPersistentDataContainer().set(
            wandKey, PersistentDataType.BYTE, (byte) 1
        );

        wand.setItemMeta(meta);
        return wand;
    }

    private boolean isGodWand(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD)
            return false;

        ItemMeta meta = item.getItemMeta();

        return meta != null &&
            meta.getPersistentDataContainer().has(
                wandKey, PersistentDataType.BYTE
            );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                              String label, String[] args) {

        if (command.getName().equalsIgnoreCase("godwand")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("godwand.use")) {
                player.sendMessage(ChatColor.RED + "No permission!");
                return true;
            }

            player.getInventory().addItem(createWand());
            player.sendMessage(ChatColor.GOLD + "You received the GOD WAND!");
            return true;
        }

        return false;
    }

    @EventHandler
    public void onWandUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!isGodWand(event.getItem()))
            return;

        Action action = event.getAction();

        if (action != Action.LEFT_CLICK_AIR &&
            action != Action.LEFT_CLICK_BLOCK &&
            action != Action.RIGHT_CLICK_AIR &&
            action != Action.RIGHT_CLICK_BLOCK)
            return;

        event.setCancelled(true);

        if (!player.hasPermission("godwand.use"))
            return;

        if (action == Action.LEFT_CLICK_AIR ||
            action == Action.LEFT_CLICK_BLOCK) {
            lightningAttack(player);
        } else {
            ultimateAttack(player);
        }
    }

    private boolean checkCooldown(Player player) {
        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < 5000) {
            player.sendMessage(ChatColor.RED + "Wait for your attack cooldown!");
            return false;
        }

        cooldowns.put(player.getUniqueId(), now);
        return true;
    }

    private Location getTarget(Player player) {
        Location target = player.getTargetBlockExact(30) != null
            ? player.getTargetBlockExact(30).getLocation().add(0.5, 1, 0.5)
            : player.getLocation().add(player.getLocation().getDirection().multiply(8));

        return target;
    }

    private void lightningAttack(Player player) {
        if (!checkCooldown(player))
            return;

        Location loc = getTarget(player);
        World world = loc.getWorld();

        world.strikeLightningEffect(loc);
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 60, 2, 2, 2, 0.2);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);

        for (Entity entity : world.getNearbyEntities(loc, 3, 3, 3)) {
            if (entity instanceof LivingEntity && entity != player) {
                ((LivingEntity) entity).damage(8.0, player);
            }
        }

        player.sendMessage(ChatColor.YELLOW + "LIGHTNING ATTACK!");
    }

    private void ultimateAttack(Player player) {
        if (!checkCooldown(player))
            return;

        Location loc = getTarget(player);
        World world = loc.getWorld();

        world.strikeLightningEffect(loc);

        world.spawnParticle(Particle.EXPLOSION, loc, 5, 2, 1, 2, 0);
        world.spawnParticle(Particle.FLAME, loc, 150, 3, 2, 3, 0.15);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 100, 3, 2, 3, 0.1);
        world.spawnParticle(Particle.CLOUD, loc, 100, 3, 2, 3, 0.2);
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 100, 3, 3, 3, 0.2);

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.7f);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 1f);

        for (Entity entity : world.getNearbyEntities(loc, 5, 5, 5)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;

                target.damage(12.0, player);
                target.setFireTicks(60);

                Vector direction = target.getLocation().toVector()
                    .subtract(loc.toVector());

                if (direction.lengthSquared() > 0.01) {
                    direction.normalize().multiply(1.5).setY(0.8);
                    target.setVelocity(direction);
                }
            }
        }

        player.sendMessage(
            ChatColor.GOLD + "" + ChatColor.BOLD + "ULTIMATE ATTACK ACTIVATED!"
        );
    }
}
