// Databricks notebook source
// MAGIC %md
// MAGIC ## About the [**VariantSpark**](http://bioinformatics.csiro.au/variantspark) Demo
// MAGIC * A custom machine learning library for real-time genomic data analysis 
// MAGIC   * Works with thousands of samples and **millions** of variants 
// MAGIC   * Implements a custom **random forest** algorithm built on Apache Spark
// MAGIC   * Authored in Scala by the team at [CSIRO Bioinformatics](http://bioinformatics.csiro.au/) Australia
// MAGIC   
// MAGIC * Finds the most *important* variants attributing to a phenotype of interest 
// MAGIC   * Uses a synthetic phenotype called *HipsterIndex* 
// MAGIC   * Includes a dataset with a subset of the samples and variants 
// MAGIC   * Uses VCF format - from the 1000 Genomes Project 

// COMMAND ----------

// MAGIC %md
// MAGIC ## About the Hipster Index
// MAGIC The synthetic HipsterIndex was created using the following genotypes and formular:
// MAGIC 
// MAGIC | ID |SNP ID     | chromosome | position | phenotype | reference |
// MAGIC |---:|----------:|----:|-------:|-----:|----------:|------:|
// MAGIC | B6 |[rs2218065](https://www.ncbi.nlm.nih.gov/projects/SNP/snp_ref.cgi?rs=2218065) | chr2 | 223034082 | monobrow | [Adhikari K, et al. (2016) Nat Commun.](https://www.ncbi.nlm.nih.gov/pubmed/?term=26926045) |
// MAGIC | R1 |[rs1363387](https://www.ncbi.nlm.nih.gov/projects/SNP/snp_ref.cgi?rs=1363387) | chr5 | 126626044 | Retina horizontal cells (checks) | [Kay, JN et al. (2012) Nature](https://www.ncbi.nlm.nih.gov/pubmed/?term=22407321)
// MAGIC | B2 |[rs4864809](https://www.ncbi.nlm.nih.gov/projects/SNP/snp_ref.cgi?rs=4864809) | chr4 |  54511913 | beard | [Adhikari K, et al. (2016) Nat Commun.](https://www.ncbi.nlm.nih.gov/pubmed/?term=26926045)
// MAGIC | C2 |[rs4410790](https://www.ncbi.nlm.nih.gov/projects/SNP/snp_ref.cgi?rs=4410790)  |chr7 |17284577| coffee consumption        | [Cornelis MC et al. (2011) PLoS Genet.](https://www.ncbi.nlm.nih.gov/pubmed/?term=21490707) |
// MAGIC 
// MAGIC `HipsterIndex = ((2 + GT[B6]) * (1.5 + GT[R1])) + ((0.5 + GT[C2]) * (1 + GT[B2]))`
// MAGIC 
// MAGIC   GT stands for the genotype at this location with *homozygote* reference encoded as 0, *heterozygote* as 1 and *homozygote alternative* as 2. We then label individuals with a HipsterIndex score above 10 as hipsters, and the rest non-hipsters. By doing so, we created a binary annotation for the individuals in the 1000 Genome Project.
// MAGIC 
// MAGIC In this notebook, we demonstrate the usage of VariantSpark to **reverse-engineer** the association of the selected SNPs to the phenotype of insterest (i.e. being a hipster).

// COMMAND ----------

// MAGIC %md 
// MAGIC ###1. LOAD DATA
// MAGIC 
// MAGIC Use python to import the urllib library to load the demo datasets from the source AWS S3 bucket to the `DBFS`(Databricks file system) destination  

// COMMAND ----------

// MAGIC %python
// MAGIC import urllib
// MAGIC urllib.request.urlretrieve("https://github.com/aehrc/VariantSpark/raw/master/data/hipsterIndex/hipster.vcf.bgz", "/tmp/hipster.vcf.gz")
// MAGIC urllib.request.urlretrieve("https://github.com/aehrc/VariantSpark/raw/master/data/hipsterIndex/hipster_labels.txt", "/tmp/hipster_labels.txt")

// COMMAND ----------

// MAGIC %md
// MAGIC # Convert Blocked GNU Zip to regular GZip
// MAGIC We need to install an OS utility here as our VariantSpark can't read the Blocked GNU Zip which the Hipster file is stored in.

// COMMAND ----------

// MAGIC %sh
// MAGIC apt-get -y install tabix

// COMMAND ----------

// MAGIC %sh
// MAGIC cd /tmp
// MAGIC bgzip -d hipster.vcf.gz
// MAGIC gzip hipster.vcf

// COMMAND ----------

dbutils.fs.mv("file:/tmp/hipster.vcf.gz", "dbfs:/vs-datasets/hipsterIndex/hipster.vcf.gz")
dbutils.fs.mv("file:/tmp/hipster_labels.txt", "dbfs:/vs-datasets/hipsterIndex/hipster_labels.txt")
display(dbutils.fs.ls("dbfs:/vs-datasets/hipsterIndex"))
