package io.meowtype.homeorback

import java.util

import org.bukkit.block.BlockFace
import org.bukkit.{ChatColor, Location, Material, Particle, WorldBorder}
import org.bukkit.entity.Player
import org.bukkit.util.Vector

object Tpr {

  private val key_debug_show_randomPointWithinTheRing = new Object
  def debug_show_randomPointWithinTheRing(player: Player, target: Location, try_count: Int) {
    val min: Double = self.back_random.min
    val max: Double = self.back_random.max
    if(min <= 0) new Exception("Config Error: [back_random.min] must be > 0")
    if(max <= min) new Exception("Config Error: [back_random.max] must be > [back_random.min]")
    val world = target.getWorld
    val sx = target.getBlockX
    val sz = target.getBlockZ
    val points = new util.ArrayList[Location]
    var i = 0
    loop(()=> i < try_count, ()=> i += 1) { _ =>
      val vec = if(self.back_random.uniform) uniformRandomPointWithinTheRing(sx, sz, min, max) else randomPointWithinTheRing(sx, sz, min, max)
      val loc = vec toLocation world
      points.add(loc)
    }
    runTask(player, 0, 1, key_debug_show_randomPointWithinTheRing) { ()=>
      points.forEach { loc =>
        player.spawnParticle(Particle.VILLAGER_HAPPY, loc.getX, 200, loc.getZ,  1, 0, 0, 0, 0)
      }
    }
  }
  def debug_clear_randomPointWithinTheRing(player: Player) {
    stopTask(player, key_debug_show_randomPointWithinTheRing)
  }

  def randomPointWithinTheRing(sx: Double, sz: Double, min: Double, max: Double): Vector = {
    val r = Math.random * (max - min) + min
    val θ = Math.random * 2 * Math.PI
    val x = Math.round(sx + r * Math.cos(θ))
    val z = Math.round(sz + r * Math.sin(θ))
    new Vector(x + 0.5, 0, z + 0.5)
  }

  def uniformRandomPointWithinTheRing(sx: Double, sz: Double, min: Double, max: Double): Vector = {
    val Max = Math.pow(max, 2)
    val Min = Math.pow(min, 2)
    val r = Math.sqrt(Math.random * (Max - Min) + Min)
    val θ = Math.random * 2 * Math.PI
    val x = Math.round(sx + r * Math.cos(θ))
    val z = Math.round(sz + r * Math.sin(θ))
    new Vector(x + 0.5, 0, z + 0.5)
  }

  def tpr(player: Player, target: Location) {
    val min: Double = self.back_random.min
    val max: Double = self.back_random.max
    if(min <= 0) new Exception("Config Error: [back_random.min] must be > 0")
    if(max <= min) new Exception("Config Error: [back_random.max] must be > [back_random.min]")
    val world = target.getWorld
    val sx = target.getBlockX
    val sz = target.getBlockZ

    var t = 0
    val max_try = self.back_random.max_try
    if(max_try < 10)  new Exception("Config Error: [back_random.max_try] must be >= 10")
    val loc = loopRc[Location](()=> t < max_try, ()=> t += 1, ()=> null) { (break: Action[Location], continue: Action0) =>
      val vec = if(self.back_random.uniform) uniformRandomPointWithinTheRing(sx, sz, min, max) else randomPointWithinTheRing(sx, sz, min, max)
      val y = world.getHighestBlockYAt(vec.getBlockX, vec.getBlockZ)
      val x = vec.getX
      val z = vec.getZ
      val loc = vec.setY(y) toLocation world

      def toCheck(loc: Location): Boolean = {
        val a1 = loc.getBlock
        val a = a1.getRelative(BlockFace.DOWN)
        val a2 = a1.getRelative(BlockFace.UP)

        (a.getType.isSolid || a.getType == Material.WATER || a.getType == Material.STATIONARY_WATER) &&
          (a1.getType == Material.AIR || a1.getType == Material.WATER || a1.getType == Material.STATIONARY_WATER) &&
          a2.getType == Material.AIR
      }

      val maxHeight = loc.getBlockY
      val centerY = maxHeight / 2
      if(loc.getBlock.getRelative(BlockFace.DOWN).getType == Material.BEDROCK && loc.getBlockY >= 100) { // like world_nether
        val loc1 = new Location(world, x, centerY, z)
        if(toCheck(loc1)) break(loc1)
        var y = 1
        val loc: Location = loopR[Location](()=> y < centerY - 5, ()=> y += 1, ()=> null) { break: Action[Location] =>
          val loc = new Location(world, x, centerY + y, z)
          if(toCheck(loc)) break(loc)
          val loc2 = new Location(world, x, centerY - y, z)
          if(toCheck(loc2)) break(loc2)
        }
        if(loc == null) continue() //continue
        break(loc)
      }

      if(toCheck(loc) && !isOutsideOfBorder(loc)) break(loc)
    }

    if(loc == null) {
      player sendMessage (Lang getFor player).tpr_failed
    } else {
      player teleport loc
    }
  }

  def isOutsideOfBorder(loc: Location): Boolean = {
    val border = loc.getWorld.getWorldBorder
    val x = loc.getX
    val z = loc.getZ
    val size = border.getSize
    val x_center = border.getCenter.getX
    val z_center = border.getCenter.getZ
    (x > size + x_center || -x > size - x_center) || (z > size + z_center || -z > size - z_center)
  }
}
