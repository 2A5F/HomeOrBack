package io.meowtype

import java.io.InputStream
import java.lang.Iterable
import java.util
import java.util.function.Function
import java.util.stream.StreamSupport
import java.util.stream.Stream
import java.util.zip.{InflaterInputStream, ZipEntry, ZipInputStream}

import javax.rmi.CORBA.Util

package object homeorback {

  implicit class Iterable2Stream[T](self: Iterable[T]) {
    def stream(): Stream[T] = StreamSupport.stream[T](self.spliterator(), false)
  }

  implicit class filter4ZipInputStream(zip: ZipInputStream) {
    def filter(cb: Function[ZipEntry, Boolean]): Iterable[ZipEntry] = {
      val list = new util.ArrayList[ZipEntry]
      var isdo = true
      while (isdo) {
        val e = zip.getNextEntry
        if (e != null) {
          if(cb(e)) list add e
        } else isdo = false
      }
      list
    }
  }
}

