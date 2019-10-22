package io

import java.lang.Iterable
import java.util
import java.util.function.Function
import java.util.stream.StreamSupport
import java.util.stream.Stream
import java.util.zip.{ZipEntry, ZipInputStream}

package object meowtype {

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

  implicit class forArray[T](self: Array[T]) {
    def stream(): Stream[T] = {
      val o_arr = self.asInstanceOf[Array[T with Object]]
      util.Arrays.stream(o_arr).asInstanceOf[Stream[T]]
    }
  }

  implicit class forStream[A](self: Stream[A]) {
    private val none = new Object
    def foldSelf(op: Func2[A, A, A]): A = {
      var result: Any = none
      self forEach {x =>
        if(result == none) result = x
        else result = op(result.asInstanceOf[A], x)
      }
      if(result == none) null.asInstanceOf[A]
      else result.asInstanceOf[A]
    }
    def fold[R](first: R, op: Func2[R, A, R]) : R = {
      var result: R = first
      self forEach {x => result = op(result, x)}
      result
    }
  }

  trait Func[R] {
    def apply(): R
  }
  trait Func1[A, R] {
    def apply(a: A): R
  }
  type ->[A, R] = Func1[A, R]
  trait Func2[A, B, R] {
    def apply(a: A, b: B): R
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
          case break: Break => if(break.uid == uid) return else throw break
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
  def loopR[R](last: Func[R]): Func1[Action[Action[R]], R] = loopR[R](()=> true, last)
  def loopR[R](c: Func[Boolean], last: Func[R]): Func1[Action[Action[R]], R] = loopR[R](c, Action0.empty, last)
  def loopR[R](c: Func[Boolean], after: Action0, last: Func[R]): Func1[Action[Action[R]], R] = new Func1[Action[Action[R]], R] {
    override def apply(a: Action[Action[R]]): R = {
      val uid = new java.lang.Object
      val break: Action[R] = (r: R) => throw new BreakR[R](uid, r)
      while (c()) {
        try { a(break) }
        catch {
          case break: BreakR[R] => if(break.uid == uid) return break.r else throw break
        }
        after()
      }
      last()
    }
  }
  def loopRc[R](last: Func[R]): Func1[Action2[Action[R], Action0], R] = loopRc[R](()=> true, last)
  def loopRc[R](c: Func[Boolean], last: Func[R]): Func1[Action2[Action[R], Action0], R] = loopRc[R](c, Action0.empty, last)
  def loopRc[R](c: Func[Boolean], after: Action0, last: Func[R]): Func1[Action2[Action[R], Action0], R] = new Func1[Action2[Action[R], Action0], R] {
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
}
