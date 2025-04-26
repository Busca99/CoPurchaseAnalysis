import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.HashPartitioner
import scala.util.Try

object Main extends App {

  // -- Gestione Argomenti della Riga di Comando --
  // Si aspettano 2 argomenti: <bucket-name> <num-worker-nodes>
  if (args.length != 2) {
    System.err.println("Inserire: Main <bucket-name> <num-worker-nodes>")
    System.exit(1)
  }

  val bucketName = args(0)
  val numNodesOpt = Try(args(1).toInt).toOption

  if (numNodesOpt.isEmpty || numNodesOpt.get <= 0) {
    System.err.println("Errore: <num-worker-nodes> deve essere un intero positivo.")
    System.exit(1)
  }
  val numNodes = numNodesOpt.get // Numero di worker nodes passato come argomento

  // -- Creazione SparkSession e SparkContext --
  val spark = SparkSession.builder
    .appName("scp-co-purchase") // Nome applicazione
    // Configurazione delle risorse
    .config("spark.executor.memory", "5g") // Memoria per executor
    .config("spark.executor.cores", "4")   // Core per executor
    .config("spark.driver.memory", "4g")   // Memoria per il driver
    .getOrCreate()

  val sc = spark.sparkContext

  // -- Definizione Percorsi e Avvio Timer --
  val startTime = System.nanoTime() // Avvio misurazione tempo

  // Usa il bucketName passato come argomento
  val inputPath = s"gs://${bucketName}/order_products.csv" // File nel bucket
  val outputPath = s"gs://${bucketName}/output" // Directory di output nel bucket

  println(s"Lettura dati da: ${inputPath}")
  println(s"Scrittura risultati in: ${outputPath}")
  println(s"Numero di worker nodes: ${numNodes}")

  // -- Caricamento e Parsing del Dataset --
  val data: RDD[(Int, Int)] = sc.textFile(inputPath)
    .mapPartitions(iter => iter.flatMap { line => // flatMap per gestire eventuali errori di parsing per riga
      Try {
        val row = line.split(",")
        (row(0).toInt, row(1).toInt) // Coppia (orderId, productId)
      }.toOption // Converte Try in Option, scartando le righe malformate
    })

  // -- Calcolo Partizioni e Ripartizionamento --
  // Ottiene il numero di core per executor dalla configurazione Spark
  // Fornisce un default nel caso non sia specificato
  val numCoresPerNode = spark.conf.get("spark.executor.cores", "2").toInt

  // Calcola il numero di partizioni basandosi sui core totali disponibili nei worker
  // Si punta ad avere 2-4 task per core come euristica comune
  val numPartitions = numNodes * numCoresPerNode * 3 // Esempio: 3 task per core

  println(s"Numero core per worker node (da config): ${numCoresPerNode}")
  println(s"Numero di partizioni calcolato: ${numPartitions}")

  // Partiziona i dati usando HashPartitioner per distribuire il carico nelle fasi successive
  val partitionData: RDD[(Int, Int)] = data.partitionBy(new HashPartitioner(numPartitions))

  // -- Raggruppamento Prodotti per Ordine --
  // Raggruppa per orderId per ottenere tutti i prodotti di un ordine
  val groupedByOrderId: RDD[(Int, Iterable[Int])] = partitionData.groupByKey()

  // -- Generazione Coppie di Prodotti Co-acquistati --
  // Per ogni ordine, genera tutte le coppie uniche di prodotti (p1, p2) con p1 < p2
  val productPairs: RDD[((Int, Int), Int)] = groupedByOrderId
    .flatMap { case (_, products) =>
      // Converte in una sequenza per poter generare le combinazioni
      val sortedProductList = products.toSeq.distinct.sorted
      // Genera le combinazioni di 2 elementi (coppie)
      sortedProductList.combinations(2).map { pair =>
        ((pair(0), pair(1)), 1) // ((prod1, prod2), 1) con prod1 < prod2
      }
    }

  // -- Conteggio Occorrenze Coppie --
  // Somma i conteggi per ogni coppia di prodotti identica
  val countedProductPairs: RDD[((Int, Int), Int)] = productPairs
    .reduceByKey(_ + _)

  // -- Formattazione Risultato Finale --
  // Formatta l'output come "product1,product2,count"
  val results: RDD[String] = countedProductPairs
    .map { case ((product1, product2), count) =>
      s"$product1,$product2,$count"
    }

  // -- Salvataggio Risultato --
  // Ripartiziona a 1 per avere un singolo file/directory di output.
  val result = results.repartition(1)
  result.saveAsTextFile(outputPath)

  // -- Calcolo e Stampa Tempo di Esecuzione --
  val endTime = System.nanoTime()
  val totalTime = (endTime - startTime) / 1e9 // Tempo totale in secondi

  println(f"Numero worker nodes utilizzati: $numNodes")
  println(f"Numero core per worker node: $numCoresPerNode")
  println(f"Numero partizioni utilizzate: $numPartitions")
  println(f"Tempo di esecuzione totale: $totalTime%.2f secondi")

  // -- Stop SparkSession --
  spark.stop()
}
