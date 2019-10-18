package io.meowtype

import java.util

import org.bukkit.{Bukkit, Location, Particle}
import org.bukkit.entity.Player
import org.bukkit.scheduler.{BukkitRunnable, BukkitTask}

package object homeorback {
  var self: HomeOrBack = _

  def runTask(cb: Runnable) {
    Bukkit.getScheduler.scheduleSyncDelayedTask(self, cb)
  }

  private val taskPool = new util.WeakHashMap[Player, util.HashMap[Any, AutoStopTask]]

  def runTask(player: Player, delay: Long, period: Long, key: Any): Action[Action0] = (a: Action0) => {
    val task: AutoStopTask = new AutoStopTask(new BukkitRunnable {
      override def run() {
        try {
          a()
        }
        catch {
          case ex: Throwable => {
            this.cancel()
            throw ex
          }
        }
      }
    }.runTaskTimer(self, delay, period))
    val map = if (taskPool.containsKey(player)) taskPool.get(player) else {
      val map = new util.HashMap[Any, AutoStopTask]
      taskPool.put(player, map)
      map
    }
    if (map.containsKey(key)) {
      map.get(key).finalize()
    }
    map.put(key, task)
  }

  def stopTask(player: Player, key: Any) {
    val map = if (taskPool.containsKey(player)) taskPool.get(player) else {
      val map = new util.HashMap[Any, AutoStopTask]
      taskPool.put(player, map)
      map
    }
    if (map.containsKey(key)) {
      map.get(key).finalize()
    }
  }

  private class AutoStopTask(val task: BukkitTask) {
    override def finalize( ){
      try {
        task.cancel()
      }
      catch {
        case _ : Throwable =>
      }
    }
  }

  implicit class forPlayer(val player: Player) {
    def open(inv: ReSpawnGui) {
      inv open player
    }
    def tpr(loc: RetryableLocation) {
      Tpr.tpr(player, loc)
    }
    def backTo(loc: RetryableLocation) {
      if(loc == null) {
        player.sendMessage((Lang getFor player).no_death_loc)
        return
      }
      if(self.back_random.enable) {
        player tpr loc
      } else {
        player teleport loc.loc
        self removeDeathLoc player
      }
    }
    def showPoint2dOn(loc: Location): Unit = showPoint2dOn(Particle.VILLAGER_HAPPY, loc)
    def showPoint2dOn(typ: Particle, loc: Location) {
      player.spawnParticle(typ, loc.getX, loc.getWorld.getMaxHeight / 2, loc.getZ,  loc.getWorld.getMaxHeight / 2, 0, loc.getWorld.getMaxHeight * 2, 0, 0)
    }
  }


}

