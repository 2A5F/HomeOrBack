package io.meowtype

import java.lang.Iterable
import java.util
import java.util.function.Function
import java.util.stream.StreamSupport
import java.util.stream.Stream
import java.util.zip.{ZipEntry, ZipInputStream}

import org.bukkit.Bukkit

package object homeorback {

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
  trait Action0 {
    def apply(): Unit
  }
  object Action0 {
    val empty: Action0 = {()=>}
  }

  class Break(val uid: java.lang.Object) extends Throwable
  class BreakR[R](uid: java.lang.Object, val r: R) extends Break(uid)

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

  def runTask(cb: Runnable) {
    Bukkit.getScheduler.scheduleSyncDelayedTask(HomeOrBack.instance, cb)
  }
}

