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
import ReSpawnGui._

class HomeOrBack extends JavaPlugin {
  private val self = this

  override def onLoad() {
    HomeOrBack._instance = this

    getLogger info "Loaded"
  }

  override def onEnable() {
    Lang.loadLang()

    saveDefaultConfig()
    getServer.getPluginManager.registerEvents(listener, this)
    getCommand("hob") setExecutor  this
    getLogger info "Enabled"
  }

  def lang: String = getConfig.getString("lang", "en")
  def auto_respawn: Boolean = getConfig.getBoolean("auto_respawn", true)
  def back_random: Boolean = getConfig.getBoolean("back_random", true)
  def random_radius: Double = getConfig.getDouble("random_radius", 16)
  def back_once: Boolean = getConfig.getBoolean("back_once", false)
  def auto_back: Boolean = getConfig.getBoolean("auto_back", false)
  def store_location: Boolean = getConfig.getBoolean("store_location", false)

  override def onDisable() {
    getLogger info "Disabled"
  }

  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    if(sender.isInstanceOf[Player]){
      sender.asInstanceOf[Player] open new ReSpawnGui(this, Lang getFor sender.asInstanceOf[Player])
    }
    true
  }

  val deathLocationMap = new util.WeakHashMap[Player, Location]

  object listener extends Listener {

    @EventHandler def onPlayerDeath(event: PlayerDeathEvent) {
      val player = event.getEntity
      deathLocationMap.put(player, player.getLocation)
      player sendMessage "你死了 " + player.hashCode

      if(auto_respawn) {
        Bukkit.getScheduler.scheduleSyncDelayedTask(self, () => {
          player.spigot respawn()
        })
      }
    }

    @EventHandler def onPlayerRespawn(event: PlayerRespawnEvent) {
      val player = event.getPlayer
      player sendMessage "重生了 " + player.hashCode
      if(deathLocationMap.containsKey(player)) {
        player sendMessage "死亡地点: " + deathLocationMap.get(player)

        if(auto_back) {
          player backTo (self.deathLocationMap get player)
        } else {
          Bukkit.getScheduler.scheduleSyncDelayedTask(self, () => {
            player open new ReSpawnGui(self, Lang getFor player)
          })
        }
      }
    }

    @EventHandler def onInventoryClose(event: InventoryCloseEvent) {
      val player = event.getPlayer
      player sendMessage "你关闭了gui " + event.getInventory.getTitle
    }

    @EventHandler def onInventoryClick(e: InventoryClickEvent) {
      if(e.getInventory.getHolder.isInstanceOf[ReSpawnGui]) {
        e.getInventory.getHolder.asInstanceOf[ReSpawnGui] onInventoryClick e
      }
    }

  }
}
object HomeOrBack {
  private var _instance: HomeOrBack = _
  def instance: HomeOrBack = _instance
}