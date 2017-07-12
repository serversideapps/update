package example

case class CopyTask(
  srcPath: String,
  destPath: String
) {
  var uptodate = 0
  var changed = 0
}

object Hello extends App {
  val srcRoot = "..\\silhmojs\\"
  val destRoot = "..\\chessapp\\"
  val tasks = List(
    CopyTask(srcRoot + "server\\app\\", destRoot + "app\\"),
    CopyTask(srcRoot + "server\\public\\", destRoot + "public\\"),
    CopyTask(srcRoot + "server\\conf\\", destRoot + "conf\\"),
    CopyTask(srcRoot + "shared\\src\\main\\scala\\shared\\", destRoot + "app\\shared\\")

  )
  def WriteStringToFile(path: String, content: String) {
    org.apache.commons.io.FileUtils.writeStringToFile(
      new java.io.File(path),
      content,
      null.asInstanceOf[String]
    )
  }
  def ReadFileToString(path: String): String =
    {
      val f = new java.io.File(path)
      if (!f.exists()) return null
      org.apache.commons.io.FileUtils.readFileToString(
        f,
        null.asInstanceOf[String]
      )
    }
  def CollectFiles(path: String, dirnames: List[String] = List[String](), recursive: Boolean = false, setfiles: List[String] = List[String]()): List[String] =
    {
      val d = new java.io.File(path)

      if (!(d.exists && d.isDirectory)) return setfiles

      var allfiles = setfiles

      val all = d.listFiles
      val dirs = all.filter(_.isDirectory).filter(_.getName != "target")
      val files = all.filter(_.isFile)

      if (recursive) for (dir <- dirs) allfiles = CollectFiles(dir.getAbsolutePath, dirnames :+ dir.getName, recursive, allfiles)

      allfiles = allfiles ::: ((for (f <- files) yield (dirnames :+ f.getName).mkString(java.io.File.separator)).toList)

      allfiles
    }
  var copies = scala.collection.mutable.ArrayBuffer[String]()
  def collect(copy: Boolean = false) {
    for (task <- tasks) {
      if ((!copy) || (task.changed > 0)) {
        println("Task " + task)
        val srcs = CollectFiles(task.srcPath, recursive = true)
        for (src <- srcs) {
          val srcpath = task.srcPath + src
          val destpath = task.destPath + src
          val srcfile = new java.io.File(srcpath)
          val destfile = new java.io.File(destpath)
          val destexists = destfile.exists
          val srclastmod = srcfile.lastModified
          val destlastmod = destfile.lastModified
          val uptodate = destexists && (destlastmod >= srclastmod)
          val uptodatestr = if (uptodate) "up to date" else "changed"
          if (uptodate) task.uptodate += 1 else task.changed += 1
          if (copy) {
            if (!uptodate) {
              println(s"--> copy $destpath")
              val content = ReadFileToString(srcpath)
              //WriteStringToFile(destpath, content)
              println(s"    ${content.length} characters")
              copies += s"""copy "$srcpath" "$destpath""""
            }
          } else println(s"$uptodatestr: $destpath")
        }
      }
    }
  }

  collect()
  val del = "----------------------------------------"
  println(s"$del\nSummary\n$del")
  for (task <- tasks) {
    println(task)
    println(s"--> up to date: ${task.uptodate}, changed: ${task.changed}")
  }
  println(s"$del\nCopy\n$del")
  collect(copy = true)
  println(s"$del\nDone\n$del")

  val bat = s"""
    |copy "..\\silhmojs\\client\\target\\scala-2.11\\client-opt.js" "..\\chessapp\\public\\javascripts\\client-opt.js"
    |copy "..\\silhmojs\\client\\target\\scala-2.11\\client-jsdeps.min.js" "..\\chessapp\\public\\javascripts\\client-jsdeps.min.js"
    |${copies.mkString("\n")}
  """.stripMargin

  println(bat)

  WriteStringToFile("c.bat", bat)
}

