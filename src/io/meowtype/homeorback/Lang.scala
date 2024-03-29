package io.meowtype.homeorback

import java.io._
import java.util
import java.util.stream._
import java.util.zip._

import io.meowtype._

import org.bukkit.configuration.file._
import org.bukkit.entity.Player

class Lang(val name: String,
           val mapping: util.List[String],
           val when_default_override: util.List[String],
           val choose_respawn: String,
           val respawn_on_spawn: String,
           val auto_choose_when_close: String,
           val respawn_near_death: String,
           val respawn_at_death: String,
           val back_command_msg_near: String,
           val back_command_msg_at: String,
           val back_near_any_time: String,
           val back_at_any_time: String,
           val tpr_failed: String,
           val tpr_you_can_retry: Func1[Int, String],
           val tpr_cant_retry: String,
           val no_death_loc: String,
           val no_permission_command: String,
           val command_only_player: String,
           val reloaded: String,
           val kill_self: Func1[String, String],
           val help: String,
           val help_back: String,
           val help_kill_self: String,
           val help_reload: String,
          ) {
  var i = 0
  loop(()=> i < mapping.size, ()=> i += 1) { _ =>
    mapping.set(i, mapping.get(i).toLowerCase)
  }
  i = 0
  loop(()=> i < when_default_override.size, ()=> i += 1) { _ =>
    when_default_override.set(i, when_default_override.get(i).toLowerCase)
  }
}
object Lang {
  val lang_map = new util.HashMap[String, Lang]

  def getFor(player: Player): Lang = {
    val local = player.getLocale.toLowerCase
    val default = self.lang
    val default_lang = if(lang_map containsKey default) lang_map get default else lang_map get "en"
    val player_lang = lang_map get local
    if(player_lang == null || default_lang.when_default_override.contains(local) || default_lang.mapping.contains(local)) default_lang
    else player_lang
  }

  def getDefault: Lang = {
    val default = self.lang
    val default_lang = if(lang_map containsKey default) lang_map get default else lang_map get "en"
    default_lang
  }

  def loadLang(): Unit = {
    val src = classOf[HomeOrBack].getProtectionDomain.getCodeSource
    val jar = src.getLocation
    val zip = new ZipInputStream(jar.openStream)

    val langs: Stream[Lang] = zip filter { e =>
      val name = e.getName
      name.startsWith("lang/") && name.endsWith(".yml")
    } stream() map[Lang] { e: ZipEntry =>
    val reader = new InputStreamReader(self.getResource(e.getName), "UTF8")
      val lang: FileConfiguration = YamlConfiguration loadConfiguration reader
      new Lang(
        e.getName substring(5, e.getName.length - 4) toLowerCase,
        {
          val mapping = lang getStringList "mapping"
          if (mapping != null) mapping else new util.ArrayList[String]
        },
        {
          val when_default_override = lang getStringList "when_default_override"
          if (when_default_override != null) when_default_override else new util.ArrayList[String]
        },
        lang getString "choose_respawn",
        lang getString "respawn_on_spawn",
        lang getString "auto_choose_when_close",
        lang getString "respawn_near_death",
        lang getString "respawn_at_death",
        lang getString "back_command_msg_near",
        lang getString "back_command_msg_at",
        lang getString "back_near_any_time",
        lang getString "back_at_any_time",
        lang getString "tpr_failed",
        get_tpr_you_can_retry(lang getString "tpr_you_can_retry"),
        lang getString "tpr_cant_retry",
        lang getString "no_death_loc",
        lang getString "no_permission_command",
        lang getString "command_only_player",
        lang getString "reloaded",
        get_kill_self(lang getString "kill_self"),
        lang getString "help",
        lang getString "help_back",
        lang getString "help_kill_self",
        lang getString "help_reload",
      )
    }

    lang_map.clear()
    langs forEach { l: Lang =>
      lang_map.put(l.name, l)
      l.mapping forEach { m =>  lang_map.put(m, l)}
    }
  }

  def get_kill_self(msg: String): Func1[String, String] = name => msg.replaceAll("\\u00A7\\{PlayerName\\}", name)
  def get_tpr_you_can_retry(msg: String): Func1[Int, String] = count => msg.replaceAll("\\u00A7\\{LastRetry\\}", count.toString)
}
