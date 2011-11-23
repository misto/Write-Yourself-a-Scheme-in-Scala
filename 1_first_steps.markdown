---
layout: default
---

## First Steps: Compiling and Running

Before we can start programming in Scala, we need to install *Scala*, that is,
the compiler and the standard library. We won't be using the features of any
specific IDE in this tutorial, Scala is supported by all major IDE platforms, so
pick your favorite one. You can also write the code in a text editor and compile
and run your programs from a shell (command line). Whatever you do, you also
need to have a JDK installed.

If you have no strong preference for any specific IDE or editor, I'd recommend
the Scala IDE for [Eclipse](http://www.scala-ide.org/). It already comes with
the Scala compiler and standard library, so you don't need to install them
separately. All you need is Eclipse Helios or later and the [Scala IDE
plug-in](http://download.scala-ide.org/). Here are some alternatives if you
prefer to work with a different IDE:

* The general Scala [Setup & Getting Started](http://www.scala-lang.org/node/201) guide.
* Installation instructions for
  [NetBeans](http://wiki.netbeans.org/Scala#Get_Started).
* Getting Started with [IntelliJ IDEA Scala
  Plugin](http://confluence.jetbrains.net/display/SCA/Getting+Started+with+IntelliJ+IDEA+Scala+Plugin).
* [ENSIME](http://aemon.com/file_dump/ensime_manual.html#tth_sEc1.1) for Emacs.

Now it's time for your first Scala program so you can check if your setup was
correct. Create a new file ending in ".scala" with the following content:

{% highlight scala %}
package chapter1

object HelloWorld {
  def main(args: Array[String]): Unit = {
    println("Hello, " + args(0))
  }
}
{% endhighlight %}

Let's go through this code. The first line specifies that we create a package
named `chapter1` to contain our first example. Packages describe a hierarchical
organization of the code and are used to structure your code at large.

If you're coming from Java, you're already familiar with packages, but the next
declaration might be surprising: an object named `HelloWorld`. An object is
almost like a class, except that the compiler automatically creates one single
instance -- that's why their full name is *singleton objects*, like the infamous
design pattern -- for you and that you cannot create new instances of it.
Objects supersede Java and C++'s static methods and variables, because all use
cases of statics are covered by `objects`, but it simplifies the language. If
you prefer, you can think of all the methods and variables inside an object as
being static. Objects can also extend other classes, implement interfaces and be
used just like any other intance of a class. The *static* analogy can be helpful
at the beginning, but try to think of an object as a combined class declaration
and instantiation.

The entry point for every Scala program is the `main` method, which is our only
declaration in the `HelloWorld` object. Method declarations in Scala start with
the `def` keyword, followed by the name of the method. Next comes the list of
parameters, which is an `Array` of `Strings` -- the arguments the user gives
when running our program. Scala makes the names of variables and methods more
prominent by putting the type after the name. Analog, the result type of the
method follows after the list of parameters. An equality sign separates the
signature and the body of the method.

Our main-method simply prints the first argument of the args array to the
command line. The `println` method looks like a *free* function, but it's
actually defined in the `Predef` object, whose members are all automatically
imported by Scala. The result type of `println` is `Unit`, which matches the
result type of the main method. The `Unit` *type* has exactly one *value*,
denoted by `()` and is used for methods that don't produce a result. `Unit` is
similar to the `void` type in other languages (you can also think of `()` as a
tuple with zero components).

We then use the `println` function to retrieve the first argument and concatenate it
with the string `"Hello, "`. Scala can actually infer the result type of
methods with a single exit point, so it would be valid to omit the `: Unit` in
the code. And, if the result type of a method is `Unit`, you can also omit the
`=`. Our simplified main method then looks like this:

{% highlight scala %}
object HelloWorld {
  def main(args: Array[String]) {
    println("Hello, " + args(0))
  }
}
{% endhighlight %}

Don't let the different kinds of brackets confuse you: Instead of Java and C++'s
`<>` for type or template parameters, Scala uses `[]`. Indexing an array does not
have any special syntax (actually, Scala does not treat arrays specially at
all), as we have seen in the example, so there's no index-operator. The rules
are quite simple: value parameters use `()` and type parameters `[]`.
Statement lists are enclosed in `{}`, this one's the same in all 3 languages.

Exercises:

1. Change the program so it reads two arguments from the command line, and
   prints out a message using both of them.
1. Change the program so it performs a simple arithmetic operation on the two
   arguments and prints out the result. You can use the `toInt` method to convert a string to
   a number, and `toString` to convert a number back into a string. Play around with
   different operations.
1. `getLine` is a method that reads a line from the console and returns it as a
   string. Change the program so it prompts for a name, reads the name, and then
   prints that instead of the command line value.

Enough with the toy examples: in the next chapter, [we're going to write a parser](2_parsing.html)!

