package ch.cern.toros.io

import java.nio.ByteBuffer
import java.nio.file.{FileSystems, Path}
import java.nio.channels.{FileChannel}
import java.io.IOException

package object tmp {

  abstract class Base;

  case class Header(version: Int, begin: Int,
                    seekfree: Long, nbytesfree: Int, nfree: Int,
                    nbytesname: Int, 
                    seekinfo: Long, nbytesinfo: Int);

  class File(val path: Path) extends Base {
    private def read_header: Header = {
      fchan.read(buffer)
      buffer.rewind

      val rootid = (buffer.get.toChar :: buffer.get.toChar :: 
        buffer.get.toChar :: buffer.get.toChar :: Nil).mkString
      if (rootid != "root")
        throw new IOException(s"Invalid ROOT header: $rootid")

      // version
      val version = buffer.getInt
      val largefile: Boolean = version > 1000000

      // rest of the header
      val begin = buffer.getInt
      val header = if (largefile) {
        val end = buffer.getLong
        val seekfree = buffer.getLong
        val nbytesfree = buffer.getInt
        val nfree = buffer.getInt
        val nbytesname = buffer.getInt
        val units = buffer.get
        val compress = buffer.getInt
        val seekinfo = buffer.getLong
        val nbytesinfo = buffer.getInt

        Header(version % 1000000, begin, 
                      seekfree, nbytesfree, nfree,
                      nbytesname,
                      seekinfo, nbytesinfo)
      } else {
        val end = buffer.getInt
        val seekfree = buffer.getInt
        val nbytesfree = buffer.getInt
        val nfree = buffer.getInt
        val nbytesname = buffer.getInt
        val units = buffer.get
        val compress = buffer.getInt
        val seekinfo = buffer.getInt
        val nbytesinfo = buffer.getInt
        
        Header(version % 1000000, begin, 
                      seekfree.toLong, nbytesfree, nfree,
                      nbytesname,
                      seekinfo.toLong, nbytesinfo)
      }

      // we need to set the current position of the buffer back to 0
      buffer.rewind
      header
    }

    val fchan = FileChannel.open(path)
    val buffer = ByteBuffer.allocateDirect(300)
    private val header: Header = read_header

    def getHeader = header
    def getDir = {
      Key(buffer.slice(header.begin)).fetch(Directory)(this)
    }
  }

  trait FactoryBase {
    type FactoryProduct;

    def build(key: Key, buffer: ByteBuffer): FactoryProduct;
  }

  class Directory(val key: Key, val buffer: ByteBuffer) extends Base {
  }

  object Directory extends FactoryBase {
    type FactoryProduct = Directory

    def build(key: Key, buffer: ByteBuffer) = new Directory(key, buffer)
  }

  /*
  class TopDirectory(val buffer: ByteBuffer) extends Directory {
    case class Product(version: Int,
                       datimec: Int, datimem: Int,
                       nbyteskeys: Int, nbytesname: Int,
                       seekdir: Long, seekparent: Long, seekkeys: Long,
                       headerkey: Key,
                       keys: List[Key]);

    val dir: Product = {
      val version = 
    }
  }

  object TopDirectory extends FactoryBase {
    type FactoryProduct = TopDirectory

    def build(buffer: ByteBuffer) = new TopDirectory(buffer)
  }
  */

  case class KeyProduct(nbytes: Int, version: Int,
                        objlen: Int, datime: Int, 
                        keylen: Short, cycle: Short,
                        seekkey: Long, seekpdir: Long,
                        clssname: String, name: String, title: String);

  class Key(val buffer: ByteBuffer) extends Base {
    def build: KeyProduct = {
      // we do not use the actual buffer -> only slices
      val tmp = buffer.slice

      val nbytes = tmp.getInt
      val version = tmp.getVersion
      val objlen = tmp.getInt
      val datime = tmp.getInt
      val keylen = tmp.getShort
      val cycle = tmp.getShort
      val (seekkey, seekpdir) = if (version > 1000) {
        (tmp.getLong, tmp.getLong)
      } else {
        (tmp.getInt.toLong, tmp.getInt.toLong)
      }
      val classname = tmp.getString
      val name = tmp.getString
      val title = tmp.getString

      KeyProduct(nbytes, version, objlen, datime, keylen,
                 cycle, seekkey, seekpdir, 
                 classname, name, title)
    }

    val keyp = build

    def fetch(factory: FactoryBase)(implicit file: File): factory.FactoryProduct = {
      // allocate a new buffer
      val tmp = ByteBuffer.allocateDirect(keyp.objlen)

      // read up to the size of the object
      file.fchan.read(tmp, keyp.seekkey+keyp.keylen)
      tmp.rewind

      // build the 
      factory.build(this, tmp)
    }
  }

  object Key {
    def apply(buffer: ByteBuffer): Key = new Key(buffer)
  }

  object File {
    def read(path: String): File = apply(path)
    def apply(path: String): File = File(FileSystems.getDefault.getPath(path))
    def apply(path: Path): File = new File(path)
  }

  // read tfile
  def read_tkey(buffer: ByteBuffer): Unit = {
    val lObjCompressed = buffer.getInt
    val keyVersion = buffer.getShort
    val lObjUncompressed = buffer.getInt
    val datime = buffer.getInt
    val keyLen = buffer.getShort
    val cycle = buffer.getShort
    val p2Key = buffer.getInt
    val p2Dir = buffer.getInt
    val nBytesClassName = buffer.get
    for (i <- 0 until nBytesClassName)
      print(buffer.get.toChar)
    println("")

    val lname = buffer.get
    for (i <- 0 until lname)
      print(buffer.get.toChar)
    println("")

    val ltitle = buffer.get
    for (i <- 0 until ltitle)
      print(buffer.get.toChar)
    println("")

    println(lObjCompressed)
    println(keyVersion)
    println(lObjUncompressed)
    println(p2Key)
    println(p2Dir)
    println(s"buffer position = ${buffer.position}")
  }
  
}
