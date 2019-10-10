package io.meowtype.homeorback

import java.util

import org.bukkit._
import org.bukkit.entity._
import org.bukkit.event.inventory._
import org.bukkit.inventory._
import ReSpawnGui._

class ReSpawnGui(val self: HomeOrBack, lang: Lang) extends InventoryHolder {
  private val inv: Inventory = Bukkit.createInventory(this, 9 * 3, lang.choose_respawn)

  private val bed = setItem(9 + 2, Material.BED, lang.respawn_on_spawn, lang.auto_choose_when_close)
  private val grass = setItem(9 * 2 - 3, Material.GRASS,
    if(self.back_random.enable) lang.respawn_near_death else lang.respawn_at_death,
    if(self.back_command)
      if(self.back_random.enable) lang.back_near_any_time else lang.back_at_any_time
      else null
  )

  private def setItem(pos: Int, material: Material, name: String, lore: String): ItemStack = {
    val item = newItem(material, name, lore)
    inv.setItem(pos, item)
    item
  }

  private def newItem(material: Material, name: String, lore: String): ItemStack = {
    val item = new ItemStack(material)
    val meta = item.getItemMeta

    meta setDisplayName name

    if(lore != null) {
      val metalore = new util.ArrayList[String]
      metalore add lore
      meta setLore metalore
    }

    item setItemMeta meta
    item
  }

  override def getInventory: Inventory = inv

  def open(player: Player) {
    player openInventory inv
  }

  def onInventoryClick(e: InventoryClickEvent) {
    e setCancelled true

    val player = e.getWhoClicked.asInstanceOf[Player]
    val targetItem = e.getCurrentItem
    if(targetItem == bed) {
      runTask { () => player closeInventory() }
    } else if(targetItem == grass) {
      runTask { () => player closeInventory() }
      player backTo (self.deathLocationMap get player)
    }
  }
}
object ReSpawnGui {
  implicit class PlayOpenGui(val player: Player) {
    def open(inv: ReSpawnGui) {
      inv open player
    }
    def tpr(loc: Location) {
      Tpr.tpr(player, loc)
    }
    def backTo(loc: Location) {
      if(loc == null) {
        return //todo
      }
      val self = HomeOrBack.instance
      if(self.back_random.enable) {
        self.getLogger.info("[debug(backTo.tpr)]: " + player.toString +" try tpr to " + loc.toString)
        player tpr loc
        //self.getServer.dispatchCommand(self.getServer.getConsoleSender, "spreadplayers " + loc.getBlockX + " " + loc.getBlockZ + " 0 " + self.back_random.max + " true " + player.getPlayerListName)
      } else {
        self.getLogger.info("[debug(backTo.tp)]: " + player.toString +" tp to " + loc.toString)
        player teleport loc
      }
      self.deathLocationMap remove player
    }
  }
}