package ch.cern.toros

// java.nio imports
import java.nio.ByteBuffer
import java.nio.file.FileSystems
import java.nio.channels.FileChannel

// java.io imports
import java.io.IOException

package object io {

  abstract class PObject;
  case class PDatime(raw: Int) extends PObject;
  object PDatime {
    def build(buffer: ByteBuffer): PDatime = {
      new PDatime(buffer.getInt)
    }
  }
  case class PFileHeader(
    version: Int,
    begin: Int,
    end: Long,
    seekfree: Long,
    nbytesfree: Int,
    nfree: Int,
    nbytesname: Int,
    units: Byte,
    compress: Int,
    seekinfo: Long,
    nbytesinfo: Int
  ) extends PObject {
    override def toString = s"""
Product File Header:
--------------------
version = ${version}
begin = ${begin}
end = ${end}
seekfree = ${seekfree}
nbytesfree = ${nbytesfree}
nfree = ${nfree}
nbytesname = ${nbytesname}
units = ${units}
compress = ${compress}
seekinfo = ${seekinfo}
nbytesinfo = ${nbytesinfo}
"""
  }
  case class PKey(
    totalBytes: Int,
    version: Int,
    objBytes: Int,
    datime: PDatime,
    keyBytes: Short,
    cycle: Short,
    seekKey: Long,
    seekPDir: Long,
    className: String,
    objName: String,
    objTitle: String
  ) extends PObject {
    override def toString = s"""
Product Key:
------------
totalBytes = ${totalBytes} (keyBytes + objBytes, unless compressed)
version    = ${version}
objBytes   = ${objBytes}
datime     = ${datime}
keyBytes   = ${keyBytes}
cycle      = ${cycle}
seekKey    = ${seekKey}
seekPDir   = ${seekPDir}
className  = ${className}
objName    = ${objName}
objTitle   = ${objTitle}
"""
  }

  // 
  // factories
  //
  object PKey {
    def build(buffer: ByteBuffer): PKey = {
      val nbytes = buffer.getInt
      val version = buffer.getVersion
      val objBytes = buffer.getInt
      val dt = PDatime.build(buffer)
      val keyBytes = buffer.getShort
      val cycle = buffer.getShort
      val (seekKey, seekPDir) = 
        if (version > 1000) (buffer.getLong, buffer.getLong)
        else (buffer.getInt.toLong, buffer.getInt.toLong)
      val className = buffer.getString
      val objName = buffer.getString
      val objTitle = buffer.getString
      new PKey(nbytes, version, objBytes,
               dt, keyBytes, cycle,
               seekKey, seekPDir, 
               className, objName,objTitle)
    }
  }
  object PFileHeader {
    def build(buffer: ByteBuffer): PFileHeader = {
      // first 4 bytes are "root"
      val rootid = (buffer.get.toChar :: buffer.get.toChar :: 
        buffer.get.toChar :: buffer.get.toChar :: Nil).mkString
      if (rootid != "root")
        throw new IOException(s"Invalid ROOT header: $rootid")

      // version
      val version = buffer.getInt
      val isLargeFile = version > 1000000

      // rest of the header
      val begin = buffer.getInt
      if (isLargeFile) {
        val end = buffer.getLong
        val seekfree = buffer.getLong
        val nbytesfree = buffer.getInt
        val nfree = buffer.getInt
        val nbytesname = buffer.getInt
        val units = buffer.get
        val compress = buffer.getInt
        val seekinfo = buffer.getLong
        val nbytesinfo = buffer.getInt
        new PFileHeader(version, begin, end,
                        seekfree, nbytesfree, nfree,
                        nbytesname, units, compress, seekinfo, nbytesinfo)
      } else {
        val end = buffer.getInt.toLong
        val seekfree = buffer.getInt.toLong
        val nbytesfree = buffer.getInt
        val nfree = buffer.getInt
        val nbytesname = buffer.getInt
        val units = buffer.get
        val compress = buffer.getInt
        val seekinfo = buffer.getLong
        val nbytesinfo = buffer.getInt
        new PFileHeader(version, begin, end,
                        seekfree, nbytesfree, nfree,
                        nbytesname, units, compress, seekinfo, nbytesinfo)
      }
    }
  }

  case class PDirectory(
    version: Int,
    datimec: PDatime,
    datimem: PDatime,
    nbyteskeys: Int,
    nbytesname: Int,
    seekdir: Long,
    seekparent: Long,
    seekkeys: Long
  ) extends PObject {
    override def toString = s"""
Product Directory:
------------------
version    = ${version}
datetimec  = ${datimec}
datimem    = ${datimem}
nbyteskeys = ${nbyteskeys}
nbytesname = ${nbytesname}
seekdir    = ${seekdir}
seekparent = ${seekparent}
seekkeys   = ${seekkeys}
"""
  }
  object PDirectory {
    def build(buffer: ByteBuffer) = {
      val version = buffer.getVersion
      val datimec = PDatime.build(buffer)
      val datimem = PDatime.build(buffer)
      val nbyteskeys = buffer.getInt
      val nbytesname = buffer.getInt
      val (seekdir, seekparent, seekkeys) = 
        if (version > 1000)
          (buffer.getLong, buffer.getLong, buffer.getLong)
        else 
          (buffer.getInt.toLong, buffer.getInt.toLong, buffer.getInt.toLong)
      new PDirectory(version, datimec, datimem,
                     nbyteskeys, nbytesname,
                     seekdir, seekparent, seekkeys)
    }
  }


  case class PNamed(name: String, title: String) extends PObject;
  object PNamed {
    def build(buffer: ByteBuffer) = {
      new PNamed(buffer.getString, buffer.getString)
    }
  }

  def lldump(path: String): Unit = {
    val (buffer, fch) = setup(path)
    // pretty printing
    def print(pkey: PKey, level: Int = 0): Unit = {
      val offset = "--"*level + " "
      println(s"$offset| objName=${pkey.objName} className=${pkey.className} at:${pkey.seekKey} size:${pkey.totalBytes}")
    }

    // recurse into sub directories if there are any
    def recurse(pdir: PDirectory, level: Int = 0): Unit = {
      // read from the channel
      val buf = ByteBuffer.allocateDirect(pdir.nbyteskeys)
      fch.read(buf, pdir.seekkeys)
      buf.rewind

      // tlist key
      val tlistKey = PKey.build(buf)
      // nkeys
      val nKeys = buf.getInt
      // collectd all of them
      val keys = for (i <- 0 until nKeys) yield PKey.build(buf)
      // for each, print the info and recurse for directories
      for (key <- keys) {
        print(key, level)
        if (key.className=="TDirectory") {
          // given a key
          // allocate a buffer for the data record
          val buf1 = ByteBuffer.allocateDirect(key.totalBytes)
          fch.read(buf1, key.seekKey)
          buf1.rewind

          val tkey = PKey.build(buf1)
          val ppdir = PDirectory.build(buf1)
          recurse(ppdir, level+1)
        }
      }
    }

    val header = PFileHeader.build(buffer.slice)

    // assume that initial buffer is able to contain the top dir key + tnamed
    val slice = buffer.slice; slice.position(header.begin)
    val topDirKey = PKey.build(slice)
    val tnamed = PNamed.build(slice)

    // assume that initial buffer is able to contain the top level directory
    val pdir = PDirectory.build(slice)
    print(topDirKey, 0)
    recurse(pdir)
  }

  // trivial decompression
  import java.unit.zip.Inflater
  def unzip(key: PKey, buffer: ByteBuffer): ByteBuffer = {
    val new_buffer = ByteBuffer.allocateDirect(key.objBytes)
    val zipped_size = key.totalBytes - key.keyBytes
  }

  // for simple cli debugging
  def setup(path: String, bsize: Int = 1000): (ByteBuffer, FileChannel) = {
    val p = FileSystems.getDefault.getPath(path)
    val ch = FileChannel.open(p)
    val buffer = ByteBuffer.allocateDirect(bsize)
    ch.read(buffer)
    buffer.rewind.limit(if (bsize.toLong > ch.size) ch.size.toInt else bsize)
    (buffer, ch)
  }

  // enhance all the buffers
  implicit class ByteBufferWrapper(buffer: ByteBuffer) {
    def slice(start: Int, end: Int): ByteBuffer = 
      buffer.slice.position(start).limit(end).asInstanceOf[ByteBuffer]

    def slice(from: Int): ByteBuffer = 
      buffer.slice.position(from).asInstanceOf[ByteBuffer]

    def toRawString(ngroups: Int = 25): String = {
      def dump(bytes: Array[Byte]): String = {
        val header = (for (i <- 0 until ngroups) yield ("%02d" format i)).mkString(" ");
        val dump = bytes.map({
          x: Byte => "%02X" format x
        }).grouped(ngroups).map(_.mkString(" ")).mkString("\n")
        header + "\n" + "="*(ngroups*2 + ngroups/2) + "\n" + dump;
      }
      
      val out = if (buffer.hasArray) 
        dump(buffer.array)
      else {
        val array = Array.fill[Byte](buffer.remaining)(0)
        buffer.get(array)
        dump(array)
      }
      buffer.rewind
      out
    }

    def getVersion: Int = {
      val v = buffer.getShort
      if ((v & 0x4000) == 0) 
        v.toInt
      else {
        val nbytes: Int = ((v & 0x4fff) << 16) + buffer.getShort
        nbytes
      }
    }

    def getString: String = {
      val size = buffer.get
      if (size == 0) return ""
      val newsize = 
        if (size == -1) 
          buffer.getInt
        else if (size < 0) 
          256 + size
        else 
          size  
      
      (for (i <- 0 until newsize) yield buffer.get.toChar).mkString
    }

    def putString(str: String): ByteBuffer = {
      buffer.put(str.length.toByte).put(str.toArray.map(_.toByte))
    }
  }
}

