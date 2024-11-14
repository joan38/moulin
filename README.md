# Moulin

Build multi-projects with Scala CLI!

https://github.com/user-attachments/assets/f4ffd9cf-e8bb-4592-9111-33718204c51f


## Getting Started

> [!NOTE]
> Before using Moulin for your project I would recommend using [the scala CLI](https://scala-cli.virtuslab.org) to start with.  
> Once scala CLI is too limited for your project (need for multi modules, use plugins...) then this is where Moulin can help:

> [!TIP]
> This tutorial exists in [moulin-simple-example](https://github.com/joan38/moulin-simple-example). So you could also just check it out and play with it. But be careful because you'll be hooked.

If not done already, create a folder for your project and cd in it:
```sh
mkdir my-project && cd $_
```

If not done already, download a standalone Scala launcher in your project:
```sh
curl -o scala https://raw.githubusercontent.com/VirtusLab/scala-cli/main/scala-cli.sh && chmod +x scala
```

Assuming you have/want the following project structure:
```
my-project
├─ app1
├─ app2
└─ common
```
where `app1` and `app2` depend on `common`.

Create `build.scala` as follows:
```scala
#!/usr/bin/env -S ./scala shebang --workspace=.scala-build/moulin --semanticdb-sourceroot=.
//> using dep com.goyeau::moulin:0.1.0-SNAPSHOT
import com.goyeau.moulin.*

// An object that will contain the Moulin definition
object `my-project` extends Moulin:
  // An object for the Scala module in the app1 folder
  object app1 extends MyProjectModule:
    override def dependsOn = super.dependsOn :+ common

  // An object for the Scala module in the app2 folder
  object app2 extends MyProjectModule:
    override def dependsOn = super.dependsOn :+ common

  // An object for the Scala module in the common folder
  object common extends MyProjectModule

  // A trait to factor out common settings
  trait MyProjectModule extends ScalaModule:
    override def scalaVersion = "3.5.2"
```
Make it executable:
```sh
chmod +x build.scala
```

Now to compile all modules run:
```sh
./build.scala all.compile
```

Compile `common` only:
```sh
./build.scala common.compile
```

Compile `app1` and it's recursive dependencies (`common`) only:
```sh
./build.scala app1.compile
```


## Command line and options

The command line is nothing else than Scala code. This means the following works:
```sh
./build.scala 'println("Hello world")'
```

So when you run `./build.scala common.compile`, there is literaly a function `compile` on the object `common`.  
This is the same with `./build.scala all.compile`, there is a function called `all` that will run `compile` on all the modules that have `compile` defined on them.


## IDE support

Moulin supports any IDE that supports [BSP](https://build-server-protocol.github.io/). For `ScalaModule`s Moulin simply forwards scala cli's BSP server.

To setup the BSP config run the following command before openning the project in your IDE:
```sh
./build.scala bsp.setup
```


## Plugins

Here is an example with [scalac-options](https://github.com/typelevel/scalac-options):
```scala
#!/usr/bin/env -S ./scala shebang --workspace=.scala-build/moulin --semanticdb-sourceroot=.
//> using dep com.goyeau::moulin:0.1.0-SNAPSHOT
//> using dep org.typelevel::scalac-options:0.1.7
import com.goyeau.moulin.*
import org.typelevel.scalacoptions.ScalacOptions.*
import org.typelevel.scalacoptions.{ScalaVersion, ScalacOptions}

object `my-project` extends Moulin:
  object app1 extends MyProjectModule:
    override def dependsOn = super.dependsOn :+ common

  object app2 extends MyProjectModule:
    override def dependsOn = super.dependsOn :+ common

  object common extends MyProjectModule

  trait MyProjectModule extends ScalaModule:
    override def scalaVersion  = "3.5.2"
    override def scalacOptions = super.scalacOptions ++ ScalacOptions.tokensForVersion(
      ScalaVersion.unsafeFromString(scalaVersion),
      ScalacOptions.default + sourceFutureMigration ++ fatalWarningOptions
    )
```

## Extend Moulin

Let's create a Hello World function on our project:
```scala
object `my-project` extends Moulin:
  object app:
    def toto = println("Hello World")
```
We can run this function with:
```sh
./build.scala app.toto
```

Now we recommend caching functions so that they execute only if something changed in the build:
```scala
import com.goyeau.moulin.cache.Cache.cached

object `my-project` extends Moulin:
  object app extends ScalaModule:
    override def scalaVersion  = "3.5.2"

    def push = cached(assembly()): assembly =>
      println(s"Pushing: ${assembly.path}")
```
Running the `push` function will evict the cache only if `assembly()` returns a different hashCode (or if the build definition is modified).

Here is an example where we want to write to a file only if the date changed:
```scala
import java.time.LocalDate

def currentDate() = LocalDate.now()

def writeDate = cached(currentDate()): date =>
  val file = dest / "file.txt"
  println(s"New day, new date written to $file!")
  os.write(file, s"Hello, the date is $date")
  file
```


## Why Moulin?

I love Scala 3, I love Mill, I love the scala CLI. Moulin is what happens if they all make out :D
Moulin means Mill in French, so this is no surprise that Moulin's API is highly inspired from Mill.

Just like Scala CLI, you can build your projects without any dependencies, no Scala or even Java installed, nothing.
Moulin is just Scala code, so there is not much to learn apart from the framework.


## Contributing

I've open sourced this project early because I need help from you guys to make this a reality. Please let me know what you think (good or bad) and how can we improve this tool.
