error id: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/a.scala:
file://<WORKSPACE>/server/src/main/scala/cp/serverPr/a.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -synchronized.
	 -synchronized#
	 -synchronized().
	 -scala/Predef.synchronized.
	 -scala/Predef.synchronized#
	 -scala/Predef.synchronized().
offset: 185
uri: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/a.scala
text:
```scala




private def nextId(): Int = this.synchronized {
  counter += 1
  counter
}

val requestOpt = this.synchronized {
  if (queue.nonEmpty) Some(queue.dequeue()) else None
}

this.synchr@@onized {
  queue.enqueue(req)
  resultQueue.enqueue(req)
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 