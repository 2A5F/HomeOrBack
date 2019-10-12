package io.meowtype.homeorback

import java.util
import java.util.regex._
import java.util.stream.Collectors

import org.bukkit._
import org.bukkit.entity._
import org.bukkit.event.inventory._
import org.bukkit.inventory._

class ReSpawnGui(lang: Lang) extends InventoryHolder {
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
      val lores = lore.split("\\\\n")
      meta setLore (util.Arrays stream lores collect Collectors.toList[String])
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