package io.meowtype.homeorback

import org.bukkit.block.BlockFace
import org.bukkit.{ChatColor, Location, Material, WorldBorder}
import org.bukkit.entity.Player

object Tpr {
  def tpr(player: Player, target: Location) {
    val self = HomeOrBack.instance
    val min: Double = self.back_random.min
    val max: Double = self.back_random.max
    if(min <= 0) new Exception("Config Error: [back_random.min] mast be > 0")
    if(max <= min) new Exception("Config Error: [back_random.max] mast be > [back_random.min]")
    val world = target.getWorld
    val sx = target.getBlockX
    val sz = target.getBlockZ

    self.getLogger.info("[debug(tpr.base)]: range:(" + min + ", " + max + ") [" + world.toString + "] target:(" + sx + " " + sz + ")")

    var t = 0
    val loc = loopR[Location](()=> t < 500, ()=> t += 1, ()=> null) { break: Action[Location] =>
      val r = Math.random * (max - min) + min
      val a = Math.random * 360
      val x = Math.round(sx + r * Math.cos(a * Math.PI / 180))
      val z = Math.round(sz + r * Math.sin(a * Math.PI / 180))
      val y = world.getHighestBlockYAt(x.toInt, z.toInt)
      val loc = new Location(world, x + 0.5, y, z + 0.5)

      self.getLogger.info("[debug(tpr.loop" + t + ")]: r:[" + r + "] a:[" + a + "] loc:(" + x + ", " + y + ", " + z + ")")

      def toCheck(loc: Location): Boolean = {
        val a1 = loc.getBlock
        val a = a1.getRelative(BlockFace.DOWN)
        val a2 = a1.getRelative(BlockFace.UP)

        self.getLogger.info("[debug(tpr.loop" + t + ".check)]: [" + a.getType.toString + "]; [" + a1.getType.toString + "]; [" + a2.getType.toString + "]")

        (a.getType.isSolid || a.getType == Material.WATER || a.getType == Material.STATIONARY_WATER) &&
          (a1.getType == Material.AIR || a1.getType == Material.WATER || a1.getType == Material.STATIONARY_WATER) &&
          a2.getType == Material.AIR
      }

      val maxHeight = loc.getBlockY
      if(loc.getBlock.getRelative(BlockFace.DOWN).getType == Material.BEDROCK && loc.getBlockY >= 100) { // like world_nether
        var y = 5
        val loc: Location = loopR[Location](()=> y < maxHeight - 5, ()=> y += 1, ()=> null) { break: Action[Location] =>
          val loc = new Location(world, x + 0.5, y, z + 0.5)

          self.getLogger.info("[debug(tpr.loop" + t + ".loop" + y + ")]: loc:[" + loc.toString + "]")

          if(toCheck(loc)) break(loc)
        }
        if(loc == null) return //continue
        break(loc)
      }

      if(toCheck(loc) && !isOutsideOfBorder(loc)) break(loc)
    }

    if(loc == null) {
      player sendMessage (Lang getFor player).tpr_failed
      self.getLogger.info("[debug(tpr.failed)]: " + player.toString + " player tpr failed" )
    } else {
      player teleport loc
      self.getLogger.info("[debug(tpr.tp)]: " + player.toString + " player tpr to [" + loc.toString +"]" )
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
