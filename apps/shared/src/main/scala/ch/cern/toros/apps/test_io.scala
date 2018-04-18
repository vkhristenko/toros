package ch.cern.toros.apps

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile

import java.nio.ByteBuffer
import java.nio.IntBuffer

import java.nio.file.FileSystems

import java.nio.channels.FileChannel

object test_io {
  def main(args: Array[String]): Unit = {
    test_java_io(args(0));
    test_java_nio(args(0));
    test_java_nio_file(args(0));

    test_root_header(args(0))
  }

  def root_header(buffer: ByteBuffer): Unit = {
    println("="*50)
    println("ROOT TFile Header:")
    println("="*50)
    // get the root id
    val r = buffer.get.toChar
    val o = buffer.get.toChar
    val oo = buffer.get.toChar
    val t = buffer.get.toChar
    println(s"header = $r$o$oo$t")

    // get file version
    val fversion = buffer.getInt
    println(s"file format version = ${fversion}")

    // fBEGIN
    val fbegin = buffer.getInt
    println(s"fbegin = ${fbegin}")

    //
    val fend = buffer.getInt
    println(s"fend = ${fend}")

    // 
    val fseekfree = buffer.getInt
    println(s"fseekfree = ${fseekfree}")

    //
    val fnbytesfree = buffer.getInt
    println(s"fnbytesfree = ${fnbytesfree}")

    //
    val nfree = buffer.getInt
    println(s"nfree = ${nfree}")

    //
    val fnbytesName = buffer.getInt
    println(s"fnbytesName = $fnbytesName")

    //
    val funits = buffer.get
    println(s"funits = $funits")

    //
    val fcompress = buffer.getInt
    println(s"fcompress = $fcompress")

    // 
    val fseekinfo = buffer.getInt
    println(s"fseekinfo = $fseekinfo")

    // 
    val fnbytesinfo = buffer.getInt
    println(s"fnbytesinfo = $fnbytesinfo")

    // next 18 bytes is uuid
    //val fuuid
    buffer.position(fbegin)
    root_key(buffer)
  }

  def root_key(buffer: ByteBuffer): Unit = {
    println("="*50)
    println("ROOT TKey: ")
    println("="*50)
    println(s"buffer current position = ${buffer.position}")

    //
    val lObjCompressed = buffer.getInt
    println(s"lObjCompressed = $lObjCompressed")

    // 
    val keyVersion = buffer.getShort
    println(s"keyVersion = $keyVersion")

    //
    val lObjUncompressed = buffer.getInt
    println(s"lObjUncompressed = $lObjUncompressed")
    
    // 
    val datime = buffer.getInt
    println(s"datime = $datime")

    // 
    val keyLen = buffer.getShort
    println(s"keyLen = $keyLen")

    //
    val cycle = buffer.getShort
    println(s"cycle = $cycle")

    //
    val p2Key = buffer.getInt
    println(s"p2Key = $p2Key")

    //
    val p2Dir = buffer.getInt
    println(s"p2Dir = $p2Dir")

    //
    val nBytesClassName = buffer.get
    println(s"nBytesClassName = $nBytesClassName")

    print("class name = ")
    for (i <- 0 until nBytesClassName)
      print(buffer.get.toChar)
    println("")

    //
    val lname = buffer.get
    println(s"lname = $lname")
    print("object name = ")
    for (i <- 0 until lname)
      print(buffer.get.toChar)
    println("")

    //
    val ltitle = buffer.get
    println(s"ltitle = $ltitle")
    print("title name = ")
    for (i <- 0 until ltitle)
      print(buffer.get.toChar)
    println("")

    println(s"buffer position = ${buffer.position}")

    val len = buffer.get
    println(s"len = $len")
    for (i <- 0 until len)
      print(buffer.get.toChar)
//      print("%02X " format buffer.get)

    println("")
    println(s"buffer position = ${buffer.position}")
    
    val len1 = buffer.get
    println(s"len1 = $len1")
    println(s"buffer position = ${buffer.position}")

    root_directory(buffer)
  }
  
  def root_directory(buffer: ByteBuffer): Unit = {
    val v = buffer.getShort
    println(s"version = $v")

    //
    val dtimec = buffer.getInt
    println(s"dtimec = $dtimec")

    // 
    val dtimem = buffer.getInt
    println(s"dtimem = $dtimem")

    // 
    val fnbyteskeys = buffer.getInt
    println(s"fnbyteskeys = $fnbyteskeys")
    
    // 
    val fnbytesname = buffer.getInt
    println(s"fnbytesname = $fnbytesname")

    //
    val fseekdir = buffer.getInt
    println(s"fseekdir = $fseekdir")

    // 
    val fseekparent = buffer.getInt
    println(s"fseekparent = $fseekparent")

    //
    val fseekkeys = buffer.getInt
    println(s"fseekkeys = $fseekkeys")

    //
    //
    //
    buffer.position(fseekkeys)
    println(s"current position = ${buffer.position}")
    
    //
    val nbytes = buffer.getInt
    println(s"nbytes = $nbytes")
  }

  def test_root_header(path: String): Unit = {
    // open a channel
    val p = FileSystems.getDefault.getPath(path)
    val ch = FileChannel.open(p)

    println(s"channel size = ${ch.size}")
    println(s"channel position = ${ch.position}") 

    // get a byte buffer and read the first 
    val bbuffer = ByteBuffer.allocateDirect(300);
    ch.read(bbuffer)
    // have to rewind the buffer back to 0
    bbuffer.rewind

    // check current position
    println(s"channel size = ${ch.size}")
    println(s"channel position = ${ch.position}") 
    println(s"bbuffer position = ${bbuffer.position}")

    root_header(bbuffer)
  }

  def dump_raw(bytes: Array[Byte], ngroups: Int = 25): Unit = {
    for (i <- 0 until 25) if (i<10) print(s"$i  ") else print(s"$i ")
    println("")
    println("-"*74)
    bytes.map({
      x: Byte => "%02X" format x
    }).grouped(ngroups).foreach({x: Array[String] => println(x.mkString(" "))})
  }

  def test_java_io(path: String): Unit = {
    val file = new File(path);

    println(s"exists = ${file.exists()}")
    println(s"isFile = ${file.isFile()}")
    println(s"length = ${file.length()}")
//    println(s"totalSpace = ${file.getTotalSpace()}")

    // testing a FileInputStream
    val inputStream = new FileInputStream(file)
    println(s"available = ${inputStream.available()}")

    // read all the bytes
    val bytes = Array.fill[Byte](inputStream.available())(0);
    val bytesRead = inputStream.read(bytes);
    println(s"bytes.size = ${bytes.size}")
    println(s"bytesRead = ${bytesRead}")
    dump_raw(bytes)

    val out_file = new File(path.stripSuffix(".root") + "_out.root")
    val outputStream = new FileOutputStream(out_file)
    outputStream.write(bytes)
    outputStream.close()

    println("="*50)

    // use Random Acess File
    val buffer = Array.fill[Byte](bytes.length)(0);
    val raFile = new RandomAccessFile(file, "r")
    val bytesRead1 = raFile.read(buffer)
    dump_raw(buffer)
  }

  def test_java_nio(path: String): Unit = {
    val bbuffer = ByteBuffer.allocateDirect(100);
    val array = Array.fill[Int](25)(10);
    val ibuffer = bbuffer.asIntBuffer().put(array)
  }
  
  def test_java_nio_file(path: String): Unit = {
    /*
    val p = FileSystems.getDefault().getPath(path)
    val ch = Files.newByteChannel(p, )
    */
    val p = FileSystems.getDefault.getPath(path)
    val ch = FileChannel.open(p)

    println(s"channel size = ${ch.size}")
    println(s"channel position = ${ch.position}")

    val bbuffer = ByteBuffer.allocateDirect(399);
    val nbytes = ch.read(bbuffer)
    println(s"nbytes = ${nbytes}")
    println(s"hasArray = ${bbuffer.hasArray}")
    if (bbuffer.hasArray)
      dump_raw(bbuffer.array())
    else {
      val arr = Array.fill[Byte](399)(0)
      println(s"position = ${bbuffer.position}")
      bbuffer.rewind;
      bbuffer.get(arr)
      dump_raw(arr)
    }
  }
}
