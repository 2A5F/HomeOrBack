package io.meowtype.homeorback

import org.bukkit.block.BlockFace
import org.bukkit.{ChatColor, Location, Material}
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

    var t = 0

    loop(()=> t < 500, ()=> t += 1) { break =>
      val r = Math.random * (max - min) + min
      val a = Math.random * 360
      val x = Math.round(sx + r * Math.cos(a * Math.PI / 180))
      val z = Math.round(sz + r * Math.sin(a * Math.PI / 180))
      val y = world.getHighestBlockYAt(x.toInt, z.toInt)
      val loc = new Location(world, x + 0.5, y, z + 0.5)

      def toCheck(loc: Location): Boolean = {
        val a = loc.getBlock
        val an1 = a.getRelative(BlockFace.DOWN)
        val a1 = a.getRelative(BlockFace.UP)
        val a2 = a1.getRelative(BlockFace.UP)
        (a.getType.isSolid || a.getType == Material.WATER) &&
          (a1.getType == Material.AIR || a1.getType == Material.WATER) &&
          a2.getType == Material.AIR &&
          an1.getType.isSolid || an1.getType == Material.WATER
      }

      if(loc.getBlock.getType == Material.BEDROCK && loc.getBlockY >= 100) { // like world_nether
        var y = 5
        val maxHeight = world.getMaxHeight
        loop(()=> y < maxHeight, ()=> y += 1) { break =>
          val loc = new Location(world, x + 0.5, y, z + 0.5)
          if(toCheck(loc)) break()
        }
      }


    }
  }
}
