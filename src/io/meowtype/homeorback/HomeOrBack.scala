package io.meowtype.homeorback

import java.io.File
import java.sql.{Connection, DriverManager}
import java.util

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
    getCommand("killself") setExecutor this
    getLogger info "Enabled"
  }

  override def onDisable() {
    getLogger info "Disabled"
  }

  // region Configs

  def lang: String = getConfig.getString("lang", "en").toLowerCase

  def auto_respawn: Boolean = getConfig.getBoolean("auto_respawn", false)
  def auto_back: Boolean = getConfig.getBoolean("auto_back", false)

  def back_random: BackRandom = new BackRandom(getConfig.getConfigurationSection("back_random"))
  class BackRandom(val section: ConfigurationSection) {
    def enable: Boolean = section.getBoolean("enable", true)
    def min: Double = section.getDouble("min", 8)
    def max: Double = section.getDouble("max", 32)
    def max_try: Int = section.getInt("max_try", 100)
    def max_retry: Int = section.getInt("max_retry", 3)
    def uniform: Boolean = section.getBoolean("uniform", true)
    def try_to_land: Boolean = section.getBoolean("try_to_land", true)
    def never_water: Boolean = section.getBoolean("never_water", false)
    def try_not_leaves: Boolean = section.getBoolean("try_not_leaves", true)
  }

  def back_command: Boolean = getConfig.getBoolean("back_command", true)

  def show_death_loc: Boolean = getConfig.getBoolean("show_death_loc", false)

  def store_location: Boolean = getConfig.getBoolean("store_location", true)

  def kill_self_command: Boolean = getConfig.getBoolean("kill_self_command", true)

  // endregion

  private val key_show_death_loc = new Object

  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    if(command.getName == "hob") {
      if(args.length == 0 || args(0) == "help") {
        val lang = if (!sender.isInstanceOf[Player]) Lang.getDefault else Lang getFor sender.asInstanceOf[Player]
        if(sender hasPermission "hob.op") {
          sender sendMessage lang.help_reload
        }
        sender sendMessage lang.help
        if(back_command) sender sendMessage lang.help_back
        if(kill_self_command) sender sendMessage lang.help_kill_self
      } else if(args(0) == "back") {
        if(!back_command) return false
        if(!sender.isInstanceOf[Player]) {
          sender sendMessage Lang.getDefault.command_only_player
          return true
        }
        val player = sender.asInstanceOf[Player]
        onBack(player)
      } else if(args(0) == "reload") {
        onReload(sender)
      } else if(args(0) == "killself" || args(0) == "kills" || args(0) == "ks") {
        if(!kill_self_command) return false
        if(!sender.isInstanceOf[Player]) {
          sender sendMessage Lang.getDefault.command_only_player
          return true
        }
        val player = sender.asInstanceOf[Player]
        onKillself(player)
      } else return false
      true
    } else if(command.getName == "back") {
      if(!back_command) return false
      if(!sender.isInstanceOf[Player]) {
        sender sendMessage Lang.getDefault.command_only_player
        return true
      }
      val player = sender.asInstanceOf[Player]
      onBack(player)
      true
    } else if(command.getName == "killself") {
      if(!kill_self_command) return false
      if(!sender.isInstanceOf[Player]) {
        sender sendMessage Lang.getDefault.command_only_player
        return true
      }
      val player = sender.asInstanceOf[Player]
      onKillself(player)
      true
    } else false
  }

  def onBack(player: Player) {
    if(!player.hasPermission("hob.base.back")) {
      player sendMessage (Lang getFor player).no_permission_command
    } else {
      player backTo getDeathLoc(player)
    }
  }

  def onReload(sender: CommandSender) {
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

  def onKillself(player: Player) {
    if(!player.hasPermission("hob.base.killself")) {
      player sendMessage (Lang getFor player).no_permission_command
    } else {
      player setLastDamageCause new KillSelfEvent(player)
      player setHealth 0
    }
  }

  class KillSelfEvent(player: Player) extends EntityDamageEvent(player, EntityDamageEvent.DamageCause.SUICIDE, 0)

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
    stat executeUpdate "create table if not exists death_loc (uuid text primary key not null, name text, world_id text not null, world_name text, x numeric not null, y numeric not null, z numeric not null, retry integer not null)"
    stat.close()
  }

  // endregion

  //region death location map

  val deathLocationMap = new util.WeakHashMap[Player, RetryableLocation]
  def setDeathLoc(player: Player, loc: Location): Unit =  setDeathLoc(player, loc, 0)
  def setDeathLoc(player: Player, loc: Location, retry: Int): Unit = setDeathLoc(player, new RetryableLocation(loc, retry))
  def setDeathLoc(player: Player, loc: RetryableLocation) {
    deathLocationMap.put(player, loc)
    if(store_location) {
      if(db == null) loadDb()
      val stat = db prepareStatement "replace into death_loc values(?, ?, ?, ?, ?, ?, ?, ?)"
      stat.setString(1, player.getUniqueId.toString)
      stat.setString(2, player.getName)
      stat.setString(3, loc.loc.getWorld.getUID.toString)
      stat.setString(4, loc.loc.getWorld.getName)
      stat.setDouble(5, loc.loc.getX)
      stat.setDouble(6, loc.loc.getY)
      stat.setDouble(7, loc.loc.getZ)
      stat.setInt(8, loc.retry)
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
  def getDeathLoc(player: Player): RetryableLocation = {
    val loc = deathLocationMap get player
    if(!store_location) return loc
    if(loc != null) return loc
    if(db == null) loadDb()
    val stat = db prepareStatement "select world_id, x, y, z, retry from death_loc where uuid = ?"
    stat.setString(1, player.getUniqueId.toString)
    val res = stat.executeQuery()
    stat.close()
    val world_id = res.getString("world_id")
    val x = res.getDouble("x")
    val y = res.getDouble("y")
    val z = res.getDouble("z")
    val retry = res.getInt("retry")
    if(world_id == null) return null
    val world = Bukkit getWorld world_id
    if(world == null) return null
    val n_loc = new Location(world, x, y, z)
    val nr_loc = new RetryableLocation(n_loc, retry)
    deathLocationMap.put(player, nr_loc)
    nr_loc
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
      setDeathLoc(player, loc)

      if(player.getLastDamageCause.isInstanceOf[KillSelfEvent]) {
        val lang = Lang.getDefault
        event.setDeathMessage(lang.kill_self(player.getName))
      }

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

class RetryableLocation(val loc: Location, var retry: Int) {
  def this(loc: Location) = this(loc, 0)
}