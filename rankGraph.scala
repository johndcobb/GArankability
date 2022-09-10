  class rankGraph(var NUM_TEAMS: Int) {
//include import statementsinside class or else they wont work 
	import org.apache.spark._ 
  	import org.apache.spark.graphx._
 	import org.apache.spark.rdd.RDD
	import scala.util.Random
  	import scala.collection.mutable.ArrayBuffer
  	import breeze.linalg._
  	import breeze.numerics._ //numerical processing library; helps to solve linear system
  	import breeze.math._
  	import org.apache.spark.mllib.linalg.{Vectors,Vector,DenseVector}
  	import org.apache.spark.mllib.linalg.{Matrix,Matrices,DenseMatrix}
  	import org.apache.spark.rdd.RDD._
  	import org.apache.spark.{SparkConf,SparkContext}

	
	/*val conf = new SparkConf().setAppName("App")
	val sc = new SparkContext(conf) */
	private val numTeams = NUM_TEAMS
	private var vertexArray = Array.ofDim[(Long,String)](NUM_TEAMS)
	private var edgeArray = ArrayBuffer[Edge[Int]]()
        var edgeRDD : RDD[Edge[Int]] = sc.parallelize(this.edgeArray)

	//define getter method for reference var
	def getNumTeams(): Int = {
		return this.numTeams }
	def getVertexArray(): Array[(Long,String)] = {
		return this.vertexArray}
	def setVertexArray(array: Array[(Long,String)]): Unit = {
		this.vertexArray = array}
	def getEdgeArray(): ArrayBuffer[Edge[Int]]={
		return this.edgeArray}
	def getEdgeRDD(): RDD[Edge[Int]] = {
		return this.edgeRDD}

	//create rand matrix using reference var numTeams (dimension of matrix);
	//will be skew symmetric
        def buildTennisGraph(filepath: String): Graph[String, Double] = {
          val file = sc.textFile(filepath)

          val header = file.first()

	  //remove header and split entries into an Array

          val body: RDD[Array[String]] = file.filter(line => line != header).map(_.split(","))

	  // maps the lines to an Edge(winner_id,loser_id,games won) by reducing ((winner_id, loser_id),1.0) tuples by key. 
          val edges: RDD[Edge[Double]] = sc.parallelize(body.map(line => ((line(7),line(17)), 1.0)).reduceByKey(_+_).map{ case ((team1, team2), count) => Edge(team1.toLong, team2.toLong, count)}.collect)

          // Saves ( (winner_id, winner_name) , (loser_id, loser_name) ) as a List to flatten it with a weird trick (.flatMap(identity)), so I can save all unique names with .distinct
          val vertices: RDD[(VertexId, String)] = sc.parallelize(body.map(line => List((line(7).toLong, line(10)),(line(17).toLong, line(20)))).flatMap(identity).distinct.collect)

	  val graph: Graph[String, Double] = Graph(vertices, edges)
          return graph
        }

        def Colley(graph: Graph[String, Double]): Graph[(String, Double), Double] = {
		val totals: RDD[(Long, Double)] = graph.aggregateMessages[Double](
		  triplet => { // Map Functon
		    //
		    triplet.sendToDst(triplet.attr)
		    triplet.sendToSrc(triplet.attr)
		    },
		  // Merge Function
		  (a,b) => a+b,
		  // Optimization preference
		  TripletFields.EdgeOnly
		)

		val wins: RDD[(Long, Double)] = graph.aggregateMessages[Double](
		  triplet => { // Map Function
		    //
		    triplet.sendToSrc(triplet.attr)
		    },
		  // Merge Function
		  (a,b) => a+b,
		  // Optimization preference
		  TripletFields.EdgeOnly
		)

		// saves totals, wins, losses

		// Join the two lists, preserving all ID's
		// leftOuterJoin leaves weird Some(value) and None Data types, convert to value and 0
		val list = totals.leftOuterJoin(wins).mapValues( vertexAttr => vertexAttr._2 match{
		    // Save as (totals, wins, losses)
		    case Some(win) => (vertexAttr._1, win.toDouble, vertexAttr._1-win)
		    case None => (vertexAttr._1, 0.0, vertexAttr._1)
		  }
		)

		// infoGraph is constructed to hold the game number totals, wins, losses
		val infoGraph: Graph[(String, (Double,Double,Double)), Double]= graph.outerJoinVertices(list)((vid, oldAttr, degOpt) => (oldAttr,degOpt.getOrElse(0,0,0)))

		// Using info from infoGraph, rankGraph is initialized with the inital rank (1+Nw)/(2+Ntotal)
		var rankGraph: Graph[(String, Double), Double] = infoGraph.mapVertices{case (id,(team, (totals, wins, losses))) => (team, (1 + wins)/(2 + totals))}

		// Using info from infoGraph, templateGraph holds the two parts of the combined iterative equation - the numerator (1+ (Nw,i - Nl,i)/2){ Note: or entry bi in Cr = b} and the denominator 2 + Ntotal. These will be combined with an updating ranklist each iteration.
		val templateGraph: Graph[(String, Double, Double), Double] = infoGraph.mapVertices{case (id,(team, (totals, wins, losses))) => (team,(1+(wins-losses)/2), 2 + totals)}

		// Differences of ranking must vary less than epsilon to indicate convergence
		val epsilon = 1E-4
		// initialize the variables holding the previous rankGraph to compare ranking deviations (prevRankGraph) and the one holding the max ranking deviation to compare to epsilon (rankDiffMax)
		var prevRankGraph: Graph[(String,Double),Double] = null

		var rankDiffMax = 1.0

		while (rankDiffMax > epsilon){
		//Check to see if collect neighbors works better
		  val rankUpdate = rankGraph.aggregateMessages[Double](
		    triplet =>{ 
		      triplet.sendToDst(triplet.attr*triplet.srcAttr._2)
		      triplet.sendToSrc(triplet.attr*triplet.dstAttr._2)
		    },
		  (a,b) => a + b,
		  TripletFields.All
		  )

		  prevRankGraph = rankGraph

		  rankGraph = templateGraph.outerJoinVertices(rankUpdate)((vid, oldAttr, degOpt)   => (oldAttr._1, (oldAttr._2 + degOpt.getOrElse(0).asInstanceOf[Double])/oldAttr._3))

		  rankDiffMax = rankGraph.vertices.leftOuterJoin(prevRankGraph.vertices).map( vertexAttr => vertexAttr._2._2 match {
		  case Some(rank) => (rank._2 - vertexAttr._2._1._2).abs
		  case None => 0
		  }).max
		} // end iterations
          return rankGraph
        } // end Colley

	def colleyFaster(graph: Graph[String, Double]): Graph[(String, Double), Double] = {
		val totals: RDD[(Long, Double)] = graph.aggregateMessages[Double](
		  triplet => { // Map Functon
		    //
		    triplet.sendToDst(triplet.attr)
		    triplet.sendToSrc(triplet.attr)
		    },
		  // Merge Function
		  (a,b) => a+b,
		  // Optimization preference
		  TripletFields.EdgeOnly
		)

		val wins: RDD[(Long, Double)] = graph.aggregateMessages[Double](
		  triplet => { // Map Function
		    //
		    triplet.sendToSrc(triplet.attr)
		    },
		  // Merge Function
		  (a,b) => a+b,
		  // Optimization preference
		  TripletFields.EdgeOnly
		)

		// saves totals, wins, losses

		// Join the two lists, preserving all ID's
		// leftOuterJoin leaves weird Some(value) and None Data types, convert to value and 0
		val list = totals.leftOuterJoin(wins).mapValues( vertexAttr => vertexAttr._2 match{
		    // Save as (totals, wins, losses)
		    case Some(win) => (vertexAttr._1, win.toDouble, vertexAttr._1-win)
		    case None => (vertexAttr._1, 0.0, vertexAttr._1)
		  }
		)

		// infoGraph is constructed to hold the game number totals, wins, losses
		val infoGraph: Graph[(String, (Double,Double,Double)), Double]= graph.outerJoinVertices(list)((vid, oldAttr, degOpt) => (oldAttr,degOpt.getOrElse(0,0,0)))

		// Using info from infoGraph, rankGraph is initialized with the inital rank (1+Nw)/(2+Ntotal)
		var rankGraph: Graph[(String, Double), Double] = infoGraph.mapVertices{case (id,(team, (totals, wins, losses))) => (team, (1 + wins)/(2 + totals))}

		// Using info from infoGraph, templateGraph holds the two parts of the combined iterative equation - the numerator (1+ (Nw,i - Nl,i)/2){ Note: or entry bi in Cr = b} and the denominator 2 + Ntotal. These will be combined with an updating ranklist each iteration.
		val templateGraph: Graph[(String, Double, Double), Double] = infoGraph.mapVertices{case (id,(team, (totals, wins, losses))) => (team,(1+(wins-losses)/2), 2 + totals)}

		// initialize the variables holding the previous rankGraph to compare ranking deviations (prevRankGraph) and the one holding the max ranking deviation to compare to epsilon (rankDiffMax)
		var prevRankGraph: Graph[(String,Double),Double] = null
		val iterations = 20

		for (i <- 0 to iterations){
		//Check to see if collect neighbors works better
		  val rankUpdate = rankGraph.aggregateMessages[Double](
		    triplet =>{ 
		      triplet.sendToDst(triplet.attr*triplet.srcAttr._2)
		      triplet.sendToSrc(triplet.attr*triplet.dstAttr._2)
		    },
		  (a,b) => a + b,
		  TripletFields.All
		  )

		  rankGraph = templateGraph.outerJoinVertices(rankUpdate)((vid, oldAttr, degOpt)   => (oldAttr._1, (oldAttr._2 + degOpt.getOrElse(0).asInstanceOf[Double])/oldAttr._3))
		} // end iterations
	  return rankGraph.cache()
	} // end colleyFaster

	def removeNode(identifier: Long, graph: Graph[String,Double]): Graph[String,Double] = {
             
		val newGraph = graph.subgraph(vpred = (id, attr) => id != identifier) 

		return newGraph //subgraph, node removed
	} // end removeNode

	def sampleGraph(): Graph[String, Double] = {
		// Initialize a small graph with optional team names.
		val vertexRDD: RDD[(Long, String)] = sc.parallelize(Array(
		(1L, "Team 1"),
		(2L, "Team 2"),
		(3L, "Team 3"),
		(4L, "Team 4"),
		(5L, "Team 5"),
		(6L, "Team 6")
		))
		// Let the edge fields be (team 1 beat, team 2, # times)
		val edgeRDD: RDD[Edge[Double]] = sc.parallelize(Array(
		Edge(2L, 1L, 7.0),
		Edge(2L, 4L, 2.0),
		Edge(3L, 2L, 4.0),
		Edge(3L, 6L, 3.0),
		Edge(4L, 1L, 1.0),
		Edge(5L, 2L, 2.0),
		Edge(5L, 3L, 8.0),
		Edge(5L, 6L, 3.0)
		))
		// consider adding a default "missing link" for loaded datasets
		val graph: Graph[String, Double]= Graph(vertexRDD,edgeRDD)
		return graph
	} // end sampleGraph

	def singleNodeSensitivity(node: VertexId, graph: Graph[String, Double], initRank: Graph[(String, Double), Double] ): Double = {
		val removedGraph: Graph[String, Double] = removeNode(node, graph)
		val newRankingGraph: Graph[(String, Double), Double] = colleyFaster(removedGraph)
		val differenceGraph: Graph[(String, Double), Double] = initRank.outerJoinVertices(newRankingGraph.vertices){ (id, oldRanking, newRanking) => newRanking match{
		case Some(newRank) => (oldRanking._1, scala.math.pow((oldRanking._2 - newRank._2).abs,2))
		case None => (oldRanking._1, scala.math.pow(oldRanking._2,2)) }
		}
		val differenceNorm: Double = scala.math.sqrt(differenceGraph.vertices.map{ case (id,(name, residual)) => residual}.reduce(_+_))
		return differenceNorm
	
	} // end singleNodeSensitivity

	def sampleEdges(graph: Graph[String, Double], fraction: Double): Graph[String, Double] = {
		val edgeSample: RDD[Edge[Double]] = graph.edges.sample(false, fraction)
		val newGraph: Graph[String, Double] = Graph(graph.vertices, edgeSample)
		return newGraph
	} // end sampleEdges

	def sampleNodes(graph: Graph[String, Double], fraction: Double): Graph[String, Double] = {
		val nodeSample: RDD[(VertexId, String)] = graph.vertices.sample(false, fraction)
		val defaultNodeAttr = "Missing"
		val newGraph: Graph[String, Double] = Graph(nodeSample, graph.edges, defaultNodeAttr)
		val finalGraph: Graph[String, Double] = newGraph.subgraph(vpred = (id, vertexAttr) => vertexAttr != "Missing")
		return finalGraph
	} // end sampleNodes

	def replaceNode(inputGraph: Graph[(String, Double), Double], newNode: (VertexId,(String, Double)) ): Graph[(String, Double), Double] = {
		val currentVertexId: VertexId = newNode._1
		val currentVertexAttr: (String, Double) = newNode._2
		val newGraph: Graph[(String, Double), Double] = inputGraph.mapVertices( (id, VertexAttr) => if (id == currentVertexId){
		newNode._2 } else VertexAttr)
		return newGraph
	}// end replaceNode
	/*
	
	//def colleyize() //returns colley rating vector


	def checkSensitivity(graph: Graph[String,Double], method: String): Graph[(String, Double),Double] = { 
		// without respect to case, check if method is "c" or "m" to use colley or massey
		if (method.compareToIgnoreCase("C") == 0  )  {
                  var initGraph: Graph[(String, Double), Double] = Colley(graph)
                  var sensitivityGraph: Graph[String, Double] = null
		  var nodeRemovedGraph: Graph[String, Double] = null
		  for (i <- graph.vertices.collect.toList) {
		    nodeRemovedGraph = Colley(removeNode(i._1, graph))
                    
		  }
		}
		else if (method.compareToIgnoreCase("M") == 0 ) {
                //	def ranking(input: Graph[String, Double]): Graph[(String, Double), Double]  = Massey(input)
		}
	} // end checkSensitivity

		
	def printWins(graph: Graph[String,Int]): Unit = {
		for (triplet<- graph.triplets.collect) {
			println(s"${triplet.srcAttr} beat ${triplet.dstAttr}")}
	}

def createRandMatrix(numTeams:Int): Array[Array[Int]] = {

        	val matrix = Array.ofDim[Int](numTeams,numTeams)
		var r = scala.util.Random //use this r variable to create random score differences 
		//in the numTeamXnumTeam matrix
		for (i <- 0 to numTeams) {
        		for (j <- 0 to numTeams) {
         			if (i == j)
            				matrix(i)(j) = 0
        		}
      		}

      		for (i <- 0 to numTeams) {
        		for (j <- (i + 1) to numTeams) {
          			matrix(i)(j) = r.nextInt(25) //max score difference is 25
        		}
      		}
      		
		for (i <- 0 to numTeams) {
        		for (j <- 0 to i) {
          			matrix(i)(j) = -matrix(j)(i) // lower triangular portion is negative
        		}
      		}

        return matrix // returns random skew symmetric matrix with 0s on diagonal; score differences
	}
	

	//define method to build graph using built matrix 
	//numVertices = numTeams; 
	def buildGraphFromMatrix(matrix: Array[Array[Int]]): Graph[String,Int] = {
		//create array of (numTeams) vertices
		//var vertexArray = Array.ofDim[(Long,String)](this.numTeams)
		for ( i <- 0 to this.numTeams)
		{ this.vertexArray(i) = (i.toLong, ("team " + i))} //array of (long, string)s
		var vertexRDD: RDD[(Long, String)] = sc.parallelize(this.vertexArray)
		//use arraybuffer to enable appending to edgeArray
		//var edgeArray = ArrayBuffer[Edge[Int]]() already defined
		for (i <-0 to this.numTeams -1) {
			for (j <- i + 1 to this.numTeams) {
			//index the vertex array to link vertices with edges
				this.edgeArray += Edge(vertexArray(i)._1,vertexArray(j)._1,matrix(i)(j))
			}
		}
		
		//now create edges corresponding to lower half of skew symmetric matrix
		for (i <- 0 to this.numTeams) {
        		for (j <- 0 to i) {
				this.edgeArray += Edge(vertexArray(i)._1,vertexArray(j)._1,-matrix(j)(i))
			}
		}
		this.edgeRDD = sc.parallelize(this.edgeArray)
		var graph: Graph[String,Int] = Graph(vertexRDD,this.edgeRDD)
		return graph
	}

*/
	
}//end class

/*
def main(): Unit = { 

	val masseyExp1 = new rankGraph(25) 
	println("Num teams: " + masseyExp1.getNumTeams())
	val numTeams = masseyExp1.getNumTeams()
	var winLossData = masseyExp1.createRandMatrix(numTeams) //skew symmetric matrix created
	for (i <- 0 to masseyExp1.getNumTeams()){
		for (j <-0 to masseyExp1.getNumTeams()){
			print(winLossData(i)(j) + "\t")
		}
	}

	val graphStuff = masseyExp1.buildGraphFromMatrix(winLossData) 
	masseyExp1.printWins(graphStuff)
*/

  val rank = new rankGraph(25)
  val tennisGraph = rank.buildTennisGraph("/home/john/Downloads/atp_matches_2011.csv")
				

		
			
				








