package io.meowtype.homeorback

import java.io.File
import java.sql.{Connection, DriverManager}
import java.util
import java.util.stream.Collectors

import org.bukkit.command._
import org.bukkit._
import org.bukkit.entity._
import org.bukkit.event.entity._
import org.bukkit.event.inventory._
import org.bukkit.event.player._
import org.bukkit.event._
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.configuration.ConfigurationSection

class HomeOrBack extends JavaPlugin {

  override def onLoad() {
    self = this
    saveDefaultConfig()
    Lang.loadLang()
    Tpr.loadWorlds()
    if(store_location) loadDb()
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
    if(label == "hob") {
      if(args.length == 0 || args(0) == "help") {
        val lang = if (!sender.isInstanceOf[Player]) Lang.getDefault else Lang getFor sender.asInstanceOf[Player]
        if(sender hasPermission "hob.op") {
          lang.help_op forEach { h =>
            sender sendMessage h
          }
        }
        lang.help forEach { h =>
          sender sendMessage h
        }
      } else if(args(0) == "back") {
        if(!sender.isInstanceOf[Player]) {
          sender sendMessage Lang.getDefault.command_only_player
          return true
        }
        val player = sender.asInstanceOf[Player]
        onBack(player)
      } else if(args(0) == "reload") {
        onReload(sender)
      } else return false
      true
    } else if(label == "back") {
      if(!sender.isInstanceOf[Player]) {
        sender sendMessage Lang.getDefault.command_only_player
        return true
      }
      val player = sender.asInstanceOf[Player]
      onBack(player)
      true
    } else false
  }

  def onBack(player: Player) {
    if(!player.hasPermission("hob.back")) {
      player sendMessage (Lang getFor player).no_permission_command
    } else {
      player backTo getDeathLoc(player)
    }
  }

  def onReload(sender: CommandSender): Unit = {
    var lang = if (!sender.isInstanceOf[Player]) Lang.getDefault else Lang getFor sender.asInstanceOf[Player]
    if(!sender.hasPermission("hob.op")) {
      sender sendMessage lang.no_permission_command
      return
    }
    reloadConfig()
    Tpr.loadWorlds()
    getLogger info "Reloaded"
    lang = if (!sender.isInstanceOf[Player]) Lang.getDefault else Lang getFor sender.asInstanceOf[Player]
    sender sendMessage lang.reloaded
  }

  override def onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array[String]): util.List[String] = {
    val name = command.getName
    val list = new util.ArrayList[String]
    if(name == "hob") {
      if(args.length == 0 || args(0) == "") {
        list add "help"
        list add "back"
        if(sender hasPermission "hob.op") list add "reload"
      }
    }
    list
  }

  // region db

  var db: Connection = _
  def loadDb() {
    val dir = getDataFolder
    val dbFile = new File(getDataFolder, "data.db")
    dir.mkdir()
    Class.forName("org.sqlite.JDBC")
    db = DriverManager.getConnection("jdbc:sqlite:" + dbFile)
    loadDbDeathLoc()
  }

  def loadDbDeathLoc() {
    val stat = db.createStatement
    stat executeUpdate "create table if not exists death_loc (uuid text primary key not null, name text, world_id text not null, world_name text, x numeric not null, y numeric not null, z numeric not null)"
    stat.close()
  }

  // endregion

  //region death location map

  val deathLocationMap = new util.WeakHashMap[Player, Location]
  def addDeathLoc(player: Player, loc: Location) {
    deathLocationMap.put(player, loc)
    if(store_location) {
      if(db == null) loadDb()
      val stat = db prepareStatement "replace into death_loc values(?, ?, ?, ?, ?, ?, ?)"
      stat.setString(1, player.getUniqueId.toString)
      stat.setString(2, player.getName)
      stat.setString(3, loc.getWorld.getUID.toString)
      stat.setString(4, loc.getWorld.getName)
      stat.setDouble(5, loc.getX)
      stat.setDouble(6, loc.getY)
      stat.setDouble(7, loc.getZ)
      stat.executeUpdate()
      stat.close()
    }
  }
  def containDeathLoc(player: Player): Boolean = {
    val has = deathLocationMap containsKey player
    if(!store_location) return has
    if(has) return has
    if(db == null) loadDb()
    val stat = db prepareStatement "select count(*) from death_loc where uuid = ?"
    stat.setString(1, player.getUniqueId.toString)
    val res = stat.executeQuery()
    stat.close()
    res.getInt(1) > 0
  }
  def getDeathLoc(player: Player): Location = {
    val loc = deathLocationMap get player
    if(!store_location) return loc
    if(loc != null) return loc
    if(db == null) loadDb()
    val stat = db prepareStatement "select world_id, x, y, z from death_loc where uuid = ?"
    stat.setString(1, player.getUniqueId.toString)
    val res = stat.executeQuery()
    stat.close()
    val world_id = res.getString("world_id")
    val x = res.getDouble("x")
    val y = res.getDouble("y")
    val z = res.getDouble("z")
    if(world_id == null) return null
    val world = Bukkit getWorld world_id
    if(world == null) return null
    val nloc = new Location(world, x, y, z)
    deathLocationMap.put(player, loc)
    nloc
  }
  def removeDeathLoc(player: Player) {
    deathLocationMap remove player
    if(store_location) {
      if(db == null) loadDb()
      val stat = db prepareStatement "delete from death_loc where uuid = ?"
      stat.setString(1, player.getUniqueId.toString)
      stat.executeUpdate()
      stat.close()
    }
  }

  // endregion

  object listener extends Listener {

    @EventHandler def onPlayerDeath(event: PlayerDeathEvent) {
      val player = event.getEntity
      val loc = player.getLocation
      addDeathLoc(player, loc)

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
      if(containDeathLoc(player)) {
        if(auto_back) {
          player backTo getDeathLoc(player)
        } else {
          runTask { () =>
            player open new ReSpawnGui(Lang getFor player)
          }
        }
      }
    }

    @EventHandler def onInventoryClick(e: InventoryClickEvent) {
      if(!e.getInventory.getHolder.isInstanceOf[ReSpawnGui]) return
      e.getInventory.getHolder.asInstanceOf[ReSpawnGui] onInventoryClick e
    }

  }
}