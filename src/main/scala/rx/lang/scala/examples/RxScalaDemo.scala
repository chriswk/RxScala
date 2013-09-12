package rx.lang.scala.examples

import org.scalatest.junit.JUnitSuite

import scala.language.postfixOps
import rx.lang.scala._
import scala.concurrent.duration._
import org.junit.{Before, Test, Ignore}

import org.junit.Assert._

//@Ignore // Since this doesn't do automatic testing.
class RxScalaDemo extends JUnitSuite {

  @Test def intervalExample() {
    val o = Observable.interval(200 millis).take(5)
    o.subscribe(n => println("n = " + n))
    
    // need to wait here because otherwise JUnit kills the thread created by interval()
    waitFor(o)
    
    println("done")
  }
  
  def msTicks(start: Long, step: Long): Observable[Long] = {
    // will be easier once we have Observable.generate method
    Observable.interval(step millis) map (_ * step + start)
  }
  
  def prefixedTicks(start: Long, step: Long, prefix: String): Observable[String] = {
    msTicks(start, step).map(prefix + _)
  }
  
  @Test def testTicks() {
    val o = prefixedTicks(5000, 500, "t = ").take(5)
    o.subscribe(output(_))
    waitFor(o)
  }
  
  @Test def testSwitch() {
    // We do not have ultimate precision: Sometimes, 747 gets through, sometimes not
    val o = Observable.interval(1000 millis).map(n => prefixedTicks(0, 249, s"Observable#$n: "))
                 .switch.take(16)
    o.subscribe(output(_))
    waitFor(o)
  }
  
  @Test def testSwitchOnObservableOfInt() {
    // Correctly rejected with error 
    // "Cannot prove that Observable[Int] <:< Observable[Observable[U]]"
    // val o = Observable(1, 2).switch
  }

  @Test def testMyOwnSequenceEqual() {
    // the sequenceEqual operation can be obtained like this:
    
    val first = Observable(10, 11, 12)
    val second = Observable(10, 11, 12)
    
    val b1 = (first zip second) map (p => p._1 == p._2) forall (b => b)
    
    val equality = (a: Any, b: Any) => a == b
    val b2 = (first zip second) map (p => equality(p._1, p._2)) forall (b => b)
    
    assertTrue(b1.toBlockingObservable.single)
    assertTrue(b2.toBlockingObservable.single)
  }
  
  @Test def testMyOwnSequenceEqualWithForComprehension() {
    // the sequenceEqual operation can be obtained like this:
    
    val first = Observable(10, 11, 12)
    val second = Observable(10, 11, 12)
    
    val booleans = for ((n1, n2) <- (first zip second)) yield (n1 == n2)
    
    val b1 = booleans.forall(_ == true) // without `== true`, b1 is assigned the forall function
    
    assertTrue(b1.toBlockingObservable.single)
  }
  
  @Test def testStartWithIsUnnecessary() {
    val before = Observable(-2, -1, 0)
    val source = Observable(1, 2, 3)
    println((before ++ source).toBlockingObservable.toList)
  }

  @Test def mergeExample() {
    val slowNumbers = Observable.interval(400 millis).take(5).map("slow " + _)
    val fastNumbers = Observable.interval(200 millis).take(10).map("fast " + _)
    val o = (slowNumbers merge fastNumbers)
    o.subscribe(output(_))
    waitFor(o)
  }
  
  @Test def rangeAndBufferExample() {
    val o = Observable(1 to 18)
    o.buffer(5).subscribe((l: Seq[Int]) => println(l.mkString("[", ", ", "]")))
  }
  
  @Test def windowExample() {
    // this will be nicer once we have zipWithIndex
    (for ((o, i) <- Observable(1 to 18).window(5) zip Observable(0 until 4); n <- o) 
      yield s"Observable#$i emits $n")
        .subscribe(output(_))
  }
  
  @Test def testReduce() {
    assertEquals(10, Observable(1, 2, 3, 4).reduce(_ + _).toBlockingObservable.single)
  }
  
  def output(s: String): Unit = println(s)
  
  // blocks until obs has completed
  def waitFor[T](obs: Observable[T]): Unit = {
    obs.toBlockingObservable.last
  }
  
}