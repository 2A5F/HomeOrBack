package io.meowtype.homeorback

import java.io._
import java.util
import java.util.zip._

import org.bukkit.configuration.file._
import org.bukkit.entity.Player

class Lang(val name: String, val choose_respawn: String,
           val respawn_on_spawn: String,
           val auto_choose_when_close: String,
           val respawn_near_death: String,
           val respawn_at_death: String,
           val back_near_any_time: String,
           val back_at_any_time: String)
object Lang {
  val lang_map = new util.HashMap[String, Lang]()

  def getFor(player: Player): Lang = {
    val self = HomeOrBack.instance
    var local = player.getLocale.toLowerCase
    if((lang_map containsKey local) && !(local == "zh_cn" && self.lang == "lzh_Hans")) lang_map get local
    else if(lang_map containsKey self.lang) lang_map get self.lang
    else lang_map get "en"
  }

  def loadLang(): Unit = {
    val self = HomeOrBack.instance
    val src = classOf[HomeOrBack].getProtectionDomain.getCodeSource
    val jar = src.getLocation
    val zip = new ZipInputStream(jar.openStream)

    val langs = zip filter { e =>
      val name = e.getName
      name.startsWith("lang/") && name.endsWith(".yml")
    } stream() map[Lang] { e =>
      val reader = new InputStreamReader(self.getResource(e.getName), "UTF8")
      val lang: FileConfiguration = YamlConfiguration loadConfiguration reader
      new Lang(
        e.getName substring (5, e.getName.length - 4) toLowerCase,
        lang getString "choose_respawn",
        lang getString "respawn_on_spawn",
        lang getString "auto_choose_when_close",
        lang getString "respawn_near_death",
        lang getString "respawn_at_death",
        lang getString "back_near_any_time",
        lang getString "back_at_any_time",
      )
    }

    lang_map.clear()
    langs forEach { l: Lang =>
      lang_map.put(l.name, l)
      if(l.name == "cmn_hans") lang_map.put("zh_cn", l)
    }
  }
}
