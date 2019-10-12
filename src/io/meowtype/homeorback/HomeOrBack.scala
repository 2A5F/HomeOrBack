package io.meowtype.homeorback

import java.util

import org.bukkit.command._
import org.bukkit._
import org.bukkit.entity._
import org.bukkit.event.entity._
import org.bukkit.event.inventory._
import org.bukkit.event.player._
import org.bukkit.event._
import org.bukkit.inventory._
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.configuration.ConfigurationSection

class HomeOrBack extends JavaPlugin {

  override def onLoad() {
    self = this
    saveDefaultConfig()
    Lang.loadLang()
    Tpr.loadWorlds()
    getLogger info "Loaded"
  }

  override def onEnable() {
    getServer.getPluginManager.registerEvents(listener, this)
    getCommand("hob") setExecutor  this
    getCommand("back") setExecutor  this
    getLogger info "Enabled"
  }

  override def onDisable() {
    getLogger info "Disabled"
  }

  // region Configs

  def lang: String = getConfig.getString("lang", "en").toLowerCase

  def auto_respawn: Boolean = getConfig.getBoolean("auto_respawn", true)
  def auto_back: Boolean = getConfig.getBoolean("auto_back", false)

  def back_random: BackRandom = new BackRandom(getConfig.getConfigurationSection("back_random"))
  class BackRandom(val section: ConfigurationSection) {
    def enable: Boolean = section.getBoolean("enable", true)
    def min: Double = section.getDouble("min", 8)
    def max: Double = section.getDouble("max", 32)
    def max_try: Int = section.getInt("max_try", 100)
    def uniform: Boolean = section.getBoolean("uniform", true)
    def try_to_land: Boolean = section.getBoolean("try_to_land", true)
    def never_water: Boolean = section.getBoolean("never_water", false)
  }

  def back_command: Boolean = getConfig.getBoolean("back_command", true)

  def show_death_loc: Boolean = getConfig.getBoolean("show_death_loc", false)

  def store_location: Boolean = getConfig.getBoolean("store_location", false)

  // endregion

  private val key_show_death_loc = new Object

  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    if(!sender.isInstanceOf[Player]) return true
    val player = sender.asInstanceOf[Player]
    if(label == "hob") {
      player sendMessage util.Arrays.deepToString(args.asInstanceOf[Array[AnyRef]])
      if(args(0) == "back") {
        if(!player.hasPermission("hob.back")) {
          player sendMessage (Lang getFor player).no_permission_command
        } else {
          player backTo (self.deathLocationMap get player)
        }
      }
      //todo
      true
    } else if(label == "back") {
      player backTo (self.deathLocationMap get player)
      true
    } else false
  }

  val deathLocationMap = new util.WeakHashMap[Player, Location]


  object listener extends Listener {

    @EventHandler def onPlayerDeath(event: PlayerDeathEvent) {
      val player = event.getEntity
      val loc = player.getLocation
      deathLocationMap.put(player, loc)

      if(show_death_loc) {
        runTask(player, 0, 1, key_show_death_loc) { ()=>
          player showPoint2dOn (Particle.CLOUD, loc)
        }
      }

      if(auto_respawn) {
        runTask { ()=>
          player.spigot respawn()
        }
      }
    }

    @EventHandler def onPlayerRespawn(event: PlayerRespawnEvent) {
      val player = event.getPlayer
      if(deathLocationMap.containsKey(player)) {
        if(auto_back) {
          player backTo (self.deathLocationMap get player)
        } else {
          runTask { () =>
            player open new ReSpawnGui(Lang getFor player)
          }
        }
      }
    }

    @EventHandler def onInventoryClick(e: InventoryClickEvent) {
      if(e.getInventory.getHolder.isInstanceOf[ReSpawnGui]) {
        e.getInventory.getHolder.asInstanceOf[ReSpawnGui] onInventoryClick e
      }
    }

  }
}