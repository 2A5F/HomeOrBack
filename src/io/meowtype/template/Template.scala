package io.meowtype.template

import java.util
import java.util.stream._

import io.meowtype._

object Template {

  class |[A, B](val value: Any)
  object | {
    def <<[A, B](b: B): |[A, B] = {
      new |[A, B](b)
    }
  }
  implicit class ofA[A](a: A) {
    def >>[B](o: |.type): |[A, B] = {
      new |[A, B](a)
    }
  }

  class Param(val key: String, val fn: String -> String)

  class ParamMap extends util.HashMap[String, String | ParamMap]

  def gen(code: String): ParamMap -> String = {
    val list = new util.ArrayList[String | Param]()

    def replaceBase(str: String): String = {
      str.replaceAll("\\\\§", "§")
    }

    var i = 0
    var l = 0
    var inParam = false
    var name: String = null
    loop(()=> i < code.length, ()=> i += 1, { ()=>
      if(l != i) list add (replaceBase(code.substring(l, i)) >> |)
    }:Action0) { _ =>
      if(inParam) {
        if(code.charAt(i) == '}') {
          if(name != null) {
            val default = code.substring(l, i).trim
            list add (| << new Param(name, { v: String => if(v == null) default else v }))
            name = null
          } else {
            list add (| << new Param(name, { v: String => v }))
          }
          l = i + 1
          inParam = false
        } else if(code.charAt(i) == ',') {
          if(name == null) {
            val n = code.substring(l, i).trim
            name = n
            l = i + 1
          }
        }
      } else {
        if(code.charAt(i) == '§' && code.charAt(i - 1) != '\\' && i + 1 < code.length && code.charAt(i + 1) == '{') {
          inParam = true
          list add (replaceBase(code.substring(l, i)) >> |)
          i += 1
          l = i + 1
        }
      }
    }

    { params =>
      list.stream map[String] { p =>
        if (!p.value.isInstanceOf[String]) {
          val fn = p.value.asInstanceOf[Param]
          val last: String | ParamMap = fn.key split "\\." stream() fold[String | ParamMap](| << params, (old: String | ParamMap, now: String) => {
            if (old == null || old.value == null) null
            else if (!old.value.isInstanceOf[String]) {
              val p_map = old.value.asInstanceOf[ParamMap]
              p_map.get(now)
            } else old.value.asInstanceOf[String] >> |
          })
          if (last == null || !last.value.isInstanceOf[String]) fn fn null
          else fn fn last.value.asInstanceOf[String]
        } else {
          p.value.asInstanceOf[String]
        }
      } collect Collectors.joining
    }
  }

}
