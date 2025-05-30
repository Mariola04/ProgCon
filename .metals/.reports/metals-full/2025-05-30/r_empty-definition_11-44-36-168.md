error id: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/a.scala:java/lang/Object#
file://<WORKSPACE>/server/src/main/scala/cp/serverPr/a.scala
empty definition using pc, found symbol in pc: java/lang/Object#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 2
uri: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/a.scala
text:
```scala
pr@@ivate def nextId(): Int = this.synchronized {
  counter += 1
  counter
}

val requestOpt = this.synchronized {
  if (queue.nonEmpty) Some(queue.dequeue()) else None
}

this.synchronized {
  queue.enqueue(req)
  resultQueue.enqueue(req)
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: java/lang/Object#