# Scalable and cloud programming - project
Project for the UNIBO "Scalable and Cloud Programming" course (a.y. 2024-25).

## Generare il file .jar del progetto
Dopo aver scaricato il progetto:
1. Apri un Terminale o Prompt dei Comandi.
2. Naviga alla Directory Radice del Progetto. <br>
``` cd /percorso/del/tuo/progetto/CoPurchaseAnalysis ```
3. Esegui il Comando sbt package: sbt scaricherà le dipendenze (se non già presenti), compilerà il tuo codice Scala e infine creerà il file JAR. <br>
sbt package
4. Individua il File JAR Creato: Una volta che il comando ha terminato con successo (vedrai un messaggio tipo [success] Total time: ...), il file JAR sarà stato creato nella seguente sottodirectory del tuo progetto. <br>
``` target/scala-2.12/copurchaseanalysis_2.12-0.1.0-SNAPSHOT.jar```

## Crea e riempi il bucket
1. Per creare il bucket inserisci nella cloud shell il seguente comando: <br>
``` gcloud storage buckets create gs://<NOME_BUCKET_UNIVOCO> --location=us-central1 ```
2. Poi inserisci nel bucket il dataset e il file .jar: <br>
``` gcloud storage cp <PERCORSO_LOCALE_CSV> gs://<NOME_TUO_BUCKET>/ ``` <br>
``` gcloud storage cp <PERCORSO_LOCALE_JAR> gs://<NOME_TUO_BUCKET>/ ```

## Cluster single-node
Inserire i seguenti comandi per settare il progetto e la regione predefinite: <br>
``` gcloud config set project scp-co-purchase ``` <br>
``` gcloud config set compute/region us-central1 ``` <br>

### Creazione cluster single-node
Lancia il seguente comando nella cloud shell: <br>
``` gcloud dataproc clusters create scp-cluster-1w --region=us-central1 --single-node --master-machine-type=n1-standard-4 --master-boot-disk-size 240 ```

### Esecuzione del job per il cluster single-node
Lancia il seguente comando nella cloud shell: <br>
```
gcloud dataproc jobs submit spark \
    --cluster=scp-cluster-1w \
    --region=us-central1 \
    --jar=gs://<NOME_TUO_BUCKET>/copurchaseanalysis_2.12-0.1.0-SNAPSHOT.jar \
    -- \
    <NOME_TUO_BUCKET> 1
```

### Eliminazione del cluster single-node
Lancia il seguente comando nella cloud shell: <br>
``` gcloud dataproc clusters delete scp-cluster-1w --region us-central1 --project=scp-co-purchase ```

## Cluster con N nodi worker

### Creazione cluster con N nodi worker
Lancia il seguente comando nella cloud shell: <br>
```
gcloud dataproc clusters create scp-cluster-<NUM_WORKERS>w \
    --region=us-central1 \
    --master-machine-type=n1-standard-4 \
    --worker-machine-type=n1-standard-4 \
    --num-workers=<NUM_WORKERS> \
    --master-boot-disk-size=240 \
    --worker-boot-disk-size=240
```

### Esecuzione del job per il cluster con N nodi worker
Lancia il seguente comando nella cloud shell: <br>
```
gcloud dataproc jobs submit spark \
    --cluster=scp-cluster-<NUM_WORKERS>w \
    --region=us-central1 \
    --jar=gs://<NOME_TUO_BUCKET>/copurchaseanalysis_2.12-0.1.0-SNAPSHOT.jar \
    -- \
    <NOME_TUO_BUCKET> <NUM_WORKERS>
```

### Eliminazione del cluster con N nodi worker
Lancia il seguente comando nella cloud shell: <br>
``` gcloud dataproc clusters delete scp-cluster-<NUM_WORKERS>w --region us-central1 --project=scp-co-purchase ```

## File di output
Se lo si preferisce si può rinominare il file di output in modo da convertirlo in un file col formato .csv:
``` gcloud storage mv gs://<NOME_TUO_BUCKET>/output/part-00000 gs://<NOME_TUO_BUCKET>/output/result.csv ```
