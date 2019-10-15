package io.meowtype

import java.lang.Iterable
import java.util
import java.util.function.Function
import java.util.stream.StreamSupport
import java.util.stream.Stream
import java.util.zip.{ZipEntry, ZipInputStream}

import org.bukkit.{Bukkit, Location, Particle}
import org.bukkit.entity.Player
import org.bukkit.scheduler.{BukkitRunnable, BukkitTask}

package object homeorback {
  var self: HomeOrBack = _

  implicit class Iterable2Stream[T](self: Iterable[T]) {
    def stream(): Stream[T] = StreamSupport.stream[T](self.spliterator(), false)
  }

  implicit class filter4ZipInputStream(zip: ZipInputStream) {
    def filter(cb: Function[ZipEntry, Boolean]): Iterable[ZipEntry] = {
      val list = new util.ArrayList[ZipEntry]
      loop() { break =>
        val e = zip.getNextEntry
        if(e == null) break()
        if(cb(e)) list add e
      }
      list
    }
  }

  trait Func[R] {
    def apply(): R
  }
  trait Action[A] {
    def apply(a: A): Unit
  }
  trait Action2[A, B] {
    def apply(a: A, b: B): Unit
  }
  trait Action0 {
    def apply(): Unit
  }
  object Action0 {
    val empty: Action0 = {()=>}
  }

  class Break(val uid: java.lang.Object) extends Throwable
  class BreakR[R](uid: java.lang.Object, val r: R) extends Break(uid)
  class Continue(val uid: java.lang.Object) extends Throwable

  def loop(): Action[Action[Action0]] = loop(()=> true)
  def loop(c: Func[Boolean]): Action[Action[Action0]] = loop(c, Action0.empty)
  def loop(c: Func[Boolean], after: Action0): Action[Action[Action0]] = loop(c, after, Action0.empty)
  def loop(c: Func[Boolean], after: Action0, last: Action0): Action[Action[Action0]] = new Action[Action[Action0]] {
    override def apply(a: Action[Action0]) {
      val uid = new java.lang.Object
      val break: Action0 = () => throw new Break(uid)
      while (c()) {
        try { a(break) }
        catch {
          case break: Break => if(break.uid == uid) return
        }
        after()
      }
      last()
    }
  }
  def loopC(): Action[Action2[Action0, Action0]] = loopC(()=> true)
  def loopC(c: Func[Boolean]): Action[Action2[Action0, Action0]] = loopC(c, Action0.empty)
  def loopC(c: Func[Boolean], after: Action0): Action[Action2[Action0, Action0]] = loopC(c, after, Action0.empty)
  def loopC(c: Func[Boolean], after: Action0, last: Action0): Action[Action2[Action0, Action0]] = new Action[Action2[Action0, Action0]] {
    override def apply(a: Action2[Action0, Action0]) {
      val uid = new java.lang.Object
      val break: Action0 = () => throw new Break(uid)
      val continue: Action0 = () => throw new Continue(uid)
      while (c()) {
        try { a(break, continue) }
        catch {
          case break: Break => if(break.uid == uid) return else throw break
          case continue: Continue => if(continue.uid != uid) throw continue
        }
        after()
      }
      last()
    }
  }
  def loopR[R](last: Func[R]): Function[Action[Action[R]], R] = loopR[R](()=> true, last)
  def loopR[R](c: Func[Boolean], last: Func[R]): Function[Action[Action[R]], R] = loopR[R](c, Action0.empty, last)
  def loopR[R](c: Func[Boolean], after: Action0, last: Func[R]): Function[Action[Action[R]], R] = new Function[Action[Action[R]], R] {
    override def apply(a: Action[Action[R]]): R = {
      val uid = new java.lang.Object
      val break: Action[R] = (r: R) => throw new BreakR[R](uid, r)
      while (c()) {
        try { a(break) }
        catch {
          case break: BreakR[R] => if(break.uid == uid) return break.r
        }
        after()
      }
      last()
    }
  }
  def loopRc[R](last: Func[R]): Function[Action2[Action[R], Action0], R] = loopRc[R](()=> true, last)
  def loopRc[R](c: Func[Boolean], last: Func[R]): Function[Action2[Action[R], Action0], R] = loopRc[R](c, Action0.empty, last)
  def loopRc[R](c: Func[Boolean], after: Action0, last: Func[R]): Function[Action2[Action[R], Action0], R] = new Function[Action2[Action[R], Action0], R] {
    override def apply(a: Action2[Action[R], Action0]): R = {
      val uid = new java.lang.Object
      val break: Action[R] = (r: R) => throw new BreakR[R](uid, r)
      val continue: Action0 = () => throw new Continue(uid)
      while (c()) {
        try { a(break, continue) }
        catch {
          case break: BreakR[R] => if(break.uid == uid) return break.r else throw break
          case continue: Continue => if(continue.uid != uid) throw continue
        }
        after()
      }
      last()
    }
  }

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
    def tpr(loc: Location) {
      Tpr.tpr(player, loc)
    }
    def backTo(loc: Location) {
      if(loc == null) {
        player.sendMessage((Lang getFor player).no_death_loc)
        return
      }
      if(self.back_random.enable) {
        player tpr loc
      } else {
        player teleport loc
      }
      self removeDeathLoc player
    }
    def showPoint2dOn(loc: Location): Unit = showPoint2dOn(Particle.VILLAGER_HAPPY, loc)
    def showPoint2dOn(typ: Particle, loc: Location) {
      player.spawnParticle(typ, loc.getX, loc.getWorld.getMaxHeight / 2, loc.getZ,  loc.getWorld.getMaxHeight / 2, 0, loc.getWorld.getMaxHeight * 2, 0, 0)
    }
  }


}

